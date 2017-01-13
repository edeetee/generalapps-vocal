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

    private Effect(){
        mIcon = android.R.color.transparent;
        mName = "None";
    }

    public int getCategoryIndex(){
        return EffectCategory.list.indexOf(category);
    }

    public int getEffectIndex(){
        return category.indexOf(this);
    }

    public void serialize(Audio.MetaData meta){
        meta.effectCategoryIndex = getCategoryIndex();
        meta.effectIndex = getEffectIndex();
    }

    public static Effect deSerialize(Audio.MetaData meta){
        EffectCategory cat = EffectCategory.list.get(meta.effectCategoryIndex);
        return cat.get(meta.effectIndex);
    }

    public static Effect none = new Effect();
    private static EffectCategory loadNone = EffectCategory.none;

    @DrawableRes public int mIcon;
    public Audio.AudioEffectApplier mProcessor;
    public EffectCategory category;
    public String mName;
}