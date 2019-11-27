package net.parasec.ob;

import net.parasec.trading.ticker.core.wire.OrderInfo;


// limit order
public final class LimitOrder {

	private final OrderInfo order;

	private Limit parent;
	private LimitOrder leftSibling;
	private LimitOrder rightSibling;


	public LimitOrder(final OrderInfo order, final Limit parent, final LimitOrder leftSibling) {
		this.order = order;

		this.parent = parent;
		this.leftSibling = leftSibling;
	}

	public LimitOrder setParent(final Limit parent) {
		this.parent = parent;
		return this;
	}

	public LimitOrder setLeftSibling(final LimitOrder leftSibling) {
		this.leftSibling = leftSibling;
		return this;
	}

	public LimitOrder setRightSibling(final LimitOrder rightSibling) {
		this.rightSibling = rightSibling;
		return this;
	}

	public Limit getParent() {
		return parent;
	}

	public LimitOrder getLeftSibling() {
		return leftSibling;
	}

	public LimitOrder getRightSibling() {
		return rightSibling;
	}

	public OrderInfo getOrder() {
		return order;
	}

}
