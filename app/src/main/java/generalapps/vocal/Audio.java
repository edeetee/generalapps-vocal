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
import java.io.FileFilter;
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
    File audioFile;
    File metaData;
    int bars = 0;
    int ticks;
    int maxBeats;

    WaveView.WaveValues waveValues = new WaveView.WaveValues();

    View view;
    AudioGroup group;
    //is audio enabled in ui
    boolean enabled = true;

    public Audio(){
        //TODO make AudioGroup and Audio constructors linked so that Recorder.FREQ*Rhythm.msMaxPeriod()/1000 can be used for buffer
        super(AudioManager.STREAM_MUSIC, Recorder.FREQ, AudioFormat.CHANNEL_OUT_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, 1000000, AudioTrack.MODE_STATIC);
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

    public void setEnabled(boolean enabled){
        if(this.enabled && !enabled)
            stop();

        this.enabled = enabled;
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
        ticks = length/2;
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
        waveValues.clear();
        for(int i = 0; i<ticks; i++){
            short val = ( (short)( ( bytes[i*2] & 0xff )|( bytes[i*2 + 1] << 8 ) ) );
            shortBuf[i] = val;
            //weird stuff to stop overloading values
            sum += Math.abs(val/(float)Short.MAX_VALUE);

            if(i % (ticks/WaveView.points) == 0){
                waveValues.add(sum);
                sum = 0;
            }
        }

        int written = write(shortBuf, 0, ticks);
        if(written < ticks)
            Log.e("AudioWrite", "Audio write failed. Length: " + Integer.toString(ticks) + ", Written: " + Integer.toString(written));

        setNotificationMarkerPosition(ticks);
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
        if(isPlaying())
            stop();
        if(getPlaybackHeadPosition() != 0)
            setPlaybackHeadPosition(0);
        play();
    }

    public void setBars(){
        if(view != null){
            TextView barsView = (TextView)view.findViewById(R.id.bars);
            barsView.setText(Integer.toString(bars) +  (bars == 1 ? " Bar" : " Bars"));
        }
    }

    public void setBars(int tryBars){
        for(int bar : Rhythm.barTypes){
            if(tryBars <= bar){
                this.bars = bar;
                break;
            }
        }
        invalidateAdapter();
    }

    public void invalidateAdapter(){
        if(MainActivity.adapter != null){
            MainActivity.context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MainActivity.adapter.notifyDataSetChanged();
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
        waveValues.clear();
        if(audioFile != null)
            audioFile.delete();
        if(metaData != null)
            metaData.delete();
    }

    public static Audio loadAudio(File dir, final String name){
        File metaFile = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().equals(name + ".json");
            }
        })[0];
        File audioFile = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().equals(name + ".wav");
            }
        })[0];
        return new Audio(audioFile, metaFile);
    }
}