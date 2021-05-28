package com.example.geogram.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.geogram.Login.RegisterActivity;
import com.example.geogram.Prof.AccountSettingsActivity;
import com.example.geogram.R;
import com.example.geogram.homee.MainActivity;
import com.example.geogram.models.Photo;
import com.example.geogram.models.User;
import com.example.geogram.models.UserAccountSettings;
import com.example.geogram.models.UserSettings;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static com.example.geogram.utils.StringManipulation.condensUsername;

public class FirebaseModels {
    private static final String TAG = "FirebaseModels";

    //firebase 
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private String userID;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private StorageReference mStorageReference;

    //ver
    private Context mContext;
    private double mPhotoUploadProgress = 0;

    public FirebaseModels(Context context) {
        mAuth = FirebaseAuth.getInstance();

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mStorageReference = FirebaseStorage.getInstance().getReference();
        myRef = mFirebaseDatabase.getReference();

        mContext = context;
        if (mAuth.getCurrentUser() != null) {
            userID = mAuth.getCurrentUser().getUid();
        }
    }

    public void uploadNewPhoto(String photoType, final String caption, int count, String imgUrl,
                               Bitmap bm){

        Log.d(TAG, "uploadNewPhoto: attempting to upload new photo...");
        FilePath filePath = new FilePath();
        //case 1--->
        if(photoType.equals(mContext.getString(R.string.new_photo))){
            Log.d(TAG, "uploadNewPhoto: UploadNewPhoto: uploading new photo...");

            String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
            StorageReference storageReference = mStorageReference
                    .child(filePath.FIREBASE_IMAGE_STORAGE + "/" + user_id + "/photo" + (count +1));

            //convert image url to bitmap
            if(bm == null) {
                bm = ImageManager.getBitmap(imgUrl);
            }
            //100 = 100% quality of image
            byte[] bytes = ImageManager.getBytesFromBitmap(bm,100);
            //change the bitmap into bytes
            UploadTask uploadTask = null;
            uploadTask = storageReference.putBytes(bytes);

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                   // Uri firebaseUrl = taskSnapshot.getDownloadUrl();
                    Task<Uri> uri = taskSnapshot.getStorage().getDownloadUrl();
                    while(!uri.isComplete());
                    Uri firebaseurl = uri.getResult();

                    Toast.makeText(mContext, "Upload Success, download URL " +
                            firebaseurl.toString(), Toast.LENGTH_LONG).show();
                    Log.i("FBApp1 URL ", firebaseurl.toString());

                    //add the new photo to Photos node and user_photos node
                    addPhotoDatabase(caption,firebaseurl.toString());


                    //navigate to the main feed so the user can see their photo
                    Intent intent = new Intent(mContext, MainActivity.class);
                    mContext.startActivity(intent);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "onSuccess: photo upload failed...");
                    Toast.makeText(mContext, "photo upload failed...", Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                    //100 = image quality 100%
                    double progress = (100 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();

                    if(progress - 15 >mPhotoUploadProgress){
                        Toast.makeText(mContext, "photo upload progress."+ String.format("%.0f",progress), Toast.LENGTH_SHORT).show();
                        mPhotoUploadProgress = progress;
                    }
                    Log.d(TAG, "onProgress: upload progress:"+progress + "% done");
                }
            });
        }
        //case 2---> new Profile Photo
        else if(photoType.equals(mContext.getString(R.string.profile_photo))){
            Log.d(TAG, "uploadNewPhoto: uploading new PROFILE PHOTO.");

            String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
            StorageReference storageReference = mStorageReference
                    .child(filePath.FIREBASE_IMAGE_STORAGE + "/" + user_id + "/profile_photo");

            //convert image url to bitmap
            if(bm == null) {
                bm = ImageManager.getBitmap(imgUrl);
            }
            //100 = 100% quality of image
            byte[] bytes = ImageManager.getBytesFromBitmap(bm,100);
            //change the bitmap into bytes
            UploadTask uploadTask = null;
            uploadTask = storageReference.putBytes(bytes);

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Uri firebaseUrl = taskSnapshot.getDownloadUrl();
                    Task<Uri> uri = taskSnapshot.getStorage().getDownloadUrl();
                    while(!uri.isComplete());
                    Uri firebaseurl = uri.getResult();

                    Toast.makeText(mContext, "Upload Success, download URL " +
                            firebaseurl.toString(), Toast.LENGTH_LONG).show();

                    //insert into 'users_account_settings' node
                    setProfilePhoto(firebaseurl.toString());

                    /**
                     * if the phone donot pause and different uploads speed
                     * if the speed is slow then it switch to the edit_profile_fragment
                     * and the photo is changed in time
                     */
                    //setting the view_pager
                    ((AccountSettingsActivity)mContext).setViewPager(
                            /*
                            retrive the fragment number using the name of fragments
                             */
                            ((AccountSettingsActivity)mContext).pagerAdapter
                                    .getFragmentNumber(mContext.getString(R.string.edit_profile_fragment))
                    );

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "onSuccess: photo upload failed...");
                    Toast.makeText(mContext, "photo upload failed...", Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                    //100 = image quality 100%
                    double progress = (100 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();

                    if(progress - 15 >mPhotoUploadProgress){
                        Toast.makeText(mContext, "photo upload progress."+ String.format("%.0f",progress), Toast.LENGTH_SHORT).show();
                        mPhotoUploadProgress = progress;
                    }
                    Log.d(TAG, "onProgress: upload progress:"+progress + "% done");
                }
            });
        }
    }
    private void setProfilePhoto(String firebaseurl){
        Log.d(TAG, "setProfilePhoto: setting new profile image: "+firebaseurl);

        myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(mContext.getString(R.string.profile_photo))
                .setValue(firebaseurl);

    }
    private String getTimeStamp(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("ASIA/Dhaka"));
        return sdf.format(new Date());
    }
    public void addPhotoDatabase(String caption, String firebaseurl){
        Log.d(TAG, "addPhotoDatabase: adding photo to database.");

        String tags = StringManipulation.getTags(caption);
        String newPhotoKey = myRef.child(mContext.getString(R.string.dbname_photos)).push().getKey();
        Photo photo = new Photo();
        photo.setCaption(caption);
        photo.setDate_created(getTimeStamp());
        photo.setImage_path(firebaseurl);
        photo.setTags(tags);
        photo.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());
        photo.setPhoto_id(newPhotoKey);

        //insert into database
        myRef.child(mContext.getString(R.string.dbname_user_photos))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(newPhotoKey).setValue(photo);
        myRef.child(mContext.getString(R.string.dbname_photos)).child(newPhotoKey).setValue(photo);
    }

    public int getImageCount(DataSnapshot snapshot){
        int Count = 0;
        for(DataSnapshot ds: snapshot.child(mContext.getString(R.string.dbname_user_photos))
                   .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .getChildren()){
            Count++;
        }
        return Count;
    }

    /**
     * Update 'user_account_settings' node for the current user
     * @param displayName
     * @param website
     * @param description
     * @param phoneNumber
     */
    public void updateUserAccountSettings(String displayName, String website, String description,String phoneNumber){
        Log.d(TAG, "updateUserAccountSettings: updating user account settings...");

        if (displayName != null) {
            myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_display_name))
                    .setValue(displayName);
        }
        if (website != null) {
            myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_website))
                    .setValue(website);
        }
        if (description != null) {
            myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_description))
                    .setValue(description);
        }
        if (phoneNumber != null) {
            myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_phone_number))
                    .setValue(phoneNumber);
        }
    }

    /**
     * update username in the node and users account settings
     * @param username
     */
    public void updateUsername(String username){
        Log.d(TAG, "updateUsername: updating username to: "+username);

        myRef.child(mContext.getString(R.string.dbname_users))
                .child(userID)
                .child(mContext.getString(R.string.field_username))
                .setValue(username);
        myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                .child(userID)
                .child(mContext.getString(R.string.field_username))
                .setValue(username);
    }

    /**
     * update the email in the user's node
     * @param email
     */

    public void updateEmail(String email) {
        Log.d(TAG, "updateUsername: updating username to: " + email);

        myRef.child(mContext.getString(R.string.dbname_users))
                .child(userID)
                .child(mContext.getString(R.string.field_email))
                .setValue(email);

    }

//    public boolean checkIfUsernameExits(String username, DataSnapshot dataSnapshot) {
//        Log.d(TAG, "checkIfUsernameExits: checking if " + username + " already exits.");
//
//        User user = new User();
//        for (DataSnapshot ds : dataSnapshot.child(userID).getChildren()) {
//            Log.d(TAG, "checkIfUsernameExits: datasnapshot" + ds);
//
//            user.setUsername(ds.getValue(User.class).getUsername());
//            Log.d(TAG, "checkIfUsernameExits: username : " + user.getUsername());
//
//            if (StringManipulation.expandUsername(user.getUsername()).equals(username)) {
//                Log.d(TAG, "checkIfUsernameExits: FOUND A MATCH:" + user.getUsername());
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     * Register a new account with email and password --- Firebase Auth
     */

    public void registerNewEmail(final String email, String password, final String username) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "onComplete: " + task.isSuccessful());
                        /**
                         * if sign in fails , display a message to the user. If sign in succeeds
                         * the auth state listener will be notified and logic
                         * to handle the signed in user can be handled in the listener.
                         */
                        if (!task.isSuccessful()) {
                            Toast.makeText(mContext, "FOUND A MATCH: TRY - AGAIN....", Toast.LENGTH_SHORT).show();

                        } else if (task.isSuccessful()) {
                            //send verification email
                            sendVerificationEmail();

                            userID = mAuth.getCurrentUser().getUid();
                            Log.d(TAG, "onComplete: Authentic  changed..." + userID);
                        }
                    }
                });
    }

    private void sendVerificationEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {

                            } else {
                                Toast.makeText(mContext, "couldn't send verification email...", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    /**
     * Add information to the user node
     * Add information to the user_account_settings node
     *  @param email
     * @param username
     * @param description
     * @param website
     * @param profile_photo
     */

    public void addNewUser(String email, String username, String description, String website, String profile_photo,String  phone_number,String message) {

        User user = new User(userID,email, StringManipulation.condensUsername(username));

        myRef.child(mContext.getString(R.string.dbname_users))
                .child(userID).setValue(user);

        UserAccountSettings settings = new UserAccountSettings(
                description,
                username,
                0,
                0,
                0,
                profile_photo,
                StringManipulation.condensUsername(username)
                ,
                website,
                userID,phone_number,message,getTimeStamp()
        );

        myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                .child(userID).setValue(settings);
    }

    /**
     * 33 no tutorial
     * Retrieves the settings for the user currently logged in
     * Dtabase: user_Account_Setting node
     *
     * @param dataSnapshot
     * @return
     */
    public UserSettings getUserSettings(DataSnapshot dataSnapshot) {
        Log.d(TAG, "getUserAccountSettings: retrieving user account settings from firebase...");

        UserAccountSettings settings = new UserAccountSettings();
        User user = new User();

        for (DataSnapshot ds : dataSnapshot.getChildren()) {

            //user_account_settings node
            if (ds.getKey().equals(mContext.getString(R.string.dbname_user_account_settings))) {
                Log.d(TAG, "getUserAccountSettings: datasnapshot: " + ds);

                try {
                    settings.setDisplay_name(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getDisplay_name());

                    settings.setUsername(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getUsername());

                    settings.setWebsite(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getWebsite());

                    settings.setDescription(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getDescription());

                    settings.setProfile_photo(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getProfile_photo());

                    settings.setPosts(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getPosts());

                    settings.setFollowing(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getFollowing());

                    settings.setFollowers(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getFollowers());
                    settings.setPhone_number(
                            (ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getPhone_number()));





                    Log.d(TAG, "getUserAccountSettings: Retrieved user_account_settings informations..." + settings.toString());
                } catch (NullPointerException e) {
                    Log.d(TAG, "getUserAccountSettings: NullPointerException..." + e.getMessage());
                }
            }
                //users node
                if (ds.getKey().equals(mContext.getString(R.string.dbname_users))) {
                    Log.d(TAG, "getUserAccountSettings: datasnapshot: " + ds);

                    user.setUsername(
                            //ds = node,child = id;
                            ds.child(userID)
                                    .getValue(User.class)//value
                                    .getUsername());//value

                    user.setEmail(
                            //ds = node,child = id;
                            ds.child(userID)
                                    .getValue(User.class)
                                    .getEmail());
                    /*user.setPhone_number(
                            //ds = node,child = id;
                            ds.child(userID)
                                    .getValue(User.class)
                                    .getPhone_number());*/

                    user.setUser_id(
                            //ds = node,child = id;
                            ds.child(userID)
                                    .getValue(User.class)
                                    .getUser_id());

                    Log.d(TAG, "getUserAccountSettings: Retrieved user informations..." + user.toString());
                }

            }

        return new UserSettings(user,settings);
    }

}
