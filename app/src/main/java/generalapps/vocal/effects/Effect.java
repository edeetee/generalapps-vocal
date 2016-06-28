package generalapps.vocal.effects;

import android.support.annotation.DrawableRes;

import be.tarsos.dsp.AudioProcessor;
import generalapps.vocal.Audio;

/**
 * Created by edeetee on 27/06/2016.
 */
public class Effect {
    public Effect(@DrawableRes int icon, Audio.AudioEffectApplier processor){
        mIcon = icon;
        mProcessor = processor;
    }

    public Effect(@DrawableRes int icon){
        mIcon = icon;
    }

    @DrawableRes public int mIcon;
    public Audio.AudioEffectApplier mProcessor;
}
