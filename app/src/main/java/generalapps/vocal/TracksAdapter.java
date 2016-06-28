package generalapps.vocal;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.List;

/**
 * Created by edeetee on 31/05/2016.
 */
public class TracksAdapter extends ArrayAdapter<File> {
    TracksFragmentListener mCallback;

    public interface TracksFragmentListener {
        void OnTrackSelected(File file);
        void OnNewTrack();
    }

    public TracksAdapter(List<File> objects, TracksFragmentListener callback) {
        super(MainActivity.context, R.layout.tracks_fragment_list, objects);

        mCallback = callback;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = MainActivity.context.getLayoutInflater().inflate(R.layout.tracks_fragment_list, parent, false);
        }

        File file = getItem(position);

        TextView name = (TextView)convertView.findViewById(R.id.name);

        name.setText(file.getName());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.OnTrackSelected(getItem(Utils.getInnerViewPosition(view)));
            }
        });

        return convertView;
    }
}
