package generalapps.vocal;

/**
 * Created by edeetee on 11/07/2016.
 */

import android.support.v7.widget.RecyclerView;

/**
 * An interface that LayoutManagers that should snap to grid should implement.
 */
public interface ISnappyLayoutManager {

    /**
     * @param velocityX
     * @param velocityY
     * @return the resultant position from a fling of the given velocity.
     */
    int getPositionForVelocity(RecyclerView recyclerView, int velocityX, int velocityY);

    /**
     * @return the position this list must scroll to to fix a state where the
     * views are not snapped to grid.
     */
    int getFixScrollPos(RecyclerView recyclerView);

}
