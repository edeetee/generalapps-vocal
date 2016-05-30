package generalapps.vocal;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by edeetee on 27/04/2016.
 */

public class WaveView extends View {
    //struct for points
    public static class WaveValues extends ArrayList<Float> {
        static private List<WaveValues> waves = new ArrayList<>();
        List<DataSetObserver> observers = new ArrayList<>();
        private float maxVal = 0;

        public WaveValues() {
            super();
            waves.add(this);
        }

        @Override
        public boolean add(Float object) {
            if(maxVal < object)
                maxVal = object;

            return super.add(object);
        }

        public synchronized void updateObservers(){
            for(DataSetObserver observer : observers){
                observer.onChanged();
            }
        }

        public synchronized void registerDataSetObserver(DataSetObserver observer){
            observers.add(observer);
        }

        @Override
        public Float get(int index) {
            return super.get(index)/getMax();
        }

        private float getMax(){
            float max = 0f;
            for(WaveValues wave : waves){
                if(max < wave.maxVal)
                    max = wave.maxVal;
            }
            return max;
        }

        @Override
        public void clear() {
            //if has been added
            maxVal = 0;
            super.clear();
        }

        @Override
        public int indexOf(Object object) {
            throw new UnsupportedOperationException("Cannot search for index");
        }
    }

    private Audio audio;

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
            audio.waveValues.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    updateWave();
                    postInvalidate();
                }
            });
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
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

            if(MainActivity.adapter.playing){
                float x = w*MainActivity.adapter.getProgress();
                canvas.drawLine(x, h, x, 0, progressPaint);
                //redraw
                postInvalidate();
            }
        }
    }
}
