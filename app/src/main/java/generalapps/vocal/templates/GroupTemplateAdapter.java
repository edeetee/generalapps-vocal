package generalapps.vocal.templates;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import generalapps.vocal.R;
import generalapps.vocal.RecorderAdapter;

/**
 * Created by edeetee on 14/07/2016.
 */
public class GroupTemplateAdapter extends RecyclerView.Adapter<GroupTemplateAdapter.GroupTemplateHolder> {
    Context mContext;
    RecorderAdapter mAdapter;
    List<GroupTemplateHolder> holders = new ArrayList<>();
    public final static int MIDDLE = Integer.MAX_VALUE/2 - (Integer.MAX_VALUE/2) % GroupTemplate.list.size();

    public GroupTemplateAdapter(Context context, RecorderAdapter adapter){
        mContext = context;
        mAdapter = adapter;
    }

    @Override
    public GroupTemplateHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        GroupTemplateHolder holder = new GroupTemplateHolder();
        holders.add(holder);
        return holder;
    }

    public void updateAllGroupTemplates(){
        for(GroupTemplateHolder holder : holders)
            holder.templateView.postInvalidate();
    }

    @Override
    public void onViewRecycled(GroupTemplateHolder holder) {
        holders.remove(holder);
    }

    @Override
    public void onBindViewHolder(GroupTemplateHolder holder, int position) {
        holder.bind();
    }

    @Override
    public int getItemCount() {
        return Integer.MAX_VALUE;
    }

    public static int getMiddleForTemplate(GroupTemplate template){
        return MIDDLE + GroupTemplate.list.indexOf(template);
    }

    class GroupTemplateHolder extends RecyclerView.ViewHolder{
        GroupTemplateView templateView;

        public GroupTemplateHolder(){
            super(new LinearLayout(mContext));
            templateView = new GroupTemplateView(mContext);
            templateView.setAdapter(mAdapter);
            int width = (int)mContext.getResources().getDimension(R.dimen.default_item_height);
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(width, width);
            ((LinearLayout)itemView).addView(templateView, layout);
        }

        public void bind(){
            templateView.setTemplate(GroupTemplate.list.get(getAdapterPosition() % GroupTemplate.list.size()));
        }
    }
}
