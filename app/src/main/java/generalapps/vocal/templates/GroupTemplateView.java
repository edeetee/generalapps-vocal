package generalapps.vocal.templates;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import generalapps.vocal.RecorderAdapter;
import generalapps.vocal.Rhythm;

/**
 * Created by edeetee on 14/07/2016.
 */
public class GroupTemplateView extends View {
    GroupTemplate mTemplate;
    RecorderAdapter mAdapter;

    Paint enabledPlayingPaint;
    Paint enabledPaint;
    Paint disabledPlayingPaint;
    Paint disabledPaint;

    public GroupTemplateView(Context context) {
        super(context);
        init();
    }

    public void init(){
        enabledPaint = new Paint();
        enabledPaint.setColor(Color.RED);
        enabledPaint.setStyle(Paint.Style.FILL);

        disabledPaint = new Paint();
        disabledPaint.setColor(Color.GRAY);
        disabledPaint.setStyle(Paint.Style.FILL);

        enabledPlayingPaint = new Paint();
        enabledPlayingPaint.setColor(Color.RED);
        enabledPlayingPaint.setStyle(Paint.Style.STROKE);

        disabledPlayingPaint = new Paint();
        disabledPlayingPaint.setColor(Color.GRAY);
        disabledPlayingPaint.setStyle(Paint.Style.STROKE);
    }

    public void setTemplate(GroupTemplate template){
        mTemplate = template;
        postInvalidate();
    }

    public void setAdapter(RecorderAdapter adapter){
        mAdapter = adapter;
    }

    static final double size = 0.2;
    static final double separation = (1-size*4)/3;

    @Override
    protected void onDraw(Canvas canvas) {
        int sizePx = (int)(size*getWidth());
        int separationPx = (int)(separation*getWidth());
        int curBeatMod = (mAdapter.beats/ Rhythm.maxBeats()) % 4;
        Paint curPaint;
        for(int i = 0; i < 4; i++){
            int x = i*(sizePx+separationPx);
            Rect group = new Rect(x, 0, x+sizePx, getHeight());

            boolean currentlyPlaying = mAdapter.playing && curBeatMod == i;
            if(mTemplate.mEnabledGroups[i])
                if(currentlyPlaying)
                    curPaint = enabledPlayingPaint;
                else
                    curPaint = enabledPaint;
            else
                if(currentlyPlaying)
                    curPaint = disabledPlayingPaint;
                else
                    curPaint = disabledPaint;
            canvas.drawRect(group, curPaint);
        }
    }
}
