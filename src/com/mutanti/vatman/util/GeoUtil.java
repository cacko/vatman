package com.mutanti.vatman.util;

import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.mutanti.vatman.Object.VatmanLocation;

public class GeoUtil {

    public static double[] getBoundingBox(final Location pLocation,
                                          final int pDistanceInMeters) {

        final double pLatitude = pLocation.getLatitude();
        final double pLongitude = pLocation.getLongitude();

        final double[] boundingBox = new double[4];

        final double latRadian = Math.toRadians(pLatitude);

        final double degLatKm = 110.574235;
        final double degLongKm = 110.572833 * Math.cos(latRadian);
        final double deltaLat = pDistanceInMeters / 1000.0 / degLatKm;
        final double deltaLong = pDistanceInMeters / 1000.0 / degLongKm;

        final double minLat = pLatitude - deltaLat;
        final double minLong = pLongitude - deltaLong;
        final double maxLat = pLatitude + deltaLat;
        final double maxLong = pLongitude + deltaLong;

        boundingBox[0] = minLat;
        boundingBox[1] = minLong;
        boundingBox[2] = maxLat;
        boundingBox[3] = maxLong;

        return boundingBox;
    }

    public static double[] getBoundingBox(VatmanLocation center,
                                          int latitudeSpan, int longitudeSpan) {

        if (latitudeSpan == 0 && longitudeSpan == 360E6) {
            return null;
        }

        double lat1 = (center.getLatitudeE6() - latitudeSpan / 2) / 1E6;
        double lat2 = (center.getLatitudeE6() + latitudeSpan / 2) / 1E6;
        double lon1 = (center.getLongitudeE6() - longitudeSpan / 2) / 1E6;
        double lon2 = (center.getLongitudeE6() + longitudeSpan / 2) / 1E6;

        final double[] boundingBox = new double[4];
        boundingBox[0] = Math.min(lat1, lat2);
        boundingBox[1] = Math.min(lon1, lon2);
        boundingBox[2] = Math.max(lat1, lat2);
        boundingBox[3] = Math.max(lon1, lon2);

        return boundingBox;
    }

    public static double[] getBoundingBox(GeoPoint topLeft, GeoPoint bottomRight) {
        VatmanLocation VatmanTopLeft = new VatmanLocation(topLeft);
        VatmanLocation VatmanBottomRight = new VatmanLocation(bottomRight);
        final double[] boundingBox = new double[4];
        boundingBox[0] = Math.min(VatmanTopLeft.getLatitude(),
                VatmanBottomRight.getLatitude());
        boundingBox[1] = Math.min(VatmanTopLeft.getLongitude(),
                VatmanBottomRight.getLongitude());
        boundingBox[2] = Math.max(VatmanTopLeft.getLatitude(),
                VatmanBottomRight.getLatitude());
        boundingBox[3] = Math.max(VatmanTopLeft.getLongitude(),
                VatmanBottomRight.getLongitude());
        return boundingBox;
    }

}
