package generalapps.vocal;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

/**
 * Created by edeetee on 15/08/2016.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }
}