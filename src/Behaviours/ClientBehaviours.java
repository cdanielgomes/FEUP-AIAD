package Behaviours;

import Agents.Client;
import Utilities.Logger;
import Utilities.Utils;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;

public class ClientBehaviours {


    private Client client;
    private TickerBehaviour timeBehaviour = null;

    public ClientBehaviours(Agent client) {
        this.client = (Client) client;
    }

    public class Request extends OneShotBehaviour {

        @Override
        public void action() {
            try {
                ACLMessage msg = new ACLMessage(ACLMessage.CFP);
                msg.addReceiver(client.getCompany());
                msg.setContentObject(client.getOrder());

                client.send(msg);
                client.addBehaviour(new WaitingTime());

                // log stuff
                client.setTime_waited(System.currentTimeMillis());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    class WaitingTime extends WakerBehaviour {

        public WaitingTime() {
            super(client, Math.round(client.getOrder().getTimeout() * 0.9));

        }

        @Override
        protected void onWake() {
            // super.onWake();
            long time = client.getOrder().getTimeout();

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent("im going away on " + (time - Math.round(time * 0.9)));
            msg.addReceiver(client.getCompany());
            client.send(msg);
            client.addBehaviour(new LastWait(time - Math.round(time * 0.9)));
        }
    }

    class LastWait extends WakerBehaviour {

        public LastWait(long time) {
            super(client, time);
        }

        @Override
        protected void onWake() {

            ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);
            msg.setContent("im going away");
            Utils.messagePrint("left order", client.getLocalName() + " left \n");
            try {
                msg.setContentObject(client.getOrder());
                msg.addReceiver(client.getCompany());
                client.send(msg); // send message that is canceling the order and leaves
                Logger.addClient(client.getOrder(),System.currentTimeMillis()- client.getTime_waited(), false); //

                client.doDelete();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    public class WaitOrder extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            MessageTemplate tmp = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM), template);
            ACLMessage receive = client.receive(tmp);

            if (receive != null) {


                if (receive.getPerformative() == ACLMessage.INFORM) {

                }

                ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
                msg.addReceiver(client.getCompany());
                msg.setContent("pay");
                try {
                    msg.setContentObject(client.getOrder());
                    client.send(msg);
                    Logger.addClient(client.getOrder(), System.currentTimeMillis()-client.getTime_waited(), true); //

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    client.doDelete();
                }

            } else {
                block();
            }

        }

    }



}
