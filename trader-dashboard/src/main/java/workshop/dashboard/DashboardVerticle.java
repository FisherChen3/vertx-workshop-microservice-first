package workshop.dashboard;


import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.rxjava.circuitbreaker.CircuitBreaker;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.handler.StaticHandler;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.rxjava.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.rest.ServiceDiscoveryRestEndpoint;
import workshop.common.BaseRxMicroServiceVerticle;

/**
 * Created by Fisher on 6/18/2017.
 */
public class DashboardVerticle extends BaseRxMicroServiceVerticle {

    private CircuitBreaker circuit;
    private WebClient client;

    @Override
    public void start(Future<Void> future) throws Exception{
        super.start();
        Router router = Router.router(vertx);

        // Event bus bridge
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        BridgeOptions options = new BridgeOptions();
        options
                .addOutboundPermitted(new PermittedOptions().setAddress("market"))
                .addOutboundPermitted(new PermittedOptions().setAddress("portfolio"))
                .addOutboundPermitted(new PermittedOptions().setAddress("service.portfolio"))
                .addInboundPermitted(new PermittedOptions().setAddress("service.portfolio"))
                .addOutboundPermitted(new PermittedOptions().setAddress("vertx.circuit-breaker"));

        sockJSHandler.bridge(options);
        router.route("/eventbus/*").handler(sockJSHandler);

        // Discovery endpoint
        ServiceDiscoveryRestEndpoint.create(router.getDelegate(), discovery.getDelegate());

        // Last operations
        router.get("/operations").handler(this::callAuditService);

        // Static content
        router.route("/*").handler(StaticHandler.create());

        // Create a circuit breaker.
        circuit = CircuitBreaker.create("http-audit-service", vertx,
                new CircuitBreakerOptions()
                        .setMaxFailures(2)
                        .setFallbackOnFailure(true)
                        .setResetTimeout(2000)
                        .setTimeout(1000))
                .openHandler(v -> retrieveAuditService());

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080, ar -> {
                    if (ar.failed()) {
                        future.fail(ar.cause());
                    } else {
                        retrieveAuditService();
                        future.complete();
                    }
                });
    }

    @Override
    public void stop() throws Exception {
        if (client != null) {
            client.close();
        }
        circuit.close();
    }

    private Future<Void> retrieveAuditService() {
        return Future.future(future -> {
            HttpEndpoint.getWebClient(discovery, new JsonObject().put("name", "audit"), client -> {
                this.client = client.result();
                future.handle(client.map((Void)null));
            });
        });
    }


    private void callAuditService(RoutingContext context) {
        if (client == null) {
            context.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(200)
                    .end(new JsonObject().put("message", "No audit service").encode());
        } else {
            client.get("/").send(ar -> {
                if (ar.succeeded()) {
                    HttpResponse<Buffer> response = ar.result();
                    context.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .end(response.body());
                }
            });
        }
    }

}