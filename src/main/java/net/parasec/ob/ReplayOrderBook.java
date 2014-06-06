package net.parasec.ob;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

public final class ReplayOrderBook {

    public static void main(final String[] args) {

	BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);

	final ReplayStream replayStream = new ReplayStream();
	final String log = args[0];
	final long delay = Long.parseLong(args[1]);
	final boolean printBook = Boolean.parseBoolean(args[2]);
	
	final StreamListener listener;
	if(printBook)
	    listener = new OrderBookStream(new OrderBookStream.Evt() {
		    public void onUpdate(final OrderBook ob) {
			System.out.print("\u001b[2J\u001b[H");
			System.out.println(ob);
			System.err.println(ob.getState().toCsv());
		    }
		});
	else
	    listener = new OrderBookStream(new OrderBookStream.Evt() {
		    public void onUpdate(final OrderBook ob) {
			System.err.println(ob.getState().toCsv());
		    }
		});

	replayStream.watch(log,listener,delay);

    }

}
