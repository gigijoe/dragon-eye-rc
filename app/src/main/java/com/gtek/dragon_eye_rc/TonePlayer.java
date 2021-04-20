package com.gtek.dragon_eye_rc;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TonePlayer implements Runnable {
    // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，1000，11025
    private static int sampleRateInHz = 8000;
    // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    //private static int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
    private static int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
    // 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private byte[] mBuffer;
    // 缓冲区字节大小
    private int bufferSizeInBytes = 0;
    //private static final int BUFFER_SIZE = 2048;
    private AudioTrack audioTrack;
    private static int resourceId;
    private Context mContext;
    private final AtomicBoolean playing = new AtomicBoolean(false);

    public TonePlayer(Context context) {
        mContext = context;
        bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz,
                channelConfig,
                audioFormat);
    }

    public void startPlay(int audioResourceId) {
        resourceId = audioResourceId;

        if(audioTrack == null) {
                audioTrack = new AudioTrack(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build(),
                        new AudioFormat.Builder()
                                .setSampleRate(sampleRateInHz)
                                .setEncoding(audioFormat)
                                .setChannelMask(channelConfig).build(),
                        bufferSizeInBytes,
                        AudioTrack.MODE_STREAM,
                        AudioManager.AUDIO_SESSION_ID_GENERATE);
        }
        audioTrack.play();

        playing.set(true);
    }

    public void stopPlay() {
        playing.set(false);

        if(audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }

    public boolean isPlaying() {
        return playing.get();
    }

    private void play(byte[] data) {
        if(audioTrack == null)
            return;
        //if(audioTrack.getPlayState() == 1)
        audioTrack.write(data, 0, data.length);
    }

    @Override
    public void run() {
        byte[] buffer = new byte[160];
        InputStream is;
        is = mContext.getResources().openRawResource(resourceId);

        try {
            while (is.read(buffer) != -1) {
                play(buffer);
                if(!playing.get())
                    break;
            }
            playing.set(false);
            System.out.println("Finish tone ...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
