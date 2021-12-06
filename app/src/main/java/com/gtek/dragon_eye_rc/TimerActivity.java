package com.gtek.dragon_eye_rc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import static com.gtek.dragon_eye_rc.TimerState.finishState;
import static com.gtek.dragon_eye_rc.TimerState.idleState;
import static com.gtek.dragon_eye_rc.TimerState.speedCourseState;
import static com.gtek.dragon_eye_rc.TimerState.thirtySecondState;

public class TimerActivity extends AppCompatActivity {
    private Timer mTimer;
    private TimerTask mTimerTask = null;
    private static int serNoA = 0, serNoB = 0;

    private long mStartTime = 0;

    private TextView mTextViewDuration;
    private TextView mTextViewTimerStatus;
    private Button mButtonA, mButtonB;

    private TimerState mTimerState = idleState;
    private boolean mOutside = false;
    private AtomicInteger mCourseCount = new AtomicInteger(-1);

    private ThirtySecondTimer mThirtySecondTimer = new ThirtySecondTimer(30000, 10);
    private long mCountDownSecond = 30;

    private RepeatTriggerTimer mRepeatTriggerTimer = new RepeatTriggerTimer(1000, 10);
    private AtomicInteger mRepeatTriggerTick = new AtomicInteger(0);

    private ListView mListViewResults;
    private ArrayList<String> mResults = new ArrayList<String>();
    private ArrayAdapter<String> mListViewResultsAdapter;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1: /* Timeout */
                    onButtonStop();
            }
        }
    };

    private void exitDialog() {
        AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(this);
        MyAlertDialog.setTitle("Exit F3F Timer");
        MyAlertDialog.setMessage("Are you sure ?");
        MyAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        MyAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        MyAlertDialog.show();
    }

    @Override
    public void onBackPressed() {
        exitDialog();
    }

    private void onButtonStart() {
        if(mTimerState == idleState) {
            DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_go);
            stopTimer();
            mTimerState = thirtySecondState;
            mCountDownSecond = 30;
            mThirtySecondTimer.start();
            mCourseCount.set(-1);
            mOutside = false;
            mTextViewTimerStatus.setText("Thirty seconds ...");
        }
    }

    private void onButtonStop() {
        if(mTimerState == thirtySecondState) {
            DragonEyeApplication.getInstance().playPriorityTone(R.raw.smb_die);
            mThirtySecondTimer.cancel();
            mTimerState = finishState;
            mTextViewTimerStatus.setText("Cancelled !!!");
        } else if(mTimerState == speedCourseState) {
            DragonEyeApplication.getInstance().playPriorityTone(R.raw.smb_die);
            stopTimer();
            mTimerState = finishState;
            mTextViewTimerStatus.setText("Cancelled !!!");
        } else if(mTimerState == finishState) {
            mTextViewDuration.setText("0.00");
            mTextViewTimerStatus.setText("Press Start ...");
            mTimerState = idleState;
            DragonEyeApplication.getInstance().stopTone();
        }
    }

    private void onBaseA() {
        if(mTimerState == thirtySecondState) {
            if(!mOutside) { // Outside
                mOutside = true;
                //System.out.println("mCourseCount.get() = " + mCourseCount.get());
                DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_outside);
                mTextViewTimerStatus.setText("Outside !!!");
            } else { // Enter speed course
                mThirtySecondTimer.cancel();
                mTimerState = speedCourseState;
                mCourseCount.incrementAndGet();
                //System.out.println("mCourseCount.get() = " + mCourseCount.get());
                startTimer();
                DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_a);
                mTextViewTimerStatus.setText("Course " + Integer.toString(mCourseCount.get() + 1));
            }
        } else if(mTimerState == speedCourseState) {
            if(!mOutside) { // Outside
                mOutside = true;
                //System.out.println("mCourseCount.get() = " + mCourseCount.get());
                DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_outside);
                mTextViewTimerStatus.setText("Outside !!!");
            } else {
                if(mCourseCount.get() % 2 == 0)
                    return;

                mCourseCount.incrementAndGet();
                //System.out.println("mCourseCount.get() = " + mCourseCount.get());

                if (mCourseCount.get() < 10) {
                    DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_a);
                    mTextViewTimerStatus.setText("Course " + Integer.toString(mCourseCount.get() + 1));
                } else {
                    long millis = System.currentTimeMillis() - mStartTime;

                    stopTimer();
                    mTimerState = finishState;
                    mTextViewTimerStatus.setText("Finished ...");

                    if(millis >= 200000) // Over 200 secs, do not play voice
                        return;

                    float duration = (float)millis / 1000;
                    mTextViewDuration.setText(String.format("%.2f", duration));
                    //System.out.println("Result = " + String.format("%.2f", duration));

                    mResults.add(0, String.format("%.2f", duration));
                    //mListViewResults.deferNotifyDataSetChanged();
                    mListViewResultsAdapter.notifyDataSetChanged();

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

                            int v = (int) (millis / 1000);
                            if (v <= 20) {
                                toneArray.add(numberToResourId(v));
                            } else if(v < 100) {
                                toneArray.add(numberToResourId((v / 10) * 10));
                                toneArray.add(numberToResourId(v % 10));
                            } else if(v < 200){
                                toneArray.add(numberToResourId((v / 100) * 100));
                                int vv = v % 100;
                                if(vv <= 20)
                                    toneArray.add(numberToResourId(vv));
                                else {
                                    toneArray.add(numberToResourId((vv / 10) * 10));
                                    toneArray.add(numberToResourId(vv % 10));
                                }
                            }
                            toneArray.add(R.raw.r_point);
                            v = (int) (millis % 1000) / 10;
                            toneArray.add(numberToResourId(v / 10));
                            toneArray.add(numberToResourId(v % 10));
                            DragonEyeApplication.getInstance().playTone(toneArray);
                            if(millis < 30000) // Less than 30 seconds
                                toneArray.add(R.raw.smb_world_clear);
                            else if(millis < 40000) // Less than 40 seconds
                                toneArray.add(R.raw.smb_stage_clear);
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
            if(mCourseCount.get() < 10) {
                if(mCourseCount.get() == -1 || mCourseCount.get() % 2 == 1)
                    return;
                mCourseCount.incrementAndGet();
                //System.out.println("mCourseCount.get() = " + mCourseCount.get());
                mTextViewTimerStatus.setText("Course " + Integer.toString(mCourseCount.get() + 1));
                if(mCourseCount.get() < 9)
                    DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_b);
                else
                    DragonEyeApplication.getInstance().playPriorityTone(R.raw.r_final);
            }
        } else if(mTimerState == finishState) { // Restart game
            mTimerState = idleState;
            //System.out.println("mCourseCount.get() = " + mCourseCount.get());
            mTextViewTimerStatus.setText("Thirty seconds ...");
            onButtonStart();
        }
    }

    private void onThirtySecondTick(long tick) {
        float duration = (float)tick / 1000;
        mTextViewDuration.setText(String.format("%.2f", duration));

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

        mTextViewDuration = (TextView) findViewById(R.id.textViewDuration);
        mTextViewTimerStatus = (TextView) findViewById(R.id.textViewTimerStatus);

        mButtonA = (Button) findViewById(R.id.buttonA);
        mButtonA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBaseA();
            }
        });

        mButtonB = (Button) findViewById(R.id.buttonB);
        mButtonB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBaseB();
            }
        });

        mListViewResults = (ListView)findViewById(R.id.resultList);
        mListViewResultsAdapter = new ArrayAdapter<String>(this, R.layout.result_content, mResults);
        mListViewResults.setAdapter(mListViewResultsAdapter);

        //registerReceiver(broadcastReceiver, new IntentFilter("mcastMsg"));
        //registerReceiver(broadcastReceiver, new IntentFilter("udpMsg"));
        //registerReceiver(broadcastReceiver, new IntentFilter("usbMsg"));

        FloatingActionButton bs = findViewById(R.id.button_start_timer);
        bs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonStart();
            }
        });

        FloatingActionButton bp = findViewById(R.id.button_stop_timer);
        bp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonStop();
            }
        });
    }

    @Override
    protected void onDestroy() {
        mRepeatTriggerTimer.cancel();
        mThirtySecondTimer.cancel();
        stopTimer();
        DragonEyeApplication.getInstance().stopTone();
        //unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    private void TimerMethod()
    {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.
        if(System.currentTimeMillis() - mStartTime >= 200000) { /* 200 seconds timeout */
            Message message = new Message();
            message.obj = "timeout";
            message.what = 1;
            mHandler.sendMessage(message);
        }

        long millis = System.currentTimeMillis() - mStartTime;
        float duration = (float)millis / 1000;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextViewDuration.setText(String.format("%.2f", duration));
            }
        });
    }

    private void startTimer(){
        mStartTime = System.currentTimeMillis();
        if (mTimer == null) {
            mTimer = new Timer();
        }

        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    TimerMethod();
                }
            };
        }

        if(mTimer != null && mTimerTask != null )
            mTimer.schedule(mTimerTask, 0, 10);
    }

    private void stopTimer() {
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
            //mTextViewDuration.setText(Float.toString(duration));
        }

        @Override
        public void onFinish() {
            //tv.setText("finish");
            onThirtySecondTimeout();
        }
    }

    class RepeatTriggerTimer extends CountDownTimer {
        public RepeatTriggerTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mRepeatTriggerTick.incrementAndGet();
        }

        @Override
        public void onFinish() {
            mRepeatTriggerTick.set(0);
        }
    }

    void onBaseTrigger(DragonEyeBase b, String s) {
        char base = s.charAt(1);
        int serNo = Integer.parseInt(s.substring(2, s.length() - 1));
        if(base == 'A' && serNo != serNoA) {
            if(mRepeatTriggerTick.get() == 0) {
                mRepeatTriggerTimer.cancel();
                mRepeatTriggerTimer.start();
                TimerActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBaseA();
                    }
                });
            }
            serNoA = serNo;
        }else if(base == 'B' && serNo != serNoB) {
            if(mRepeatTriggerTick.get() == 0) {
                mRepeatTriggerTimer.cancel();
                mRepeatTriggerTimer.start();
                TimerActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBaseB();
                    }
                });
            }
            serNoB = serNo;
        }
    }
/*
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("udpMsg")) {
                if (intent.hasExtra("udpRcvMsg")) {
                    onUdpRx(intent.getStringExtra("udpRcvMsg"));
                }
            } else if(intent.getAction().equals("mcastMsg")) {
                if (intent.hasExtra("mcastRcvMsg")) {
                    onMulticastRx(intent.getStringExtra("mcastRcvMsg"));
                }
            } else if(intent.getAction().equals("usbMsg")) {
                if (intent.hasExtra("usbRcvMsg")) {
                    onUsbRx(intent.getStringExtra("usbRcvMsg"));
                }
            }
        }
    };
 */
}