package workshop.common;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.rxjava.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.servicediscovery.types.MessageSource;
import rx.Single;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Fisher on 6/10/2017.
 */
public class BaseRxMicroServiceVerticle extends AbstractVerticle {

    protected ServiceDiscovery discovery;
//    protected CircuitBreaker circuitBreaker;
    protected Set<Record> registeredRecords = new ConcurrentHashSet<>();

    @Override
    public void start() throws Exception {

        // init service discovery instance
        discovery = ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions().setBackendConfiguration(config()));

        // looks like circitBreaker will not work on cluster mode
//        // init circuit breaker instance
//        JsonObject cbOptions = config().getJsonObject("circuit-breaker") != null ?
//                config().getJsonObject("circuit-breaker") : new JsonObject();
//        circuitBreaker = CircuitBreaker.create(cbOptions.getString("name", "circuit-breaker"), vertx,
//                new CircuitBreakerOptions()
//                        .setMaxFailures(cbOptions.getInteger("max-failures", 5))
//                        .setTimeout(cbOptions.getLong("timeout", 10000L))
//                        .setFallbackOnFailure(true)
//                        .setResetTimeout(cbOptions.getLong("reset-timeout", 30000L))
//        );
    }

    protected Single<Void> publishHttpEndpoint(String name, String host, int port) {
        Record record = HttpEndpoint.createRecord(name, host, port, "/",
                new JsonObject().put("api.name", config().getString("api.name", ""))
        );
        return publish(record);
    }

    protected Single<Void> publishApiGateway(String host, int port) {
        Record record = HttpEndpoint.createRecord("api-gateway", true, host, port, "/", null)
                .setType("api-gateway");
        return publish(record);
    }

    public Single<Void> publishEventBusService(String name, String address, Class serviceClass) {
        Record record = EventBusService.createRecord(name, address, serviceClass);
        return publish(record);
    }

    public Single<Void> publishMessageSource(String name, String address) {
        Record record = MessageSource.createRecord(name, address);
        return publish(record);
    }

    protected Single<Void> publish(Record record) {
        if (discovery == null) {
            try {
                start();
            } catch (Exception e) {
                throw new RuntimeException("Cannot create discovery service");
            }
        }

        return discovery.rxPublish(record)
                .doOnSuccess(r -> registeredRecords.add(r))
                // map(null) is not working, will throw some strange error somehow
                .map(ign -> (Void)null);
    }

    @Override
    public void stop(Future<Void> future) throws Exception {
//        Observable.from(registeredRecords)
//                .flatMap(record -> discovery.rxUnpublish(record.getRegistration()).toObservable())
//                .doOnTerminate(() -> discovery.close())
//                .subscribe(future::complete,future::fail);
        // the above rx version will cause system not responding when stopping the app
        List<Future> futures = new ArrayList<>();
        for (Record record : registeredRecords) {
            Future<Void> unregistrationFuture = Future.future();
            futures.add(unregistrationFuture);
            discovery.unpublish(record.getRegistration(), unregistrationFuture);
        }

        if (futures.isEmpty()) {
            discovery.close();
            future.complete();
        } else {
            CompositeFuture composite = CompositeFuture.all(futures);
            composite.setHandler(ar -> {
                discovery.close();
                if (ar.failed()) {
                    future.fail(ar.cause());
                } else {
                    future.complete();
                }
            });
        }
    }
}
