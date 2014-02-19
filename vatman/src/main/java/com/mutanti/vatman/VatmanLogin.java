package com.mutanti.vatman;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public final class VatmanLogin extends Activity {

    public final static String ARG_PERSIST = "persist";
    private EditText edit_username;
    private EditText edit_password;
    private CheckBox checkbox_save;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        setContentView(R.layout.login);

        edit_username = (EditText) findViewById(R.id.edit_username);
        edit_password = (EditText) findViewById(R.id.edit_password);
        checkbox_save = (CheckBox) findViewById(R.id.checkbox_save);

        Button buttonOk = (Button) findViewById(R.id.button_login_ok);
        buttonOk.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mgr.hideSoftInputFromWindow(v.getWindowToken(), 0);
                Intent intent = new Intent();
                intent.putExtra(VatmanPreferences.KEY_PREF_USERNAME, edit_username.getText()
                        .toString());
                intent.putExtra(VatmanPreferences.KEY_PREF_PASSWORD, edit_password.getText()
                        .toString());
                intent.putExtra(ARG_PERSIST, checkbox_save.isChecked());
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });

        Button buttonCancel = (Button) findViewById(R.id.button_login_cancel);
        buttonCancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mgr.hideSoftInputFromWindow(v.getWindowToken(), 0);
                Intent intent = new Intent();
                setResult(Activity.RESULT_CANCELED, intent);
                finish();
            }
        });

    }
}
