package com.example.geogram.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.geogram.Prof.ProfileActivity;
import com.example.geogram.R;
import com.example.geogram.homee.MainActivity;
import com.example.geogram.models.Like;
import com.example.geogram.models.Photo;
import com.example.geogram.models.User;
import com.example.geogram.models.UserAccountSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainfeedListAdapter extends ArrayAdapter<Photo> {

    public interface OnLoadMoreItemListener{
        void onLoadMoreItems();
    }
    OnLoadMoreItemListener mOnLoadMoreItemListener;
    private static final String TAG = "MainfeedListAdapter";
    private LayoutInflater mInflater;
    private int mLayoutResource;
    private Context mContext;
    private DatabaseReference mReference;
    private String currentUsername = "";

    public MainfeedListAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Photo> objects) {
        super(context, resource, objects);

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayoutResource = resource;
        this.mContext = context;
        mReference = FirebaseDatabase.getInstance().getReference();
    }

    static class ViewHolder{
        CircleImageView mProfileImage;
        String likesString;
        TextView username,timeDelta,caption,likes,comments;
        SquareImageView image;
        ImageView heartRed,heartWhite,comment;

        UserAccountSettings settings = new UserAccountSettings();
        User user = new User();
        StringBuilder users;
        String mLikesString;
        boolean likedByCurrentUser;
        Heart heart;
        GestureDetector detector;
        Photo photo;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        final ViewHolder holder;

        if(convertView == null){
            convertView = mInflater.inflate(mLayoutResource,parent,false);
            holder = new ViewHolder();

            holder.username = (TextView) convertView.findViewById(R.id.username);
            holder.image = (SquareImageView) convertView.findViewById(R.id.post_image);
            holder.heartRed = (ImageView) convertView.findViewById(R.id.image_heart_red);
            holder.heartWhite = (ImageView) convertView.findViewById(R.id.image_heart);
            holder.comment = (ImageView) convertView.findViewById(R.id.speech_bubble);
            holder.likes = (TextView) convertView.findViewById(R.id.image_likes);
            holder.comments = (TextView) convertView.findViewById(R.id.image_comments_link);
            holder.caption = (TextView) convertView.findViewById(R.id.image_caption);
            holder.timeDelta = (TextView) convertView.findViewById(R.id.image_time_posted);
            holder.mProfileImage = (CircleImageView) convertView.findViewById(R.id.profile_photo);
            holder.heart = new Heart(holder.heartWhite,holder.heartRed);
            holder.photo = getItem(position);

            holder.detector = new GestureDetector(mContext, new GestureListener(holder));
            holder.users = new StringBuilder();

            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }
        //get the current users username(need for checking likes string)
        getCurrentUsername();

        //get likes string
        getLikesString(holder);

        //set the caption
        holder.caption.setText(getItem(position).getCaption());
        //set the comment
        List<Comment> comments = getItem(position).getComments();
        holder.comments.setText("View all "+comments.size());
        holder.comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: loading comment thread for "+getItem(position).getPhoto_id());
                //save the user accounts setting appear in our view holder ---> user account settings
                ((MainActivity)mContext).onCommentThreadSelected(getItem(position)
                    ,mContext.getString(R.string.home_activity)); //,holder.settings
                //specific to the particular post

                //going to something else
                ((MainActivity)mContext).hideLayout();
            }
        });
        //set the time it was posted
        String timestampDifference = getTimestampDifference(getItem(position));
        if(!timestampDifference.equals("0")){
            holder.timeDelta.setText(timestampDifference + "days ago");
        }else {
            holder.timeDelta.setText("today");
        }
        //set the full image
        final ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(getItem(position).getImage_path(),holder.image);//image

        //get the profile image and username
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        //all the userId
        Query query = reference
                .child(mContext.getString(R.string.dbname_user_account_settings))//bundle_user_account_setting
                .orderByChild(mContext.getString(R.string.field_user_id))
                .equalTo(getItem(position).getUser_id());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //create another query for finding username and appending them to string
                for (final DataSnapshot singleSnapshot : snapshot.getChildren()) {
                    //currentUsername = singleSnapshot.getValue(UserAccountSettings.class).getUsername();

                    Log.d(TAG, "onDataChange: found user: "+singleSnapshot
                             .getValue(UserAccountSettings.class).getUsername());

                    holder.username.setText(singleSnapshot.getValue(UserAccountSettings.class).getUsername());
                    holder.username.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //navigating to the profile of the user
                            Log.d(TAG, "onClick: navigating to profile of:"
                                      +holder.user.getUsername());

                            Intent intent = new Intent(mContext, ProfileActivity.class);
                            intent.putExtra(mContext.getString(R.string.calling_activity),
                                    mContext.getString(R.string.home_activity));
                            intent.putExtra(mContext.getString(R.string.intent_user),holder.user);
                            mContext.startActivity(intent);
                        }
                    });

                    imageLoader.displayImage(singleSnapshot.getValue(UserAccountSettings.class).getProfile_photo(),
                            holder.mProfileImage);

                    holder.mProfileImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //navigating to the profile of the user
                            Log.d(TAG, "onClick: navigating toprofile of:"
                                    +holder.user.getUsername());

                            Intent intent = new Intent(mContext, ProfileActivity.class);
                            intent.putExtra(mContext.getString(R.string.calling_activity),
                                    mContext.getString(R.string.home_activity));
                            intent.putExtra(mContext.getString(R.string.intent_user),holder.user);
                            mContext.startActivity(intent);
                        }
                    });
                    holder.settings = singleSnapshot.getValue(UserAccountSettings.class);
                    holder.comment.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ((MainActivity)mContext).onCommentThreadSelected(getItem(position)
                                    ,mContext.getString(R.string.home_activity));//,holder.settings

                            //another thing...?
                            ((MainActivity)mContext).hideLayout();

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //get the user object
        Query userQuery = mReference
                .child(mContext.getString(R.string.dbname_users))
                .orderByChild(mContext.getString(R.string.field_user_id))
                .equalTo(getItem(position).getUser_id());

        userQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //create another query for finding username and appending them to string
                for (DataSnapshot singleSnapshot : snapshot.getChildren()) {
                    Log.d(TAG, "onDataChange: found user:"+
                            singleSnapshot.getValue(User.class).getUsername());

                    holder.user = singleSnapshot.getValue(User.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        if(reachedEndOfList(position)){
            loadMoreData();
        }
        return convertView;
    }

    private boolean reachedEndOfList(int position){
        return position == getCount() - 1;
    }

    private void loadMoreData(){

        try{
            mOnLoadMoreItemListener = (OnLoadMoreItemListener) getContext();
        }catch (ClassCastException e){
            Log.e(TAG, "loadMoreData: ClassCastException: " +e.getMessage() );
        }

        try{
            mOnLoadMoreItemListener.onLoadMoreItems();
        }catch (NullPointerException e){
            Log.e(TAG, "loadMoreData: NullPointerException: " +e.getMessage() );
        }
    }


    public class GestureListener extends GestureDetector.SimpleOnGestureListener {

        //need a constructor
        ViewHolder mHolder;

        public GestureListener(ViewHolder holder) {
            this.mHolder = holder;
        }

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
                    .child(mContext.getString(R.string.dbname_photos))
                    .child(mHolder.photo.getPhoto_id())
                    .child(mContext.getString(R.string.field_likes));

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    for (DataSnapshot singleSnapshot : snapshot.getChildren()) {

                        String keyId = singleSnapshot.getKey();
                        //case 1-> User already liked the photo
                        if (mHolder.likedByCurrentUser && singleSnapshot.getValue(Like.class)
                                .getUser_id().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {

                            mReference.child(mContext.getString(R.string.dbname_photos))
                                    .child(mHolder.photo.getPhoto_id())
                                    .child(mContext.getString(R.string.field_likes))
                                    .child(keyId)
                                    .removeValue();

                            mReference.child(mContext.getString(R.string.dbname_user_photos))
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .child(mHolder.photo.getPhoto_id())
                                    .child(mContext.getString(R.string.field_likes))
                                    .child(keyId)
                                    .removeValue();

                            mHolder.heart.toggleLike();
                            getLikesString(mHolder);
                        }
                        //case 2->User does not liked the photo
                        else if (!mHolder.likedByCurrentUser) {
                            //add new like
                            addNewLike(mHolder);
                            break;
                        }
                    }
                    if (!snapshot.exists()) {
                        addNewLike(mHolder);
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

    private void addNewLike(final ViewHolder holder) {
        Log.d(TAG, "addNewLike: adding new like...");

        String newLikeId = mReference.push().getKey();
        Like like = new Like();
        like.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());


        mReference.child(mContext.getString(R.string.dbname_photos))
                .child(holder.photo.getPhoto_id())
                .child(mContext.getString(R.string.field_likes))
                .child(newLikeId)
                .setValue(like);

        mReference.child(mContext.getString(R.string.dbname_user_photos))
                .child(holder.photo.getUser_id())
                .child(holder.photo.getPhoto_id())
                .child(mContext.getString(R.string.field_likes))
                .child(newLikeId)
                .setValue(like);

        holder.heart.toggleLike();
        getLikesString(holder);

    }

    private void getCurrentUsername(){
        Log.d(TAG, "getCurrentUsername: retriving user account settings...");

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        //all the userId
        Query query = reference
                .child(mContext.getString(R.string.dbname_users))
                .orderByChild(mContext.getString(R.string.field_user_id))
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //create another query for finding username and appending them to string
                for (DataSnapshot singleSnapshot : snapshot.getChildren()) {
                    //second queary
                    currentUsername = singleSnapshot.getValue(UserAccountSettings.class).getUsername();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void getLikesString(final ViewHolder holder) {
        //get the liked string - we see in instra LIKED BY MITCH,TABASSUM,SARA AND 30 OTHERS
        try{


        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        //all the userId
        Query query = reference
                .child(mContext.getString(R.string.dbname_photos))
                .child(holder.photo.getPhoto_id())
                .child(mContext.getString(R.string.field_likes));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //create another query for finding username and appending them to string
                holder.users = new StringBuilder();
                for (DataSnapshot singleSnapshot : snapshot.getChildren()) {
                    //finding those userId

                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                    //all the userId
                    Query query = reference.child(mContext.getString(R.string.dbname_users))
                            .orderByChild(mContext.getString(R.string.field_user_id))
                            .equalTo(singleSnapshot.getValue(Like.class).getUser_id());

                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot singleSnapshot : snapshot.getChildren()) {
                                //if we find some then append to the string

                                Log.d(TAG, "onDataChange: found like: " +
                                        singleSnapshot.getValue(User.class).getUsername());

                                holder.users.append(singleSnapshot.getValue(User.class).getUsername());
                                holder.users.append(",");
                            }

                            String[] splitUsers = holder.users.toString().split(",");
                            //current user has liked or not
                            if (holder.users.toString().contains(currentUsername + ",")) {//because of coma mitch, mitchel - duto nam kintu mitch ei word gula to same tai koma diye alada kora hoyeche -74. no tutorial
                                holder.likedByCurrentUser = true;
                            } else {
                                holder.likedByCurrentUser = false;
                            }

                            int length = splitUsers.length;
                            if (length == 1) {
                                holder.likesString = "Likes by" + splitUsers[0];
                            } else if (length == 2) {
                                holder.likesString = "Likes by" + splitUsers[0]
                                        + "and" + splitUsers[1];
                            } else if (length == 3) {
                                holder.likesString = "Likes by" + splitUsers[0]
                                        + "," + splitUsers[1]
                                        + "and" + splitUsers[2];
                            } else if (length == 4) {
                                holder.likesString = "Liked by" + splitUsers[0]
                                        + "," + splitUsers[1]
                                        + "," + splitUsers[2]
                                        + "and" + splitUsers[3];
                            } else if (length > 4) {
                                holder.likesString = "Likes by" + splitUsers[0]
                                        + "," + splitUsers[1]
                                        + "," + splitUsers[2]
                                        + "and" + (splitUsers.length - 3) + "others";
                            }
                            Log.d(TAG, "onDataChange: likes string "+holder.likesString);
                            //setupWidgets(); ---> for viewPostFragment
                            //set up like string ----> for MainfeedListAdapter
                            setupLikesString(holder,holder.likesString);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
                if (!snapshot.exists()) {
                    holder.likesString = "";
                    holder.likedByCurrentUser = false;
                    //setupWidgets(); ---> for viewPostFragment
                    //set up like string ----> for MainfeedListAdapter
                    setupLikesString(holder,holder.likesString);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        }catch (NullPointerException e){
            Log.d(TAG, "getLikesString: NullPointerException: "+e.getMessage());

            holder.likesString = "";
            holder.likedByCurrentUser = false;
            //set up like string ----> for MainfeedListAdapter
            setupLikesString(holder,holder.likesString);
        }
    }
    private void setupLikesString(final ViewHolder holder , String likesString){
        Log.d(TAG, "setupLikesString: likes string :"+holder.likesString);

        if(holder.likedByCurrentUser){
            Log.d(TAG, "setupLikesString:  photo is liked by current user");
            holder.heartWhite.setVisibility(View.GONE);
            holder.heartRed.setVisibility(View.VISIBLE);
            holder.heartRed.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return holder.detector.onTouchEvent(motionEvent);
                }
            });
        }else {
            Log.d(TAG, "setupLikesString:  photo is liked by current user");
            holder.heartWhite.setVisibility(View.VISIBLE);
            holder.heartRed.setVisibility(View.GONE);
            holder.heartWhite.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return holder.detector.onTouchEvent(motionEvent);
                }
            });
        }
        holder.likes.setText(likesString);
    }

    /**
     * Returns a string representing the number of days ago and the post was made
     * @return
     */
    private String getTimestampDifference(Photo photo) {
        Log.d(TAG, "getTimestampDifference: getting timestamp difference.");

        String difference = "";
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA);
        sdf.setTimeZone(TimeZone.getTimeZone("Canada/pacific")); //google 'android list of timezone'
        Date today = c.getTime(); //created a date
        sdf.format(today); //created a date
        Date timestamp;
        final String photoTimestamp = photo.getDate_created();
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
}
