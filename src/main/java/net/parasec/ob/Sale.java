package net.parasec.ob;

import net.parasec.trading.ticker.core.wire.Direction;


public final class Sale {
    private final int price;
    private final long amount;
    private final Direction type;
    private final long ts;
    private final int takerId; // corresponding market order
    private final int makerId; // corresponding limit order

    public Sale(final int price, final long amount, final Direction type,
		final int takerId, final int makerId) {
	this.ts = System.currentTimeMillis();
	this.price = price;
	this.amount = amount;
	this.type = type;
	this.takerId=takerId;
	this.makerId=makerId;
    }

    public int getPrice() {
	return price;
    }

    public long getAmount() {
	return amount;
    }

    public Direction getType() {
	return type;
    }

    public long getTs() {
	return ts;
    }

    public int getTakerId() {
	return takerId;
    }

    public int getMakerId() {
	return makerId;
    }

    public StringBuilder toCsv(final String dl) {
       	return (new StringBuilder().append(ts).append(dl)
		.append(Util.asUSD(price)).append(dl)
		.append(Util.asBTC(amount)).append(dl)
		.append(type.equals(Direction.BUY) ? 1 : -1).append(dl)
		.append(takerId).append(dl)
		.append(makerId)
	);
    }

    public String toString() {
	final String nameLabel = type.equals(Direction.BUY) ? "BUY  " : "SELL ";
	return nameLabel + Util.asBTC(amount) + " @ $" + Util.asUSD(price) + " maker = " + makerId + " taker = " + takerId;
    }
}
