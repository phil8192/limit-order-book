package net.parasec.ob;

import net.parasec.trading.bitstampws.BitstampClient;
import net.parasec.trading.bitstampws.BitstampMessageHandler;
import net.parasec.trading.bitstampws.Client;
import net.parasec.trading.bitstampws.OrderEvent;

import net.parasec.trading.ticker.core.wire.Direction;
import net.parasec.trading.ticker.core.wire.OrderInfo;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.*;

public final class OrderBookStream implements BitstampMessageHandler<OrderEvent> {

	private final OrderBook ob = new LinkedOrderBook();

	private String venue;
	private String symbol;


	public OrderBookStream(String venue, String symbol) {
		this.venue = venue;
		this.symbol = symbol;
	}

	public OrderBook getOb() {
		return ob;
	}

	private int getLimitPrice(double price, String symbol) {
		return Util.asCents(price);
	}

	private long getVolume(double volume, String symbol) {
		return Util.asSatoshi(volume);
	}

	@Override
	public void onMessage(OrderEvent orderEvent) {
		OrderEvent.Order order = orderEvent.order;
		Direction direction = order.type == 0 ? Direction.BUY : Direction.SELL;
		long localTs = System.currentTimeMillis();
		String exchangeOrderId = Long.toString(order.id);
		int limitPrice = getLimitPrice(order.price, symbol);
		long volume = getVolume(order.amount, symbol);
		long exchangeTimestamp = order.microTimestamp / 1000;

		OrderInfo orderInfo = new OrderInfo(exchangeOrderId, limitPrice, volume, exchangeTimestamp);
		net.parasec.trading.ticker.core.wire.OrderEvent.State state;

		switch (orderEvent.event) {
			case ORDER_CREATED:
				state = net.parasec.trading.ticker.core.wire.OrderEvent.State.CREATED;
				ob.addOrder(new net.parasec.trading.ticker.core.wire.OrderEvent(state, direction, symbol, venue, localTs, orderInfo));
				break;
			case ORDER_UPDATED:
				state = net.parasec.trading.ticker.core.wire.OrderEvent.State.UPDATED;
				ob.modOrder(new net.parasec.trading.ticker.core.wire.OrderEvent(state, direction, symbol, venue, localTs, orderInfo));
				break;
			case ORDER_DELETED:
				state = net.parasec.trading.ticker.core.wire.OrderEvent.State.DELETED;
				ob.delOrder(new net.parasec.trading.ticker.core.wire.OrderEvent(state, direction, symbol, venue, localTs, orderInfo));
		}
		//System.out.println(ansi().a(ob).reset());
		//System.out.println(ansi().eraseScreen(Erase.ALL).a(ob).reset());
		System.err.println(ob.getState().toCsv());
	}

	public static void main(final String[] args) throws Exception {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.ERROR);
		AnsiConsole.systemInstall();

		String symbol = args[0];
		OrderBookStream orderBookStream = new OrderBookStream("bitstamp", symbol);
		Client client = new BitstampClient();
		String subscriptionId = client.subscribeOrders(symbol, orderBookStream);
		try {
			OrderBook ob = orderBookStream.getOb();
			while (true) {
				Thread.sleep(1000);
				System.out.println(ansi().eraseScreen(Erase.ALL).a(ob).reset());
				//System.out.println(ansi().a(ob).reset());
			}
		} catch (InterruptedException e) {
		}
	}
}

