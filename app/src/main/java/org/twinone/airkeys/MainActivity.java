package org.twinone.airkeys;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import java.util.List;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private static final int REQ_ADD_METHOD = 100;
    private Button mAddIME;
    private Button mSelectIME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAddIME = (Button) findViewById(R.id.main_b_add_ime);
        mAddIME.setOnClickListener(this);
        mSelectIME = (Button) findViewById(R.id.main_b_select_ime);
        mSelectIME.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_b_add_ime:
                startActivityForResult(
                        new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS), REQ_ADD_METHOD);
                break;
            case R.id.main_b_select_ime:
                AirKeysService.showSelectInputMethodDialog(this);
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ADD_METHOD) {
            Log.d("MainActivity", "Result code: " + resultCode);
        }
        Log.d("MainActivity", "OnActivityResult AirKeysEnabled: " + isAirKeysEnabled());
    }

    private void updateLayout() {
        boolean enabled = isAirKeysEnabled();
        if (enabled) {
            // TODO
        }
    }

    private boolean isAirKeysEnabled() {
        InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        List<InputMethodInfo> list = im.getEnabledInputMethodList();
        for (InputMethodInfo info : list) {
            Log.v("MainActivity", "InputMethod: " + info.getPackageName());
            if (info.getPackageName().equals(getPackageName())) return true;
        }
        return false;
    }
}
