package generalapps.vocal;

import android.app.Activity;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SeekBar;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by edeetee on 13/04/2016.
 */
public class MusicAdapter extends BaseAdapter {

    AudioGroup group;
    Activity context;
    boolean playing = false;
    int beats = 0;
    ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 20);
    Timer timer = new Timer();

    private boolean recordAtEnd;
    private boolean stopAtEnd;

    View.OnClickListener infoClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = getInnerViewPosition(v);
            Audio audio = group.get(pos);
            audio.setEnabled(!audio.enabled);

            WaveView wave = (WaveView)v.findViewById(R.id.waveform);
            wave.postInvalidate();
        }
    };

    Button.OnClickListener deleteClick = new Button.OnClickListener(){
        @Override
        public void onClick(View v){
            int pos = getInnerViewPosition(v);
            if(pos == 0){
                group.delete();
                group = null;
                notifyDataSetChanged();
            } else
                group.remove(pos);
        }
    };

    private Handler progressHandler = new Handler();
    long startTime;

    public float getProgress(){
        return (float)(System.currentTimeMillis()-startTime)/group.msMaxPeriod() % 1;
    }

    public MusicAdapter(Activity context, AudioGroup group) {
        this.context = context;
        this.group = group;
    }

    public MusicAdapter(Activity context){
        this.context = context;
    }

    public void setGroup(AudioGroup group){
        this.group = group;
        notifyDataSetChanged();
    }

    public void play() {
        if (playing || getCount() == 0)
            return;

        if(getCount() == 1)
            MainActivity.lengthSeekBar.setVisibility(View.GONE);

        MainActivity.recordProgress.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.recordProgress.setInnerBottomText("Stop");
            }
        });

        playing = true;
        startTime = System.currentTimeMillis();

        for (int i = 0; i < getCount(); i++) {
            Audio audio = group.get(i);
            audio.play();
            audio.view.findViewById(R.id.waveform).postInvalidate();
        }

        beats = 0;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                beats = (beats+1)%Rhythm.maxBeats();
                updateAudios();
                if (!playing)
                    cancel();
                if(recordAtEnd){
                    //if was too close to end
                    if(beats == Rhythm.bpb*(Rhythm.maxBars-1)){
                        stopAtEnd = true;
                        MainActivity.recorder.startRecordingBeatIn();
                    } else if(beats == 0 && stopAtEnd){
                        stopAtEnd = false;
                        recordAtEnd = false;
                        stop();
                    }
                }
            }
        }, group.msBeatPeriod(), group.msBeatPeriod());
    }

    public void updateAudios(){
        for(Audio audio : group){
            if(audio.canPlay()){
                if(beats % (audio.bars*Rhythm.bpb) == 0){
                    audio.restart();
                }
            }
        }
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public void stop(){
        if(!playing)
            return;

        if(getCount() == 1)
            MainActivity.lengthSeekBar.setVisibility(View.VISIBLE);

        MainActivity.recordProgress.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.recordProgress.setInnerBottomText("Play/Record");
            }
        });

        playing = false;
        beats = 0;

        for(Audio audio : group){
            if(audio.canPlay())
                audio.stop();
        }
    }

    public void recordAtEnd(){
        MainActivity.recorder.prepareRecord();
        recordAtEnd = true;
    }

    @Override
    public int getCount(){
        return group == null ? 0 : group.size();
    }

    @Override
    public Object getItem(int position) {
        return group.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = context.getLayoutInflater().inflate(R.layout.audio_list_view, parent, false);
        }

        Audio audio = group.get(position);
        audio.view = convertView;

        audio.setName();

        FrameLayout info = (FrameLayout) convertView.findViewById(R.id.waveAndName);
        info.setOnClickListener(infoClick);

        Button delete = (Button)convertView.findViewById(R.id.delete);
        if(position == 0)
            delete.setText("Delete (All)");
        else
            delete.setText("Delete");
        delete.setOnClickListener(deleteClick);

        if(audio.waveValues != null){
            WaveView waveView = (WaveView)convertView.findViewById(R.id.waveform);
            waveView.setAudio(audio);
            waveView.invalidate();
        }

        audio.setBars();

        if(getCount() == 1 && !playing){
            MainActivity.lengthSeekBar.setVisibility(View.VISIBLE);
            MainActivity.lengthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    float pos = (float)(i-50)/50;
                    group.setMsBarPeriodMod(Math.round(pos * 1000));
                    notifyDataSetChanged();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        } else {
            MainActivity.lengthSeekBar.setVisibility(View.GONE);
        }

        return convertView;
    }

    int getInnerViewPosition(View v){
        ViewParent parent = v.getParent();
        while(parent != null){
            if(parent.getClass().equals(ListView.class))
                break;
            parent = parent.getParent();
        }
        ListView listView = (ListView)parent;
        return listView.getPositionForView(v);
    }
}
