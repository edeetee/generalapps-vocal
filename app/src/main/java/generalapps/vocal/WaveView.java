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
import java.util.List;
import java.util.logging.Level;

/**
 * Created by edeetee on 27/04/2016.
 */

public class WaveView extends View {
    //struct for points
    public static class WaveValues extends ArrayList<Float> {
        static private List<WaveValues> waves = new ArrayList<>();
        private float maxVal = 0;

        public WaveValues() {
            super();
            waves.add(this);
        }

        @Override
        public boolean add(Float object) {
            if(getMax() < object)
                maxVal = object;

            return super.add(object);
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

    static int points = 100;
    static int[] BARCOLORS = {Color.CYAN, Color.BLUE};//, Color.GREEN, Color.MAGENTA};
    static int minWidth;

    Paint wavePaint;
    Paint disabledWavePaint;
    Paint progressPaint;
    Paint[] barPaint;
    Paint disabledBarPaint;

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
        this.audio = audio;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
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

            Path path = new Path();
            boolean preWaveForm = audio.audioFile == null;


            for(int xi = 0; xi < Rhythm.maxBars/audio.bars; xi++){
                int xMod = w*xi*audio.bars/Rhythm.maxBars;

                canvas.drawRect(new Rect(xMod, 0, w*audio.bars/Rhythm.maxBars+xMod, h), audio.enabled ? barPaint[xi%barPaint.length] : disabledBarPaint);

                int inv = -1;
                while(true){
                    int i = 0;
                    path.moveTo(xMod,h/2);
                    while(true){
                        int x;
                        int xNext;
                        //what format is the waveform in
                        if(!preWaveForm){
                            x = Math.round((float)i * w * audio.ticks / audio.group.maxTicks() / points) + xMod;
                            xNext = Math.round((float)(i+1) * w * audio.ticks / audio.group.maxTicks() / points) + xMod;
                        }
                        else{
                            x = w * i / points + xMod;
                            xNext =  w * (i+1) / points + xMod;
                        }

                        //exit point. Either end of wave values or reached end of bar
                        if(audio.waveValues.size()-1 == i || xMod+w*audio.bars/Rhythm.maxBars <= x){
                            Log.i("Wave i", Integer.toString(i));
                            path.lineTo(x, h/2);
                            break;
                        } else {
                            int y = h/2 + inv*Math.round(audio.waveValues.get(i)*h/2);
                            int yNext = h/2 + inv*Math.round(audio.waveValues.get(i+1)*h/2);
                            path.quadTo(x, y, xNext, yNext);

                            i++;
                        }
                    }
                    //double sided waveform
                    if(inv < 0)
                        inv = -inv;
                    else
                        break;
                }
            }

            canvas.drawPath(path, audio.enabled ? wavePaint : disabledWavePaint);

            if(MainActivity.adapter.playing){
                float x = w*MainActivity.adapter.getProgress();
                canvas.drawLine(x, h, x, 0, progressPaint);
                //redraw
                postInvalidate();
            }
        }
    }
}
