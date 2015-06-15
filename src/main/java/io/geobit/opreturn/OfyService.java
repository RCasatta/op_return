package io.geobit.opreturn;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

import io.geobit.opreturn.entity.CoinSecret;
import io.geobit.opreturn.entity.CoinSecretGrouped;


public class OfyService {
    static {
        factory().register(CoinSecret.class);
        factory().register(CoinSecretGrouped.class);
    }

    public static Objectify ofy() {
        return ObjectifyService.ofy();
    }

    public static ObjectifyFactory factory() {
        return ObjectifyService.factory();
    }
}