package Utilities;

import jade.core.AID;

import java.io.Serializable;

public class Order implements Serializable {


    private final AID aid;
    private final int quantity;
    private final int timeout;
    private final int payment;

    public Order(AID aid, int quantity, int timeout, int payment) {
        this.aid = aid;
        this.quantity = quantity;
        this.timeout = timeout;
        this.payment = payment;
    }

    public AID getAid() {
        return aid;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getPayment() {
        return payment;
    }
}
