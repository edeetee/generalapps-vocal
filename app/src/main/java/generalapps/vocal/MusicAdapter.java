package generalapps.vocal;

import android.app.Activity;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * Created by edeetee on 13/04/2016.
 */
public class MusicAdapter extends BaseAdapter {

    List<Audio> audios;
    Activity context;

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

        TextView textView = (TextView)convertView.findViewById(R.id.name);
        Audio audio = audios.get(position);
        textView.setText(audio.name);

        LinearLayout info = (LinearLayout)convertView.findViewById(R.id.info);
        info.setOnClickListener(infoClick);

        Button delete = (Button)convertView.findViewById(R.id.delete);
        delete.setOnClickListener(deleteClick);

        ProgressBar progressBar = (ProgressBar)convertView.findViewById(R.id.progressBar);
        progressBar.setMax(audio.maxBeats);

        audio.view = convertView;

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
