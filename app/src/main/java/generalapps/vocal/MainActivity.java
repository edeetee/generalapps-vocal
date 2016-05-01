package generalapps.vocal;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.style.TtsSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.github.lzyzsd.circleprogress.DonutProgress;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;


public class MainActivity extends ActionBarActivity {

    Recorder recorder;
    MusicAdapter musicAdapter;
    static Activity context;
    static DonutProgress recordProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO remove folder delete
        File audioDir = new File(getFilesDir(), "audios");
        if(!audioDir.exists())
            audioDir.mkdir();

        List<Audio> oldAudios = new ArrayList<>();

        final Pattern audioPattern = Pattern.compile("recording_\\d\\d_\\d\\d_\\d\\d.wav");
        final Pattern metaPattern = Pattern.compile("recording_\\d\\d_\\d\\d_\\d\\d.json");

        File[] audioFiles = audioDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return audioPattern.matcher(file.getName()).matches();
            }
        });

        for(File audioFile : audioFiles){
            File metaData = new File(audioDir, audioFile.getName().replace("wav", "json"));
            if(metaData.exists())
                oldAudios.add(new Audio(audioFile, metaData));
            else
                Log.w("MetaData Loading", "Metadata does not exist for the file " + audioFile.getName());
        }

        for(Audio audio : oldAudios){
            if(audio.getState() == 2){
                Log.w("Bad State", "Audio " + audio.name + " is " + audio.getState());

            }
        }

        recorder = new Recorder(this);

        ListView list = (ListView)findViewById(R.id.mainListView);
        musicAdapter = new MusicAdapter(this, oldAudios);
        list.setAdapter(musicAdapter);

        recordProgress = (DonutProgress)findViewById(R.id.donut_progress);
        //recordProgress.setMax(4);
        recordProgress.setSuffixText("");
        recordProgress.setInnerBottomText("Play/Record");
        //playing
        recordProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!musicAdapter.playing){
                    musicAdapter.play();
                    recordProgress.setInnerBottomText("Stop");
                }
                else{
                    musicAdapter.stop();
                    recordProgress.setInnerBottomText("Play/Record");
                }
            }
        });
        //recording
        recordProgress.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(!musicAdapter.playing){
                    recorder.record();
                }
                return true;
            }
        });
        //stopping recording
        recordProgress.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(recorder.isRecording() && event.getAction() == MotionEvent.ACTION_UP){
                    recorder.stop();
                }
                return false;
            }
        });

        Rhythm.bpm = 100;
        SeekBar metronomeBar = (SeekBar)findViewById(R.id.metronomeBar);
        metronomeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                int minBPM = 40;
                int maxBPM = 200;
                //don't allow BPM changing atm
                //Rhythm.bpm = minBPM + progress * (maxBPM-minBPM) / 100;
            };
        });


        Rhythm.bars = 1;
        NumberPicker barsPicker = (NumberPicker)findViewById(R.id.barsPicker);
        barsPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        barsPicker.setMinValue(1);
        barsPicker.setMaxValue(4);
        //actual barspicker value = 2^(n-1)
        barsPicker.setDisplayedValues(new String[]{"1", "2", "4", "8"});
        barsPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                Rhythm.bars = 1 << newVal-1;
            }
        });


    }
}
