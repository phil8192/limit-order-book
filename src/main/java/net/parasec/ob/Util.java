package net.parasec.ob;

import net.parasec.trading.ticker.core.wire.Direction;
import net.parasec.trading.ticker.core.wire.Trade;


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

  public static StringBuilder tradeToCsv(final Trade trade, final String dl) {
    return (new StringBuilder().append(ts).append(dl)
        .append(asUSD(trade.getPrice())).append(dl)
        .append(asBTC(trade.getVolume())).append(dl)
        .append(trade.getDirection().equals(Direction.BUY) ? 1 : -1).append(dl)
        .append(trade.getTakerIdentifier()).append(dl)
        .append(trade.getMakerIdentifier())
    );
  }

  public static String tradeToString(final Trade trade) {
    final String nameLabel 
        = trade.getDirection().equals(Direction.BUY) ? "BUY  " : "SELL ";
    return nameLabel + asBTC(trade.getVolume()) + " @ $" + 
        asUSD(trade.getPrice()) + " maker = " + 
        trade.getMakerIdentifier() + " taker = " + 
        trade.getTakerIdentifier();
  }
}

