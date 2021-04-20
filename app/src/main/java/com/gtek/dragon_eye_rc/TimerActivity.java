package com.gtek.dragon_eye_rc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Timer;
import java.util.TimerTask;

public class TimerActivity extends AppCompatActivity {
    private Timer mTimer;
    private TimerTask mTimerTask = null;
    private boolean isPause = false;
    private static int mCount = 0;
    private static int serNoA = -1, serNoB = -1;

    public TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        mTextView = (TextView) findViewById(R.id.durationTextView);

        IntentFilter mcastRcvIntentFilter = new IntentFilter("mcastMsg");
        registerReceiver(broadcastReceiver, mcastRcvIntentFilter);

        FloatingActionButton bs = findViewById(R.id.button_start_timer);
        bs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if(DragonEyeApplication.getInstance().mBaseList.isEmpty())
                //    return;

                DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_go);

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startTimer();
                    }
                });
                thread.start();
            }
        });

        FloatingActionButton bp = findViewById(R.id.button_stop_timer);
        bp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if(DragonEyeApplication.getInstance().mBaseList.isEmpty())
                //    return;

                DragonEyeApplication.getInstance().playPriorityTone(R.raw.smb_die);

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        stopTimer();
                    }
                });
                thread.start();
            }
        });
    }

    @Override
    protected void onDestroy() {
        DragonEyeApplication.getInstance().stopTone();
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    private void TimerMethod()
    {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.

        mCount++;
        //We call the method that will work with the UI
        //through the runOnUiThread method.
        this.runOnUiThread(Timer_Tick);
    }

    private Runnable Timer_Tick = new Runnable() {
        public void run() {

            //This method runs in the same thread as the UI.

            //Do something to the UI thread here
            float duration = (float)mCount / 100;
            //mTextView.setText(Float.toString(duration));
            mTextView.setText(String.format("%.2f", duration));
        }
    };

    private void startTimer(){
        mCount = 0;
        if (mTimer == null) {
            mTimer = new Timer();
        }

        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    do {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                        }
                    } while (isPause);

                    TimerMethod();
                }
            };
        }

        if(mTimer != null && mTimerTask != null )
            mTimer.schedule(mTimerTask, 0, 10);

    }

    private void stopTimer(){
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        //mCount = 0;
    }

    class ThirtySecondsTimer extends CountDownTimer {
        public ThirtySecondsTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            //tv.setText("請等待：" + millisUntilFinished / 1000 + "秒...");
        }

        @Override
        public void onFinish() {
            //tv.setText("finish");
        }
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1: // RX
                    System.out.println("Handler receive : " + msg);
                    String str = msg.obj.toString();
                    String baseHost[] = str.split(":");
                    if (baseHost.length >= 2) {
                        if (TextUtils.equals(baseHost[0], "TRIGGER_A")) {
                            int i = Integer.parseInt(baseHost[1]);
                            if (i != serNoA) {
                                DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_a);
                                serNoA = i;
                            }
                        } else if (TextUtils.equals(baseHost[0], "TRIGGER_B")) {
                            int i = Integer.parseInt(baseHost[1]);
                            if(i != serNoB) {
                                DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_b);
                                serNoB = i;
                            }
                        }
                    }
                    break;
                case 2: // TX
                    break;
                case 3: // Timeout
                    System.out.println("Handler timeout ...");
                    break;
            }
        }
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("mcastRcvMsg")) {
                Message message = new Message();
                message.obj = intent.getStringExtra("mcastRcvMsg");
                message.what = 1;
                //Log.i("主界面Broadcast", "收到" + message.obj.toString());
                mHandler.sendMessage(message);
            } else if(intent.hasExtra("mcastSendMsg")) {
                Message message = new Message();
                message.obj = intent.getStringExtra("mcastSendMsg");
                message.what = 2;
                //Log.i("主界面Broadcast", "發送" + message.obj.toString());
                mHandler.sendMessage(message);
            } else if(intent.hasExtra("mcastPollTimeout")) {
                Message message = new Message();
                message.what = 3;
                //Log.i("主界面Broadcast","逾時");
                mHandler.sendMessage(message);
            }
        }
    };
}