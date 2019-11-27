package net.parasec.ob;

public final class Limit {
	private final int price;
	private long volume;
	private int orders;

	private Limit leftSibling;
	private Limit rightSibling;

	/* order queue */
	private LimitOrder head;
	private LimitOrder last;

	private long lastAdded;

	public Limit(final int price, final Limit leftSibling, final Limit rightSibling) {
		this.price = price;
		this.leftSibling = leftSibling;
		this.rightSibling = rightSibling;
	}

	public long getLastAdded() {
		return lastAdded;
	}

	public void setLastAdded(long lastAdded) {
		this.lastAdded = lastAdded;
	}

	public Limit setVolume(final long volume) {
		this.volume = volume;
		return this;
	}

	public Limit setOrders(final int orders) {
		this.orders = orders;
		return this;
	}

	public Limit setLeftSibling(final Limit leftSibling) {
		this.leftSibling = leftSibling;
		return this;
	}

	public Limit setRightSibling(final Limit rightSibling) {
		this.rightSibling = rightSibling;
		return this;
	}

	public Limit setHead(final LimitOrder head) {
		this.head = head;
		return this;
	}

	public Limit setLast(final LimitOrder last) {
		this.last = last;
		return this;
	}

	public int getPrice() {
		return price;
	}

	public long getVolume() {
		return volume;
	}

	public int getOrders() {
		return orders;
	}

	public Limit getLeftSibling() {
		return leftSibling;
	}

	public Limit getRightSibling() {
		return rightSibling;
	}

	public LimitOrder getHead() {
		return head;
	}

	public LimitOrder getLast() {
		return last;
	}

	public StringBuilder toCsv(final String dl) {
		return (new StringBuilder().append(Util.asUSD(price)).append(dl)
				.append(Util.asBTC(volume))
		);
	}
}
