package dev.zerorsen.rgoldenheads;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The live item set. Commands hold an item ID rather than a spec, so a reload can swap
 * the whole map at once and every command serves the new values on its next execution.
 */
public final class ConsumableRegistry {

    private volatile Map<String, ConsumableSpec> byId = Map.of();

    public void replaceAll(List<ConsumableSpec> specs) {
        Map<String, ConsumableSpec> next = new LinkedHashMap<>();
        for (ConsumableSpec spec : specs) {
            next.put(spec.id(), spec);
        }
        this.byId = Collections.unmodifiableMap(next);
    }

    /** Null when the item was disabled or removed since the server started. */
    public ConsumableSpec byId(String id) {
        return byId.get(id);
    }

    public List<ConsumableSpec> all() {
        return List.copyOf(byId.values());
    }

    public Set<String> ids() {
        return byId.keySet();
    }
}
