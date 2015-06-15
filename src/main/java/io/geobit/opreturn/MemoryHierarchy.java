package io.geobit.opreturn;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.base.Optional;

import java.util.logging.Logger;

/**
 * Created by Riccardo Casatta @RCasatta on 12/04/15.
 */
public abstract class MemoryHierarchy<T> {
    private final static Logger log  = Logger.getLogger(MemoryHierarchy.class.getName());
    private MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    protected String kind;

    public void set(String key, T value) {
        final String composedKey = kind + "-" + key;

        if(memcache.get(composedKey)==null ) {
            memcache.put(composedKey,value);

            Optional<T> optData = getDatastore(key);
            if(!optData.isPresent()) {
                setDatastore(value);
            }
        }
    }

    public Optional<T> getCached(String key) {
        final String composedKey = kind + "-" + key;
        T cached = (T) memcache.get(composedKey);
        if(cached!=null) {
            return Optional.of( cached );
        } else {
            return Optional.absent();
        }
    }

    public void setCached(String key, T value) {
        final String composedKey = kind + "-" + key;
        memcache.put(composedKey,value);
    }

    public Optional<T> get(String key) {

        Optional<T> cached = getCached(key);
        if(cached.isPresent()) {
            return cached;
        }

        Optional<T> datastored = getDatastore(key);
        if(datastored.isPresent()) {
            log.info("got from datastore");
            if(cacheIf(datastored.get()))
                setCached(key,datastored.get());
            return datastored;
        }

        Optional<T> optRemote = getRemote(key);
        if(optRemote.isPresent()) {
            log.info("got from remote");
            T val = optRemote.get();
            if(cacheIf(val)) {
                setCached(key,val);
                setDatastore(val);
            }
            return optRemote;
        }

        log.warning("can't get key " + key);
        return Optional.absent();

    }

    protected Boolean cacheIf(T object) {
        return true;
    }

    protected abstract Optional<T> getRemote(String key);

    protected abstract Optional<T> getDatastore(String key);

    public void setDatastore(T value) {
        OfyService.ofy().save().entity(value).now();
    }


}
