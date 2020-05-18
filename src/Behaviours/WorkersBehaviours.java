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

            MessageTemplate template = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                    MessageTemplate.MatchPerformative(ACLMessage.CFP));

            worker.addBehaviour(new GetWork(template));
            worker.addBehaviour(new DoMyJob());
            worker.addBehaviour(new Fired());
            worker.addBehaviour(new ReceiveJob());

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(worker.getCompany());
            msg.setContent("worker hired" + worker.getSalary());
            worker.send(msg);

        }
    }

    public class Fired extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate cancelPerf = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
            MessageTemplate with_inform = MessageTemplate.or(cancelPerf, MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            MessageTemplate last = MessageTemplate.and(with_inform, MessageTemplate.not(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET)));
            ACLMessage msg = worker.receive(last);
            if (msg != null) {

                if (msg.getPerformative() == ACLMessage.CANCEL) {
                    System.out.println("FIRE WORK");

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

                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContentObject(new WorkerOffer(worker));

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
                System.out.println(worker.getLocalName() + " received a job");
            } catch (UnreadableException e) {
                e.printStackTrace();
            }

            ACLMessage reply = accept.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent("added job");
            return reply;
        }


        @Override
        protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {

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

    public class ReceiveJob extends CyclicBehaviour {

        @Override
        public void action() {

            MessageTemplate performativeTemplate = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            MessageTemplate protocolTemplate = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            MessageTemplate template = MessageTemplate.and(MessageTemplate.not(protocolTemplate), performativeTemplate);
            ACLMessage msg = worker.receive(template);

            if (msg != null) {
                try {

                    worker.addOrder((Order) msg.getContentObject());
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }
}

