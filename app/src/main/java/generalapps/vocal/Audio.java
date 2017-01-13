package generalapps.vocal;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.FadeIn;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import generalapps.vocal.effects.Effect;
import generalapps.vocal.templates.BarTemplate;
import generalapps.vocal.templates.GroupTemplate;

/**
 * Created by edeetee on 13/04/2016.
 */
public class Audio implements AudioProcessor, ValueEventListener{
    private static final String TAG = "Audio";

    Lock dispatcherLock;
    AudioDispatcher dispatcher;
    Thread dispatcherThread;
    TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(Recorder.FREQ, 16, 1, true, ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN));

    String name;
    File audioFile;
    StorageReference audioRef;
    //File metaData;
    DatabaseReference audioMetaRef;
    int ticks;
    int msDelay;

    State state;

    public Effect effect = Effect.none;
    public BarTemplate barTemplate = BarTemplate.defaultFromBars(1);

    public interface AudioEffectApplier{
        void Apply(AudioDispatcher dispatcher, int bufferSize);
    }

    public interface OnAudioChangeListener {
        void OnChange(Audio audio);
    }
    OnAudioChangeListener mCallback;

    List<Float> waveValues;

    public RecorderAdapter.AudioHolder holder;
    Track group;
    //is audio enabled in ui
    public boolean enabled = true;

    public Audio(){
        state = State.UNLOADED;
        dispatcherLock = new ReentrantLock();
        waveValues = new ArrayList<>();
    }

    public Audio(String name){
        this();
        setName(name);
        audioMetaRef.addValueEventListener(this);
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        MetaData meta = dataSnapshot.getValue(MetaData.class);
        if(meta != null)
            readMetaData(meta);
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        Log.e(TAG, "Loading failed");
    }

    public void setTrack(Track callback){
        group = callback;
        mCallback = callback;
    }

    public void setName(String name){
        this.name = name;
        audioMetaRef = MainActivity.database.getReference("meta").child("audios").child(name);
    }

    public void setEnabled(boolean enabled){
        if(this.enabled && !enabled)
            stop();

        this.enabled = enabled;
        //update waves
        holder.barTemplateAdapter.updateWaves();
    }

    public void setEffect(Effect effect){
        if(this.effect != effect){
            this.effect = effect;
            loadDispatcher();
            writeMetaData();
            Toast toast = Toast.makeText(holder.itemView.getContext(), "Effect selected: " + effect.mName, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public double maxMsDelayAbs(){
        return group.getMsBarPeriod()*.9*barTemplate.mRecordingLength;
    }

    public void setMsDelay(int msDelay){
        if(Math.abs(msDelay) < maxMsDelayAbs()){
            this.msDelay = msDelay;
            dispatcherLock.lock();
            try{
                if(dispatcher != null && msDelay < 0)
                    dispatcher.skip((double)-msDelay/1000);
            } finally{
                dispatcherLock.unlock();
            }
            writeMetaData();
        } else
            Log.d(TAG, "msDelay not set");
    }

    public void readMetaData(MetaData meta){
        if(!meta.isEqual(this)){
            name = meta.title;
            msDelay = meta.msDelay;

            barTemplate = BarTemplate.deSerialize(meta);
            effect = Effect.deSerialize(meta);

            if(mCallback != null)
                mCallback.OnChange(this);
        }

        //try read file, will skip if already read
        readFile();
    }

    public void writeMetaData() {
        audioMetaRef.setValue(new Audio.MetaData(this));
    }

    public File getAudioFile(){
        return new File(MainActivity.context.getFilesDir(), "audios/" + name + ".wav");
    }

    public StorageReference getAudioRef(){
        return MainActivity.storageRef.child("audios").child(name + ".wav");
    }

    void readFile(){
        readFile(false);
    }

    private void readFile(boolean recursiveCall){
        if(audioFile != null || name == null)
            return;

        audioRef = getAudioRef();
        audioFile = getAudioFile();

        if(!audioFile.exists()) {
            audioRef.getFile(audioFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    readFile();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "fileLoadFailed, retrying");
                    readFile(true);
                }
            });
            Log.i(TAG, "readFile: audiofile nonexistant exit, audioFile: " + audioFile.toString() + ", name: " + name);
            audioRef = null;
            audioFile = null;
            return;
        } else if(recursiveCall){
            Log.i(TAG, "readFile: recursive call exit");
            audioRef = null;
            audioFile = null;
            return;
        }

        Log.i(TAG, "readFile: file exists and loading");
        final int length = (int)audioFile.length();
        ticks = length/2;
        byte[] bytes = new byte[length];
        //short[] shortBuf = new short[length/2];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(audioFile));
            buf.read(bytes, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //upload if doesn't exist or is different size
        audioRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                if(storageMetadata.getSizeBytes() != length)
                    audioRef.putFile(Uri.fromFile(audioFile));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                audioRef.putFile(Uri.fromFile(audioFile));
            }
        });

        if(bytes.length/2 != ticks)
            Log.e(TAG, "readFile: bytes: "+bytes.length+", ticks: "+ticks);

        long sum = 0;
        waveValues.clear();
        int averages = 10;
        int skips = WaveView.tickInterval/averages;
        for(int i = 0; i<ticks; i += skips){
            short val = ( (short)( ( bytes[i*2] & 0xff )|( bytes[i*2 + 1] << 8 ) ) );
            //shortBuf[i] = val;
            //weird stuff to stop overloading values
            sum += Math.abs(val);

            if((i+skips) % (skips*averages) == 0){
                waveValues.add((float)sum/averages/Short.MAX_VALUE);
                sum = 0;
            }
        }
        if(holder != null)
            holder.barTemplateAdapter.updateWaves();

        loadDispatcher();
    }

    boolean playAfterLoad;
    public void loadDispatcher(){
        dispatcherLock.lock();
        try{
            int audioBufferSize = AudioTrack.getMinBufferSize((int)format.getSampleRate(), AudioFormat.CHANNEL_OUT_MONO,  AudioFormat.ENCODING_PCM_16BIT)/(format.getSampleSizeInBits()/8);
            //raise to next pow2
            audioBufferSize = (int)Math.pow(2, Math.ceil(Math.log(audioBufferSize)/Math.log(2)));
            int overlap = audioBufferSize - audioBufferSize/16;
            UniversalAudioInputStream input = null;
            try{
                input = new UniversalAudioInputStream(new BufferedInputStream(new FileInputStream(audioFile)), format);
            } catch(IOException e){
                Log.e(TAG, "Input loading", e);
            }
            AndroidAudioPlayer audioPlayer = null;
            while(audioPlayer == null){
                try{
                    audioPlayer = new AndroidAudioPlayer(format, audioBufferSize, AudioManager.STREAM_MUSIC);
                } catch (IllegalStateException e){
                    Log.e(TAG, "audioPlayed loading failed", e);
                }
            }

            dispatcher = new AudioDispatcher(input, audioBufferSize, overlap);
            if(msDelay < 0)
                dispatcher.skip((double)-msDelay/1000);

            if(effect.mProcessor != null){
                effect.mProcessor.Apply(dispatcher, audioBufferSize);
            }

            dispatcher.addAudioProcessor(new CleanStart());
            dispatcher.addAudioProcessor(audioPlayer);
            dispatcher.addAudioProcessor(this);

            dispatcherThread = new Thread(dispatcher, name + " player thread");
            dispatcherThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    Log.e(TAG, "DispatcherThreadException", throwable);
                }
            });

            state = State.READY;

            if(playAfterLoad){
                playAfterLoad = false;
                play();
            }
        } finally{
            dispatcherLock.unlock();
        }
    }



    @Override
    public boolean process(AudioEvent audioEvent) {
//        if(isPlaying() && group.barTicks()*barTemplate.mRecordingLength < audioEvent.getSamplesProcessed()){
//            stop();
//            return false;
//        }
        return true;
    }

    @Override
    public void processingFinished() {
        if(!deleted)
            loadDispatcher();
    }

    @Override
    public String toString(){
        return name;
    }

    public boolean isPlaying(){
        return state == State.PLAYING;
    }

    public boolean isLoaded(){
        return state != State.UNLOADED;
    }

    public void play() throws IllegalStateException {
        dispatcherLock.lock();
        try{
            if(enabled && isLoaded()){
                if(state == State.READY){
                    if(0 < msDelay)
                        holder.itemView.postDelayed(delayedStartRunnable, msDelay);
                    else{
                        dispatcherThread.start();
                    }
                    state = State.PLAYING;
                } else{
                    Log.e(TAG, "Incorrect state: " + state.name(), new Exception());
                }
            }
        } finally{
            dispatcherLock.unlock();
        }
    }

    Runnable delayedStartRunnable = new Runnable() {
        @Override
        public void run() {
            dispatcherLock.lock();
            try{
                dispatcherThread.start();
            } finally{
                dispatcherLock.unlock();
            }
        }
    };

    public void stop(){
        dispatcherLock.lock();
        try{
            if(dispatcher != null){
                //Log.d(TAG, name + " is Stopping");
                state = State.STOPPED;
                if(holder != null)
                    holder.itemView.removeCallbacks(delayedStartRunnable);
                try{
                    dispatcher.stop();
                } catch (Exception e){
                    Log.e(TAG, "stop failed", e);
                    if(!deleted)
                        loadDispatcher();
                }
            }
        } finally{
            dispatcherLock.unlock();
        }
    }

    public void setBarTemplate(BarTemplate barTemplate){
        if(this.barTemplate != barTemplate){
            if(group.size() == 1 && this.barTemplate.mRecordingLength != barTemplate.mRecordingLength){
                setTicks(ticks*this.barTemplate.mRecordingLength/barTemplate.mRecordingLength);
                holder.barTemplateAdapter.updateWaves();
            }
            this.barTemplate = barTemplate;
            if(maxMsDelayAbs() < Math.abs(msDelay)){
                msDelay = (int)Math.round(Math.min(maxMsDelayAbs(), Math.abs(msDelay)) * Math.signum(msDelay));
                holder.barTemplateAdapter.updateWaves();
            }
            writeMetaData();
        }
    }

    public void beat(int beat){
        dispatcherLock.lock();
        try{
            boolean shouldBePlaying = barTemplate.isPlaying(beat);
            if(barTemplate.isStartOfBar(beat) && shouldBePlaying){
                if(isPlaying()) {
                    playAfterLoad = true;
                    stop();
                }
                else
                    play();
            } else if(isPlaying() && !shouldBePlaying){
                stop();
            }
        } finally{
            dispatcherLock.unlock();
        }
    }

    public void setTicks(int ticks){
        this.ticks = ticks;
        if(1 < group.size()){
            int bars = (int)(1+Rhythm.ticksToMs(ticks) / (group.getMsBarPeriod()*1.1));
            BarTemplate defaultFromBars = BarTemplate.defaultFromBars(bars);
            if(barTemplate != defaultFromBars){
                barTemplate = defaultFromBars;
                if(holder != null)
                    holder.barTemplate.post(new Runnable() {
                        @Override
                        public void run() {
                            holder.barTemplate.setCurrentItem(BarTemplate.list.indexOf(barTemplate), false);
                        }
                    });
            }
        }
    }

    void cleanup(){
        audioMetaRef.removeEventListener(this);
    }

    boolean deleted = false;
    void delete(){
        deleted = true;
        try{
            stop();
        } catch (IllegalStateException e){
            Log.w("IllegalState", "Audio " + name + " is in an illegal state. Continuing with delete.");

        }
        cleanup();
        waveValues.clear();
        getAudioFile().delete();
        getAudioRef().delete();
        audioMetaRef.removeValue();
    }

    public enum State{
        UNLOADED,
        READY,
        STOPPED,
        PLAYING
    }

    public static class MetaData {
        public String title;
        public Integer msDelay;
        public Integer barTemplateIndex;
        public Integer effectIndex;
        public Integer effectCategoryIndex;

        public MetaData(){}

        public MetaData(Audio audio){
            title = audio.name;
            msDelay = audio.msDelay;
            audio.barTemplate.serialize(this);
            audio.effect.serialize(this);
        }

        @Exclude
        boolean isEqual(Audio compare){
            return compare.name.equals(title) && compare.msDelay == msDelay && compare.barTemplate.getIndex() == barTemplateIndex &&
                    effectIndex == compare.effect.getEffectIndex() && effectCategoryIndex == compare.effect.getCategoryIndex();
        }
    }
}