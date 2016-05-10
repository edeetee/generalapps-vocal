package generalapps.vocal;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.SeekBar;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class MainActivity extends ActionBarActivity {

    Recorder recorder;
    MusicAdapter musicAdapter;
    static Activity context;
    static RecorderCircle recordProgress;

    static WaveView testThis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        List<File> goodFiles = new ArrayList<>();

        for(File audioFile : audioFiles){
            File metaData = new File(audioDir, audioFile.getName().replace("wav", "json"));
            if(metaData.exists()){
                oldAudios.add(new Audio(audioFile, metaData));
                goodFiles.add(audioFile);
                goodFiles.add(metaData);
            }
        }

        for(File deleteFile : audioDir.listFiles()){
            Log.w("Deleting", "Deleting " + deleteFile.getName());
            if(!goodFiles.contains(deleteFile))
                deleteFile.delete();
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

        recordProgress = (RecorderCircle)findViewById(R.id.recordProgress);
        //recordProgress.setMax(4);
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
    }
}
