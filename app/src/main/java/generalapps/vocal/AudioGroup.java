package generalapps.vocal;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.BufferedReader;
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
public class AudioGroup implements Iterable<Audio> {
    private List<Audio> audios;
    private File metaData;
    Audio first;
    File dir;
    private int msBarPeriod;
    private int msBarPeriodMod = 0;

    public AudioGroup(Audio first){
        audios = new ArrayList<>();

        //manually add just for first value
        first.group = this;
        this.first = first;
        audios.add(first);

        MainActivity.adapter.setGroup(this);

        generateDir();
        changed();
    }

    //only for use with load() static func
    private AudioGroup(File dir, List<Audio> audios, int msBarPeriod, int msBarPeriodMod){
        for(Audio audio : audios){
            audio.group = this;
        }
        this.first = audios.get(0);
        this.audios = audios;
        this.msBarPeriod = msBarPeriod;
        this.msBarPeriodMod = msBarPeriodMod;
        this.dir = dir;

        MainActivity.adapter.setGroup(this);
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
        this.msBarPeriodMod += msBarPeriodModChange;
        updateMetadata();
    }

    public int getMsBarPeriod(){
        return msBarPeriod+msBarPeriodMod;
    }

    public void add(Audio audio){
        audio.group = this;
        audios.add(audio);
        changed();
    }

    public Audio get(int i){
        return audios.get(i);
    }

    private void changed(){
        MainActivity.adapter.notifyDataSetChanged();
        MainActivity.context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.adapter.notifyDataSetChanged();
            }
        });
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
        if(MainActivity.adapter.playing)
            MainActivity.adapter.stop();

        for(Audio audio : audios){
            audio.delete();
        }
        audios.clear();
        first = null;
        if(metaData != null)
            metaData.delete();
        if(dir != null)
            dir.delete();
        MainActivity.adapter.notifyDataSetInvalidated();
        MainActivity.adapter.group = null;
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

    static AudioGroup load(File dir){
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
                                audios.add(Audio.loadAudio(dir, reader.nextString()));
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
                Utils.deleteDirectory(dir);
            }

            return new AudioGroup(dir, audios, msBarPeriod, msBarPeriodMod);
        } catch (Exception e){
            Log.w("AudioGroup Load", "Something occured while loading files. Deleting...", e);
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

    public int maxTicks(){
        return msMaxPeriod()*Recorder.FREQ/1000;
    }
}
