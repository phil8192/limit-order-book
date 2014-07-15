package net.parasec.ob;

import net.parasec.trading.ticker.core.wire.Trade;


// updated on every tick

public final class State {
    
    public long event = 0;

    public long ts=0; // current time milliseconds
    public Trade lastTrade=null;
  
    //public long lastTradeTs=0;
    //public int lastPrice=0;
    //public long lastVol=0;
    //public int buyOrSell=0; // last price result of buy +1 or sell -1.
    // int takerId, int makerId. 

    //////////// liquidity takers/mos
    public int moActiveBuys=0; // number of current active buy mo orders (large order will take some ticks to complete (as orders on other side of book are filled))
    public int moActiveSells=0;
    public long moOutstandingBuyVolume=0; // sum of active buys remaining volume (decrease on each tick as sell orders get lifted)
    public long moOutstandingSellVolume=0;
    public int moBuyTip=0; // the knife tip after all outstanding buy mo's filled (according to maintained order book)
    public int moSellTip=0;
    
    public int moLast100Buy=0; // from the last 100 mos, how many were buy orders
    public long moLast100BuyVol=0; // from last 100 mos, how much (filled) buy volume
    public long moLast100BuyMax=0; // from last 100 mos, what was the max (filled) buy volume
    public long moLast100SellVol=0; // from last 100 mos, how much (filled) sell volume
    public long moLast100SellMax=0; // from last 100 mos, what was the max (filled) sell volume
    
    public int moLast100BuyTrades=0; // from last 100 _trades_ how many were from buy mos
    public long moLast100BuyTradeVol=0; // from last 100 _trades_ how much buy volume
    public long moLast100BuyTradeMax=0; // from last 100 _trades_ what is the max trade size
    public long moLast100SellTradeVol=0;
    public long moLast100SellTradeMax=0;

    


    //////////// liquidity providers/makers
    public Limit bestBid=null;
    public Limit bestAsk=null;
    //public int bestBid=0;
    //public int bestAsk=0;
    //public long bestBidVol=0;
    //public long bestAskVol=0;

    public Percentile[] bidPercentile = new Percentile[Percentile.PERCENTILE_STEPS];
    public Percentile[] askPercentile = new Percentile[Percentile.PERCENTILE_STEPS];    

    //public int bidVwap=0; // <5% percentile
    //public int askVwap=0; // ""
    //public int bidLimitOrders=0; //""
    //public int askLimitOrders=0; //""
    //public long bidVol=0; // "" total vol <5% from best bid
    //public long askVol=0; //""

    public int lowestPrice=Integer.MAX_VALUE; // visibility
    public int highestPrice=0;
    public int totalBids=0;
    public int totalAsks=0;
    public long totalBidVol=0;
    public long totalAskVol=0;

    // from cancels where vol >0
    public int bidLast100Cancel=0; // from the last 100 cancels, how many were bids
    public long bidLast100CancelVolume=0; // from this last 100 cancels, how much bid volume was removed
    public long bidLast100CancelMax=0; // from this last 100 cancels, what was the max bid volume from a single order removed
    public long askLast100CancelVolume=0;
    public long askLast100CancelMax=0;

    
    public final static long[] impactPoints = new long[] {1,2,4,8,16,32,64,128,256,512,1024};
    static{
	for(int i = 0, len = impactPoints.length; i<len; i++) {
	    impactPoints[i] = Util.asSatoshi(impactPoints[i]);
	}
    }

    public int[] buyImpact;
    public int[] sellImpact;
    
    public long totalMoBuyVol = 0;
    public long totalMoSellVol = 0;
    public int totalMoBuys = 0;
    public int totalMoSells = 0;

    // .csv
    public StringBuilder toCsv() {
	// cat State.java |grep "=" |awk '{print $3}' |sed 's/^/.append(/g' |sed 's/\=0;/\).append(dl)/g'
	final String dl=",";
	return (new StringBuilder()
		.append("state,")
		.append(event).append(dl)
		.append(ts).append(dl)
		.append(lastTrade!=null ? Util.tradeToCsv(lastTrade, dl) : ",,,,,")
		.append(dl)
		.append(moActiveBuys).append(dl)
		.append(moActiveSells).append(dl)
		.append(Util.asBTC(moOutstandingBuyVolume)).append(dl)
		.append(Util.asBTC(moOutstandingSellVolume)).append(dl)
		.append(Util.asUSD(moBuyTip)).append(dl)
		.append(Util.asUSD(moSellTip)).append(dl)
		.append(moLast100Buy).append(dl)
		.append(Util.asBTC(moLast100BuyVol)).append(dl)
		.append(Util.asBTC(moLast100BuyMax)).append(dl)
		.append(Util.asBTC(moLast100SellVol)).append(dl)
		.append(Util.asBTC(moLast100SellMax)).append(dl)
		.append(moLast100BuyTrades).append(dl)
		.append(Util.asBTC(moLast100BuyTradeVol)).append(dl)
		.append(Util.asBTC(moLast100BuyTradeMax)).append(dl)
		.append(Util.asBTC(moLast100SellTradeVol)).append(dl)
		.append(Util.asBTC(moLast100SellTradeMax)).append(dl)
		.append(bestBid!=null ? bestBid.toCsv(dl) : ",")
		.append(dl)
		.append(bestAsk!=null ? bestAsk.toCsv(dl) : ",")
		.append(dl)
		

		//.append(bidPercentile!=null ? bidPercentile.toCsv(dl) : ",,")
		.append(Percentile.toCsv(bidPercentile, dl))
		.append(dl)
		//.append(askPercentile!=null ? askPercentile.toCsv(dl) : ",,")
		.append(Percentile.toCsv(askPercentile, dl))

		.append(dl)
		.append(Util.asUSD(lowestPrice)).append(dl)
		.append(Util.asUSD(highestPrice)).append(dl)
		.append(totalBids).append(dl)
		.append(totalAsks).append(dl)
		.append(Util.asBTC(totalBidVol)).append(dl)
		.append(Util.asBTC(totalAskVol)).append(dl)
		.append(bidLast100Cancel).append(dl)
		.append(Util.asBTC(bidLast100CancelVolume)).append(dl)
		.append(Util.asBTC(bidLast100CancelMax)).append(dl)
		.append(Util.asBTC(askLast100CancelVolume)).append(dl)
		.append(Util.asBTC(askLast100CancelMax)).append(dl)
	       
		.append(Util.asBTC(totalMoBuyVol)).append(dl).append(totalMoBuys).append(dl)
		.append(Util.asBTC(totalMoSellVol)).append(dl).append(totalMoSells).append(dl)

		.append(Impact.toCsv(buyImpact)).append(dl)
		.append(Impact.toCsv(sellImpact))
	);
    }

    

    public String toString() {

	try {

	    final String nl = "\n";
	    final StringBuilder sb = new StringBuilder().append("event = ").append(event).append(" ts = ").append(ts).append(" ")
		.append("last trade:");
	    if(lastTrade==null){
		sb.append(" none");
	    } else { 
		sb.append(" ts = ").append(lastTrade.getExchangeTimestamp()).append(" type = ").append(lastTrade.getDirection().name()).append(" amount = ").append(Util.asBTC(lastTrade.getVolume())).append(" btc").append(" @ $").append(Util.asUSD(lastTrade.getPrice())).append(" taker = ").append(lastTrade.getTakerIdentifier()).append(" maker = ").append(lastTrade.getMakerIdentifier());
	    }
	    sb.append(nl);


	    sb.append("takers (market orders): ").append("active buys (#|vol|impact) = ").append(moActiveBuys).append("|").append(Util.asBTC(moOutstandingBuyVolume)).append("|").append(Util.asUSD(moBuyTip)).append(" active sells (#|vol|impact) = ").append(moActiveSells).append("|").append(Util.asBTC(moOutstandingSellVolume)).append("|").append(Util.asUSD(moSellTip)).append(nl)
		.append("takers, last 100 market orders: buys|sells = ").append(moLast100Buy).append("|").append(100-moLast100Buy).append(" filled|max buy vol = ").append(Util.asBTC(moLast100BuyVol)).append("|").append(Util.asBTC(moLast100BuyMax)).append(" filled|max sell vol = ").append(Util.asBTC(moLast100SellVol)).append("|").append(Util.asBTC(moLast100SellMax)).append(nl)
		.append("takers, last 100 trades: ").append(" buys|sells = ").append(moLast100BuyTrades).append("|").append(100-moLast100BuyTrades).append(" total|max buy vol = ").append(Util.asBTC(moLast100BuyTradeVol)).append("|").append(Util.asBTC(moLast100BuyTradeMax)).append(" total|max sell vol = ").append(Util.asBTC(moLast100SellTradeVol)).append("|").append(Util.asBTC(moLast100SellTradeMax)).append(nl)
		.append("makers, best bid|ask = ");
	
	    sb.append((bestBid==null?"na":Util.asUSD(bestBid.getPrice())));
	    sb.append("|").append((bestAsk==null?"na":Util.asUSD(bestAsk.getPrice())));


	    sb.append(" bid percentile vwap|orders|price_levels|volume = ");


	    sb.append(Percentile.statusString(bidPercentile)).append(" ask = ").append(Percentile.statusString(askPercentile));

       
	    sb.append(nl)
		.append("makers, visibility: lowest|highest price = ").append(Util.asUSD(lowestPrice)).append("|").append(Util.asUSD(highestPrice)).append(nl)
		.append("makers, total bids|asks = ").append(totalBids).append("|").append(totalAsks).append(" total bid|ask vol = ").append(Util.asBTC(totalBidVol)).append("|").append(Util.asBTC(totalAskVol)).append(nl)
		.append("makers, last 100 bid|ask cancellations: ").append(bidLast100Cancel).append("|").append(100-bidLast100Cancel).append(" removed bid vol|max = ").append(Util.asBTC(bidLast100CancelVolume)).append("|").append(Util.asBTC(bidLast100CancelMax)).append(" ask vol|max = ").append(Util.asBTC(askLast100CancelVolume)).append("|").append(Util.asBTC(askLast100CancelMax));

	    if(buyImpact!=null) {
		sb.append("\nbuy impact:  ");
		for(int i = 0, len = impactPoints.length; i< len;i++) {
		    sb.append((int)Math.pow(2,i)).append("(").append(Util.asUSD(buyImpact[i])).append(") ");
		}
	    }
	    if(sellImpact!=null) {
		sb.append("\nsell impact: ");
		for(int i = 0, len = impactPoints.length; i< len;i++) {
		    sb.append((int)Math.pow(2,i)).append("(").append(Util.asUSD(sellImpact[i])).append(") ");
		}
	    }
	
	    sb.append("\n")
		.append("total market buy_vol|buys. sell_vol|sells = ")
		.append(Util.asBTC(totalMoBuyVol)).append("|").append(totalMoBuys).append(" ")
		.append(Util.asBTC(totalMoSellVol)).append("|").append(totalMoSells);

	    return sb.toString();

	}catch(Exception e){
	    
	    System.err.println(e);

	    e.printStackTrace();

	    return "";
	}
    }

}
