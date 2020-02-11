package Behaviours;

import Agents.Worker;
import Utilities.Order;
import Utilities.WorkerOffer;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetResponder;

import java.io.IOException;

public class WorkersBehaviours {

    private Worker worker;
    Order currentOrder = null;

    public WorkersBehaviours(Agent worker) {
        this.worker = (Worker) worker;
    }


    public class StartWork extends OneShotBehaviour {

        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(worker.getCompany());
            msg.setContent("worker");
            worker.send(msg);
        }
    }


    public class GetWork extends ContractNetResponder {

        public GetWork(MessageTemplate mt) {
            super(worker, mt);

        }

        @Override
        protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
            ACLMessage reply = cfp.createReply();

            try {
                Order o = (Order) cfp.getContentObject();
                if(worker.getCapacity() < o.getQuantity() || worker.isFull()) {
                    reply.setPerformative(ACLMessage.REFUSE);

                } else {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContentObject(new WorkerOffer(worker));
                }
            } catch (UnreadableException | IOException e) {
                e.printStackTrace();
            }
            return reply;
        }


        @Override
        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {

            try {
                worker.addOrder((Order) cfp.getContentObject());

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
            super(worker, 500);
        }

        @Override
        protected void onTick() {
            if (currentOrder != null){
                currentOrder.decreaseQt(worker.getRate());

                if(currentOrder.getQuantity() <= 0){
                    ACLMessage l = new ACLMessage(ACLMessage.INFORM); // inform?
                    try {
                        l.setContentObject(currentOrder);
                        l.addReceiver(worker.getCompany());
                        worker.send(l);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    currentOrder = null;
                }else {
                    currentOrder = worker.getOrders().poll();

                }
            }
        }
    }
}

