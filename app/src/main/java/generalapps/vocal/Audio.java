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

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
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
public class Audio implements AudioProcessor{
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
    public GroupTemplate groupTemplate = GroupTemplate.list.get(0);

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
        //TODO make AudioGroup and Audio constructors linked so that Recorder.FREQ*Rhythm.msMaxPeriod()/1000 can be used for buffer
        state = State.UNLOADED;
        waveValues = new ArrayList<>();
    }

    public Audio(MetaData meta){
        this();
        readMetaData(meta);
    }

    public Audio(String name){
        this();
        MainActivity.database.getReference("meta").child("audios").child(name).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                readMetaData(dataSnapshot.getValue(MetaData.class));
                readFile();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Audio", "Loading failed");
            }
        });
    }

    public void setTrack(Track callback){
        group = callback;
        mCallback = callback;
        readFile();
    }

    public void setName(String name){
        this.name = name;
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
            if(effect != Effect.none){
                Toast toast = Toast.makeText(holder.itemView.getContext(), "Effect selected: " + effect.mName, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    public void setGroupTemplate(GroupTemplate groupTemplate){
        if(this.groupTemplate != groupTemplate){
            this.groupTemplate = groupTemplate;
            writeMetaData();
        }
    }

    public void setMsDelay(int msDelay){
        if(Math.abs(msDelay) < group.getMsBarPeriod()*.9){
            this.msDelay = msDelay;
            if(dispatcher != null && msDelay < 0)
                dispatcher.skip((double)-msDelay/1000);
            writeMetaData();
        } else
            Log.d("Audio", "msDelay not set");
    }

    public void readMetaData(MetaData meta){
        name = meta.title;
        msDelay = meta.msDelay;

        barTemplate = BarTemplate.deSerialize(meta);
        groupTemplate = GroupTemplate.deSerialize(meta);
        effect = Effect.deSerialize(meta);

        if(mCallback != null)
            mCallback.OnChange(this);
    }

    public void writeMetaData() {
        //TODO use a map
        if(audioMetaRef == null)
            audioMetaRef = MainActivity.database.getReference("meta").child("audios").child(name);
        audioMetaRef.setValue(new Audio.MetaData(this));
    }

    public void readFile(){
        if(audioFile != null || name == null)
            return;

        audioRef = group.cloudDir.child(name + ".wav");
        audioFile = new File(group.dir, name + ".wav");

        if(!audioFile.exists()) {
            audioRef.getFile(audioFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    readFile();
                    if(holder != null)
                        holder.barTemplateAdapter.updateWaves();
                }
            });
            audioRef = null;
            audioFile = null;
            return;
        }

        final int length = (int)audioFile.length();
        ticks = length/2;
        byte[] bytes = new byte[length];
        short[] shortBuf = new short[length/2];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(audioFile));
            buf.read(bytes, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //upload if doesn't exist or is different size
        //TODO make it just upload at same point as saving to disk, and do all uploading after offline editing in one place
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

        float sum = 0;
        waveValues.clear();
        for(int i = 0; i<ticks; i++){
            short val = ( (short)( ( bytes[i*2] & 0xff )|( bytes[i*2 + 1] << 8 ) ) );
            shortBuf[i] = val;
            //weird stuff to stop overloading values
            sum += Math.abs(val/(float)Short.MAX_VALUE);

            if(i % WaveView.tickInterval == 0){
                waveValues.add(sum/WaveView.tickInterval);
                sum = 0;
            }
        }

        loadDispatcher();
    }

    boolean playAfterLoad;
    public void loadDispatcher(){
        int audioBufferSize = AudioTrack.getMinBufferSize((int)format.getSampleRate(), AudioFormat.CHANNEL_OUT_MONO,  AudioFormat.ENCODING_PCM_16BIT)/(format.getSampleSizeInBits()/8);
        //raise to next pow2
        audioBufferSize = (int)Math.pow(2, Math.ceil(Math.log(audioBufferSize)/Math.log(2)));
        int overlap = audioBufferSize - audioBufferSize/16;
        UniversalAudioInputStream input = null;
        try{
            input = new UniversalAudioInputStream(new BufferedInputStream(new FileInputStream(audioFile)), format);
        } catch(IOException e){
            Log.e("Audio", "Input loading", e);
        }
        dispatcher = new AudioDispatcher(input, audioBufferSize, overlap);
        if(msDelay < 0)
            dispatcher.skip((double)-msDelay/1000);
        AndroidAudioPlayer audioPlayer = null;
        while(audioPlayer == null){
            try{
                audioPlayer = new AndroidAudioPlayer(format, audioBufferSize, AudioManager.STREAM_MUSIC);
            } catch (IllegalStateException e){
                Log.e("Audio", "audioPlayed loading failed", e);
            }
        }
        if(effect.mProcessor != null){
            effect.mProcessor.Apply(dispatcher, audioBufferSize);
        }
        dispatcher.addAudioProcessor(audioPlayer);
        dispatcher.addAudioProcessor(this);

        dispatcherThread = new Thread(dispatcher, name + " player thread");
        dispatcherThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e("Audio", "DispatcherThreadException", throwable);
            }
        });

        state = State.READY;

        if(playAfterLoad){
            playAfterLoad = false;
            play();
        }
    }



    @Override
    public boolean process(AudioEvent audioEvent) {
        if(isPlaying() && group.barTicks()*barTemplate.mRecordingLength < audioEvent.getSamplesProcessed()){
            stop();
            return false;
        }
        return true;
    }

    @Override
    public void processingFinished() {
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
        if(enabled && isLoaded()){
            if(state == State.READY){
                if(0 < msDelay)
                    holder.itemView.postDelayed(delayedStartRunnable, msDelay);
                else
                    dispatcherThread.start();
                state = State.PLAYING;
            } else{
                Log.e("Audio", "Incorrect state: " + state.name(), new Exception());
            }
        }
    }

    Runnable delayedStartRunnable = new Runnable() {
        @Override
        public void run() {
            dispatcherThread.start();
        }
    };

    public void stop(){
        if(dispatcher != null){
            Log.d("Audio", name + " is Stopping");
            state = State.STOPPED;
            dispatcher.stop();
            holder.itemView.removeCallbacks(delayedStartRunnable);
        }
    }

    public void setBarTemplate(BarTemplate barTemplate){
        if(this.barTemplate != barTemplate){
            if(group.size() == 1){
                setTicks(ticks*this.barTemplate.mRecordingLength/barTemplate.mRecordingLength);
                holder.barTemplateAdapter.updateWaves();
            }
            this.barTemplate = barTemplate;
            writeMetaData();
        }
    }

    public void beat(int beat){
        boolean shouldBePlaying = barTemplate.isPlaying(beat) && groupTemplate.isPlaying(beat);
        if(barTemplate.isStartOfBar(beat) && shouldBePlaying){
            if(isPlaying()) {
                playAfterLoad = true;
                stop();
            }
            else
                play();
            holder.groupTemplateAdapter.updateAllGroupTemplates();
        } else if(isPlaying() && !shouldBePlaying){
            stop();
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

    public void delete(){
        try{
            stop();
        } catch (IllegalStateException e){
            Log.w("IllegalState", "Audio " + name + " is in an illegal state. Continuing with delete.");

        }
        waveValues.clear();
        if(audioFile != null)
            audioFile.delete();
        if(audioRef != null)
            audioRef.delete();
        if(audioMetaRef != null)
            audioMetaRef.removeValue();
    }

    public interface OnAudioLoaded{
        void OnAudioLoaded(Audio audio);
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
        public Integer groupTemplateIndex;
        public Integer effectIndex;
        public Integer effectCategoryIndex;

        public MetaData(){}

        public MetaData(Audio audio){
            title = audio.name;
            msDelay = audio.msDelay;
            audio.barTemplate.serialize(this);
            audio.groupTemplate.serialize(this);
            audio.effect.serialize(this);
        }
    }
}