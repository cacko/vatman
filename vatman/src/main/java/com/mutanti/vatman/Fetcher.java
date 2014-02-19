package com.mutanti.vatman;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.mutanti.vatman.Exception.VatmanException;
import com.mutanti.vatman.Object.Cache;
import com.mutanti.vatman.Object.PhoneInfo;
import com.mutanti.vatman.Object.ScheduleItem;
import com.mutanti.vatman.Providers.RemoteProvider;
import com.mutanti.vatman.util.Base64;
import com.mutanti.vatman.util.Crypt;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

public final class Fetcher {
    public static final int STATUS_SERVER_MESSAGE = 701;
    protected String mBaseURL;
    protected String mUsername;
    protected String mPassword;
    protected String mVersion;
    protected String mErrorMessage;
    protected int mStop;
    protected DefaultHttpClient mClient = null;
    private PhoneInfo mPhoneInfo;
    private long mFetchStart;
    private long mFetchEnd;
    private ArrayList<Integer> mUpdatedStops;
    private Handler mHandler;
    private RemoteProvider mParser;

    public Fetcher(String baseURL, String version, Handler callback,
                   PhoneInfo phoneInfo, String username, String password) {
        mBaseURL = baseURL;
        mVersion = version;
        mUsername = username;
        mPassword = password;
        mHandler = callback;
        mPhoneInfo = phoneInfo;
    }

    private int[] getUpdatedStopsIntArray(ArrayList<Integer> list) {
        if (list.size() < 1) {
            return null;
        }
        int[] result = new int[list.size()];
        for (int idx = 0; idx < list.size(); idx++) {
            result[idx] = list.get(idx);
        }
        return result;
    }

    private void sendMessage(int operation, String msg) {
        Message message = new Message();
        Bundle data = new Bundle();
        data.putString(Vatman.BUNDLE_ARG_MESSAGE, msg);
        data.putIntArray(Vatman.BUNDLE_ARG_UPDATED_STOPS,
                getUpdatedStopsIntArray(mUpdatedStops));
        message.setData(data);
        message.what = operation;
        mHandler.sendMessage(message);
    }

    private HttpResponse execute(long dbVersion)
            throws IOException {
        if (mClient == null) {
            HttpParams params = new BasicHttpParams();
            params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION,
                    HttpVersion.HTTP_1_1);
            mClient = new DefaultHttpClient(params);
        }
        String encryptedClientId = null;
        try {
            String phoneId = mPhoneInfo.getIMEI() + "|"
                    + mPhoneInfo.getOperator() + "|"
                    + mPhoneInfo.getPhoneModel() + "|"
                    + mPhoneInfo.getOSVersion() + "|" + mVersion;
            encryptedClientId = Crypt.encrypt(phoneId, true);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        StringEntity entity = new StringEntity("<request><client>"
                + encryptedClientId + "</client><stops><stop>" + mStop
                + "</stop></stops><dbVersion>" + dbVersion
                + "</dbVersion></request>");
        HttpPost method = new HttpPost(mBaseURL);
        if (Vatman.AUTORIZATION_REQUIRED) {
            method.addHeader("Authorization",
                    "Basic " + Base64.encode(mUsername + ":" + mPassword));
        }
        method.setEntity(entity);
        mFetchStart = System.currentTimeMillis();
        return mClient.execute(method);
    }

    private String getContent(InputStream in) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private void setDocument(long dbVersion)
            throws ParserConfigurationException, SAXException, IOException,
            VatmanException {

        HttpResponse response = execute(dbVersion);
        HttpEntity entity = response.getEntity();
        int status = response.getStatusLine().getStatusCode();
        if (HttpStatus.SC_UNAUTHORIZED == status) {
            throw new VatmanException(VatmanException.ACCESS_DENIED, null);
        } else if (status == STATUS_SERVER_MESSAGE) {
            mErrorMessage = getContent(entity.getContent());
            entity.consumeContent();
            throw new VatmanException(VatmanException.SERVER_MESSAGE, null);
        } else if (status != 200) {
            entity.consumeContent();
            throw new VatmanException(VatmanException.CONNECTION_FAILED, null);
        }
        InputStream stream = entity.getContent();
        mParser = new RemoteProvider(stream);
        mParser.parse();
        mFetchEnd = System.currentTimeMillis();
        mUpdatedStops = mParser.getUpdatedStops();
        entity.consumeContent();
    }

    private long getTimeDrift(String calculated) {
        long calculatedAt = 0;
        if (calculated != null) {
            try {
                calculatedAt = Long.valueOf(calculated) * 1000;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        long result = 0;
        if (calculatedAt != 0) {
            result = (System.currentTimeMillis() - calculatedAt)
                    - (mFetchEnd - mFetchStart);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<ScheduleItem> fetch(int stop, long dbVersion)
            throws ParserConfigurationException, SAXException, IOException,
            VatmanException {
        mStop = stop;
        ArrayList<ScheduleItem> result = Cache.getInstance().get(stop);
        if (result != null) {
            for (ScheduleItem se : result) {
                se.unFilter();
            }
            return result;
        }
        mErrorMessage = null;
        setDocument(dbVersion);
        HashMap<String, String> values = mParser.getValues();
        long newVersion = mParser.getUpdateVersion();
        if (newVersion > 0) {
            sendMessage(Vatman.OPERATION_NEW_UPDATE, "" + newVersion);
        }
        Vatman.TIME_DRIFT = getTimeDrift(values
                .get(RemoteProvider.NODE_CALCULATED));
        result = mParser.getSchedule();
        if (result != null) {
            Cache.getInstance().put(stop, (ArrayList<ScheduleItem>) result.clone());
        }
        return result;
    }

    public void setCredentials(String username, String password) {
        mUsername = username;
        mPassword = password;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }
}
