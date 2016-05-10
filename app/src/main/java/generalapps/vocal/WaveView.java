package generalapps.vocal;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by edeetee on 27/04/2016.
 */

public class WaveView extends View {
    //struct for points
    public static class WaveValues extends ArrayList<Float> {
        float maxVal = 0;

        @Override
        public boolean add(Float object) {
            if(maxVal < object)
                maxVal = object;

            return super.add(object);
        }

        @Override
        public Float get(int index) {
            return super.get(index)/maxVal;
        }

        @Override
        public void clear() {
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
    static int ENABLEDCOLOR = Color.CYAN;
    static int DISABLEDCOLOR = Color.GRAY;

    Paint wavePaint;
    Paint progressPaint;
    Paint barPaint;

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

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(Color.RED);
        progressPaint.setStrokeWidth(2.0f);

        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(ENABLEDCOLOR);
        barPaint.setAlpha(100);
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

            canvas.drawRect(new Rect(0, 0, w*audio.bars/Rhythm.maxBars, h), barPaint);

            Path path = new Path();
            path.moveTo(0,h);

            for(int i = 0; i < 100; i++){
                int x = i * w/100;

                if(audio.waveValues.size() <= i){
                    path.lineTo(x, h);
                    break;
                }

                int y = Math.round(h-audio.waveValues.get(i)*h);
                path.lineTo(x, y);
            }
            path.lineTo(w,h);

            canvas.drawPath(path, wavePaint);

            if(MusicAdapter.playing && audio.isPlaying()){
                float x = (w*MusicAdapter.progress) % (w*audio.bars/Rhythm.maxBars);
                canvas.drawLine(x, h, x, 0, progressPaint);
                //redraw
                postInvalidate();
            }
        }
    }
}
