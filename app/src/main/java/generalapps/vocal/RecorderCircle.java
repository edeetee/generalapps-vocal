package generalapps.vocal;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Canvas;
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
    Timer timer;
    TimerTask loopTask = new TimerTask() {
        @Override
        public void run() {
            post(new Runnable() {
                @Override
                public void run() {
                    setProgress((getProgress()+1) % getMax());
                }
            });
        }
    };

    long beatStart = 0;
    boolean inBeat = false;
    int startSize;

    static int heartBeatLength = Rhythm.msBeatPeriod()/4;
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

        timer = new Timer();

        setInnerBottomText("Play/Record");
        setBeat(0);

        setMax(100);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(inBeat)
            drawHeartBeat();
        super.onDraw(canvas);
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

    public void doHeartBeat(){
        inBeat = true;
        beatStart = System.currentTimeMillis();
        startSize = getLayoutParams().height;
        postInvalidate();
    }

    public void setBeat(int beats){
        setText(Integer.toString(beats));
    }

    public void startLoop(int msLoopDuration){
        loopTask = new TimerTask() {
            @Override
            public void run() {
                post(new Runnable() {
                    @Override
                    public void run() {
                        setProgress((getProgress()+1) % getMax());
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(loopTask, 0, msLoopDuration/getMax());
    }

    public void resetLoop(){
        post(new Runnable() {
            @Override
            public void run() {
                setProgress(0);
            }
        });
        loopTask.cancel();
        post(new Runnable() {
            @Override
            public void run() {
                setBeat(0);
            }
        });
    }
}
