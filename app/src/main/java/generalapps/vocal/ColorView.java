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
        colorPaint = new Paint();
        colorPaint.setColor(Color.BLACK);
        colorPaint.setAlpha(100);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), colorPaint);
    }
}
