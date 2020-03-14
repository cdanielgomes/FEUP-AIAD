package Behaviours;

import Agents.Company;
import Utilities.Order;
import Utilities.Utils;
import Utilities.WorkerOffer;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetInitiator;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jdk.jshell.execution.Util;

import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.function.BiConsumer;

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
                else {
                    System.out.println(msg.getContent());
                }
            } else {
                block();
            }
        }
    }


    public class WarnClients extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = company.receive(template);

            if (msg != null) {
                try {
                    Order l = (Order) msg.getContentObject();
                    ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
                    m.addReceiver(l.getAid());
                    company.removeOrder(l, false);
                    company.send(m);
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }

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
                            company.addBehaviour(new AssignWork(o, new ACLMessage(ACLMessage.CFP)));

                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                        break;
                    case ACLMessage.CONFIRM:
                        try {
                            Order o = (Order) msg.getContentObject();
                            company.receivePayment(o);

                            Utils.printCompany(company);

                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                        break;

                    case ACLMessage.CANCEL:
                        ACLMessage new_msg = new ACLMessage(ACLMessage.INFORM);
                        try {

                            Order o = (Order) msg.getContentObject();
                            company.lostPayment(o);
                            new_msg.setContent("Cancel Order");
                            new_msg.setContentObject(o);
                            new_msg.addReceiver(company.removeOrder(o, true));
                            company.send(new_msg);
                            return;

                        } catch (UnreadableException | IOException e) {
                            e.printStackTrace();
                        }
                        break;
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

        public AssignWork(Order order, ACLMessage cfp) {
            super(company, cfp);
            this.order = order;
        }

        @Override
        protected Vector prepareCfps(ACLMessage cfp) {

            for (AID k : company.getWorkers()) {
                cfp.addReceiver(k);
            }

            cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            cfp.setPerformative(ACLMessage.CFP);
            cfp.setReplyByDate(new Date(System.currentTimeMillis() + 7000));

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

                        if (w.isFull() || w.getCapacity() <= order.getQuantity()) continue;
                        if (workerOffer == null) {
                            offer = (ACLMessage) i;
                            workerOffer = w;
                            continue;
                        }
                        if ((!w.isWorking() && !workerOffer.isWorking()) || (w.isWorking() && workerOffer.isWorking())) {
                            if (workerOffer.getnOrders() > w.getnOrders()) {
                                offer = (ACLMessage) i;
                                workerOffer = w;
                            } else if (workerOffer.getWorkingTime() > w.getWorkingTime()) {
                                offer = (ACLMessage) i;
                                workerOffer = w;
                            }
                        } else if (workerOffer.isWorking() && !w.isWorking()) {
                            offer = (ACLMessage) i;
                            workerOffer = w;
                        }
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }

                }
            }

            if (offer != null) {
                try {
                    for (Object i : responses) {
                        ACLMessage rp = ((ACLMessage) i).createReply();

                        if (offer.equals(i)) {
                            rp.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            company.addOrder(offer.getSender(), order);
                            rp.setContentObject(order);
                        } else {
                            rp.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        }
                        acceptances.add(rp);

                    }
                } catch (IOException e) {
                    e.printStackTrace();

                }

            } else {
                Utils.print("Check added Worker");
                if (company.isAddedWorker()) {
                    Utils.print("Worker had to be added or waiting for one");
                    company.addBehaviour(new AssignWork(order, new ACLMessage(ACLMessage.CFP)));
                    company.removeBehaviour(this);
                } else {
                    Utils.print("Creating Worker");
                    createWorker(order);
                }
            }
        }
    }

    public class PayEmployees extends TickerBehaviour {

        double payment;

        public PayEmployees(long period, double payment) {
            super(company, period);
            this.payment = payment;
        }

        @Override
        protected void onTick() {
            company.payEmployees(company.getWorkers().size());

            double pay = company.getWorkers().size() * company.getPayment();

            if (company.getCash() < pay && company.getWorkers().size() > company.getRangeEmployees()[0]) {
                Utils.print(company.getCash() + " <- Cash");
                Utils.print(company.getWorkers().size() + " <- Number of Workers");
                Utils.print(company.getRangeEmployees()[0] + " <-  Minimum of Workers");
                Vector<AID> workers = company.getWorkers();
                Vector<Integer> sizes = new Vector<>();
                AID worker = workers.get(0);
                int nOrders = company.getOrdersTasked().get(worker).size();

                for (AID w : workers) {

                    int size = company.getOrdersTasked().get(w).size();

                    if (size < nOrders) {
                        worker = w;
                        nOrders = size;
                    }
                }

                if (company.removeWorker(worker)) {

                    ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);
                    msg.addReceiver(worker);
                    company.send(msg);
                    System.out.println("Removed Worker cause no money to him");
                }
            }
            Utils.print("Company configs: ");
            Utils.printCompany(company);

        }
    }


    public boolean createWorker(Order order) {
        try {
            company.setAddedWorker(true);

            // TODO important thing, keep guys optimal
/*
            Vector<AID> aids = company.getWorkers();

            for (AID w : aids){
                if(company.getOrdersTasked().get(w).size() == 0){
                    if (company.removeWorker(w)) {
                        ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);
                        msg.addReceiver(w);
                        company.send(msg);
                        System.out.println("Removed Worker cause is doing nothing");
                        break;
                    }
                }
            }*/



            int numberEmp = company.getWorkers().size();
            Utils.print(String.valueOf(numberEmp));

            if (company.getRangeEmployees()[1] > numberEmp) {

                System.out.println("TRY HIRE WORKER");
                Random rand = new Random();
                int rate = rand.nextInt(600 - 300) + 300;
                int cap = rand.nextInt(1000) + order.getQuantity();

                ContainerController cc = company.getContainerController();
                AgentController ac = cc.createNewAgent("worker" + (numberEmp + 1),
                        "Agents.Worker", new Object[]{cap + "", rate + ""});

                ac.start();
            }
            return true;
        } catch (Exception e) {

            // e.printStackTrace();
            return false;
        }
    }

}
