package com.catchingnow.tinyclipboardmanager;

import android.animation.Animator;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.graphics.PixelFormat;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

//https://github.com/EatHeat/FloatingExample

public class FloatingWindowService extends Service {

    public static final String FLOATING_WINDOW_X = "floating_window_x";
    public static final String FLOATING_WINDOW_Y = "floating_window_y";

    private SharedPreferences preference;
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    private int foregroundActivityCount = 0;

    private boolean checkPermission() {
         return (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(ActivitySetting.PREF_FLOATING_BUTTON, false));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!checkPermission()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {

        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        preference = MyUtil.getLocalSharedPreferences(this);
        LayoutInflater layoutInflater =
                (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final Intent i = new Intent(this, ActivityMainDialog.class)
                .putExtra(ActivityMain.EXTRA_IS_FROM_NOTIFICATION, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        floatingView =
                layoutInflater.inflate(R.layout.floating_window, null);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = preference.getInt(FLOATING_WINDOW_X, 120);
        params.y = preference.getInt(FLOATING_WINDOW_Y, 120);

        windowManager.addView(floatingView, params);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(new BroadcastReceiver() {
                                      @Override
                                      public void onReceive(final Context context, Intent intent) {
                                          foregroundActivityCount -=1;
                                          if (foregroundActivityCount < 0) foregroundActivityCount = 0;
                                          if (foregroundActivityCount == 0) {
                                              floatingView.animate().scaleX(1).scaleY(1);
                                              params.x = preference.getInt(FLOATING_WINDOW_X, 120);
                                              params.y = preference.getInt(FLOATING_WINDOW_Y, 120);
                                              windowManager.updateViewLayout(floatingView, params);
                                          }
                                      }
                                  },
                        new IntentFilter(MyActionBarActivity.DIALOG_CLOSED));

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(new BroadcastReceiver() {
                                      @Override
                                      public void onReceive(final Context context, Intent intent) {
                                          foregroundActivityCount += 1;
                                          if (foregroundActivityCount > 0) {
                                              floatingView.animate().scaleX(0).scaleY(0);

                                              WindowManager.LayoutParams tmpParams = new WindowManager.LayoutParams(
                                                      WindowManager.LayoutParams.WRAP_CONTENT,
                                                      WindowManager.LayoutParams.WRAP_CONTENT,
                                                      WindowManager.LayoutParams.TYPE_PHONE,
                                                      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                                      PixelFormat.TRANSLUCENT);

                                              tmpParams.gravity = Gravity.TOP | Gravity.LEFT;
                                              tmpParams.x = preference.getInt(FLOATING_WINDOW_X, 120);
                                              tmpParams.y = preference.getInt(FLOATING_WINDOW_Y, 120);
                                              tmpParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

                                              windowManager.updateViewLayout(floatingView, tmpParams);
                                          }
                                      }
                                  },
                        new IntentFilter(MyActionBarActivity.DIALOG_OPENED));

        try {
            floatingView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(i);
                }
            });
            floatingView.setOnTouchListener(new View.OnTouchListener() {
                private WindowManager.LayoutParams paramsF = params;
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:

                            // Get current time in nano seconds.

                            initialX = paramsF.x;
                            initialY = paramsF.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_UP:
                            preference.edit()
                                    .putInt(FLOATING_WINDOW_X, paramsF.x)
                                    .putInt(FLOATING_WINDOW_Y, paramsF.y)
                                    .apply();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
                            paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingView, paramsF);
                            break;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            Log.e(MyUtil.PACKAGE_NAME, e.toString());
        }

    }

    @Override
    public void onDestroy() {
        if (floatingView != null) windowManager.removeView(floatingView);
        super.onDestroy();
    }

}
