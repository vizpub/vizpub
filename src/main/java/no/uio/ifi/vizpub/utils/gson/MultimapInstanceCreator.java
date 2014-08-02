package no.uio.ifi.vizpub.utils.gson;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.InstanceCreator;

import java.lang.reflect.Type;

/**
 * @author Nils Peder Korsveien
 */
public class MultimapInstanceCreator implements InstanceCreator<Multimap> {
    @Override
    public Multimap createInstance(Type type) {
        return HashMultimap.create();
    }
}
