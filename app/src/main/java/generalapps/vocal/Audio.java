package generalapps.vocal;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import generalapps.vocal.effects.Effect;
import generalapps.vocal.effects.EffectCategory;
import generalapps.vocal.templates.BarTemplate;

/**
 * Created by edeetee on 13/04/2016.
 */
public class Audio implements
        AudioProcessor{
    AudioDispatcher dispatcher;
    Thread dispatcherThread;
    TarsosDSPAudioInputStream input;
    AndroidAudioPlayer audioPlayer;
    TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(Recorder.FREQ, 16, 1, true, ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN));

    String name;
    File audioFile;
    File metaData;
    int ticks;
    int maxBeats;

    State state;

    public Effect effect = Effect.none;
    public BarTemplate barTemplate = new BarTemplate(1, true, true, true, true);

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
        waveValues = new ArrayList<Float>();
    }

    public Audio(File metaData){
        this();
        setMetaData(metaData);
    }

    public Audio(File audio, File metaData){
        this(metaData);
        writeFile(audio);
    }

    public void setTrack(Track callback){
        group = callback;
        mCallback = callback;
        //waveValues.setOnMaxWaveValueChangedListener(callback);
    }

    public void setName(String name){
        this.name = name;
    }

    public void setEnabled(boolean enabled){
        if(this.enabled && !enabled)
            stop();

        this.enabled = enabled;
        holder.barTemplateAdapter.notifyDataSetChanged();
    }

    public void setMetaData(File metaData){
        this.metaData = metaData;
        readMetaData();
    }

    public void setEffect(Effect effect){
        this.effect = effect;
        loadDispatcher();
        writeMetaData();
        if(effect != Effect.none){
            Toast toast = Toast.makeText(holder.itemView.getContext(), "Effect selected " + holder.itemView.getContext().getResources().getResourceName(effect.mIcon), Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void readMetaData(){
        JSONObject metaObj = Utils.loadJSON(metaData, "Audio load: ");
        try{
            name = metaObj.getString("Title");
            barTemplate = BarTemplate.deSerialize(metaObj.getJSONObject("BarTemplate"));
            effect = Effect.deSerialize(metaObj.getJSONObject("Effect"));
        } catch(JSONException e){
            Log.e("Audio", "readMetaData", e);
        }

        if(mCallback != null)
            mCallback.OnChange(this);
    }

    public File writeMetaData() {
        try{
            File metaData = new File(group.dir, name + ".json");
            JSONObject metaObj = new JSONObject();

            metaObj.put("Title", name);
            metaObj.put("Effect", effect.serialize());
            metaObj.put("BarTemplate", barTemplate.serialize());

            String output = metaObj.toString();

            FileWriter writer = new FileWriter(metaData);
            writer.write(output);
            writer.flush();
            writer.close();

            Utils.printFile("Audio write", metaData);

            return metaData;
        } catch(Exception e){
            Log.e("Track", "updateMetadata", e);
            return null;
        }
    }

    public void writeFile(File audioFile){
        this.audioFile = audioFile;

        int length = (int)audioFile.length();
        ticks = length/2;
        byte[] bytes = new byte[length];
        short[] shortBuf = new short[length/2];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(audioFile));
            buf.read(bytes, 0, length);
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

            if(i % WaveView.tickInterval == 0){
                waveValues.add(sum/WaveView.tickInterval);
                sum = 0;
            }
        }

        loadDispatcher();

        state = State.LOADED;
    }

    public void loadDispatcher(){
        int audioBufferSize = AudioTrack.getMinBufferSize((int)format.getSampleRate(), AudioFormat.CHANNEL_OUT_MONO,  AudioFormat.ENCODING_PCM_16BIT)/(format.getSampleSizeInBits()/8);
        //raise to next pow2
        audioBufferSize = (int)Math.pow(2, Math.ceil(Math.log(audioBufferSize)/Math.log(2)));
        int overlap = audioBufferSize - audioBufferSize/16;
        try{
            input = new UniversalAudioInputStream(new BufferedInputStream(new FileInputStream(audioFile)), format);
        } catch(IOException e){
            Log.e("Audio", "Input loading", e);
        }
        dispatcher = new AudioDispatcher(input, audioBufferSize, overlap);

        audioPlayer = new AndroidAudioPlayer(format, audioBufferSize, AudioManager.STREAM_MUSIC);
        if(effect.mProcessor != null){
            effect.mProcessor.Apply(dispatcher, audioBufferSize);
        }
        dispatcher.addAudioProcessor(this);
        dispatcher.addAudioProcessor(audioPlayer);

        dispatcherThread = new Thread(dispatcher, name + " player thread");
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        if(group.barTicks()*barTemplate.mRecordingLength < audioEvent.getSamplesProcessed()){
            stop();
            return false;
        }
        return true;
    }

    @Override
    public void processingFinished() {
        loadDispatcher();
        state = State.LOADED;
    }

    @Override
    public String toString(){
        return name;
    }

    public boolean isPlaying(){
        return state == State.PLAYING;
    }

    public boolean canPlay(){
        return enabled && audioFile != null && metaData != null;
    }

    public void play() throws IllegalStateException {
        if(canPlay()){
            if(state == State.LOADED)
                dispatcherThread.start();
            else
                Log.e("Audio", "Incorrect state: " + state.name());
            state = State.PLAYING;
        }
        else
            Log.w("'Hol up", "Audio name:" + name + " is not loaded and/or enabled");
    }

    public void stop(){
        if(dispatcher != null){
            dispatcher.stop();
            state = State.LOADED;
        }
    }

    public void restart(){
        if(isPlaying())
            stop();
        play();
    }

    public void setBarTemplate(BarTemplate barTemplate){
        this.barTemplate = barTemplate;
        writeMetaData();
    }

    public void setTicks(int ticks){
        this.ticks = ticks;
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
        if(metaData != null)
            metaData.delete();
    }

    public static Audio loadAudio(File dir, final String name){
        try{
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
        } catch(Exception e){
            Log.e("Audio", "Load file " + name + " failure.", e);
            return null;
        }
    }

    public enum State{
        UNLOADED,
        LOADED,
        PLAYING
    }
}