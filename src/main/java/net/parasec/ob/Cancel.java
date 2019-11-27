package net.parasec.ob;

import net.parasec.trading.ticker.core.wire.Direction;


public final class Cancel {
	private String id;
	private Direction type;
	private long amount;

	public Cancel(final String id, final Direction type, final long amount) {
		this.id = id;
		this.type = type;
		this.amount = amount;
	}

	public String getId() {
		return id;
	}

	public Direction getType() {
		return type;
	}

	public long getAmount() {
		return amount;
	}
}

