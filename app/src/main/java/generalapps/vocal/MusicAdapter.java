package generalapps.vocal;

import android.app.Activity;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by edeetee on 13/04/2016.
 */
public class MusicAdapter extends BaseAdapter {

    List<Audio> audios;
    Activity context;
    boolean playing = false;
    int beats = 0;
    ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 20);
    Timer timer = new Timer();

    View.OnClickListener infoClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = getInnerViewPosition(v);
            Audio audio = audios.get(pos);
            audio.enabled = !audio.enabled;
            v.setBackgroundColor(audio.enabled ? Color.TRANSPARENT : Color.GRAY);
        }
    };

    Button.OnClickListener deleteClick = new Button.OnClickListener(){
        @Override
        public void onClick(View v){
            int pos = getInnerViewPosition(v);
            delete(pos);
        }
    };

    public MusicAdapter(Activity context) {
        this(context, new ArrayList<Audio>());
    }

    public MusicAdapter(Activity context, List<Audio> audios) {
        this.context = context;
        this.audios = audios;
    }

    public void add(Audio audio){
        audios.add(audio);
        notifyDataSetChanged();
    }

    public void play(){
        if(playing)
            return;

        playing = true;
        for(Audio audio : audios){
            audio.play();
        }

        beats = 0;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                beats++;
                updateAudios();
                if(!playing)
                    cancel();
            }
        }, Rhythm.msBeatPeriod(), Rhythm.msBeatPeriod());
    }

    public void updateAudios(){
        for(Audio audio : audios){
            if(audio.canPlay()){
                if(beats % (audio.bars*Rhythm.bpb) == 0){
                    audio.restart();
                }
            }
        }
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetInvalidated();
            }
        });
    }

    public void stop(){
        if(!playing)
            return;

        playing = false;
        beats = 0;
        for(Audio audio : audios){
            audio.stop();
        }
    }

    public void delete(int pos){
        delete(audios.get(pos));
    }

    public void delete(Audio audio){
        audios.remove(audio);
        audio.delete();
        audio = null;
        notifyDataSetChanged();
    }

    @Override
    public int getCount(){
        return audios.size();
    }

    @Override
    public Object getItem(int position) {
        return audios.get(position);
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

        Audio audio = audios.get(position);
        if(audio.adapter != this)
            audio.adapter = this;
        audio.view = convertView;

        audio.setName();

        TextView info = (TextView)convertView.findViewById(R.id.name);
        info.setOnClickListener(infoClick);

        Button delete = (Button)convertView.findViewById(R.id.delete);
        delete.setOnClickListener(deleteClick);

        if(audio.waveValues != null){
            WaveView waveView = (WaveView)convertView.findViewById(R.id.waveform);
            waveView.points = audio.waveValues;
            waveView.invalidate();
        }

        audio.setBars();


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
