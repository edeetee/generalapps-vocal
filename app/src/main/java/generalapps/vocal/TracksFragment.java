package generalapps.vocal;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;

/**
 * Created by edeetee on 1/06/2016.
 */
public class TracksFragment extends Fragment {
    interface TracksFragmentListener{
        void OnTrackSelected(Track.MetaData meta);
        void OnNewTrack();
    }
    TracksFragmentListener mCallback;
    File audioDir;
    RecyclerView recycler;
    FilteredFirebaseRecyclerAdapter adapter;

    Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;

        audioDir = new File(context.getFilesDir(), "audios");

        try{
            mCallback = (TracksFragmentListener)context;
        } catch(ClassCastException e){
            Log.e("TracksFragment", context.toString() + " didn't implement " + TracksFragmentListener.class.getName(), e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.tracks_fragment, container, false);
        recycler = (RecyclerView)fragView.findViewById(R.id.tracksRecycler);

        DatabaseReference metaRef = MainActivity.database.getReference("meta").child("tracks");
        adapter = new FilteredFirebaseRecyclerAdapter<TrackHolder>(TrackHolder.class, R.layout.track_item, metaRef) {
            @Override
            public void populateViewHolder(TrackHolder holder, DataSnapshot item) {
                holder.bind(item.getValue(Track.MetaData.class), mCallback);
            }
        };
        adapter.setFilter(new FilteredFirebaseRecyclerAdapter.Filter() {
            @Override
            public boolean shouldAdd(DataSnapshot data) {
                for (DataSnapshot editor : data.child("editors").getChildren()) {
                    if(editor.getValue(Track.EditorItem.class).uid.equals(MainActivity.user.uid))
                        return true;
                }
                return false;
            }
        });

        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));

        FloatingActionButton createNew = (FloatingActionButton)fragView.findViewById(R.id.FABnew);
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

    public static class TrackHolder extends RecyclerView.ViewHolder{
        TextView name;

        public TrackHolder(View itemView){
            super(itemView);
            name = (TextView)itemView.findViewById(R.id.name);
        }

        public void bind(final Track.MetaData trackMeta, final TracksFragmentListener callback){
            name.setText(trackMeta.getFirst());

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.OnTrackSelected(trackMeta);
                }
            });
        }
    }
}