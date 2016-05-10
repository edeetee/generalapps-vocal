package generalapps.vocal;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.util.JsonWriter;
import android.widget.ListView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by edeetee on 14/04/2016.
 */
public class Recorder {
    AudioRecord recorder;
    ToneGenerator toneGenerator;
    Timer toneTimer;
    TimerTask beatTask;
    Thread recordingThread;
    File audioFile;
    String timeString;
    Activity context;
    Audio audio;
    //0 == 1. 1and2and3and4and (music signature)
    int beats = 0;
    List<Float> wavePoints;
    int buffer;
    static int FREQ = 44100;

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

        final RecorderCircle recordProgress = (RecorderCircle)context.findViewById(R.id.recordProgress);

        beatTask = new TimerTask() {
            @Override
            public void run() {
                //update record button beat value
                recordProgress.post(new Runnable() {
                    //cache current beats value
                    int curBeats = beats;
                    @Override
                    public void run() {
                        recordProgress.setBeat(curBeats%Rhythm.bpb+1);
                    }
                });

                //if pre-recording (metronome)
                recordProgress.doHeartBeat();
                if(beats < Rhythm.bpb)
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 50);
                //if start recording
                else if (beats == Rhythm.bpb){
                    recordingThread.start();
                    recorder.startRecording();
                //currently recording (update bars)
                } else if (beats < Rhythm.bpb*(Rhythm.maxBars+1)){
                    context.runOnUiThread(new Runnable() {
                        //cache current value
                        int curBeats = beats;
                        @Override
                        public void run() {
                            if(audio != null)
                                audio.setBars(curBeats / Rhythm.bpb);
                        }
                    });
                } else {
                    stop();
                }

                beats++;
        } };

        recordProgress.startLoop(Rhythm.msBarPeriod());

        toneTimer.scheduleAtFixedRate(beatTask, 0, Rhythm.msBeatPeriod());
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

        int totalI = 0;
        float sum = 0f;
        WaveView waveView = (WaveView)audio.view.findViewById(R.id.waveform);

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

            //generate wave values
            for(int i = 0; i<buffer/2; i++){
                //weird stuff to stop overloading values
                sum += Math.abs(sData[i]/(float)Short.MAX_VALUE);

                if((totalI+i) % (Rhythm.maxTicks()/WaveView.points) == 0){
                    audio.waveValues.add(sum);
                    sum = 0f;
                    waveView.postInvalidate();
                }
            }
            totalI += buffer/2;


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

        beatTask.cancel();

        audio = null;
        beats = 0;
        recordingThread = null;
        beatTask = null;

        RecorderCircle recordProgress = (RecorderCircle)context.findViewById(R.id.recordProgress);
        recordProgress.resetLoop();
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
}
