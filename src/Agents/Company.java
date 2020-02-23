package Agents;

import Behaviours.CompanyBehaviours;
import Utilities.Order;
import Utilities.Utils;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

public class Company extends Agent {

    private Vector<AID> workers = new Vector<>();
    private Hashtable<AID, Vector<Order>> ordersTask = new Hashtable<>();
    private double cash = 0;
    private double lostCash = 0;
    private double payments = 0;
    private double payment = 0;
    private int[] rangeEmployees = {0,0};
    private CompanyBehaviours manager;

    @Override
    protected void setup() {

        Object[] args = getArguments();
        System.out.println(args.length);
        if (args != null && args.length == 3) {
            try {
                this.payment = Double.parseDouble((String) args[0]); // work that it can handle
                this.rangeEmployees[0] = Integer.parseInt((String) args[1]);
                this.rangeEmployees[1] = Integer.parseInt((String) args[2]);
            }catch (Exception e){
                e.printStackTrace();
            }
        } else {
            doDelete();
            return;
        }
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Company");
        sd.setName(getName());
        sd.setOwnership("FEUPCER");
        dfd.setName(getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);

            manager = new CompanyBehaviours(this);

            addBehaviour(manager.new ReceiveRequests());

            addBehaviour(manager.new ReceiveWorkers());
            addBehaviour(manager.new WarnClients());


        } catch (FIPAException e) {

            System.out.println("LIGGGGGGGMMMMMMMMAAAAAAAAAAA");
            // doDelete();
        }
    }


    public void addWorker(AID worker) {
        workers.add(worker);
        ordersTask.put(worker, new Vector<>());
    }

    public AID removeOrder(Order order) {

        try {
            Set<AID> workers = ordersTask.keySet();

            for (AID worker : workers) {
                Vector<Order> orders = ordersTask.get(worker);

                for (Order o : orders) {
                    if (o.getAid().equals(order.getAid())) {
                        System.out.println("First " + orders.size());
                        orders.remove(o);
                        System.out.println("Second " + orders.size());
                        ordersTask.put(worker, orders);
                        return worker;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean addOrder(AID worker, Order order) {
        try {
            Vector<Order> o = ordersTask.get(worker);
            o.add(order);
            ordersTask.put(worker, o);

        } catch (NullPointerException e) {
            System.out.println("Something wrong passing args order and worker");
            return false;
        }
        return true;
    }

    public void receivePayment(Order o) {
        cash += o.getPayment();
    }

    public void lostPayment(Order o) {
        lostCash += o.getPayment();
    }

    public boolean removeWorker(AID worker) {
        try {
            if (workers.size() > rangeEmployees[0]) {
                System.out.println(workers.size());
                this.workers.remove(worker); /// TODO see if it works without matching
                Vector<Order> orders = this.ordersTask.get(worker);
                if (orders.size() > 0) {
                    for (Order o : orders) {
                        addBehaviour(manager.new AssignWork(o, new ACLMessage(ACLMessage.CFP)));
                    }

                    ordersTask.remove(worker);
                }
                System.out.println(workers.size());
            } else return false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public Vector<AID> getWorkers() {
        return workers;
    }

    public void setWorkers(Vector<AID> workers) {
        this.workers = workers;
    }

    public Hashtable<AID, Vector<Order>> getOrdersTasked() {
        return ordersTask;
    }

    public void setOrdersTasked(Hashtable<AID, Vector<Order>> ordersTasked) {
        this.ordersTask = ordersTasked;
    }

    public double getCash() {
        return cash;
    }

    public void setCash(int cash) {
        this.cash = cash;
    }

    public double getPayment(){
        return payment;
    }

    public void payEmployees(int nEmployees) {
        this.cash -= nEmployees * this.payment;
        this.payments += nEmployees * this.payment;
    }

    public int[] getRangeEmployees(){
        return this.rangeEmployees;
    }
}
