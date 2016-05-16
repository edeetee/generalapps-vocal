package generalapps.vocal;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.JsonWriter;
import android.util.Log;
import android.widget.ListView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import generalapps.vocal.audioGen.AudioGenerator;

/**
 * Created by edeetee on 14/04/2016.
 */
public class Recorder {

    enum State {
        NONE, PREPARED, RECORDING
    };
    private State state;

    AudioRecord recorder;
    AudioGenerator toneGenerator;
    Handler handler;
    Thread recordingThread;
    File audioFile;
    String timeString;
    Activity context;
    private Audio audio;
    //0 == 1. 1and2and3and4and (music signature)
    int beats = 0;
    List<Float> wavePoints;
    int buffer;
    static int FREQ = 44100;

    long recordingStart;
    boolean first = false;
    AudioGroup group;

    public Recorder(Activity context){
        this.context = context;

        handler = new Handler();
        toneGenerator = new AudioGenerator(1000, FREQ/16, FREQ);
    }

    public void record(){
        prepareRecord();

        if(!first)
            startRecordingBeatIn();
        else
            startRecordingCountDown();
    }

    public void prepareRecord(){
        MusicAdapter adapter = MainActivity.adapter;
        group = adapter.group;
        first = group == null;

        timeString = new SimpleDateFormat("HH_mm_ss").format(Calendar.getInstance().getTime());

        audio = new Audio();
        audio.setName(timeString);
        if(first){
            audio.setBars(1);
            group = new AudioGroup(audio);
        } else {
            adapter.group.add(audio);
        }

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

        state = state.PREPARED;
    }

    //region countDown
    private void startRecordingCountDown(){
        MainActivity.recordProgress.post(countDownRunnable);
    }

    private Runnable countDownRunnable = new Runnable() {
        int count = 3;
        @Override
        public void run() {
            MainActivity.recordProgress.postDelayed(this, 500);
            MainActivity.recordProgress.setBeat(count);
            if(count == 0){
                startRecording();
                MainActivity.recordProgress.removeCallbacks(this);
                count = 3;
            } else {
                count--;
            }

        }
    };
    //endregion

    //region beatIn
    public void startRecordingBeatIn(){
        MainActivity.recordProgress.startLoop(group.getMsBarPeriod());
        MainActivity.recordProgress.post(beatInRunnable);
    }

    private Runnable beatInRunnable = new Runnable() {
        //cache current beats value
        int beat = 0;
        @Override
        public void run() {
            MainActivity.recordProgress.postDelayed(this, group.msBeatPeriod());

            if(beat < Rhythm.bpb){
                toneGenerator.play();
                MainActivity.recordProgress.doHeartBeat(group.msBeatPeriod()/4);
                MainActivity.recordProgress.setBeat(beat+1);
                beat++;
            } else{
                MainActivity.recordProgress.resetLoop();
                MainActivity.recordProgress.removeCallbacks(this);
                beat = 0;
                startRecordingBeat();
            }
        }
    };
    //endregion

    //region beat
    private void startRecordingBeat(){
        MainActivity.recordProgress.startLoop(group.getMsBarPeriod());
        handler.post(beatRunnable);
        startRecording();
    }

    private Runnable beatRunnable = new Runnable() {
        @Override
        public void run() {
            //setup to run again
            handler.postDelayed(this, group.msBeatPeriod());

            //update record button beat value
            MainActivity.recordProgress.post(new Runnable() {
                //cache current beats value
                int curBeats = beats;
                @Override
                public void run() {
                    MainActivity.recordProgress.setBeat(curBeats%Rhythm.bpb+1);
                }
            });

            MainActivity.recordProgress.doHeartBeat(group.msBeatPeriod()/4);

            if (beats < Rhythm.bpb*Rhythm.maxBars){
                context.runOnUiThread(new Runnable() {
                    //cache current value
                    int curBeats = beats;
                    @Override
                    public void run() {
                        Log.i("setBars input", Integer.toString(curBeats));
                        if(audio != null)
                            audio.setBars(curBeats / Rhythm.bpb + 1);
                    }
                });
            } else {
                stop();
            }

            beats++;
        } };
    //endregion

    public void startRecording(){
        if(state == State.PREPARED){
            recordingThread.start();
            recorder.startRecording();
            recordingStart = System.currentTimeMillis();
        } else
            throw new IllegalStateException("State is " + state.name() + ". It should be " + State.PREPARED.name());
    }

    private void writeAudioDataToFile() {
        state = State.RECORDING;

        short sData[] = new short[buffer/2];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(audioFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int totalI = 0;
        float sum = 0f;
        WaveView waveView = (WaveView)audio.view.findViewById(R.id.waveform);

        while (state == State.RECORDING) {
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, buffer/2);

            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(!first){
                //generate wave values
                for(int i = 0; i<buffer/2; i++){
                    //weird stuff to stop overloading values
                    sum += Math.abs(sData[i]/(float)Short.MAX_VALUE);

                    if((totalI+i) % (group.maxTicks()/WaveView.points) == 0){
                        audio.waveValues.add(sum);
                        sum = 0f;
                        waveView.postInvalidate();
                    }
                }
                totalI += buffer/2;
            }
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

    public State getState(){
        return state;
    }

    public void stop(){
        //if the recording did not start, remove the empty audio view
        if(state == State.RECORDING){
            state = State.NONE;

            if(first){
                MainActivity.adapter.group.setMsBarPeriod((int)(System.currentTimeMillis() - recordingStart));
            }
        } else{
            MainActivity.adapter.group.remove(audio);
        }

        handler.removeCallbacks(beatRunnable);
        MainActivity.recordProgress.removeCallbacks(beatInRunnable);
        MainActivity.recordProgress.removeCallbacks(countDownRunnable);


        RecorderCircle recordProgress = (RecorderCircle)context.findViewById(R.id.recordProgress);
        recordProgress.resetLoop();

        try{
            recordingThread.join();
        } catch(InterruptedException e){
            Log.e("Recording thread", e.getMessage());
        }
    }

    private void postStop(){
        File metaData = writeMetaData();
        audio.setMetaData(metaData);
        audio.setFile(audioFile);

        recorder.stop();
        recorder.release();

        audio = null;
        beats = 0;
        recordingThread = null;
    }

    public File writeMetaData() {
        File metaData = new File(group.dir, timeString + ".json");
        try {
            FileWriter file = new FileWriter(metaData);
            JsonWriter writer = new JsonWriter(file);
            writer.beginObject();

            writer.name("Bars");
            writer.value(audio.bars);

            writer.name("First");
            writer.value(first);

            writer.name("Title");
            writer.value(timeString);

            writer.endObject();
            writer.close();
            file.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        return metaData;
    }
}
