package net.parasec.ob;

public final class JSON {  
    private final static com.google.gson.Gson GSON 
	= new com.google.gson.Gson();

    public static final class Order {
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

    public static final class Trade {
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

    public static final class Ob {
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
