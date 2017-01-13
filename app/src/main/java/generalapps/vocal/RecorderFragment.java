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

import java.util.List;

import generalapps.vocal.effects.Effect;
import generalapps.vocal.effects.EffectAdapter;

import generalapps.vocal.HowToOverlay.HowToInfo;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_UP;
import static java.util.Arrays.asList;


public class RecorderFragment extends Fragment implements
        RecorderAdapter.OnMusicAdapterChangeListener,
        Recorder.OnRecorderStateChangeListener{

    RecorderAdapter adapter;
    Context context;
    RecorderCircle recordProgress;
    RelativeLayout rootLayout;
    RecyclerView recycler;
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

    public void setTrack(Track track){
        mTrack = track;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.record_fragment, container, false);

        if(savedInstanceState == null){
            adapter = new RecorderAdapter(context, this, (TrackFragment)getParentFragment());
            mTrack.addOnTrackChangeListener(adapter);
        }

        recycler = (RecyclerView)fragView.findViewById(R.id.mainRecycler);
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(context));

        rootLayout = (RelativeLayout)fragView.findViewById(R.id.rootLayout);

        recordProgress = (RecorderCircle)fragView.findViewById(R.id.recordProgress);
        adapter.setRecordProgress(recordProgress);
        //recordProgress.setMax(4);
        recordProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(adapter.group.size() == 0)
                    return;
                if(!adapter.playing){
                    adapter.play();
                }else{
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
                        recycler.smoothScrollBy(0, 200);
                    }
                    return true;
                }
                return false;
            }
        });
        //stopping recording
        recordProgress.setOnTouchListener(new View.OnTouchListener() {
            boolean tryLongPress = false;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Recorder.State state = MainActivity.recorder.getState();
                int action = event.getAction();
                //recording let go
                if((state == Recorder.State.RECORDING || state == Recorder.State.PREPARED)
                        && (action == ACTION_UP || action == ACTION_CANCEL)){
                    if(MainActivity.recorder.stop())
                        adapter.play();
                    return true;
                }
                //if first recording
                if(state == Recorder.State.NONE && action == MotionEvent.ACTION_DOWN) {
                    if(adapter.getGroupSize() == 0) {
                        if (adapter.group.canRecord(context))
                            MainActivity.recorder.record(adapter.group);
                    }else{
                        recordProgress.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(tryLongPress){
                                    tryLongPress = false;
                                    recordProgress.performLongClick();
                                }
                            }
                        }, 150);
                        tryLongPress = true;
                    }
                    return true;
                }
                if(tryLongPress){
                    switch(action){
                        case ACTION_UP:
                            recordProgress.performClick();
                        case ACTION_CANCEL:
                            tryLongPress = false;
                            return true;
                    }
                }
                return false;
            }
        });
        recordProgress.setLongClickable(false);
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
            HowToOverlay.showHelpIfUnseen(HowToOverlay.HowToInfo.RECORDER_CIRCLE, recordProgress);

        return fragView;
    }

    @Override
    public void Play() {
        recordProgress.setPlaying(true);
    }

    @Override
    public void Stop() {
        recordProgress.setPlaying(false);
    }

    @Override
    public void OnRecorderStateChange(Recorder.State state) {
        if(state == Recorder.State.ENDING){
            if(adapter.recordAtEnd)
                adapter.stopRecordAtEnd();
        }else if (state == Recorder.State.NONE) {
            recordProgress.removeCallbacks(MainActivity.recorder.beatInRunnable);
            recordProgress.stopLoop();
            recordProgress.hideBeat();
        }
        if (state == Recorder.State.NONE && mTrack != null && mTrack.first != null){
            RecorderAdapter.AudioHolder holder = mTrack.first.holder;
            View adjustView = adapter.adjust != null ? adapter.adjust.adjustLayout : null;
            HowToOverlay.doHelpList(asList(HowToInfo.PLAY_CIRCLE, HowToInfo.DELETE, HowToInfo.ADJUST, HowToInfo.LEFT_ADJUST, HowToInfo.BAR_TEMPLATE, HowToInfo.EFFECTS, HowToInfo.MUTE, HowToInfo.ADD_MORE, HowToInfo.FIRST_SETUP),
                asList(recordProgress, holder.barTemplate, adjustView, holder.lowestParent, holder.barTemplate, holder.effectCategorySelector, holder.barTemplate, recordProgress, ((TrackFragment) getParentFragment()).publishFAB),
                    new HowToOverlay.MiddleCallback() {
                        @Override
                        public <T extends View> int done(List<HowToInfo> infos, List<T> helpingView, int index) {
                            //if last index was a record button make sure is still playing
                            if(0 < index && helpingView.get(index-1) == recordProgress)
                                if(!adapter.playing)
                                    adapter.play();

                            //if adjust is null
                            if(infos.get(index) == HowToInfo.ADJUST && helpingView.get(index) == null)
                                return index+1;
                            //if there are no recordings left, reset and do record again
                            if(mTrack.size() == 0){
                                HowToOverlay.showHelp(HowToOverlay.HowToInfo.RECORDER_CIRCLE, recordProgress);
                                return infos.size();
                            }
                            return index;
                        }
                    });
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapter.cleanup();
    }
}