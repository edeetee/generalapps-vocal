package generalapps.vocal;

import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by edeetee on 26/09/2016.
 */

public class FirebaseInstanceIDHolder extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        final UserInfo userInfo = MainActivity.auth.getCurrentUser();
        if(userInfo != null){
            final DatabaseReference ref = MainActivity.database.getReference("users").child(userInfo.getUid()).child("instanceIDToken");
            ref.setValue(FirebaseInstanceId.getInstance().getToken());
        }
    }
}
