package generalapps.vocal;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;

import generalapps.vocal.com.github.lzyzsd.circleprogress.DonutProgress;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by edeetee on 1/05/2016.
 */
public class RecorderCircle extends DonutProgress {

    long beatStart = 0;
    boolean inBeat = false;
    boolean doHighText = false;
    boolean doLoop = false;
    AudioGroup loopGroup;
    int startSize;
    int heartBeatLength;

    static float peak = .02f;
    static float peakMod = .1f;


    public RecorderCircle(Context context) {
        this(context, null);
    }

    public RecorderCircle(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecorderCircle(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setInnerBottomText("Play/Record");
        setBeat(0);

        setMax(100);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(inBeat)
            drawHeartBeat();
        if(doHighText)
            drawHighText(canvas);
        if(doLoop)
            drawLoop();
        super.onDraw(canvas);
    }

    private void drawLoop(){
        int currentMs = loopGroup.ticksToMs(MainActivity.recorder.ticks);
        int barMs = loopGroup.getMsBarPeriod();
        float progress = (float)(currentMs%barMs)/barMs;
        setProgress(Math.round(getMax()*progress));
        postInvalidate();
    }

    public void setDoHighText(boolean doHighText){
        this.doHighText = doHighText;
        postInvalidate();
    }

    private void drawHighText(Canvas canvas){
        String text = getText();
        if (!TextUtils.isEmpty(text)) {
            float textHeight = (float)(textPaint.descent() + textPaint.ascent() + getHeight()*1.5);
            canvas.drawText(text, (getWidth() - textPaint.measureText(text)) / 2.0f, (getWidth() - textHeight) / 2.0f, textPaint);
        }
    }

    private void drawHeartBeat(){
        long time = System.currentTimeMillis()-beatStart;
        float progress = (float)time/heartBeatLength;
        ViewGroup.LayoutParams params = getLayoutParams();
        if(progress < peak){
            float incrProgress = progress/peak;
            params.height = Math.round(startSize+startSize*incrProgress*peakMod);
            params.width = params.height;
            setLayoutParams(params);
        } else if(progress < 1){
            float decrProgress = 1-(progress-peak)/(1-peak);
            params.height = Math.round(startSize+startSize*decrProgress*peakMod);
            params.width = params.height;
            setLayoutParams(params);
        } else{
            inBeat = false;
            params.height = startSize;
            params.width = startSize;
            setLayoutParams(params);
        }
        postInvalidate();
    }

    public void doHeartBeat(int heartBeatLength){
        this.heartBeatLength = heartBeatLength;
        inBeat = true;
        beatStart = System.currentTimeMillis();
        startSize = getLayoutParams().height;
        postInvalidate();
    }

    public void setBeat(int beats){
        setText(Integer.toString(beats));
    }

    public void doLoop(AudioGroup group){
        doLoop = true;
        loopGroup = group;
        postInvalidate();
    }

    public void stopLoop(){
        doLoop = false;
        setProgress(0);
        postInvalidate();
    }
}
