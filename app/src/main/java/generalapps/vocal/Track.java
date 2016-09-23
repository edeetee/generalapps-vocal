package generalapps.vocal;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by edeetee on 11/05/2016.
 */
public class Track implements Iterable<Audio>, Audio.OnAudioChangeListener {
    private List<Audio> audios;
    private Map<String, EditorItem> editors;
    Audio first;
    File dir;
    StorageReference cloudDir;
    DatabaseReference audioMetaRef;
    int msBarPeriodMod = 0;
    String key;

    private OnTrackChangeListener mCallback = new OnTrackChangeListener() {
        @Override
        public void OnLoad(Track track) {
            for (OnTrackChangeListener callback : mCallbacks) {
                callback.OnLoad(track);
            }
        }

        @Override
        public void OnAdd(int pos) {
            for (OnTrackChangeListener callback : mCallbacks) {
                callback.OnAdd(pos);
            }
        }

        @Override
        public void OnRemoved(int pos) {
            for (OnTrackChangeListener callback : mCallbacks) {
                callback.OnRemoved(pos);
            }
        }

        @Override
        public void OnChanged(int pos) {
            for (OnTrackChangeListener callback : mCallbacks) {
                callback.OnChanged(pos);
            }
        }

        @Override
        public void OnDelete() {
            for (OnTrackChangeListener callback : mCallbacks) {
                callback.OnDelete();
            }
        }
    };
    private List<OnTrackChangeListener> mCallbacks = new ArrayList<>();

    @Override
    public void OnChange(Audio audio) {
        if(mCallback != null)
            mCallback.OnChanged(audios.indexOf(audio));
    }

    private ListListener<Audio> mAudiosListener;
    private ListListener<String> mEditorsListener;

    public interface OnTrackChangeListener{
        void OnLoad(Track track);
        void OnAdd(int pos);
        void OnRemoved(int pos);
        void OnChanged(int pos);
        void OnDelete();
    }

    //on first creation
    public Track(OnTrackChangeListener callback){
        audioMetaRef = MainActivity.database.getReference("meta").child("tracks").push();
        audios = new ArrayList<>();
        editors = new HashMap<>();
        //manually add just for first value
        //function calls after field sets
        key = audioMetaRef.getKey();
        cloudDir = MainActivity.storageRef.child("audios").child(key);
        addEditor(MainActivity.user);
        addOnTrackChangeListener(callback);
        audioMetaRef.child("editors").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GenericTypeIndicator<Map<String, EditorItem>> t = new GenericTypeIndicator<Map<String, EditorItem>>() {};
                editors = dataSnapshot.getValue(t);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        generateDir();
        mCallback.OnLoad(this);
    }

    //on recreation
    public Track(MetaData meta, OnTrackChangeListener callback){
        this.audios = new ArrayList<>();
        for(String audioName : meta.audios){
            audios.add(new Audio(audioName));
        }
        this.first = 0 < audios.size() ? audios.get(0) : null;
        this.key = meta.key;
        editors = meta.editors;
        this.msBarPeriodMod = meta.msBarPeriodMod;
        cloudDir = MainActivity.storageRef.child("audios").child(key);
        audioMetaRef = MainActivity.database.getReference("meta").child("tracks").child(key);
        //function calls after field sets
        addOnTrackChangeListener(callback);
        generateDir();
        //set tracks last
        for(Audio audio : audios){
            audio.setTrack(this);
        }
        audioMetaRef.child("editors").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GenericTypeIndicator<Map<String, EditorItem>> t = new GenericTypeIndicator<Map<String, EditorItem>>() {};
                editors = dataSnapshot.getValue(t);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        callback.OnLoad(this);
    }

    public void addOnTrackChangeListener(OnTrackChangeListener listener){
        mCallbacks.add(listener);
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
        if(editors.size() <= size())
            addEditor(MainActivity.user);
        if(audios.size() == 0)
            first = audio;
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

    void addEditor(User artist){
        EditorItem item = new EditorItem();
        item.position = editors.size();
        item.uid = artist.uid;
        editors.put(audioMetaRef.child("tracks").push().getKey(), item);
        updateMetadata();
    }

    boolean swapEditorPos(final int fromPos, final int toPos){
        //only move around editors if both positions are under audios size
        if(toPos < audios.size() || fromPos < audios.size())
            return false;

        for (EditorItem editor : editors.values())
            if(editor.position == fromPos)
                editor.position = toPos;
            else if(editor.position == toPos)
                editor.position = fromPos;
        updateMetadata();
        return true;
    }

    EditorItem getEditor(int pos){
        for(Map.Entry<String, EditorItem> editor : editors.entrySet()){
            if(editor.getValue().position == pos)
                return editor.getValue();
        }
        return null;
    }

    int numEditors(){
        return editors.size();
    }

    void removeEditor(int pos){
        String removalKey = null;
        for(Map.Entry<String, EditorItem> editor : editors.entrySet()){
            if(editor.getValue().position == pos){
                removalKey = editor.getKey();
            } else if(pos < editor.getValue().position)
                editor.getValue().position--;
        }
        editors.remove(removalKey);
        updateMetadata();
    }

    public void remove(Audio audio){
        remove(audios.indexOf(audio));
    }

    public boolean contains(Audio audio){
        return audios.contains(audio);
    }

    public void delete(){
        mCallback.OnDelete();

        for(Audio audio : audios){
            audio.delete();
        }
        audios.clear();
        first = null;
        audioMetaRef.removeValue();
        cloudDir.delete();
        if(dir != null)
            Utils.deleteDirectory(dir);
    }

    public int size(){
        return audios.size();
    }

    private void generateDir(){
        dir = new File(MainActivity.context.getFilesDir() + "/audios/" + key);
        if(!dir.isDirectory())
            dir.mkdir();
        cloudDir = MainActivity.storageRef.child("audios").child(key);
    }

    private void updateMetadata(){
        Log.i("Track", "updateMetadata");
        try{
            audioMetaRef.keepSynced(true);
            MetaData meta = new MetaData(this);
            audioMetaRef.setValue(meta);
        } catch(Exception e){
            Log.e("Track", "updateMetadata", e);
        }
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

    @IgnoreExtraProperties
    public static class MetaData implements Serializable{
        public List<String> audios = new ArrayList<>();
        public Map<String, EditorItem> editors = new HashMap<>();
        public Integer msBarPeriodMod;
        public String key;

        public MetaData(){}
        public MetaData(Track track){
            msBarPeriodMod = track.msBarPeriodMod;
            audios = new ArrayList<>();
            for(Audio audio : track.audios){
                audios.add(audio.name);
            }
            key = track.key;
            editors = track.editors;
        }

        @Exclude
        public DatabaseReference getRef(){
            return MainActivity.database.getReference("meta").child("tracks").child(key);
        }

        @Exclude
        public String getFirst(){
            return 0 < audios.size() ? audios.get(0) : "no audios!!";
        }
    }

    public static class EditorItem{
        public int position;
        public String uid;
    }
}
