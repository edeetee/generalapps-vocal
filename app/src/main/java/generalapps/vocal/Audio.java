package generalapps.vocal;

import android.media.AudioManager;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteOrder;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.resample.RateTransposer;
import be.tarsos.dsp.resample.Resampler;

/**
 * Created by edeetee on 13/04/2016.
 */
public class Audio implements AudioProcessor {
    AudioDispatcher dispatcher;
    Thread dispatcherThread;
    TarsosDSPAudioInputStream input;
    AndroidAudioPlayer audioPlayer;

    String name;
    File audioFile;
    File metaData;
    int bars = 1;
    int ticks;
    int maxBeats;

    State state;
    double timeStamp;

    public interface OnAudioChangeListener {
        void OnChange();
    }
    OnAudioChangeListener mCallback;
    WaveView.WaveValues.OnMaxWaveValueChangedListener mMaxCallback;

    WaveView.WaveValues waveValues;

    View view;
    Track group;
    //is audio enabled in ui
    boolean enabled = true;
    boolean roundBarToNext = true;

    public Audio(){
        //TODO make AudioGroup and Audio constructors linked so that Recorder.FREQ*Rhythm.msMaxPeriod()/1000 can be used for buffer
        state = State.UNLOADED;
        waveValues = new WaveView.WaveValues();
        //super(AudioManager.STREAM_MUSIC, Recorder.FREQ, AudioFormat.CHANNEL_OUT_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, 1000000, AudioTrack.MODE_STATIC);
        //super(new TarsosDSPAudioFormat(Recorder.FREQ, 16, 1, true, true), 500000, AudioManager.STREAM_MUSIC);
    }

    public Audio(File metaData){
        this();
        setMetaData(metaData);
    }

    public Audio(File audio, File metaData){
        this(metaData);
        setFile(audio);
    }

    public void setTrack(Track callback){
        group = callback;
        mCallback = callback;
        waveValues.setOnMaxWaveValueChangedListener(callback);
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

        if(mCallback != null)
            mCallback.OnChange();
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

        loadDispatcher();

        state = State.LOADED;

//        int written = write(shortBuf, 0, ticks);
//        if(written < ticks)
//            Log.e("AudioWrite", "Audio write failed. Length: " + Integer.toString(ticks) + ", Written: " + Integer.toString(written));

        //setNotificationMarkerPosition(ticks);
    }

    public void loadDispatcher(){
        final double tempo = 1.0;
        //WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(tempo, Recorder.FREQ));

        final int audioBufferSize = 2048;
        final int overlap = audioBufferSize - audioBufferSize/16;
        TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(Recorder.FREQ, 16, 1, true, ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN));
        try{
            input = new UniversalAudioInputStream(new BufferedInputStream(new FileInputStream(audioFile)), format);
        } catch (FileNotFoundException e){
            Log.e("Audio", "File not found", e);
        }
        dispatcher = new AudioDispatcher(input, audioBufferSize, overlap);

        //wsola.setDispatcher(dispatcher);
        //foreach effect
        audioPlayer = new AndroidAudioPlayer(format, 10000, AudioManager.STREAM_MUSIC);
        //processor = new PitchShifter(dispatcher, tempo, Recorder.FREQ, audioBufferSize, audioBufferSize - audioBufferSize/8);
        //pitchShifter = new PitchShifterNew(tempo, Recorder.FREQ, audioBufferSize, overlap);
        dispatcher.addAudioProcessor(this);
        //dispatcher.addAudioProcessor(wsola);
        //dispatcher.addAudioProcessor(pitchShifter);
        dispatcher.addAudioProcessor(audioPlayer);

        dispatcherThread = new Thread(dispatcher, name + " player thread");
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        timeStamp = audioEvent.getTimeStamp();
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
        if(dispatcher != null)
            dispatcher.stop();
    }

    public void restart(){
        if(isPlaying())
            stop();
        if(timeStamp != 0)
            dispatcher.skip(0);
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
        if(view != null){
            WaveView wave = (WaveView)view.findViewById(R.id.waveform);
            wave.updateWave();
            wave.postInvalidate();
            view.findViewById(R.id.bars).postInvalidate();
        }
//        if(view != null)
//            view.findViewById(R.id.waveform).postInvalidate();
    }

    public void setTicks(int ticks){
        this.ticks = ticks;
        autoSetBar();
    }

    public void autoSetBar(){
        float calcBars = Rhythm.maxBars*ticks/group.maxTicks();
        int prevBar = 1;
        int nextBar = 1;
        for(int bars : Rhythm.barTypes){
            prevBar = nextBar;
            nextBar = bars;

            if(calcBars < nextBar)
                break;
        }
        setBars(roundBarToNext ? nextBar : prevBar);
    }

    //if roundBarToNext is true, the bar length will emcompass the whole length, otherwise it will clip
    public void setRoundBarToNext(boolean roundBarToNext){
        this.roundBarToNext = roundBarToNext;
        autoSetBar();
    }

    public void toggleRoundBarToNext(){
        setRoundBarToNext(!roundBarToNext);
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