package generalapps.vocal;

import com.google.firebase.auth.UserInfo;

/**
 * Created by edeetee on 8/09/2016.
 */
public class User {
    public String uid;
    public String name;

    public User(){}
    public User(UserInfo user){
        uid = user.getUid();
        name = user.getDisplayName();
    }
}
