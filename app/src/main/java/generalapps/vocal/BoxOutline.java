package generalapps.vocal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by edeetee on 13/01/2017.
 */

public class BoxOutline extends View {
    Paint boxPaint;
    Rect sizeRect;

    static final int padding = 2;

    public BoxOutline(Context context) {
        super(context);
        init();
    }

    void init(){
        boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setColor(ContextCompat.getColor(getContext(), R.color.accent));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(padding*2);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        sizeRect = new Rect(padding,padding,w-padding,h-padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(sizeRect, boxPaint);
    }
}
