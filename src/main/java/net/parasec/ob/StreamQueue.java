package net.parasec.ob;

import com.pusher.client.*;
import com.pusher.client.channel.*;
import com.pusher.client.connection.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;


public final class StreamQueue {
    private static final Logger LOG = Logger.getLogger(StreamQueue.class);
    
    private final BlockingQueue<Event> q = new LinkedBlockingQueue<Event>();
    
    final static class Event {
	enum Type {
	    NEW,MOD,DEL
	};
	final Type t;
	final JSON.Order o;
	
	Event(final Type t, final JSON.Order o) {
	    this.t = t;
	    this.o = o;
	}
    }

    public StreamQueue(final StreamListener sl) {
	watch();
	consume(sl);
    }

    private void push(final Event e) {
	try {
	    q.put(e);
	} catch(final InterruptedException ie) {
	    Thread.currentThread().interrupt();
	    LOG.warn(ie,ie);
	}
    }

    public void watch() {

	final PusherOptions options = new PusherOptions().setEncrypted(false);
	final Pusher pusher = new Pusher("de504dc5763aeef9ff52", options);
	
	pusher.connect(new ConnectionEventListener() {
		public final void onConnectionStateChange(final ConnectionStateChange change) {
		    Thread.currentThread().setName("Stream");
		    LOG.info("connection state changed from: " + change.getPreviousState() + " to: " + change.getCurrentState());
		}
		public final void onError(final String message, final String code, final Exception e) {
		    LOG.error(e, e);
		}
	    }, ConnectionState.ALL);

	final Channel ordersChannel = pusher.subscribe("live_orders");
	ordersChannel.bind("order_created", new SubscriptionEventListener() {			    
		public final void onEvent(final String channel, final String event, final String data) {
		    System.err.println("order_created " + data);
		    push(new Event(Event.Type.NEW, JSON.parseOrder(data)));
		}
	    });

	ordersChannel.bind("order_changed", new SubscriptionEventListener() {	    
		public final void onEvent(final String channel, final String event, final String data) {
		    System.err.println("order_changed " + data);
		    push(new Event(Event.Type.MOD, JSON.parseOrder(data)));
		}
	    });

	ordersChannel.bind("order_deleted", new SubscriptionEventListener() {
		public final void onEvent(final String channel, final String event, final String data) {
		    System.err.println("order_deleted " + data);
		    push(new Event(Event.Type.DEL, JSON.parseOrder(data)));
		}
	    });
    }

    public void consume(final StreamListener sl) {
	final Thread t = Executors.defaultThreadFactory().newThread(new Runnable() {
		public void run() {
		    try {
			while(!Thread.currentThread().isInterrupted()) {
			    final Event e = q.take();
			    if(e.t.equals(Event.Type.NEW)) {
				sl.onNewOrder(e.o);
			    } else if(e.t.equals(Event.Type.MOD)) {
				sl.onModOrder(e.o);
			    } else {
				sl.onDelOrder(e.o);
			    }
			}
		    } catch(final InterruptedException ie) {
			LOG.warn(ie,ie);
			Thread.currentThread().interrupt();
		    }

		}

	    });
	t.setName("Raptor");
	t.start();
    }

}
