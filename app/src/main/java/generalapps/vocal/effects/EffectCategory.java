package generalapps.vocal.effects;

import android.support.annotation.DrawableRes;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import generalapps.vocal.R;

/**
 * Created by edeetee on 27/06/2016.
 */
public class EffectCategory {
    public EffectCategory(int iconId, List<Effect> effects){
        mIconId = iconId;
        mEffects = effects;
    }

    @DrawableRes public int mIconId;
    List<Effect> mEffects;

    public boolean hasChildren(){
        return mEffects != null && 0 < mEffects.size();
    }

    public Effect get(int i){
        return mEffects.get(i);
    }

    public int size(){
        return mEffects.size();
    }

    public static EffectCategory none = new EffectCategory(R.drawable.effect_category_none, null);

    public static EffectCategory micTest = new EffectCategory(R.drawable.effect_category_voice_modulation, new ArrayList<>(Arrays.asList(
            new Effect(R.drawable.effect_category_none),
            new Effect(R.drawable.effect_category_voice_modulation))));
}