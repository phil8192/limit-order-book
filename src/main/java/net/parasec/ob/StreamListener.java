package net.parasec.ob;

public interface StreamListener {
 
  void onNewOrder(JSON.Order o);
  void onModOrder(JSON.Order o);
  void onDelOrder(JSON.Order o);

}
