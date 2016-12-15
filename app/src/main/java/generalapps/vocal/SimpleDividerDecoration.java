package generalapps.vocal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by edeetee on 13/10/2016.
 */

class SimpleDividerDecoration extends RecyclerView.ItemDecoration {
    private Drawable mDivider;
    private float mDividerMargin;

    /**
     *
     * @param context the context
     * @param dividerMargin double between 0 and 1 defining how much margin
     */
    SimpleDividerDecoration(Context context, float dividerMargin) {
        mDivider = ContextCompat.getDrawable(context, R.drawable.line_divider);
        mDividerMargin = dividerMargin;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        //outRect.bottom = mDivider.getIntrinsicHeight();
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int width = parent.getWidth();
        int left = parent.getPaddingLeft() + Math.round(width*mDividerMargin);
        int right = Math.round(width*(1-mDividerMargin)) - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount-1; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + mDivider.getIntrinsicHeight();

            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }
}