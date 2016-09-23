package generalapps.vocal.templates;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import generalapps.vocal.Audio;
import generalapps.vocal.RecorderAdapter;
import generalapps.vocal.WaveView;
import generalapps.vocal.templates.BarTemplate;

/**
 * Created by edeetee on 1/07/2016.
 */

public class BarTemplatePagerAdapter extends PagerAdapter implements View.OnClickListener{
    public static final int HALF_MAX_VALUE = Integer.MAX_VALUE/2;
    public final int MIDDLE;
    RecorderAdapter.AudioHolder mHolder;
    Audio mAudio;
    RecorderAdapter mAdapter;
    WaveView currentWave;
    CopyOnWriteArrayList<WaveView> waves = new CopyOnWriteArrayList<>();

    public BarTemplatePagerAdapter(RecorderAdapter adapter, RecorderAdapter.AudioHolder holder) {
        mHolder = holder;
        mAudio = holder.audio;
        mAdapter = adapter;
        MIDDLE = HALF_MAX_VALUE - HALF_MAX_VALUE % BarTemplate.list.size();
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if(currentWave != object){
            mAudio.setBarTemplate(BarTemplate.list.get(position));
            currentWave = (WaveView)object;
        }
    }

    public void updateWaves(){
        for(WaveView wave : waves){
            wave.updateWave();
            wave.postInvalidate();
        }
    }

    Callable<Float> mProgressCallback;
    public void setProgressCallback(@NonNull Callable<Float> progressCallback){
        mProgressCallback = progressCallback;
        for(WaveView wave : waves){
            wave.setCursorProgressCall(progressCallback);
        }
    }

    public void stopProgressCallback(@NonNull Callable<Float> progressCallback){
        if(mProgressCallback == progressCallback){
            mProgressCallback = null;
            for(WaveView wave : waves){
                wave.setCursorProgressCall(null);
            }
        }
    }

    @Override
    public void onClick(View view) {
        mAudio.setEnabled(!mAudio.enabled);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        WaveView wave = new WaveView(container.getContext());
        wave.setAudio(mAudio);
        wave.setTemplate(BarTemplate.list.get(position));
        wave.setOnClickListener(mHolder);
        wave.setOnLongClickListener(mHolder);
        wave.setCursorProgressCall(mProgressCallback);
        container.addView(wave);
        waves.add(wave);
        return wave;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View)object);
        waves.remove(object);
    }

    @Override
    public int getCount() {
        return BarTemplate.list.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
}
