package net.parasec.ob;

public interface OMS {
    /* src = (optional) known order source */

    /* new order */
    void addOrder(String src, String id, int orderId, OrderType type, 
		  long exchangeTimestamp, long localTimestamp, double volume, 
		  double price);

    /* new order volume (0 = complete fill) */
    void modOrder(String src, String id, int orderId, OrderType type, 
		  long exchangeTimestamp, long localTimestamp, double volume, 
		  double price);

    /* 0 = complete fill, >0 = partial or no fill */
    /*
      void remOrder(String id, int orderId, OrderType type, long localTimestamp,
      boolean completeFill, double price);
    */
    void delOrder(String src, String id, int orderId, OrderType type, 
		  long exchangeTimestamp, long localTimestamp, double volume, 
		  double price);


}
