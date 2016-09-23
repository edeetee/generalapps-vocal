package generalapps.vocal;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.Space;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.concurrent.Callable;

import generalapps.vocal.effects.Effect;
import generalapps.vocal.effects.EffectAdapter;
import generalapps.vocal.effects.EffectCategoryAdapter;
import generalapps.vocal.templates.BarTemplate;
import generalapps.vocal.templates.BarTemplatePagerAdapter;
import generalapps.vocal.templates.GroupTemplate;
import generalapps.vocal.templates.GroupTemplateAdapter;

/**
 * Created by edeetee on 13/04/2016.
 */
interface OnEffectCategoryChangeListener {
    void OnEffectCategoryChanging(RecorderAdapter.AudioHolder holder);
}


public class RecorderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements
        Track.OnTrackChangeListener{

    public boolean playing = false;
    public int beats = 0;
    Handler handler = new Handler();
    Track group;
    Context mContext;
    int bottomSpecialMargin;

    private boolean recordAtEnd;
    private boolean stopAtEnd;

    OnEffectCategoryChangeListener mEffectCategoryListener;
    EffectAdapter.OnEffectSelectedListener mEffectListener;
    RecorderFragment mFragment;

    interface OnMusicAdapterChangeListener{
        void Play();
        void Stop();
    }
    OnMusicAdapterChangeListener mCallback;

    @Override
    public void OnLoad(Track track) {
        group = track;
        notifyDataSetChanged();
    }

    @Override
    public void OnAdd(final int pos) {
        mFragment.handler.post(new Runnable() {
            @Override
            public void run() {
                notifyItemInserted(pos);
            }
        });
    }

    @Override
    public void OnChanged(final int pos) {
        mFragment.handler.post(new Runnable() {
            @Override
            public void run() {
                notifyItemChanged(pos);
            }
        });
    }

    @Override
    public void OnRemoved(final int pos) {
        mFragment.handler.post(new Runnable() {
            @Override
            public void run() {
                notifyItemRemoved(pos);
            }
        });
    }

    @Override
    public void OnDelete() {
        if(playing)
            stop();
        notifyItemRangeRemoved(0, getItemCount());
        group = null;
    }

    long startTime;

    Callable<Float> progressCallable = new Callable<Float>() {
        @Override
        public Float call() throws Exception {
            return (float)(System.currentTimeMillis()-startTime)/group.msMaxPeriod() % 1;
        }
    };

    public RecorderAdapter(Context context, RecorderFragment callback){
        mContext = context;
        mCallback = callback;
        mEffectCategoryListener = callback;
        mEffectListener = callback;
        mFragment = callback;
    }

    public void play(RecorderCircle recordProgress) {
        if (playing || getGroupSize() == 0)
            return;

        if(mCallback != null)
            mCallback.Play();

        playing = true;
        startTime = System.currentTimeMillis();

        for (int i = 0; i < getGroupSize(); i++) {
            Audio audio = group.get(i);
            audio.holder.barTemplateAdapter.setProgressCallback(progressCallable);
            audio.holder.groupTemplateAdapter.updateAllGroupTemplates();
        }

        beats = 0;

        playRunnable = new PlayRunnable();
        playRunnable.recordProgress = recordProgress;
        handler.post(playRunnable);
    }

    PlayRunnable playRunnable;
    class PlayRunnable implements Runnable {
        public RecorderCircle recordProgress;
        @Override
        public void run() {
            if (!playing)
                handler.removeCallbacks(this);
            else
                handler.postDelayed(this, group.msBeatPeriod());
            if(recordAtEnd){
                if(MainActivity.recorder.getState() == Recorder.State.PREPARED){
                    //only does count in from last bar
                    if(beats % Rhythm.maxBeats() == Rhythm.bpb*(Rhythm.maxBars-1)){
                        stopAtEnd = true;
                        MainActivity.recorder.startRecordingBeatIn(recordProgress);
                    //count in has completed
                    } else if(beats % Rhythm.maxBeats() == 0 && stopAtEnd){
                        stopAtEnd = false;
                        recordAtEnd = false;
                        stop();
                        return;
                    }
                } else{
                    stopAtEnd = false;
                    recordAtEnd = false;
                }
            }
            updateAudios();
            beats++;
        }
    };

    public void updateAudios(){
        if(getGroupSize() != 0){
            //update starttime to reset to start when editing first bar length
            startTime = System.currentTimeMillis()-group.msBeatPeriod()*beats;
            for(Audio audio : group){
                audio.beat(beats);
            }
        }
    }

    public void stop(){
        if(!playing)
            return;

        playing = false;
        beats = 0;

        if(mCallback != null)
            mCallback.Stop();

        handler.removeCallbacks(playRunnable);

        for(Audio audio : group){
            audio.stop();
            audio.holder.barTemplateAdapter.stopProgressCallback(progressCallable);
            audio.holder.groupTemplateAdapter.updateAllGroupTemplates();
        }
    }

    public void setBottomSpecialMargin(int margin){
        bottomSpecialMargin = margin;
        if(group != null)
            notifyItemChanged(getGroupSize());
    }

    public void recordAtEnd(){
        MainActivity.recorder.prepareRecord(this);
        recordAtEnd = true;
    }

    @Override
    public int getItemCount(){
        return getGroupSize() == 0 ? 0 : getGroupSize()+1;
    }

    public int getGroupSize(){
        return group == null ? 0 : group.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void onAdjust(int adjustment){
        if(getGroupSize() == 1) {
            group.changeMsBarPeriodMod(adjustment*20);
            group.first.holder.barTemplateAdapter.updateWaves();
        } else {
            Audio last = group.get(getGroupSize()-1);
            last.setMsDelay(last.msDelay - adjustment*20);
            last.holder.barTemplateAdapter.updateWaves();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == AUDIO_TYPE){
            ViewGroup view = (ViewGroup)MainActivity.context.getLayoutInflater().inflate(R.layout.audio_list_view, parent, false);
            return new AudioHolder(view);
        } else{
            ViewGroup view = (ViewGroup)MainActivity.context.getLayoutInflater().inflate(R.layout.adjust_buttons, parent, false);
            return new AdjustHolder(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == getGroupSize() ? ADJUST_TYPE : AUDIO_TYPE;
    }

    static final int AUDIO_TYPE = 0;
    static final int ADJUST_TYPE = 1;

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int pos){
        if(holder.getItemViewType() == AUDIO_TYPE)
            ((AudioHolder)holder).bind();
    }

    class AdjustHolder extends RecyclerView.ViewHolder implements View.OnTouchListener{
        ImageView leftAdjust;
        ImageView rightAdjust;
        Space spacer;
        long pressStartTime;

        public AdjustHolder(View root) {
            super(root);
            leftAdjust = (ImageView)root.findViewById(R.id.leftAdjust);
            rightAdjust = (ImageView)root.findViewById(R.id.rightAdjust);
            spacer = (Space)root.findViewById(R.id.space);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, bottomSpecialMargin));

            leftAdjust.setOnTouchListener(this);
            leftAdjust.setColorFilter(Color.BLACK);
            rightAdjust.setOnTouchListener(this);
            rightAdjust.setColorFilter(Color.BLACK);
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            Runnable adjustRunnable = (view.getId() == R.id.leftAdjust) ? adjustLeftRunnable : adjustRightRunnable;
            if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                pressStartTime = System.currentTimeMillis();
                handler.post(adjustRunnable);
                return true;
            } else if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                handler.removeCallbacks(adjustRunnable);
                return true;
            }
            return false;
        }

        final int accelMs = 1500;
        final int adjustStart = 1;
        final int adjustEnd = 2;
        final int periodStart = 200;
        final int periodEnd = 20;

        final Runnable adjustLeftRunnable = new Runnable() {
            @Override
            public void run() {
                onAdjust(getAdjustMod());
                handler.postDelayed(this, getPeriod());
            }
        };
        final Runnable adjustRightRunnable = new Runnable() {
            @Override
            public void run() {
                onAdjust(-getAdjustMod());
                handler.postDelayed(this, getPeriod());
            }
        };

        public int getAdjustMod(){
            return (int)Math.min(adjustEnd, adjustStart + (adjustEnd-adjustStart)*(System.currentTimeMillis() - pressStartTime)/accelMs);
        }

        public int getPeriod(){
            Log.i("AdjustHolder", "period: " + (periodEnd-periodStart)*(System.currentTimeMillis() - pressStartTime)/accelMs);
            return (int)Math.max(periodEnd, periodStart + (periodEnd-periodStart)*(System.currentTimeMillis() - pressStartTime)/accelMs);
        }
    }

    public class AudioHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener, View.OnClickListener, EffectAdapter.OnEffectSelectedListener{

        SnappyRecyclerView effectCategorySelector;
        EffectCategoryAdapter effectCategoryAdapter;
        SnappyRecyclerView groupTemplate;
        GroupTemplateAdapter groupTemplateAdapter;
        ViewPager barTemplate;
        BarTemplatePagerAdapter barTemplateAdapter;
        CardView card;
        public Audio audio;
        ViewGroup lowestParent;
        Space space;
        InvisibleView noneditableCover;

        public AudioHolder(ViewGroup root) {
            super(root);

            lowestParent = (ViewGroup)itemView.findViewById(R.id.lowestParent);

            groupTemplate = (SnappyRecyclerView)itemView.findViewById(R.id.groupTemplate);
            groupTemplateAdapter = new GroupTemplateAdapter(mContext, RecorderAdapter.this);
            groupTemplate.setAdapter(groupTemplateAdapter);
            groupTemplate.setLayoutManager(new SnappyLinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false, new SnappyLinearLayoutManager.OnPosChangedListener() {
                @Override
                public void onPosChanged(int pos) {
                    audio.setGroupTemplate(GroupTemplate.list.get(pos % GroupTemplate.list.size()));
                }
            }));

            effectCategorySelector = (SnappyRecyclerView)itemView.findViewById(R.id.effectCategory);
            effectCategoryAdapter = new EffectCategoryAdapter(itemView.getContext(), this);
            effectCategorySelector.setAdapter(effectCategoryAdapter);
            effectCategorySelector.setLayoutManager(new SnappyLinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false, null));
            //effectCategorySelector.setOnScroll
            effectCategorySelector.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
                @Override
                public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                    int action = e.getAction();
                    if(action == MotionEvent.ACTION_DOWN)
                        mEffectCategoryListener.OnEffectCategoryChanging(AudioHolder.this);
                    return false;
                }

                @Override
                public void onTouchEvent(RecyclerView rv, MotionEvent e) {

                }

                @Override
                public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

                }
            });

            barTemplate = (ViewPager)itemView.findViewById(R.id.barTemplate);
            card = (CardView)itemView.findViewById(R.id.card_view);
            noneditableCover = (InvisibleView)itemView.findViewById(R.id.blackTint);
        }

        public void bind(){
            //remove old audio binding
            Audio audio = group.get(getAdapterPosition());
            if(audio != null && audio.holder == this)
                audio.holder = null;

            this.audio = audio;
            barTemplateAdapter = new BarTemplatePagerAdapter(RecorderAdapter.this, this);
            barTemplate.setAdapter(barTemplateAdapter);
            barTemplate.setCurrentItem(BarTemplate.list.indexOf(audio.barTemplate));
            effectCategorySelector.scrollToPosition(EffectCategoryAdapter.getMiddleForEffectCategory(audio.effect.category));
            groupTemplate.scrollToPosition(GroupTemplateAdapter.getMiddleForTemplate(audio.groupTemplate));
            audio.holder = this;

            if(audio.group.getEditor(getAdapterPosition()).uid.equals(MainActivity.user.uid)){
                card.setForeground(null);
                noneditableCover.setVisibility(View.INVISIBLE);
            } else {
                ColorDrawable foreground = new ColorDrawable(Color.DKGRAY);
                foreground.setAlpha(150);
                card.setForeground(foreground);
                noneditableCover.setVisibility(View.VISIBLE);
            }
        }

        public void setSpecialFloat(ViewGroup specialFloatGroup){
            if (specialFloatGroup != null) {
                Rect actualItemRect = new Rect();
                Rect specialFloatGroupRect = new Rect();
                specialFloatGroup.getGlobalVisibleRect(specialFloatGroupRect);
                effectCategorySelector.getGlobalVisibleRect(actualItemRect);

                int width = (int)mContext.getResources().getDimension(R.dimen.default_item_height);
                final RelativeLayout.LayoutParams actualItemMargins = new RelativeLayout.LayoutParams(width, width*5);
                actualItemMargins.topMargin = actualItemRect.top - specialFloatGroupRect.top;
                actualItemMargins.leftMargin = actualItemRect.left - specialFloatGroupRect.left;

                space = new Space(mContext);

                //lowestParent.addView(new Space(mContext), effectCategorySelector.getLayoutParams());
                lowestParent.removeView(effectCategorySelector);
                lowestParent.addView(space, new LinearLayout.LayoutParams(width, width));
                specialFloatGroup.addView(effectCategorySelector, actualItemMargins);
                effectCategoryAdapter.setShowEffects(true);
            } else{
                ((ViewGroup)effectCategorySelector.getParent()).removeView(effectCategorySelector);
                lowestParent.removeView(space);
                lowestParent.addView(effectCategorySelector);
            }
        }

        @Override
        public void onClick(View view) {
            audio.setEnabled(!audio.enabled);
        }

        @Override
        public void OnEffectSelected(Effect effect) {
            effectCategoryAdapter.setShowEffects(false);
            effectCategoryAdapter.setActivated(effect != Effect.none);
            if(effect == Effect.none)
                effectCategorySelector.scrollToPosition(EffectCategoryAdapter.MIDDLE);
            audio.setEffect(effect);
            setSpecialFloat(null);
            mEffectListener.OnEffectSelected(effect);
        }

        @Override
        public boolean onLongClick(View view) {
            group.remove(getAdapterPosition());
            return false;
        }
    }
}
