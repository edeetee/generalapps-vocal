package generalapps.vocal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by edeetee on 28/06/2016.
 */
public class ColorView extends View {
    Paint colorPaint;

    public ColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        int alpha = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "alpha", 100);
        colorPaint = new Paint();
        colorPaint.setColor(Color.BLACK);
        colorPaint.setAlpha(alpha);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), colorPaint);
    }
}