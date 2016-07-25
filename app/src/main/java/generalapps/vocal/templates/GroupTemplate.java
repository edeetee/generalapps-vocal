package generalapps.vocal.templates;

import android.util.Log;

/**
 * Created by edeetee on 14/07/2016.
 */
public class GroupTemplate {
    public GroupTemplate(boolean... enabledGroups){
        if(enabledGroups.length != 4)
            Log.e("GroupTemplate", "enabledGroups length should be 4, is " + enabledGroups.length);
        mEnabledGroups = enabledGroups;
    }

    boolean[] mEnabledGroups;
}
