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
    Activity context;
    Audio audio;
    int buffer;
    int FREQ = 44100;

    public Recorder(Activity context){
        this.context = context;

        toneTimer = new Timer();
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 20);
    }

    public void record(){
        String timeString = new SimpleDateFormat("HH_mm_ss").format(Calendar.getInstance().getTime());
        fileName = context.getFilesDir() + "/audios/recording_" + timeString + ".wav";

        MusicAdapter adapter = (MusicAdapter)((ListView) context.findViewById(R.id.mainListView)).getAdapter();
        audio = new Audio(Rhythm.totalBeats(), timeString);
        adapter.add(audio);

        buffer = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                FREQ,
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
                audio.beats++;

                if(audio.maxBeats+1 <= audio.beats){
                    stop();
                    return;
                }

                ProgressBar progressBar = (ProgressBar)audio.view.findViewById(R.id.progressBar);
                progressBar.setProgress(audio.beats);

                toneGenerator.startTone(Rhythm.posInBar(audio.beats) == 0 ? ToneGenerator.TONE_DTMF_6 : ToneGenerator.TONE_DTMF_1, 100);
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
        boolean last = false;
        int count = 0;
        long countAim = (60*Rhythm.bpb*FREQ)/Rhythm.bpm;
        while (count < countAim) { //isRecording() || last
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, buffer/2);
            count += buffer/2;

            if(first) {
                first = false;
                toneTimer.scheduleAtFixedRate(toneTask, 0, Rhythm.msBeatPeriod());
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
        //recorder.stop();
        toneTask.cancel();

        //recorder.release();
        audio.setFile(fileName);

        audio = null;
        recordingThread = null;
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
