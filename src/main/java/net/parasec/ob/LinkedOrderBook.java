package net.parasec.ob;

import net.parasec.trading.ticker.core.wire.OrderEvent;
import net.parasec.trading.ticker.core.wire.OrderInfo;
import net.parasec.trading.ticker.core.wire.Direction;
import net.parasec.trading.ticker.core.wire.Trade;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.ArrayDeque;
import java.util.HashSet;


public final class LinkedOrderBook implements OrderBook {

  private long firstNewOrderTs = 4102444800L; // Fri Jan  1 00:00:00 UTC 2100

    private final State state = new State();
    private final ArrayDeque<MarketOrder> lastOrders = new ArrayDeque<MarketOrder>(100);
    

    private final int depth = 45;
    // last 100 t&s (trades) derived from order book.
    private final ArrayDeque<Trade> t_and_s = new ArrayDeque<Trade>(100);

    // last 100 cancels (with vol >0)
    private final ArrayDeque<Cancel> lastCancels = new ArrayDeque<Cancel>(100);
   

    // market orders (emulated: an order is a market order if it crosses the book)
    // insertion order.
    // Bouchard, in Statistical properties of stock order books, defines this type of order as technically being 
    // a "marketable limit order"
    private final LinkedHashMap<String,MarketOrder> buyMarketOrders = new LinkedHashMap<String,MarketOrder>();
    private final LinkedHashMap<String,MarketOrder> sellMarketOrders = new LinkedHashMap<String,MarketOrder>();

    // in the event that an mo has not been cleared after 1 minute.
    // this can happen if data is omitted from the feed. 
    private void purgeStaleBuyMos(long exchangeTs) {
      boolean purged=false;
      for(Iterator<Map.Entry<String,MarketOrder>> it
          = buyMarketOrders.entrySet().iterator(); it.hasNext(); ) {
        OrderInfo oi=it.next().getValue().getOrder();
        if(exchangeTs-oi.getExchangeTimestamp() > 60000) {
          state.moActiveBuys--;
          state.moOutstandingBuyVolume-=oi.getVolume();  
          it.remove();  
          purged=true;
        }
      }
      if(purged)
        state.moBuyTip = asks.getMarketImpact(buyMarketOrders);
    }

    private void purgeStaleSellMos(long exchangeTs) {
      boolean purged=false;
      for(Iterator<Map.Entry<String,MarketOrder>> it
          = sellMarketOrders.entrySet().iterator(); it.hasNext(); ) {
        OrderInfo oi=it.next().getValue().getOrder();
        if(exchangeTs-oi.getExchangeTimestamp() > 60000) {
          state.moActiveSells--;
          state.moOutstandingSellVolume-=oi.getVolume();
          it.remove();
          purged=true;
        }
      }
      if(purged)
        state.moSellTip = bids.getMarketImpact(sellMarketOrders);
    }

    private String getFirstKey(final LinkedHashMap<String,MarketOrder> marketOrders) {
	if(marketOrders.isEmpty())
	    return null; 
	final Map.Entry<String, MarketOrder> me = marketOrders.entrySet().iterator().next();
	return me.getKey();
    }
    
    private boolean pruneBuyMo(final MarketOrder mo) {
	final int bestAskPrice = state.bestAsk != null ? state.bestAsk.getPrice() : Integer.MAX_VALUE;
	final OrderInfo o = mo.getOrder();
	if(o.getLimitPrice() < bestAskPrice) {
	    final long unFilledVolume = o.getVolume();
	    final long filledVolume = mo.getInitialVolume() - unFilledVolume;
	    if(filledVolume == 0)
		return false;		
	    bids.modOrder(o);
	    mo.setFilledVolume(filledVolume);
	    state.moActiveBuys--;
	    state.moOutstandingBuyVolume -= unFilledVolume;
	    state.totalBids++;
	    state.totalBidVol += unFilledVolume;	
	    addFilledMo(mo);
	    return true;
	}
	return false;
    }

    private boolean pruneSellMo(final MarketOrder mo) {
	final int bestBidPrice = state.bestBid != null ? state.bestBid.getPrice() : 0;
	final OrderInfo o = mo.getOrder();
	if(o.getLimitPrice() > bestBidPrice) {
	    final long unFilledVolume = o.getVolume();
	    final long filledVolume = mo.getInitialVolume() - unFilledVolume;
	    if(filledVolume == 0)
		return false;
	    asks.modOrder(o);
	    mo.setFilledVolume(filledVolume);		
	    state.moActiveSells--;
	    state.moOutstandingSellVolume -= unFilledVolume;
	    state.totalAsks++;
	    state.totalAskVol += unFilledVolume;
	    addFilledMo(mo);
	    return true;
	}
	return false;
    }


    // limit buy/sell orders. orders that do not cross the book are inserted 
    // immediately. otherwise, they are market orders, in which case they are
    // stored in the buy/sellMarketOrders set until the best bid/ask on the
    // other side of the book is uncrossed. they are then inserted into the book
    // (this can happen if market order is partially filled: it will knock out
    // the best bid/ask at the final price level. we wait until the best bid/ask
    // has changed because there is no guarantee which event will arrive first:
    // partial fill update, or cancel/fill update on other side of book.
    private final Orders asks = new Orders(Direction.SELL, new DepthListener() {
	    public void onBestChanged(final Limit l) {
		state.bestAsk = l;
		// best ask has changed. iterate through current market orders,
		// if market order target price < best ask, hand market order over
		// to order book as a limit order.
		for(final Iterator<Map.Entry<String,MarketOrder>> it 
			= buyMarketOrders.entrySet().iterator(); it.hasNext(); ) {
		    final MarketOrder mo = it.next().getValue();
		    if(pruneBuyMo(mo)) {
			it.remove();
		    }
		}	
	    }
	});
    private final Orders bids = new Orders(Direction.BUY, new DepthListener() {
	    public void onBestChanged(final Limit l) {
		state.bestBid = l;
		// best bid has changed. same logic as ask.
		for(final Iterator<Map.Entry<String,MarketOrder>> it 
			= sellMarketOrders.entrySet().iterator(); it.hasNext(); ) {
		    final MarketOrder mo = it.next().getValue();
		    if(pruneSellMo(mo)) {
			it.remove();
		    }
		}
	    } 
	});

    private long getMaxFill(final Direction type) {	
	long max = 0;
	for(final MarketOrder mo : lastOrders) {
	    if(!type.equals(mo.getDirection()))
		continue;
	    final long volume = mo.getFilledVolume();
	    if(volume > max) {
		max = volume;
	    }
	}
	return max;
    }

    private long getMaxTrade(final Direction type) {
	long max = 0;
	for(final Trade s : t_and_s) {
	    if(!type.equals(s.getDirection()))
		continue;
	    final long volume = s.getVolume();
	    if(volume > max) {
		max = volume;
	    }
	}
	return max;
    }

    private long getMaxCancel(final Direction type) {
	long max = 0;
	for(final Cancel c : lastCancels) {
	    if(!type.equals(c.getType()))
		continue;
	    final long volume = c.getAmount();
	    if(volume > max) {
		max = volume;
	    }
	}
	return max;	
    }

    private void addFilledMo(final MarketOrder mo) {
	final long filledVolume = mo.getFilledVolume();
	if(lastOrders.size() == 100) {
	    final MarketOrder first = lastOrders.removeFirst();
	    final long firstFilledVolume = first.getFilledVolume();
	    if(first.getDirection().equals(Direction.BUY)) {
		state.moLast100BuyVol -= firstFilledVolume;
		if(firstFilledVolume == state.moLast100BuyMax) {
		    state.moLast100BuyMax = getMaxFill(Direction.BUY);
		}
		state.moLast100Buy--;
	    } else {
		state.moLast100SellVol -= firstFilledVolume;
		if(firstFilledVolume == state.moLast100SellMax) {
		    state.moLast100SellMax = getMaxFill(Direction.SELL);
		}
	    }
	}
	if(mo.getDirection().equals(Direction.BUY)) {
	    state.moLast100BuyVol += filledVolume;
	    if(filledVolume > state.moLast100BuyMax) {
		state.moLast100BuyMax = filledVolume;
	    }
	    state.moLast100Buy++;
	    if(state.moActiveBuys == 0) {
		state.moBuyTip = 0;
	    } else {
		state.moBuyTip = asks.getMarketImpact(buyMarketOrders);
	    }

	    state.totalMoBuyVol += filledVolume;
	    state.totalMoBuys++;

	} else {
	    state.moLast100SellVol += filledVolume;
	    if(filledVolume > state.moLast100SellMax) {
		state.moLast100SellMax = filledVolume;
	    }
	    if(state.moActiveSells == 0) {
		state.moSellTip = 0;
	    } else {
		state.moSellTip = bids.getMarketImpact(sellMarketOrders);
	    }
	    	    
	    state.totalMoSellVol += filledVolume;
	    state.totalMoSells++;

	}
	lastOrders.addLast(mo);
    }
	
    public void addSale(final Trade s) {
	final long volumeRemoved = s.getVolume();
	final int price = s.getPrice();

	if(price > state.highestPrice) {
	    state.highestPrice = price;
	} else if(price < state.lowestPrice) {
	    state.lowestPrice = price;
	}
	
	if(t_and_s.size() == 100) {
	    final Trade first = t_and_s.removeFirst();
	    final long firstVol = first.getVolume();
	    if(first.getDirection().equals(Direction.BUY)) {
		if(firstVol == state.moLast100BuyTradeMax) {
		    state.moLast100BuyTradeMax = getMaxTrade(Direction.BUY);
		}
		state.moLast100BuyTrades--;
		state.moLast100BuyTradeVol -= firstVol;
	    } else {
		if(firstVol == state.moLast100SellTradeMax) {
		    state.moLast100SellTradeMax = getMaxTrade(Direction.SELL);
		}
		state.moLast100SellTradeVol -= firstVol;
	    }
	}

	if(s.getDirection().equals(Direction.BUY)) {
	    if(volumeRemoved > state.moLast100BuyTradeMax) {
		state.moLast100BuyTradeMax = volumeRemoved;
	    }
	    state.moLast100BuyTrades++;
	    state.moLast100BuyTradeVol += volumeRemoved;	        
	    state.totalAskVol -= volumeRemoved;    
	    prune(s, asks);
	    state.askPercentile = getPercentileVwap(state.bestAsk, Percentile.PERCENTILE_STEP_SIZE, Percentile.PERCENTILE_STEPS);
	    state.buyImpact = asks.getMarketImpact(State.impactPoints);
	} else {
	    if(volumeRemoved > state.moLast100SellTradeMax) {
		state.moLast100SellTradeMax = volumeRemoved;
	    }
	    state.moLast100SellTradeVol += volumeRemoved;
	    state.totalBidVol -= volumeRemoved;
	    prune(s, bids);
	    state.bidPercentile = getPercentileVwap(state.bestBid, Percentile.PERCENTILE_STEP_SIZE, Percentile.PERCENTILE_STEPS);
	    state.sellImpact = bids.getMarketImpact(State.impactPoints);
	}
	state.event++;
	state.ts = System.currentTimeMillis();
	state.lastTrade = s;
	t_and_s.addLast(s);
    }
    

    private long removeOrphanedOrders(final Limit best, final Orders orders, final long hitTimestamp) {
	// remove any orders from the best bid/ask that arrived before the last sale.
	long orphanedVolume = 0;
	LimitOrder lo = best.getHead();
	while(lo!=null) {
	    final LimitOrder next = lo.getRightSibling();
	    final OrderInfo o = lo.getOrder();
	    if(o.getExchangeTimestamp() < hitTimestamp) { 
		final String id = o.getexchangeOrderId();
		orphanedVolume += orders.remOrder(id);
		orders.getDeadPool().add(id);
	    }
	    lo = next;
	}
	return orphanedVolume;
    }

    private void prune(final Trade s, Orders orders) {
	final Limit best = orders.getBest();
	if(best!=null) {
            final long hitTimestamp = s.getMakerTimestamp();
	    final int existingOrders = best.getOrders();
	    if(s.getDirection().equals(Direction.BUY)) {
		if(s.getPrice() > best.getPrice()) {
		    // ask side.
		    state.totalAskVol -= removeOrphanedOrders(best, orders, hitTimestamp);
		    state.totalAsks -= existingOrders - best.getOrders();
		}
	    } else {
		if(s.getPrice() < best.getPrice()) {
		    // bid side.
		    state.totalBidVol -= removeOrphanedOrders(best, orders, hitTimestamp);
		    state.totalBids -= existingOrders - best.getOrders();
		}
	    }
	}
    }

    private void addCancel(final Cancel c) {
	final long volumeCancelled = c.getAmount();
	
	if(lastCancels.size() == 100) {
	    
	    final Cancel first = lastCancels.removeFirst();
	    final long firstVol = first.getAmount();
	    
	    if(first.getType().equals(Direction.BUY)) {
	
		if(firstVol == state.bidLast100CancelMax) {
		    state.bidLast100CancelMax = getMaxCancel(Direction.BUY);
		}

		state.bidLast100Cancel--;
		state.bidLast100CancelVolume -= firstVol;
	
	    } else {
		
		if(firstVol == state.askLast100CancelMax) {
		    state.askLast100CancelMax = getMaxCancel(Direction.SELL);
		}

		state.askLast100CancelVolume -= firstVol;
	    }
	}

	if(c.getType().equals(Direction.BUY)) {

	    if(volumeCancelled > state.bidLast100CancelMax) {
		state.bidLast100CancelMax = volumeCancelled;
	    }

	    state.totalBidVol -= volumeCancelled;
	    //state.bidPercentile = getPercentileVwap(state.bestBid, 0.05);
	    state.bidPercentile = getPercentileVwap(state.bestBid, Percentile.PERCENTILE_STEP_SIZE, Percentile.PERCENTILE_STEPS);
	    state.sellImpact = bids.getMarketImpact(State.impactPoints);

	    state.bidLast100Cancel++;
	    state.bidLast100CancelVolume += volumeCancelled;	        
	} else {

	    if(volumeCancelled > state.askLast100CancelMax) {
		state.askLast100CancelMax = volumeCancelled;
	    }

	    state.totalAskVol -= volumeCancelled;
	    //state.askPercentile = getPercentileVwap(state.bestAsk, 0.05);
	    state.askPercentile = getPercentileVwap(state.bestAsk, Percentile.PERCENTILE_STEP_SIZE, Percentile.PERCENTILE_STEPS);
	    state.buyImpact = asks.getMarketImpact(State.impactPoints);

	    state.askLast100CancelVolume += volumeCancelled;
	}

	state.event++;
	state.ts = System.currentTimeMillis();
	lastCancels.addLast(c);
    }  
	
    private Percentile[] getPercentileVwap(final Limit best, final double stepSize, final int steps) {
	final Percentile[] p = new Percentile[steps];

	if(best!=null) {

	    final int bestPrice = best.getPrice();
	    
	    int orders = 0;
	    int priceLevels = 0;
	    long vwapSum = 0;
	    long volume = 0;

	    Limit l = best;
	    int i = 0;

	    do { 

		final int price = l.getPrice();
		final double pct = Math.abs((bestPrice-price)/(double)bestPrice);
		final int j = ((int)Math.ceil(pct/stepSize))-1; 
		
		if(j>i) {

		    final int vwap = (int)Math.round(vwapSum/(double)volume);

		    p[i] = new Percentile(vwap, orders, priceLevels, volume);

		    if(j>=steps) {
			break;
		    } else {
			orders = priceLevels = 0; 
			vwapSum = volume = 0;
			i = j;
		    }	
		} 

		final long levelVolume = l.getVolume();

		vwapSum += levelVolume*price;
		volume += levelVolume;	    
		orders += l.getOrders();
		priceLevels++;
		
		l = l.getRightSibling();
		
	    } while(l != null);

	}        
	return p;
    }

  public void addOrder(final OrderEvent oe) {
        final OrderInfo o = oe.getOrderInfo();    

        long exchangeTs = o.getExchangeTimestamp();
        if(firstNewOrderTs == 4102444800L)
            firstNewOrderTs = exchangeTs; 

        // new orders contain latest exchange ts.
        // remove any stale market orders.
        purgeStaleBuyMos(exchangeTs);
        purgeStaleSellMos(exchangeTs);

        final Direction type = oe.getDirection();
	final int priceIdx = o.getLimitPrice(); 
	final long volSatoshi = o.getVolume();

	final String id = o.getexchangeOrderId();
	
	if(type.equals(Direction.BUY)) {

	    if(bids.getDeadPool().contains(id))
		return; // discard stale information.

	    final Limit best = asks.getBest();
	    if(best != null && best.getPrice() <= priceIdx) {
		// book crossed: put in bid market order map.
		if(!buyMarketOrders.containsKey(id)) {
		    buyMarketOrders.put(id, new MarketOrder(o, Direction.BUY));
		    state.event++;
		    state.ts = System.currentTimeMillis();
		    state.moActiveBuys++;
		    state.moOutstandingBuyVolume+=volSatoshi;
		    state.moBuyTip = asks.getMarketImpact(buyMarketOrders);
		} 
		// else inserted before from modOrder(), discard stale information.
	    } else {
		// put in buy side of limit order book.
		bids.addOrder(o);	
		state.event++;
		state.ts = System.currentTimeMillis();
		state.totalBids++;
		state.totalBidVol += volSatoshi;
		state.bidPercentile = getPercentileVwap(state.bestBid, Percentile.PERCENTILE_STEP_SIZE, Percentile.PERCENTILE_STEPS);
		state.sellImpact = bids.getMarketImpact(State.impactPoints);
	    }
	}else{
	    if(priceIdx >= 1000000) {
		// ignore orders to sell >= 10k.
		return;
	    }

	    if(asks.getDeadPool().contains(id))
		return;

	    final Limit best = bids.getBest();
	    if(best != null && best.getPrice() >= priceIdx) {

		// book crossed: put in ask market order map.
		if(!sellMarketOrders.containsKey(id)) {
		    sellMarketOrders.put(id, new MarketOrder(o, Direction.SELL));
		    state.event++;
		    state.ts = System.currentTimeMillis();
		    state.moActiveSells++;
		    state.moOutstandingSellVolume+=volSatoshi;
		    state.moSellTip = bids.getMarketImpact(sellMarketOrders);
		}
	    } else {
		// put in ask side of limit order book.
		asks.addOrder(o);
		state.event++;
		state.ts = System.currentTimeMillis();
		state.totalAsks++;
		state.totalAskVol += volSatoshi;
		state.askPercentile = getPercentileVwap(state.bestAsk, Percentile.PERCENTILE_STEP_SIZE, Percentile.PERCENTILE_STEPS);
		state.buyImpact = asks.getMarketImpact(State.impactPoints);
	    }
	}
    }

    public void modOrder(final OrderEvent oe) {
        final OrderInfo o = oe.getOrderInfo();

        if(o.getExchangeTimestamp() < firstNewOrderTs)
            return;

        final Direction type = oe.getDirection();
	final int priceIdx = o.getLimitPrice();
        final long volSatoshi = o.getVolume();

        final String id = o.getexchangeOrderId();

	if(type.equals(Direction.BUY)) {
	    
	    if(bids.getDeadPool().contains(id))
		return;

	    final LimitOrder existingOrder = bids.getOrder(id);
	    if(existingOrder!=null && existingOrder.getOrder().getLimitPrice() != priceIdx) {

		// remove from order book
		final long volumeRemoved = bids.remOrder(id);

		// update state
		state.totalBids--;
		state.totalBidVol -= volumeRemoved;
		state.bidPercentile = getPercentileVwap(state.bestBid, Percentile.PERCENTILE_STEP_SIZE, Percentile.PERCENTILE_STEPS);
		state.sellImpact = bids.getMarketImpact(State.impactPoints);

		// add back as a new order.
		addOrder(oe);	
	
		return;
	    }

	    if(buyMarketOrders.containsKey(id)) {
		// modify market order. (if modified volume info. is not stale).
		final MarketOrder mo = buyMarketOrders.get(id);
		final OrderInfo bmo = mo.getOrder();
		final long bmoVol = bmo.getVolume();
		if(bmoVol > volSatoshi) {
		    bmo.setVolume(volSatoshi);
		    final long delta = bmoVol - volSatoshi;
		    state.moOutstandingBuyVolume -= delta;
		    state.event++;
		    state.ts = System.currentTimeMillis();
		    if(pruneBuyMo(mo)) {
			buyMarketOrders.remove(id);
		    }
		}
	    } else {
		// if modified order results in crossed book, event has arrived out of order,
		// (should have seen new order first) -add it to market orders map.
		final Limit best = asks.getBest();
		if(best != null && best.getPrice() <= priceIdx) {
		    buyMarketOrders.put(id, new MarketOrder(o, Direction.BUY));
		    state.event++;
		    state.ts = System.currentTimeMillis();
		    state.moActiveBuys++;
		    state.moOutstandingBuyVolume+=volSatoshi;
		    state.moBuyTip = asks.getMarketImpact(buyMarketOrders);
		} else {
		    // send it to limit order book.
		    final long volRemoved = bids.modOrder(o);

		    if(volRemoved == 0) {
			// was inserted as a new (bid) order.
			state.event++;
			state.ts = System.currentTimeMillis();
			state.totalBids++;
			state.totalBidVol += volSatoshi;
			//state.bidPercentile = getPercentileVwap(state.bestBid, 0.05);
			state.bidPercentile = getPercentileVwap(state.bestBid, Percentile.PERCENTILE_STEP_SIZE, Percentile.PERCENTILE_STEPS);
			state.sellImpact = bids.getMarketImpact(State.impactPoints);
		    } else if(volRemoved > 0) {
			// a modified buy order, if in the order book, is a partial fill from some
			// sell market order: liquidity is being removed. log a sale in t&s (will
			// be the tip of the knife so to speak).
			final String takerId = getFirstKey(sellMarketOrders);
			final String makerId = o.getexchangeOrderId();
			final Trade s = new Trade(Direction.SELL, priceIdx, volRemoved, System.currentTimeMillis(), o.getExchangeTimestamp(), null, makerId, takerId); 
			addSale(s);
		    }
		}
	    }
	} else {
	    if(asks.getDeadPool().contains(id))
		return;

	    final LimitOrder existingOrder = asks.getOrder(id);
	    if(existingOrder!=null && existingOrder.getOrder().getLimitPrice() != priceIdx) {
		// remove from order book
		final long volumeRemoved = asks.remOrder(id);

		// update state
		state.totalAsks--;
		state.totalAskVol -= volumeRemoved;
		state.askPercentile = getPercentileVwap(state.bestAsk, Percentile.PERCENTILE_STEP_SIZE, Percentile.PERCENTILE_STEPS);
		state.buyImpact = asks.getMarketImpact(State.impactPoints);

		// add back as a new order.
		addOrder(oe);
		
		return;
	    }

	    if(sellMarketOrders.containsKey(id)){
		final MarketOrder mo = sellMarketOrders.get(id);
		final OrderInfo smo = mo.getOrder();
		final long smoVol = smo.getVolume();
		if(smoVol > volSatoshi) {
		    smo.setVolume(volSatoshi);
		    final long delta = smoVol - volSatoshi;
		    state.moOutstandingSellVolume -= delta;
		    state.event++;
		    state.ts = System.currentTimeMillis();
		    if(pruneSellMo(mo)) {
			sellMarketOrders.remove(id);
		    }
		}
	    } else {
		final Limit best = bids.getBest();
		if(best != null && best.getPrice() >= priceIdx) {
		    sellMarketOrders.put(id, new MarketOrder(o, Direction.SELL));
		    state.event++;
		    state.ts = System.currentTimeMillis();
		    state.moActiveSells++;
		    state.moOutstandingSellVolume+=volSatoshi;
		    state.moSellTip = bids.getMarketImpact(sellMarketOrders);
	        } else {
		    final long volRemoved = asks.modOrder(o);

		    if(volRemoved == 0) {
			// was inserted as a new (ask) order.
			state.event++;
			state.ts = System.currentTimeMillis();
			state.totalAsks++;
			state.totalAskVol += volSatoshi;
			state.askPercentile = getPercentileVwap(state.bestAsk, Percentile.PERCENTILE_STEP_SIZE, Percentile.PERCENTILE_STEPS);
			state.buyImpact = asks.getMarketImpact(State.impactPoints);
		    } else if(volRemoved > 0) {
			final String takerId = getFirstKey(buyMarketOrders);
			final String makerId = o.getexchangeOrderId();
		        final Trade s = new Trade(Direction.BUY, priceIdx, volRemoved, System.currentTimeMillis(), o.getExchangeTimestamp(), null, makerId, takerId);
			addSale(s);
		    }
		}
	    }
	}
    }

    public void delOrder(final OrderEvent oe) {
        final OrderInfo o = oe.getOrderInfo();

        if(o.getExchangeTimestamp() < firstNewOrderTs)
            return;

        final String id = o.getexchangeOrderId();
        final Direction type = oe.getDirection();
        final int priceIdx = o.getLimitPrice();
        final long volSatoshi = o.getVolume();

	final boolean completeFill = (volSatoshi == 0);

	state.ts = System.currentTimeMillis();
	  
	if(type.equals(Direction.BUY)) {
	    bids.getDeadPool().add(id);
	    if(buyMarketOrders.containsKey(id)) {
		final MarketOrder mo = buyMarketOrders.remove(id);
		final OrderInfo moOrderInfo = mo.getOrder();
		final long unFilledVolume = (completeFill ? 0 : moOrderInfo.getVolume());
		final long filledVolume = mo.getInitialVolume() - unFilledVolume;
		mo.setFilledVolume(filledVolume);
		state.moActiveBuys--;
		state.moOutstandingBuyVolume -= (completeFill ? moOrderInfo.getVolume() : unFilledVolume);
		addFilledMo(mo);
	    } else { 
		final long volRemoved = bids.remOrder(id);
		if(volRemoved > 0) { // -1 = unknown id.
		    if(completeFill) {
			final String takerId = getFirstKey(sellMarketOrders);
			final String makerId = id;
		        final Trade s = new Trade(Direction.SELL, priceIdx, volRemoved, System.currentTimeMillis(), o.getExchangeTimestamp(), null, makerId, takerId);
			addSale(s);
		    } else {
			// trader removed after a partial fill or cancelled before any fill
			addCancel(new Cancel(id, Direction.BUY, volRemoved));
		    }
		    state.totalBids--;
		}
	    }
	} else {
	    asks.getDeadPool().add(id);
	    if(sellMarketOrders.containsKey(id)) {
		final MarketOrder mo = sellMarketOrders.remove(id);
		final OrderInfo moOrderInfo = mo.getOrder();
		final long unFilledVolume = (completeFill ? 0 : moOrderInfo.getVolume());
		final long filledVolume = mo.getInitialVolume() - unFilledVolume;
		mo.setFilledVolume(filledVolume);
		state.moActiveSells--;
		state.moOutstandingSellVolume -= (completeFill ? moOrderInfo.getVolume() : unFilledVolume);
		addFilledMo(mo);
	    } else {
		final long volRemoved = asks.remOrder(id);
		if(volRemoved > 0) {
		    if(completeFill){
			final String takerId = getFirstKey(buyMarketOrders);
			final String makerId = id;
			final Trade s = new Trade(Direction.BUY, priceIdx, volRemoved, System.currentTimeMillis(), o.getExchangeTimestamp(), null, makerId, takerId);
			addSale(s);
		    } else {
			addCancel(new Cancel(id, Direction.SELL, volRemoved));
		    }
		    state.totalAsks--;
		}
	    }
	}
    }

    public State getState() {
	return state;
    }

    public Orders getBids() {
	return bids;
    }

    public Orders getAsks() {
	return asks;
    }

    public Trade getLastTrade() {
	return t_and_s.getLast();
    }

    private String formatAskLevel(final double per, final long volSum, final Limit askLevel) {
	if(askLevel!=null){
	    return Util.asUSD(askLevel.getPrice()) + "\t" +
		Util.asBTC(askLevel.getVolume()) + "\t" +
		askLevel.getOrders() + "\t" +
		Util.asBTC(volSum) + "\t" +
		String.format("%.2f", per) + "%";
	}
	return "";
    }

    private String formatBidLevel(final double per, final long volSum, final Limit bidLevel) {
	if(bidLevel!=null){
	    return String.format("%.2f", per) + "%\t" + 
		Util.asBTC(volSum) + "\t" + 
		bidLevel.getOrders() + "\t" +
		Util.asBTC(bidLevel.getVolume()) + "\t" +
		Util.asUSD(bidLevel.getPrice());
	}
	return "                                                      ";
    }

    public String toString() {
	final StringBuilder sb = new StringBuilder();
	final Limit[] bids = this.bids.getLevels(depth);
	final Limit bestBid = this.bids.getBest();
	final Limit[] asks = this.asks.getLevels(depth);
	final Limit bestAsk = this.asks.getBest();
	long bidVolSum = 0, askVolSum = 0;
	final Iterator<Trade> t_and_s_it = t_and_s.descendingIterator();
	for(int i = 0; i < depth; i++) {
	    final Limit bid = bids[i];
	    final Limit ask = asks[i];
	    final double bidPer;
	    if(bid!=null){
		final long bidVolume = bid.getVolume();
		final int bidPrice = bid.getPrice();
		final int best = bestBid.getPrice();
		bidVolSum+=bidVolume;
		bidPer=100*((best-bidPrice)/(double)best);
	    }else{
		bidPer = 0;
	    }
	    final double askPer;
	    if(ask!=null){
		final long askVolume = ask.getVolume();
		final int askPrice = ask.getPrice();
		final int best = bestAsk.getPrice();
		askVolSum+=askVolume;
		askPer=100*((askPrice-best)/(double)askPrice);
	    }else{
		askPer = 0;
	    }
	    sb.append(formatBidLevel(bidPer, bidVolSum, bid))
		.append(" | ")
		.append(formatAskLevel(askPer, askVolSum, ask));
	    if(t_and_s_it.hasNext()) {
		final Trade sale = t_and_s_it.next();
		sb.append(" ").append(Util.tradeToString(sale));
	    }
	    sb.append("\n");
	}
	return sb.append("=============================================================================================================\n")
	    .append(state).toString();
    }

}
