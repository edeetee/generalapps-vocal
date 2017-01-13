package generalapps.vocal;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

/**
 * Created by edeetee on 12/01/2017.
 */

public class AdjustButton extends Button {
    public AdjustButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        isLeft = getId() == R.id.leftAdjust;
    }

    final static int accelMs = 1500;
    final static int adjustStart = 1;
    final static int adjustEnd = 4;
    final static int periodStart = 200;
    final static int periodEnd = 100;

    final Runnable adjustRunnable = new Runnable() {
        @Override
        public void run() {
            postDelayed(this, getPeriod());
            if(mCallback != null)
                mCallback.onAdjust(getAdjustMod() * (isLeft ? 1 : -1));
        }
    };
    final boolean isLeft;

    long pressStartTime;

    int getAdjustMod(){
        return (int)Math.min(adjustEnd, adjustStart + (adjustEnd-adjustStart)*(System.currentTimeMillis() - pressStartTime)/accelMs);
    }

    int getPeriod(){
        Log.i("AdjustHolder", "period: " + (periodEnd-periodStart)*(System.currentTimeMillis() - pressStartTime)/accelMs);
        return (int)Math.max(periodEnd, periodStart + (periodEnd-periodStart)*(System.currentTimeMillis() - pressStartTime)/accelMs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            setPressed(true);
            pressStartTime = System.currentTimeMillis();
            post(adjustRunnable);
            return true;
        } else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
            setPressed(false);
            removeCallbacks(adjustRunnable);
            return true;
        }
        return false;
    }

    interface OnAdjustCallback{
        void onAdjust(int adjustment);
    }
    OnAdjustCallback mCallback;

    void setOnAdjustCallback(OnAdjustCallback callback){
        mCallback = callback;
    }


}
