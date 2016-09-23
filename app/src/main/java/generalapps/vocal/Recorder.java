package generalapps.vocal;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
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

import generalapps.vocal.audioGen.AudioGenerator;

/**
 * Created by edeetee on 14/04/2016.
 */
public class Recorder {

    enum State {
        NONE, PREPARED, RECORDING, ENDING
    };
    private State state = State.NONE;

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
    Track group;

    interface OnRecorderStateChangeListener{
        void OnRecorderStateChange(State state);
    }
    OnRecorderStateChangeListener mCallback;

    public Recorder(Activity context){
        handler = new Handler();
        toneGenerator = new AudioGenerator(1000, FREQ/16, FREQ);
    }

    void setOnRecorderStateChangeListener(OnRecorderStateChangeListener callback){
        mCallback = callback;
    }

    public State getState(){
        return state;
    }

    void setState(State state){
        this.state = state;
        if(mCallback != null)
            mCallback.OnRecorderStateChange(state);
    }

    public void record(RecorderFragment frag){
        prepareRecord(frag.adapter);

        if(!first)
            startRecordingBeatIn(frag.recordProgress);
        else
            startRecording();
    }

    public void prepareRecord(RecorderAdapter adapter){
        group = adapter.group;
        first = group.size() == 0;

        timeString = new SimpleDateFormat("HH_mm_ss").format(Calendar.getInstance().getTime());

        audio = new Audio();
        audio.setName(timeString);
        adapter.group.add(audio);

        audioFile = new File(group.dir, timeString + ".wav");

        buffer = AudioRecord.getMinBufferSize(FREQ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                FREQ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffer);


        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");

        setState(State.PREPARED);
    }

    //region countDown
    private void startRecordingCountDown(RecorderCircle recordProgress){
        recordProgress.setDoHighText(true);
        countDownRunnable = new CountDownRunnable();
        countDownRunnable.recordProgress = recordProgress;
        recordProgress.post(countDownRunnable);
    }

    CountDownRunnable countDownRunnable;
    private class CountDownRunnable implements Runnable {
        int count = 3;
        RecorderCircle recordProgress;
        @Override
        public void run() {
            recordProgress.postDelayed(this, 500);
            if(count == 0){
                recordProgress.setText("Go");
                startRecording();
                recordProgress.removeCallbacks(this);
                count = 3;
            } else {
                recordProgress.setBeat(count);
                count--;
            }
        }
    };
    //endregion

    //region beatIn
    public void startRecordingBeatIn(RecorderCircle recordProgress){
        recordProgress.setDoHighText(true);
        recordProgress.doLoop(group);
        beatInRunnable = new BeatInRunnable();
        beatInRunnable.recordProgress = recordProgress;
        recordProgress.post(beatInRunnable);
    }

    BeatInRunnable beatInRunnable;
    private class BeatInRunnable implements Runnable {
        //cache current beats value
        int beat = 0;
        RecorderCircle recordProgress;
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
                startRecordingBeat(recordProgress);
            }
        }
    };
    //endregion

    //region beat
    private void startRecordingBeat(RecorderCircle recordProgress){
        recordProgress.doLoop(group);
        beatRunnable = new BeatRunnable();
        beatRunnable.recordProgress = recordProgress;
        handler.post(beatRunnable);
        startRecording();
    }

    BeatRunnable beatRunnable ;
    private class BeatRunnable implements Runnable {
        RecorderCircle recordProgress;
        @Override
        public void run() {
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

    public void startRecording(){
        if(state == State.PREPARED){
            recordingThread.start();
            recorder.startRecording();
            if(1 < group.size())
            for(Audio audio : group){
                audio.holder.barTemplateAdapter.setProgressCallback(recordProgressCallback);
            }
            recordingStart = System.currentTimeMillis();
        } else
            Log.w("Recording State", "State is " + state.name() + ". It should be " + State.PREPARED.name());
    }

    Callable<Float> recordProgressCallback = new Callable<Float>() {
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
        int wait = 1;

        while (state == State.RECORDING) {
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, buffer/2);

            if(wait != 0){
                wait--;
                continue;
            }

            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //generate wave values
            for(int i = 0; i<buffer/2; i++){
                //weird stuff to stop overloading values
                sum += Math.abs(sData[i]/(float)Short.MAX_VALUE);

                if((ticks+i) % WaveView.tickInterval == 0){
                    audio.waveValues.add(sum/WaveView.tickInterval);
                    sum = 0f;
                }
            }
            ticks += buffer/2;
            audio.setTicks(ticks);
            if(audio.holder != null)
                audio.holder.barTemplateAdapter.updateWaves();
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        postStop();
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
        }
        return bytes;
    }

    private boolean isLongEnough(){
        return 500 < System.currentTimeMillis() - recordingStart;
    }

    public void stop(){
        //if the recording did not start, remove the empty audio view. This determines if the audio data will be saved later
        if(state != State.RECORDING || !isLongEnough()){
            group.remove(audio);
        }

        handler.removeCallbacks(beatRunnable);

        setState(State.ENDING);

        if(recordingThread.isAlive()){
            try{
                recordingThread.join();
            } catch(InterruptedException e){
                Log.e("Recording thread", e.getMessage());
            }
        } else
            postStop();
    }

    private void postStop(){
        //if the audio is still in the group
        if(group.contains(audio)){
            audio.writeMetaData();
            audio.readFile();
            if(first)
                HowTo.Basic("Adjusting first recording",
                        "Click the left and right buttons to change the length of the track. Whatever length you choose will be the bar length for all consecutive tracks.",
                        MainActivity.context);
            else if(group.size() == 2){
                HowTo.Basic("Adjusting additional recordings",
                        "Click the left and right buttons to make your new recording line up with your other recordings",
                        MainActivity.context);
                HowTo.StopHelp(MainActivity.context);
            }
        }

        for(Audio audio : group){
            audio.holder.barTemplateAdapter.stopProgressCallback(recordProgressCallback);
        }

        recorder.stop();
        recorder.release();

        Log.i("Recorder", Float.toString((float)buffer/ticks));

        audio = null;
        beats = 0;
        ticks = 0;
        recordingThread = null;

        setState(State.NONE);
    }
}
