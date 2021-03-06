package com.doowzs.jbapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.doowzs.jbapp.utils.JSONSharedPreferences;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.Duration;
import org.ocpsoft.prettytime.PrettyTime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {
    // Application and Shared Preferences
    private JBAppApplication mApp = null;
    private SharedPreferences mPrefs = null;
    private Context mContext = null;

    // Layout Components
    private ActionBar mActionbar = null;
    private DisplayMetrics mDisplayMetrics = null;
    private DrawerLayout mDrawerLayout = null;
    private CoordinatorLayout mCoordinatorLayout = null;
    private LinearLayout mLinearLayout = null;
    private SwipeRefreshLayout mSwipeRefreshLayout = null;
    private AlertDialog.Builder mBuilder = null;

    // Volley Request Queue
    private RequestQueue mQueue = null;
    private GetAssignmentsTask mGetAssignmentsTask = null;

    /**
     * Create the activity
     * @param savedInstanceState (not used)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActionbar = getSupportActionBar();
        mActionbar.setDisplayHomeAsUpEnabled(true);
        mActionbar.setHomeAsUpIndicator(R.drawable.ic_menu_white);

        // Check Login Status
        mApp = ((JBAppApplication) getApplication());
        mPrefs = this.getSharedPreferences(getPackageName(), MODE_PRIVATE);
        mContext = this.getBaseContext();
        mQueue = Volley.newRequestQueue(mContext);

        // Fetch layout components
        mDisplayMetrics = mContext.getResources().getDisplayMetrics();
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mCoordinatorLayout = findViewById(R.id.main_coordinator_layout);
        mLinearLayout = findViewById(R.id.assignment_layout);
        mBuilder = new AlertDialog.Builder(MainActivity.this);

        // Listen to drawer events
        mDrawerLayout.addDrawerListener(
                new DrawerLayout.DrawerListener() {
                    @Override
                    public void onDrawerSlide(View drawerView, float slideOffset) {
                        //
                    }

                    @Override
                    public void onDrawerOpened(View drawerView) {
                        mActionbar.setTitle(R.string.title_activity_menu);
                        mActionbar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {
                        mActionbar.setTitle(R.string.title_activity_main);
                        mActionbar.setHomeAsUpIndicator(R.drawable.ic_menu_white);
                    }

                    @Override
                    public void onDrawerStateChanged(int newState) {
                        //
                    }
                }
        );

        // Set up navigation
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(0).setChecked(true);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        mDrawerLayout.closeDrawers();
                        if (menuItem.getItemId() == R.id.nav_about) {
                            Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
                            startActivity(aboutIntent);
                        } else if (menuItem.getItemId() == R.id.nav_logout) {
                            performLogout();
                        }
                        return true;
                    }
                });

        // Update Navigation header with user info
        LinearLayout headerView = (LinearLayout) navigationView.getHeaderView(0);
        TextView navHeaderText1 = (TextView) headerView.getChildAt(0);
        navHeaderText1.setText(mPrefs.getString(mApp.nameKey, "Anonymous"));
        TextView navHeaderText2 = (TextView) headerView.getChildAt(1);
        navHeaderText2.setText(mPrefs.getString(mApp.idKey, "404"));

        // Set up swipe fresh layout
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        mGetAssignmentsTask = new GetAssignmentsTask();
                        mGetAssignmentsTask.execute();
                    }
                }
        );

        // Load saved assignments
        try {
            JSONArray assignmentsArray = JSONSharedPreferences.loadJSONArray(mContext, getPackageName(), mApp.assignmentsKey);
            loadAssignmentsToLayout(assignmentsArray);
        } catch (JSONException jex) {
            Toast.makeText(this, jex.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

        // Check app update
        mQueue.add(mApp.checkUpdateRequest(mBuilder));

        // Fetch latest assignments
        mGetAssignmentsTask = new GetAssignmentsTask();
        mGetAssignmentsTask.execute();
    }

    /**
     * Action Bar menu handler
     * @param item menu of action bar
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Logout from app.
     */
    public void performLogout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.confirm_logout_title))
                .setMessage(getString(R.string.confirm_logout_content)
                        + mPrefs.getString(mApp.idKey, "404") + " - "
                        + mPrefs.getString(mApp.nameKey, "Anonymous"))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        doPerformLogout();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(getDrawable(R.drawable.ic_exclamation_circle))
                .show();
    }

    /**
     * Handler of true logout event
     */
    private void doPerformLogout() {
        mPrefs.edit().remove(mApp.tokenKey)
                .remove(mApp.idKey)
                .remove(mApp.nameKey)
                .apply();
        JSONSharedPreferences.remove(mContext, getPackageName(), mApp.assignmentsKey);
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(loginIntent);
        finish(); // destroy MainActivity
    }

    /**
     * Convert dp to pixel
     * @param dp int
     * @return the pixel value
     */
    public float dp2pixel(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mDisplayMetrics);
    }

    /**
     * Convert dp to pixel (integer)
     * @param dp int
     * @return the pixel value (integer)
     */
    public int dp2pixelInt(int dp) {
        return Math.round(dp2pixel(dp));
    }

    /**
     * Represents an asynchronous task to fetch assignmens.
     */
    public class GetAssignmentsTask extends AsyncTask<Void, Void, Boolean> {
        GetAssignmentsTask() {
            if (!mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                mQueue.add(mApp.new AppJsonObjectRequest(
                        Request.Method.POST, mApp.assignmentsURL, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject data) {
                                try {
                                    JSONArray assignmentArray = data.getJSONArray("data");
                                    JSONSharedPreferences.remove(mContext, getPackageName(), mApp.assignmentsKey);
                                    JSONSharedPreferences.saveJSONArray(mContext, getPackageName(), mApp.assignmentsKey, assignmentArray);
                                    loadAssignmentsToLayout(assignmentArray);
                                } catch (JSONException jex) {
                                    Snackbar.make(mCoordinatorLayout, jex.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
                                }
                                if (mSwipeRefreshLayout.isRefreshing()) {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError vex) {
                        if (mSwipeRefreshLayout.isRefreshing()) {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                        if (vex instanceof AuthFailureError) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle(R.string.error_auth_failure_title)
                                    .setMessage(R.string.error_auth_failure_content)
                                    .setIcon(R.drawable.ic_user_times)
                                    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // do nothing
                                        }
                                    }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    // revoke all credentials and request login
                                    doPerformLogout();
                                }
                            }).show();
                        }
                        Snackbar.make(mCoordinatorLayout, vex.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
                    }
                }));
                return true;
            } catch (Exception ex) {
                Snackbar.make(mCoordinatorLayout, ex.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
                return false;
            }
        }

        @Override
        protected void onCancelled() {
            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    /**
     * Load assignments from an array to linear layout.
     * @param assignmentArray a JSON array of assignments
     */
    public void loadAssignmentsToLayout(JSONArray assignmentArray) {
        int dp5 = dp2pixelInt(5), dp8 = dp2pixelInt(8);
        mLinearLayout.removeAllViews();
        if(assignmentArray.length() == 0) {
            mLinearLayout.setGravity(Gravity.CENTER_VERTICAL);
            TextView textView = new TextView(mContext);
            textView.setTextColor(getColor(R.color.colorBlack));
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textView.setText(getString(R.string.info_no_assignment));
            mLinearLayout.addView(textView);
        } else {
            Date now = new Date();
            PrettyTime prettyTime = new PrettyTime();
            DateFormat oldDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                    newDateFormat = new SimpleDateFormat("yyyy-MM-dd (E) HH:mm:ss", Locale.getDefault());
            mLinearLayout.setGravity(Gravity.NO_GRAVITY);
            try {
                for (int i = 0; i < assignmentArray.length(); ++i) {
                    final JSONObject assignmentObject = assignmentArray.getJSONObject(i);
                    final int assignmentID = assignmentObject.getInt("id");
                    final String assignmentName = assignmentObject.getString("name");
                    final String assignmentContent = assignmentObject.getString("content");
                    final String assignmentDDLStr = assignmentObject.getString("due_time");
                    final Boolean assignmentFinished = assignmentObject.getBoolean("finished");
                    String assignmentDDLDiff = null;
                    Date assignmentDDLDate = null;
                    try {
                        assignmentDDLDate = oldDateFormat.parse(assignmentDDLStr);
                        List<Duration> durations = prettyTime.calculatePreciseDuration(assignmentDDLDate);
                        if (durations.size() > 2) durations = durations.subList(0, 2);
                        assignmentDDLDiff = newDateFormat.format(assignmentDDLDate) + "\n" + prettyTime.format(durations);
                    } catch (java.text.ParseException jtpex) {
                        Log.e("ParsingDDL", jtpex.getLocalizedMessage());
                    }

                    CardView cardView = new CardView(mContext);
                    cardView.setClickable(true);
                    cardView.setUseCompatPadding(true);
                    cardView.setRadius(dp8);
                    cardView.setCardElevation(dp5);
                    cardView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mBuilder.setIcon(assignmentFinished ? R.drawable.ic_calendar_times : R.drawable.ic_calendar_check)
                                    .setTitle(getString(assignmentFinished ?
                                            R.string.assignment_mark_unfinished_title : R.string.assignment_mark_finished_title))
                                    .setMessage(getString(assignmentFinished ?
                                            R.string.assignment_mark_unfinished_content : R.string.assignment_mark_finished_content)
                                            + "\n\n" + assignmentName)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Snackbar.make(mCoordinatorLayout,
                                                    getString(R.string.assignment_mark_progressing_left) + " " + assignmentName
                                                            + " " + getString(R.string.assignment_mark_progressing_right),
                                                    Snackbar.LENGTH_SHORT)
                                                    .show();
                                            if (!mSwipeRefreshLayout.isRefreshing()) {
                                                mSwipeRefreshLayout.setRefreshing(true);
                                            }
                                            mQueue.add(mApp.new AppJsonObjectRequest(
                                                    Request.Method.POST, mApp.assignmentStatusURL(assignmentID, !assignmentFinished),
                                                    null, new Response.Listener<JSONObject>() {
                                                @Override
                                                public void onResponse(JSONObject response) {
                                                    if (mSwipeRefreshLayout.isRefreshing()) {
                                                        mSwipeRefreshLayout.setRefreshing(false);
                                                    }
                                                    try {
                                                        loadAssignmentsToLayout(response.getJSONArray("data"));
                                                    } catch (JSONException jex) {
                                                        Log.e("ASStatusResponseError", jex.getLocalizedMessage());
                                                    }
                                                }
                                            }, new Response.ErrorListener() {
                                                @Override
                                                public void onErrorResponse(VolleyError vex) {
                                                    Log.e("ASStatusUpdateError", vex.getLocalizedMessage());
                                                }
                                            }
                                            ));
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, null)
                                    .show();
                        }
                    });

                    RelativeLayout relativeLayout = new RelativeLayout(mContext);
                    relativeLayout.setPadding(dp8, dp8, dp8, dp8);
                    if (assignmentFinished) {
                        relativeLayout.setBackgroundColor(getColor(R.color.colorGreenLighten5));
                    } else if (assignmentDDLDate.before(new Date(now.getTime() + 24*60*60*1000))) {
                        relativeLayout.setBackgroundColor(getColor(R.color.colorRedLighten5));
                    } else if (assignmentDDLDate.before(new Date(now.getTime() + 48*60*60*1000))) {
                        relativeLayout.setBackgroundColor(getColor(R.color.colorAmberLighten5));
                    }

                    LinearLayout linearLayout = new LinearLayout(mContext);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    linearLayout.setPadding(15, 10, 15, 10);

                    TextView textView = new TextView(mContext);
                    textView.setText(assignmentName);
                    textView.setTextSize(20);
                    textView.setTextColor(getColor(R.color.colorBlack));

                    WebView webView = new WebView(mContext);
                    webView.setBackgroundColor(Color.TRANSPARENT);
                    webView.loadData(assignmentContent, "text/html; charset=UTF-8", null);
                    webView.setWebViewClient(new WebViewClient(){
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                                return true;
                            } else {
                                return false;
                            }
                        }
                    });

                    TextView textView2 = new TextView(mContext);
                    textView2.setText(assignmentDDLDiff);
                    textView2.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

                    linearLayout.addView(textView);
                    linearLayout.addView(webView);
                    linearLayout.addView(textView2);
                    relativeLayout.addView(linearLayout);
                    cardView.addView(relativeLayout);

                    mLinearLayout.addView(cardView);
                }
                TextView textView = new TextView(mContext);
                String assignmentCountStr = getString(R.string.assignment_count_left) + " "
                        + assignmentArray.length() + " " + getString(R.string.assignment_count_right);
                textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                textView.setText(assignmentCountStr);
                textView.setPadding(dp8, dp8 * 2, dp8, dp8 * 2);
                mLinearLayout.addView(textView);
            } catch (JSONException jex) {
                Log.e("LoadASError", jex.getLocalizedMessage());
            }
        }
        Snackbar.make(mCoordinatorLayout, getString(R.string.info_updated), Snackbar.LENGTH_SHORT).show();
    }
}
