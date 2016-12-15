package generalapps.vocal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by edeetee on 4/11/2016.
 */

public class PreviousEditorLinkDecoration extends RecyclerView.ItemDecoration {
    private Drawable link;
    private Track mTrack;

    PreviousEditorLinkDecoration(Context context, Track track){
        link = ContextCompat.getDrawable(context, R.drawable.ic_insert_link_black_24dp);
        mTrack = track;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if(mTrack == null)
            return;

        int itemWidth = link.getIntrinsicWidth();
        int itemHeight = link.getIntrinsicHeight();
        int left = parent.getLeft();
        int parentWidth = parent.getWidth();

        int items = parent.getAdapter().getItemCount();
        List<String> foundEditors = new ArrayList<>();
        for (int i = 0; i < items; i++) {
            //go through all editors, even undrawn ones
            String editor = mTrack.getEditor(i);
            if(Track.SpecialEditor.parse(editor) != Track.SpecialEditor.PREVIOUS || foundEditors.contains(editor))
                continue;
            //add to found editors so link isn't redrawn
            foundEditors.add(editor);

            RecyclerView.ViewHolder child = parent.findViewHolderForAdapterPosition(i);
            //only draw if visible
            if(child == null || child.getLayoutPosition() < 0)
                continue;

            int top = Math.round(child.itemView.getY()-itemHeight/2);
            int bottom = top + itemHeight;

            int first = Math.round(left+parentWidth*0.1f-itemWidth/2);
            int second = Math.round(left+parentWidth*0.9f-itemWidth/2);

            c.save();
            c.rotate(90, first+itemWidth/2, top+itemHeight/2);
            link.setBounds(first, top, first+itemWidth, bottom);
            link.draw(c);
            c.restore();

            c.save();
            c.rotate(-90, second+itemWidth/2, top+itemHeight/2);
            link.setBounds(second, top, second+itemWidth, bottom);
            link.draw(c);
            c.restore();
        }
    }
}
