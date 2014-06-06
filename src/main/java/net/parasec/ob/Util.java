package net.parasec.ob;

public final class Util {

    public static long asSatoshi(final double btc) {
	return (long) Math.round(btc*100000000);
    }

    public static int asCents(final double dollar) {
	return (int) Math.round(dollar*100);
    }

    public static String asBTC(final long satoshi) {
	return String.format("%.8f", satoshi*0.00000001);
    }

    public static String asUSD(final int cents) {
	return String.format("%.2f", cents*0.01);
    }

}
