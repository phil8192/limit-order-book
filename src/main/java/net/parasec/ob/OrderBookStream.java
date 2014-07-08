package net.parasec.ob;

public final class OrderBookStream implements StreamListener {

    private final static String SRC = "BSX";
    private final OrderBook ob = new LinkedOrderBook();
    private long firstNewOrderTs = 4102444800L; // Fri Jan  1 00:00:00 UTC 2100 

 
    public interface Evt {
	void onUpdate(OrderBook ob);
    };
    private final Evt evt;

    
    public OrderBookStream(final Evt evt) {
	this.evt = evt;
    }

    private OrderType getType(final int type) {
	return type == 0 ? OrderType.BUY : OrderType.SELL;
    }

    public void onNewOrder(final JSON.Order o) {
        if(firstNewOrderTs == 4102444800L)
            firstNewOrderTs = o.getDatetime();
	final int orderId = o.getId();
	ob.addOrder(SRC, Integer.toString(orderId), orderId, getType(o.getType()), o.getDatetime(), 
		    System.currentTimeMillis(), o.getAmount(), o.getPrice());
	evt.onUpdate(ob);
    }

    public void onModOrder(final JSON.Order o) {
        final long ts = o.getDatetime();
        if(ts < firstNewOrderTs)
            return;
	final int orderId = o.getId();
	ob.modOrder(SRC, Integer.toString(orderId), orderId, getType(o.getType()), ts, 
		    System.currentTimeMillis(), o.getAmount(), o.getPrice());
	evt.onUpdate(ob);
    }

    public void onDelOrder(final JSON.Order o) {
        final long ts = o.getDatetime();
        if(ts < firstNewOrderTs)
            return;
	final int orderId = o.getId();
	ob.delOrder(SRC, Integer.toString(orderId), orderId, getType(o.getType()), o.getDatetime(), 
		    System.currentTimeMillis(), o.getAmount(), o.getPrice());
	evt.onUpdate(ob);
    }

    public void onTrade(final JSON.Trade t) {}
    public void onOb(final JSON.Ob ob) {}
     
    public static void main(final String[] args) throws Exception {
	new StreamQueue(new OrderBookStream(new OrderBookStream.Evt() {
	    public void onUpdate(final OrderBook ob) {	   
                System.out.print("\u001b[2J\u001b[H");
		System.out.println(ob);
		System.err.println(ob.getState().toCsv());
            }
	}));	
    }
}
