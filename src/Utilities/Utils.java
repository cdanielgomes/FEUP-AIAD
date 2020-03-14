package Utilities;

import Agents.Company;
import jade.core.AID;

import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

public class Utils {

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
        Hashtable<AID, Vector<Order>> g = comp.getOrdersTasked();
        Set<AID> keys = g.keySet();
        for (AID key : keys){
            System.out.println("AID: " + key.getName() + "; " + g.get(key).size());
        }

    }
}
