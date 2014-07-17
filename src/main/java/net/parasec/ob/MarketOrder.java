package net.parasec.ob;

import net.parasec.trading.ticker.core.wire.OrderInfo;
import net.parasec.trading.ticker.core.wire.Direction;


public final class MarketOrder {
    private final OrderInfo order; 
    private final long initialVolume;
    private long filledVolume = 0;
    private final Direction direction;    

    public MarketOrder(final OrderInfo order, final Direction direction) {
	this.order = order;
	this.initialVolume = order.getVolume();
	this.direction = direction;
    }

    public OrderInfo getOrder() {
	return order;
    }

    public void setFilledVolume(final long filledVolume) {
	this.filledVolume = filledVolume;
    }

    public long getInitialVolume() {
	return initialVolume;
    }

    public long getFilledVolume() {
	return filledVolume;
    }

    public Direction getDirection() {
	return direction;
    }
}

