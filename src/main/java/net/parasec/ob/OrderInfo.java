package net.parasec.ob;

// an Order is simply a order update/add message from the exchange -
// not to be confused with limit or market order.

public class OrderInfo {
    private final String src;
    
    private final String id; // my id
    private final int orderId; // set by exchange 

    private final long exchangeTimestamp;
    private final long localTimestamp;

    // in "satoshi" 1 satoshi = 0.00000001 btc.
    private long volume;    

    // in cents.
    private final int price;

    private final OrderType type;


    public OrderInfo(final String src, final String id, final int orderId, final long exchangeTimestamp,
		     final long localTimestamp, final long volume, final int price, final OrderType type) {
	this.src = src;
	this.id = id;
	this.orderId = orderId;
	this.exchangeTimestamp = exchangeTimestamp;
	this.localTimestamp = localTimestamp;
	this.volume = volume;
	this.price = price;
	this.type = type;
    }

    public OrderInfo setVolume(final long volume) {
	this.volume = volume;
	return this;
    }

    public String getSrc() {
	return src;
    }

    public String getId() {
	return id;
    }

    public int getOrderId() {
	return orderId;
    }

    public long getExchangeTimestamp() {
	return exchangeTimestamp;
    }

    public long getLocalTimestamp() {
	return localTimestamp;
    }

    public long getVolume() {
	return volume;
    }    

    public int getPrice() {
	return price;
    }

    public OrderType getType() {
	return type;
    }

    public String toString() {
	return type.name() + " " + Util.asBTC(volume) + " btc @ $" + Util.asUSD(price);
    }

}
