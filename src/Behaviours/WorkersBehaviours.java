package Behaviours;

import Agents.Worker;
import Utilities.Order;
import Utilities.Utils;
import Utilities.WorkerOffer;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ReceiverBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetResponder;

import javax.swing.plaf.synth.SynthTextAreaUI;
import java.io.IOException;

public class WorkersBehaviours {

    private Worker worker;

    public WorkersBehaviours(Agent worker) {
        this.worker = (Worker) worker;
    }


    public class StartWork extends OneShotBehaviour {

        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(worker.getCompany());
            msg.setContent("worker hired" + worker.getSalary());
            worker.send(msg);
            MessageTemplate template = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                    MessageTemplate.MatchPerformative(ACLMessage.CFP));

            worker.addBehaviour(new GetWork(template));
            worker.addBehaviour(new DoMyJob());
            worker.addBehaviour(new Fired());
        }
    }

    public class Fired extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate tmp = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
            MessageTemplate with_inform = MessageTemplate.or(tmp, MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage msg = worker.receive(with_inform);
            if (msg != null) {
                System.out.println("FIRE WORK");

                if (msg.getPerformative() == ACLMessage.CANCEL) {
                    worker.doDelete();
                } else {
                    try {
                        Order o = (Order) msg.getContentObject();

                        worker.deleteOrder(o);
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                }
            } else block();
        }
    }


    public class GetWork extends ContractNetResponder {

        public GetWork(MessageTemplate template) {
            super(worker, template);

        }

        @Override
        protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
            ACLMessage reply = null;
            try {
                reply = cfp.createReply();

                Order o = (Order) cfp.getContentObject();

                if (worker.isFull()) {
                    reply.setPerformative(ACLMessage.REFUSE);

                } else {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContentObject(new WorkerOffer(worker));
                }
            } catch (UnreadableException | IOException e) {
                Utils.print("ERROR TRYING WORK ANSWER");
                e.printStackTrace();
            }

            return reply;
        }


        @Override
        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {

            try {
                worker.addOrder((Order) cfp.getContentObject());
                Utils.print("ADDED JOB to " + worker.getName());
            } catch (UnreadableException e) {
                e.printStackTrace();
            }

            ACLMessage reply = accept.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent("added job");
            return reply;
        }
    }


    public class DoMyJob extends TickerBehaviour {

        public DoMyJob() {
            super(worker, Utils.DAY_IN_MILLISECONDS);
        }

        @Override
        protected void onTick() {

            if (worker.getCurrentOrder() == null) {
                worker.setCurrentOrder(worker.getOrders().poll());
            }

            Order currentOrder = worker.getCurrentOrder();
            // currentOrder = currentOrder == null ? worker.getOrders().poll() : currentOrder;

            if (currentOrder != null) {
                currentOrder.decreaseQt(worker.getRate());
                worker.setCurrentOrder(currentOrder);
                worker.increaseTime();
                if (currentOrder.getQuantity() <= 0) {

                    ACLMessage l = new ACLMessage(ACLMessage.REQUEST); // inform?
                    try {
                        l.setContentObject(currentOrder);

                        l.addReceiver(worker.getCompany());
                        worker.send(l);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    worker.setCurrentOrder(null);
                }
            }
        }
    }
}

