package generalapps.vocal;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.SeekBar;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class MainActivity extends ActionBarActivity {

    static Recorder recorder;
    static MusicAdapter adapter;
    static Activity context;
    static RecorderCircle recordProgress;
    static SeekBar lengthSeekBar;

    static WaveView testThis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //check permissions
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 0);
        }

        recorder = new Recorder(this);

        ListView list = (ListView)findViewById(R.id.mainListView);
        adapter = new MusicAdapter(this);
        list.setAdapter(adapter);

        lengthSeekBar = (SeekBar)MainActivity.context.findViewById(R.id.lengthSeekBar);

        recordProgress = (RecorderCircle)findViewById(R.id.recordProgress);
        //recordProgress.setMax(4);
        //playing
        recordProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!adapter.playing){
                    adapter.play();
                }
                else{
                    adapter.stop();
                }
            }
        });
        //recording
        recordProgress.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(!adapter.playing)
                    recorder.record();
                else
                    adapter.recordAtEnd();
                return true;
            }
        });
        //stopping recording
        recordProgress.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Recorder.State state = recorder.getState();
                if((state == Recorder.State.RECORDING || state == Recorder.State.PREPARED)
                        && event.getAction() == MotionEvent.ACTION_UP){
                    recorder.stop();
                    adapter.play();
                }
                return false;
            }
        });

        loadFiles();
    }

    private void loadFiles(){
        File audioDir = new File(getFilesDir(), "audios");
        if(!audioDir.exists())
            audioDir.mkdir();

        File[] audioDirs = audioDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });

        AudioGroup group = null;
        for(final File dir : audioDirs){
            if(group != null)
                group.delete();
            group = AudioGroup.load(dir);
        }

        adapter.setGroup(group);
    }
}
