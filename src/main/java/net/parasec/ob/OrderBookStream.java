package net.parasec.ob;

import net.parasec.trading.ticker.core.dispatch.EventListener;
import net.parasec.trading.ticker.core.dispatch.EventQueue;
import net.parasec.trading.ticker.core.wire.OrderEvent;
import net.parasec.trading.ticker.core.Ticker;
import net.parasec.trading.ticker.bitstamp.BitstampTicker;


public final class OrderBookStream implements EventListener<OrderEvent> { 

  public interface Evt {
    void onUpdate(OrderBook ob);
  };

  private final OrderBook ob = new LinkedOrderBook();
  private final Evt evt;

    
  public OrderBookStream(final Evt evt) {
    this.evt = evt;
  }

  public void onEvent(final OrderEvent oe) {
    switch(oe.getState()) {
      case CREATED: 
        ob.addOrder(oe); 
        return;
      case UPDATED:
        ob.modOrder(oe);
        return;
      case DELETED:
        ob.delOrder(oe);
        return;
      defualt:
        assert false;
        return;
    }
  }

  public static void main(final String[] args) throws Exception {
    final Ticker t = new BitstampTicker();
    t.watchOrders(new EventQueue(new OrderBookStream(new OrderBookStream.Evt() {
      public void onUpdate(final OrderBook ob) {
        System.out.print("\u001b[2J\u001b[H");
        System.out.println(ob);
        System.err.println(ob.getState().toCsv());
      }
    })));
  }
}

