package workshop.portfolio;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * Created by Fisher on 6/15/2017.
 */
public interface PortfolioService {

    void getPortfolio(Handler<AsyncResult<Portfolio>> resultHandler);
}
