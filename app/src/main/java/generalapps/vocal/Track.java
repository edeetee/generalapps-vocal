package generalapps.vocal;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by edeetee on 11/05/2016.
 */
public class Track implements Iterable<Audio>, Audio.OnAudioChangeListener {
    private Map<String, Integer> positions;
    //private List<Audio> audios;
    private Map<String, Audio> audios;
    private Map<String, EditorItem> editors;

    enum SpecialEditor {
        RANDOM('R', "? Random ?"), PREVIOUS('P', null), JOIN('J', "Join & Share");

        final private char value;
        final String printable;
        SpecialEditor(char mValue, String mPrintable){
            value = mValue;
            printable = mPrintable;
        }

        String encodeWithColor(){
            return value+Integer.toString(ColorUtils.HSLToColor(new float[]{(float)Math.random()*360, (float)(0.4+Math.random()*0.3), (float)(0.3+Math.random()*0.4)}));
        }

        boolean canPrint(){
            return printable != null;
        }

        interface PrintCallback{
            void printLoaded(String print);
        }

        static void loadPrint(String uid, Track track, final PrintCallback callback){
            Track.SpecialEditor trySpecial = Track.SpecialEditor.parse(uid);
            if(trySpecial == null)
                return;

            if(trySpecial == Track.SpecialEditor.PREVIOUS){
                int firstEditorPos = track.getFirstEditorPos(uid);
                if(firstEditorPos != 0)
                    if(!track.getShuffled())
                        MainActivity.database.getReference("users").child(track.getEditor(firstEditorPos-1)).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                callback.printLoaded(dataSnapshot.getValue(User.class).name + "'s Choice");
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    else
                        callback.printLoaded("?" + "'s Choice");
                else
                    callback.printLoaded("Your Choice");
            }else
                callback.printLoaded(trySpecial.printable);
        }

        static SpecialEditor parse(char value){
            for (SpecialEditor specialEditor : values()) {
                if(specialEditor.value == value)
                    return specialEditor;
            }
            return null;
        }

        static SpecialEditor parse(String value){
            return parse(value.charAt(0));
        }

        static int parseColor(String value){
            return Integer.parseInt(value.substring(1));
        }
    }

    Audio first;
    DatabaseReference audioMetaRef;
    private String owner;
    private String title;
    private int msBarPeriodMod;
    private @NonNull String key;
    int currentEditorIndex;
    private boolean shuffled;
    private boolean isSetup;
    private boolean freeMode;
    private boolean finished;
    private static String TAG = "Track";

    //region databaseCallbacks
    private OnTrackChangeListener mCallback = new OnTrackChangeListener() {
        @Override
        public void OnLoad(Track track) {
            for (OnTrackChangeListener callback : mCallbacks) {
                callback.OnLoad(track);
            }
        }

        @Override
        public void OnDataChange(Track track) {
            for (OnTrackChangeListener callback : mCallbacks) {
                callback.OnDataChange(track);
            }
        }

        @Override
        public void OnAudioAdd(int pos) {
            for (OnTrackChangeListener callback : mCallbacks) {
                callback.OnAudioAdd(pos);
            }
        }

        @Override
        public void OnAudioRemoved(int pos) {
            for (OnTrackChangeListener callback : mCallbacks) {
                callback.OnAudioRemoved(pos);
            }
        }

        @Override
        public void OnAudioChanged(int pos) {
            for (OnTrackChangeListener callback : mCallbacks) {
                callback.OnAudioChanged(pos);
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

    private ChildEventListener audiosChildListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            Log.i(TAG, dataSnapshot.getKey() + " child added");
            //check if already in list
            if(!positions.containsKey(dataSnapshot.getKey()))
                add(new Audio(dataSnapshot.getKey()), false);
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            //happens inside audio
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            String removedName = dataSnapshot.getKey();
            Log.i(TAG, removedName + " child removed");
            if(positions.containsKey(removedName))
                remove(removedName, false);
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    private ValueEventListener editorsValueListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            GenericTypeIndicator<Map<String, EditorItem>> t = new GenericTypeIndicator<Map<String, EditorItem>>() {
            };
            if (dataSnapshot.exists())
                editors = dataSnapshot.getValue(t);
            else
                editors.clear();
            doDataChange();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    private ValueEventListener metaRefListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot.exists()) {
                MetaData meta = dataSnapshot.getValue(MetaData.class);
                currentEditorIndex = meta.currentEditIndex;
                msBarPeriodMod = meta.msBarPeriodMod;

                doDataChange();
            } else
                delete();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };
    //endregion

    @Override
    public void OnChange(Audio audio) {
        mCallback.OnAudioChanged(positions.get(audio.name));
    }

    private ListListener<Audio> mAudiosListener;
    private ListListener<String> mEditorsListener;

    interface OnTrackChangeListener{
        void OnDataChange(Track track);
        void OnLoad(Track track);
        void OnAudioAdd(int pos);
        void OnAudioRemoved(int pos);
        void OnAudioChanged(int pos);
        void OnDelete();
    }

    //on first creation
    public Track(){
        audioMetaRef = MainActivity.database.getReference("meta").child("tracks").push();
        audios = new HashMap<>();
        positions = new HashMap<>();
        editors = new HashMap<>();
        //manually add just for first value
        //function calls after field sets
        title = new SimpleDateFormat("HH_mm_ss").format(Calendar.getInstance().getTime());
        key = audioMetaRef.getKey();
        owner = MainActivity.user.uid;
        currentEditorIndex = -1;
        msBarPeriodMod = 0;

        updateMetadata();
        audioMetaRef.child("editors").addValueEventListener(editorsValueListener);
        audioMetaRef.child("audios").addChildEventListener(audiosChildListener);
        audioMetaRef.addValueEventListener(metaRefListener);
    }

    //on recreation
    private Track(MetaData meta){
        audios = new HashMap<>();
        positions = meta.audios;
        for (String s : positions.keySet()) {
            audios.put(s, new Audio(s));
        }
        first = 0 < audios.size() ? audios.get(getKey(0)) : null;
        shuffled = meta.shuffled;
        freeMode = meta.freeMode;
        isSetup = meta.isSetup;
        finished = meta.finished;
        key = meta.key;
        title = meta.title;
        owner = meta.owner;
        editors = meta.editors;
        currentEditorIndex = meta.currentEditIndex;
        msBarPeriodMod = meta.msBarPeriodMod;
        audioMetaRef = MainActivity.database.getReference("meta").child("tracks").child(key);
        //function calls after field sets
        //set tracks last
        for (Audio audio : audios.values()) {
            audio.setTrack(this);
        }
        audioMetaRef.child("editors").addValueEventListener(editorsValueListener);
        audioMetaRef.child("audios").addChildEventListener(audiosChildListener);
        audioMetaRef.addValueEventListener(metaRefListener);
    }

    public boolean isOwner() {
        return MainActivity.user.uid.equals(owner);
    }

    public String getTitle(){
        return title;
    }

    public void setTitle(String title){
        if(!title.equals(this.title)){
            this.title = title;
            updateMetadata();
        }
    }

    public void addOnTrackChangeListener(OnTrackChangeListener listener){
        if(listener != null){
            mCallbacks.add(listener);
            listener.OnLoad(this);
            for (int i = 0; i < audios.size(); i++) {
                listener.OnAudioAdd(i);
            }
            listener.OnDataChange(this);
        }
    }

    @Override
    public Iterator iterator() {
        return audios.values().iterator();
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

    boolean getFreeMode(){
        return freeMode;
    }

    void setFreeMode(boolean enabled){
        if(freeMode != enabled){
            freeMode = enabled;
            updateMetadata();
        }
    }

    void setShuffled(boolean shuffled){
        if(shuffled != this.shuffled){
            this.shuffled = shuffled;
            updateMetadata();
        }
    }

    boolean getShuffled(){
        return shuffled;
    }

    void finalizeSetup(){
        if(isSetup && !isFinished())
            Log.e(TAG, "finalizeSetup: in editing phase");
        else if(isNextEditorSpecial())
            prepareNextSpecialEditor(new Runnable() {
                @Override
                public void run() {
                    finalizeSetup();
                }
            });
        else{
            isSetup = true;
            if(shuffled){
                //shuffle the editors which do not already have recordings
                List<Integer> options = new ArrayList<>();
                for(int i = size(); i < editors.size(); i++)
                    options.add(i);
                for (EditorItem editorItem : editors.values()) {
                    if(size() <= editorItem.position){
                        int pos = Utils.randomInt(size(), size()+options.size());
                        editorItem.position = options.get(pos);
                        options.remove(pos);
                    }
                }
            }
            currentEditorIndex = size();
            finished = false;
            String nextEditor = getEditor(currentEditorIndex);
            if(nextEditor != null)
                VocalNotifications.usersTurnToRecord(key, nextEditor);
            else{
                VocalNotifications.notifyFinished(key, owner);
                finished = true;
            }
            updateMetadata();
        }
    }

    boolean isSetup(){
        return isSetup;
    }

    private void add(Audio audio, boolean upload){
        if(isSetup && !freeMode && enoughAudiosForPublish()){
            Log.w(TAG, "add: tried to add while enough audios already added");
            return;
        }

        if(upload){
            if(editors.size() <= size())
                addEditor(MainActivity.user.uid);
            else if(!getEditor(size()).equals(MainActivity.user.uid))
                insertEditor(size(), MainActivity.user.uid);
        }

        if(positions.size() == 0)
            first = audio;
        audio.setTrack(this);
        positions.put(audio.name, positions.size());
        audios.put(audio.name, audio);

        mCallback.OnAudioAdd(positions.size()-1);
        updateMetadata(upload);
    }

    public void add(Audio audio){
        add(audio, true);
    }

    public Audio get(int i){
        return audios.get(getKey(i));
    }

    private String getKey(int i){
        for (Map.Entry<String, Integer> stringIntegerEntry : positions.entrySet()) {
            if(stringIntegerEntry.getValue() == i)
                return stringIntegerEntry.getKey();
        }
        return null;
    }

    private void remove(String key, boolean upload){
        int pos = positions.get(key);
        //only fail if is Setup, is inside the editable items for the current user, and it is the last item
        if(!isEditable(pos))
            return;

        audios.get(key).delete();
        audios.remove(key);
        positions.remove(key);

        for (Map.Entry<String, Integer> stringIntegerEntry : positions.entrySet()) {
            if(pos < stringIntegerEntry.getValue())
                positions.put(stringIntegerEntry.getKey(), stringIntegerEntry.getValue()-1);
        }

        if(!isSetup)
            removeEditor(pos, upload);
        if(pos == 0)
            if(audios.size() == 0)
                first = null;
            else
                first = audios.get(getKey(0));

        mCallback.OnAudioRemoved(pos);
        updateMetadata(upload);
    }

    public void remove(String key){
        remove(key, true);
    }

    void addEditor(String uid){
        if(freeMode){
            for (EditorItem editorItem : editors.values()) {
                if(editorItem.uid.equals(uid))
                    return;
            }
        }
        EditorItem item = new EditorItem();
        item.position = editors.size();
        item.uid = uid;
        editors.put(audioMetaRef.child("tracks").push().getKey(), item);
        updateMetadata();
    }

    void insertEditor(int index, String uid){
        for (EditorItem editorItem : editors.values()) {
            if(index <= editorItem.position)
                editorItem.position++;
        }
        EditorItem item = new EditorItem();
        item.position = index;
        item.uid = uid;
        editors.put(audioMetaRef.child("tracks").push().getKey(), item);
        updateMetadata();
    }

    private void setEditor(int index, String uid){
        for (EditorItem editorItem : editors.values()) {
            if(editorItem.position == index)
                editorItem.uid = uid;
        }
    }

    boolean enoughAudiosForPublish(){
        //enough audios to publish if it has gone past the current Editor index, and the next editor will be new.
        return currentEditorIndex+1 <= size() && !getEditor(size()-1).equals(getEditor(size()));
    }

    boolean canPublish(){
        if(freeMode)
            return false;
        return canArtistRecord(MainActivity.user.uid) && enoughAudiosForPublish();
    }

    private boolean isNextEditorSpecial(){
        if(numEditors() <= size())
            return false;

        //check values if next recording will change the editor or if still setting up
        String uid = getEditor(size());
        return SpecialEditor.parse(uid) != null;
    }

    private boolean isPreparing = false;
    private void prepareNextSpecialEditor(final Runnable preparedCallback){
        if(isPreparing){
            Log.w(TAG, "prepareNextSpecialEditor: chill bro, too much preparation");
            return;
        }
        switch (SpecialEditor.parse(getEditor(size()))){
            case RANDOM:
                //preloading
                isPreparing = true;
                MainActivity.database.getReference("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        GenericTypeIndicator<List<User>> usersType = new GenericTypeIndicator<List<User>>(){};
                        List<User> users = dataSnapshot.getValue(usersType);
                        setEditor(size(), users.get(Utils.randomInt(users.size())).uid);
                        isPreparing = false;
                        preparedCallback.run();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                break;
            case PREVIOUS:
                isPreparing = true;
                ArtistSearcher searcher = new ArtistSearcher(MainActivity.context);
                final AlertDialog artistPicker = new AlertDialog.Builder(MainActivity.context, R.style.ThemedAlertDialog)
                        .setView(searcher)
                        .setNegativeButton("CANCEL", null)
                        .create();
                searcher.setOnUserSelectedListener(new ArtistSearcher.OnUserSelectedListener() {
                    @Override
                    public void OnUserSelected(User user) {
                        artistPicker.hide();
                        setEditor(size(), user.uid);
                        isPreparing = false;
                        preparedCallback.run();
                    }
                });
                artistPicker.show();
                break;
            case JOIN:
                Log.e(TAG, "prepareNextSpecialEditor: this shouldnt happen");
                break;
        }
    }



    void publish(){
        if(enoughAudiosForPublish()){
            if(isNextEditorSpecial()){
                prepareNextSpecialEditor(new Runnable() {
                    @Override
                    public void run() {
                        publish();
                    }
                });
                return;
            }
            currentEditorIndex = size();
            if(numEditors() <= currentEditorIndex){
                for (EditorItem editorItem : editors.values()) {
                    VocalNotifications.notifyFinished(key, editorItem.uid);
                }
                finished = true;
            } else
                VocalNotifications.usersTurnToRecord(key, getEditor(currentEditorIndex));
            updateMetadata();
        } else{
            Log.e(TAG, "Publish: not ready to publish");
        }
    }

    boolean isFinished(){
        return finished;
    }

    boolean canRecord(Context context){
        if(!isSetup || freeMode)
            return true;
        else if(isFinished()) {
            Toast editorToast = Toast.makeText(context, "You cannot record because this track has finished recording", Toast.LENGTH_LONG);
            editorToast.show();
        }else if(!canArtistRecord(MainActivity.user.uid)) {
            Toast editorToast = Toast.makeText(context, "You cannot record because it is not your turn", Toast.LENGTH_LONG);
            editorToast.show();
        } else if(enoughAudiosForPublish()){
            Toast editorToast = Toast.makeText(context, "You must publish your track before the next person has their turn", Toast.LENGTH_LONG);
            editorToast.show();
        }else
            return true;
        return false;
    }

    boolean canArtistRecord(String artistUID){
        //cannot record if has finished
        if(numEditors() <= currentEditorIndex)
            return false;
        return getEditor(currentEditorIndex).equals(artistUID);
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

    String getEditor(int pos){
        for (EditorItem editorItem : editors.values()) {
            if(editorItem.position == pos)
                return  editorItem.uid;
        }
        Log.w(TAG, "getEditor: pos " + pos + " does not exist");
        return null;
    }

    int getFirstEditorPos(String uid){
        int lowest = editors.size();
        for (EditorItem editorItem : editors.values()) {
            if(editorItem.uid.equals(uid) && editorItem.position < lowest)
                lowest = editorItem.position;
        }
        return lowest;
    }

    int numEditors(){
        return editors.size();
    }

    void removeEditor(int pos){
        removeEditor(pos, true);
    }

    void removeEditor(int pos, boolean upload){
        String removalKey = null;
        for(Map.Entry<String, EditorItem> editor : editors.entrySet()){
            if(editor.getValue().position == pos){
                removalKey = editor.getKey();
            } else if(pos < editor.getValue().position)
                editor.getValue().position--;
        }
        editors.remove(removalKey);
        updateMetadata(upload);
    }

    boolean isEditable(int pos){
        if(finished && !isOwner())
            return false;
        return freeMode || !isSetup || (getEditor(pos).equals(MainActivity.user.uid) && currentEditorIndex <= pos);
    }

    public boolean contains(Audio audio){
        return audios.containsValue(audio);
    }

    void cleanup(){
        audioMetaRef.child("editors").removeEventListener(editorsValueListener);
        audioMetaRef.child("audios").removeEventListener(audiosChildListener);
        audioMetaRef.removeEventListener(metaRefListener);
        for (Audio audio : audios.values()) {
            audio.cleanup();
        }
    }

    public void delete(){
        cleanup();
        mCallback.OnDelete();

        for (Audio audio : audios.values()) {
            audio.delete();
        }
        audios.clear();
        first = null;
        audioMetaRef.removeValue();
    }

    public int size(){
        return audios.size();
    }

    private void updateMetadata(){
        updateMetadata(true);
    }
    private void updateMetadata(boolean upload){
        Log.i("Track", "updateMetadata");
        try{
            if(upload){
                audioMetaRef.keepSynced(true);
                MetaData meta = new MetaData(this);
                audioMetaRef.setValue(meta);
            }
            doDataChange();
        } catch(Exception e){
            Log.e("Track", "updateMetadata", e);
        }
    }

    private void doDataChange(){
        mCallback.OnDataChange(this);
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
        public Map<String, Integer> audios = new HashMap<>();
        public Map<String, EditorItem> editors = new HashMap<>();
        public Integer msBarPeriodMod = 0;
        public String key;
        public Integer currentEditIndex = 0;
        public Boolean shuffled = false;
        public Boolean freeMode = false;
        public String owner;
        public String title = "unnamed";
        public boolean isSetup;
        public boolean finished;

        public MetaData(){

        }

        public MetaData(Track track){
            finished = track.finished;
            msBarPeriodMod = track.msBarPeriodMod;
            audios = new HashMap<>();
            audios = track.positions;
            freeMode = track.freeMode;
            owner = track.owner;
            key = track.key;
            editors = track.editors;
            currentEditIndex = track.currentEditorIndex;
            shuffled = track.shuffled;
            isSetup = track.isSetup;
            title = track.title;
        }

        @Exclude
        public DatabaseReference getRef(){
            return MainActivity.database.getReference("meta").child("tracks").child(key);
        }

        @Exclude
        public Track getTrack(){
            return new Track(this);
        }

        interface EditorCallback {
            void editorNameLoaded(User editor);
        }
        @Exclude void getCurrentEditor(final EditorCallback callback){
            for (EditorItem editorItem : editors.values()) {
                if (editorItem.position == currentEditIndex)
                    MainActivity.database.getReference("users").child(editorItem.uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            callback.editorNameLoaded(dataSnapshot.getValue(User.class));
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
            }
        }

        @Exclude boolean canOpen(String uid){
            try{
                if(owner.equals(uid))
                    return true;
                if(isSetup)
                    for (EditorItem editorItem : editors.values()) {
                        if(editorItem.uid.equals(uid))
                            return true;
                    }
            } catch (NullPointerException e){
                Log.i(TAG, "Meta:canOpen: ", e);
            }
            return false;
        }
    }

    public static class EditorItem implements Serializable{
        public int position;
        public String uid;
    }
}
