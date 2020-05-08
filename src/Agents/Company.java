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
import org.apache.commons.lang3.SerializationUtils;

import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Company extends Agent {

    private ConcurrentHashMap<AID, Integer> workers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<AID, Vector<Order>> ordersTask = new ConcurrentHashMap<>();
    private ConcurrentHashMap<AID, Order> lostClients = new ConcurrentHashMap<>();

    private double cash = 0;
    private double lostCash = 0;
    private double payments = 0;
    private int[] rangeEmployees = {0, 0};
    private ConcurrentSkipListSet<Order> monthOrders = new ConcurrentSkipListSet<>();
    private CompanyBehaviours manager;
    private boolean addedWorker = false;
    private Order savedWork = null;
    private AID savedAID = null;
    private int currentDay = 0;


    @Override
    protected void setup() {

        Object[] args = getArguments();

        if (args != null && args.length == 2) {
            try {
                this.rangeEmployees[0] = Integer.parseInt((String) args[0]);
                this.rangeEmployees[1] = Integer.parseInt((String) args[1]);
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
            addBehaviour(manager.new  EndOfPeriodToSupportDebit());

        } catch (FIPAException e) {

            System.out.println("Error Registering Company");
            // doDelete();
        }
    }


    public void addWorker(AID worker, int salary) {
        workers.put(worker, salary);
        ordersTask.put(worker, new Vector<>());
        if (addedWorker) {
            savedAID = worker;
            addBehaviour(manager.new SendSavedWork());
            addedWorker = false;
        }
        payments += salary;
    }

    public AID removeOrder(Order order, boolean cancelled) {
        lostClients.put(order.getAid(),order);
        try {
            Set<AID> workers = ordersTask.keySet();

            for (AID worker : workers) {
                Vector<Order> orders = ordersTask.get(worker);

                for (Order o : orders) {

                    if (o.getAid().equals(order.getAid())) {

                        orders.remove(o);
                        if (cancelled) {
                            System.out.println("///// ORDER CANCELED /////");
                            System.out.println("Order removed " + o.getAid().getLocalName());
                            System.out.println("Payment of  " + o.getPayment());
                            System.out.println("Was on worker " + worker.getLocalName());
                            System.out.println("///// ---- ORDER CANCELED /////");

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
                Integer salary = workers.remove(worker);
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

    public ConcurrentSkipListSet<Order> getMonthOrders() {
        return monthOrders;
    }

    public Order getSavedWork() {
        return savedWork;
    }

    public void setSavedWork(Order savedWork) {
        this.savedWork = savedWork;
    }

    public AID getSavedAID() {
        return savedAID;
    }

    public ConcurrentHashMap<AID, Order> getLostClients() {
        return lostClients;
    }

    public double getPayments() {
        return payments;
    }

    public int getCurrentDay() {
            return currentDay;
    }

    public void updateDay(){
        this.currentDay++;
    }
    public void resetDay(){
        this.currentDay = 0;
    }
}
