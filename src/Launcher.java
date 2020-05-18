import Utilities.Utils;
import com.github.javafaker.Faker;
import jade.Boot;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.tools.sniffer.Sniffer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Launcher {


    private static jade.core.Runtime runtime;
    private static Profile profile;
    private static ContainerController mainContainer;
    static int iClients = 1;

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
            case 4:
                iClients = 0;
                generateFewWorkersAndLotClients();
                createClientsRandomly(15, 30, 25, 34);
                break;

            case 5:
                iClients = 0;
                generateFewWorkersAndLotClients();
                createClientsRandomly(25 , 40, 25, 1);
                break;

            case 6:
                // create workers with low rate or capacity
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

        AgentController sniffer = mainContainer.createNewAgent("sniffer_name", "jade.tools.sniffer.Sniffer", new Object[]{"company;IM*;client*"});
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
        generateClients(2,0, 1.5);
    }


    public static void generateCompany() throws InterruptedException {

        Object[] compArgs = {"" + 2, "" + 10};

        // create company

        createAgent("company", "Company", compArgs);

      //  Thread.sleep(1000);

    }

    /**
     * Generates 7 workers and 25 clients
     * Enough clients to workers handle
     *
     * @throws InterruptedException
     */

    public static void generateWorkersAndClientsLot() throws InterruptedException {

        generateCompany();
        generateWorkers(7);

        generateClients(25, 0, 1.5);

    }


    /**
     * Generate 10 workers and 10 Clients
     * Workers will get fired
     *
     * @throws InterruptedException
     */
    public static void generateLotWorkersAndFewClients() throws InterruptedException {

        generateCompany();


        generateWorkers(10);

        generateClients(10, 0, 1.5);
    }


    /**
     * Creates 4 workers and 25 clients
     * <p>
     * Will be created more workers by the program
     *
     * @throws InterruptedException
     */
    public static void generateFewWorkersAndLotClients() throws InterruptedException {

        generateCompany();
        generateWorkers(4);

        generateClients(25,0, 1.5);
    }

    /**
     * Creates between 10 and 30 new Clients each x seconds after 20 seconds program's start
     *
     * @param seconds
     */
    public static void createClientsRandomly(int nMax, int seconds, int starter, double priceRate) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
                    Random n = new Random();
                    int clients = n.nextInt(nMax - 10) + 10;
                    generateClients(clients, starter+iClients, priceRate);
                    iClients = clients;
                }, 15, seconds, TimeUnit.SECONDS
        );

    }


    public static void generateWorkers(int maxWorkers) {
        Random rand = new Random();
        Faker f = new Faker();
        for (int i = 0; i < maxWorkers; i++) {

            int type = rand.nextInt(3);

            Object[] workArgs = {Utils.TYPE_OF_WORKER.values()[type]};

            createAgent(  "IM"+f.name().fullName() + i, "Worker", workArgs);
        }
    }

    public static void generateClients(int clients, int starter, double priceRate) {
        Random rand = new Random();

        for (int i = starter; i < clients + starter; i++) {

            int type = rand.nextInt(3) ;
            int quantity = rand.nextInt(1000 - 100) + 100;

            Object[] args = {Utils.TYPE_OF_CLIENT.values()[type],quantity};

            createAgent("client" + i, "Client", args);
        }
    }

}
