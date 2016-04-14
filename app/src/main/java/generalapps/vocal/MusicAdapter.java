package generalapps.vocal;

import android.app.Activity;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by edeetee on 13/04/2016.
 */
public class MusicAdapter extends BaseAdapter {

    List<Audio> audios;
    Activity context;

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
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = context.getLayoutInflater().inflate(R.layout.audio_list_view, parent, false);
        }

        TextView textView = (TextView)convertView.findViewById(R.id.Name);
        Audio audio = audios.get(position);
        textView.setText(audio.name);
        textView.setBackgroundColor( audio.isPlaying() ? Color.RED : Color.GRAY );

        return convertView;
    }
}
