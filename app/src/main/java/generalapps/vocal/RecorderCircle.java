package generalapps.vocal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;

import generalapps.vocal.com.github.lzyzsd.circleprogress.DonutProgress;

/**
 * Created by edeetee on 1/05/2016.
 */
public class RecorderCircle extends DonutProgress {

    long beatStart = 0;
    boolean inBeat = false;
    boolean doHighText = true;
    boolean isPlaying = false;
    int loopLength = 0;
    long loopStart = 0;
    int startSize;
    int heartBeatLength;

    BitmapDrawable record_play;
    BitmapDrawable record_pause;

    Paint circlePaint;
    Paint circlePlayingPaint;

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

        record_play = new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.mipmap.record_play));
        record_pause = new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.mipmap.record_pause));

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(ContextCompat.getColor(getContext(), R.color.accent));

        circlePlayingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePlayingPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary_light));
        circlePlayingPaint.setStyle(Paint.Style.STROKE);
        circlePlayingPaint.setStrokeWidth(20);

        setPlaying(false);
        hideBeat();

        setMax(100);
        setStartingDegree(-90);
    }

    void setPlaying(boolean playing){
        isPlaying = playing;
        invalidate();
    }

    private static final int border = 4;

    @Override
    protected void onDraw(Canvas canvas) {
        if(inBeat)
            drawHeartBeat();
        if(doHighText)
            drawHighText(canvas);
        if(loopLength != 0 && loopStart != 0)
            drawLoop();
        super.onDraw(canvas);
        if(getText().isEmpty()){
//            Drawable drawable = isPlaying ? record_pause : record_play;
//            drawable.setBounds(getWidth()/border, getHeight()/border, (int)(getWidth()*(1-1f/border)), (int)(getHeight()*(1-1f/border)));
//            drawable.draw(canvas);
            canvas.drawCircle(getWidth()/2, getHeight()/2, getWidth()/4, circlePaint);
            if(isPlaying)
                canvas.drawCircle(getWidth()/2, getHeight()/2, getWidth()/4, circlePlayingPaint);
        }
    }



    private void drawLoop(){
        float progress = (float)(System.currentTimeMillis()-loopStart)/loopLength;
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

    void hideBeat(){
        setText("");
    }

    public void setBeat(int beats){
        setText(Integer.toString(beats));
    }

    public void doLoop(int loopLength, int startLength){
        this.loopLength = loopLength;
        loopStart = System.currentTimeMillis() - startLength;
        postInvalidate();
    }

    public void doLoop(int loopLength){
        doLoop(loopLength, 0);
    }

    public void stopLoop(){
        loopLength = 0;
        loopStart = 0;
        setProgress(0);
        postInvalidate();
    }
}
