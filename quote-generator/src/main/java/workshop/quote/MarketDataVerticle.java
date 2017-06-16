package workshop.quote;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import workshop.common.BaseRxMicroServiceVerticle;

import java.util.Objects;
import java.util.Random;

/**
 * Created by Fisher on 6/11/2017.
 */
public class MarketDataVerticle extends BaseRxMicroServiceVerticle {

    public static String ADDRESS = "market";

    String name;
    String symbol;
    int stocks;   // volume in the config
    double price;
    int variation;
    // not in the config
    long period;

    // calculate based on some simple logic
    private double value;

    private final Random random = new Random();
    // randomly generate:
    // the price of the stock when you buy them (seller price)
    double ask;
    // the price of the stock when you sell them (buyer price)
    double bid;

    // the number of stock that can be bought
    int share;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start();
        // get config from DeploymentOption.setConfig(config) in
        JsonObject config = config();
        init(config);

        // Every `period` ms, the given Handler is called.
        // it's still using handler
        vertx.setPeriodic(period, l->{
            compute();
            send();
        });

        System.out.println("name is " + name);
        // Publish the services in the discovery infrastructure.
        publishMessageSource("market-data-" + name, ADDRESS)
                .doOnSuccess(ign -> System.out.println("market data is successfully deployed"))
                .subscribe(ar -> startFuture.complete(),
                        error -> startFuture.fail(error.getCause()));
    }

    /**
     * Read the configuration and set the initial values.
     * @param config the configuration
     */
    void init(JsonObject config) {
        // variable in the config
        name = config.getString("name");
        Objects.requireNonNull(name);
        symbol = config.getString("symbol", name);
        stocks = config.getInteger("volume", 10000);  // volume in the config
        price = config.getDouble("price", 100.0);
        variation = config.getInteger("variation", 100);

        // variable not in the config -- update every 3 seconds
        period = config.getLong("period", 3000L);

        value = price;
        ask = price + random.nextInt(variation / 2);
        bid = price + random.nextInt(variation / 2);

        share = stocks / 2;
    }

    /**
     * Compute the new evaluation...
     */
    void compute() {

        if (random.nextBoolean()) {
            value = value + random.nextInt(variation);
            ask = value + random.nextInt(variation / 2);
            bid = value + random.nextInt(variation / 2);
        } else {
            value = value - random.nextInt(variation);
            ask = value - random.nextInt(variation / 2);
            bid = value - random.nextInt(variation / 2);
        }

        if (value <= 0) {
            value = 1.0;
        }
        if (ask <= 0) {
            ask = 1.0;
        }
        if (bid <= 0) {
            bid = 1.0;
        }

        if (random.nextBoolean()) {
            // Adjust share
            int shareVariation = random.nextInt(100);
            if (shareVariation > 0 && share + shareVariation < stocks) {
                share += shareVariation;
            } else if (shareVariation < 0 && share + shareVariation > 0) {
                share += shareVariation;
            }
        }
    }

    private void send(){
//        System.out.println(toJson());
        vertx.eventBus().send(ADDRESS, toJson());
    }

    /**
     * @return a json representation of the market data (quote). The structure is close to
     * <a href="https://en.wikipedia.org/wiki/Market_data">https://en.wikipedia.org/wiki/Market_data</a>.
     */
    private JsonObject toJson() {
        return new JsonObject()
                .put("exchange", "vert.x stock exchange")
                .put("symbol", symbol)
                .put("name", name)
                .put("bid", bid)
                .put("ask", ask)
                .put("volume", stocks)
                .put("open", price)
                .put("shares", share);

    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        super.stop(stopFuture);
    }
}
