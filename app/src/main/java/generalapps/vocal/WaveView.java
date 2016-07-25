package generalapps.vocal;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import generalapps.vocal.templates.BarTemplate;

/**
 * Created by edeetee on 27/04/2016.
 */

public class WaveView extends View {

    private Audio audio;
    private RecorderAdapter adapter;
    private BarTemplate template;

    static int[] BARCOLORS = {Color.CYAN, Color.BLUE};//, Color.GREEN, Color.MAGENTA};

    static int tickInterval = Recorder.FREQ/40;

    Paint wavePaint;
    Paint disabledWavePaint;
    Paint progressPaint;
    Paint[] barPaint;
    Paint disabledBarPaint;
    Paint barTrianglePaint;

    Path wavePath;

    public WaveView(Context context) {
        super(context);
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
        barTrianglePaint = new Paint();
        barTrianglePaint.setColor(Color.BLACK);

        disabledBarPaint = new Paint();
        disabledBarPaint.setColor(Color.LTGRAY);
        disabledBarPaint.setAlpha(100);
    }

    public void setAudio(Audio audio){
        if(!audio.equals(this.audio)){
            this.audio = audio;
        }
    }

    public void updateWave(){
        if(audio != null && audio.waveValues != null && !audio.waveValues.isEmpty() && 0 < audio.group.maxTicks()) {

            int w = getWidth();
            int h = getHeight();

            wavePath = new Path();

            for (int xi = 0; xi < Rhythm.maxBars / template.mRecordingLength; xi++) {
                //skip if not enabled
                if(!template.mEnabledBars[xi])
                    continue;

                int xMod = w * xi * template.mRecordingLength / Rhythm.maxBars;

                int inv = -1;
                //double sided
                while (true) {
                    int i = 0;
                    wavePath.moveTo(xMod, h / 2);
                    //wavePoint iteration
                    while (true) {
                        int x = w * i * tickInterval / audio.group.maxTicks() + xMod;
                        int xNext = w * (i + 1)  * tickInterval / audio.group.maxTicks() + xMod;

                        //exit point. Either end of wave values or reached end of bar
                        if (audio.waveValues.size() == i+1 || (xMod + w * template.mRecordingLength / Rhythm.maxBars) <= x) {
                            wavePath.lineTo(x, h / 2);
                            break;
                        } else {
                            int y = (h / 2 + inv * Math.round(Math.min(1,audio.waveValues.get(i)*3) * h / 2));
                            int yNext = (h / 2 + inv * Math.round(Math.min(1,audio.waveValues.get(i+1)*3) * h / 2));
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

    public void setAdapter(RecorderAdapter adapter){
        this.adapter = adapter;
    }

    public void setTemplate(BarTemplate template){
        this.template = template;
    }

    @Override
    public void invalidate() {
        updateWave();
        super.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(audio != null){

            int w = getWidth();
            int h = getHeight();

            int barWidth = w*template.mRecordingLength/Rhythm.maxBars;

            for(int xi = 0; xi < Rhythm.maxBars/template.mRecordingLength; xi++){
                int xMod = xi*barWidth;//skip if not enabled
                canvas.drawRect(new Rect(xMod, 0, barWidth+xMod, h), audio.enabled && template.mEnabledBars[xi] ? barPaint[xi%barPaint.length] : disabledBarPaint);
            }

            if(wavePath == null)
                updateWave();
            if(wavePath != null)
                canvas.drawPath(wavePath, audio.enabled ? wavePaint : disabledWavePaint);

            int triangleSize = 30;
            for(int xi = 1; xi < Rhythm.maxBars/template.mRecordingLength; xi++){
                int xMod = xi*barWidth;
                Path tri = new Path();
                tri.moveTo(xMod-triangleSize/2, 0);
                tri.lineTo(xMod, triangleSize/2);
                tri.lineTo(xMod+triangleSize/2, 0);
                canvas.drawPath(tri, barTrianglePaint);
                Matrix flip = new Matrix();
                flip.postRotate(180, xMod, h/2);
                tri.transform(flip);
                canvas.drawPath(tri, barTrianglePaint);
            }

            if(adapter.playing){
                float x = w*adapter.getProgress();
                canvas.drawLine(x, h, x, 0, progressPaint);
                //redraw
                postInvalidate();
            }
        }
    }
}
