package generalapps.vocal;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;

/**
 * Created by edeetee on 15/08/2016.
 */
public final class HowTo {
    public static void Basic(final String title, final String message, final Activity activity){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(ShouldHelp(activity)){
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
                    dialogBuilder.setTitle(title);
                    dialogBuilder.setMessage(message);
                    AlertDialog dialog = dialogBuilder.create();
                    dialog.show();
                }
            }
        });
    }

    static private boolean ShouldHelp(Activity activity){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return prefs.getBoolean("pref_howto", false);
    }

    static public void StopHelp(final Activity activity){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("pref_howto", false);
                editor.apply();
            }
        });
    }
}
