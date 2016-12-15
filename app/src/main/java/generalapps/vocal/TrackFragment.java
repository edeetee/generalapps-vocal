package generalapps.vocal;

import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

/**
 * Created by edeetee on 8/09/2016.
 */
public class TrackFragment extends Fragment implements BackOverrideFragment, Track.OnTrackChangeListener {

    static final String ITEM_KEY = "track_key";
    static final String TAG = "TrackPagerFragment";
    static final String RECORDER_FRAGMENT_TAG = "RECORDER_FRAGMENT";
    static final String ARTIST_FRAGMENT_TAG = "ARTIST_FRAGMENT";

    FloatingActionButton publishFAB;
    Track.MetaData trackMeta;
    Track track;

    public static TrackFragment newInstance(Track.MetaData meta) {
        Bundle args = new Bundle();
        args.putSerializable(ITEM_KEY, meta);

        TrackFragment fragment = new TrackFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        if(savedInstanceState == null){
            if (getArguments() != null)
                if (getArguments().containsKey(ITEM_KEY))
                    trackMeta = (Track.MetaData)getArguments().getSerializable(ITEM_KEY);
            track = trackMeta == null ? new Track() : trackMeta.getTrack();
        }
    }

    @Override
    public boolean processBackPressed() {
        if(getChildFragmentManager().getBackStackEntryCount() != 0){
            getChildFragmentManager().popBackStack();
            return false;
        }
        return true;
    }

    @Override
    public void OnDataChange(Track track) {
        if(!track.isSetup() || (track.canArtistRecord(MainActivity.user.uid) && !track.getFreeMode()) || (track.isFinished() && track.isOwner()))
            publishFAB.show(true);
        else
            publishFAB.hide(true);

        publishFAB.setImageResource(track.isSetup() && !track.isFinished() ? R.drawable.ic_done_white_24dp : R.drawable.ic_publish_white_24dp);

        boolean canPublish = !track.isSetup() || track.canPublish() || (track.isFinished() && track.isOwner());
        publishFAB.setColorNormalResId(canPublish ? android.R.color.holo_green_light : android.R.color.holo_red_light);
        publishFAB.setColorPressedResId(canPublish ? android.R.color.holo_green_dark : android.R.color.holo_red_dark);
    }

    //region unneededCallbacks
    @Override
    public void OnLoad(Track track) {
    }

    @Override
    public void OnAudioAdd(int pos) {
    }

    @Override
    public void OnAudioRemoved(int pos) {
    }

    @Override
    public void OnAudioChanged(int pos) {

    }
    //endregion

    @Override
    public void OnDelete() {
        getFragmentManager().popBackStack();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final EditText title = MainActivity.context.title;
        title.setText((track != null) ? track.getTitle() : "Vocal");
        title.setFocusable(true);
        title.setFocusableInTouchMode(true);
        title.setBackground(null);
        title.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER){
                    UIUtil.hideKeyboard(getContext(), title);
                    return true;
                }
                return false;
            }
        });

        KeyboardVisibilityEvent.setEventListener(getActivity(), new KeyboardVisibilityEventListener() {
            @Override
            public void onVisibilityChanged(boolean isOpen) {
                title.setCursorVisible(isOpen);
            }
        });

        title.addTextChangedListener(titleWatcher);

        MainActivity.context.editButton.setVisibility((track != null) ? View.VISIBLE : View.GONE);
        MainActivity.context.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIUtil.showKeyboard(getContext(), MainActivity.context.title);
            }
        });
    }

    TextWatcher titleWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            track.setTitle(s.toString());
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.track_pager_fragment, container, false);

        publishFAB = (FloatingActionButton)fragView.findViewById(R.id.FABdone);
        publishFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!track.isSetup() || track.isFinished()){
                    if(getChildFragmentManager().findFragmentByTag(ARTIST_FRAGMENT_TAG) == null){
                        ArtistsFragment artistFragment = ArtistsFragment.newInstance();
                        artistFragment.setTrack(track);
                        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                        transaction.add(R.id.trackFragment, artistFragment, ARTIST_FRAGMENT_TAG);
                        transaction.addToBackStack(null);
                        transaction.commit();
                    } else{
                        AlertDialog alertDialog = new AlertDialog.Builder(getContext(), R.style.ThemedAlertDialog).create();
                        alertDialog.setTitle("Publish");
                        alertDialog.setMessage("Are you sure you want to publish? People will be able to start recording from now and the settings will be locked.");
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "PUBLISH", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                track.finalizeSetup();
                                getChildFragmentManager().popBackStack();
                            }
                        });
                        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                        alertDialog.show();
                    }
                }else{
                    if(!track.canArtistRecord(MainActivity.user.uid)){
                        Log.e(TAG, "onClick: artist cannot currently record should not be reached any more");
                        Toast editorToast = Toast.makeText(MainActivity.context, "You cannot publish because it is not your turn.", Toast.LENGTH_LONG);
                        editorToast.show();
                    } else if(!track.enoughAudiosForPublish()){
                        Toast editorToast = Toast.makeText(MainActivity.context, "You cannot publish because there are not enough tracks. Complete your track before publishing.", Toast.LENGTH_LONG);
                        editorToast.show();
                    }else {
                        track.publish();
                    }
                }
            }
        });
        publishFAB.hide(false);

        RecorderFragment recorderFragment;
        if(savedInstanceState == null){
            recorderFragment = RecorderFragment.newInstance(trackMeta);
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.add(R.id.trackFragment, recorderFragment, RECORDER_FRAGMENT_TAG);
            transaction.commit();
            track.addOnTrackChangeListener(this);
            recorderFragment.setTrack(track);
        } else
            recorderFragment = (RecorderFragment)getChildFragmentManager().findFragmentByTag(RECORDER_FRAGMENT_TAG);

        return fragView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(track != null)
            track.cleanup();

        MainActivity.context.editButton.setVisibility(View.GONE);
        EditText title = MainActivity.context.title;
        title.removeTextChangedListener(titleWatcher);
        title.setText("Vocal");
        title.setFocusable(false);
        title.setBackground(ContextCompat.getDrawable(getContext(), android.R.color.transparent));
    }
}
