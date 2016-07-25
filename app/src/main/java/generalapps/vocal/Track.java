package generalapps.vocal;

import android.support.annotation.UiThread;
import android.support.v7.widget.RecyclerView;
import android.test.UiThreadTest;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
public class Track implements Iterable<Audio>, Audio.OnAudioChangeListener {
    private List<Audio> audios;
    private File metaData;
    Audio first;
    File dir;
    private int msBarPeriodMod = 0;

    OnTrackChangeListener mCallback;

    @Override
    public void OnChange(Audio audio) {
        if(mCallback != null)
            mCallback.OnChanged(audios.indexOf(audio));
    }

    public void invalidateWaves(){
        for(Audio audio : audios){
            if(audio.holder != null)
                audio.holder.barTemplateAdapter.postInvalidateCurrent();
        }
    }

    public interface OnTrackChangeListener{
        void OnAdd(int pos);
        void OnRemoved(int pos);
        void OnChanged(int pos);
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
        mCallback.OnAdd(0);
        updateMetadata();
    }

    //only for use with load() static func
    private Track(File dir, List<Audio> audios, int msBarPeriodMod, RecorderAdapter callback){
        for(Audio audio : audios){
            audio.setTrack(this);
        }
        this.first = audios.get(0);
        this.audios = audios;
        this.msBarPeriodMod = msBarPeriodMod;
        this.dir = dir;

        mCallback = callback;
        callback.group = this;
    }

    @Override
    public Iterator iterator() {
        return audios.iterator();
    }

    public void changeMsBarPeriodMod(int msBarPeriodModChange){
        int newMsBarPeriodMod = this.msBarPeriodMod + msBarPeriodModChange;
        msBarPeriodMod = (int)Math.min(Math.max(-getFirstBarPeriodMs()*0.9, newMsBarPeriodMod), getFirstBarPeriodMs()*0.9);
        updateMetadata();
    }

    public int getFirstBarPeriodMs(){
        return Rhythm.ticksToMs(first.ticks);
    }

    public int getMsBarPeriod(){
        return getFirstBarPeriodMs()+msBarPeriodMod;
    }

    public void add(Audio audio){
        audio.setTrack(this);
        audios.add(audio);
        mCallback.OnAdd(audios.size()-1);
        updateMetadata();
    }

    public Audio get(int i){
        return audios.get(i);
    }

    public void remove(int i){
        if(i != 0){
            audios.get(i).delete();
            audios.remove(i);
            mCallback.OnRemoved(i);
            updateMetadata();
        } else {
            delete();
        }
    }

    public void remove(Audio audio){
        remove(audios.indexOf(audio));
    }

    public void delete(){
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
        try{
            metaData = new File(dir, "group.json");
            JSONObject metaObj = new JSONObject();
            JSONArray JSONaudios = new JSONArray();
            for(Audio audio : audios){
                JSONaudios.put(audio.name);
            }
            metaObj.put("audios", JSONaudios);
            metaObj.put("msBarPeriodMod", msBarPeriodMod);

            String output = metaObj.toString();

            new FileWriter(metaData).write(output);
            Utils.printFile("write", metaData);
        } catch(Exception e){
            Log.e("Track", "updateMetadata", e);
        }
    }

    static Track load(File dir, RecorderAdapter callback){
        try{
            File groupMetaData = new File(dir, "group.json");
            JSONObject metaObj = Utils.loadJSON(groupMetaData, "Track load: ");

            List<Audio> audios = new ArrayList<>();

            int msBarPeriodMod = metaObj.getInt("msBarPeriodMod");
            JSONArray JSONaudios = metaObj.getJSONArray("audios");
            for(int i = 0; i < JSONaudios.length(); i++){
                Audio curAudio = Audio.loadAudio(dir, JSONaudios.getString(i));
                if(curAudio != null)
                    audios.add(curAudio);
            }

            return new Track(dir, audios, msBarPeriodMod, callback);
        } catch (Exception e){
            String deletion = "Something occured while loading files. Deleting " + dir.getName() + "...";
            Log.e("AudioGroup Load", deletion, e);
            Toast toast = Toast.makeText(callback.mContext, deletion, Toast.LENGTH_SHORT);
            toast.show();
            Utils.deleteDirectory(dir);
        }
        return null;
    }

    public int msBeatPeriod(){
        return getMsBarPeriod() /Rhythm.bpb;
    }

    public int msMaxPeriod(){
        return getMsBarPeriod()*Rhythm.maxBars;
    }

    public int barTicks(){
        return getMsBarPeriod()*Recorder.FREQ/1000;
    }

    public int maxTicks(){
        return msMaxPeriod()*Recorder.FREQ/1000;
    }
}
