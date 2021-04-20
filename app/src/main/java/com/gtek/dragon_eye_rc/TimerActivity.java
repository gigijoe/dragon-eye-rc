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

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.gtek.dragon_eye_rc.TimerState.finishState;
import static com.gtek.dragon_eye_rc.TimerState.idleState;
import static com.gtek.dragon_eye_rc.TimerState.speedCourseState;
import static com.gtek.dragon_eye_rc.TimerState.thirtySecondState;

public class TimerActivity extends AppCompatActivity {
    private Timer mTimer;
    private TimerTask mTimerTask = null;
    private boolean isPause = false;
    private static int mCount = 0;
    private static int serNoA = -1, serNoB = -1;

    public TextView mTextView;

    private TimerState mTimerState = idleState;
    private boolean mOutside = false;
    private int mCourseCount = -1;

    private ThirtySecondTimer mThirtySecondTimer = new ThirtySecondTimer(30000, 10);
    private long mCountDownSecond = 30;

    private void onButtonStart() {
        if(mTimerState == idleState) {
            DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_go);
            mTimerState = thirtySecondState;
            mCountDownSecond = 30;
            mThirtySecondTimer.start();
            mCourseCount = -1;
            mOutside = false;
        }
    }

    private void onButtonStop() {
        if(mTimerState != idleState) {
            if(mTimerState == thirtySecondState) {
                DragonEyeApplication.getInstance().playPriorityTone(R.raw.smb_die);
                mThirtySecondTimer.cancel();
                mTimerState = finishState;
            } else if(mTimerState == speedCourseState) {
                DragonEyeApplication.getInstance().playPriorityTone(R.raw.smb_die);
                stopTimer();
                mTimerState = finishState;
            } else if(mTimerState == finishState) {
                mTextView.setText("0.00");
                mTimerState = idleState;
                DragonEyeApplication.getInstance().stopTone();
            }
        }
    }

    private void onBaseA() {
        if(mTimerState == thirtySecondState) {
            if(mOutside == false) { // Outside
                mOutside = true;
                DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_outside);
            } else { // Enter speed course
                mThirtySecondTimer.cancel();
                mTimerState = speedCourseState;
                mCourseCount = 0;
                startTimer();
                DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_a);
            }
        } else if(mTimerState == speedCourseState) {
            if(mOutside == false) { // Outside
                mOutside = true;
                DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_outside);
            } else {
                if(mCourseCount > 0 && mCourseCount % 2 == 0)
                    return;

                mCourseCount++;

                if (mCourseCount < 10)
                    DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_a);
                else {
                    stopTimer();
                    mTimerState = finishState;

                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            ArrayList<Integer> toneArray = new ArrayList<>();
                            toneArray.add(R.raw.r_e);
                            int v = mCount / 100;
                            if (v <= 20)
                                toneArray.add(numberToResourId(v));
                            else {
                                toneArray.add(numberToResourId((v / 10) * 10));
                                toneArray.add(numberToResourId(v % 10));
                            }
                            toneArray.add(R.raw.r_point);
                            v = mCount % 100;
                            toneArray.add(numberToResourId(v / 10));
                            toneArray.add(numberToResourId(v % 10));
                            DragonEyeApplication.getInstance().playTone(toneArray);
                            if(mCount < 3000) // Less than 30 seconds
                                toneArray.add(R.raw.smb_world_clear);
                        }
                    });
                    thread.start();
                }
            }
        }
    }

    private int numberToResourId(int number) {
        switch(number) {
            case 0: return R.raw.r_0;
            case 1: return R.raw.r_1;
            case 2: return R.raw.r_2;
            case 3: return R.raw.r_3;
            case 4: return R.raw.r_4;
            case 5: return R.raw.r_5;
            case 6: return R.raw.r_6;
            case 7: return R.raw.r_7;
            case 8: return R.raw.r_8;
            case 9: return R.raw.r_9;
            case 10: return R.raw.r_10;
            case 11: return R.raw.r_11;
            case 12: return R.raw.r_12;
            case 13: return R.raw.r_13;
            case 14: return R.raw.r_14;
            case 15: return R.raw.r_15;
            case 16: return R.raw.r_16;
            case 17: return R.raw.r_17;
            case 18: return R.raw.r_18;
            case 19: return R.raw.r_19;
            case 20: return R.raw.r_20;
            case 30: return R.raw.r_30;
            case 40: return R.raw.r_40;
            case 50: return R.raw.r_50;
            case 60: return R.raw.r_60;
            case 70: return R.raw.r_70;
            case 80: return R.raw.r_80;
            case 90: return R.raw.r_90;
            case 100: return R.raw.r_100;
        }
        return 0;
    }

    private void onBaseB() {
        if(mTimerState == speedCourseState) {
            if(mCourseCount < 10) {
                if(mCourseCount % 2 == 1)
                    return;
                mCourseCount++;
                if(mCourseCount < 9)
                    DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_b);
                else
                    DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_final);
            }
        } else if(mTimerState == finishState) { // Restart game
            mTimerState = idleState;
            onButtonStart();
        }
    }

    private void onThirtySecondTick(long tick) {
        float duration = (float)tick / 1000;
        mTextView.setText(String.format("%.2f", duration));

        if(mCountDownSecond - (tick / 1000) > 1) {
            mCountDownSecond--;
            if(mCountDownSecond == 20) {
                DragonEyeApplication.getInstance().playTone(R.raw.r_20);
            } else if(mCountDownSecond == 10) {
                DragonEyeApplication.getInstance().playTone(R.raw.r_10);
            } else if(mCountDownSecond == 9) {
                DragonEyeApplication.getInstance().playTone(R.raw.r_9);
            } else if(mCountDownSecond == 8) {
                DragonEyeApplication.getInstance().playTone(R.raw.r_8);
            } else if(mCountDownSecond == 7) {
                DragonEyeApplication.getInstance().playTone(R.raw.r_7);
            } else if(mCountDownSecond == 6) {
                DragonEyeApplication.getInstance().playTone(R.raw.r_6);
            } else if(mCountDownSecond == 5) {
                DragonEyeApplication.getInstance().playTone(R.raw.r_5);
            } else if(mCountDownSecond == 4) {
                DragonEyeApplication.getInstance().playTone(R.raw.r_4);
            } else if(mCountDownSecond == 3) {
                DragonEyeApplication.getInstance().playTone(R.raw.r_3);
            } else if(mCountDownSecond == 2) {
                DragonEyeApplication.getInstance().playTone(R.raw.r_2);
            } else if(mCountDownSecond == 1) {
                DragonEyeApplication.getInstance().playTone(R.raw.r_1);
            }
        }
    }

    private void onThirtySecondTimeout() {
        if(mTimerState == thirtySecondState) {
            mTimerState = speedCourseState;
            startTimer();
            DragonEyeApplication.getInstance().playTone(R.raw.smb_warning);
        }
    }

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
                onButtonStart();
/*
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startTimer();
                    }
                });
                thread.start();
 */
            }
        });

        FloatingActionButton bp = findViewById(R.id.button_stop_timer);
        bp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if(DragonEyeApplication.getInstance().mBaseList.isEmpty())
                //    return;
                onButtonStop();
                /*
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        stopTimer();
                    }
                });
                thread.start();
                 */
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
    }

    class ThirtySecondTimer extends CountDownTimer {
        public ThirtySecondTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            onThirtySecondTick(millisUntilFinished);
            //tv.setText("請等待：" + millisUntilFinished / 1000 + "秒...");
            //mTextView.setText(Float.toString(duration));
        }

        @Override
        public void onFinish() {
            //tv.setText("finish");
            onThirtySecondTimeout();
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
                                //DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_a);
                                onBaseA();
                                serNoA = i;
                            }
                        } else if (TextUtils.equals(baseHost[0], "TRIGGER_B")) {
                            int i = Integer.parseInt(baseHost[1]);
                            if(i != serNoB) {
                                //DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_b);
                                onBaseB();
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