package workshop.portfolio.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.rxjava.servicediscovery.types.HttpEndpoint;
import rx.Observable;
import rx.Single;
import workshop.portfolio.Portfolio;
import workshop.portfolio.PortfolioService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by Fisher on 6/15/2017.
 */
public class PortfolioServiceImpl implements PortfolioService {
    private final Vertx vertx;
    // at beginning, portfolio only has cash, later after buying and selling, portfolio state will be changed
    private final Portfolio portfolio;
    private final ServiceDiscovery discovery;

    // get the vertx and discovery from verticle
    // initialize portfolio, only cash, no share
    public PortfolioServiceImpl(Vertx vertx, ServiceDiscovery discovery, double initialCash) {
        this.vertx = vertx;
        this.portfolio = new Portfolio().setCash(initialCash);
        this.discovery = discovery;
    }

    @Override
    public void getPortfolio(Handler<AsyncResult<Portfolio>> resultHandler) {
        // client pass the handler as call back to this method
        // client.getPortfolio(result -> if(result is successful) print result)
        // -- this is the handler interface defined by lambda
        // so server side just need to pass the result to the handler and trigger the handler
        // and actually trigger the method defined at client side
        resultHandler.handle(Future.succeededFuture(portfolio));
    }

    @Override
    public void buy(int amount, JsonObject quote, Handler<AsyncResult<Portfolio>> resultHandler) {
        if (amount <= 0) {
            resultHandler.handle(Future.failedFuture("Cannot buy " + quote.getString("name") + " - the amount must be " +
                    "greater than 0"));
            return;
        }

        if (quote.getInteger("shares") < amount) {
            resultHandler.handle(Future.failedFuture("Cannot buy " + amount + " - not enough " +
                    "stocks on the market (" + quote.getInteger("shares") + ")"));
            return;
        }

        double price = amount * quote.getDouble("ask");
        String name = quote.getString("name");
        // 1) do we have enough money
        if (portfolio.getCash() >= price) {
            // Yes, buy it
            portfolio.setCash(portfolio.getCash() - price);
            int current = portfolio.getAmount(name);
            int newAmount = current + amount;
            portfolio.getShares().put(name, newAmount);
            sendActionOnTheEventBus("BUY", amount, quote, newAmount);
            resultHandler.handle(Future.succeededFuture(portfolio));
        } else {
            resultHandler.handle(Future.failedFuture("Cannot buy " + amount + " of " + name + " - " + "not enough money, " +
                    "need " + price + ", has " + portfolio.getCash()));
        }
    }

    @Override
    public void sell(int amount, JsonObject quote, Handler<AsyncResult<Portfolio>> resultHandler) {
        if (amount <= 0) {
            resultHandler.handle(Future.failedFuture("Cannot sell " + quote.getString("name") + " - the amount must be " +
                    "greater than 0"));
            return;
        }

        double price = amount * quote.getDouble("bid");
        String name = quote.getString("name");
        int current = portfolio.getAmount(name);
        // 1) do we have enough stocks
        if (current >= amount) {
            // Yes, sell it
            int newAmount = current - amount;
            if (newAmount == 0) {
                portfolio.getShares().remove(name);
            } else {
                portfolio.getShares().put(name, newAmount);
            }
            portfolio.setCash(portfolio.getCash() + price);
            sendActionOnTheEventBus("SELL", amount, quote, newAmount);
            resultHandler.handle(Future.succeededFuture(portfolio));
        } else {
            resultHandler.handle(Future.failedFuture("Cannot sell " + amount + " of " + name + " - " + "not enough stocks " +
                    "in portfolio"));
        }
    }

    private void sendActionOnTheEventBus(String action, int amount, JsonObject quote, int newAmount) {
        // TODO
        // ----
        //
        vertx.eventBus().publish(EVENT_ADDRESS, new JsonObject().put("action", action)
        .put("quote",quote)
        .put("date", System.currentTimeMillis())
        .put("amount",amount)
        .put("owned", newAmount));
        // ----
    }

    @Override
    public void evaluate(Handler<AsyncResult<Double>> resultHandler) {
        HttpEndpoint.rxGetWebClient(discovery, new JsonObject().put("name","quotes"))
                .flatMap(client -> computeEvaluation(client))
                .doOnError(error -> System.out.println(error))
                // convert from observable to handler
                .subscribe(RxHelper.toSubscriber(resultHandler));
    }

    private Single<Double> computeEvaluation(WebClient webClient) {
        // we need to call the service for each company we own shares
        return Observable.from(portfolio.getShares().entrySet())
                .flatMap(entry -> getValueForCompany(webClient,entry.getKey(),entry.getValue()).toObservable())
                .reduce((x,y) -> x+y)
                .toSingle();
    }

    private Single<Double> getValueForCompany(WebClient client, String company, int numberOfShares) {
//        // Create the future object that will  get the value once the value have been retrieved
//        Future<Double> future = Future.future();

        //TODO
        //----
        return client.get("/?name=" + encode(company))
                .rxSend()
                .map(resp -> resp.bodyAsJsonObject()
                .getDouble("bid"))
                .map(price -> Double.valueOf(price)*numberOfShares);
        // ---

//        return future;
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding");
        }
    }
}
