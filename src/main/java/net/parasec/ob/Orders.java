package net.parasec.ob;

import net.parasec.trading.ticker.core.wire.OrderInfo;
import net.parasec.trading.ticker.core.wire.Direction;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;


public final class Orders {

    // order pool contains all active orders.
    private final HashMap<String, LimitOrder> orderPool = new HashMap<String, LimitOrder>();

    // dead pool contains all processed orders.
    // (for now just set of ids)
    private final HashSet<String> deadPool = new HashSet<String>();

    // todo: move this up to linkedorderbook
    public HashSet<String> getDeadPool() {
	return deadPool;
    }

    // limit orders are stored in sparse array for fast access. index is USD*100,
    // max value = 9999.99 USD.
    private final Limit[] sparseLevels = new Limit[1000000];

    // the current best bid or ask.
    private Limit best = null;

    // when searching for next best ask or bid from price level, search down (-1)
    // for asks, and up (1) for bids.
    private final int direction;

    // to receive best bid/ask changes
    private final DepthListener depthListener;

    // market impact comparitor
    private interface Mic {
	boolean exceedsLimit(int levelPrice, int priceLimit);
    }
    private final Mic mic;


    // order type will determine the direction to search.
    public Orders(final Direction type, final DepthListener depthListener) {
	this.direction = type.equals(Direction.BUY) ? 1 : -1;
	if(depthListener==null){
	    this.depthListener = (new DepthListener() {
		    public void onBestChanged(final Limit l){}
		});
	}else{
	    this.depthListener=depthListener;
	}
	if(this.direction == 1) {
	    mic = (new Mic() {
		    public boolean exceedsLimit(final int levelPrice, final int priceLimit) {
			return (levelPrice < priceLimit);
		    }
		});
	} else {
	    mic = (new Mic() {
		    public boolean exceedsLimit(final int levelPrice, final int priceLimit) {
			return (levelPrice > priceLimit);
		    }
		});
	}
    }
    
    // given a new price point, search in direction of best bid/ask for higher level
    // price point. when found, insert new price point, for example in level 3:
    // L2 <-> L3 <-> previous L3 (now L4).
    private Limit linkNextBest(final int priceIdx) {
	final int direction = this.direction;
	final Limit[] sparseLevels = this.sparseLevels;

	int j = direction == -1 ? priceIdx : (sparseLevels.length - priceIdx) - 1;
	for(int i = priceIdx + direction; --j >= 0; i += direction) {

	    final Limit left = sparseLevels[i];

	    if(left != null) {
		// active price point found.
		final Limit right = left.getRightSibling();
		final Limit p = new Limit(priceIdx, left, right);
	        left.setRightSibling(p);

	        if(right != null) {
		    // if previous at this level, insert in-between.
		    right.setLeftSibling(p);
		} 
		return p;
	    }

	} 
	return null;
    }

    // create a new price point. 
    private Limit createLimit(final int priceIdx) {
	final Limit p;
	if(best == null) {
	    // we are the first order (must be the best).
	    p = new Limit(priceIdx, null, null);
	    best = p;
	    depthListener.onBestChanged(p);
	} else if(direction*(priceIdx-best.getPrice()) > 0) {
	    // better than best bid or ask  
	    p = new Limit(priceIdx, null, best);
	    best.setLeftSibling(p);
	    best = p;
	    depthListener.onBestChanged(p);
	} else {
	    // insert behind next best price point.
	    p = linkNextBest(priceIdx);
	}
	sparseLevels[priceIdx] = p;
	return p;
    }

    public void addOrder(final OrderInfo order) {
	final String id = order.getexchangeOrderId();

	if(orderPool.containsKey(id)) {
	    // if the active order pool already contains this order id, we have received a 
	    // modify order before a new order. in this case, the previously added modify
	    // order will reflect the latest known remaining volume. discard this event, since
	    // it contains stale information.
	    return;
	}

	addNewOrder(order);
    }

    private void addNewOrder(final OrderInfo order) {
	final int priceIdx = order.getPrice();
	final int orderId = Integer.parseInt(order.getexchangeOrderId());

	Limit p = sparseLevels[priceIdx];
	final LimitOrder o;

	if(p == null) {

	    // price level does not exist yet, create it.
	    p = createLimit(priceIdx);

	    // create initial order for this price level.
	    o = new LimitOrder(order, p, null);  

	    // initialise the order queue at this price level (head = tail)
	    p.setHead(o).setLast(o);

	} else {

	    // price limit exists. insert new order into this price level queue ordered by order id
	    // (this is because there is no guarantee order book events will arrive in order).
		
	    // keep looking left until we find the left sibling (ls order_id < new order_id)
	    LimitOrder ls = p.getLast();
	    while(ls != null && Integer.parseInt(ls.getOrder().getexchangeOrderId()) > orderId) {
		ls = ls.getLeftSibling();
	    }
		
	    o = new LimitOrder(order, p, ls);

	    if(ls == null) {
		// jumped to front of queue.
		final LimitOrder rs = p.getHead();
		rs.setLeftSibling(o);
		o.setRightSibling(rs);
		p.setHead(o);
	    } else {
		final LimitOrder existingRs = ls.getRightSibling();
		ls.setRightSibling(o);
		if(existingRs == null) {
		    // at end of queue (as expected.)
		    p.setLast(o);
		} else {
		    // somewhere in-between
		    o.setRightSibling(existingRs);
		    existingRs.setLeftSibling(o);
		}
	    }
	}

	// increase number of orders and volume at this price level.
	p.setOrders(p.getOrders() + 1).setVolume(p.getVolume() + order.getVolume());

	// add the new order (id) to the active order pool.
	orderPool.put(order.getexchangeOrderId(), o);
    }

    public long modOrder(final OrderInfo order) {
	final String id = order.getexchangeOrderId();

	final LimitOrder o = orderPool.get(id);
	if(o == null) {
	    // treat as a new order.
	    addNewOrder(order);
	    return 0; // we do not know how much volume has been removed since we have not seen the initial order yet.
	}

	// modify existing order.
	
	final OrderInfo curOrder = o.getOrder();
	final long curVolume = curOrder.getVolume();
	final long modVolume = order.getVolume();
	if(curVolume <= modVolume) {
	    // modified is stale information (<) or is duplicate event (=) discard.
	    return -1;
	}

	// set new (remaining) volume for order
	curOrder.setVolume(modVolume);
	
	// subtract volume from price level
	final long delta = curVolume - modVolume;
	final Limit parent = o.getParent();
	parent.setVolume(parent.getVolume() - delta);

	assert delta > 0 : "error: "+o.getOrder().getexchangeOrderId();

	return delta;
    }

    public long remOrder(final String id, final long localTimestamp) {
	    
	final LimitOrder o = orderPool.remove(id);
	if(o == null) return -1;

	// volume removed is last remaining volume in order.
	final long volRemoved = o.getOrder().getVolume();
	
	final Limit parent = o.getParent();
	final LimitOrder leftSibling = o.getLeftSibling();
	final LimitOrder rightSibling = o.getRightSibling();

	if(leftSibling == null) {
	    if(rightSibling == null) { 
		// both null: no more orders. remove price level.
		final Limit leftLimit = parent.getLeftSibling();
		final Limit rightLimit = parent.getRightSibling();
		if(leftLimit != null) {
		    leftLimit.setRightSibling(rightLimit);
		}
		if(rightLimit != null) {
		    rightLimit.setLeftSibling(leftLimit);
		}
		if(parent.equals(best)) {
		    best = rightLimit;
		    depthListener.onBestChanged(rightLimit);
		}
		sparseLevels[parent.getPrice()] = null;
	    } else { 
		// left = null, right != null: remove from front of order queue,
		// update overall price level volume and # of orders.
		parent.setHead(rightSibling.setLeftSibling(null))
		    .setVolume(parent.getVolume() - volRemoved)
		    .setOrders(parent.getOrders() - 1);
	    }
	} else {
	    if(rightSibling != null) { 
		// both not null: remove from inside of order queue. 
		leftSibling.setRightSibling(rightSibling.setLeftSibling(leftSibling));
	    } else { 
		// left != null, right = null: remove from end of order queue.
		parent.setLast(leftSibling.setRightSibling(null));
	    }
	    // update the overall price level volume and # of orders
	    parent.setVolume(parent.getVolume() - volRemoved)
		.setOrders(parent.getOrders() - 1);   
	}
	return volRemoved;
    }

    public LimitOrder getOrder(final String id) {
	return orderPool.get(id);
    }

    public int getOrders() {
	return orderPool.size();
    }

    public Limit getBest() {
	return best;
    }
    
    public Limit[] getLevels(final int depth) {
	final Limit[] levels = new Limit[depth];
	final Limit[] sparseLevels = this.sparseLevels;

	if(direction == -1) {
	    // asks: lowest -> highest
	    for(int i = 0, j = 0, len = sparseLevels.length; i < len && j < depth; i++) {
		final Limit l = sparseLevels[i];
		if(l != null) {
		    levels[j] = l;
		    j++;
		}
	    }
	} else {
	    // bids: highest -> lowest
	    for(int i = sparseLevels.length, j = 0; --i >= 0 && j < depth; ) {
		final Limit l = sparseLevels[i];
		if(l != null) {
		    levels[j] = l;
		    j++;
		}

	    }

	}
	return levels;
    }
    
    public int getMarketImpact(final long volume) {
	
	if(best==null)
	    return 0; 

	int impact = 0;
	long volSum = 0;
	Limit l = best;

	do {

	    impact = l.getPrice();
	    volSum += l.getVolume();

	    l = l.getRightSibling();

	} while(volSum < volume && l != null);

	return impact;
    }

    public int[] getMarketImpact(final long[] volume) {
	
	final int len = volume.length;
	final int[] impacts = new int[len];

       
	if(best!=null) {
	    
	    Limit l = best;
	    int impact = l.getPrice();
	    long cumVol = l.getVolume();
	    
	    for(int i = 0; i < len; i++) {
		final long btc = volume[i];
	
		while(cumVol < btc && l != null && (l = l.getRightSibling()) != null) {
		    cumVol += l.getVolume();
		    impact = l.getPrice();
		}	
		impacts[i] = impact;
	    }

	}
	return impacts;
    }

    public int getMarketImpact(final Map<String,MarketOrder> activeMarketOrders) {
	int impact = 0;
	
	long offset = 0;
	Limit l = best;
        final Iterator<Map.Entry<String,MarketOrder>> it = activeMarketOrders.entrySet().iterator(); 
	final Mic mic = this.mic;

	while(it.hasNext()) {

	    final OrderInfo o = it.next().getValue().getOrder();
	    final int priceLimit = o.getPrice();
	    final long orderVolume = o.getVolume();

	    long volSum = offset;
	    Limit last = null;

	    while(l != null) {

		impact = l.getPrice();
		
		if(mic.exceedsLimit(impact, priceLimit)) {
		    
		    if(last != null) {
			impact = last.getPrice();
		    }
		    
		    break;
		} 

		final long levelVolume = l.getVolume();
		volSum += levelVolume;
       
		if(volSum >= orderVolume) {
		    offset = (volSum - orderVolume) - levelVolume;
		    break;
		}
		
		last = l;
		l = l.getRightSibling();
		
	    }

	}
	return impact;
    }

    // ask side.
    // return: priceIdx if level exists there, or next up - 1
    public int snapLevel(final int priceIdx) {
	return snapLevel(priceIdx, -1);
    }

    public int snapLevel(final int priceIdx, final int ignorePrice) {
	
	if(best==null)
	    return priceIdx;

	final Limit[] sparseLevels = this.sparseLevels;
	int i = priceIdx;

	Limit l = sparseLevels[i];
	while(i < 99999 && (l == null || (l.getPrice() == ignorePrice && l.getOrders() == 1))) {
	    l = sparseLevels[++i];
	}

	return (i == priceIdx ? priceIdx : i - 1);
    }


}
