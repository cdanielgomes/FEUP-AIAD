package Utilities;

import jade.core.AID;

import java.io.Serializable;

public class Order implements Serializable {


    private final AID aid;
    private int quantity;
    private final long timeout;
    private final double payment;
    private boolean done = false;

    public Order(AID aid, int quantity, int timeout, double payment) {
        this.aid = aid;
        this.quantity = quantity;
        this.timeout = timeout;
        this.payment = payment;
    }

    public void decreaseQt(int quantity){
            this.quantity-= quantity;
    }

    public AID getAid() {
        return aid;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getTimeout() {
        return timeout;
    }

    public double getPayment() {
        return payment;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}

