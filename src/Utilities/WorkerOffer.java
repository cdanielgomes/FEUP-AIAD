package Utilities;

import Agents.Worker;

import java.io.Serializable;

public class WorkerOffer implements Serializable {


    private float rate = 0;
    private int workingTime = 0;
    private int nOrders = 0;
    private boolean working = false;
    private boolean full = false;
    private int salary = 0;


    public WorkerOffer(Worker w) {
        this.rate = w.getRate();
        this.workingTime = w.getWorkingTime();
        this.working = w.getCurrentOrder() != null;
        this.full = w.isFull();
        this.nOrders = w.getOrders().size();
        this.salary = w.getSalary();
    }

    public float getRate() {
        return rate;
    }

    public int getWorkingTime() {
        return workingTime;
    }

    public boolean isWorking() {
        return working;
    }

    public boolean isFull() {
        return this.full;
    }

    public int getnOrders() {
        return nOrders;
    }

    public int getSalary() {
        return salary;
    }

    public void setnOrders(int nOrders) {
        this.nOrders = nOrders;
    }
}
