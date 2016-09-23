package generalapps.vocal;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

/**
 * Created by edeetee on 8/09/2016.
 */
public class TrackPagerFragment extends Fragment implements BackOverrideFragment, RecorderFragment.TrackInstantiatedListener {

    public static final String ITEM_KEY = "track_key";

    ViewPager pager;
    Track.MetaData trackMeta;
    Track track;

    ArtistsFragment artistFragment;
    RecorderFragment recorderFragment;

    public static TrackPagerFragment newInstance(Track.MetaData meta) {
        Bundle args = new Bundle();
        args.putSerializable(ITEM_KEY, meta);

        TrackPagerFragment fragment = new TrackPagerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            savedInstanceState = getArguments();
        }

        if (savedInstanceState != null)
            if (savedInstanceState.containsKey(ITEM_KEY))
                trackMeta = (Track.MetaData)savedInstanceState.getSerializable(ITEM_KEY);
    }

    @Override
    public boolean processBackPressed() {
        return true;
    }

    @Override
    public void TrackInstantiated(Track track) {
        artistFragment.setTrack(track);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.track_pager_fragment, container, false);

        pager = (ViewPager)fragView.findViewById(R.id.trackPager);
        pager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager()) {
            @Override
            public android.support.v4.app.Fragment getItem(int position) {
                switch (position){
                    case 0:
                        recorderFragment = RecorderFragment.newInstance(trackMeta);
                        return recorderFragment;
                    case 1:
                        artistFragment = ArtistsFragment.newInstance();
                        if(track != null)
                            artistFragment.setTrack(track);
                        return artistFragment;
                    default:
                        return null;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        });

        return fragView;
    }
}
