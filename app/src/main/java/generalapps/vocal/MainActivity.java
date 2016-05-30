package generalapps.vocal;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    static LinearLayout adjustLayout;

    static WaveView testThis;

    Handler handler;

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

        handler = new Handler();

        final int adjustVal = 0;
        final Runnable adjustLeftRunnable = new Runnable() {
            @Override
            public void run() {
                adapter.adjustGroup(20);
                handler.postDelayed(this, 50);
            }
        };
        final Runnable adjustRightRunnable = new Runnable() {
            @Override
            public void run() {
                adapter.adjustGroup(-20);
                handler.postDelayed(this, 50);
            }
        };

        View.OnTouchListener adjustTouch = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Runnable adjustRunnable = (view.getId() == R.id.leftAdjust) ? adjustLeftRunnable : adjustRightRunnable;
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    handler.post(adjustRunnable);
                    return true;
                } else if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                    handler.removeCallbacks(adjustRunnable);
                    return true;
                }

                return false;
            }
        };
        ImageView leftAdjust = (ImageView)findViewById(R.id.leftAdjust);
        leftAdjust.setOnTouchListener(adjustTouch);
        leftAdjust.setColorFilter(Color.BLACK);
        ImageView rightAdjust = (ImageView)findViewById(R.id.rightAdjust);
        rightAdjust.setOnTouchListener(adjustTouch);
        rightAdjust.setColorFilter(Color.BLACK);

        adjustLayout = (LinearLayout)findViewById(R.id.adjustLayout);

        recordProgress = (RecorderCircle)findViewById(R.id.recordProgress);
        //recordProgress.setMax(4);
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
                //only if not first recording. First recording doesn't use longClick
                Recorder.State state = recorder.getState();
                if(adapter.getCount() != 0 && state == Recorder.State.NONE){
                    if(!adapter.playing)
                        recorder.record();
                    else
                        adapter.recordAtEnd();
                }
                return true;
            }
        });
        //stopping recording
        recordProgress.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Recorder.State state = recorder.getState();
                int action = event.getAction();
                //recording let go
                if((state == Recorder.State.RECORDING || state == Recorder.State.PREPARED)
                        && action == MotionEvent.ACTION_UP){
                    recorder.stop();
                    adapter.play();
                }
                //if first recording
                if(state == Recorder.State.NONE && adapter.getCount() == 0 && action == MotionEvent.ACTION_DOWN){
                    recorder.record();
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
