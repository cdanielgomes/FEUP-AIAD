package Utilities;


import jade.core.AID;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

public class Order implements Serializable, Comparator, Cloneable {


    private final AID aid;
    private int quantity;
    private final long timeout;
    private final double payment;
    private final Utils.TYPE_OF_CLIENT type_of_client;
    private boolean done = false;

    public Order(AID aid, int quantity, long timeout, double payment, Utils.TYPE_OF_CLIENT client) {
        this.aid = aid;
        this.quantity = quantity;
        this.timeout = timeout;
        this.payment = payment;
        this.type_of_client = client;
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

    public Utils.TYPE_OF_CLIENT getType_of_client() {
        return type_of_client;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    @Override
    public int compare(Object o, Object t1) {

        Order a = (Order) o;
        Order b = (Order) t1;

        return compare(a.getAid(),b.getAid());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return getQuantity() == order.getQuantity() &&
                getAid().equals(order.getAid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAid(), getQuantity());
    }

}

