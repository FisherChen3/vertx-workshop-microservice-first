package workshop.quote;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import workshop.common.BaseRxMicroServiceVerticle;

/**
 * Created by Fisher on 6/11/2017.
 */
public class QuoteConfigVerticle extends BaseRxMicroServiceVerticle {

    @Override
    public void start() throws Exception{
        super.start();
        // Read the configuration, and deploy a MarketDataVerticle for each company listed in the configuration.
        JsonArray quotes = config().getJsonArray("companies");
        for (Object q : quotes) {
            JsonObject company = (JsonObject) q;
            // Deploy the verticle with a configuration.
            vertx.deployVerticle(MarketDataVerticle.class.getName(), new DeploymentOptions().setConfig(company));
        }

        // Deploy another verticle
        vertx.deployVerticle(RestAPIQuoteVerticle.class.getName(), new DeploymentOptions().setConfig(config()));
    }

    @Override
    public void stop(Future<Void> future) throws Exception {
        super.stop(future);
    }
}
