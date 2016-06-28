package generalapps.vocal;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.Arrays;
import java.util.List;

import generalapps.vocal.effects.EffectCategory;

/**
 * Created by edeetee on 27/06/2016.
 */
public class EffectCategoryPagerAdapter extends PagerAdapter {

    private Context mContext;
    Audio mAudio;

    public <T extends OnEffectCategorySelectedListener> EffectCategoryPagerAdapter(Context context, T listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, final int position) {
        final EffectCategory category = Audio.categories.get(position);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ImageView view = new ImageView(mContext);
        view.setImageResource(category.mIconId);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.OnEffectCategorySelected(view, category);
            }
        });

        collection.addView(view);
        return view;
    }

    @Override
    public int getCount() {
        return Audio.categories.size();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View)object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public interface OnEffectCategorySelectedListener{
        void OnEffectCategorySelected(View item, EffectCategory category);
    }
    public OnEffectCategorySelectedListener mListener;
}