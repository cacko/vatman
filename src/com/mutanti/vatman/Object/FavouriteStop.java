package com.mutanti.vatman.Object;

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.TextView;

import com.mutanti.vatman.Vatman;
import com.mutanti.vatman.Adapter.FavouritesAdapter;

public class FavouriteStop extends Favourite {

	private String mRoutes;
	private String mLines;
	private String mLinesRoutes;

	private static final String TYPE_TRAMVAI = "0:";
	private static final String TYPE_TROLLEY = "2:";
	private static final String TYPE_BUS = "1:";

	public FavouriteStop(int code, String name, FavouriteProperties properties,
			float[] distance, VatmanLocation location, boolean isFavourite) {
		super(code, name, properties, distance, location, isFavourite);
		mRoutes = properties.get(FavouriteProperties.ARG_ROUTES);
		mLines = properties.get(FavouriteProperties.ARG_LINES);
		mLinesRoutes = transformLinesRoutes(properties.get(FavouriteProperties.ARG_LINES_ROUTES), "\n");
	}

	public static String transformLinesRoutes(String linesRoutes,
			String separator) {
		return linesRoutes.replace("::", " / ").replace("|", separator)
				.replace(TYPE_TRAMVAI, "Трам.").replace(TYPE_TROLLEY, "Трол.")
				.replace(TYPE_BUS, "Авт.");
	}

	public FavouriteStop(Parcel in) {
		String[] data = new String[9];
		in.readStringArray(data);
		mCode = Integer.valueOf(data[0]);
		mName = data[1];
		mDistance = Double.valueOf(data[2]);
		mBearing = Double.valueOf(data[3]);
		mDirection = (float) (((mBearing - Vatman.getHeading()) + 360) % 360);
		mIsFavourite = Boolean.valueOf(data[4]);
		mLocation = new VatmanLocation(Integer.valueOf(data[5]), Integer.valueOf(
				data[6]));
		mRoutes = data[7];
		mLines = data[8];
	}

	public String getRoute() {
		return mRoutes;
	}

	public String getLines() {
		return mLines;
	}

	public String getLinesRoutes() {
		return mLinesRoutes;
	}

	public void setDescription(TextView description, int viewMode) {
		if (description != null) {
			String text = "";
			switch (viewMode) {
			case FavouritesAdapter.MODE_LINES:
				text = getLines();
				break;
			case FavouritesAdapter.MODE_ROUTES:
				text = getRoute();
				break;
			case FavouritesAdapter.MODE_LINES_ROUTES:
				text = getLinesRoutes();
				break;
			}
			description.setText(text);
			description.setSelected(true);
		}
	}

	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStringArray(new String[] { "" + mCode, mName, "" + mDistance,
				"" + mBearing, "" + mIsFavourite,
				"" + mLocation.getLatitudeE6(),
				"" + mLocation.getLongitudeE6(), mRoutes, mLines });
	}

	@SuppressWarnings({ "rawtypes" })
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public FavouriteStop createFromParcel(Parcel in) {
			return new FavouriteStop(in);
		}

		public FavouriteStop[] newArray(int size) {
			return new FavouriteStop[size];
		}
	};

}
