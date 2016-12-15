package generalapps.vocal;

import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import info.hoang8f.android.segmented.SegmentedGroup;

/**
 * Created by edeetee on 8/09/2016.
 */
public class ArtistsFragment extends Fragment {
    Track mTrack;
    @BindView(R.id.artistsRecycler) RecyclerView artistsRecycler;
    FilteredFirebaseRecyclerAdapter artistsAdapter;
    PreviousEditorLinkDecoration previousEditorLinkDecoration;

    @BindView(R.id.artist_searcher) ArtistSearcher searcher;
    @BindView(R.id.shuffle) SegmentedGroup shuffle;
    @BindView(R.id.freeMode) SegmentedGroup freeMode;
    @BindView(R.id.optionsLayout) LinearLayout optionsLayout;
    @BindView(R.id.artistsHeader) TextView artistsHeader;
    @BindView(R.id.specialEditorsLayout) LinearLayout specialEditorsLayout;
    @BindView(R.id.artistsLayout) LinearLayout artistsLayout;

    @BindView(R.id.previousChoice) Button previousChoiceButton;
    @BindView(R.id.join) Button joinButton;

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
        artistsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        final ItemTouchHelper artistsChangeHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.START | ItemTouchHelper.END) {
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if(viewHolder.getAdapterPosition() < (mTrack.currentEditorIndex))
                    return 0;
                return super.getMovementFlags(recyclerView, viewHolder);
            }

            int prevFromPos = -1;
            int prevToPos = -1;
            RecyclerView.ViewHolder prevViewHolder;

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                //to deal with firebase updating, only call if pos's have changed or if operating on a new viewHolder
                final int fromPos = viewHolder.getAdapterPosition();
                final int toPos = target.getAdapterPosition();
                if(prevFromPos != fromPos || prevToPos != toPos || prevViewHolder != viewHolder){
                    mTrack.swapEditorPos(fromPos, toPos);
                    prevFromPos = fromPos;
                    prevToPos = toPos;
                    prevViewHolder = viewHolder;
                }
                return false;
            }



            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                mTrack.removeEditor(viewHolder.getAdapterPosition());
            }
        });
        artistsAdapter = new FilteredFirebaseRecyclerAdapter<ArtistHolder>(ArtistHolder.class, R.layout.artist_item, mTrack.audioMetaRef.child("editors").orderByChild("position")) {
            @Override
            public void populateViewHolder(final ArtistHolder holder, DataSnapshot item) {
                holder.bind(item.getValue(Track.EditorItem.class).uid, mTrack, new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        artistsChangeHelper.startDrag(holder);
                        return false;
                    }
                });
            }
        };
        artistsAdapter.setFilter(mTrack.getFreeMode() ? new FilteredFirebaseRecyclerAdapter.Filter() {
            final List<String> uids = new ArrayList<>();
            @Override
            public boolean shouldAdd(DataSnapshot data) {
                String uid = data.getValue(Track.EditorItem.class).uid;
                if(Track.SpecialEditor.parse(uid) == null && !uids.contains(uid)){
                    uids.add(uid);
                    return true;
                }
                return false;
            }
        } : null);

        artistsRecycler.setItemAnimator(null);
        artistsRecycler.setAdapter(artistsAdapter);
        artistsRecycler.addItemDecoration(new SimpleDividerDecoration(getContext(), 0.03f));
        artistsRecycler.addItemDecoration(new PreviousEditorLinkDecoration(getContext(), mTrack));
        artistsChangeHelper.attachToRecyclerView(artistsRecycler);

        searcher.setOnUserSelectedListener(new ArtistSearcher.OnUserSelectedListener() {
            @Override
            public void OnUserSelected(User user) {
                mTrack.addEditor(user.uid);
            }
        });

        mTrack.addOnTrackChangeListener(new Track.OnTrackChangeListener() {
            int lastEditingIndex = mTrack.currentEditorIndex;
            boolean lastIsSetup = mTrack.isSetup();
            @Override
            public void OnLoad(Track track) {

            }

            @Override
            public void OnDataChange(Track track) {
                if(track.currentEditorIndex != lastEditingIndex){
                    artistsAdapter.notifyItemChanged(lastEditingIndex);
                    artistsAdapter.notifyItemChanged(track.currentEditorIndex);
                    lastEditingIndex = track.currentEditorIndex;
                }
                if(!lastIsSetup && track.isSetup()){
                    optionsLayout.setVisibility(View.GONE);
                    artistsAdapter.notifyItemChanged(track.currentEditorIndex);
                    lastIsSetup = track.isSetup();
                }
            }

            @Override
            public void OnAudioAdd(int pos) {
                artistsAdapter.notifyItemChanged(pos);
            }

            @Override
            public void OnAudioRemoved(int pos) {
                artistsAdapter.notifyItemChanged(pos);
            }

            @Override
            public void OnAudioChanged(int pos) {

            }

            @Override
            public void OnDelete() {

            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View fragView = inflater.inflate(R.layout.artists_fragment, container, false);
        ButterKnife.bind(this, fragView);

        setupAdapters();

        KeyboardVisibilityEvent.setEventListener(getActivity(), new KeyboardVisibilityEventListener() {
            @Override
            public void onVisibilityChanged(boolean isOpen) {
                int visibility = isOpen ? View.GONE : View.VISIBLE;
                optionsLayout.setVisibility(visibility);
                artistsHeader.setVisibility(visibility);
                specialEditorsLayout.setVisibility(visibility);
                searcher.setLayoutParams(Utils.setWeight((LinearLayout.LayoutParams)searcher.getLayoutParams(), isOpen ? 3 : 2));
                artistsLayout.setLayoutParams(Utils.setWeight((LinearLayout.LayoutParams)artistsLayout.getLayoutParams(), isOpen ? 2 : 3));
            }
        });


        freeMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                boolean freeModeEnabled = checkedId == R.id.freeModeEnabled;
                mTrack.setFreeMode(freeModeEnabled);
                artistsHeader.setText(freeModeEnabled ? "Artist" : "Order Artist");
                artistsAdapter.setFilter(freeModeEnabled ? new FilteredFirebaseRecyclerAdapter.Filter() {
                    final List<String> uids = new ArrayList<>();
                    @Override
                    public boolean shouldAdd(DataSnapshot data) {
                        String uid = data.getValue(Track.EditorItem.class).uid;
                        if(Track.SpecialEditor.parse(uid) == null && !uids.contains(uid)){
                            uids.add(uid);
                            return true;
                        }
                        return false;
                    }
                } : null);
                joinButton.setEnabled(!freeModeEnabled);
                previousChoiceButton.setEnabled(!freeModeEnabled);
                shuffle.setEnabled(!freeModeEnabled);
            }
        });
        freeMode.check(mTrack.getFreeMode() ? R.id.freeModeEnabled : R.id.freeModeDisabled);

        optionsLayout.setVisibility(mTrack.isSetup() ? View.GONE : View.VISIBLE);

        shuffle.check(mTrack.getShuffled() ? R.id.shuffleEnabled : R.id.shuffleDisabled);
        shuffle.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mTrack.setShuffled(checkedId == R.id.shuffleEnabled);
                artistsAdapter.notifyItemRangeChanged(0, artistsAdapter.getItemCount());
            }
        });

        return fragView;
    }

    @OnClick({R.id.random, R.id.previousChoice, R.id.join})
    void onRandomClick(Button view){
        Track.SpecialEditor specialEditor = null;
        switch (view.getId()){
            case R.id.random:
                specialEditor = Track.SpecialEditor.RANDOM;
                break;
            case R.id.previousChoice:
                specialEditor = Track.SpecialEditor.PREVIOUS;
                break;
            case R.id.join:
                Toast toast = Toast.makeText(getContext(), "Join cannot be used yet", Toast.LENGTH_SHORT);
                toast.show();
                return;
        }
        
        mTrack.addEditor(specialEditor.encodeWithColor());
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        searcher.cleanup();
        artistsAdapter.cleanup();
    }

    public static class ArtistHolder extends RecyclerView.ViewHolder{
        @BindView(R.id.text) TextView text;
        @BindView(R.id.handle) ImageView handle;
        @BindView(R.id.index) TextView index;
        @BindView(R.id.duplicate) ImageView duplicate;
        ColorStateList textColor;

        public ArtistHolder(View view){
            super(view);
            ButterKnife.bind(this, view);
            textColor = text.getTextColors();
        }

        public void bind(final String userID, final Track track, View.OnTouchListener handleTouchListener){
            final Track.SpecialEditor trySpecial = Track.SpecialEditor.parse(userID);
            if(trySpecial != null){
                duplicate.setVisibility(View.VISIBLE);
                duplicate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        track.insertEditor(getAdapterPosition()+1, userID);
                    }
                });
                Track.SpecialEditor.loadPrint(userID, track, new Track.SpecialEditor.PrintCallback() {
                    @Override
                    public void printLoaded(String print) {
                        text.setText(print);
                    }
                });
            } else{
                duplicate.setVisibility(View.GONE);
                duplicate.setOnClickListener(null);
                text.setTextColor(textColor);
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
            }

            index.setVisibility(track.getFreeMode() ? View.GONE : View.VISIBLE);
            index.setText(track.getShuffled() ? "?" : Integer.toString(getAdapterPosition()+1));

            if(track.getFreeMode() || getAdapterPosition() < track.size()){
                handle.setVisibility(View.INVISIBLE);
                handle.setOnTouchListener(null);
            }
            else{
                handle.setVisibility(View.VISIBLE);
                handle.setOnTouchListener(handleTouchListener);
            }

            if(track.isSetup() && getAdapterPosition() == track.currentEditorIndex)
                text.setPaintFlags(text.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            else
                text.setPaintFlags(text.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
        }
    }
}
