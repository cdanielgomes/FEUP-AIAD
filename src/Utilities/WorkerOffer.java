package Utilities;

import Agents.Worker;
import java.io.Serializable;

public class WorkerOffer implements Serializable {


    private int capacity = 0;
    private float rate = 0;
    private int workingTime = 0;
    private int nOrders = 0;
    private boolean working = false;
    private boolean full = false;


    public WorkerOffer(Worker w) {
        this.capacity = w.getCapacity();
        this.rate = w.getRate();
        this.workingTime = w.getWorkingTime();
        this.working = w.getOrders().size() > 0;
        this.full = w.isFull();
        this.nOrders = w.getOrders().size();
    }

    public int getCapacity() {
        return capacity;
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

    public void setnOrders(int nOrders) {
        this.nOrders = nOrders;
    }
}
