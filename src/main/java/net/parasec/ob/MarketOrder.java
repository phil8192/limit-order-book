package net.parasec.ob;

public final class MarketOrder {
    private final OrderInfo order; 
    private final long initialVolume;
    private long filledVolume = 0;
    
    public MarketOrder(final OrderInfo order) {
	this.order = order;
	this.initialVolume = order.getVolume();
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

}
