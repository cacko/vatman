package com.mutanti.vatman.Object;

import android.location.Location;

import com.google.android.maps.GeoPoint;

public class VatmanLocation {

    private double mLatitude;
    private double mLongitude;
    private int mLatitudeE6;
    private int mLongitudeE6;
    private GeoPoint mGeoPoint;

    public VatmanLocation(GeoPoint location) {
        mLatitudeE6 = location.getLatitudeE6();
        mLongitudeE6 = location.getLongitudeE6();
        mLatitude = mLatitudeE6 / 1E6;
        mLongitude = mLongitudeE6 / 1E6;
        initGeoPoint();
    }

    public VatmanLocation(Location location) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        mLatitudeE6 = (int) (mLatitude * 1E6);
        mLongitudeE6 = (int) (mLongitude * 1E6);
        initGeoPoint();
    }

    public VatmanLocation(double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
        mLatitudeE6 = (int) (mLatitude * 1E6);
        mLongitudeE6 = (int) (mLongitude * 1E6);
        initGeoPoint();
    }

    public VatmanLocation(int latitudeE6, int longitudeE6) {
        mLatitudeE6 = latitudeE6;
        mLongitudeE6 = longitudeE6;
        mLatitude = mLatitudeE6 / 1E6;
        mLongitude = mLongitudeE6 / 1E6;
        initGeoPoint();
    }

    private void initGeoPoint() {
        mGeoPoint = new GeoPoint(mLatitudeE6, mLongitudeE6);
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public int getLatitudeE6() {
        return mLatitudeE6;
    }

    public int getLongitudeE6() {
        return mLongitudeE6;
    }

    public GeoPoint getGeoPoint() {
        return mGeoPoint;
    }
}
