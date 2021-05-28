package com.example.geogram.Prof;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.geogram.R;
import com.example.geogram.dialogs.ConfirmPasswordDialog;
import com.example.geogram.like.LikesActivity;
import com.example.geogram.models.User;
import com.example.geogram.models.UserAccountSettings;
import com.example.geogram.models.UserSettings;
import com.example.geogram.utils.FirebaseModels;
import com.example.geogram.utils.UnivarsalImageHolder;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.hbb20.CountryCodePicker;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileFragment extends Fragment implements ConfirmPasswordDialog.OnConfirmPassword {
    private static final String TAG = "EditProfileFragment";

    @Override
    public void onConfirmPassword(String password) {
        Log.d(TAG, "onConfirmPassword: got the password..."+password);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        //get auth credentials from the user for re authentication.the example below show
        //email and password credential but there are multiple possible providers,
        //such as GoogleAuthProvider or FacebookAuthProvider.
        AuthCredential credential = EmailAuthProvider
                .getCredential(mAuth.getCurrentUser().getEmail(),password);

        //step 1: promote the user to re provider their sign in credential
        mAuth.getCurrentUser().reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            Log.d(TAG, "onComplete: User re Authenticated...");

                            //step 2: check to see if the email is not already present in the database
                            mAuth.fetchSignInMethodsForEmail(mEmail.getText().toString())
                                    .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                                            if(task.isSuccessful()){
                                                try {

                                                    if (task.getResult().getSignInMethods().size() == 1) {
                                                        Log.d(TAG, "onComplete: the email is already use.");
                                                        Toast.makeText(getActivity(), "The email is already in use...", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Log.d(TAG, "onComplete: the email is available...");

                                                        //step 3 : the email is available so update it
                                                        mAuth.getCurrentUser().updateEmail(mEmail.getText().toString())
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if (task.isSuccessful()) {
                                                                            Log.d(TAG, "onComplete: User email address is updated...");
                                                                            Toast.makeText(getActivity(), "Email updated...", Toast.LENGTH_SHORT).show();

                                                                            mFirebaseModels.updateEmail(mEmail.getText().toString());
                                                                        }
                                                                    }
                                                                });

                                                    }
                                                }catch (NullPointerException e){
                                                    Log.e(TAG, "onComplete: NullPointerException"+e.getMessage() );
                                                }
                                            }
                                        }
                                    });

                        }else {
                            Log.d(TAG, "onComplete: re authenticatation failed...");
                        }

                    }
                });
    }
    // Initialize Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseModels mFirebaseModels;
    private String userId;

    //Edit Profile Widgets
    private EditText mDisplayName, mUsername, mWebsite, mDescription, mEmail, mPhoneNumber;
    private TextView mChangeProfilePhoto;
    private CircleImageView mProfilePhoto;
    private CountryCodePicker ccp;
    //variables
    private UserSettings mUserSettings;
    private User mUuser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_editprofile, container, false);

        mProfilePhoto = (CircleImageView) view.findViewById(R.id.profile_photo);
        mDisplayName = (EditText) view.findViewById(R.id.display_name);
        mUsername = (EditText) view.findViewById(R.id.username);
        mWebsite = (EditText) view.findViewById(R.id.website);
        mDescription = (EditText) view.findViewById(R.id.description);
        mEmail = (EditText) view.findViewById(R.id.email);
        mPhoneNumber = (EditText) view.findViewById(R.id.phonNumber);
        ccp = (CountryCodePicker)view.findViewById(R.id.ccp);
        mChangeProfilePhoto = (TextView) view.findViewById(R.id.changeProfilePhoto);


        mFirebaseModels = new FirebaseModels(getActivity());
        //setProfileImage();
        setupFirebaseAuth();


        //back arrow for navigating back to profileActivity

        ImageView backArrow = (ImageView) view.findViewById(R.id.backArrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: navigating back to Profile Activity");
                getActivity().finish();
            }
        });

        ImageView checkmark = (ImageView) view.findViewById(R.id.checkMark);
        checkmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: attempting to save changes...");
                saveProfileSettings();
                Toast.makeText(getActivity(), "Thanks for Editing Profile...!", Toast.LENGTH_SHORT).show();

            }
        });
        return view;
    }

    /**
     * Retrived the data contained in the widgets and submit it
     * to the database before doing so it checks to make sure
     * username choosen is unique.
     */

    private void saveProfileSettings() {
        final String displayName = mDisplayName.getText().toString();
        final String username = mUsername.getText().toString();
        final String website = mWebsite.getText().toString();
        final String description = mDescription.getText().toString();
        final String email = mEmail.getText().toString();
        //final String phoneNumber = mPhoneNumber.getText().toString();
        final String code = ccp.getSelectedCountryCodeWithPlus();
        final String number = mPhoneNumber.getText().toString().trim();
        final String phoneNumber = code + number;
        //final long phonenumber = Long.valueOf(phoneNumber);//mPhoneNumber.getText().toString();


        /**if our apps has 6k users and all the user change
         * the name in time then we that the User method
         * works slow.. ultimately the app has been slow..

         User user = new User();
         for(DataSnapshot ds : snapshot.child(getString(R.string.dbname_users)).getChildren()){
         if(ds.getKey().equals(userId)){
         user.setUsername(ds.getValue(User.class).getUsername());
         }
         }
         Log.d(TAG, "onDataChange: CURRENT USERNAME : " + user.getUsername());
         */
        //case 1: if the user node a change to their username
        if (!mUserSettings.getUser().getUsername().equals(username)) {
            checkIfUsernameExists(username);
        }
        //case 2:  if the user node a change to their email
        if (!mUserSettings.getUser().getEmail().equals(email)) {


            //  step 1 : Re authenticate
            //  .confirm the password and email
            ConfirmPasswordDialog dialog = new ConfirmPasswordDialog();
               dialog.show(getFragmentManager(), getString(R.string.confirm_password_dialog));
            /**
             * the reason of target fragment cause if we declare the target fragment
             * then after finish editing we stay the edit profile page. if we ignore
             * the target fragment then after editing the page is swing to home activity
             */
            dialog.setTargetFragment(EditProfileFragment.this,1);
            //  step 2 : check if the email already is registered
            //  .fetchProvidersForEmail(String email)
            // step 3 : change the email
            //  .submit the new email to the database and authentic

        }
        /**
         * change the rest of the settings that do not require uniqueness
         */
        if(!mUserSettings.getSettings().getDisplay_name().equals(displayName)){
            //update displayname
            mFirebaseModels.updateUserAccountSettings(displayName,null,null,null);
        }
        if(!mUserSettings.getSettings().getWebsite().equals(website)){
            //update website
            mFirebaseModels.updateUserAccountSettings(null,website,null,null);
        }
        if(!mUserSettings.getSettings().getDescription().equals(description)){
            //update description
            mFirebaseModels.updateUserAccountSettings(null,null,description,null);
        }
        if(!mUserSettings.getSettings().getPhone_number().equals(code + number)){
            //update phone number
            mFirebaseModels.updateUserAccountSettings(null, null, null, phoneNumber);
        }
    }



    /**
     * check if username already exits in the database
     *
     * @param username
     */

    private void checkIfUsernameExists(final String username) {
        Log.d(TAG, "checkIfUsernameExists: checking if " + username + "already exists.");

        /**
         * this query system is fast before the user method
         */
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbname_users))
                .orderByChild(getString(R.string.field_username))
                .equalTo(username);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    //add the username
                    mFirebaseModels.updateUsername(username);
                    Toast.makeText(getActivity(), "saved username..", Toast.LENGTH_SHORT).show();
                }
                for (DataSnapshot singleSnapshot : snapshot.getChildren()) {
                    if (singleSnapshot.exists()) {
                        Log.d(TAG, "checkIfUsernameExists: FOUND A MATCH: " + singleSnapshot.getValue(User.class).getUsername());
                        Toast.makeText(getActivity(), "The Username already exists..", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setProfileWidgets(UserSettings userSettings) {
        Log.d(TAG, "setProfileWidgets: setting widgets with data retriving from firebase database : " + userSettings.toString());
        Log.d(TAG, "setProfileWidgets: setting widgets with data retriving from firebase database : " + userSettings.getUser().getEmail());
        Log.d(TAG, "setProfileWidgets: setting widgets with data retriving from firebase database : " + userSettings.getSettings().getPhone_number());

        mUserSettings = userSettings;
        //User user = userSettings.getUser();
        UserAccountSettings settings = userSettings.getSettings();

        UnivarsalImageHolder.setImage(settings.getProfile_photo(), mProfilePhoto, null, "");

        mDisplayName.setText(settings.getDisplay_name());
        //fragment r moddhe eta ase, tai settings r variable
        //create kore tar moddhe upr r user name dukai dbo
        mUsername.setText(settings.getUsername());
        mWebsite.setText(settings.getWebsite());
        mDescription.setText(settings.getDescription());
        mPhoneNumber.setText(String.valueOf(settings.getPhone_number()));
        mEmail.setText(userSettings.getUser().getEmail());


        mChangeProfilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: changing profile photo.");

                Intent intent = new Intent(getActivity(), LikesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//248435456 -> flag decode
                getActivity().startActivity(intent);
                getActivity().finish();

            }
        });
    }

    /*
----------------------------firebase-----------------------
*/
    /*
     Setting up Firebase Autentication object
    */
    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth...");

        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        userId = mAuth.getCurrentUser().getUid();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    //user signed in
                    Log.d(TAG, "onAuthStateChanged: signed in." + user.getUid());
                } else {
                    //user signed out
                    Log.d(TAG, "onAuthStateChanged: signed out");
                }
            }
        };
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //retrieve user information from the database
                setProfileWidgets(mFirebaseModels.getUserSettings(snapshot));

                //retrieve images for the user in question


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);

    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }


}
