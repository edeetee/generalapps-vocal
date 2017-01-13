package generalapps.vocal;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import java.io.File;

/**
 * Created by edeetee on 1/06/2016.
 */
public class TracksListFragment extends Fragment {
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View fragView = inflater.inflate(R.layout.tracks_fragment, container, false);
        recycler = (RecyclerView)fragView.findViewById(R.id.tracksRecycler);

        recycler.addItemDecoration(new SimpleDividerDecoration(getContext(), 0.03f));

        final DatabaseReference metaRef = MainActivity.database.getReference("meta").child("tracks");
        adapter = new FilteredFirebaseRecyclerAdapter<TrackHolder>(TrackHolder.class, R.layout.track_item, metaRef) {
            @Override
            public void populateViewHolder(TrackHolder holder, DataSnapshot item) {
                holder.bind(item.getValue(Track.MetaData.class), mCallback);
            }
        };
        adapter.setFilter(new FilteredFirebaseRecyclerAdapter.Filter() {
            @Override
            public boolean shouldAdd(DataSnapshot data) {
                Track.MetaData trackMeta = data.getValue(Track.MetaData.class);
                return trackMeta.canOpen(MainActivity.user.uid);
            }
        });

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT){
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                Utils.deleteTrack(adapter.getItem(viewHolder.getAdapterPosition()).getValue(Track.MetaData.class));
            }
        });
        helper.attachToRecyclerView(recycler);

        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));

        FloatingActionButton createNew = (FloatingActionButton)fragView.findViewById(R.id.FABnew);
        createNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.OnNewTrack();
            }
        });

        HowToOverlay.showHelpIfUnseen(HowToOverlay.HowToInfo.NEW_TRACK, createNew);

        return fragView;
    }

    public static class TrackHolder extends RecyclerView.ViewHolder{
        TextView name;
        ProgressBar progress;

        public TrackHolder(View itemView){
            super(itemView);
            name = (TextView)itemView.findViewById(R.id.name);
            progress = (ProgressBar)itemView.findViewById(R.id.progressBar);
        }

        void bind(final Track.MetaData trackMeta, final TracksFragmentListener callback){
            if(trackMeta.finished)
                name.setText(trackMeta.title + " : Complete");
            else if(!trackMeta.isSetup)
                name.setText(trackMeta.title + " : Setup");
            else{
                trackMeta.getCurrentEditor(new Track.MetaData.EditorCallback() {
                    @Override
                    public void editorNameLoaded(User editor) {
                        name.setText(trackMeta.title + " : " + (!editor.uid.equals(MainActivity.user.uid) ? editor.firstName() + "'s turn" : "Your turn"));
                    }
                });
            }
                ;
            if(!trackMeta.freeMode && trackMeta.isSetup){
                progress.setVisibility(View.VISIBLE);
                progress.setMax(trackMeta.editors.size());
                progress.setProgress(trackMeta.currentEditIndex);
                if(trackMeta.finished)
                    DrawableCompat.setTint(progress.getProgressDrawable(), ContextCompat.getColor(itemView.getContext(), R.color.primary_light));
            } else
                progress.setVisibility(View.GONE);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.OnTrackSelected(trackMeta);
                }
            });
        }
    }
}