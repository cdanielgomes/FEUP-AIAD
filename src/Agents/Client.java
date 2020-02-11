package Agents;

import Behaviours.ClientBehaviours;
import Utilities.Order;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.awt.*;

public class Client extends Agent {


    int timeout;
    Order order;
    AID company;
    ClientBehaviours manager;

    @Override
    protected void setup() {

        Object[] args = getArguments();
        if (args != null && args.length == 3) {

            this.timeout = Integer.parseInt((String) args[0]);
            int quantity = Integer.parseInt((String) args[1]);
            int payment = Integer.parseInt((String) args[2]);

            this.order = new Order(getAID(), quantity, timeout, payment);
        }
        else {

            System.out.println("Fail Loading Client " + getName() );
            return;
        }

        manager = new ClientBehaviours(this);

        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Client");
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


            addBehaviour(manager.new Request());
            addBehaviour(manager.new WaitOrder());
        } catch (FIPAException e) {
            System.out.println("Error on registering Client " + getName());
            doDelete();
        }

    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println("Client " + getName() + " is being deleted");
    }

    public Order getOrder() {
        return order;
    }

    public AID getCompany() {
        return company;
    }
}

