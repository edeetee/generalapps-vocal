package generalapps.vocal;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by edeetee on 11/05/2016.
 */
public class Track implements Iterable<Audio>, Audio.OnAudioChangeListener, WaveView.WaveValues.OnMaxWaveValueChangedListener {
    private List<Audio> audios;
    private File metaData;
    Audio first;
    File dir;
    private int msBarPeriod;
    private int msBarPeriodMod = 0;

    OnTrackChangeListener mCallback;

    @Override
    public void OnChange() {
        if(mCallback != null)
            mCallback.OnChange();
    }

    private float maxWaveVal = 0f;

    @Override
    public void OnMaxWaveValueChanged(float max) {
        if(maxWaveVal < max){
            maxWaveVal = max;
        }
    }

    @Override
    public float GetMaxWaveValue() {
        return maxWaveVal;
    }

    public void invalidateWaves(){
        for(Audio audio : audios){
            if(audio.view != null)
                audio.view.findViewById(R.id.waveform).postInvalidate();
        }
    }

    public interface OnTrackChangeListener{
        void OnChange();
        void OnDelete();
    }

    public Track(Audio first, RecorderAdapter callback){
        audios = new ArrayList<>();

        //manually add just for first value
        first.setTrack(this);
        this.first = first;
        audios.add(first);
        mCallback = callback;
        callback.group = this;

        generateDir();
        changed();
    }

    //only for use with load() static func
    private Track(File dir, List<Audio> audios, int msBarPeriod, int msBarPeriodMod, RecorderAdapter callback){
        for(Audio audio : audios){
            audio.setTrack(this);
        }
        this.first = audios.get(0);
        this.audios = audios;
        this.msBarPeriod = msBarPeriod;
        this.msBarPeriodMod = msBarPeriodMod;
        this.dir = dir;

        mCallback = callback;
        callback.group = this;
    }

    @Override
    public Iterator iterator() {
        return audios.iterator();
    }

    public void setMsBarPeriod(int msBarPeriod){
        this.msBarPeriod = msBarPeriod;
        updateMetadata();
    }

    public void changeMsBarPeriodMod(int msBarPeriodModChange){
        int newMsBarPeriodMod = this.msBarPeriodMod + msBarPeriodModChange;
        msBarPeriodMod = (int)Math.min(Math.max(-msBarPeriod*0.9, newMsBarPeriodMod), msBarPeriod*0.9);
        updateMetadata();
    }

    public int getMsBarPeriod(){
        return msBarPeriod+msBarPeriodMod;
    }

    public void add(Audio audio){
        audio.setTrack(this);
        audios.add(audio);
        changed();
    }

    public Audio get(int i){
        return audios.get(i);
    }

    private void changed(){
        if(mCallback != null)
            mCallback.OnChange();
        updateMetadata();
    }

    public void remove(int i){
        remove(audios.get(i));
    }

    public void remove(Audio audio){
        if(audios.indexOf(audio) != 0){
            audio.delete();
            audios.remove(audio);
            changed();
        } else {
            delete();
        }
    }

    public void delete(){
        if(mCallback != null)
            mCallback.OnDelete();

        for(Audio audio : audios){
            audio.delete();
        }
        audios.clear();
        first = null;
        if(metaData != null)
            metaData.delete();
        if(dir != null)
            Utils.deleteDirectory(dir);
    }

    public int size(){
        return audios.size();
    }

    private void generateDir(){
        dir = new File(MainActivity.context.getFilesDir() + "/audios/" + audios.get(0).name);
        dir.mkdir();
    }

    private void updateMetadata(){
        metaData = new File(dir, "group.json");
        try {
            JsonWriter writer = new JsonWriter(new FileWriter(metaData));

            writer.beginObject();

            writer.name("audios");
            writer.beginArray();
            for(Audio audio : audios){
                writer.value(audio.name);
            }
            writer.endArray();

            writer.name("msBarPeriod");
            writer.value(msBarPeriod);

            writer.name("msBarPeriodMod");
            writer.value(msBarPeriodMod);

            writer.endObject();

            writer.close();

            Utils.printFile("write", metaData);
        } catch (IOException e){
            e.printStackTrace();
        }


    }

    static Track load(File dir, RecorderAdapter callback){
        try{
            File groupMetaData = new File(dir, "group.json");
            Utils.printFile("load", groupMetaData);
            List<Audio> audios = new ArrayList<>();
            int msBarPeriod = 0;
            int msBarPeriodMod = 0;
            try {
                FileReader fileReader = new FileReader(groupMetaData);
                JsonReader reader = new JsonReader(fileReader);
                reader.beginObject();
                while(reader.hasNext()){
                    String curName = reader.nextName();
                    switch (curName){
                        case "audios":
                            reader.beginArray();

                            while(reader.hasNext()){
                                Audio curAudio = Audio.loadAudio(dir, reader.nextString());
                                if(curAudio != null)
                                    audios.add(curAudio);
                            }

                            reader.endArray();
                            break;
                        case "msBarPeriod":
                            msBarPeriod = reader.nextInt();
                            break;
                        case "msBarPeriodMod":
                            msBarPeriodMod = reader.nextInt();
                            break;
                        default:
                            reader.skipValue();
                    }
                }
                reader.endObject();
                reader.close();
                fileReader.close();
            } catch (IOException e){
                Log.e("AudioGroup Load", "Loading failed during metadata parsing", e);
                //Utils.deleteDirectory(dir);
            }

            return new Track(dir, audios, msBarPeriod, msBarPeriodMod, callback);
        } catch (Exception e){
            Log.w("AudioGroup Load", "Something occured while loading files. Deleting...", e);
            //Utils.deleteDirectory(dir);
        }
        return null;
    }

    public int msBeatPeriod(){
        return getMsBarPeriod() /Rhythm.bpb;
    }

    public int msMaxPeriod(){
        return getMsBarPeriod()*Rhythm.maxBars;
    }

    public int maxTicks(){
        return msMaxPeriod()*Recorder.FREQ/1000;
    }
}
