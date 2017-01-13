package generalapps.vocal.effects;

import android.content.Context;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;

import java.util.Arrays;
import java.util.List;

import generalapps.vocal.R;
import generalapps.vocal.effects.Effect;
import generalapps.vocal.effects.EffectCategory;

/**
 * Created by edeetee on 27/06/2016.
 */
public class EffectAdapter extends BaseAdapter {
    private Context mContext;
    private EffectCategory mCategory;

    public <T extends OnEffectSelectedListener> EffectAdapter(Context context, T listener, EffectCategory category) {
        mContext = context;
        mListener = listener;
        mCategory = category;
    }

    @Override
    public Object getItem(int i){
        return mCategory.get(i%mCategory.size());
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup collection) {
        final Effect effect = (Effect)getItem(position);
        if(view == null){
            view = new FrameLayout(mContext);
            FrameLayout layout = (FrameLayout)view;
            ImageView image = new ImageView(mContext);
            image.setImageResource(effect.mIcon);

            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.OnEffectSelected( effect);
                }
            });
            int width = (int)mContext.getResources().getDimension(R.dimen.default_item_height);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, width);

            layout.addView(image, params);
            layout.setBackgroundResource(effect != Effect.none ? R.drawable.round_rect_shape : 0);
        }

        return view;
    }

    @Override
    public int getCount() {
        return Integer.MAX_VALUE;
    }

    public interface OnEffectSelectedListener{
        void OnEffectSelected(Effect effect);
    }
    public OnEffectSelectedListener mListener;
}