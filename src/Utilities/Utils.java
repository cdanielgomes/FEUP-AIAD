package Utilities;

import Agents.Company;
import jade.core.AID;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;


public class Utils {
    // CONSTANTS AND ENUMS
    public enum TYPE_OF_WORKER {LAZY, NORMAL, RENDER}
    public enum TYPE_OF_CLIENT {PATIENT, NORMAL, NOPATIENT}
    public static int DAYS_IN_A_MONTH = 30;
    public static int DAY_IN_MILLISECONDS = 1000;
    public static int MONTH_IN_MILLISECONDS = DAY_IN_MILLISECONDS * DAYS_IN_A_MONTH;
    public static double PERCENTAGE_OF_WASTES = 0.07;
    public static double MEDIUM_PIECES_DAY = 45;
    public static int SALARY_OF_NORMAL_WORKER = 1500;
    public static int SALARY_OF_LAZY_WORKER = 1000;
    public static int SALARY_OF_RENDER_WORKER = 2500;
    public static int RATE_OF_NORMAL_WORKER = 43;
    public static int RATE_OF_LAZY_WORKER = 29;
    public static int RATE_OF_RENDER_WORKER = 77;

    public static void print(String msg) {
        System.out.println("Line Number: " + Thread.currentThread().getStackTrace()[2].getLineNumber() + "; File: " + Thread.currentThread().getStackTrace()[2].getFileName() + "; MSG: " + msg);
    }

    public static void printCompany(Company comp) {
        System.out.println("COMPANY: ");
        System.out.println("\tCash: " + comp.getCash());
        System.out.println("\tLost Cash: " + comp.getLostCash());
        System.out.println("\t Number of Workers is " + comp.getWorkers().size());
        System.out.println("\t Size of tasks is " + comp.getOrdersTasked().size());
        System.out.println("\t\t WORKERS");
        ConcurrentHashMap<AID, Vector<Order>> g = comp.getOrdersTasked();
        Set<AID> keys = g.keySet();
        for (AID key : keys) {
            System.out.println("Name: " + key.getLocalName() + "; " + g.get(key).size());
        }

    }


}
