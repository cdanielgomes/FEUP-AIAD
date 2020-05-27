package Utilities;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Logger {
    String CSV_FILE_NAME = "data.csv";

    enum HEADERS {
        Tempo_Decorrido_em_Dias,
        Indice_de_Desperdicio,
        Preço_unidade,
        Numero_Incial_de_Lazies,
        Numero_Inicial_de_Normals,
        Numero_Incial_de_Renders,
        Tipo_de_Cliente,
        Tempo_Maximo_de_Espera,
        Tempo_de_Espera,
        Quantidade_Encomendada,
        Recebida
    }

    public static boolean endInitialWorker = false;

    public static int nRender = 0;
    public static int nNormal = 0;
    public static int nLazy = 0;
    public static long timeElapsed = 0;
    public static ConcurrentLinkedQueue<ArrayList<String>> queue = new ConcurrentLinkedQueue<>();


    public static void initiateTimer() {

        try {

            timeElapsed = System.currentTimeMillis();
            FileWriter out = new FileWriter("data.csv", true);
            CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT);

            (new Timer()).schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        try {

                            while (!queue.isEmpty()) {
                                ArrayList<String> mt = queue.poll();

                                printer.printRecord(mt);
                            }
                            printer.flush();
                            //  printer.close();
                            //out.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static void addWorker(int salary) {
        if (!endInitialWorker) {
            if (salary == Utils.SALARY_OF_RENDER_WORKER) nRender++;
            else if (salary == Utils.SALARY_OF_LAZY_WORKER) nLazy++;
            else nNormal++;
            // initialWorkers.add(salary);
        }
    }

    public static void addClient(Order o, long time, boolean delivered) {

        try {

            ArrayList<String> tmp = new ArrayList<String>();
            tmp.add((double) ((System.currentTimeMillis() - timeElapsed) / Utils.DAY_IN_MILLISECONDS) + "");
            tmp.add(Utils.PERCENTAGE_OF_WASTES + "");
            tmp.add(Utils.PRICE_UNIT + "");
            tmp.add(nLazy + "");
            tmp.add(nNormal + "");
            tmp.add(nRender + "");
            tmp.add(o.getType_of_client() + "");
            tmp.add(o.getTimeout() + "");
            tmp.add(time + "");
            tmp.add(o.getQuantity() + "");
            tmp.add(delivered ? "sim" : "nao");
            queue.add(tmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
