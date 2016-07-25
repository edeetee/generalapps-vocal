package generalapps.vocal.templates;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import generalapps.vocal.R;

/**
 * Created by edeetee on 14/07/2016.
 */
public class GroupTemplateAdapter extends RecyclerView.Adapter<GroupTemplateAdapter.GroupTemplateHolder> {
    static List<GroupTemplate> templates = new ArrayList<>(Arrays.asList(
            new GroupTemplate(true, true, true, true),
            new GroupTemplate(true, false, true, false)
    ));
    Context mContext;

    public GroupTemplateAdapter(Context context){
        mContext = context;
    }

    @Override
    public GroupTemplateHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new GroupTemplateHolder();
    }

    @Override
    public void onBindViewHolder(GroupTemplateHolder holder, int position) {
        holder.bind();
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    class GroupTemplateHolder extends RecyclerView.ViewHolder{
        GroupTemplateView templateView;

        public GroupTemplateHolder(){
            super(new LinearLayout(mContext));
            templateView = new GroupTemplateView(mContext);
            int width = (int)mContext.getResources().getDimension(R.dimen.default_item_height);
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(width, width);
            ((LinearLayout)itemView).addView(templateView, layout);
        }

        public void bind(){
            templateView.setTemplate(templates.get(getAdapterPosition()));
        }
    }
}
