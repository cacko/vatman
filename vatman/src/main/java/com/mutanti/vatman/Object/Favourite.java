package com.mutanti.vatman.Object;

import android.os.Parcelable;

import com.google.android.maps.GeoPoint;
import com.mutanti.vatman.Interface.IFavourite;
import com.mutanti.vatman.Vatman;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public abstract class Favourite implements IFavourite, Parcelable {

    public static final NumberFormat FORMATTER_METERS = new DecimalFormat(
            "@@##");
    public static final NumberFormat FORMATTER_KILOMETERS = new DecimalFormat(
            "####.##");
    protected int mCode;
    protected String mName;
    protected double mDistance;
    protected double mBearing;
    protected VatmanLocation mLocation;
    protected float mDirection;
    protected boolean mIsFavourite = false;

    public Favourite(int code, String name, FavouriteProperties properties,
                     float[] distance, VatmanLocation location, boolean isFavourite) {
        mCode = code;
        mName = name;
        mDistance = distance[0];
        mBearing = distance[2];
        mDirection = (float) (((mBearing - Vatman.getHeading()) + 360) % 360);
        mIsFavourite = isFavourite;
        mLocation = location;
    }

    public Favourite() {
    }

    public void updateDirection() {
        mDirection = (float) (((mBearing - Vatman.getHeading()) + 360) % 360);
    }

    public float getDirection() {
        return mDirection;
    }

    public boolean isFavourite() {
        return mIsFavourite;
    }

    public int getCode() {
        return mCode;
    }

    public String getName() {
        return mName;
    }

    public double getDistance() {
        return mDistance;
    }

    public String getDistanceString() {
        String result = null;
        if (mDistance >= 1000) {
            result = FORMATTER_KILOMETERS.format(mDistance / 1000) + "km";
        } else if (mDistance > -1) {
            result = FORMATTER_METERS.format(mDistance) + "m";
        }
        return result;
    }

    public GeoPoint getGeoPoint() {
        return mLocation.getGeoPoint();
    }

}
