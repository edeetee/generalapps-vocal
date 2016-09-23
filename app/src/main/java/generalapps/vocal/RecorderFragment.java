package generalapps.vocal;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.File;

import generalapps.vocal.effects.Effect;
import generalapps.vocal.effects.EffectAdapter;


public class RecorderFragment extends Fragment implements
        BackOverrideFragment,
        RecorderAdapter.OnMusicAdapterChangeListener,
        Recorder.OnRecorderStateChangeListener,
        OnEffectCategoryChangeListener,
        EffectAdapter.OnEffectSelectedListener{

    public static final String ITEM_KEY = "track_key";

    RecorderAdapter adapter;
    Context context;
    RecorderCircle recordProgress;
    RelativeLayout rootLayout;
    RecyclerView recycler;
    ColorView blackTint;
    Track.MetaData trackMeta;

    Handler handler = new Handler();

    public interface TrackInstantiatedListener{
        void TrackInstantiated(Track track);
    }
    TrackInstantiatedListener mCallback;

    public static RecorderFragment newInstance(Track.MetaData meta){
        RecorderFragment fragment = new RecorderFragment();

        Bundle args = new Bundle();
        args.putSerializable(ITEM_KEY, meta);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        MainActivity.recorder.setOnRecorderStateChangeListener(this);

        mCallback = (TrackInstantiatedListener)getParentFragment();
    }

    @Override
    public void OnEffectSelected(Effect effect) {
        effectSelection = null;
        blackTint.setVisibility(View.INVISIBLE);
    }

    RecorderAdapter.AudioHolder effectSelection;

    @Override
    public void OnEffectCategoryChanging(RecorderAdapter.AudioHolder holder) {
        if(effectSelection == null){
            effectSelection = holder;
            blackTint.setVisibility(View.VISIBLE);
            holder.setSpecialFloat(rootLayout);
        }
    }

    @Override
    public boolean processBackPressed() {
        if(effectSelection == null)
            return true;

        effectSelection.OnEffectSelected(Effect.none);
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState == null){
            savedInstanceState = getArguments();
        }

        if(savedInstanceState != null)
            if(savedInstanceState.containsKey(ITEM_KEY))
                trackMeta = (Track.MetaData) savedInstanceState.getSerializable(ITEM_KEY);

        HowTo.Basic("HowTo: Record",
                "To record, hold down the recording circle at the bottom. Click to play. If you are already playing, holding to record will wait till the end of the bars and count you in.",
                MainActivity.context);
    }

    @Override
    public void onPause() {
        super.onPause();

        adapter.stop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.record_fragment, container, false);

        adapter = new RecorderAdapter(context, this);

        mCallback.TrackInstantiated((trackMeta == null) ? new Track(adapter) : new Track(trackMeta, adapter));

        recycler = (RecyclerView)fragView.findViewById(R.id.mainRecycler);
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(context));

        rootLayout = (RelativeLayout)fragView.findViewById(R.id.rootLayout);

        blackTint = (ColorView)fragView.findViewById(R.id.blackTint);
        blackTint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(effectSelection != null)
                    effectSelection.OnEffectSelected(Effect.none);
            }
        });

        recordProgress = (RecorderCircle)fragView.findViewById(R.id.recordProgress);
        //recordProgress.setMax(4);
        recordProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!adapter.playing){
                    adapter.play(recordProgress);
                }
                else{
                    adapter.stop();
                }
            }
        });
        //recording
        recordProgress.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //only if not first recording. First recording doesn't use longClick
                Recorder.State state = MainActivity.recorder.getState();
                if(adapter.getGroupSize() != 0 && state == Recorder.State.NONE){
                    //if current editor
                    if(adapter.group.numEditors() <= adapter.getGroupSize() || adapter.group.getEditor(adapter.getGroupSize()).uid.equals(MainActivity.user.uid))
                        if(!adapter.playing)
                            MainActivity.recorder.record(RecorderFragment.this);
                        else
                            adapter.recordAtEnd();
                    else{
                        Toast editorToast = Toast.makeText(context, "You cannot record because you are not the editor for this recording", Toast.LENGTH_LONG);
                        editorToast.show();
                    }
                }
                return true;
            }
        });
        //stopping recording
        recordProgress.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Recorder.State state = MainActivity.recorder.getState();
                int action = event.getAction();
                //recording let go
                if((state == Recorder.State.RECORDING || state == Recorder.State.PREPARED)
                        && action == MotionEvent.ACTION_UP){
                    MainActivity.recorder.stop();
                    adapter.play(recordProgress);
                }
                //if first recording
                if(state == Recorder.State.NONE && adapter.getGroupSize() == 0 && action == MotionEvent.ACTION_DOWN){
                    if(adapter.group.numEditors() <= adapter.getGroupSize() || adapter.group.getEditor(adapter.getGroupSize()).uid.equals(MainActivity.user.uid))
                        MainActivity.recorder.record(RecorderFragment.this);
                    else{
                        Toast editorToast = Toast.makeText(context, "You cannot record because you are not the editor for this recording", Toast.LENGTH_LONG);
                        editorToast.show();
                    }

                }
                return false;
            }
        });
        recordProgress.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                adapter.setBottomSpecialMargin(recordProgress.getHeight());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    recordProgress.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    recordProgress.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });

        return fragView;
    }



    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(ITEM_KEY, trackMeta);
    }


    @Override
    public void Play() {
        recordProgress.setInnerBottomText("Stop");
    }

    @Override
    public void Stop() {
        recordProgress.setInnerBottomText("Play/Record");
    }

    @Override
    public void OnRecorderStateChange(Recorder.State state) {
        if(state == Recorder.State.ENDING){
            recordProgress.removeCallbacks(MainActivity.recorder.beatInRunnable);
            recordProgress.removeCallbacks(MainActivity.recorder.countDownRunnable);

            recordProgress.setDoHighText(false);
            recordProgress.stopLoop();
            recordProgress.setText("");
        }
    }
}