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

import generalapps.vocal.effects.Effect;
import generalapps.vocal.effects.EffectAdapter;


public class RecorderFragment extends Fragment implements
        BackOverrideFragment,
        RecorderAdapter.OnMusicAdapterChangeListener,
        Recorder.OnRecorderStateChangeListener,
        OnEffectCategoryChangeListener,
        EffectAdapter.OnEffectSelectedListener{

    RecorderAdapter adapter;
    Context context;
    RecorderCircle recordProgress;
    RelativeLayout rootLayout;
    RecyclerView recycler;
    ColorView blackTint;
    Track mTrack;

    Handler handler = new Handler();

    public static RecorderFragment newInstance(Track.MetaData meta){
        RecorderFragment fragment = new RecorderFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        MainActivity.recorder.setOnRecorderStateChangeListener(this);
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
            holder.setEffectHover(rootLayout);
        }
    }

    @Override
    public boolean processBackPressed() {
        if(effectSelection == null)
            return true;

        effectSelection.OnEffectSelected(Effect.none);
        return false;
    }

    public void setTrack(Track track){
        mTrack = track;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onPause() {
        super.onPause();

        //adapter.stop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.record_fragment, container, false);

        if(savedInstanceState == null){
            adapter = new RecorderAdapter(context, this);
            mTrack.addOnTrackChangeListener(adapter);
        }

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
                    if(adapter.group.canRecord(context)) {
                        if(!adapter.playing)
                            MainActivity.recorder.record(adapter.group);
                        else
                            adapter.recordAtEnd();
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
                    int preSize = adapter.group.size();
                    MainActivity.recorder.stop();
                    //only play if sucessfully recorded
                    if(adapter.group.size() == preSize)
                        adapter.play(recordProgress);
                }
                //if first recording
                if(state == Recorder.State.NONE && adapter.getGroupSize() == 0 && action == MotionEvent.ACTION_DOWN) {
                    if (adapter.group.canRecord(context))
                        MainActivity.recorder.record(adapter.group);

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

        MainActivity.recorder.setRecordProgress(recordProgress);

        if(savedInstanceState == null)
            ((MainActivity)getActivity()).howToOverlayLayout.tryHelpingView(HowToOverlayLayout.HowToInfo.RECORDER_CIRCLE, recordProgress);

        return fragView;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapter.cleanup();
    }
}