package Agents;

import Utilities.Order;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.Hashtable;
import java.util.Vector;

public class Company  extends Agent {

    private Vector<AID> Workers = new Vector<>();
    private Hashtable<Order, Vector<AID>> OrdersTasked = new Hashtable<>();


    @Override
    protected void setup() {
        super.setup();

        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Company");
        sd.setName(getName());
        sd.setOwnership("FEUPCER");
        dfd.setName(getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this,dfd);

        } catch (FIPAException e) {

            doDelete();
        }
    }
}
