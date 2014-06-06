package net.parasec.ob;

public final class Percentile {

    public final static double PERCENTILE_STEP_SIZE = 0.0025; 
    public final static int PERCENTILE_STEPS = 20;

    private final int vwap;
    private final int orders;
    private final int priceLevels;
    private final long volume;

    public Percentile(final int vwap, final int orders, final int priceLevels,
		      final long volume) {
	this.vwap = vwap;
	this.orders = orders;
	this.priceLevels = priceLevels;
	this.volume = volume;
    }

    public int getVwap() {
	return vwap;
    }

    public int getOrders() {
	return orders;
    }

    public int getIdPriceLevels() {
	return priceLevels;
    }

    public long getVolume() {
	return volume;
    }

    public StringBuilder toCsv(final String dl) {
	return (new StringBuilder().append(Util.asUSD(vwap)).append(dl)
		.append(orders).append(dl)
		.append(priceLevels).append(dl)
		.append(Util.asBTC(volume))
	);
    }

    public static String toCsv(final Percentile[] percentiles, final String dl) {
	final StringBuilder sb = new StringBuilder();
	for(int i = 0, len = percentiles.length; i<len; i++) {
	    final Percentile p = percentiles[i];
	    if(p!=null) {
		sb.append(p.toCsv(dl)).append(dl);
	    } else { 
		sb.append(dl).append(dl).append(dl).append(dl);
	    }

	}
	return sb.toString().substring(0, sb.length()-1);
    }

    public String toString() {
	return new StringBuilder().append(Util.asUSD(vwap)).append("|").append(orders)
	    .append("|").append(priceLevels).append("|").append(Util.asBTC(volume))
	    .toString();
    }

    public static String statusString(final Percentile[] percentiles) {
	if(percentiles==null || percentiles.length < 2)
	    return "-";
	final Percentile p1 = percentiles[0];
	final Percentile p2 = percentiles[1];

	int p1vwap = 0, p1orders = 0, p1levels = 0;
	int p2vwap = 0, p2orders = 0, p2levels = 0;
	long p1volume = 0, p2volume = 0;

	if(p1!=null) {
	    p1vwap = p1.getVwap(); 
	    p1orders = p1.getOrders();
	    p1levels = p1.getIdPriceLevels();
	    p1volume = p1.getVolume();
	}
	if(p2!=null) {
	    p2vwap = p2.getVwap(); 
	    p2orders = p2.getOrders();
	    p2levels = p2.getIdPriceLevels();
	    p2volume = p2.getVolume();
	}
	
	final long totVol = p1volume+p2volume;		
	final int vwapCombo = totVol > 0 ? (int)Math.round(((p1vwap*p1volume)+(p2vwap*p2volume))/(double)(totVol)) : 0;
	return Util.asUSD(vwapCombo) + "|" + (p1orders+p2orders) + "|" + (p1levels+p2levels) + "|" + Util.asBTC(totVol); 
					
    }

}
