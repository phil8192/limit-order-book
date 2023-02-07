package net.parasec.ob;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;


public class HttpLob {

	private final static String ENDPOINT = "https://www.bitstamp.net/api/v2/order_book/";

	static class Order {
		String orderId;
		String price;
		String volume;

		@Override
		public String toString() {
			return "Order{" +
					"orderId='" + orderId + '\'' +
					", price='" + price + '\'' +
					", volume='" + volume + '\'' +
					'}';
		}
	}

	static class Lob {
		List<Order> asks;
		List<Order> bids;

		@Override
		public String toString() {
			return "Lob{" +
					"asks=" + asks +
					", bids=" + bids +
					'}';
		}
	}

	private List<Order> parseOrders(JsonNode jsonNodeOrders) {
		List<Order> orders = new ArrayList<>();
		for (JsonNode jsonOrder : jsonNodeOrders) {
			Order order = new Order();
			order.orderId = jsonOrder.get(2).asText();
			order.price = jsonOrder.get(0).asText();
			order.volume = jsonOrder.get(1).asText();
			orders.add(order);
		}
		return orders;
	}

	private Lob parseLob(JsonNode jsonNodeLob) {
		Lob lob = new Lob();
		lob.asks = parseOrders(jsonNodeLob.path("asks"));
		lob.bids = parseOrders(jsonNodeLob.path("bids"));
		return lob;
	}

	private Lob parseLob(String body) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(body);
		return parseLob(rootNode);
	}

	private String httpGet(String symbol) throws URISyntaxException, IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(new URI(ENDPOINT + symbol + "?group=2"))
				.header("Accept", "application/json")
				.build();
		HttpClient client = HttpClient.newBuilder().build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		return response.body();
	}

	Lob getLob(String symbol) {
		try {
			String resp = httpGet(symbol);
			return parseLob(resp);
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		return null;
	}

	public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
		HttpLob httpLob = new HttpLob();
		Lob lob = httpLob.getLob("btcusd");
		System.out.print(lob);
	}
}
