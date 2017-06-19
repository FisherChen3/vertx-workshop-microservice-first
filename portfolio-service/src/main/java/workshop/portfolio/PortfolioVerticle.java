package workshop.portfolio;


import io.vertx.core.Future;
import io.vertx.serviceproxy.ProxyHelper;
import workshop.common.BaseRxMicroServiceVerticle;
import workshop.portfolio.impl.PortfolioServiceImpl;

import static workshop.portfolio.PortfolioService.ADDRESS;
import static workshop.portfolio.PortfolioService.EVENT_ADDRESS;

/**
 * Created by Fisher on 6/18/2017.
 */
public class PortfolioVerticle extends BaseRxMicroServiceVerticle {

    @Override
    public void start(Future<Void> future) throws Exception {
        PortfolioService service = new PortfolioServiceImpl(vertx, discovery, config().getDouble("money", 10000.00));
        // Register service on the event bus using the ProxyHelper class:
        // we need to register our service first to an address, and then we can publish our service on this address
        ProxyHelper.registerService(PortfolioService.class, vertx.getDelegate(), service, ADDRESS);
        // publish this service proxy
        publishEventBusService("portfolio",ADDRESS, PortfolioService.class)
                // publish the message source of events of buying and selling
                // will be consumed by Audit
                .flatMap(ign -> publishMessageSource("portfolio-events", EVENT_ADDRESS))
                .doOnError(error -> System.out.println(error.getCause()))
                .subscribe(ar -> future.complete(), future::fail);
    }
}
