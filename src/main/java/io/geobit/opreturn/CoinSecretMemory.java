package io.geobit.opreturn;

import com.google.common.base.Optional;

import java.util.logging.Logger;

import io.geobit.opreturn.entity.CoinSecret;

/**
 * Created by Riccardo Casatta @RCasatta on 12/04/15.
 */
public class CoinSecretMemory extends MemoryHierarchy<CoinSecret> {
    private final static Logger log  = Logger.getLogger(CoinSecretMemory.class.getName());
    public CoinSecretMemory() {
        super.kind = "CoinsecretMemory";
    }

    @Override
    protected Optional<CoinSecret> getRemote(String height) {
        try {
            Long heightLong=Long.parseLong(height);
            final String urlString = "http://api.coinsecrets.org/block/" + height;
            Optional<String> optResult = Http.get(urlString);

            if (optResult.isPresent()) {
                final String s = optResult.get();
                log.info("got " + s);
                CoinSecret coinSecret=CoinSecret.fromJSON(s);
                if(coinSecret!=null)
                    return Optional.of(coinSecret);
            } else {
                log.warning("returning absent result " + urlString);
            }
        } catch (Exception e) {

        }
        return Optional.absent();
    }

    @Override
    protected Optional<CoinSecret> getDatastore(String key) {
        Long keyLong=Long.parseLong(key);
        CoinSecret coinSecret = OfyService.ofy().load().type(CoinSecret.class).id(keyLong).now();
        if(coinSecret!=null)
            return Optional.of(coinSecret);
        else
            return Optional.absent();
    }


    @Override
    protected Boolean cacheIf(CoinSecret coinSecret) {

        return coinSecret.getId()!=null;

    }
}
