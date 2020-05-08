package Behaviours;

import Agents.Company;
import Utilities.Order;
import Utilities.Utils;
import Utilities.WorkerOffer;
import com.github.javafaker.Faker;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetInitiator;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class CompanyBehaviours {

    private Company company;

    public CompanyBehaviours(Agent company) {

        this.company = (Company) company;

    }


    public class ReceiveWorkers extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate performativeTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            MessageTemplate protocolTemplate = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            MessageTemplate template = MessageTemplate.and(MessageTemplate.not(protocolTemplate), performativeTemplate);
            ACLMessage msg = company.receive(template);

            if (msg != null) {
                try {
                    if (msg.getContent().contains("worker hired")) {

                        int salary = Integer.parseInt(msg.getContent().substring("worker hired".length()));
                        company.addWorker(msg.getSender(), salary);

                    } else {
                        Utils.print(msg.getContent());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
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
            MessageTemplate important = MessageTemplate.and(MessageTemplate.not(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET)), tmp);
            ACLMessage msg = company.receive(important);

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

        Order order;

        public AssignWork(Order order, ACLMessage cfp) {
            super(company, cfp);
            this.order = order;
        }

        @Override
        protected Vector prepareCfps(ACLMessage cfp) {

            for (Enumeration<AID> workers = company.getWorkers().keys(); workers.hasMoreElements(); ) {
                cfp.addReceiver(workers.nextElement());
            }

            cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            cfp.setPerformative(ACLMessage.CFP);
            cfp.setReplyByDate(new Date(System.currentTimeMillis() + 3000));

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
            ACLMessage offer = getWorkerAssigned(responses, order);

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

                System.out.println("/////////////////////////////");
                System.out.println("// Offer Type : " + order.getType_of_client() + "/////");
                System.out.println("/////////////////////////////");
                for (Map.Entry<AID, Vector<Order>> k : company.getOrdersTasked().entrySet()) {
                    System.out.println(k.getKey().getLocalName() + " has this number of orders: " + k.getValue().size());
                }

                if (company.isAddedWorker()) {
                    if (company.getLostClients().get(order.getAid()) != null) {
                        company.removeBehaviour(this);
                        return;
                    }
                    company.addBehaviour(new AssignWork(order, new ACLMessage(ACLMessage.CFP)));
                } else {
                    Utils.print("Creating Worker");
                    company.setSavedWork(order);
                    createWorker(order);
                }
                company.removeBehaviour(this);
            }
        }

        private ACLMessage getWorkerAssigned(Vector responses, Order order) {
            ACLMessage offer = null;
            WorkerOffer workerOffer = null;
            Integer timeToEndOffer = null;
            System.out.println("//////////////////////////////////////");
            System.out.println("///// Order Type: " + order.getType_of_client() + " ////////////");
            System.out.println("//////////////////////////////////////");
            System.out.println("Tempo da order é " + order.getTimeout());

            for (Object i : responses) {
                if (((ACLMessage) i).getPerformative() == ACLMessage.PROPOSE) {

                    try {
                        WorkerOffer w = (WorkerOffer) ((ACLMessage) i).getContentObject();

                        Integer time = calculateWaitingTime(((ACLMessage) i).getSender());

                        System.out.println("|||||||||| Worker |||||||||||||");
                        System.out.println("Para o worker " + ((ACLMessage) i).getSender().getLocalName() + " o tempo da order é " + time);

                        if (time < order.getTimeout()) {
                            if (offer == null) {
                                offer = (ACLMessage) i;
                                workerOffer = w;
                                timeToEndOffer = time;
                                System.out.println("Salary do " + ((ACLMessage) i).getSender().getLocalName() + " é de " + w.getSalary());

                                continue;
                            }

                            System.out.println("Salary do " + ((ACLMessage) i).getSender().getLocalName() + " é de " + w.getSalary());


                            switch (order.getType_of_client()) {
                                case PATIENT:
                                    if ((isLazy(workerOffer) && isLazy(w) && time < timeToEndOffer) ||
                                            (isNormal(workerOffer) && isNormal(w) && time < timeToEndOffer)) {
                                        offer = (ACLMessage) i;
                                        workerOffer = w;
                                        timeToEndOffer = time;
                                    } else if (!isLazy(workerOffer) && isLazy(w)) {
                                        offer = (ACLMessage) i;
                                        workerOffer = w;
                                        timeToEndOffer = time;
                                    } else if (!isLazy(workerOffer) && isNormal(w)) {
                                        offer = (ACLMessage) i;
                                        workerOffer = w;
                                        timeToEndOffer = time;
                                    }
                                    break;
                                case NORMAL:
                                    if ((isNormal(workerOffer) && isNormal(w) && time < timeToEndOffer)) {
                                        offer = (ACLMessage) i;
                                        workerOffer = w;
                                        timeToEndOffer = time;
                                    } else if (!isNormal(workerOffer) && isNormal(w)) {
                                        offer = (ACLMessage) i;
                                        workerOffer = w;
                                        timeToEndOffer = time;
                                    } else if (!isNormal(workerOffer) && isLazy(w)) {
                                        offer = (ACLMessage) i;
                                        workerOffer = w;
                                        timeToEndOffer = time;
                                    }
                                    break;
                                case NOPATIENT:
                                    if (isRender(workerOffer) && isRender(w) && time < timeToEndOffer) {
                                        offer = (ACLMessage) i;
                                        workerOffer = w;
                                        timeToEndOffer = time;
                                    } else if (!isRender(workerOffer) && isRender(w)) {
                                        offer = (ACLMessage) i;
                                        workerOffer = w;
                                        timeToEndOffer = time;
                                    } else if (!isRender(workerOffer) && isNormal(w)) {
                                        offer = (ACLMessage) i;
                                        workerOffer = w;
                                        timeToEndOffer = time;
                                    }
                                    break;
                            }

                        }

                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }

                }
            }
            System.out.println("///// FInd WHy /////");
            System.out.println((offer == null ? "COSPE NULL" : offer.getSender().getLocalName() + " com um salario = " + company.getWorkers().get(offer.getSender())));

            return offer;
        }

        boolean isLazy(WorkerOffer w) {
            return w.getSalary() == Utils.SALARY_OF_LAZY_WORKER;
        }


        boolean isRender(WorkerOffer w) {
            return w.getSalary() == Utils.SALARY_OF_RENDER_WORKER;
        }

        boolean isNormal(WorkerOffer w) {
            return w.getSalary() == Utils.SALARY_OF_NORMAL_WORKER;
        }

        int calculateWaitingTime(AID worker) {
            int sum = 0;

            Integer c = company.getWorkers().get(worker);
            int qt;
            if (c == Utils.SALARY_OF_LAZY_WORKER) qt = Utils.RATE_OF_LAZY_WORKER;
            else if (c == Utils.SALARY_OF_RENDER_WORKER) qt = Utils.RATE_OF_RENDER_WORKER;
            else qt = Utils.RATE_OF_NORMAL_WORKER;

            sum += order.getQuantity() * Utils.DAY_IN_MILLISECONDS / qt;

            for (Order order : company.getOrdersTasked().get(worker)) {

                sum += order.getQuantity() * Utils.DAY_IN_MILLISECONDS / qt;
            }
            return sum;
        }

    }

    public class PayEmployees extends TickerBehaviour {

        public PayEmployees(long period) {
            super(company, period);
        }

        @Override
        protected void onTick() {

            double pay = company.payEmployees();
            try {


                if (company.getCash() < pay && company.getWorkers().size() > company.getRangeEmployees()[0]) {
                    Utils.print(company.getCash() + " <- Cash");
                    Utils.print(company.getWorkers().size() + " <- Number of Workers");
                    Utils.print(company.getRangeEmployees()[0] + " <-  Minimum of Workers");
                    ConcurrentHashMap<AID, Integer> workers = company.getWorkers();


                    AID worker = null;
                    Integer nOrders = null;

                    for (Map.Entry<AID, Integer> w : workers.entrySet()) {
                        if (worker == null) {
                            worker = w.getKey();
                            nOrders = company.getOrdersTasked().get(worker).size();
                        }

                        int size = company.getOrdersTasked().get(w.getKey()).size();

                        if (size < nOrders) {
                            worker = w.getKey();
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public double checMoney(Order o) {
        int money = 0;
        for (Map.Entry<AID, Vector<Order>> k : company.getOrdersTasked().entrySet()) {
            for (Order order : k.getValue()) {
                money += order.getPayment();
            }
        }
        return money + o.getPayment();
    }

    public void createWorker(Order order) {
        try {
            company.setAddedWorker(true);
            Utils.TYPE_OF_WORKER workerType;
            int numberEmp = company.getWorkers().size();


            if (company.getRangeEmployees()[1] > numberEmp && checMoney(order) + company.getCash() > company.getPayments()) {

                ConcurrentSkipListSet<Order> orders = company.getMonthOrders();
                int numberOfPatient = 0;
                int numberOfNoPatient = 0;
                int numberOfNormal = 0;

                for (Order o : orders) {
                    if (isNoPatient(o)) numberOfNoPatient++;
                    else if (isPatient(o)) numberOfPatient++;
                    else numberOfNormal++;
                }


                double media = orders.size() < 0 ? (0 * numberOfPatient + 1 * numberOfNormal + 2 *
                        numberOfNoPatient) / orders.size() : 3;

                if (isPatient(order)) {
                    if (media > 0.7) workerType = Utils.TYPE_OF_WORKER.NORMAL;
                    else workerType = Utils.TYPE_OF_WORKER.LAZY;
                } else if (isNormal(order)) {
                    if (media < 0.7) workerType = Utils.TYPE_OF_WORKER.LAZY;
                    else if (media > 1.5) workerType = Utils.TYPE_OF_WORKER.RENDER;
                    else workerType = Utils.TYPE_OF_WORKER.NORMAL;
                } else {
                    if (media > 1.3) workerType = Utils.TYPE_OF_WORKER.RENDER;
                    else workerType = Utils.TYPE_OF_WORKER.NORMAL;
                }

                System.out.println("//////// Worker Creation ////////");
                System.out.println("//////// Worker type: " + workerType);
                System.out.println("//////// Order type: " + order.getType_of_client());

                ContainerController cc = company.getContainerController();
                AgentController ac = cc.createNewAgent("IM" + (new Faker()).name().name() + (numberEmp + 1),
                        "Agents.Worker", new Object[]{workerType});

                ac.start();

            }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public class SendSavedWork extends OneShotBehaviour {

        @Override
        public synchronized void action() {

            if (company.getSavedWork() != null) {
                try {

                    ACLMessage acl = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    acl.addReceiver(company.getSavedAID());
                    acl.setContentObject(company.getSavedWork());
                    company.addOrder(company.getSavedAID(), company.getSavedWork());
                    company.send(acl);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    boolean isPatient(Order o) {
        return o.getType_of_client() == Utils.TYPE_OF_CLIENT.PATIENT;
    }

    boolean isNoPatient(Order o) {
        return o.getType_of_client() == Utils.TYPE_OF_CLIENT.NOPATIENT;
    }

    boolean isNormal(Order o) {
        return o.getType_of_client() == Utils.TYPE_OF_CLIENT.NORMAL;
    }

}
