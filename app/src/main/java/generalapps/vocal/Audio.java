package generalapps.vocal;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by edeetee on 13/04/2016.
 */
public class Audio extends AudioTrack {
    AudioTrack track;
    String name;
    private File audioFile;
    private File metaData;
    int bpm;
    int bars = 0;
    int maxBeats;

    List<Float> waveValues;

    View view;
    MusicAdapter adapter;
    boolean enabled = true;

    public Audio(){
        super(AudioManager.STREAM_MUSIC, Recorder.FREQ, AudioFormat.CHANNEL_OUT_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, 500000, AudioTrack.MODE_STATIC);

        setPlaybackPositionUpdateListener(new OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack track) {
                stop();
            }

            @Override
            public void onPeriodicNotification(AudioTrack track) {

            }
        });
    }

    public Audio(File metaData){
        this();
        setMetaData(metaData);
    }

    public Audio(File audio, File metaData){
        this(metaData);
        setFile(audio);
    }

    public void setName(){
        if(view != null){
            TextView textView = (TextView)view.findViewById(R.id.name);
            textView.setText(name);
        }
    }

    public void setName(String name){
        this.name = name;
        setName();
    }

    public void setMetaData(File metaData){
        this.metaData = metaData;
        readMetaData();
        maxBeats = bars * Rhythm.bpb;
    }

    public void readMetaData(){
        try{
            FileReader fileReader = new FileReader(metaData);
            JsonReader reader = new JsonReader(fileReader);
            reader.beginObject();
            while(reader.hasNext()){
                String curName = reader.nextName();
                switch (curName){
                    case "Bars":
                        bars = reader.nextInt();
                        break;
                    case "BPM":
                        bpm = reader.nextInt();
                        break;
                    case "Title":
                        name = reader.nextString();
                        break;
                    default:
                        reader.skipValue();
                }
            }
            reader.endObject();
            reader.close();
            fileReader.close();

        } catch(IOException e){
            e.printStackTrace();
        }

        invalidateAdapter();
    }

    public void setFile(File audioFile){
        this.audioFile = audioFile;
        int length = (int)audioFile.length();
        byte[] bytes = new byte[length];
        short[] shortBuf = new short[length/2];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(audioFile));
            buf.read(bytes, 0, length);
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        float sum = 0;
        int values = 100;
        int maxTick = 4*Rhythm.bpb*Recorder.FREQ/2;
        float maxSum = 0;
        waveValues = new ArrayList<>();
        for(int i = 0; i<length/2; i++){
            short val = ( (short)( ( bytes[i*2] & 0xff )|( bytes[i*2 + 1] << 8 ) ) );
            shortBuf[i] = val;
            //weird stuff to stop overloading values
            sum += Math.abs(val/(float)Short.MAX_VALUE);

            if(i % (maxTick/values) == 0){
                waveValues.add(sum);
                if(maxSum < sum)
                    maxSum = sum;
                sum = 0;
            }
        }

        //normalize
        for(int i = 0; i < waveValues.size(); i++){
            waveValues.set(i, waveValues.get(i)/maxSum);
        }

        write(shortBuf, 0, length/2);
        setNotificationMarkerPosition(length/2);
    }

    @Override
    public String toString(){
        return name;
    }

    public boolean isPlaying(){
        return getPlayState() == PLAYSTATE_PLAYING;
    }

    public boolean canPlay(){
        return enabled && audioFile != null && metaData != null;
    }

    @Override
    public void play() throws IllegalStateException {
        if(canPlay())
            super.play();
        else
            Log.w("'Hol up", "Audio name:" + name + " is not loaded and/or enabled");
    }

    public void restart(){
        stop();
        setPlaybackHeadPosition(0);
        play();
    }

    public void setBars(){
        if(view != null){
            TextView barsView = (TextView)view.findViewById(R.id.bars);
            barsView.setText(Integer.toString(bars) +  (bars == 1 ? " Bar" : " Bars"));
        }
    }

    public void setBars(int bars){
        this.bars = bars;
        invalidateAdapter();
        //setBars();
    }

    public void invalidateAdapter(){
        if(adapter != null){
            MainActivity.context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetInvalidated();
                }
            });
        }
    }

    public void delete(){
        try{
            stop();
            release();
        } catch (IllegalStateException e){
            Log.w("IllegalState", "Audio " + name + " is in an illegal state. Continuing with delete.");

        }
        if(audioFile != null)
            audioFile.delete();
        if(metaData != null)
            metaData.delete();
    }
}
