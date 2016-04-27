package generalapps.vocal;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.util.JsonWriter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.github.lzyzsd.circleprogress.DonutProgress;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.Buffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by edeetee on 14/04/2016.
 */
public class Recorder {
    AudioRecord recorder;
    ToneGenerator toneGenerator;
    Timer toneTimer;
    TimerTask toneTask;
    Thread recordingThread;
    File audioFile;
    String timeString;
    Activity context;
    Audio audio;
    int beats = 0;
    int buffer;
    int FREQ = 44100;

    boolean recording = false;

    public Recorder(Activity context){
        this.context = context;

        toneTimer = new Timer();
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 20);
    }

    public void record(){
        timeString = new SimpleDateFormat("HH_mm_ss").format(Calendar.getInstance().getTime());
        audioFile = new File(context.getFilesDir() + "/audios/recording_" + timeString + ".wav");

        MusicAdapter adapter = (MusicAdapter)((ListView) context.findViewById(R.id.mainListView)).getAdapter();
        audio = new Audio();
        adapter.add(audio);
        audio.setName(timeString);

        buffer = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
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

        toneTask = new TimerTask() {
            @Override
            public void run() {
                beats++;
                if(beats < Rhythm.bpb)
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 50);
                else if (beats == Rhythm.bpb){
                    recordingThread.start();
                    recorder.startRecording();
                } else if (beats < Rhythm.bpb*Rhythm.maxBars){
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(audio != null)
                                audio.setBars(beats / Rhythm.bpb);
                        }
                    });
                } else {
                    stop();
                }
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    DonutProgress progressBar = (DonutProgress)context.findViewById(R.id.donut_progress);
                    progressBar.setProgress(beats % Rhythm.bpb+1);
                    }
                });
        } };

        toneTimer.scheduleAtFixedRate(toneTask, 0, Rhythm.msBeatPeriod());

        updateButton();
    }

    private void writeAudioDataToFile() {
        recording = true;

        short sData[] = new short[buffer/2];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(audioFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        boolean last = false;
        //long countAim = (60*Rhythm.bpb*FREQ)/Rhythm.bpm*Rhythm.bars;
        while (recording) { //isRecording() || last
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

            if(last)
                last = false;
            if(isRecording())
                last = true;
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recorder.stop();
        recorder.release();
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    public void stop(){
        //if the recording did not start, remove the empty audio view
        if(recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
            recording = false;

            File metaData = writeMetaData();
            audio.setMetaData(metaData);
            audio.setFile(audioFile);
        } else{
            MusicAdapter adapter = (MusicAdapter)((ListView) context.findViewById(R.id.mainListView)).getAdapter();
            adapter.delete(audio);
        }

        toneTask.cancel();

        audio = null;
        beats = 0;
        recordingThread = null;
        toneTask = null;
        updateButton();
    }

    public File writeMetaData() {
        File metaData = new File(context.getFilesDir() + "/audios/recording_" + timeString + ".json");
        try {
            FileWriter file = new FileWriter(metaData);
            JsonWriter writer = new JsonWriter(file);
            writer.beginObject();

            writer.name("Bars");
            writer.value(audio.bars);

            writer.name("BPM");
            writer.value(Rhythm.bpm);

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

    public boolean isRecording(){
        return audio != null;
    }

    public void updateButton(){
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //TODO Remove or do something here
//                Button button = (Button)context.findViewById(R.id.recordButton);
//                button.setText(isRecording() ? "Stop" : "Record");
            }
        });
    }
}
