import jade.Boot;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.tools.sniffer.Sniffer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.Random;

public class Launcher {


    private static jade.core.Runtime runtime;
    private static Profile profile;
    private static ContainerController mainContainer;


    public static void main(String[] args) throws InterruptedException, StaleProxyException {


        int type = Integer.parseInt(args[0]);
        createJade();

        switch (type){

            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                generateAgents();
                break;
        }


    }


    public static void createJade() throws StaleProxyException {

        //Get the JADE runtime interface (singleton)
        runtime = jade.core.Runtime.instance();

        //Create a Profile, where the launch arguments are stored
        profile = new ProfileImpl();
        profile.setParameter(Profile.CONTAINER_NAME, "TestContainer");
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.GUI, "true");
        mainContainer = runtime.createMainContainer(profile);

        AgentController sniffer = mainContainer.createNewAgent("sniffer_name", "jade.tools.sniffer.Sniffer", new Object[]{"company;worker*;client*"});
        sniffer.start();

        // createAgent("runner", "RunnerAgent", new Object[]{});

    }

    public static void createAgent(String agentNick, String agentName, Object[] agentArguments) {

        try {
            AgentController ac = mainContainer.createNewAgent(agentNick, "Agents." + agentName, agentArguments);

            ac.start();

        } catch (jade.wrapper.StaleProxyException e) {
            System.err.println("Error launching agent...");
        }
    }

    public static void generateAgents() throws InterruptedException {

        Random rand = new Random();
        Object[] compArgs = {1200 + "", "" + 2, "" + 10};

        // create company

        createAgent("company", "Company", compArgs);

        Thread.sleep(2000);

        for (int i = 0; i < 3; i++) {

            int rate = rand.nextInt(200 - 99) + 99;
            int cap = rand.nextInt(8000 - 1000) + 1000;

            Object[] workArgs = {cap + "", rate + ""};

            createAgent("worker" + i, "Worker", workArgs);


        }

        // create Clients

        for (int i = 0; i < 2; i++) {

            int time = rand.nextInt(30000 - 10000) + 10000;
            int quantity = rand.nextInt(5000 - 500) + 500;
            double payment = 1.5 * quantity;

            Object[] args = {time + "", "" + quantity, payment + ""};

            createAgent("client" + i, "Client", args);

        }
    }


}
