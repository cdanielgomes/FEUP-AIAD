package Behaviours;

import Agents.Client;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SimpleAchieveREInitiator;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;

public class ClientBehaviours {


    private Client client;



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


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    class WaitingTime extends WakerBehaviour {

        public WaitingTime() {
            super(client, Math.round(client.getOrder().getTimeout()*0.9));

        }

        @Override
        protected void onWake() {
           // super.onWake();
            long time = client.getOrder().getTimeout();

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent("im going away on " + (time - Math.round(time*0.9)));
            msg.addReceiver(client.getCompany());
            client.send(msg);
            client.addBehaviour(new LastWait(time - Math.round(time*0.9)));
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
            msg.addReceiver(client.getCompany());
            client.send(msg);            // send message that is canceling the order and leaves
            client.doDelete();
        }
    }


    public class WaitOrder extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.INFORM);

            ACLMessage receive = client.receive(template);

            if (receive != null){

                if(receive.getContent() == "done") {
                    ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
                    msg.addReceiver(client.getCompany());
                    msg.setContent("pay");
                    client.send(msg);
                }

                client.doDelete();
            } else {
                block();
            }

        }

    }

   /* class Request extends SimpleAchieveREInitiator {

        public Request(Agent a, ACLMessage msg) {
            super(a, msg);
        }


        @Override
        protected ACLMessage prepareRequest(ACLMessage msg) {
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            msg.addReceiver(client.getCompany());
            try {
                msg.setContentObject(client.getOrder());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return super.prepareRequest(msg);
        }

        @Override
        protected void handleAgree(ACLMessage msg) {
            super.handleAgree(msg);
        }

        @Override
        protected void handleFailure(ACLMessage msg) {
            super.handleFailure(msg);
        }

        @Override
        protected void handleInform(ACLMessage msg) {
            super.handleInform(msg);
        }
    }*/
}
