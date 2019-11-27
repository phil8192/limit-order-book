package net.parasec.ob;

import net.parasec.trading.ticker.core.wire.OrderEvent;


public interface OrderBook {

	State getState();

	Orders getBids();

	Orders getAsks();

	void addOrder(OrderEvent oe);

	void modOrder(OrderEvent oe);

	void delOrder(OrderEvent oe);

	void setConsoleHeight(int height);
}

