package generalapps.vocal.effects;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

/**
 * Created by edeetee on 4/07/2016.
 */
public class EffectCategoryViewPager extends ViewPager {

    public EffectCategoryViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public interface OnPageScrolledListener{
        void OnPageScroller(int position, float offset, int offsetPixels);
    }
    OnPageScrolledListener mPageScrolledListener;
    public void setOnPageScrolledListener(OnPageScrolledListener listener){
        mPageScrolledListener = listener;
    }

    @Override
    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        super.onPageScrolled(position, offset, offsetPixels);
        if(mPageScrolledListener != null)
            mPageScrolledListener.OnPageScroller(position, offset, offsetPixels);
    }
}
