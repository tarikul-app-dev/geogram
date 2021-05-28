package com.example.geogram.utils;

import android.content.Context;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.geogram.R;
import com.example.geogram.models.Like;
import com.example.geogram.models.Photo;
import com.example.geogram.models.User;
import com.example.geogram.models.UserAccountSettings;
import com.example.geogram.models.UserSettings;
import com.example.geogram.utils.BottomNavigationViewHelper;
import com.example.geogram.utils.FirebaseModels;
import com.example.geogram.utils.GridImageAdapter;
import com.example.geogram.utils.SquareImageView;
import com.example.geogram.utils.UnivarsalImageHolder;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewPostFragment extends Fragment {
    private static final String TAG = "ViewPostFragment";

    /**
     * for comment - interface and we send the interface in profile activity
     * https://youtu.be/oh4YOj9VkVE
     */
    public interface OnCommentThreadSelectedListener {
        void onCommentThreadSelectedListener(Photo photo);
    }

    //create a object
    OnCommentThreadSelectedListener mOnCommentThreadSelectedListener;

    /**
     * at first we need profile image url
     */
    //variables
    private Photo mPhoto;
    private int mActivityNumber = 0;
    private String photoUsername = "";
    private String profilePhotoUrl = "";
    private UserAccountSettings mUserAccountSettings;
    UserSettings userSettings;
    //for love react
    private GestureDetector mGestureDetector;
    private Heart mHeart;

    private Boolean mLikeByCurrentUser;
    private StringBuilder mUsers;
    private String mLikesString = "";

    //widgets
    private SquareImageView mPostImage;
    private BottomNavigationViewEx bottomNavigationView;
    private TextView mBackLAbel, mCaption, mUserName, mTimestamp, mLikes, mComments;
    private ImageView mBackArrow, mEllipses, mHeartRed, mHeartWhite, mProfileImage, mComment;


    // Initialize Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    // Initialize Firebase Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseModels mFirebaseModels;
    private User mCurrentUser;

    public ViewPostFragment() {
        super();
        setArguments(new Bundle());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_post, container, false);

        mPostImage = (SquareImageView) view.findViewById(R.id.post_image);
        bottomNavigationView = (BottomNavigationViewEx) view.findViewById(R.id.bottomNavViewBar);
        mBackArrow = (ImageView) view.findViewById(R.id.backArrow);
        mEllipses = (ImageView) view.findViewById(R.id.ivEllipses);
        mHeartRed = (ImageView) view.findViewById(R.id.image_heart_red);
        mHeartWhite = (ImageView) view.findViewById(R.id.image_heart);
        mProfileImage = (CircleImageView) view.findViewById(R.id.profile_photo);

        mBackLAbel = (TextView) view.findViewById(R.id.tvBackLabel);
        mCaption = (TextView) view.findViewById(R.id.image_caption);
        mUserName = (TextView) view.findViewById(R.id.username);
        mTimestamp = (TextView) view.findViewById(R.id.image_time_posted);
        mLikes = (TextView) view.findViewById(R.id.image_likes);
        mComment = (ImageView) view.findViewById(R.id.speech_bubble);
        mComments = (TextView) view.findViewById(R.id.image_comments_link);

        //mHeartRed.setVisibility(View.GONE);
        //mHeartWhite.setVisibility(View.VISIBLE);
        mHeart = new Heart(mHeartWhite, mHeartRed);
        mGestureDetector = new GestureDetector(getActivity(), new GestureListener());

        mUserAccountSettings = new UserAccountSettings();

        setupFirebaseAuth();
        setUpBottomNavigationView();

        //init();
        //setupWidgets();
        return view;
    }

    private void init() {
        try {
            mPhoto = getPhotoFromBundle();
            UnivarsalImageHolder.setImage(mPhoto.getImage_path(), mPostImage, null, "");
            mActivityNumber = getActivityNumFromBundle();

            /**
             * profile -> view post -> view comments
             * photo -> Bundle -> photo -> Bundle -> photo
             *        <-          <-      <-        <-
             */
            String photo_id = mPhoto.getPhoto_id();
            Query query = FirebaseDatabase.getInstance().getReference()
                    .child(getString(R.string.dbname_photos))
                    .orderByChild(getString(R.string.field_photo_id))
                    .equalTo(photo_id);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    for (DataSnapshot singleSnapshot : snapshot.getChildren()) {
                        Photo newPhoto = new Photo();

                        Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                        newPhoto.setCaption(objectMap.get(getString(R.string.field_caption)).toString());
                        newPhoto.setTags(objectMap.get(getString(R.string.field_tags)).toString());
                        newPhoto.setPhoto_id(objectMap.get(getString(R.string.field_photo_id)).toString());
                        newPhoto.setUser_id(objectMap.get(getString(R.string.field_user_id)).toString());
                        newPhoto.setDate_created(objectMap.get(getString(R.string.field_date_created)).toString());
                        newPhoto.setImage_path(objectMap.get(getString(R.string.field_image_path)).toString());

                        List<Comment> commentList = new ArrayList<Comment>();

                        for (DataSnapshot dSnapshot : singleSnapshot
                                .child(getString(R.string.field_comments)).getChildren()) {
                            Comment comment = new Comment();
                            comment.setUser_id(dSnapshot.getValue(Comment.class).getUser_id()); //get the user_id
                            comment.setComment(dSnapshot.getValue(Comment.class).getComment()); //get the comment
                            comment.setDate_created(dSnapshot.getValue(Comment.class).getDate_created()); //get the time_srtamp
                            commentList.add(comment);
                        }
                        newPhoto.setComments(commentList);
                        mPhoto = newPhoto;

                        getCurrentUser();
                        getPhotoDetails();
                        //getLikesString();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d(TAG, "onCancelled: querry cancelled...");
                }
            });

            //mActivityNumber = getActivityNumFromBundle();
            //getCurrentUser();
            //getPhotoDetails();
            //getLikesString();
        } catch (NullPointerException e) {
            Log.e(TAG, "onCreateView: NullPointerException" + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            init();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            mOnCommentThreadSelectedListener = (OnCommentThreadSelectedListener) getActivity();//for context
        } catch (ClassCastException e) {
            Log.d(TAG, "onAttach: ClassCastException: " + e.getMessage());
        }
    }

    private void getLikesString() {
        //get the liked string - we see in instra LIKED BY MITCH , TABASSUM , SARA AND 30 OTHERS

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        //all the userId
        Query query = reference
                .child(getString(R.string.dbname_photos))
                .child(mPhoto.getPhoto_id())
                .child(getString(R.string.field_likes));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //create another query for finding username and appending them to string
                mUsers = new StringBuilder();
                for (DataSnapshot singleSnapshot : snapshot.getChildren()) {
                    //finding those userId

                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                    //all the userId
                    Query query = reference.child(getString(R.string.dbname_users))
                            .orderByChild(getString(R.string.field_user_id))
                            .equalTo(singleSnapshot.getValue(Like.class).getUser_id());

                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot singleSnapshot : snapshot.getChildren()) {
                                //if we find some then append to the string

                                Log.d(TAG, "onDataChange: found like: " +
                                        singleSnapshot.getValue(User.class).getUsername());

                                mUsers.append(singleSnapshot.getValue(User.class).getUsername());
                                mUsers.append(",");


                            }

                            String[] splitUsers = mUsers.toString().split(",");
                            //current user has liked or not
                            if (mUsers.toString().contains(mCurrentUser.getUsername() + ",")) {//because of coma mitch, mitchel
                                // - duto nam kintu mitch ei word gula to same tai koma diye alada kora hoyeche -74. no tutorial
                                mLikeByCurrentUser = true;
                            } else {
                                mLikeByCurrentUser = false;
                            }

                            int length = splitUsers.length;
                            if (length == 1) {
                                mLikesString = "Likes by" + splitUsers[0];
                            } else if (length == 2) {
                                mLikesString = "Likes by" + splitUsers[0]
                                        + "and" + splitUsers[1];
                            } else if (length == 3) {
                                mLikesString = "Likes by" + splitUsers[0]
                                        + "," + splitUsers[1]
                                        + "and" + splitUsers[2];
                            } else if (length == 4) {
                                mLikesString = "Liked by" + splitUsers[0]
                                        + "," + splitUsers[1]
                                        + "," + splitUsers[2]
                                        + "and" + splitUsers[3];
                            } else if (length > 4) {
                                mLikesString = "Likes by" + splitUsers[0]
                                        + "," + splitUsers[1]
                                        + "," + splitUsers[2]
                                        + "and" + (splitUsers.length - 3) + "others";
                            }
                            setupWidgets();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
                if (!snapshot.exists()) {
                    mLikesString = "";
                    mLikeByCurrentUser = false;
                    setupWidgets();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void getCurrentUser() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbname_users))
                .orderByChild(getString(R.string.field_user_id))
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot singleSnapshot : snapshot.getChildren()) {
                    mCurrentUser = singleSnapshot.getValue(User.class);
                }
                getLikesString();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "onCancelled: query canceled...");
            }
        });
    }

    public class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG, "onDoubleTap: double tab detected.");

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
            //all the userId
            Query query = reference
                    .child(getString(R.string.dbname_photos))
                    .child(mPhoto.getPhoto_id())
                    .child(getString(R.string.field_likes));

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    for (DataSnapshot singleSnapshot : snapshot.getChildren()) {

                        String keyId = singleSnapshot.getKey();
                        //case 1-> User already liked the photo
                        if (mLikeByCurrentUser && singleSnapshot.getValue(Like.class)
                                .getUser_id().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {

                            myRef.child(getString(R.string.dbname_photos))
                                    .child(mPhoto.getPhoto_id())
                                    .child(getString(R.string.field_likes))
                                    .child(keyId)
                                    .removeValue();

                            myRef.child(getString(R.string.dbname_user_photos))
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .child(mPhoto.getPhoto_id())
                                    .child(getString(R.string.field_likes))
                                    .child(keyId)
                                    .removeValue();

                            mHeart.toggleLike();
                            getLikesString();
                        }
                        //case 2->User does not liked the photo
                        else if (!mLikeByCurrentUser) {
                            //add new like
                            addNewLike();
                            break;
                        }
                    }
                    if (!snapshot.exists()) {
                        addNewLike();
                        //add new like
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
            return true;
        }
    }

    private void addNewLike() {
        Log.d(TAG, "addNewLike: adding new like...");

        String newLikeId = myRef.push().getKey();
        Like like = new Like();

        like.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());


        myRef.child(getString(R.string.dbname_photos))
                .child(mPhoto.getPhoto_id())
                .child(getString(R.string.field_likes))
                .child(newLikeId)
                .setValue(like);

        myRef.child(getString(R.string.dbname_user_photos))
                .child(mPhoto.getUser_id())
                .child(mPhoto.getPhoto_id())
                .child(getString(R.string.field_likes))
                .child(newLikeId)
                .setValue(like);

        mHeart.toggleLike();
        getLikesString();


    }


    private void getPhotoDetails() {
        Log.d(TAG, "getPhotoDetails: retrieving photo details");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbname_user_account_settings))
                .orderByChild(getString(R.string.field_user_id))
                .equalTo(mPhoto.getUser_id());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot singleSnapshot : snapshot.getChildren()) {

                    Log.d(TAG, "onDataChange: " + singleSnapshot.getValue().toString());
                    mUserAccountSettings = singleSnapshot.getValue(UserAccountSettings.class);
                    Log.d(TAG, "onDataChange: " + mUserAccountSettings.getUsername());

                }
                // setupWidgets();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "onCancelled: query canceled...");
            }
        });
    }

    private void setupWidgets() {

        String timestampDiff = getTimestampDifference();
        if (!timestampDiff.equals("0")) {
            mTimestamp.setText(timestampDiff + " DAYS AGO");
        } else {
            mTimestamp.setText("TODAY");
        }
        try {
            UnivarsalImageHolder.setImage(mUserAccountSettings.getProfile_photo(), mProfileImage, null, "");
            mUserName.setText(mUserAccountSettings.getUsername());
            mLikes.setText(mLikesString);
            //for caption
            mCaption.setText(mPhoto.getCaption());

            if (mPhoto.getComments().size() > 0) {
                mComments.setText("View all" + mPhoto.getComments().size() + "comments");
            } else {
                mComments.setText("");
            }
            mComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onClick: navigating to comments thread..");

                    mOnCommentThreadSelectedListener.onCommentThreadSelectedListener(mPhoto);
                }
            });

            mBackArrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onClick: navigating to back...");
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });

            mComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onClick: navigating to back...");

                    /**
                     * navigate to our comment section.we can easily action navigate fragment
                     * but we need to do is the photo that looking currently
                     * we need to impliment the new interface - we can actully pass the photo
                     * to the new comments fragment
                     */
                    mOnCommentThreadSelectedListener.onCommentThreadSelectedListener(mPhoto);
                }
            });

            if (mLikeByCurrentUser) {
                mHeartWhite.setVisibility(View.GONE);
                mHeartRed.setVisibility(View.VISIBLE);
                mHeartRed.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        Log.d(TAG, "onTouch: red heart touch detected...");
                        return mGestureDetector.onTouchEvent(motionEvent);
                    }
                });
            } else {
                mHeartWhite.setVisibility(View.VISIBLE);
                mHeartRed.setVisibility(View.GONE);
                mHeartWhite.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        Log.d(TAG, "onTouch: white heart touch detected...");
                        return mGestureDetector.onTouchEvent(motionEvent);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private String getTimestampDifference() {
        Log.d(TAG, "getTimestampDifference: getting timestamp difference.");

        String difference = "";
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA);
        sdf.setTimeZone(TimeZone.getTimeZone("Canada/pacific")); //google 'android list of timezone'
        Date today = c.getTime(); //created a date
        sdf.format(today); //created a date
        Date timestamp;
        final String photoTimestamp = mPhoto.getDate_created();
        try {
            timestamp = sdf.parse(photoTimestamp);
            difference = String.valueOf(Math.round(((today.getTime() - timestamp.getTime()) / 1000 / 60 / 60 / 24)));
            //60 = second,60 second = 1 minite;24=hour ; 1000 mili second is a second
        } catch (ParseException e) {
            Log.e(TAG, "getTimestampDifference: ParseException" + e.getMessage());
            difference = "0";
        }
        return difference;
    }


    /**
     * retrive the activity from the incoming bundle from
     * profileActivity interface
     *
     * @return
     */
    private int getActivityNumFromBundle() {
        Log.d(TAG, "getActivityNumFromBundle: arguments: " + getArguments());

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            return bundle.getInt(getString(R.string.activity_number));
        } else {
            return 0;
        }
    }

    /**
     * retrive the photo from the incoming bundle from
     * profileActivity interface
     *
     * @return
     */
    private Photo getPhotoFromBundle() {
        Log.d(TAG, "getPhotoFromBundle: arguments: " + getArguments());

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            return bundle.getParcelable(getString(R.string.photo));
        } else {
            return null;
        }
    }

    /**
     * //     * Bottom NavigationView setUp
     * //
     */
    private void setUpBottomNavigationView() {
        Log.d(TAG, "setUpBottomNavigaitonView: setting up bottom Navigation view...");

        BottomNavigationViewHelper.setupBottomNavigaitonView(bottomNavigationView);
        BottomNavigationViewHelper.enableNavigation(getActivity(), getActivity(), bottomNavigationView);

        /**
         *Inthe Bottom Navigation View the switch case method
         * we count the menuItem
         */
        Menu menu = bottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(mActivityNumber);
        menuItem.setChecked(true);
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
