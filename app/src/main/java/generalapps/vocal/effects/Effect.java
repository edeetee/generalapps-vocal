package generalapps.vocal.effects;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringDef;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import generalapps.vocal.Audio;

/**
 * Created by edeetee on 27/06/2016.
 */
public class Effect {
    public Effect(@DrawableRes int icon, String name, Audio.AudioEffectApplier processor){
        mIcon = icon;
        mName = name;
        mProcessor = processor;
    }

    public Effect(@DrawableRes int icon, String name){
        mIcon = icon;
        mName = name;
    }

    private Effect(){}

    public void serialize(Audio.MetaData meta){
        meta.effectCategoryIndex = EffectCategory.list.indexOf(category);
        meta.effectIndex = category.indexOf(this);
    }

    public static Effect deSerialize(Audio.MetaData meta){
        EffectCategory cat = EffectCategory.list.get(meta.effectCategoryIndex);
        return cat.get(meta.effectIndex);
    }

    public static Effect none = new Effect();

    @DrawableRes public int mIcon;
    public Audio.AudioEffectApplier mProcessor;
    public EffectCategory category;
    public String mName;
}