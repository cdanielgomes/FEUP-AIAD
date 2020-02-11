package Agents;

import Behaviours.WorkersBehaviours;
import Utilities.Order;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.concurrent.LinkedBlockingQueue;

public class Worker extends Agent {

    private int capacity = 0;
    private int rate = 0;
    private int workingTime = 0;
    private int capacityUsed = 0;
    private LinkedBlockingQueue<Order> orders =  new LinkedBlockingQueue<>();
    private AID company = null;
    private WorkersBehaviours manager = new WorkersBehaviours(this);


    @Override
    protected void setup() {

        Object[] args = getArguments();
        if (args != null && args.length == 3) {
            this.capacity = (int) args[0]; // work that it can handle
            this.rate = (int) args[1]; // work rate - work per time unit

        }
        else {
            doDelete();
            return ;
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
            this.company = DFService.search(this, template)[1].getName();

            addBehaviour(manager.new StartWork());

        } catch (FIPAException e) {
            System.out.println("Error on registering Worker " + getName());
            doDelete();
        }



    }


    @Override
    protected void takeDown() {
        super.takeDown();
        try {
            DFService.deregister(this);
            System.out.println("Shutting down Worker " + getName());
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public void addOrder(Order o){
        orders.add(o);
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public int getWorkingTime() {
        return workingTime;
    }

    public void setWorkingTime(int workingTime) {
        this.workingTime = workingTime;
    }

    public int getCapacityUsed() {
        return capacityUsed;
    }

    public void setCapacityUsed(int capacityUsed) {
        this.capacityUsed = capacityUsed;
    }

    public LinkedBlockingQueue<Order> getOrders() {
        return orders;
    }

    public void setOrders(LinkedBlockingQueue<Order> orders) {
        this.orders = orders;
    }

    public AID getCompany() {
        return company;
    }

    public void setCompany(AID company) {
        this.company = company;
    }

    public boolean isFull() {
        return orders.size() == 3;
    }
}

