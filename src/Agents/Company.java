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

import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Company extends Agent {

    private ConcurrentHashMap<AID, Integer> workers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<AID, Vector<Order>> ordersTask = new ConcurrentHashMap<>();
    private double cash = 0;
    private double lostCash = 0;
    private double payments = 0;
    private int[] rangeEmployees = {0, 0};
    private ConcurrentSkipListSet<Order> monthOrders = new ConcurrentSkipListSet<>();
    private CompanyBehaviours manager;
    private boolean addedWorker = false;

    @Override
    protected void setup() {

        Object[] args = getArguments();

        if (args != null && args.length == 3) {
            try {
                this.rangeEmployees[0] = Integer.parseInt((String) args[1]);
                this.rangeEmployees[1] = Integer.parseInt((String) args[2]);
            } catch (Exception e) {
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

            addBehaviour(manager.new PayEmployees(Utils.MONTH_IN_MILLISECONDS));

        } catch (FIPAException e) {

            System.out.println("Error Registering Company");
            // doDelete();
        }
    }


    public void addWorker(AID worker, int salary) {
        workers.put(worker, salary);
        ordersTask.put(worker, new Vector<>());
        addedWorker = false;
        payments += salary;
    }

    public AID removeOrder(Order order, boolean cancelled) {

        try {
            Set<AID> workers = ordersTask.keySet();

            for (AID worker : workers) {
                Vector<Order> orders = ordersTask.get(worker);

                for (Order o : orders) {

                    if (o.getAid().equals(order.getAid())) {

                        orders.remove(o);
                        if (cancelled) {
                            Utils.print("Order removed " + o.getAid().getLocalName());
                            Utils.print("Payment of  " + o.getPayment());
                            Utils.print("Was on worker " + worker.getLocalName());
                        }
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

    public double getLostCash() {
        return lostCash;
    }

    public boolean removeWorker(AID worker) {
        try {
            if (workers.size() > rangeEmployees[0]) {
                Integer salary =  workers.remove(worker);
                if (salary != null) {
                    Vector<Order> orders = this.ordersTask.get(worker);
                    if (orders.size() > 0) {
                        for (Order o : orders) {
                            addBehaviour(manager.new AssignWork(o, new ACLMessage(ACLMessage.CFP)));
                        }
                    }
                    Vector a = ordersTask.remove(worker);
                    payments -= salary;

                } else return false;
            } else return false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public ConcurrentHashMap<AID, Integer> getWorkers() {
        return workers;
    }

    public ConcurrentHashMap<AID, Vector<Order>> getOrdersTasked() {
        return ordersTask;
    }

    public double getCash() {
        return cash;
    }

    public double getPayments() {
        return payments;
    }

    public double payEmployees() {
        int pieces = 0;
        double earns = 0;

        for (Order o : monthOrders) {
            pieces += o.getQuantity();
            earns += o.getPayment();
        }
        double wastes = this.payments + Utils.PERCENTAGE_OF_WASTES * pieces;
        this.cash -= wastes;
        monthOrders.clear();
        return wastes;
    }

    public int[] getRangeEmployees() {
        return this.rangeEmployees;
    }

    public boolean isAddedWorker() {
        return addedWorker;
    }

    public void setAddedWorker(boolean addedWorker) {
        this.addedWorker = addedWorker;
    }


}
