package net.parasec.ob;

import org.apache.log4j.Logger;
import java.io.BufferedReader;
import java.io.FileReader;

public final class ReplayStream {
    private static final Logger LOG = Logger.getLogger(ReplayStream.class);

    private interface DelayFun {
	void delay() throws InterruptedException;
    };

    public void watch(final String log, final StreamListener sl, final long delay) {
	
	final DelayFun delayFun;
	if(delay>0) {
	    delayFun = (new DelayFun() {
		    public void delay() throws InterruptedException {
			Thread.sleep(delay);
		    }
		});
	} else {
	    delayFun = (new DelayFun() {
		    public void delay() throws InterruptedException {
		    }
		});
	}

	try {

	    final BufferedReader in = new BufferedReader(new FileReader(log));

	    try {

		String data;
		while((data = in.readLine()) != null) {

		    if(data.contains("order_created"))
			sl.onNewOrder(JSON.parseOrder(getDictionary(data)));

		    else if(data.contains("order_changed"))
			sl.onModOrder(JSON.parseOrder(getDictionary(data)));
	
		    else if(data.contains("order_deleted"))
			sl.onDelOrder(JSON.parseOrder(getDictionary(data)));
	
		    else {
			LOG.warn("unknown command: " + data);
		    }
		    
		    delayFun.delay();
		}

	    } catch(final InterruptedException e) {
		LOG.error(e,e);
		Thread.currentThread().interrupt();
	    } finally {
		in.close();
	    }

	} catch(final Exception e) {
	    LOG.error(e,e);
	}    

    }

    private String getDictionary(final String data) {
	return data.substring(data.indexOf("{"),data.length());
    }

}
