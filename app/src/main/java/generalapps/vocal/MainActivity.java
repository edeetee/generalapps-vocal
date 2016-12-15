package generalapps.vocal;


import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TracksListFragment.TracksFragmentListener, SharedPreferences.OnSharedPreferenceChangeListener {

    static Recorder recorder;
    static MainActivity context;
    FragmentManager fragManager;
    static FirebaseStorage storage;
    static StorageReference storageRef;
    static FirebaseDatabase database;
    static FirebaseAuth auth;
    static User user;
    static FirebaseMessaging messaging;

    static String MAIN_FRAGMENT_TAG = "MAIN_FRAGMENT_TAG";
    static int RC_SIGN_IN = 10231;

    HowToOverlayLayout howToOverlayLayout;
    Toolbar toolBar;
    EditText title;
    ImageView editButton;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        init();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        recorder.initRecorder();
    }

    String trackToLoad;

    void doIntentExtras(Bundle extras){
        //if is special notification
        if(Boolean.valueOf(extras.getString("vocalNotification"))){
            switch (extras.getString("type", "")){
                //special notification to open track
                case VocalNotifications.OPEN_TRACK:
                    trackToLoad = extras.getString("trackKey");
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if(intent.getExtras() != null)
            doIntentExtras(getIntent().getExtras());
    }

    boolean isReloading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        isReloading = savedInstanceState != null;

        howToOverlayLayout = (HowToOverlayLayout)findViewById(R.id.howToOverlay);

        toolBar = (Toolbar) findViewById(R.id.my_toolbar);
        editButton = (ImageView)toolBar.findViewById(R.id.editTitle);
        title = (EditText)toolBar.findViewById(R.id.title);
        setSupportActionBar(toolBar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if(getIntent().getExtras() != null)
            doIntentExtras(getIntent().getExtras());

        //check permissions
        List<String> permissions = new ArrayList<>(Arrays.asList(android.Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.VIBRATE));
        for (int i = 0; i < permissions.size(); i++)
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                permissions.remove(i);


        if(permissions.size() == 0)
            init();
        else
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 1);

        storage = FirebaseStorage.getInstance();
        messaging = FirebaseMessaging.getInstance();
        database = FirebaseDatabase.getInstance();
        storageRef = MainActivity.storage.getReferenceFromUrl("gs://vocal-d80ba.appspot.com/");
        auth = FirebaseAuth.getInstance();
        fragManager = getSupportFragmentManager();
    }

    void init(){
        recorder = new Recorder(this);

        new File(ContextCompat.getDataDir(this), "files/audios").mkdirs();

        if(!isReloading){
            try{
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            } catch(DatabaseException e){
                Log.w("MainActivity", "setPersistenceEnabled Failed");
            }

            if(auth.getCurrentUser() == null){
                startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setTheme(R.style.AppTheme).build(), RC_SIGN_IN);
            } else {
                makeUserThenStart();
            }
        }

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recorder.releaseRecorder();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN){
            if(resultCode == RESULT_OK){
                makeUserThenStart();
            }
        }
    }

    private void makeUserThenStart(){
        final UserInfo userInfo = auth.getCurrentUser();
        user = new User(userInfo);
        final DatabaseReference ref = database.getReference("users").child(userInfo.getUid());
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists() || !dataSnapshot.getValue(User.class).equals(user)){
                    ref.setValue(user);
                }
                start();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void start(){
        if(fragManager.findFragmentByTag(MAIN_FRAGMENT_TAG) == null){
            FragmentTransaction trans = fragManager.beginTransaction();
            trans.add(R.id.trackFragment, new TracksListFragment(), MAIN_FRAGMENT_TAG);
            trans.commit();
        }
        if(trackToLoad != null)
            database.getReference("meta").child("tracks").child(trackToLoad).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    OnTrackSelected(dataSnapshot.getValue(Track.MetaData.class));
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
    }

    @Override
    public void OnTrackSelected(Track.MetaData meta) {
        TrackFragment fragment = TrackFragment.newInstance(meta);

        FragmentTransaction transaction = fragManager.beginTransaction();
        transaction.replace(R.id.trackFragment, fragment, MAIN_FRAGMENT_TAG);
        transaction.addToBackStack(null);

        transaction.commit();
    }

    @Override
    public void OnNewTrack() {
        TrackFragment fragment = new TrackFragment();

        FragmentTransaction transaction = fragManager.beginTransaction();
        transaction.replace(R.id.trackFragment, fragment, MAIN_FRAGMENT_TAG);
        transaction.addToBackStack(null);

        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if(howToOverlayLayout.isOn()){
            howToOverlayLayout.clear();
            return;
        }

        try{
            BackOverrideFragment curRecorder = (BackOverrideFragment)getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT_TAG);
            if(curRecorder != null && curRecorder.processBackPressed())
                super.onBackPressed();
        } catch (ClassCastException e) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_settings){
            if(fragManager.findFragmentByTag(MAIN_FRAGMENT_TAG) instanceof SettingsFragment) {
                fragManager.popBackStack();
            } else{
                SettingsFragment fragment = new SettingsFragment();
                FragmentTransaction transaction = fragManager.beginTransaction();
                transaction.replace(R.id.trackFragment, fragment, MAIN_FRAGMENT_TAG);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        }
        return super.onOptionsItemSelected(item);
    }
}