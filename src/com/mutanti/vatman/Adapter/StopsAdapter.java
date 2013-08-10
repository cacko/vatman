package com.mutanti.vatman.Adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.mutanti.vatman.Object.FavouriteStop;
import com.mutanti.vatman.Providers.Db;
import com.mutanti.vatman.R;
import com.mutanti.vatman.VatmanPreferences;

import java.util.HashMap;

public final class StopsAdapter extends CursorAdapter {
    public final static HashMap<String, String> LATIN2CYRILLIC = new HashMap<String, String>();

    static {
        LATIN2CYRILLIC.put("A", "А");
        LATIN2CYRILLIC.put("B", "Б");
        LATIN2CYRILLIC.put("W", "В");
        LATIN2CYRILLIC.put("G", "Г");
        LATIN2CYRILLIC.put("D", "Д");
        LATIN2CYRILLIC.put("E", "Е");
        LATIN2CYRILLIC.put("V", "Ж");
        LATIN2CYRILLIC.put("Z", "З");
        LATIN2CYRILLIC.put("I", "И");
        LATIN2CYRILLIC.put("J", "Й");
        LATIN2CYRILLIC.put("K", "К");
        LATIN2CYRILLIC.put("L", "Л");
        LATIN2CYRILLIC.put("M", "М");
        LATIN2CYRILLIC.put("N", "Н");
        LATIN2CYRILLIC.put("O", "О");
        LATIN2CYRILLIC.put("P", "П");
        LATIN2CYRILLIC.put("R", "Р");
        LATIN2CYRILLIC.put("S", "С");
        LATIN2CYRILLIC.put("T", "Т");
        LATIN2CYRILLIC.put("U", "У");
        LATIN2CYRILLIC.put("F", "Ф");
        LATIN2CYRILLIC.put("H", "Х");
        LATIN2CYRILLIC.put("`", "Ч");
        LATIN2CYRILLIC.put("C", "Ц");
        LATIN2CYRILLIC.put("[", "Ш");
        LATIN2CYRILLIC.put("]", "Щ");
        LATIN2CYRILLIC.put("Y", "Ъ");
        LATIN2CYRILLIC.put("X", "Ь");
        LATIN2CYRILLIC.put("\\", "Ю");
        LATIN2CYRILLIC.put("Q", "Я");
    }

    private static final int COL_NAME = 1;
    private static final int COL_ROUTES = 2;
    private static final int COL_LINES = 3;
    private static final int COL_LINES_ROUTES = 4;
    private final Db m_db;
    private SharedPreferences preferences;

    @SuppressWarnings("deprecation")
    public StopsAdapter(Context context, Db db) { // Cursor c, SQLiteDatabase
        // db, String table,
        // String[] columns, String
        // filter) {
        super(context, db.getStopsCursor());
        m_db = db;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.autocomplete_item, parent,
                false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        TextView textStopItem = (TextView) view
                .findViewById(R.id.autocomplete_stop_item);
        textStopItem.setText(cursor.getString(COL_NAME));

        TextView textStopDescription = (TextView) view
                .findViewById(R.id.autocomplete_stop_description);
        String description = "";
        int viewMode = Integer.valueOf(preferences.getString(
                VatmanPreferences.KEY_PREF_VIEW_MODE, "1"));
        switch (viewMode) {
            case FavouritesAdapter.MODE_LINES:
                description = cursor.getString(COL_LINES);
                break;
            case FavouritesAdapter.MODE_ROUTES:
                description = cursor.getString(COL_ROUTES);
                break;
            case FavouritesAdapter.MODE_LINES_ROUTES:
                description = FavouriteStop.transformLinesRoutes(
                        cursor.getString(COL_LINES_ROUTES), "\n");
                break;
        }
        textStopDescription.setText(description);
        textStopDescription.setSelected(true);
    }

    @Override
    public String convertToString(Cursor cursor) {
        return cursor.getString(COL_NAME);
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        if (constraint != null) {
            String filter = constraint.toString().toUpperCase();
            return m_db.getStopsFilterCursor(convertLatin2Cyrillic(filter));
        } else {
            return null;
        }
    }

    private String convertLatin2Cyrillic(String latin) {
        String result = "";
        for (int i = 0; i < latin.length(); i++) {
            char c = latin.charAt(i);
            int code = (int) c;
            String s = String.valueOf(c);
            if ((code < 1040 || code > 1071) && LATIN2CYRILLIC.containsKey(s)) {
                result += LATIN2CYRILLIC.get(s);
            } else {
                result += c;
            }
        }
        return result;

    }
}