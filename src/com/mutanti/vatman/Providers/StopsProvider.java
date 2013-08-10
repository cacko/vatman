package com.mutanti.vatman.Providers;

import com.mutanti.vatman.Object.Favourite;
import com.mutanti.vatman.Object.VatmanLocation;

import java.util.ArrayList;

public class StopsProvider {

    private static StopsProvider sInstance = null;
    private Db mDb;

    private StopsProvider(Db db) {
        mDb = db;
    }

    public static StopsProvider getInstance(Db db) {
        if (sInstance == null) {
            sInstance = new StopsProvider(db);
        }
        return sInstance;
    }

    public ArrayList<Favourite> getStops(VatmanLocation location,
                                         double[] boundingBox, boolean includeFavourites) {
        return (mDb != null) ? mDb.getFavourites(location, boundingBox, includeFavourites) : new ArrayList<Favourite>();
    }

    public ArrayList<Favourite> getStops(VatmanLocation location,
                                         double[] boundingBox) {
        return getStops(location, boundingBox, true);
    }

}
