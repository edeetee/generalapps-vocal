package generalapps.vocal;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.IdRes;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Created by edeetee on 6/12/2016.
 */

abstract class HowToOverlay extends View implements View.OnLayoutChangeListener{
    public HowToOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HowToOverlay(Context context) {
        super(context);
    }

    public HowToOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public HowToOverlay(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}

public class HowToOverlayLayout extends RelativeLayout{
    public HowToOverlayLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            boolean prevVal = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("pref_howto", true);
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(!prevVal && sharedPreferences.getBoolean("prev_howto", true)){
                    SharedPreferences prefs = getContext().getSharedPreferences(HOWTO_PREFS, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    for (HowToInfo howToInfo : HowToInfo.values()) {
                        editor.remove(howToInfo.name());
                    }
                    editor.apply();
                }
            }
        });
    }

    public static final String HOWTO_PREFS = "HOWTO_PREFS";

    View mHelpingView;
    HowToOverlay mOverlayItem;

    enum HowToOverlayType{
        CIRCLE, SQUARE
    }

    enum HowToInfo{
        RECORDER_CIRCLE(R.id.recordProgress, "Here is the recording button.\nClick to play\nHold to record", HowToOverlayType.CIRCLE),
        NEW_TRACK(R.id.FABnew, "Click this to create a new track", HowToOverlayType.CIRCLE);

        final @IdRes int id;
        final String text;
        final HowToOverlayType overlayType;

        HowToInfo(@IdRes int id, String text, HowToOverlayType overlayType){
            this.text = text;
            this.overlayType = overlayType;
            this.id = id;
        }
    }

    boolean isPrefSeen(HowToInfo info){
        SharedPreferences prefs = getContext().getSharedPreferences(HOWTO_PREFS, Context.MODE_PRIVATE);
        return prefs.contains(info.name());
    }

    void setPrefSeen(HowToInfo info){
        SharedPreferences prefs = getContext().getSharedPreferences(HOWTO_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(info.name(), true).apply();
    }

    void tryHelpingView(HowToInfo info, View helpingView){
        if(info.id != helpingView.getId())
            return;
        if(isPrefSeen(info))
            return;
        if(!PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("pref_howto", false))
            return;

        setPrefSeen(info);
        if(info.overlayType == HowToOverlayType.CIRCLE){
            helpingView(helpingView, new CircleOverlayHelp(getContext()).setText(info.text));
        }
    }

    private void helpingView(View helpingView, HowToOverlay overlayItem){
        clear();

        addView(overlayItem);
        mOverlayItem = overlayItem;

        mHelpingView = helpingView;
        mHelpingView.addOnLayoutChangeListener(overlayItem);
    }

    boolean isOn(){
        return mHelpingView != null && mOverlayItem != null;
    }

    void clear(){
        if(isOn())
            mHelpingView.removeOnLayoutChangeListener(mOverlayItem);
        removeAllViews();
        mHelpingView = null;
        mOverlayItem = null;
    }
}
