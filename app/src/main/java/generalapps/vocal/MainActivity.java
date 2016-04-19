package generalapps.vocal;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.style.TtsSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity {

    Recorder recorder;
    boolean playing = false;
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timer = new Timer();

        //TODO remove folder delete
        File audios = new File(getFilesDir(), "audios");
        if(!audios.exists())
            audios.mkdir();

        List<Audio> oldAudios = new ArrayList<Audio>();
        for(File child : audios.listFiles()){
           oldAudios.add(new Audio(child.getName(), child.getAbsolutePath()));
        }

        recorder = new Recorder(this);

        ListView list = (ListView)findViewById(R.id.mainListView);
        list.setAdapter(new MusicAdapter(this, oldAudios));

        Button recButton = (Button)findViewById(R.id.recordButton);
        recButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!recorder.isRecording()){
                    recorder.record();
                } else {
                    recorder.stop();
                }
            }
        });

        Button playButton = (Button)findViewById(R.id.playButton);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicAdapter adapter = (MusicAdapter)((ListView) findViewById(R.id.mainListView)).getAdapter();
                for(Audio audio : adapter.audios){
                    if(!playing){
                        audio.play();
                    }
                    else
                        audio.stop();
                }
                playing = true;
                ((Button)v).setText(playing ? "Stop" : "Play");

                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                playing = false;
                                ((Button)findViewById(R.id.playButton)).setText(playing ? "Stop" : "Play");
                            }
                        });
                    }
                }, Rhythm.msTotalPeriod());
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
                Rhythm.bpm = minBPM + progress * (maxBPM-minBPM) / 100;
            };
        });
    }
}
