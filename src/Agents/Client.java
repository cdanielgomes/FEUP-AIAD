package Agents;

import Behaviours.ClientBehaviours;
import Utilities.Order;
import Utilities.Utils;
import Utilities.Utils.*;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import static Utilities.Utils.DAY_IN_MILLISECONDS;
import static Utilities.Utils.MEDIUM_PIECES_DAY;

public class Client extends Agent {

    Order order;
    AID company;
    ClientBehaviours manager;
    long time_waited = 0;
    TYPE_OF_CLIENT client = TYPE_OF_CLIENT.NORMAL;

    @Override
    protected void setup() {

        Object[] args = getArguments();
        if (args != null && args.length == 2) {
            client = (TYPE_OF_CLIENT) args[0];
            int quantity = (Integer) args[1];
            this.order = calculateWaitingTime(quantity);
        } else {

            System.out.println("Fail Loading Client " + getLocalName());
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
            this.company = DFService.search(this, template)[0].getName();


            addBehaviour(manager.new Request());
            addBehaviour(manager.new WaitOrder());
        } catch (FIPAException e) {
            System.out.println("Error on registering Client " + getLocalName());
            doDelete();
        }

    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    public Order getOrder() {
        return order;
    }

    public AID getCompany() {
        return company;
    }

    private Order calculateWaitingTime(int quantity) {

        int timeout = 0;
        double payment = Utils.PRICE_UNIT * quantity;

        switch (client) {
            case NOPATIENT:
                timeout = (int) Math.ceil(1.7 * quantity / MEDIUM_PIECES_DAY);
                break;
            case PATIENT:
                timeout = (int) Math.ceil(3.3 * quantity / MEDIUM_PIECES_DAY);
                break;
            case NORMAL:
                timeout = (int) Math.ceil(2.3 * quantity / MEDIUM_PIECES_DAY);
                break;
            default:
                break;
        }

        timeout *=  DAY_IN_MILLISECONDS;
        return new Order(getAID(), quantity, timeout, payment, client);
    }

    public void setTime_waited(long time_waited) {
        this.time_waited = time_waited;
    }

    public long getTime_waited() {
        return time_waited;
    }
}

