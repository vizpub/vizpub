package no.uio.ifi.vizpub.utils.gson;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class needed for deserializing the guava multimap data structure using gson
 *
 * @author Nils Peder Korsveien
 */
public class MultimapDeserializer implements JsonDeserializer<Multimap<?, ?>> {
    @Override
    public Multimap<?, ?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        final Multimap<String, String> map = Multimaps.newMultimap(new HashMap<String, Collection<String>>(), new Supplier<Set<String>>() {
            public Set<String> get() {
                return Sets.newHashSet();
            }
        });
        for (Map.Entry<String, JsonElement> entry : ((JsonObject) json).entrySet()) {
            for (JsonElement element : (JsonArray) entry.getValue()) {
                map.get(entry.getKey()).add(element.getAsString());
            }
        }
        return map;
    }
}
