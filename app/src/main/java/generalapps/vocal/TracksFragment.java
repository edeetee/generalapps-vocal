package generalapps.vocal;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by edeetee on 1/06/2016.
 */
public class TracksFragment extends ListFragment {
    TracksAdapter.TracksFragmentListener mCallback;
    File audioDir;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        audioDir = new File(context.getFilesDir(), "audios");

        try{
            mCallback = (TracksAdapter.TracksFragmentListener)context;
        } catch(ClassCastException e){
            Log.e("TracksFragment", context.toString() + " didn't implement " + TracksAdapter.TracksFragmentListener.class.getName(), e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.tracks_fragment, container, false);

        List<File> files = getTrackDirs();

        final TracksAdapter adapter = new TracksAdapter(files, mCallback);

        setListAdapter(adapter);

        TextView createNew = (TextView)fragView.findViewById(R.id.createNew);
        createNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.OnNewTrack();
            }
        });

        return fragView;
    }

    private List<File> getTrackDirs(){
        if (!audioDir.exists())
            audioDir.mkdir();

        File[] audioDirs = audioDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if(file.isDirectory())
                    return true;

                file.delete();
                return false;
            }
        });

        return Arrays.asList(audioDirs);
    }
}
