package net.parasec.ob;

import net.parasec.trading.ticker.core.dispatch.EventListener;
import net.parasec.trading.ticker.core.dispatch.EventQueue;
import net.parasec.trading.ticker.core.wire.OrderEvent;
import net.parasec.trading.ticker.core.Ticker;
import net.parasec.trading.ticker.bitstamp.BitstampTicker;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * old bitstamp pusher api.
 */
@Deprecated
public final class PusherOrderBookStream implements EventListener<OrderEvent> {

	public interface Evt {
		void onUpdate(OrderBook ob);
	}

	;

	private final OrderBook ob = new LinkedOrderBook();
	private final Evt evt;


	public PusherOrderBookStream(final Evt evt) {
		this.evt = evt;
	}

	public void onEvent(final OrderEvent oe) {
		switch (oe.getState()) {
			case CREATED:
				ob.addOrder(oe);
				break;
			case UPDATED:
				ob.modOrder(oe);
				break;
			case DELETED:
				ob.delOrder(oe);
				break;
		}
		evt.onUpdate(ob);
	}

	public static void main(final String[] args) throws Exception {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.ERROR);

		final Ticker t = new BitstampTicker();
		t.watchOrders(new EventQueue(new PusherOrderBookStream(new PusherOrderBookStream.Evt() {
			public void onUpdate(final OrderBook ob) {
				System.out.print("\u001b[2J\u001b[H");
				System.out.println(ob);
				System.err.println(ob.getState().toCsv());
			}
		})));
	}
}