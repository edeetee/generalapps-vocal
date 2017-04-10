package generalapps.vocal;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.Space;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import generalapps.vocal.effects.Effect;
import generalapps.vocal.effects.EffectAdapter;
import generalapps.vocal.effects.EffectCategoryAdapter;
import generalapps.vocal.templates.BarTemplate;
import generalapps.vocal.templates.BarTemplatePagerAdapter;

/**
 * Created by edeetee on 13/04/2016.
 */
interface OnEffectCategoryChangeListener {
    /**
     *
     * @param holder audio holder in question
     * @return did it consume the change
     */
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

    boolean recordAtEnd;
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
        if(group != null)
            group.audioMetaRef.child("editors").removeEventListener(editorsListener);

        group = track;
        notifyDataSetChanged();
        group.audioMetaRef.child("editors").orderByChild("position").addChildEventListener(editorsListener);

        lastFreeMode = group.getFreeMode();
        lastShuffle = group.getShuffled();
        lastFinished = group.isFinished();
    }

    private ChildEventListener editorsListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            Track.EditorItem item = dataSnapshot.getValue(Track.EditorItem.class);
//            if(group.size() <= item.position && item.position+getAdjustOffset() < getSpaceHolderPos() && item.position+getAdjustOffset() < getItemCount()-1)
//                notifyItemInserted(item.position + getAdjustOffset());
            if(group.size() <= item.position)
                notifyDataSetChanged();
//            if(item.position+getAdjustOffset() == getSpaceHolderPos())
//                notifyItemInserted(item.position + getAdjustOffset());
            //notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            Track.EditorItem item = dataSnapshot.getValue(Track.EditorItem.class);
            notifyItemChanged(item.position + getAdjustOffset());
            //notifyDataSetChanged();
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            Track.EditorItem item = dataSnapshot.getValue(Track.EditorItem.class);
            if(group.size() <= item.position && item.position+getAdjustOffset() < getSpaceHolderPos())
                notifyItemRemoved(item.position + getAdjustOffset());
            //notifyDataSetChanged();
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            //do nothing, onChildChanged will do it all (will not animate movement)
            //notifyDataSetChanged();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    boolean lastFreeMode;
    boolean lastShuffle;
    boolean lastFinished;
    @Override
    public void OnDataChange(Track track) {
        boolean newFreeMode = group.getFreeMode();
        boolean newShuffle = group.getShuffled();
        boolean newFinished = group.isFinished();
        if(lastFreeMode != newFreeMode){
            notifyDataSetChanged();
            lastFreeMode = newFreeMode;
        }
        if(lastShuffle != newShuffle || lastFinished != newFinished){
            notifyItemRangeChanged(0, getItemCount());
            lastShuffle = newShuffle;
            lastFinished = newFinished;
        }
    }

    //TODO fix item inserted callbacks

    @Override
    public void OnAudioAdd(final int pos) {
        //remove adjust if exists
//        if(pos != 0 && adjust != null && adjust.getAdapterPosition() != RecyclerView.NO_POSITION)
//            notifyItemRemoved(1);
//
//        //remove artist thing if exists
//        if(pos < group.numEditors()-1 && !group.getFreeMode())
//            notifyItemRemoved(pos+getAdjustOffset());
//        notifyItemInserted(pos);
//
//        //add adjust buttons
//        if(pos == 0)
//            notifyItemInserted(1);

        notifyDataSetChanged();
    }

    @Override
    public void OnAudioChanged(final int pos) {
        mFragment.handler.post(new Runnable() {
            @Override
            public void run() {
                notifyItemChanged(pos);
            }
        });
    }

    @Override
    public void OnAudioRemoved(final int pos) {

        //remove adjust buttons
        if(getGroupSize() == 0) {
            stop();
            //notifyItemRemoved(1);
        }
//
//        notifyItemRemoved(pos);
//
//        //add adjust
//        if(pos == 1 && group.size() == 1){
//            notifyItemInserted(1);
//        }
//
//        //re-add editor (only if doing editors and is setup)
//        if(!group.getFreeMode() && group.isSetup() && pos < group.numEditors())
//            notifyItemInserted(group.size()+getAdjustOffset());

        notifyDataSetChanged();
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

    public RecorderAdapter(Context context, RecorderFragment fragment, TrackFragment parentFragment){
        mContext = context;
        mCallback = fragment;
        mEffectCategoryListener = parentFragment;
        mEffectListener = parentFragment;
        mFragment = fragment;
    }

    public void setRecordProgress(RecorderCircle recordProgress){
        this.recordProgress = recordProgress;
    }

    private RecorderCircle recordProgress;

    int layer = -1;

    public void play() {
        if (playing || getGroupSize() == 0)
            return;

        if(mCallback != null)
            mCallback.Play();

        playing = true;
        startTime = System.currentTimeMillis();

        for (int i = 0; i < getGroupSize(); i++) {
            if(layer < i && group.isFinished())
                break;
            Audio audio = group.get(i);
            audio.holder.barTemplateAdapter.setProgressCallback(progressCallable);
        }

        beats = 0;

        playRunnable = new PlayRunnable();
        handler.post(playRunnable);
    }

    PlayRunnable playRunnable;
    class PlayRunnable implements Runnable {
        @Override
        public void run() {
            if (!playing)
                handler.removeCallbacks(this);
            else
                handler.postDelayed(this, group.msBeatPeriod());

            if(recordAtEnd){
                recordProgress.setBeat(beats%Rhythm.bpb+1);
                if(beats % Rhythm.bpb == 0){
                    recordAtEnd = false;
                    stop();
                    MainActivity.recorder.startRecordingBeat();
                    return;
                }
            }

            if(layer < group.size()-1 && beats % Rhythm.maxBeats() == 0){
                layer++;
                group.get(layer).holder.barTemplateAdapter.setProgressCallback(progressCallable);
            }

            startTime = System.currentTimeMillis()-group.msBeatPeriod()*beats;
            for (int i = 0; i < group.size(); i++) {
                group.get(i).beat(beats);

                if(group.isFinished() && i == layer)
                    break;
            }
            beats++;
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
        }
    }

    void cleanup(){
        group.audioMetaRef.child("editors").removeEventListener(editorsListener);
    }

    public void setBottomSpecialMargin(int margin){
        bottomSpecialMargin = margin;
        if(group != null)
            notifyItemChanged(getItemCount()-1);
    }

    public void recordAtEnd(){
        MainActivity.recorder.prepareRecord(group);
        recordAtEnd = true;
        int barLength = group.getMsBarPeriod();
        recordProgress.setBeat(beats%Rhythm.bpb);
        recordProgress.doLoop(barLength, (int)(System.currentTimeMillis() - startTime));
    }

    public void stopRecordAtEnd(){
        recordAtEnd = false;
    }

    @Override
    public int getItemCount(){
        return getSpaceOffset() + ( (group == null || group.getFreeMode()) ? getGroupSize() : getEditorsSize() );
    }

    int getSpaceOffset(){
        return getAdjustOffset() + 1;
    }

    int getAdjustOffset(){
        return getGroupSize() == 1 ? 1 : 0;
    }

    int getEditorsSize(){
        return group == null ? 0 : group.numEditors();
    }

    public int getGroupSize(){
        return group == null ? 0 : group.size();
    }

    @Override
    public long getItemId(int position) {
        return position < getGroupSize() ? group.get(position).name.hashCode() : "adjust".hashCode();
    }

    AdjustHolder adjust;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == AUDIO_TYPE){
            ViewGroup view = (ViewGroup)MainActivity.context.getLayoutInflater().inflate(R.layout.audio_item, parent, false);
            return new AudioHolder(view);
        } else if(viewType == ADJUST_TYPE){
            ViewGroup view = (ViewGroup)MainActivity.context.getLayoutInflater().inflate(R.layout.adjust_buttons, parent, false);
            adjust = new AdjustHolder(view);
            return adjust;
        } else if(viewType == EDITOR_TYPE){
            ViewGroup view = (ViewGroup)MainActivity.context.getLayoutInflater().inflate(R.layout.audio_list_editor_view, parent, false);
            return new EditorHolder(view);
        } else{
            spaceHolder = new SpaceHolder();
            return spaceHolder;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(position < getGroupSize())
            return AUDIO_TYPE;
        else if(getGroupSize() == 1 && position == 1)
            return ADJUST_TYPE;
        else if(position < getItemCount()-1)
            return EDITOR_TYPE;
        else
            return SPACE_TYPE;
    }

    static final int AUDIO_TYPE = 0;
    static final int ADJUST_TYPE = 1;
    static final int EDITOR_TYPE = 2;
    static final int SPACE_TYPE = 3;

    RecyclerView.ViewHolder spaceHolder;
    int getSpaceHolderPos(){
        return spaceHolder == null ? 0 : spaceHolder.getAdapterPosition();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int pos){
        if(holder instanceof Bindable){
            ((Bindable) holder).bind();
        }
    }

    class SpaceHolder extends RecyclerView.ViewHolder implements Bindable{
        public SpaceHolder(){
            super(new Space(mContext));
        }

        @Override
        public void bind() {
            itemView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, bottomSpecialMargin));
        }
    }

    class EditorHolder extends RecyclerView.ViewHolder implements Bindable{
        @BindView(R.id.editorName) TextView editorName;
        ColorStateList colors;

        public EditorHolder(View root){
            super(root);
            ButterKnife.bind(this, root);
            colors = editorName.getTextColors();
        }

        public void bind(){
            final String editorUID = group.getEditor(getAdapterPosition() - getAdjustOffset());
            Track.SpecialEditor specialEditor = Track.SpecialEditor.parse(editorUID);
            if(specialEditor != null){
                Track.SpecialEditor.loadPrint(editorUID, group, new Track.SpecialEditor.PrintCallback() {
                    @Override
                    public void printLoaded(String print) {
                        editorName.setText(print);
                    }
                });
                editorName.setTextColor(Track.SpecialEditor.parseColor(editorUID));
            }else
                MainActivity.database.getReference("users").child(editorUID).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        editorName.setText(dataSnapshot.exists() ? dataSnapshot.getValue(User.class).name : "????");
                        editorName.setTextColor(colors);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
        }
    }

    class AdjustHolder extends RecyclerView.ViewHolder implements AdjustButton.OnAdjustCallback{
        AdjustButton leftAdjust;
        AdjustButton rightAdjust;
        LinearLayout adjustLayout;

        public AdjustHolder(View root) {
            super(root);
            leftAdjust = (AdjustButton)root.findViewById(R.id.leftAdjust);
            rightAdjust = (AdjustButton)root.findViewById(R.id.rightAdjust);
            adjustLayout = (LinearLayout)root.findViewById(R.id.adjustLayout);

            leftAdjust.setOnAdjustCallback(this);
            rightAdjust.setOnAdjustCallback(this);
        }

        @Override
        public void onAdjust(int adjustment) {
            group.changeMsBarPeriodMod(adjustment*20);
            group.first.holder.barTemplateAdapter.updateWaves();
        }
    }

    public class AudioHolder extends RecyclerView.ViewHolder implements Bindable, View.OnLongClickListener, View.OnClickListener, EffectAdapter.OnEffectSelectedListener, AdjustButton.OnAdjustCallback{

        @BindView(R.id.effectCategory) RecyclerView effectCategorySelector;
        EffectCategoryAdapter effectCategoryAdapter;
        @BindView(R.id.barTemplate) ViewPager barTemplate;
        BarTemplatePagerAdapter barTemplateAdapter;
        @BindView(R.id.card_view) CardView card;
        public Audio audio;
        @BindView(R.id.lowestParent) ViewGroup lowestParent;
        @BindView(R.id.effectSpace) View space;
        @BindView(R.id.blackTint) InvisibleView noneditableCover;
        @BindView(R.id.leftAdjust) AdjustButton leftAdjust;
        @BindView(R.id.rightAdjust) AdjustButton rightAdjust;

        public AudioHolder(ViewGroup root) {
            super(root);

            ButterKnife.bind(this, root);

            root.setOnClickListener(this);

            space.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        effectCategorySelector.scrollBy(-(int)mContext.getResources().getDimension(R.dimen.default_item_height)/4, 0);
                        mEffectCategoryListener.OnEffectCategoryChanging(AudioHolder.this);
                    }
                    return effectCategorySelector.onTouchEvent(event);
                }
            });

            effectCategoryAdapter = new EffectCategoryAdapter(itemView.getContext(), this);
            effectCategorySelector.setAdapter(effectCategoryAdapter);
            LinearSnapHelper snapHelper = new LinearSnapHelper(){
                @Override
                public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {
                    return super.findTargetSnapPosition(layoutManager, velocityX/10, velocityY/10);
                }
            };
            snapHelper.attachToRecyclerView(effectCategorySelector);
            effectCategorySelector.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));

            noneditableCover.setOnClickListener(this);
            card.setOnClickListener(this);

            leftAdjust.setOnAdjustCallback(this);
            rightAdjust.setOnAdjustCallback(this);
        }

        public void bind(){
            //remove old audio binding
            Audio audio = group.get(getAdapterPosition());
            if(audio != null && audio.holder == this)
                audio.holder = null;

            this.audio = audio;
            barTemplateAdapter = new BarTemplatePagerAdapter(RecorderAdapter.this, this);
            if(playing && (!group.isFinished() || getAdapterPosition() <= layer))
                barTemplateAdapter.setProgressCallback(progressCallable);
            else
                barTemplateAdapter.stopProgressCallback(progressCallable);

            barTemplate.setAdapter(barTemplateAdapter);
            barTemplate.setCurrentItem(BarTemplate.list.indexOf(audio.barTemplate));
            effectCategorySelector.scrollToPosition(EffectCategoryAdapter.getMiddleForEffectCategory(audio.effect.category));
            audio.holder = this;

            if(audio.group.isEditable(getAdapterPosition())){
                card.setForeground(null);
                noneditableCover.setVisibility(View.INVISIBLE);
            } else {
                ColorDrawable foreground = new ColorDrawable(Color.DKGRAY);
                foreground.setAlpha(100);
                card.setForeground(foreground);
                noneditableCover.setVisibility(View.VISIBLE);
            }
        }

        boolean isHover = false;
        public void setEffectHover(ViewGroup specialFloatGroup){
            isHover = specialFloatGroup != null;
            if (isHover) {
                Rect actualItemRect = new Rect();
                Rect specialFloatGroupRect = new Rect();
                specialFloatGroup.getGlobalVisibleRect(specialFloatGroupRect);
                effectCategorySelector.getGlobalVisibleRect(actualItemRect);

                //margins inside whole screen
                int width = (int)mContext.getResources().getDimension(R.dimen.default_item_height);
                RelativeLayout.LayoutParams specialLayoutMargins = new RelativeLayout.LayoutParams(Math.round(width*1.5f), width*5);
                specialLayoutMargins.topMargin = actualItemRect.top - specialFloatGroupRect.top;
                specialLayoutMargins.leftMargin = actualItemRect.left - specialFloatGroupRect.left - width/4;

                effectCategorySelector.setHorizontalFadingEdgeEnabled(true);
                effectCategorySelector.setFadingEdgeLength(width/4);

                //do the move
                lowestParent.removeView(effectCategorySelector);
                specialFloatGroup.addView(effectCategorySelector, specialLayoutMargins);
                effectCategoryAdapter.setShowEffects(true);
            } else{
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)space.getLayoutParams();
                ((ViewGroup)effectCategorySelector.getParent()).removeView(effectCategorySelector);
                lowestParent.addView(effectCategorySelector, lowestParent.indexOfChild(space), params);
                effectCategorySelector.setHorizontalFadingEdgeEnabled(false);
            }
        }

        @Override
        public void onClick(View view) {
            audio.setEnabled(!audio.enabled);
        }

        @Override
        public void OnEffectSelected(Effect effect) {
            setEffectHover(null);
            audio.setEffect(effect);
            effectCategoryAdapter.setShowEffects(false);
            effectCategoryAdapter.setActivated(effect != Effect.none);
            effectCategorySelector.scrollToPosition(EffectCategoryAdapter.getMiddleForEffectCategory(audio.effect.category));
            mEffectListener.OnEffectSelected(effect);
        }

        @Override
        public void onAdjust(int adjustment) {
            if(audio == null)
                return;
            audio.setMsDelay(audio.msDelay - adjustment*20);
            audio.holder.barTemplateAdapter.updateWaves();
        }

        @Override
        public boolean onLongClick(View view) {
            group.remove(audio.name);
            return false;
        }
    }

    interface Bindable{
        void bind();
    }
}
