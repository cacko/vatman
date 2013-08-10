package com.mutanti.vatman;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

public class VatmanAbout extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.about);

        TextView m_aboutVersion = (TextView) findViewById(R.id.about_version);
        TextView m_aboutCopyright = (TextView) findViewById(R.id.about_copyright);

        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {

        }
        String versionInfo = pInfo.versionName;

        m_aboutVersion.setText(versionInfo);
        m_aboutCopyright.setText(Html.fromHtml("&copy; 2010 mutanti.com"));


    }
}
