package com.mutanti.vatman.Providers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.mutanti.vatman.Object.Favourite;
import com.mutanti.vatman.Object.FavouriteProperties;
import com.mutanti.vatman.Object.FavouriteStop;
import com.mutanti.vatman.Object.VatmanLocation;
import com.mutanti.vatman.Vatman;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

@SuppressWarnings("deprecation")
public class Db {

    public static final String DB_NAME = "vatman";
    public static final String DB_TABLE_STOPS = "stops";
    public static final String DB_TABLE_FAVORITES = "favorites";
    public final static String STOPS_COL_ID = "_id";
    public final static String STOPS_COL_NAME = "name";
    public final static String STOPS_COL_ROUTE = "route";
    public final static String STOPS_COL_LINES = "lines";
    public final static String STOPS_COL_LAT = "lat";
    public final static String STOPS_COL_LON = "lon";
    public final static String STOPS_COL_LINESROUTES = "linesRoute";
    public final static String STOPS_COL_IS_ACTIVE = "isActive";
    public final static String COL_ROW_ID = "docid";
    public final static String FAVOURITES_COL_STOP_ID = "_id";
    public final static int TABLE_VERSION_STOPS = 2;
    private static final NumberFormat CODE_FORMATTER = new DecimalFormat("0000");
    private static Db mInstance;
    private final Handler mHandler;
    private SQLiteDatabase mDB;
    private String[] stopsColumns = new String[]{STOPS_COL_ID,
            STOPS_COL_NAME, STOPS_COL_ROUTE, STOPS_COL_LINES,
            STOPS_COL_LINESROUTES, STOPS_COL_IS_ACTIVE, STOPS_COL_LAT,
            STOPS_COL_LON};
    private InsertHelper mAddStopHelper = null;
    private HashMap<String, Integer> mAddStopColumns;

    private Db(SQLiteDatabase db, Handler handler) {
        mHandler = handler;
        mDB = db;
    }

    public static Db initInstance(SQLiteDatabase db, Handler handler) {
        mInstance = new Db(db, handler);
        return mInstance;
    }

    public static Db getInstance() {
        return mInstance;
    }

    public void close() {
        mDB.close();
    }

    public void initTableStops() {
        mDB.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_STOPS);
        mDB.execSQL("CREATE VIRTUAL TABLE " + DB_TABLE_STOPS + " USING fts3("
                + STOPS_COL_NAME + " TEXT, " + STOPS_COL_ROUTE + " TEXT, "
                + STOPS_COL_LINES + " TEXT, " + STOPS_COL_LINESROUTES
                + " TEXT, " + STOPS_COL_IS_ACTIVE + " INTEGER, "
                + STOPS_COL_LAT + " FLOAT, " + STOPS_COL_LON + " FLOAT, "
                + STOPS_COL_ID + " INTEGER PRIMARY KEY)");
    }

    public void initTableFavourites() {
        mDB.execSQL("CREATE TABLE IF NOT EXISTS " + DB_TABLE_FAVORITES + "("
                + FAVOURITES_COL_STOP_ID + " INTEGER PRIMARY KEY)");
    }

    public void init() {
        initTableStops();
        initTableFavourites();
    }

    public boolean isFavourite(int code) {
        Cursor cursor = getCursor("SELECT count(*) `total` FROM "
                + DB_TABLE_FAVORITES + " WHERE " + FAVOURITES_COL_STOP_ID + "="
                + code, null);
        cursor.moveToFirst();
        int result = cursor.getInt(0);
        cursor.close();
        return (result > 0);
    }

    private InsertHelper getAddStopHelper() {
        if (mAddStopHelper == null) {
            mAddStopHelper = new InsertHelper(mDB, DB_TABLE_STOPS);
            mAddStopColumns = new HashMap<String, Integer>();
            mAddStopColumns.put(STOPS_COL_NAME,
                    mAddStopHelper.getColumnIndex(STOPS_COL_NAME));
            mAddStopColumns.put(STOPS_COL_ROUTE,
                    mAddStopHelper.getColumnIndex(STOPS_COL_ROUTE));
            mAddStopColumns.put(STOPS_COL_LINES,
                    mAddStopHelper.getColumnIndex(STOPS_COL_LINES));
            mAddStopColumns.put(STOPS_COL_LINESROUTES,
                    mAddStopHelper.getColumnIndex(STOPS_COL_LINESROUTES));
            mAddStopColumns.put(STOPS_COL_IS_ACTIVE,
                    mAddStopHelper.getColumnIndex(STOPS_COL_IS_ACTIVE));
            mAddStopColumns.put(STOPS_COL_LAT,
                    mAddStopHelper.getColumnIndex(STOPS_COL_LAT));
            mAddStopColumns.put(STOPS_COL_LON,
                    mAddStopHelper.getColumnIndex(STOPS_COL_LON));
            mAddStopColumns.put(STOPS_COL_ID,
                    mAddStopHelper.getColumnIndex(STOPS_COL_ID));
        }
        return mAddStopHelper;
    }

    public void startOperation() {
        mDB.beginTransaction();
    }

    public void endOperation() {
        if (mAddStopHelper != null) {
            mAddStopHelper.close();
            mAddStopHelper = null;
        }
        mDB.setTransactionSuccessful();
        mDB.endTransaction();
    }

    public long addStop(int code, String name, String route, String lines,
                        String linesRoute, int isActive, double lat, double lon,
                        int operation) {
        String routes = "";
        for (String r : route.split("\\|")) {
            routes += (routes.length() == 0) ? r : " / " + r;
        }
        String linesValue = "";
        for (String r : lines.split("\\|")) {
            linesValue += (linesValue.length() == 0) ? r : " / " + r;
        }
        InsertHelper ih = getAddStopHelper();
        ih.prepareForInsert();
        ih.bind(mAddStopColumns.get(STOPS_COL_NAME),
                "" + CODE_FORMATTER.format(code) + " - " + name.toUpperCase());
        ih.bind(mAddStopColumns.get(STOPS_COL_ROUTE), routes.toUpperCase());
        ih.bind(mAddStopColumns.get(STOPS_COL_LINES), linesValue.toUpperCase());
        ih.bind(mAddStopColumns.get(STOPS_COL_LINESROUTES),
                linesRoute.toUpperCase());
        ih.bind(mAddStopColumns.get(STOPS_COL_IS_ACTIVE), isActive);
        ih.bind(mAddStopColumns.get(STOPS_COL_LAT), lat);
        ih.bind(mAddStopColumns.get(STOPS_COL_LON), lon);
        ih.bind(mAddStopColumns.get(STOPS_COL_ID), code);
        long res = ih.execute();
        if (mHandler != null) {

            mDB.yieldIfContendedSafely();

            mHandler.sendEmptyMessage(operation);
        }
        return res;

    }

    public long updateStop(int code, String name, String route, String lines,
                           String linesRoute, int isActive, double lat, double lon,
                           int operation) {
        String routes = "";
        for (String r : route.split("\\|")) {
            routes += (routes.length() == 0) ? r : " / " + r;
        }
        String linesValue = "";
        for (String r : lines.split("\\|")) {
            linesValue += (linesValue.length() == 0) ? r : " / " + r;
        }
        ContentValues values = new ContentValues();
        values.put(STOPS_COL_NAME, "" + CODE_FORMATTER.format(code) + " - "
                + name.toUpperCase());
        values.put(STOPS_COL_ROUTE, routes.toUpperCase());
        values.put(STOPS_COL_LINES, linesValue.toUpperCase());
        values.put(STOPS_COL_LINESROUTES, linesRoute.toUpperCase());
        values.put(STOPS_COL_IS_ACTIVE, isActive);
        values.put(STOPS_COL_LAT, lat);
        values.put(STOPS_COL_LON, lon);
        values.put(STOPS_COL_ID, code);
        int res = mDB.update(DB_TABLE_STOPS, values, COL_ROW_ID + "=?",
                new String[]{Integer.toString(code)});
        if (mHandler != null) {
            try {
                mDB.yieldIfContendedSafely();
            } catch (Exception e) {

            }
            mHandler.sendEmptyMessage(operation);
        }
        return res;

    }

    public Cursor getStopsCursor() {
        Cursor cursor = null;
        try {
            cursor = mDB.query(DB_TABLE_STOPS, stopsColumns, null, null, null,
                    null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cursor;
    }

    public Cursor getStopsFilterCursor(String filter) {
        final String selection = STOPS_COL_NAME + " MATCH \'" + filter + "*\'";
        return mDB.query(DB_TABLE_STOPS, stopsColumns, selection, null, null,
                null, null);
    }

    private void sendMessage(int operation, String msg) {
        Message message = new Message();
        Bundle data = new Bundle();
        data.putString(Vatman.BUNDLE_ARG_MESSAGE, msg);
        message.setData(data);
        message.what = operation;
        mHandler.sendMessage(message);
    }

    public String getStopName(int stop) {
        Cursor cursor = mDB.query(DB_TABLE_STOPS,
                new String[]{STOPS_COL_NAME}, STOPS_COL_ID + "=" + stop,
                null, null, null, null);
        cursor.moveToFirst();
        String result = cursor.getString(0);
        cursor.close();
        return result;
    }

    public boolean addFavouriteStop(int stop) {
        try {
            ContentValues values = new ContentValues();
            values.put(FAVOURITES_COL_STOP_ID, stop);
            mDB.insert(DB_TABLE_FAVORITES, null, values);
            sendMessage(Vatman.OPERATION_ADD_FAVOURITE, "Добавена спирка "
                    + getStopName(stop));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean removeFavourite(int stop) {
        try {
            mDB.delete(DB_TABLE_FAVORITES, FAVOURITES_COL_STOP_ID + "=?",
                    new String[]{"" + stop});
            sendMessage(Vatman.OPERATION_REMOVE_FAVOURITE, "Премахната спирка "
                    + getStopName(stop));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private Favourite getFavouriteFromCursor(Cursor cursor,
                                             VatmanLocation location, boolean isFavourite) {
        final String stopName = cursor.getString(0);
        final String routeName = cursor.getString(1);
        final String linesName = cursor.getString(2);
        final int code = cursor.getInt(3);
        final Double latitude = cursor.getDouble(4);
        final Double longitude = cursor.getDouble(5);
        final String linesRoutes = cursor.getString(6);
        final VatmanLocation stopLocation = new VatmanLocation(latitude,
                longitude);
        float[] distance = new float[3];
        distance[0] = -1;
        if (location != null) {
            Location.distanceBetween(location.getLatitude(),
                    location.getLongitude(), latitude, longitude, distance);
        }
        FavouriteProperties properties = new FavouriteProperties();
        properties.put(FavouriteProperties.ARG_ROUTES, routeName);
        properties.put(FavouriteProperties.ARG_LINES, linesName);
        properties.put(FavouriteProperties.ARG_LINES_ROUTES, linesRoutes);
        return new FavouriteStop(code, stopName, properties, distance,
                stopLocation, Boolean.valueOf(isFavourite));
    }

    private ArrayList<Favourite> fetchFavouriteStops(Cursor cursor,
                                                     VatmanLocation location, boolean isFavourite) {
        ArrayList<Favourite> result = new ArrayList<Favourite>();
        cursor.moveToFirst();
        while (cursor.isAfterLast() == false) {
            result.add(getFavouriteFromCursor(cursor, location, isFavourite));
            cursor.moveToNext();
        }
        cursor.close();
        if (location != null) {
            Comparator<Favourite> comperator = new Comparator<Favourite>() {
                public int compare(Favourite object1, Favourite object2) {
                    if (object1.getDistance() > object2.getDistance()) {
                        return 1;
                    } else if (object1.getDistance() < object2.getDistance()) {
                        return -1;
                    }
                    return 0;
                }
            };
            Collections.sort(result, comperator);
        }
        return result;
    }

    public Favourite getFavourite(int code, VatmanLocation location) {
        Cursor cursor = null;
        Favourite result = null;
        try {
            String q = "SELECT " + STOPS_COL_NAME + ", " + STOPS_COL_ROUTE
                    + ", " + STOPS_COL_LINES + ", " + STOPS_COL_ID + ", "
                    + STOPS_COL_LAT + ", " + STOPS_COL_LON + ", "
                    + STOPS_COL_LINESROUTES + " FROM " + DB_TABLE_STOPS
                    + " WHERE " + STOPS_COL_ID + "=" + code;
            cursor = getCursor(q, null);
            cursor.moveToFirst();
            result = getFavouriteFromCursor(cursor, location, false);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
        return result;
    }

    private Cursor getCursor(String sql, String[] selectionArgs) {
        if (!mDB.isOpen()) {
            mDB = SQLiteDatabase.openOrCreateDatabase(mDB.getPath(), null);
        }
        return mDB.rawQuery(sql, selectionArgs);
    }

    public ArrayList<Favourite> getFavourites(VatmanLocation location,
                                              double[] boundingBox, boolean includeFavourites) {
        ArrayList<Favourite> result = new ArrayList<Favourite>();
        ArrayList<Integer> addedFavourites = new ArrayList<Integer>();
        Cursor cursor = null;
        try {
            String q;
            if (includeFavourites) {
                q = "SELECT " + STOPS_COL_NAME + ", " + STOPS_COL_ROUTE + ", "
                        + STOPS_COL_LINES + ", " + DB_TABLE_FAVORITES + "."
                        + FAVOURITES_COL_STOP_ID + ", " + STOPS_COL_LAT + ", "
                        + STOPS_COL_LON + ", " + STOPS_COL_LINESROUTES
                        + " FROM " + DB_TABLE_FAVORITES + ", " + DB_TABLE_STOPS
                        + " WHERE " + DB_TABLE_STOPS + "."
                        + STOPS_COL_IS_ACTIVE + "=1 AND " + DB_TABLE_FAVORITES
                        + "." + FAVOURITES_COL_STOP_ID + "=" + DB_TABLE_STOPS
                        + "." + STOPS_COL_ID;
                cursor = getCursor(q, null);
                final ArrayList<Favourite> favourites = fetchFavouriteStops(
                        cursor, location, true);
                if (favourites.size() > 0) {
                    for (Favourite f : favourites) {
                        addedFavourites.add(f.getCode());
                        result.add(f);
                    }
                }
            }

            if (boundingBox != null) {
                q = "SELECT " + STOPS_COL_NAME + ", " + STOPS_COL_ROUTE + ", "
                        + STOPS_COL_LINES + ", " + STOPS_COL_ID + ", "
                        + STOPS_COL_LAT + ", " + STOPS_COL_LON + ", "
                        + STOPS_COL_LINESROUTES + " FROM " + DB_TABLE_STOPS
                        + " WHERE " + DB_TABLE_STOPS + "."
                        + STOPS_COL_IS_ACTIVE + "=1 AND ";
                q += STOPS_COL_LAT + " > " + boundingBox[0];
                q += " AND " + STOPS_COL_LON + " > " + boundingBox[1];
                q += " AND " + STOPS_COL_LAT + " < " + boundingBox[2];
                q += " AND " + STOPS_COL_LON + " < " + boundingBox[3];
                cursor = getCursor(q, null);
                final ArrayList<Favourite> near = fetchFavouriteStops(cursor,
                        location, false);
                if (near.size() > 0) {
                    for (Favourite f : near) {
                        if (!addedFavourites.contains(f.getCode())) {
                            result.add(f);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                cursor.close();
            } catch (NullPointerException e) {

            }
        }
        return result;
    }
}