import Utilities.Utils;
import com.github.javafaker.Faker;
import jade.Boot;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.tools.sniffer.Sniffer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Random;
public class Launcher {


    private static jade.core.Runtime runtime;
    private static Profile profile;
    private static ContainerController mainContainer;
    static int iClients = 1;
    private static double wastes = 0.07;
    static int nWorker = 4;
    static double price = 2;
    public static void main(String[] args) throws InterruptedException, StaleProxyException {


        int type = Integer.parseInt(args[0]);

        createJade();

        switch (type) {

            case 1:
                generateWorkersAndClientsLot();
                break;
            case 2:
                nWorker=7;
                generateLotWorkersAndFewClients();
                break;
            case 3:
                nWorker = 2;
                generateFewWorkersAndLotClients();
                break;
            case 4:
                iClients = 0;
                generateFewWorkersAndLotClients();
                createClientsRandomly(27, 25);
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
    //    profile.setParameter(Profile.GUI, "true");
        mainContainer = runtime.createMainContainer(profile);

        //AgentController sniffer = mainContainer.createNewAgent("sniffer_name", "jade.tools.sniffer.Sniffer", new Object[]{"company;Wo*;Client*"});
        //sniffer.start();

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

        generateCompany();
        generateWorkers();
        generateClients(2);
    }


    public static void generateCompany() throws InterruptedException {

        Object[] compArgs = {"" + 1, wastes+"", price +""};

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
        generateWorkers();

        generateClients(25);

    }


    /**
     * Generate 10 workers and 10 Clients
     * Workers will get fired
     *
     * @throws InterruptedException
     */
    public static void generateLotWorkersAndFewClients() throws InterruptedException {

        generateCompany();


        generateWorkers();

        generateClients(10);
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

        generateWorkers();

        generateClients(25);
    }

    /**
     * Creates between 10 and 30 new Clients each x seconds after 20 seconds program's start
     *
     */
    public static void createClientsRandomly(int nMax, int starter) {
        (new Timer()).schedule(new TimerTask() {
           @Override
           public void run() {
               Random n = new Random();
               int clients = n.nextInt(nMax - 10) + 10;
               generateClients(clients);
               iClients = clients + 1;
           }
       }, 20*1000,33*1000);
    }


    public static void generateWorkers() {
        Random rand = new Random();
        Faker f = new Faker();
        for (int i = 0; i < nWorker; i++) {

            int type = rand.nextInt(3);

            Object[] workArgs = {Utils.TYPE_OF_WORKER.values()[type]};

            createAgent("Worker " + f.name().fullName() + i, "Worker", workArgs);
        }
    }

    public static void generateClients(int clients) {
        Random rand = new Random();
        Faker f = new Faker();

        for (int i = 0; i < clients; i++) {
            int type = rand.nextInt(3);
            int quantity = rand.nextInt(2000 - 500) + 500;

            Object[] args = {Utils.TYPE_OF_CLIENT.values()[type], quantity};

            createAgent("Client " + f.name().fullName() + i, "Client", args);
        }
    }

}
