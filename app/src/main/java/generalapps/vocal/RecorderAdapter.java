package generalapps.vocal;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.Space;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import generalapps.vocal.effects.Effect;
import generalapps.vocal.effects.EffectAdapter;
import generalapps.vocal.effects.EffectCategory;
import generalapps.vocal.effects.EffectCategoryAdapter;
import generalapps.vocal.templates.BarTemplate;
import generalapps.vocal.templates.BarTemplatePagerAdapter;
import generalapps.vocal.templates.GroupTemplateAdapter;

/**
 * Created by edeetee on 13/04/2016.
 */
interface OnEffectCategoryChangeListener {
    void OnEffectCategoryChanging(RecorderAdapter.AudioHolder holder);
}


public class RecorderAdapter extends RecyclerView.Adapter<RecorderAdapter.AudioHolder> implements
        Track.OnTrackChangeListener{

    boolean playing = false;
    int beats = 0;
    Handler handler = new Handler();
    Track group;
    Context mContext;

    private boolean recordAtEnd;
    private boolean stopAtEnd;

    OnEffectCategoryChangeListener mEffectCategoryListener;
    EffectAdapter.OnEffectSelectedListener mEffectListener;
    RecorderFragment mFragment;

    interface OnMusicAdapterChangeListener{
        void Play();
        void Stop();
        void ItemsChanged(int count);
    }
    OnMusicAdapterChangeListener mCallback;

    @Override
    public void OnAdd(final int pos) {
        mFragment.handler.post(new Runnable() {
            @Override
            public void run() {
                notifyItemInserted(pos);
            }
        });
        mCallback.ItemsChanged(getItemCount());
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
        mCallback.ItemsChanged(getItemCount());
    }

    public void OnDeleteItem(int pos) {
        if(pos == 0){
            group.delete();
            group = null;
        } else
            group.remove(pos);
        notifyItemRemoved(pos);
    }

    @Override
    public void OnDelete() {
        if(playing)
            stop();
        group = null;
    }

    long startTime;

    public float getProgress(){
        return (float)(System.currentTimeMillis()-startTime)/group.msMaxPeriod() % 1;
    }

    public RecorderAdapter(Context context, RecorderFragment callback){
        mContext = context;
        mCallback = callback;
        mEffectCategoryListener = callback;
        mEffectListener = callback;
        mFragment = callback;
    }

    public void play(RecorderCircle recordProgress) {
        if (playing || getItemCount() == 0)
            return;

        if(mCallback != null)
            mCallback.Play();

        playing = true;
        startTime = System.currentTimeMillis();

        for (int i = 0; i < getItemCount(); i++) {
            Audio audio = group.get(i);
            audio.play();
        }
        notifyDataSetChanged();

        beats = 0;

        playRunnable = new PlayRunnable();
        playRunnable.recordProgress = recordProgress;
        handler.postDelayed(playRunnable, group.msBeatPeriod());
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
            beats = (beats+1)%Rhythm.maxBeats();
            updateAudios();
            if(recordAtEnd){
                //if was too close to end
                if(beats == Rhythm.bpb*(Rhythm.maxBars-1)){
                    stopAtEnd = true;
                    MainActivity.recorder.startRecordingBeatIn(recordProgress);
                } else if(beats == 0 && stopAtEnd){
                    stopAtEnd = false;
                    recordAtEnd = false;
                    stop();
                }
            }
        }
    };

    public void updateAudios(){
        if(getItemCount() != 0){
            //update starttime to reset to start when editing first bar length
            startTime = System.currentTimeMillis()-group.msBeatPeriod()*beats;
            for(Audio audio : group){
                if(audio.canPlay()){
                    if(audio.barTemplate.shouldPlay(beats)){
                        audio.restart();
                    }
                }
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
            if(audio.canPlay())
                audio.stop();
        }
    }

    public void recordAtEnd(){
        MainActivity.recorder.prepareRecord(this);
        recordAtEnd = true;
    }

    @Override
    public int getItemCount(){
        return group == null ? 0 : group.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void adjustGroup(int adjustment){
        group.changeMsBarPeriodMod(adjustment);
        //TODO reimplement wave length change updater
//        WaveView wave = group.first.holder.waveView;
//        wave.updateWave();
//        wave.postInvalidate();
    }

    @Override
    public AudioHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup view = (ViewGroup)MainActivity.context.getLayoutInflater().inflate(R.layout.audio_list_view, parent, false);
        return new AudioHolder(view);
    }

    @Override
    public void onBindViewHolder(AudioHolder holder, int pos){
        holder.bindAudio(group.get(pos));
    }

    public class AudioHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener, View.OnClickListener, EffectAdapter.OnEffectSelectedListener{

        SnappyRecyclerView effectCategorySelector;
        EffectCategoryAdapter effectCategoryAdapter;
        SnappyRecyclerView groupTemplate;
        ViewPager barTemplate;
        BarTemplatePagerAdapter barTemplateAdapter;
        public Audio audio;
        ViewGroup lowestParent;
        Space space;

        public AudioHolder(ViewGroup root) {
            super(root);

            lowestParent = (ViewGroup)itemView.findViewById(R.id.lowestParent);

            groupTemplate = (SnappyRecyclerView)itemView.findViewById(R.id.groupTemplate);
            groupTemplate.setAdapter(new GroupTemplateAdapter(mContext));
            groupTemplate.setLayoutManager(new SnappyLinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));

            effectCategorySelector = (SnappyRecyclerView)itemView.findViewById(R.id.effectCategory);
            effectCategoryAdapter = new EffectCategoryAdapter(itemView.getContext(), this);
            effectCategorySelector.setAdapter(effectCategoryAdapter);
            effectCategorySelector.setLayoutManager(new SnappyLinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
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
            effectCategorySelector.scrollToPosition(EffectCategoryAdapter.MIDDLE);

            barTemplate = (ViewPager)itemView.findViewById(R.id.barTemplate);
        }

        public void bindAudio(Audio audio){
            //remove old audio binding
            if(audio != null && audio.holder == this)
                audio.holder = null;

            this.audio = audio;
            barTemplateAdapter = new BarTemplatePagerAdapter(RecorderAdapter.this, this);
            barTemplate.setAdapter(barTemplateAdapter);
            barTemplate.setCurrentItem(BarTemplate.list.indexOf(audio.barTemplate));
            effectCategorySelector.scrollToPosition(EffectCategory.list.indexOf(audio.effect.category));
            audio.holder = this;
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
            OnDeleteItem(getAdapterPosition());
            return false;
        }
    }
}
