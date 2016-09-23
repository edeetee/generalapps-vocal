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
import java.util.concurrent.Callable;

import generalapps.vocal.templates.BarTemplate;

/**
 * Created by edeetee on 27/04/2016.
 */

public class WaveView extends View {

    private Audio audio;
    private BarTemplate template;

    static int[] BARCOLORS = {Color.CYAN, Color.BLUE};//, Color.GREEN, Color.MAGENTA};

    static int tickInterval = Recorder.FREQ/40;

    Paint wavePaint;
    Paint disabledWavePaint;
    Paint progressPaint;
    Paint[] barPaint;
    Paint disabledBarPaint;
    Paint barTrianglePaint;
    Paint beatLinePaint;

    Path wavePath;
    Path wavePathRead;

    List<Path> triangles;
    List<Rect> bars;

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

        beatLinePaint = new Paint();
        beatLinePaint.setColor(Color.BLACK);
        beatLinePaint.setAlpha(50);
    }

    public void setAudio(Audio audio){
        if(!audio.equals(this.audio)){
            this.audio = audio;
        }
    }

    public void updateWave(){
        if(audio != null && audio.waveValues != null && !audio.waveValues.isEmpty() && 0 < audio.group.maxTicks()) {
            //Log.d("WaveView", "updateWave() processing");

            int w = getWidth();
            int h = getHeight();

            wavePath = new Path();
            int delayShift = (int)((double)w / Rhythm.maxBars * audio.msDelay / audio.group.getMsBarPeriod());

            for (int xi = 0; xi < Rhythm.maxBars / template.mRecordingLength; xi++) {
                //skip if not enabled
                if(!template.mEnabledBars[xi])
                    continue;

                int xMod = w * xi * template.mRecordingLength / Rhythm.maxBars;

                int inv = -1;
                //double sided
                while (true) {
                    int i = 0;
                    wavePath.moveTo(Math.max(xMod+delayShift, xMod), h / 2);
                    //wavePoint iteration
                    while (true) {
                        int x = w * i * tickInterval / audio.group.maxTicks() + xMod + delayShift;
                        int xNext = w * (i + 1)  * tickInterval / audio.group.maxTicks() + xMod + delayShift;

                        //exit point. Either end of wave values or reached end of bar
                        if (audio.waveValues.size() == i+1 || (xMod + w * template.mRecordingLength / Rhythm.maxBars) < x) {
                            wavePath.lineTo(x, h / 2);
                            break;
                        //don't draw if delay has it shifted before margin
                        } else if(xMod < x) {
                            int y = (h / 2 + inv * Math.round(Math.min(1,audio.waveValues.get(i)*3) * h / 2));
                            int yNext = (h / 2 + inv * Math.round(Math.min(1,audio.waveValues.get(i+1)*3) * h / 2));
                            wavePath.quadTo(x, y, xNext, yNext);
                        }
                        i++;
                    }
                    //double sided waveform
                    if (inv < 0)
                        inv = -inv;
                    else
                        break;
                }
            }
            wavePathRead = wavePath;
        }
    }

    public void setTemplate(BarTemplate template){
        this.template = template;
    }

    @Override
    public void invalidate() {
        super.invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int barWidth = w*template.mRecordingLength/Rhythm.maxBars;
        bars = new ArrayList<>();
        triangles = new ArrayList<>();

        for(int xi = 0; xi < Rhythm.maxBars/template.mRecordingLength; xi++){
            int xMod = xi*barWidth;//skip if not enabled
            bars.add(new Rect(xMod, 0, barWidth+xMod, h));
        }

        int triangleSize = 30;
        for(int xi = 1; xi < Rhythm.maxBars/template.mRecordingLength; xi++){
            int xMod = xi*barWidth;
            Path topTri = new Path();
            topTri.moveTo(xMod-triangleSize/2, 0);
            topTri.lineTo(xMod, triangleSize/2);
            topTri.lineTo(xMod+triangleSize/2, 0);
            triangles.add(topTri);
            Path botTri = new Path(topTri);
            Matrix flip = new Matrix();
            flip.postRotate(180, xMod, h/2);
            botTri.transform(flip);
            triangles.add(botTri);
        }

        if(wavePathRead == null)
            updateWave();
    }

    Callable<Float> getProgressCall;
    public void setCursorProgressCall(Callable<Float> getProgress){
        boolean isNew = getProgress != getProgressCall;
        getProgressCall = getProgress;
        if(isNew)
            postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(audio != null){
            int w = getWidth();
            int h = getHeight();

            for(int i = 0; i < bars.size(); i++){
                canvas.drawRect(bars.get(i), audio.enabled && template.mEnabledBars[i] ? barPaint[i%barPaint.length] : disabledBarPaint);
            }

            for(int i = 1; i < Rhythm.bpb*Rhythm.maxBars; i++){
                int x = w*i/(Rhythm.bpb*Rhythm.maxBars);
                canvas.drawLine(x, 0, x, h, beatLinePaint);
            }

            if(wavePathRead != null)
                canvas.drawPath(wavePathRead, audio.enabled ? wavePaint : disabledWavePaint);

            for(Path triangle : triangles){
                canvas.drawPath(triangle, barTrianglePaint);
            }

            if(getProgressCall != null){
                try{
                    float x = w*getProgressCall.call();
                    canvas.drawLine(x, h, x, 0, progressPaint);
                    //redraw
                    postInvalidateDelayed(1000/60);
                } catch(Exception e){
                    Log.e("WaveView", "progressCallCall", e);
                }
            }
        }
    }
}
