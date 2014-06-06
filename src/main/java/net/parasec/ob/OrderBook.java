package net.parasec.ob;

public interface OrderBook extends OMS {

    State getState();
    Orders getBids();
    Orders getAsks();

}


