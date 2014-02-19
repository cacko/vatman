package com.mutanti.vatman.Providers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.mutanti.vatman.Vatman;
import com.mutanti.vatman.Exception.TerminateSaxParsing;
import com.mutanti.vatman.Object.Line;
import com.mutanti.vatman.Object.Lines;
import com.mutanti.vatman.Object.ScheduleItem;

public final class RemoteProvider {

	public static final String ATTR_TIME = "time";
	public static final String ATTR_LINE_NUM = "num";
	public static final String ATTR_LINE_TYPE = "type";
	public static final String ATTR_LINE_ROUTE = "route";
	public static final String ATTR_STOPS_COUNT = "count";
	public static final String ATTR_STOPS_VERSION = "ver";
	public static final String ATTR_STOP_CODE = "code";
	public static final String ATTR_STOP_NAME = "name";
	public static final String ATTR_STOP_ROUTE = "route";
	public static final String ATTR_STOP_LINES = "lines";
	public static final String ATTR_STOP_LINES_ROUTES = "linesRoutes";
	public static final String ATTR_STOP_IS_ACTIVE = "isActive";
	public static final String ATTR_STOP_LAT = "lat";
	public static final String ATTR_STOP_LON = "lon";

	public static final String NODE_SCHEDULE = "schedule";
	public static final String NODE_TIME = "time";
	public static final String NODE_LINE = "line";
	public static final String NODE_CALCULATED = "calculated";
	public static final String NODE_UPDATES = "updates";
	public static final String NODE_STOPS = "stops";
	public static final String NODE_STOP = "stop";

	private static final String ENCODING = "UTF8";
	private final InputStream mStream;
	private HashMap<String, String> mValues;
	private ArrayList<ScheduleItem> mSchedule;
	private long mUpdateVersion;
	private ArrayList<Integer> mUpdatedStops;

	public RemoteProvider(InputStream xmlStream) {
		mStream = xmlStream;
		mUpdatedStops = new ArrayList<Integer>();
	}

	final class XmlHandler extends DefaultHandler {

		private final Db db;
		private StringBuilder mBuilder;

		private String mCurrentProcessingNodeValue;
		private boolean mScheduleStarted;
		private boolean mUpdatesStarted;
		private String mCurrentScheduleTime;
		private Lines mCurrentLines;

		public XmlHandler() {
			db = Db.getInstance();
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			super.characters(ch, start, length);
			mBuilder.append(ch, start, length);
		}

		@Override
		public void endElement(String uri, String name, String qName)
				throws SAXException {
			super.endElement(uri, name, qName);
			if (mCurrentProcessingNodeValue != null) {
				if (mCurrentProcessingNodeValue
						.equalsIgnoreCase(NODE_CALCULATED)) {
					mValues.put(mCurrentProcessingNodeValue,
							mBuilder.toString());
					mCurrentProcessingNodeValue = null;
				}
				mBuilder.setLength(0);
			}
			if (mScheduleStarted && NODE_TIME.equals(name)) {
				mSchedule.add(new ScheduleItem(mCurrentScheduleTime,
						mCurrentLines));
			} else if (NODE_SCHEDULE.equals(name)) {
				mScheduleStarted = false;
			} else if (NODE_UPDATES.equals(name)) {
				mUpdatesStarted = false;
			}
		}

		@Override
		public void startDocument() throws SAXException {
			super.startDocument();
			mBuilder = new StringBuilder();
			mValues = new HashMap<String, String>();
			mSchedule = new ArrayList<ScheduleItem>();
			mScheduleStarted = false;
			mUpdatesStarted = false;
		}

		@Override
		public void startElement(String uri, String name, String qName,
				Attributes atts) throws TerminateSaxParsing {

			if (mScheduleStarted) {
				if (NODE_TIME.equals(name)) {
					mCurrentScheduleTime = atts.getValue(ATTR_TIME);
					mCurrentLines = new Lines();
				}
				if (NODE_LINE.equals(name)) {
					Line line = new Line(atts.getValue(ATTR_LINE_NUM),
							atts.getValue(ATTR_LINE_TYPE),
							atts.getValue(ATTR_LINE_ROUTE));
					mCurrentLines.add(line);
				}
			}

			if (mUpdatesStarted) {
				if (NODE_STOPS.equals(name)) {
					mUpdateVersion = Long.valueOf(atts
							.getValue(ATTR_STOPS_VERSION));
				} else if (NODE_STOP.equals(name)) {
					final int code = Integer.valueOf(atts
							.getValue(ATTR_STOP_CODE));
					final double lat = Double.valueOf(atts
							.getValue(ATTR_STOP_LAT));
					final double lon = Double.valueOf(atts
							.getValue(ATTR_STOP_LON));
					final String label = atts.getValue(ATTR_STOP_NAME);
					final String route = atts.getValue(ATTR_STOP_ROUTE);
					final String lines = atts.getValue(ATTR_STOP_LINES);
					final String linesRoutes = atts
							.getValue(ATTR_STOP_LINES_ROUTES);
					final int isActive = Integer.valueOf(atts
							.getValue(ATTR_STOP_IS_ACTIVE));
					long addResult = db.addStop(code, label, route, lines,
							linesRoutes, isActive, lat, lon, Vatman.OPERATION_REMOTE_UPDATE);
					if (addResult == -1) {
						db.updateStop(code, label, route, lines, linesRoutes,
								isActive, lat, lon, Vatman.OPERATION_REMOTE_UPDATE);
					}
					mUpdatedStops.add(code);
				}
			}

			if (NODE_CALCULATED.equals(name)) {
				mCurrentProcessingNodeValue = name;
			} else if (NODE_SCHEDULE.equals(name)) {
				mScheduleStarted = true;
			} else if (NODE_UPDATES.equals(name)) {
				mUpdatesStarted = true;
			}
		}
	}

	public void parse() throws IOException, SAXException {
		initParser();

		final XMLReader xr = XMLReaderFactory.createXMLReader();
		final XmlHandler handler = new XmlHandler();

		xr.setContentHandler(handler);
		xr.setErrorHandler(handler);

		final InputSource inputSource = new InputSource(mStream);
		inputSource.setEncoding(ENCODING);
		final Db db = Db.getInstance();
		db.startOperation();
		xr.parse(inputSource);
		db.endOperation();

	}

	public HashMap<String, String> getValues() {
		return mValues;
	}

	public ArrayList<ScheduleItem> getSchedule() {
		return mSchedule;
	}

	public long getUpdateVersion() {
		return mUpdateVersion;
	}

	public ArrayList<Integer> getUpdatedStops() {
		return mUpdatedStops;
	}

	private void initParser() {
		System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
	}
}