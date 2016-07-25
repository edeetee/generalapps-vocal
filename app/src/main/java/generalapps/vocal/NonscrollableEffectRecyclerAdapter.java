package generalapps.vocal;

import android.content.Context;
import android.support.v4.widget.Space;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import generalapps.vocal.effects.EffectAdapter;
import generalapps.vocal.effects.EffectCategory;

/**
 * Created by edeetee on 4/07/2016.
 */

public class NonscrollableEffectRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context mContext;
    final public int MIDDLE;
    static final int VIEWTYPE_SPACE = 0;
    static final int VIEWTYPE_ITEM = 1;
    EffectAdapter.OnEffectSelectedListener mEffectSelectedListener;

    public NonscrollableEffectRecyclerAdapter(Context context, EffectAdapter.OnEffectSelectedListener effectSelectedListener){
        mContext = context;
        mEffectSelectedListener = effectSelectedListener;
        MIDDLE = Integer.MAX_VALUE/2 - (Integer.MAX_VALUE/2) % EffectCategory.list.size();
    }

    @Override
    public int getItemViewType(int position) {
        return EffectCategory.list.get(actualPosition(position)).mEffects == null ? VIEWTYPE_SPACE : VIEWTYPE_ITEM;
    }

    public int actualPosition(int position){
        return position % EffectCategory.list.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType){
            case VIEWTYPE_SPACE:
                return new EmptyEffectHolder();
            case VIEWTYPE_ITEM:
                return new NonScrollableEffectHolder();
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder.getItemViewType() == VIEWTYPE_ITEM)
            ((NonScrollableEffectHolder)holder).bind();
    }

    @Override
    public int getItemCount() {
        return Integer.MAX_VALUE;
    }

    class EmptyEffectHolder extends RecyclerView.ViewHolder{
        public EmptyEffectHolder() {
            super(new Space(mContext));
            int width = (int)mContext.getResources().getDimension(R.dimen.default_item_height);
            itemView.setLayoutParams(new RecyclerView.LayoutParams(width, width));
        }
    }

    class NonScrollableEffectHolder extends RecyclerView.ViewHolder{

        SnappingListView mEffectPager;

        public NonScrollableEffectHolder(){
            super(new SnappingListView(mContext));
            mEffectPager = (SnappingListView)itemView;
            mEffectPager.setVerticalFadingEdgeEnabled(true);
        }

        public void bind(){
            EffectCategory category = EffectCategory.list.get(actualPosition(getAdapterPosition()));
            if(category.mEffects != null){
                EffectAdapter adapter = new EffectAdapter(mContext, mEffectSelectedListener, category);
                mEffectPager.setAdapter(adapter);
                mEffectPager.setVisibility(View.VISIBLE);
            } else{
                mEffectPager.setAdapter(null);
                mEffectPager.setVisibility(View.INVISIBLE);
            }
        }
    }
}
