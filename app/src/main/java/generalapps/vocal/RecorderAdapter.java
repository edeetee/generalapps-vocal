package generalapps.vocal;

import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import generalapps.vocal.effects.EffectCategory;

/**
 * Created by edeetee on 13/04/2016.
 */
public class RecorderAdapter extends BaseAdapter implements Track.OnTrackChangeListener {

    Track group;
    boolean playing = false;
    int beats = 0;
    Handler handler = new Handler();

    private boolean recordAtEnd;
    private boolean stopAtEnd;

    interface OnMusicAdapterChangeListener{
        void Play();
        void Stop();
        void ItemsChanged(int count);
    }
    OnMusicAdapterChangeListener mCallback;
    EffectCategoryPagerAdapter.OnEffectCategorySelectedListener mEffectCategoryListener;

    @Override
    public void OnChange() {
        MainActivity.context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void OnDelete() {
        if(playing)
            stop();
        notifyDataSetInvalidated();
        group = null;
    }

    View.OnClickListener infoClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = Utils.getInnerViewPosition(v);
            Audio audio = group.get(pos);
            audio.setEnabled(!audio.enabled);

            WaveView wave = (WaveView)v.findViewById(R.id.waveform);
            wave.postInvalidate();
        }
    };

    Button.OnClickListener deleteClick = new Button.OnClickListener(){
        @Override
        public void onClick(View v){
            int pos = Utils.getInnerViewPosition(v);
            if(pos == 0){
                group.delete();
                group = null;
                MainActivity.context.fragManager.popBackStack();
            } else
                group.remove(pos);
        }
    };

    View.OnClickListener barsClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = Utils.getInnerViewPosition(v);
            Audio audio = group.get(pos);
            audio.toggleRoundBarToNext();
        }
    };

    long startTime;

    public float getProgress(){
        return (float)(System.currentTimeMillis()-startTime)/group.msMaxPeriod() % 1;
    }

    public <T extends OnMusicAdapterChangeListener & EffectCategoryPagerAdapter.OnEffectCategorySelectedListener> RecorderAdapter(T callback){
        mCallback = callback;
        mEffectCategoryListener = callback;
    }

    public void play(RecorderCircle recordProgress) {
        if (playing || getCount() == 0)
            return;


        if(mCallback != null)
            mCallback.Play();

        playing = true;
        startTime = System.currentTimeMillis();

        for (int i = 0; i < getCount(); i++) {
            Audio audio = group.get(i);
            audio.play();
            final WaveView waveForm = (WaveView)audio.view.findViewById(R.id.waveform);
            waveForm.post(new Runnable() {
                @Override
                public void run() {
                    waveForm.postInvalidate();
                }
            });
        }

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
        if(getCount() != 0){
            //update starttime to reset to start when editing first bar length
            startTime = System.currentTimeMillis()-group.msBeatPeriod()*beats;
            for(Audio audio : group){
                if(audio.canPlay()){
                    if(beats % (audio.bars*Rhythm.bpb) == 0){
                        audio.restart();
                    }
                }
            }
            MainActivity.context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
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
    public int getCount(){
        return group == null ? 0 : group.size();
    }

    @Override
    public Object getItem(int position) {
        return group.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        if(mCallback != null)
            mCallback.ItemsChanged(getCount());
    }

    @Override
    public void notifyDataSetInvalidated() {
        super.notifyDataSetInvalidated();
        stop();
    }

    public void adjustGroup(int adjustment){
        group.changeMsBarPeriodMod(adjustment);
        WaveView wave = (WaveView)group.first.view.findViewById(R.id.waveform);
        wave.updateWave();
        wave.postInvalidate();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = MainActivity.context.getLayoutInflater().inflate(R.layout.audio_list_view, parent, false);
        }

        Audio audio = group.get(position);
        audio.view = convertView;

        audio.setName();

        FrameLayout info = (FrameLayout) convertView.findViewById(R.id.waveAndName);
        info.setOnClickListener(infoClick);

        Button delete = (Button)convertView.findViewById(R.id.delete);
        if(position == 0){
            delete.setText("Delete (All)");
        }else{
            delete.setText("Delete");
        }
        delete.setOnClickListener(deleteClick);

        TextView bars = (TextView)convertView.findViewById(R.id.bars);
        bars.setOnClickListener(barsClick);

        ViewPager effectCategorySelector = (ViewPager)convertView.findViewById(R.id.effectCategoryPager);
        if(effectCategorySelector.getAdapter() == null)
            effectCategorySelector.setAdapter(new EffectCategoryPagerAdapter(convertView.getContext(), mEffectCategoryListener));

        if(audio.waveValues != null){
            WaveView waveView = (WaveView)convertView.findViewById(R.id.waveform);
            waveView.setAudio(audio);
            waveView.setAdapter(this);
            waveView.invalidate();
        }

        audio.setBars();

        return convertView;
    }
}
