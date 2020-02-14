package Agents;

import Behaviours.CompanyBehaviours;
import Utilities.Order;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.Hashtable;
import java.util.Vector;

public class Company extends Agent {

    private Vector<AID> workers = new Vector<>();
    private Hashtable<AID, Vector<Order>> ordersTask = new Hashtable<>();
    private double cash = 0;
    private CompanyBehaviours manager;

    @Override
    protected void setup() {

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
            System.out.println("Registered");

            manager = new CompanyBehaviours(this);

            addBehaviour(manager.new ReceiveRequests());

            addBehaviour(manager.new ReceiveWorkers());

        } catch (FIPAException e) {

            System.out.println("LIGGGGGGGMMMMMMMMAAAAAAAAAAA");
           // doDelete();
        }
    }


    public void addWorker(AID worker) {
        workers.add(worker);
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


    public void removeWorker(AID worker) {
        workers.remove(worker); /// TODO see if it works without matching
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
}
