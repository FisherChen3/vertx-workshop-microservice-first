package workshop.quote;

import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.servicediscovery.types.MessageSource;
import workshop.common.BaseRxMicroServiceVerticle;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Fisher on 6/11/2017.
 */
public class RestAPIQuoteVerticle extends BaseRxMicroServiceVerticle {

    private Map<String, JsonObject> quotes = new HashMap<>();

    @Override
    public void start(Future<Void> startFuture) throws Exception{
        super.start();
        MessageSource.<JsonObject>rxGetConsumer(discovery, new JsonObject().put("name", "market-data-MacroHard"))
                .map(mConsumer -> mConsumer.handler(msg -> {
                    JsonObject quote = msg.body();
                    // I will receive all the quotes, not just MacroHard, because I am actually getting the address
                    // of the event bus, so I will receive 3 quotes being sent by the 3 company
//                    System.out.println("recevied quote " + quote);
                    quotes.put(quote.getString("name"),quote);
                }))
                .map(ign ->
                    vertx.createHttpServer()
                            .requestHandler(request -> {
                                HttpServerResponse response = request.response()
                                        .putHeader("content-type", "application/json");

                                // request could specify the company name
                                String company = request.getParam("name");
                                // if company name is not provided by the client
                                // respond all the company data
                                if(company==null){
                                    // Json static method
                                    String content = Json.encodePrettily(quotes);
                                    response.end(content);
                                } else {
                                    JsonObject quote = quotes.get(company);
                                    if(quote==null) {
                                        response.setStatusCode(404).end();
                                    } else {
                                        // jsonObject instance method
                                        response.end(quote.encodePrettily());
                                    }
                                }
                            })
                )
                .flatMap(httpServer -> httpServer.rxListen(config().getInteger("http.port",8080)))
                .doOnSuccess(httpServre -> System.out.println("server started on port" + httpServre.actualPort()))
                .map(ign -> publishHttpEndpoint("quotes","localhost",config().getInteger("http.port",8080)))
                .subscribe(ar -> startFuture.complete(), startFuture::fail);
    }


    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        super.stop(stopFuture);
    }

}
