package generalapps.vocal;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by edeetee on 8/09/2016.
 */
public class ArtistsFragment extends Fragment {
    Track mTrack;
    RecyclerView artistsRecycler;
    RecyclerView searchRecycler;
    EditText searchBox;

    public static ArtistsFragment newInstance() {
        Bundle args = new Bundle();

        ArtistsFragment fragment = new ArtistsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public void setTrack(Track track){
        mTrack = track;
    }

    public void setupAdapters(){
        final DatabaseReference usersRef = MainActivity.database.getReference("users");

        artistsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        final ItemTouchHelper artistsChangeHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.START | ItemTouchHelper.END) {
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if(viewHolder.getAdapterPosition() < mTrack.size())
                    return 0;
                return super.getMovementFlags(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                final int fromPos = viewHolder.getAdapterPosition();
                final int toPos = target.getAdapterPosition();
                return mTrack.swapEditorPos(fromPos, toPos);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                mTrack.removeEditor(viewHolder.getAdapterPosition());
            }
        });
        final FirebaseRecyclerAdapter artistsAdapter = new FirebaseRecyclerAdapter<Track.EditorItem, ArtistHolder>(Track.EditorItem.class, R.layout.artist_item, ArtistHolder.class, mTrack.audioMetaRef.child("editors").orderByChild("position")) {
            @Override
            protected void populateViewHolder(final ArtistHolder viewHolder, Track.EditorItem model, int position) {
                viewHolder.bind(model.uid, mTrack, new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        artistsChangeHelper.startDrag(viewHolder);
                        return false;
                    }
                });
            }
        };
        artistsRecycler.setItemAnimator(null);
        artistsRecycler.setAdapter(artistsAdapter);
        artistsChangeHelper.attachToRecyclerView(artistsRecycler);

        searchRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        FirebaseRecyclerAdapter searchAdapter = new FirebaseRecyclerAdapter<User, SearchHolder>(User.class, R.layout.artist_search_item, SearchHolder.class, usersRef.orderByChild("name")) {
            @Override
            protected void populateViewHolder(SearchHolder viewHolder, User model, int position) {
                viewHolder.bind(model, mTrack);
            }
        };
        searchRecycler.setAdapter(searchAdapter);

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
                FirebaseRecyclerAdapter searchAdapter = new FirebaseRecyclerAdapter<User, SearchHolder>(User.class, R.layout.artist_item, SearchHolder.class, usersRef.orderByChild("name").startAt(s.toString())) {
                    @Override
                    protected void populateViewHolder(SearchHolder viewHolder, User model, int position) {
                        viewHolder.bind(model, mTrack);
                    }
                };
                searchRecycler.setAdapter(searchAdapter);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mTrack.addOnTrackChangeListener(new Track.OnTrackChangeListener() {
            @Override
            public void OnLoad(Track track) {

            }

            @Override
            public void OnAdd(int pos) {
                artistsAdapter.notifyItemChanged(pos);
            }

            @Override
            public void OnRemoved(int pos) {
                artistsAdapter.notifyItemChanged(pos);
            }

            @Override
            public void OnChanged(int pos) {

            }

            @Override
            public void OnDelete() {

            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.artists_fragment, container, false);

        artistsRecycler = (RecyclerView)fragView.findViewById(R.id.artistsRecycler);
        searchRecycler = (RecyclerView)fragView.findViewById(R.id.searchRecycler);
        searchBox = (EditText)fragView.findViewById(R.id.search);

        setupAdapters();

        return fragView;
    }

    public static class SearchHolder extends RecyclerView.ViewHolder{
        TextView text;
        public SearchHolder(View itemView) {
            super(itemView);
            text = (TextView)itemView.findViewById(R.id.text);
        }

        public void bind(final User user, final Track track){
            text.setText(user.name);
            text.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    track.addEditor(user);
                }
            });
        }
    }

    public static class ArtistHolder extends RecyclerView.ViewHolder{
        TextView text;
        ImageView handle;

        public ArtistHolder(View view){
            super(view);
            text = (TextView)view.findViewById(R.id.text);
            handle = (ImageView) view.findViewById(R.id.handle);
        }

        public void bind(String userID, Track track, View.OnTouchListener handleTouchListener){
            MainActivity.database.getReference("users").child(userID).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    if(user != null)
                        text.setText(user.name);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("ArtistHolder", databaseError.getMessage());
                }
            });

            if(getAdapterPosition() < track.size()){
                handle.setVisibility(View.INVISIBLE);
                handle.setOnTouchListener(null);
            }
            else{
                handle.setVisibility(View.VISIBLE);
                handle.setOnTouchListener(handleTouchListener);
            }
        }
    }
}
