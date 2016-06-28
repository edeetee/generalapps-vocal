package generalapps.vocal.effects;

import android.support.annotation.DrawableRes;

/**
 * Created by edeetee on 27/06/2016.
 */
public class Effect {
    public Effect(@DrawableRes int icon){
        mIcon = icon;
    }

    @DrawableRes public int mIcon;
}
