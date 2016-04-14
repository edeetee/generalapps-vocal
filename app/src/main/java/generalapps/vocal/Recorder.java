package generalapps.vocal;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
    String fileName;
    long bpm;
    int bpb;
    int bars;
    Activity context;
    Audio audio;

    int buffer;

    public Recorder(Activity context){
        this.context = context;

        toneTimer = new Timer();
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 50);

        bpm = 100;
        bpb = 4;
        bars = 1;
    }

    public void record(){
        String timeString = new SimpleDateFormat("HH_mm_ss").format(Calendar.getInstance().getTime());
        fileName = context.getFilesDir() + "/recording_" + timeString + ".wav";

        MusicAdapter adapter = (MusicAdapter)((ListView) context.findViewById(R.id.mainListView)).getAdapter();
        audio = new Audio(bars*bpb, timeString);
        adapter.add(audio);

        buffer = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffer);

        recorder.startRecording();

        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

        toneTask = new TimerTask() {
            @Override
            public void run() {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toneGenerator.startTone(audio.beats%bpb == 0 ? ToneGenerator.TONE_DTMF_6 : ToneGenerator.TONE_DTMF_1, 100);
                        audio.beats++;

                        ProgressBar progressBar = (ProgressBar)audio.view.findViewById(R.id.progressBar);
                        progressBar.setProgress(audio.beats);

                        if(audio.maxBeats <= audio.beats)
                            stop();
                    }
                });

            } };

        updateButton();
    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte

        short sData[] = new short[buffer/2];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        boolean first = true;
        while (isRecording()) {
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, buffer/2);

            if(first) {
                first = false;
                toneTimer.scheduleAtFixedRate(toneTask, 0, 1000*60/bpm);
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
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        recorder.stop();
        recorder.release();

        audio.setFile(fileName);
        audio = null;
        recordingThread = null;

        toneTask.cancel();
        toneTask = null;
        updateButton();
    }

    public boolean isRecording(){
        return audio != null;
    }

    public void updateButton(){
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button button = (Button)context.findViewById(R.id.recordButton);
                button.setText(isRecording() ? "Stop" : "Record");
            }
        });
    }
}
