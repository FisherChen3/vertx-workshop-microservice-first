package workshop.portfolio;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Fisher on 6/15/2017.
 */
/**
 * Structure representing a portfolio. It stores the available cash and the owned shares.
 */
// need generatorConverter is true
@DataObject(generateConverter = true)
public class Portfolio {

    // why use treeMap?
    private Map<String, Integer> shares = new TreeMap<>();

    private double cash;

    public Portfolio() {
        // Empty constructor
    }

    public Portfolio(Portfolio other){
        // collectors are not immutable
        this.shares = new TreeMap<>(other.shares);
        this.cash = other.cash;
    }

    public Portfolio(JsonObject json) {
        PortfolioConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        PortfolioConverter.toJson(this, json);
        return json;
    }

    public Map<String, Integer> getShares() {
        return shares;
    }

    public Portfolio setShares(Map<String, Integer> shares) {
        this.shares = shares;
        return this;
    }

    public double getCash() {
        return cash;
    }

    public Portfolio setCash(double cash) {
        this.cash = cash;
        return this;
    }

    // -- additional methods:
    // get share amount by name
    public int getAmount(String name) {
        Integer current = shares.get(name);
        if (current == null) {
            return 0;
        }
        return current;
    }
}
