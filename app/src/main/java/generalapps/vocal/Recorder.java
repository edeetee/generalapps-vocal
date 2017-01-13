package generalapps.vocal;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import generalapps.vocal.audioGen.AudioGenerator;

/**
 * Created by edeetee on 14/04/2016.
 */
public class Recorder implements SharedPreferences.OnSharedPreferenceChangeListener {

    enum State {
        NONE, PREPARED, RECORDING, ENDING
    };
    private State state = State.NONE;

    private static final String TAG = "Recorder";

    AudioRecord recorder;
    AudioGenerator toneGenerator;
    Handler handler;
    Thread recordingThread;
    File audioFile;
    String timeString;
    private Audio audio;
    int ticks;
    //0 == 1. 1and2and3and4and (music signature)
    int beats = 0;
    int buffer;
    static public int FREQ = 44100;

    long recordingStart;
    boolean first = false;
    boolean isStereo;
    Track group;

    RecorderCircle recordProgress;

    interface OnRecorderStateChangeListener{
        void OnRecorderStateChange(State state);
    }
    OnRecorderStateChangeListener mCallback;

    public Recorder(Activity context){
        handler = new Handler();
        toneGenerator = new AudioGenerator(1000, FREQ/16, FREQ);
        initRecorder();
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        boolean newIsStereo = sharedPreferences.getBoolean("pref_stereo", false);
        if(isStereo != newIsStereo){
            isStereo = newIsStereo;
            initRecorder();
        }
    }

    void initRecorder(){
        int channel_in = isStereo ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;

        releaseRecorder();

        buffer = AudioRecord.getMinBufferSize(FREQ, channel_in, AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                FREQ,
                channel_in,
                AudioFormat.ENCODING_PCM_16BIT,
                buffer);
        recorder.startRecording();
    }

    void releaseRecorder(){
        if(recorder != null)
            recorder.release();
    }

    void setOnRecorderStateChangeListener(OnRecorderStateChangeListener callback){
        mCallback = callback;
    }

    public State getState(){
        return state;
    }

    private void setState(State state){
        this.state = state;
        if(mCallback != null)
            mCallback.OnRecorderStateChange(state);
    }

    void setRecordProgress(RecorderCircle recordProgress){
        this.recordProgress = recordProgress;
    }

    void record(Track group){
        prepareRecord(group);

        if(!first)
            startRecordingBeatIn();
        else
            startRecording();
    }

    public void prepareRecord(Track track){
        group = track;
        first = group.size() == 0;

        timeString = new SimpleDateFormat("HH_mm_ss").format(Calendar.getInstance().getTime());

        audio = new Audio();
        audio.setName(timeString);
        group.add(audio);

        audioFile = audio.getAudioFile();

        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");

        setState(State.PREPARED);
    }

    //region beatIn
    public void startRecordingBeatIn(){
        recordProgress.doLoop(group.getMsBarPeriod());
        beatInRunnable = new BeatInRunnable();
        recordProgress.post(beatInRunnable);
    }

    BeatInRunnable beatInRunnable;
    private class BeatInRunnable implements Runnable {
        //cache current beats value
        int beat = 0;
        @Override
        public void run() {
            recordProgress.postDelayed(this, group.msBeatPeriod());

            if(beat < Rhythm.bpb){
                toneGenerator.play();
                recordProgress.doHeartBeat(group.msBeatPeriod()/4);
                recordProgress.setBeat(beat+1);
                beat++;
            } else{
                recordProgress.stopLoop();
                recordProgress.removeCallbacks(this);
                beat = 0;
                startRecordingBeat();
            }
        }
    };
    //endregion

    //region beat
    void startRecordingBeat(){
        recordProgress.doLoop(group.getMsBarPeriod());
        beatRunnable = new BeatRunnable();
        handler.post(beatRunnable);
        startRecording();
    }

    private BeatRunnable beatRunnable ;
    private class BeatRunnable implements Runnable {
        @Override
        public void run() {
            //Log.i(TAG, "BeatRunnable: beats: " + beats + ", ticks: " + Rhythm.ticksToMs(ticks) + ", groupbarsMs: " + group.msMaxPeriod());

            if (beats == Rhythm.bpb*Rhythm.maxBars){
                stop();
            } else{
                //setup to run again
                handler.postDelayed(this, group.msBeatPeriod());

                //update record button beat value
                recordProgress.post(new Runnable() {
                    //cache current beats value
                    int curBeats = beats;
                    @Override
                    public void run() {
                        recordProgress.setBeat(curBeats%Rhythm.bpb+1);
                    }
                });

                recordProgress.doHeartBeat(group.msBeatPeriod()/4);

                beats++;
            }
        } };
    //endregion

    private void startRecording(){
        if(state == State.PREPARED){
            recordingThread.start();
            //recorder.startRecording();
            if(1 < group.size())
            for(Audio audio : group){
                audio.holder.barTemplateAdapter.setProgressCallback(recordProgressCallback);
            }
            recordingStart = System.currentTimeMillis();
        } else
            Log.w("Recording State", "State is " + state.name() + ". It should be " + State.PREPARED.name());
    }

    private Callable<Float> recordProgressCallback = new Callable<Float>() {
        @Override
        public Float call() throws Exception {
            return (float)Rhythm.ticksToMs(ticks)/(group.getMsBarPeriod()*Rhythm.maxBars);
        }
    };

    private void writeAudioDataToFile() {
        setState(State.RECORDING);

        short sData[] = new short[buffer/2];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(audioFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        ticks = 0;
        float sum = 0f;
        int skips = 15;

        while (state == State.RECORDING) {
            // gets the voice output from microphone to byte format

            int readValues = recorder.read(sData, 0, buffer/4);

            if(0 != skips){
                skips--;
                continue;
            }

            if(isStereo){
                readValues = readValues/2;
                for (int i = 0; i < readValues; i++) {
                    sData[i] = (short) ((sData[i*2] + sData[i*2+1]) /2);
                }
            }

            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, readValues*2);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //generate wave values
            for(int i = 0; i<readValues; i++){
                //weird stuff to stop overloading values
                sum += Math.abs(sData[i]/(float)Short.MAX_VALUE);

                if((ticks+i) % WaveView.tickInterval == 0){
                    audio.waveValues.add(sum/WaveView.tickInterval);
                    sum = 0f;
                }
            }
            ticks += readValues;
            audio.setTicks(ticks);
            if(audio.holder != null)
                audio.holder.barTemplateAdapter.updateWaves();
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                postStop();
            }
        });
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) sData[i];
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
        }
        return bytes;
    }

    private boolean isLongEnough(){
        return 500 < System.currentTimeMillis() - recordingStart;
    }

    private boolean shouldDelete;

    /**
     *
     * @return true if it recorded anything
     */
    public boolean stop(){
        //if the recording did not start, remove the empty audio view. This determines if the audio data will be saved later
        shouldDelete = state != State.RECORDING || !isLongEnough();

        handler.removeCallbacks(beatRunnable);

        setState(State.ENDING);

        if(recordingThread != null && recordingThread.isAlive()){
            try{
                recordingThread.join();
            } catch(InterruptedException e){
                Log.e("Recording thread", e.getMessage());
            }
        } else
            postStop();

        return !shouldDelete;
    }

    private void postStop(){
        if(shouldDelete)
            group.remove(audio.name);
        else{
            audio.writeMetaData();
            Log.i(TAG, "postStop: preparing to read file");
            audio.readFile();
            for(Audio audio : group){
                audio.holder.barTemplateAdapter.stopProgressCallback(recordProgressCallback);
            }
        }

        audio = null;
        beats = 0;
        ticks = 0;
        recordingThread = null;

        setState(State.NONE);
    }
}
