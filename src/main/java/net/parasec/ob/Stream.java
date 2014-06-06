package net.parasec.ob;

import com.pusher.client.*;
import com.pusher.client.channel.*;
import com.pusher.client.connection.*;

import org.apache.log4j.Logger;

public final class Stream {
    private static final Logger LOG = Logger.getLogger(Stream.class);

    public Stream(final StreamListener sl) {
	watch(sl);
    }

    public void watch(final StreamListener sl) {
	LOG.info("Connecting to stream...");

	final PusherOptions options = new PusherOptions().setEncrypted(false);
	final Pusher pusher = new Pusher("de504dc5763aeef9ff52", options);
	
	pusher.connect(new ConnectionEventListener() {
		public final void onConnectionStateChange(ConnectionStateChange change) {
		    Thread.currentThread().setName("Stream");
		}
		public final void onError(String message, String code, Exception e) {
		    LOG.error(e, e);
		}
	    }, ConnectionState.ALL);
    
	final Channel ordersChannel = pusher.subscribe("live_orders");
	ordersChannel.bind("order_created", new SubscriptionEventListener() {
		public final void onEvent(String channel, String event, String data) {
		    System.err.println("order_created " + data);
		    sl.onNewOrder(JSON.parseOrder(data));
		}
	    });

	ordersChannel.bind("order_changed", new SubscriptionEventListener() {
		public final void onEvent(String channel, String event, String data) {
		    System.err.println("order_changed " + data);
		    sl.onModOrder(JSON.parseOrder(data));
		}
	    });

	ordersChannel.bind("order_deleted", new SubscriptionEventListener() {
		public final void onEvent(String channel, String event, String data) {
		    System.err.println("order_deleted " + data);
		    sl.onDelOrder(JSON.parseOrder(data));
		}
	    });

	final Channel tradesChannel = pusher.subscribe("live_trades");
	tradesChannel.bind("trade", new SubscriptionEventListener() {
		public final void onEvent(String channel, String event, String data) {
		    System.err.println("trade " + data);
		    sl.onTrade(JSON.parseTrade(data));
		}
	    });

	final Channel obChannel = pusher.subscribe("order_book");
	obChannel.bind("data", new SubscriptionEventListener() {
		public final void onEvent(String channel, String event, String data) {
		    sl.onOb(JSON.parseOb(data));
		}
	    });

    }

}
