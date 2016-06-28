package generalapps.vocal;

import android.content.Context;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.PagerAdapter;
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

import generalapps.vocal.effects.Effect;
import generalapps.vocal.effects.EffectCategory;

/**
 * Created by edeetee on 27/06/2016.
 */
public class EffectAdapter extends BaseAdapter {
    private Context mContext;
    private EffectCategory mCategory;
    public static final int HALF_MAX_VALUE = Integer.MAX_VALUE/2;
    public final int MIDDLE;

    public <T extends OnEffectSelectedListener> EffectAdapter(Context context, T listener, EffectCategory category) {
        mContext = context;
        mListener = listener;
        mCategory = category;
        MIDDLE = HALF_MAX_VALUE - HALF_MAX_VALUE % category.size();
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
                    mListener.OnEffectSelected(view, effect);
                }
            });
            layout.addView(image);
            layout.setBackgroundResource(R.drawable.round_rect_shape);
        }


        return view;
    }

    @Override
    public int getCount() {
        return Integer.MAX_VALUE;
    }

    public interface OnEffectSelectedListener{
        void OnEffectSelected(View item, Effect category);
    }
    public OnEffectSelectedListener mListener;
}