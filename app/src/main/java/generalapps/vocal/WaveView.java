package generalapps.vocal;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by edeetee on 27/04/2016.
 */

public class WaveView extends View {
    //struct for points
    public static class WaveValues extends ArrayList<Float> {
        public interface OnMaxWaveValueChangedListener {
            void OnMaxWaveValueChanged(float max);
            float GetMaxWaveValue();
        }
        OnMaxWaveValueChangedListener mListener;
        public float localMax = 0f;

        public WaveValues() {
            super();
        }

        public void setOnMaxWaveValueChangedListener(OnMaxWaveValueChangedListener listener){
            mListener = listener;
            mListener.OnMaxWaveValueChanged(localMax);
        }

        @Override
        public boolean add(Float object) {
            if(localMax < object){
                localMax = object;
                if(mListener != null)
                    mListener.OnMaxWaveValueChanged(localMax);
            }
            return super.add(object);
        }

        @Override
        public Float get(int index) {
            if(mListener != null)
                return super.get(index)/mListener.GetMaxWaveValue();
            else
            {
                Log.w("WaveView.WaveValues", "no Listener on get call. Falling back to localMax");
                return super.get(index)/localMax;
            }
        }

        @Override
        public int indexOf(Object object) {
            throw new UnsupportedOperationException("Cannot search for index");
        }
    }

    private Audio audio;
    private RecorderAdapter adapter;

    static int points = 200;
    static int[] BARCOLORS = {Color.CYAN, Color.BLUE};//, Color.GREEN, Color.MAGENTA};
    static int minWidth;

    Paint wavePaint;
    Paint disabledWavePaint;
    Paint progressPaint;
    Paint[] barPaint;
    Paint disabledBarPaint;

    Path wavePath;

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WaveView, 0, 0);
        a.recycle();

        init();
    }

    private void init(){
        wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint.setColor(Color.BLUE);
        wavePaint.setStyle(Paint.Style.FILL);

        disabledWavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        disabledWavePaint.setColor(Color.DKGRAY);
        disabledWavePaint.setStyle(Paint.Style.STROKE);

        progressPaint = new Paint();
        progressPaint.setColor(Color.RED);
        progressPaint.setStrokeWidth(2.0f);

        barPaint = new Paint[BARCOLORS.length];
        for(int i = 0; i < BARCOLORS.length; i++){
            barPaint[i] = new Paint();
            barPaint[i].setColor(BARCOLORS[i]);
            barPaint[i].setAlpha(100);
        }

        disabledBarPaint = new Paint();
        disabledBarPaint.setColor(Color.LTGRAY);
        disabledBarPaint.setAlpha(100);
    }

    public void setAudio(Audio audio){
        if(!audio.equals(this.audio)){
            this.audio = audio;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    protected void setAdapter(RecorderAdapter adapter) {
        this.adapter = adapter;
    }

    public void updateWave(){
        if(audio != null && audio.waveValues != null && !audio.waveValues.isEmpty()) {

            int w = getWidth();
            int h = getHeight();

            //all line up
            //TODO this will screw up with orientation changes etc
            if (minWidth == 0)
                minWidth = w;
            if (minWidth < w)
                w = minWidth;

            wavePath = new Path();
            boolean preWaveForm = audio.audioFile == null;

            for (int xi = 0; xi < Rhythm.maxBars / audio.bars; xi++) {
                int xMod = w * xi * audio.bars / Rhythm.maxBars;

                int inv = -1;
                while (true) {
                    int i = 0;
                    wavePath.moveTo(xMod, h / 2);
                    while (true) {
                        int x;
                        int xNext;
                        //what format is the waveform in
                        if (!preWaveForm) {
                            x = Math.round((float) i * w * audio.ticks / audio.group.maxTicks() / points) + xMod;
                            xNext = Math.round((float) (i + 1) * w * audio.ticks / audio.group.maxTicks() / points) + xMod;
                        } else {
                            x = w * i / points + xMod;
                            xNext = w * (i + 1) / points + xMod;
                        }

                        //exit point. Either end of wave values or reached end of bar
                        if (!(i < audio.waveValues.size()-1) || (xMod + w * audio.bars / Rhythm.maxBars) <= x) {
                            wavePath.lineTo(x, h / 2);
                            break;
                        } else {
                            int y = h / 2 + inv * Math.round(audio.waveValues.get(i) * h / 2);
                            int yNext = h / 2 + inv * Math.round(audio.waveValues.get(i + 1) * h / 2);
                            wavePath.quadTo(x, y, xNext, yNext);

                            i++;
                        }
                    }
                    //double sided waveform
                    if (inv < 0)
                        inv = -inv;
                    else
                        break;
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(audio != null && audio.waveValues != null && !audio.waveValues.isEmpty()){

            int w = canvas.getWidth();
            int h = canvas.getHeight();

            //all line up
            //TODO this will screw up with orientation changes etc
            if(minWidth == 0)
                minWidth = w;
            if(minWidth < w)
                w = minWidth;

            for(int xi = 0; xi < Rhythm.maxBars/audio.bars; xi++){
                int xMod = w*xi*audio.bars/Rhythm.maxBars;

                canvas.drawRect(new Rect(xMod, 0, w*audio.bars/Rhythm.maxBars+xMod, h), audio.enabled ? barPaint[xi%barPaint.length] : disabledBarPaint);
            }

            if(wavePath == null)
                updateWave();

            canvas.drawPath(wavePath, audio.enabled ? wavePaint : disabledWavePaint);

            if(adapter.playing){
                float x = w*adapter.getProgress();
                canvas.drawLine(x, h, x, 0, progressPaint);
                //redraw
                postInvalidate();
            }
        }
    }
}
