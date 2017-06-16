package workshop.portfolio.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import workshop.portfolio.Portfolio;
import workshop.portfolio.PortfolioService;

/**
 * Created by Fisher on 6/15/2017.
 */
public class PortfolioServiceImpl implements PortfolioService {
    private final Vertx vertx;
    private final Portfolio portfolio;

    public PortfolioServiceImpl(Vertx vertx, Portfolio portfolio) {
        this.vertx = vertx;
        this.portfolio = portfolio;
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
}
