package generalapps.vocal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by edeetee on 7/12/2016.
 */

public class CircleOverlayHelp extends HowToOverlay {
    Paint mainPaint;
    Paint holePaint;
    Bitmap background;
    TextView text;

    public CircleOverlayHelp(Context context) {
        super(context);
        init();
    }

    public CircleOverlayHelp(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    protected void init(){
        mainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mainPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary_dark));
        mainPaint.setStyle(Paint.Style.FILL);
        mainPaint.setAlpha(200);

        holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        holePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        holePaint.setAlpha(0);

        text = new TextView(getContext());
        text.setTextSize(30);
        text.setTextColor(ContextCompat.getColor(getContext(), R.color.material_color_white));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        getLayout().addView(text, params);
        getLayout().setVisibility(INVISIBLE);
    }

    public HowToOverlayLayout getLayout(){
        return ((HowToOverlayLayout)getParent());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        boolean isAlpha = 0 == Color.alpha(background.getPixel((int)x, (int)y));

        if(isAlpha && event.getAction() == MotionEvent.ACTION_DOWN){
            getLayout().clear();
        }

        return !isAlpha;
    }

    CircleOverlayHelp setText(String value){
        text.setText(value);
        invalidate();
        return this;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if(getWidth() == 0 || getHeight() == 0)
            return;

        Rect visibleRect = new Rect();
        v.getGlobalVisibleRect(visibleRect);

        Rect thisRect = new Rect();
        getGlobalVisibleRect(thisRect);

        background = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas tempC = new Canvas(background);

        tempC.drawPaint(mainPaint);
        tempC.drawCircle(visibleRect.centerX()-thisRect.left, visibleRect.centerY()-thisRect.top, visibleRect.width()/2, holePaint);

        text.setPadding(20, 0, 20, thisRect.bottom-visibleRect.top + 20);

        getLayout().setVisibility(VISIBLE);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(background != null){
            canvas.drawBitmap(background, 0, 0, null);
        }
    }
}
