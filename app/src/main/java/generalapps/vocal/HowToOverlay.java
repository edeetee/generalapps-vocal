package generalapps.vocal;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Dimension;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.preference.PreferenceManager;
import android.telecom.Call;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;

import java.util.List;
import java.util.concurrent.Callable;

import static generalapps.vocal.HowToOverlay.HowToInfo.ARTIST_SEARCH;
import static generalapps.vocal.HowToOverlay.HowToInfo.CONTRIBUTORS;
import static generalapps.vocal.HowToOverlay.HowToInfo.EFFECTS;

/**
 * Created by edeetee on 6/12/2016.
 */

public class HowToOverlay{
    static boolean prevVal = PreferenceManager.getDefaultSharedPreferences(MainActivity.context).getBoolean("pref_howto", true);

    public static void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        boolean newVal = sharedPreferences.getBoolean("pref_howto", true);
        if(!prevVal && newVal){
            SharedPreferences prefs = MainActivity.context.getSharedPreferences(HOWTO_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            for (HowToInfo howToInfo : HowToInfo.values()) {
                editor.remove(howToInfo.name());
            }
            editor.apply();
        }
        prevVal = newVal;
    }

    public static final String HOWTO_PREFS = "HOWTO_PREFS";

    enum HowToInfo{
        NEW_TRACK("New Track", "Tap this to create a new track, or tap anywhere else to close this message."),

        RECORDER_CIRCLE("Record a Layer", "Hold this button down to start recording and let go to stop. Letting go at the right moment will get the timing perfect, otherwise you can adjust later."),

        PLAY_CIRCLE("Play/Pause", "You can also tap the same button to play and pause."),
        DELETE("Delete", "If you don’t like your recording, long press on the layer to delete it. Deleting this recording will restart the tutorial."),
        ADJUST("Timing Crop", "Timing of the first recording can be adjusted in two ways. The first is to alter the loop length. Tap the left button to shorten, and right button to lengthen."),
        LEFT_ADJUST("Timing Delay", "The second way to adjust timing is to change the delay. Tap left to remove delay, and right to add delay to the beginning of the track. Use both timing adjustments to get the first track sounding right."),
        BAR_TEMPLATE("Templates", "Slide left or right to apply a looping template. You can create tactical pauses if you swipe to the right, or you can reduce the amount of loops if you slide left."),
        EFFECTS("Effects", "Slide to left or right to bring up selections for changing the sound of your voice. Currently in development, in the future you will be able to turn your voice into instruments, and effects."),
        MUTE("Mute", "Tapping a track will toggle the track being mute. Very helpful once you have more tracks"),
        ADD_MORE("Add more layers", "Now you can add to your initial layer to add more depth to your song before you get your friends to add to it."),
        FIRST_SETUP("Setup", "Now that you are ready to share, click this button when you are ready to get your friends to add to your song."),

        CONTRIBUTORS("Contributors", "This is where you design who will be involved in your song. Notice that your tracks are already there"),
        ARTIST_SEARCH("Add Friends", "Scroll down this to find who you want to add to your song. Tap their name, or your own name, as many times as you want to give them a layers to record."),
        PUBLISH("Publish Track", "Once friends are added, edit the order and then publish the track. Friends will be notified when it is their turn and can afterwards pass it on to the next person.");

        final String mDesc;
        final String mTitle;

        HowToInfo(String title, String desc){
            mDesc = desc;
            mTitle = title;
        }
    }

    static private boolean isPrefSeen(HowToInfo info){
        SharedPreferences prefs = MainActivity.context.getSharedPreferences(HOWTO_PREFS, Context.MODE_PRIVATE);
        return prefs.contains(info.name());
    }

    static private void setPrefSeen(HowToInfo info){
        SharedPreferences prefs = MainActivity.context.getSharedPreferences(HOWTO_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(info.name(), true).apply();
    }

    static private void setPrefUnseen(HowToInfo info){
        SharedPreferences prefs = MainActivity.context.getSharedPreferences(HOWTO_PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(info.name()).apply();
    }

    interface MiddleCallback{
        /**
         *
         * @param infos
         * @param helpingView
         * @param index the index that will be processed after this call
         * @param <T>
         * @return the index that will be processed next. Return the size of the lists to reset the stuff
         */
        <T extends View> int done(List<HowToInfo> infos, List<T> helpingView, int index);
    }

    static <T extends View> void doHelpList(final List<HowToInfo> infos, final List<T> helpingViews){
        doHelpList(infos, helpingViews, null);
    }

    static <T extends View> void doHelpList(final List<HowToInfo> infos, final List<T> helpingViews, @Nullable final MiddleCallback middleCallback){
        if(infos.size() != helpingViews.size())
            throw new IllegalArgumentException("Differing list sizes");
        if(isPrefSeen(infos.get(0)))
            return;
        new Runnable() {
            int index = 0;
            @Override
            public void run() {
                if(infos.size() <= index)
                    return;
                if(middleCallback != null){
                    int newIndex = middleCallback.done(infos, helpingViews, index);

                    if(infos.size() < newIndex)
                        throw new ArrayIndexOutOfBoundsException("returned index from callback too high");
                    else if(infos.size() == newIndex){
                        for (int i = 0; i < index; i++) {
                            setPrefUnseen(infos.get(i));
                        }
                        return;
                    }

                    if(newIndex < index){
                        for (int i = newIndex; i < index; i++) {
                            setPrefUnseen(infos.get(i));
                        }
                    }
                    index = newIndex;
                }

                View view = helpingViews.get(index);
                HowToInfo info = infos.get(index);
                showHelp(info, view, index+1 < infos.size() ? this : null);

                index++;
            }
        }.run();
    }

    static void showHelpIfUnseen(HowToInfo info, View helpingView){
        if(!isPrefSeen(info))
            showHelp(info, helpingView, null);
    }

    static void showHelp(HowToInfo info, View helpingView){
        showHelp(info, helpingView, null);
    }

    static private void showHelp(final HowToInfo info, final View helpingView, final @Nullable Runnable callback){
        if(!PreferenceManager.getDefaultSharedPreferences(MainActivity.context).getBoolean("pref_howto", true))
            return;

        setPrefSeen(info);
        final Runnable createHelp = new Runnable() {
            TapTargetView tapTarget;
            @Override
            public void run() {
                if(tapTarget != null)
                    tapTarget.dismiss(false);

                final Button gotIt = new Button(helpingView.getContext());
                gotIt.setText("Got it!");
                gotIt.setTextSize(Dimension.SP, 20);
                gotIt.setBackgroundColor(ContextCompat.getColor(MainActivity.context, R.color.accent));
                gotIt.setVisibility(callback == null ? View.GONE : View.VISIBLE);

                Rect viewRect = new Rect();
                helpingView.getGlobalVisibleRect(viewRect);
                int width = (int)MainActivity.context.getResources().getDimension(R.dimen.default_item_height);
                if(info == EFFECTS){
                    viewRect.left -= width/4;
                    viewRect.bottom = viewRect.top + width*5;
                    viewRect.right += width/4;
                }

                final RelativeLayout parent = (RelativeLayout)MainActivity.context.getWindow().getDecorView().findViewById(R.id.mainActivity);
                Rect parentRect = new Rect();
                parent.getGlobalVisibleRect(parentRect);

                final FrameLayout.LayoutParams boxParams = new FrameLayout.LayoutParams(viewRect.width(), viewRect.height());
                boxParams.leftMargin = viewRect.left;
                boxParams.topMargin = viewRect.top - parentRect.top;

                final BoxOutline outline = new BoxOutline(MainActivity.context);
                float aspect = (float)viewRect.height()/ viewRect.width();
                outline.setVisibility(aspect*1.1 < 1 || 1 < aspect/1.1 ? View.VISIBLE : View.GONE);
                parent.addView(outline, boxParams);

                final RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                buttonParams.leftMargin = 20;
                buttonParams.bottomMargin = 20;
                buttonParams.topMargin = 20;
                if(info == EFFECTS || info == CONTRIBUTORS || info == ARTIST_SEARCH)
                    buttonParams.addRule(RelativeLayout.BELOW, R.id.my_toolbar);
                else
                    buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                //if top separation is largest separation
                //params.topMargin = viewRect.top;
                // params.bottomMargin = parentRect.bottom - viewRect.bottom;
                parent.addView(gotIt, buttonParams);

                gotIt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        parent.removeView(gotIt);
                        parent.removeView(outline);
                        tapTarget.dismiss(false);
                    }
                });

                final Rect finalViewRect = new Rect(viewRect);
                int diameter = Math.max(finalViewRect.width(), finalViewRect.height());
                tapTarget = showFor(MainActivity.context, TapTarget.forBounds(viewRect, info.mTitle, info.mDesc)
                        .outerCircleColor(R.color.primary)
                        .dimColor(R.color.primary_dark)
                        .titleTextColor(R.color.accent)
                        .targetCircleColor(R.color.material_color_white)
                        .descriptionTextColor(R.color.primary_text)
                        .targetRadius(Utils.pxToDp(MainActivity.context, diameter/2))
                        .transparentTarget(true)
                        .tintTarget(false), new TapTargetView.Listener(){
                    @Override
                    public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                        if(callback != null)
                            callback.run();
                    }
                });
                tapTarget.setClickable(false);
                tapTarget.setLongClickable(false);
                if(23 <= Build.VERSION.SDK_INT)
                    tapTarget.setContextClickable(false);

                tapTarget.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        //if inside, don't consume event and dismiss thingy
                        if((finalViewRect.contains((int)event.getRawX(), (int)event.getRawY()))){
                            if(callback == null){
                                parent.removeView(gotIt);
                                parent.removeView(outline);
                                tapTarget.dismiss(true);
                            }
                            return false;
                        } else if (event.getAction() == MotionEvent.ACTION_UP && callback == null){
                            parent.removeView(gotIt);
                            parent.removeView(outline);
                            tapTarget.dismiss(false);
                        }
                        return true;
                    }
                });
            }
        };
        helpingView.post(createHelp);

//        helpingView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
//            @Override
//            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
//                createHelp.run();
//            }
//        });
    }

    public static TapTargetView showFor(Activity activity, TapTarget target, TapTargetView.Listener listener) {
        if (activity == null) throw new IllegalArgumentException("Activity is null");

        final ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        final RelativeLayout content = (RelativeLayout)decor.findViewById(R.id.mainActivity);
        layoutParams.addRule(RelativeLayout.BELOW, R.id.my_toolbar);
        final TapTargetView tapTargetView = new TapTargetView(activity, content, null, target, listener);
        content.addView(tapTargetView, layoutParams);

        return tapTargetView;
    }
}