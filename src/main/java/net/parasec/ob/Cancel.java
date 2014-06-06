package net.parasec.ob;

public final class Cancel {
    private String id;
    private OrderType type;
    private long amount;

    public Cancel(final String id, final OrderType type, final long amount) {
	this.id = id;
	this.type = type;
	this.amount = amount;
    }

    public String getId() {
	return id;
    }

    public OrderType getType() {
	return type;
    }

    public long getAmount() {
	return amount;
    }
}

