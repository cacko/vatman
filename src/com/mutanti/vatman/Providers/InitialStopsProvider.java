package com.mutanti.vatman.Providers;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.mutanti.vatman.Exception.TerminateSaxParsing;
import com.mutanti.vatman.Vatman;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public final class InitialStopsProvider {

    public static final String CODE = "code";
    public static final String NAME = "name";
    public static final String ROUTE = "route";
    public static final String LINES = "lines";
    public static final String LINES_ROUTES = "linesRoutes";
    public static final String IS_ACTIVE = "isActive";
    public static final String LAT = "lat";
    public static final String LON = "lon";
    public static final String COUNT = "count";
    public static final String VERSION = "ver";
    public static final String NODE_STOP = "stop";
    public static final String NODE_STOPS = "stops";
    private static final String ENCODING = "UTF8";
    private final Handler mHandler;
    private final GZIPInputStream mStream;
    private boolean mTerminateOnHeaderFound;
    private int mStopCount = 0;
    private long mStopVersion = 0;

    public InitialStopsProvider(Handler callback, InputStream resourceStream)
            throws IOException {
        mHandler = callback;
        mStream = new GZIPInputStream(resourceStream);
    }

    private void sendVersion() {
        Message message = new Message();
        Bundle data = new Bundle();
        data.putInt(Vatman.BUNDLE_ARG_STOP_COUNT, mStopCount);
        data.putLong(Vatman.BUNDLE_ARG_STOP_VERSION, mStopVersion);
        message.setData(data);
        message.what = Vatman.OPERATION_INIT_STOPS_VERSION_INFO;
        mHandler.sendMessage(message);
    }

    private void parseHeader() {
        mTerminateOnHeaderFound = true;
        initParser();
        ArrayList<String> nodes = new ArrayList<String>();
        nodes.add(NODE_STOPS);

        XMLReader xr = null;
        try {
            xr = XMLReaderFactory.createXMLReader();
        } catch (SAXException e1) {
            e1.printStackTrace();
        }
        final XmlHandler handler = new XmlHandler(nodes);

        xr.setContentHandler(handler);
        xr.setErrorHandler(handler);

        final InputSource inputSource = new InputSource(mStream);
        inputSource.setEncoding(ENCODING);
        try {
            xr.parse(inputSource);
            mStream.close();
        } catch (TerminateSaxParsing e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    public long getVersion() {
        if (mStopVersion == 0) {
            parseHeader();
        }
        return mStopVersion;
    }

    public int getCount() {
        if (mStopCount == 0) {
            parseHeader();
        }
        return mStopCount;
    }

    public void parse() throws IOException, SAXException {
        initParser();
        ArrayList<String> nodes = new ArrayList<String>();
        nodes.add(NODE_STOP);
        nodes.add(NODE_STOPS);

        final XMLReader xr = XMLReaderFactory.createXMLReader();
        final XmlHandler handler = new XmlHandler(nodes);

        xr.setContentHandler(handler);
        xr.setErrorHandler(handler);

        final InputSource inputSource = new InputSource(mStream);
        final Db db = Db.getInstance();
        db.startOperation();
        inputSource.setEncoding(ENCODING);
        xr.parse(inputSource);
        db.endOperation();
        mStream.close();
    }

    private void initParser() {
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
    }

    final class XmlHandler extends DefaultHandler {

        private final ArrayList<String> m_nodesOfInterest;
        private final Db db;

        public XmlHandler(ArrayList<String> nodes) {
            m_nodesOfInterest = nodes;
            db = Db.getInstance();
        }

        @Override
        public void startElement(String uri, String name, String qName,
                                 Attributes atts) throws TerminateSaxParsing {
            if (m_nodesOfInterest.contains(name)) {
                if (NODE_STOP.equals(name)) {
                    final int code = Integer.valueOf(atts.getValue(CODE));
                    final double lat = Double.valueOf(atts.getValue(LAT));
                    final double lon = Double.valueOf(atts.getValue(LON));
                    final String label = atts.getValue(NAME);
                    final String route = atts.getValue(ROUTE);
                    final String lines = atts.getValue(LINES);
                    final String linesRoutes = atts.getValue(LINES_ROUTES);
                    final int isActive = Integer.valueOf(atts
                            .getValue(IS_ACTIVE));
                    long addResult = db.addStop(code, label, route, lines,
                            linesRoutes, isActive, lat, lon, Vatman.OPERATION_INIT_STOPS);
                    if (addResult == -1) {
                        db.updateStop(code, label, route, lines, linesRoutes,
                                isActive, lat, lon, Vatman.OPERATION_INIT_STOPS);
                    }
                } else if (NODE_STOPS.equals(name)) {
                    mStopCount = Integer.valueOf(atts.getValue(COUNT));
                    mStopVersion = Long.valueOf(atts.getValue(VERSION));
                }
            }
        }

        public void endElement(String uri, String name, String qName)
                throws SAXException {
            super.endElement(uri, name, qName);
            if (NODE_STOPS.equals(name)) {
                sendVersion();
                if (mTerminateOnHeaderFound) {
                    throw new TerminateSaxParsing("mente", null);
                }
            }
        }
    }
}