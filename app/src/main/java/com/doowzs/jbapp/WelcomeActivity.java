package com.doowzs.jbapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

public class WelcomeActivity extends AppCompatActivity {
    // Shared Preferences and Application
    private JBAppApplication mApp = null;
    private SharedPreferences mPrefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        TextView versionTextView = findViewById(R.id.textView_version);
        versionTextView.setText(BuildConfig.VERSION_NAME);

        mApp = ((JBAppApplication) getApplication());
        mPrefs = this.getSharedPreferences(getPackageName(), MODE_PRIVATE);

        if (mPrefs.contains(mApp.tokenKey)) {
            Snackbar.make(findViewById(R.id.welcome_coordinator_layout),
                    getText(R.string.welcome_back) + " " + mPrefs.getString(mApp.nameKey, "anonymous"),
                    Snackbar.LENGTH_LONG)
                    .show();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mPrefs.contains(mApp.tokenKey)) {
                    Intent mainIntent = new Intent(WelcomeActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                } else {
                    Intent loginIntent = new Intent(WelcomeActivity.this, LoginActivity.class);
                    startActivity(loginIntent);
                }
                finish();
            }
        }, 2000);
    }
}
