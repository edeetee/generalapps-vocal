package generalapps.vocal;

import android.content.Context;
import android.util.AttributeSet;

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
