package generalapps.vocal;

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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity {

    Recorder recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recorder = new Recorder(this);

        ListView list = (ListView) findViewById(R.id.mainListView);
        list.setAdapter(new MusicAdapter(this));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MusicAdapter adapter = (MusicAdapter)((ListView) findViewById(R.id.mainListView)).getAdapter();
                Audio audio = (Audio)adapter.getItem(position);
                audio.PlayStop();
            }
        });

        Button button = (Button)findViewById(R.id.recordButton);
        button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!recorder.isRecording()){
                    recorder.record();
                } else {
                    recorder.stop();
                }
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
                recorder.bpm = minBPM + progress * (maxBPM-minBPM) / 100;
            };
        });
    }
}
