package generalapps.vocal;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.ToneGenerator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by edeetee on 14/04/2016.
 */
public class Recorder {
    boolean recording;
    MediaRecorder recorder;
    ToneGenerator toneGenerator;
    Timer toneTimer;
    String fileName;
    long bpm;
    int beats;
    int bar;
    Activity context;

    public Recorder(Activity context){
        this.context = context;
        recorder = new MediaRecorder();
        toneTimer = new Timer();
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 10);

        recording = false;
        bpm = 100;
        beats = 0;
        bar = 4;
    }

    public void record(){
        recording = true;

        recorder.reset();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);

        String timeString = new SimpleDateFormat("HH_mm_ss").format(Calendar.getInstance().getTime());
        fileName = context.getFilesDir() + "/recording_" + timeString + ".3gp";
        recorder.setOutputFile(fileName);
        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recorder.start();

        toneTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(!recording)
                    cancel();
                toneGenerator.startTone(beats%bar == 0 ? ToneGenerator.TONE_DTMF_6 : ToneGenerator.TONE_DTMF_1, 100);


                beats++;
            } }, 0, 1000*60/bpm);
    }

    public void stop(){
        recording = false;
        recorder.stop();
    }

    public Audio getAudio(){
        return new Audio(fileName);
    }
}
