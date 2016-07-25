package generalapps.vocal.effects;

import android.support.annotation.DrawableRes;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

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

    private Effect(){}

    public JSONObject serialize(){
        try{
            JSONObject obj = new JSONObject();
            obj.put("categoryIndex", EffectCategory.list.indexOf(category));
            obj.put("effectIndex", category.indexOf(this));
            return obj;
        } catch(JSONException e){
            Log.e("Effect", "serialize failed", e);
            return null;
        }
    }

    public static Effect deSerialize(JSONObject obj) {
        try{
            EffectCategory cat = EffectCategory.list.get(obj.getInt("categoryIndex"));
            return cat.get(obj.getInt("effectIndex"));
        } catch(JSONException e){
            Log.e("Effect", "serialize failed", e);
            return null;
        }
    }

    public static Effect none = new Effect();

    @DrawableRes public int mIcon;
    public Audio.AudioEffectApplier mProcessor;
    public EffectCategory category;
}