package generalapps.vocal;

import android.app.ListFragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.io.File;

import generalapps.vocal.effects.Effect;
import generalapps.vocal.effects.EffectCategory;


public class RecorderFragment extends ListFragment implements
        RecorderAdapter.OnMusicAdapterChangeListener,
        Recorder.OnRecorderStateChangeListener,
        EffectCategoryPagerAdapter.OnEffectCategorySelectedListener,
        EffectAdapter.OnEffectSelectedListener{

    public static final String FILE_KEY = "track_key";

    RecorderAdapter adapter;
    Context context;
    RecorderCircle recordProgress;
    LinearLayout adjustLayout;
    ListView effectPager;
    RelativeLayout rootLayout;

    static WaveView testThis;

    Handler handler = new Handler();

    Track track;

    public static RecorderFragment newInstance(File file){
        RecorderFragment fragment = new RecorderFragment();

        Bundle args = new Bundle();
        args.putString(FILE_KEY, file.getAbsolutePath());
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        MainActivity.recorder.setOnRecorderStateChangeListener(this);
    }

    @Override
    public void OnEffectCategorySelected(View item, EffectCategory category) {
        if(category.hasChildren()){
            effectPager = new SnappingListView(context);
            effectPager.setAdapter(new EffectAdapter(item.getContext(), this, category));
            effectPager.setSelection(EffectAdapter.HALF_MAX_VALUE);

            final Rect alignRect = new Rect();
            final Rect rootRect = new Rect();
            item.getGlobalVisibleRect(alignRect);
            rootLayout.getGlobalVisibleRect(rootRect);

            int widthDP = 35;
            int width = Math.round(widthDP*Utils.getDpToPxMod(context));
            int height = width*5;
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
            params.leftMargin = alignRect.left - rootRect.left;
            params.topMargin = alignRect.top - rootRect.top - params.height/2;
            rootLayout.addView(effectPager, params);

            rootLayout.findViewById(R.id.blackTint).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void OnEffectSelected(View item, Effect category) {
        rootLayout.removeView(effectPager);
        rootLayout.findViewById(R.id.blackTint).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        File file = null;

        if(savedInstanceState == null)
            savedInstanceState = getArguments();

        if(savedInstanceState != null)
            if(savedInstanceState.containsKey(FILE_KEY))
                file = new File(savedInstanceState.getString(FILE_KEY));

        adapter = new RecorderAdapter(this);
        setListAdapter(adapter);
        if(file != null){
            track = Track.load(file, adapter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        adapter.stop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.record_fragment, container, false);

        rootLayout = (RelativeLayout)fragView.findViewById(R.id.rootLayout);

        final Runnable adjustLeftRunnable = new Runnable() {
            @Override
            public void run() {
                adapter.adjustGroup(20);
                handler.postDelayed(this, 50);
            }
        };
        final Runnable adjustRightRunnable = new Runnable() {
            @Override
            public void run() {
                adapter.adjustGroup(-20);
                handler.postDelayed(this, 50);
            }
        };

        View.OnTouchListener adjustTouch = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Runnable adjustRunnable = (view.getId() == R.id.leftAdjust) ? adjustLeftRunnable : adjustRightRunnable;
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    handler.post(adjustRunnable);
                    return true;
                } else if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                    handler.removeCallbacks(adjustRunnable);
                    return true;
                }

                return false;
            }
        };
        ImageView leftAdjust = (ImageView)fragView.findViewById(R.id.leftAdjust);
        leftAdjust.setOnTouchListener(adjustTouch);
        leftAdjust.setColorFilter(Color.BLACK);
        ImageView rightAdjust = (ImageView)fragView.findViewById(R.id.rightAdjust);
        rightAdjust.setOnTouchListener(adjustTouch);
        rightAdjust.setColorFilter(Color.BLACK);

        adjustLayout = (LinearLayout)fragView.findViewById(R.id.adjustLayout);
        setAdjustLayoutVisible(adapter.getCount() == 1);

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
                if(adapter.getCount() != 0 && state == Recorder.State.NONE){
                    if(!adapter.playing)
                        MainActivity.recorder.record(RecorderFragment.this);
                    else
                        adapter.recordAtEnd();
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
                if(state == Recorder.State.NONE && adapter.getCount() == 0 && action == MotionEvent.ACTION_DOWN){
                    MainActivity.recorder.record(RecorderFragment.this);
                }
                return false;
            }
        });

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

    public void setAdjustLayoutVisible(boolean visible){
        adjustLayout.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void ItemsChanged(int count) {
        setAdjustLayoutVisible(count == 1);
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