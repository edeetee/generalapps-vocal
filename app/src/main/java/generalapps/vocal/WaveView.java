package generalapps.vocal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * Created by edeetee on 27/04/2016.
 */
public class WaveView extends View {
    public List<Float> points;
    Paint wavePaint;
    Paint progressPaint;
    float progress;
    boolean playing;

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint.setColor(Color.BLUE);
        wavePaint.setStyle(Paint.Style.FILL);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(Color.RED);
    }

    public void setPoints(List<Float> points){
        this.points = points;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(points != null && !points.isEmpty()){
            int w = canvas.getWidth();
            int h = canvas.getHeight();

            Path path = new Path();
            path.moveTo(0,h);

            for(int i = 0; i < 100; i++){
                int x = i * w/100;

                if(points.size() <= i){
                    path.lineTo(x, h);
                    break;
                }

                int y = Math.round(h-points.get(i)*h);
                path.lineTo(x, y);
            }
            path.lineTo(w,h);

            canvas.drawPath(path, wavePaint);

            if(playing){
                float x = progress*w;
                canvas.drawLine(x, h, x, 0, progressPaint);
            }
        }
    }
}
