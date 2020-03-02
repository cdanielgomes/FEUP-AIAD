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

        switch (type) {

            case 1:
                generateWorkersAndClientsLot();
                break;
            case 2:
                generateLotWorkersAndFewClients();
                break;
            case 3:
                generateFewWorkersAndLotClients();
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

        generateCompany();
        generateWorkers(3);
        generateClients(2);
    }


    public static void generateCompany() throws InterruptedException {

        Object[] compArgs = {1200 + "", "" + 2, "" + 10};

        // create company

        createAgent("company", "Company", compArgs);

        Thread.sleep(2000);

    }

    public static void generateWorkersAndClientsLot() throws InterruptedException {

        generateCompany();
        Thread.sleep(2000);

        generateWorkers(7);

        generateClients(25);

    }


    public static void generateLotWorkersAndFewClients() throws InterruptedException {

        generateCompany();
        Thread.sleep(2000);

        generateWorkers(10);

        generateClients(10);
    }


    public static void generateFewWorkersAndLotClients() throws InterruptedException {

        generateCompany();
        Thread.sleep(2000);

        generateWorkers(4);

        generateClients(15);
    }


    public static void generateWorkers(int maxWorkers) {
        Random rand = new Random();

        for (int i = 0; i < maxWorkers; i++) {

            int rate = rand.nextInt(200 - 99) + 99;
            int cap = rand.nextInt(8000 - 1000) + 1000;

            Object[] workArgs = {cap + "", rate + ""};

            createAgent("worker" + i, "Worker", workArgs);
        }
    }

    public static void generateClients(int clients) {
        Random rand = new Random();

        for (int i = 0; i < clients; i++) {

            int time = rand.nextInt(50000 - 25000) + 25000;
            int quantity = rand.nextInt(5000 - 500) + 500;
            double payment = 1.5 * quantity;

            Object[] args = {time + "", "" + quantity, payment + ""};

            createAgent("client" + i, "Client", args);
        }
    }

}
