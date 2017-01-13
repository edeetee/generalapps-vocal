package generalapps.vocal.effects;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import generalapps.vocal.Audio;
import generalapps.vocal.MainActivity;
import generalapps.vocal.R;
import generalapps.vocal.SnappingListView;

/**
 * Created by edeetee on 27/06/2016.
 */
public class EffectCategoryAdapter extends RecyclerView.Adapter<EffectCategoryAdapter.EffectCategoryHolder> {

    private Context mContext;
    boolean mShowEffects = false;
    boolean mActivated = false;
    public final static int MIDDLE = Integer.MAX_VALUE/2 - (Integer.MAX_VALUE/2) % EffectCategory.list.size();

    public EffectAdapter.OnEffectSelectedListener mListener;

    public <T extends EffectAdapter.OnEffectSelectedListener> EffectCategoryAdapter(Context context, T listener) {
        mContext = context;
        mListener = listener;
    }

    public void setActivated(boolean activated){
        boolean isChanged = mActivated != activated;
        mActivated = activated;
        if(isChanged)
            notifyDataSetChanged();
    }

    public void setShowEffects(boolean showEffects){
        boolean isChanged = mShowEffects != showEffects;
        if(showEffects != mShowEffects){
            mShowEffects = showEffects;
            if(isChanged)
                notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return Integer.MAX_VALUE;
    }

    public static int getMiddleForEffectCategory(EffectCategory category){
        return MIDDLE + EffectCategory.list.indexOf(category);
    }

    @Override
    public void onBindViewHolder(EffectCategoryHolder holder, int position) {
        holder.bind();
    }

    @Override
    public EffectCategoryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new EffectCategoryHolder();
    }

    class EffectCategoryHolder extends RecyclerView.ViewHolder{
        ImageView imageView;
        SnappingListView effectsListView;

        public EffectCategoryHolder() {
            super(new LinearLayout(mContext));
            LinearLayout layout = (LinearLayout)itemView;
            layout.setOrientation(LinearLayout.VERTICAL);
            imageView = new ImageView(mContext);
            int width = (int)mContext.getResources().getDimension(R.dimen.default_item_height);
            layout.addView(imageView, new RecyclerView.LayoutParams(width, width));
            effectsListView = new SnappingListView(mContext);
            layout.addView(effectsListView, new RecyclerView.LayoutParams(width, width*4));
        }

        public void bind(){
            final EffectCategory category = EffectCategory.list.get(getAdapterPosition() % EffectCategory.list.size());
            Drawable categoryIcon = ContextCompat.getDrawable(itemView.getContext(), category.mIconId);
            DrawableCompat.setTint(categoryIcon, ContextCompat.getColor(itemView.getContext(), android.R.color.white));
            imageView.setImageDrawable(categoryIcon);

            if(category == EffectCategory.filters)
                Log.i("EFFCATAD", "bind: filters");

            if(category == EffectCategory.none)
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.OnEffectSelected(Effect.none);
                    }
                });
            if(mShowEffects){
                EffectAdapter adapter = new EffectAdapter(mContext, mListener, category);
                effectsListView.setAdapter(adapter);
                effectsListView.setVisibility(View.VISIBLE);
            } else{
                effectsListView.setVisibility(View.GONE);
            }
        }
    }
}