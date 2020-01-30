package Agents;

import Behaviours.WorkersBehaviours;
import Utilities.Order;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.Vector;

public class Worker extends Agent {

    private int capacity;
    private float rate;
    private int workingTime = 0;
    private int capacityUsed = 0;
    private Vector<Order> Orders = new Vector<>();

    private WorkersBehaviours manager = new WorkersBehaviours(this);

    @Override
    protected void setup() {
        super.setup();

        Object[] args = getArguments();
        if (args != null && args.length == 3) {
            this.capacity = (int) args[0];
            this.rate = (int) args[1];
        }
        else {

        }

        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Worker");
        sd.setName(getName());
        sd.setOwnership("FEUPCER");
        dfd.setName(getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);

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
}

