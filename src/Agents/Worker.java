package Agents;

import Behaviours.WorkersBehaviours;
import Utilities.Order;
import Utilities.Utils;
import Utilities.Utils.*;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.concurrent.LinkedBlockingQueue;


public class Worker extends Agent {
    private int rate = 0;
    private int workingTime = 0;
    private int salary = 0;
    TYPE_OF_WORKER type_of_worker = TYPE_OF_WORKER.NORMAL;
    private Order currentOrder = null;
    private LinkedBlockingQueue<Order> orders = new LinkedBlockingQueue<>();
    private AID company = null;
    private WorkersBehaviours manager = new WorkersBehaviours(this);



    @Override
    protected void setup() {

        Object[] args = getArguments();
        if (args != null && args.length == 1) {

            TYPE_OF_WORKER worker_type = (TYPE_OF_WORKER) args[0];
            switch (worker_type) {
                case LAZY:
                    this.rate = Utils.RATE_OF_LAZY_WORKER;
                    this.salary = Utils.SALARY_OF_LAZY_WORKER;
                    this.type_of_worker = TYPE_OF_WORKER.LAZY;
                    break;
                case NORMAL:
                    this.rate = Utils.RATE_OF_NORMAL_WORKER;
                    this.salary = Utils.SALARY_OF_NORMAL_WORKER;
                    break;
                case RENDER:
                    this.rate = Utils.RATE_OF_RENDER_WORKER;
                    this.salary = Utils.SALARY_OF_RENDER_WORKER;
                    this.type_of_worker = TYPE_OF_WORKER.RENDER;
                    break;
                default:
                    break;
            }

        } else {
            doDelete();
            return;
        }

        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("worker");
        sd.setName(getName());
        sd.setOwnership("FEUPCER");

        dfd.setName(getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            // find Company
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription serviceTemplate = new ServiceDescription();
            serviceTemplate.setType("Company");
            template.addServices(serviceTemplate);
            this.company = DFService.search(this, template)[0].getName();

            addBehaviour(manager.new StartWork());

        } catch (FIPAException e) {
            System.out.println("Error on registering Worker " + getName());
            doDelete();
        }


    }


    @Override
    protected void takeDown() {

        try {
            DFService.deregister(this);
            System.out.println("    Shutting down Worker " + getName());
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }


    public void addOrder(Order o) {
        orders.add(o);
    }

    public boolean deleteOrder(Order o) {
        if (currentOrder != null) {
            if (o.getAid().equals(currentOrder.getAid())) {
                currentOrder = null;
                return true;
            }
        }

        return orders.remove(o);
    }

    public int getRate() {
        return rate;
    }

    public int getWorkingTime() {
        return workingTime;
    }

    public void increaseTime() {
        this.workingTime += 1;
    }

    public LinkedBlockingQueue<Order> getOrders() {
        return orders;
    }

    public AID getCompany() {
        return company;
    }

    public int getSalary() {
        return salary;
    }


    public boolean isFull() {
        return orders.size() == 3;
    }

    public Order getCurrentOrder() {
        return currentOrder;
    }

    public void setCurrentOrder(Order currentOrder) {
        this.currentOrder = currentOrder;
    }
}
