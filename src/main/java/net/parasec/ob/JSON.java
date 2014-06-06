package net.parasec.ob;

public final class JSON {  
    private final static com.google.gson.Gson GSON 
	= new com.google.gson.Gson();

    static final class Balance {
	private double usd_balance;
	private double btc_balance;
	private double usd_reserved;
	private double btc_reserved;
	private double usd_available;
	private double btc_available;
	private double fee;


	public Balance(){}

	public double getUsdBalance() {
	    return usd_balance;
	}

	public double getBtcBalance() {
	    return btc_balance;
	}

	public double getUsdReserved() {
	    return usd_reserved;
	}

	public double getBtcReserved() {
	    return btc_reserved;
	}

	public double getUsdAvailable() {
	    return usd_available;
	}

	public double getBtcAvailable() {
	    return btc_available;
	}

	public double getFee() {
	    return fee;
	}
	
	public String toString() {
	    return "usd_balance: $" + String.format("%.2f", usd_balance) + 
		" btc_balance: " + String.format("%.8f", btc_balance) + 
		" usd_reserved: $" + String.format("%.2f", usd_reserved) + 
		" btc_reserved: " + String.format("%.8f", btc_reserved) + 
		" usd_available: $" + String.format("%.2f", usd_available) + 
		" btc_available: " + String.format("%.8f", btc_available) + 
		" fee: " + String.format("%.2f", fee) + "%";
	}

    };
    public static JSON.Balance parseBalance(final String s) {
	return GSON.fromJson(s, JSON.Balance.class);
    }    

    static final class Order {
	private int order_type;
	private double price;
	private double amount;
	private long datetime;
	private int id;

	
	public Order(){}

	public int getType() {
	    return order_type;
	}

	public double getPrice() {
	    return price;
	}

	public double getAmount() {
	    return amount;
	}

	public long getDatetime() {
	    return datetime;
	}

	public int getId() {
	    return id;
	}
    };
    public static JSON.Order parseOrder(final String s) {
	return GSON.fromJson(s, JSON.Order.class);
    }

    static final class Trade {
	private double price;
	private double amount;
	private String id;


	public Trade(){}

	public double getPrice() {
	    return price;
	}

	public double getAmount() {
	    return amount;
	}

	public String getId() {
	    return id;
	}
    };
    public static JSON.Trade parseTrade(final String s) {
	return GSON.fromJson(s, JSON.Trade.class);
    }

    static final class ExchangeOrder {
	private double price;
	private double amount;
	private int type;
	private int id;
	private String datetime;

	private String orig;

	public ExchangeOrder(){}

	public double getPrice() {
	    return price;
	}

	public double getAmount() {
	    return amount;
	}

	public int getType() {
	    return type;
	}

	public int getId() {
	    return id;
	}

	public String getDateTime() {
	    return datetime;
	}

	public String getOrig() {
	    return orig;
	}

	public ExchangeOrder setOrig(final String orig) {
	    this.orig = orig;
	    return this;
	}

    };
    public static JSON.ExchangeOrder parseExchangeOrder(final String s) {
	return GSON.fromJson(s, JSON.ExchangeOrder.class);
    }

    static final class Ob {
	private double[][] bids;
	private double[][] asks;

	
	public Ob(){}

	public double[][] getBids() {
	    return bids;
	}

	public double[][] getAsks() {
	    return asks;
	}
    };
    public static JSON.Ob parseOb(final String s) {
	return GSON.fromJson(s, JSON.Ob.class);
    }

}
