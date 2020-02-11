package Behaviours;

import Agents.Company;
import Agents.Worker;
import Utilities.Order;
import Utilities.WorkerOffer;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetInitiator;

import java.io.IOException;
import java.util.PriorityQueue;
import java.util.Vector;

public class CompanyBehaviours {

    private Company company;

    public CompanyBehaviours(Agent company) {

        this.company = (Company) company;

    }


    public class ReceiveWorkers extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = company.receive(template);

            if (msg != null) {
                if (msg.getContent().equals("worker")) company.addWorker(msg.getSender());
            } else {
                block();
            }

        }
    }

    public class ReceiveRequests extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate template = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                    MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));

            MessageTemplate tmp = MessageTemplate.or(template, MessageTemplate.MatchPerformative(ACLMessage.CANCEL));

            ACLMessage msg = company.receive(tmp);

            if (msg != null) {

                switch (msg.getPerformative()) {

                    case ACLMessage.CFP:
                        try {
                            Order o = (Order) msg.getContentObject();
                            company.addBehaviour(new AssignWork(o));

                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                        break;
                    case ACLMessage.CONFIRM:


                    case ACLMessage.CANCEL:
                        // send message to Worker Telling to stop doing its work and be available again
                    default:
                        break;
                }
            } else {
                block();
            }

        }
    }


    public class AssignWork extends ContractNetInitiator {

        Order order = null;

        public AssignWork(Order order) {
            super(company, null);
            this.order = order;
        }

        @Override
        protected Vector prepareCfps(ACLMessage cfp) {
            for (AID k : company.getWorkers()) {
                cfp.addReceiver(k);
            }
            cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            cfp.setPerformative(ACLMessage.CFP);

            try {
                cfp.setContentObject(order);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Vector v = new Vector();
            v.add(cfp);
            return v;
        }


        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {

            ACLMessage offer = null;
            WorkerOffer workerOffer = null;

            for (Object i : responses) {
                if (((ACLMessage) i).getPerformative() == ACLMessage.PROPOSE) {

                    try {
                        WorkerOffer w = (WorkerOffer) ((ACLMessage) i).getContentObject();

                        if (w.isFull() || w.getCapacity() >= order.getQuantity()) continue;
                        if (workerOffer == null) {
                            offer = (ACLMessage) i;
                            workerOffer = w;
                            continue;
                        }
                        if ((!w.isWorking() && !workerOffer.isWorking()) || (w.isWorking() && workerOffer.isWorking())) {
                            if (workerOffer.getWorkingTime() > w.getWorkingTime()) {
                                offer = (ACLMessage) i;
                                workerOffer = w;
                            }
                        }
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }

                }
            }

            if (offer != null) {
                ACLMessage reply = offer.createReply();
                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                try {
                    reply.setContentObject(order);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                acceptances.add(reply);

                company.addOrder(offer.getSender(), order);
            } else {
                // TODO send cancels para parar negociação
                //  E DAR HANDLE A TODAS AS CONSEQUENCIAS
                //  DE NAO SE CONSEGUIR DAR HANDLE
            }
        }
    }

}
