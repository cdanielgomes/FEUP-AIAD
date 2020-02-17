package Utilities;

public class Utils {

    public static void print(String msg){
        System.out.println("Line Number: "+ Thread.currentThread().getStackTrace()[2].getLineNumber() + "; File: " + Thread.currentThread().getStackTrace()[2].getFileName() + "; MSG: " + msg);
    }
}
