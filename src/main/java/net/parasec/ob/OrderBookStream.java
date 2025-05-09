package net.parasec.ob;

import net.parasec.trading.bitstampws.BitstampClient;
import net.parasec.trading.bitstampws.BitstampMessageHandler;
import net.parasec.trading.bitstampws.Client;
import net.parasec.trading.bitstampws.OrderEvent;

import net.parasec.trading.ticker.core.wire.Direction;
import net.parasec.trading.ticker.core.wire.OrderInfo;
import org.fusesource.jansi.AnsiConsole;

import static org.fusesource.jansi.Ansi.*;

public final class OrderBookStream implements BitstampMessageHandler<OrderEvent> {

	private final OrderBook ob = new LinkedOrderBook();

	private String venue;
	private String symbol;
	private boolean real_time;

	public OrderBookStream(String venue, String symbol, boolean real_time) {
		this.venue = venue;
		this.symbol = symbol;
		this.real_time = real_time;
	}

	public OrderBook getOb() {
		return ob;
	}

	private int getLimitPrice(double price, String symbol) {
		if(symbol.equals("btcusd")) {
			return (int) price;
		}
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
		if (real_time) {
			AnsiConsole.system_out.println(ansi().a(ob).reset());
		}
		System.err.println(ob.getState().toCsv());
	}

	public static void main(final String[] args) throws Exception {
		AnsiConsole.systemInstall();

		int console_height = Integer.parseInt(args[0]);
		boolean real_time = Boolean.parseBoolean((args[1]));
		String symbol = args[2];
		OrderBookStream orderBookStream = new OrderBookStream("bitstamp", symbol, real_time);
		orderBookStream.getOb().setConsoleHeight(console_height);
		Client client = new BitstampClient();
		String subscriptionId = client.subscribeOrders(symbol, orderBookStream);

		// after 10 seconds, back-fill the lob using
		// https://www.bitstamp.net/api/v2/order_book/btcusd?group=2
		// .. not thread safe ..
		Thread.sleep(10000);
		long origEvent = orderBookStream.getOb().getState().event;

		HttpLob httpLob = new HttpLob();
		HttpLob.Lob backFillLob = httpLob.getLob(symbol);

		long ts = System.currentTimeMillis() - 86400000;
		for (HttpLob.Order ask : backFillLob.asks) {
			if (Double.parseDouble(ask.price) > 100000) {
				continue;
			}
			String exchangeOrderId = ask.orderId;
			int limitPrice = Util.asCents(Double.parseDouble(ask.price));
			long volume = Util.asSatoshi(Double.parseDouble(ask.volume));
			OrderInfo orderInfo = new OrderInfo(exchangeOrderId, limitPrice, volume, ts);
			orderBookStream.getOb().addOrder(new net.parasec.trading.ticker.core.wire.OrderEvent(net.parasec.trading.ticker.core.wire.OrderEvent.State.CREATED, Direction.SELL, symbol, "bitstamp", ts, orderInfo));
		}

		for (HttpLob.Order bid : backFillLob.bids) {
			String exchangeOrderId = bid.orderId;
			int limitPrice = Util.asCents(Double.parseDouble(bid.price));
			long volume = Util.asSatoshi(Double.parseDouble(bid.volume));
			OrderInfo orderInfo = new OrderInfo(exchangeOrderId, limitPrice, volume, ts);
			orderBookStream.getOb().addOrder(new net.parasec.trading.ticker.core.wire.OrderEvent(net.parasec.trading.ticker.core.wire.OrderEvent.State.CREATED, Direction.BUY, symbol, "bitstamp", ts, orderInfo));
		}
		orderBookStream.getOb().getState().event = origEvent;

		try {
			OrderBook ob = orderBookStream.getOb();
			while (true) {
				Thread.sleep(1000);
				if (!real_time) {
					AnsiConsole.system_out.println(ansi().eraseScreen(Erase.ALL).a(ob).reset());
				}
			}
		} catch (InterruptedException e) {
		}
	}
}
