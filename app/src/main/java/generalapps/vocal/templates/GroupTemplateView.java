package generalapps.vocal.templates;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by edeetee on 14/07/2016.
 */
public class GroupTemplateView extends View {
    GroupTemplate mTemplate;

    Paint enabledPaint;
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
        disabledPaint.setColor(Color.RED);
        disabledPaint.setStyle(Paint.Style.STROKE);
    }

    public void setTemplate(GroupTemplate template){
        mTemplate = template;
        postInvalidate();
    }

    static final double size = 0.2;
    static final double separation = (1-size*4)/3;

    @Override
    protected void onDraw(Canvas canvas) {
        int sizePx = (int)(size*getWidth());
        int separationPx = (int)(separation*getWidth());
        for(int i = 0; i < 4; i++){
            int x = i*(sizePx+separationPx);
            Rect group = new Rect(x, 0, x+sizePx, getHeight());
            canvas.drawRect(group, mTemplate.mEnabledGroups[i] ? enabledPaint : disabledPaint);
        }
    }
}
