package Agents;

import Utilities.Order;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class Client extends Agent {


    int timeout;
    Order order;


    @Override
    protected void setup() {
        super.setup();


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

        } catch (FIPAException e) {
            System.out.println("Error on registering Client " + getName());
            doDelete();
        }
    }
}

