package com.example.geogram;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.example.geogram.Login.LoginActivity;
import com.example.geogram.homee.MainActivity;
import com.example.geogram.models.UserAccountSettings;
import com.example.geogram.utility.ChatMessageListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class ChatroomActivity extends AppCompatActivity {

    private static final String TAG = "ChatroomActivity";
    private NotificationCompat.Builder notificationBuilder;
    private int currentNotificationID = 0;
    private NotificationManager notificationManager;

    //firebase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mMessagesReference;

    //widgets
    private TextView mChatroomName;
    private ListView mListView;
    private EditText mMessage;
    private ImageView mCheckmark;

    //vars
    private Chatroom mChatroom;
    private List<UserAccountSettings> mMessagesList;
    private Set<String> mMessageIdSet;
    private ChatMessageListAdapter mAdapter;
    public static boolean isActivityRunning;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);
        mChatroomName = (TextView) findViewById(R.id.text_chatroom_name);
        mListView = (ListView) findViewById(R.id.listView);
        mMessage = (EditText) findViewById(R.id.input_message);
        mCheckmark = (ImageView) findViewById(R.id.checkmark);
//        getSupportActionBar().hide();
        Log.d(TAG, "onCreate: started.");

        setupFirebaseAuth();
        getChatroom();
        init();
        hideSoftKeyboard();

    }

    private void init(){
        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        mMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListView.setSelection(mAdapter.getCount() - 1); //scroll to the bottom of the list
            }
        });

        mCheckmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!mMessage.getText().toString().equals("")){
                    String message = mMessage.getText().toString();
                    Log.d(TAG, "onClick: sending new message: " + message);

                    //create the new message object for inserting
                    ChatMessage newMessage = new ChatMessage();
                    newMessage.setMessage(message);
                    newMessage.setType("send");
                    newMessage.setTimestamp(getTimestamp());
                    newMessage.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());

                    //get a database reference
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                            .child(getString(R.string.dbnode_chatrooms))
                            .child(mChatroom.getChatroom_id())
                            .child(getString(R.string.field_chatroom_messages));

                    //create the new messages id
                    String newMessageId = reference.push().getKey();

                    //insert the new message into the chatroom
                    reference
                            .child(newMessageId)
                            .setValue(newMessage);

                    //clear the EditText
                    mMessage.setText("");

                    //refresh the messages list? Or is it done by the listener??
                }

            }
        });

    }

    /**
     * Retrieve the chatroom name using a query
     */
    private void getChatroom(){
        Log.d(TAG, "getChatroom: getting selected chatroom details");

        Intent intent = getIntent();
        if(intent.hasExtra(getString(R.string.intent_chatroom))){
            Chatroom chatroom = intent.getParcelableExtra(getString(R.string.intent_chatroom));
            Log.d(TAG, "getChatroom: chatroom: " + chatroom.toString());
            mChatroom = chatroom;
            mChatroomName.setText(mChatroom.getChatroom_name());

            enableChatroomListener();
        }
    }


    private void getChatroomMessages(){

        if(mMessagesList == null){
            mMessagesList = new ArrayList<>();
            mMessageIdSet = new HashSet<>();
            initMessagesList();
        }
        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbnode_chatrooms))
                .child(mChatroom.getChatroom_id())
                .child(getString(R.string.field_chatroom_messages));
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String userId = " ";
                for(DataSnapshot snapshot: dataSnapshot.getChildren()) {

                    Log.d(TAG, "onDataChange: found chatroom message: "
                            + snapshot.getValue());
                    try {//need to catch null pointer here because the initial welcome message to the
                        //chatroom has no user id
                        UserAccountSettings message = new UserAccountSettings();
                        userId = snapshot.getValue(UserAccountSettings.class).getUser_id();

                        //check to see if the message has already been added to the list
                        //if the message has already been added we don't need to add it again
                        if(!mMessageIdSet.contains(snapshot.getKey())){
                            Log.d(TAG, "onDataChange: adding a new message to the list: " + snapshot.getKey());
                            //add the message id to the message set
                            mMessageIdSet.add(snapshot.getKey());
                            if(userId != null){ //check and make sure it's not the first message (has no user id)
                                message.setMessage(snapshot.getValue(UserAccountSettings.class).getMessage());
                                message.setUser_id(snapshot.getValue(UserAccountSettings.class).getUser_id());
                                message.setTimestamp(snapshot.getValue(ChatMessage.class).getTimestamp());
                                message.setProfile_photo("");
                                message.setDisplay_name("");
                                mMessagesList.add(message);
                            }else{
                                message.setMessage(snapshot.getValue(UserAccountSettings.class).getMessage());
                                message.setTimestamp(snapshot.getValue(ChatMessage.class).getTimestamp());
                                message.setProfile_photo("");
                                message.setDisplay_name("");
                                mMessagesList.add(message);
                            }
                        }

                    } catch (NullPointerException e) {
                        Log.e(TAG, "onDataChange: NullPointerException: " + e.getMessage());
                    }
                }
                //query the users node to get the profile images and names
               getUserDetails();
                mAdapter.notifyDataSetChanged(); //notify the adapter that the dataset has changed
                mListView.setSelection(mAdapter.getCount() - 1); //scroll to the bottom of the list
                //initMessagesList();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setChatroomMessages(){

        if(mMessagesList == null){
            mMessagesList = new ArrayList<>();
            mMessageIdSet = new HashSet<>();
            initMessagesList();
        }
        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbnode_chatrooms))
                .child(mChatroom.getChatroom_id())
                .child(getString(R.string.field_chatroom_messages));
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String userId = " ";
                String user = " ";
                String msg = " ";
                for(DataSnapshot snapshot: dataSnapshot.getChildren()) {

                    Log.d(TAG, "onDataChange: found chatroom message: "
                            + snapshot.getValue());
                    try {//need to catch null pointer here because the initial welcome message to the
                        //chatroom has no user id
                        UserAccountSettings message = new UserAccountSettings();
                        userId = snapshot.getValue(UserAccountSettings.class).getUser_id();

                        //check to see if the message has already been added to the list
                        //if the message has already been added we don't need to add it again
                        if(!mMessageIdSet.contains(snapshot.getKey())){
                            Log.d(TAG, "onDataChange: adding a new message to the list: " + snapshot.getKey());
                            //add the message id to the message set
                            mMessageIdSet.add(snapshot.getKey());
                            if(userId != null){ //check and make sure it's not the first message (has no user id)
                                message.setMessage(snapshot.getValue(UserAccountSettings.class).getMessage());
                                message.setUser_id(snapshot.getValue(UserAccountSettings.class).getUser_id());
                                message.setTimestamp(snapshot.getValue(ChatMessage.class).getTimestamp());
                                message.setProfile_photo("");
                                message.setDisplay_name("");
                                mMessagesList.add(message);
                                user = message.getDisplay_name();
                                msg = message.getMessage();

                            }else{
                                message.setMessage(snapshot.getValue(UserAccountSettings.class).getMessage());
                                message.setTimestamp(snapshot.getValue(ChatMessage.class).getTimestamp());
                                message.setProfile_photo("");
                                message.setDisplay_name("");
                                mMessagesList.add(message);
                                user = message.getDisplay_name();
                                msg = message.getMessage();
                            }
                        }

                    } catch (NullPointerException e) {
                        Log.e(TAG, "onDataChange: NullPointerException: " + e.getMessage());
                    }
                }
                //query the users node to get the profile images and names
                getUserDetails();
                mAdapter.notifyDataSetChanged(); //notify the adapter that the dataset has changed
                mListView.setSelection(mAdapter.getCount() - 1); //scroll to the bottom of the list
                String userIdS = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                        .getString("userId", "");
                if(userId.equals(userIdS)){

                }else {
                    receivedNewMessage(user,msg);
                }

            }


            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getUserDetails(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        for(int i = 0; i < mMessagesList.size(); i++) {
            // Log.d(TAG, "onDataChange: searching for userId: " + mMessagesList.get(i).getUser_id());
            final int j = i; //i = iteaterator variable
            if(mMessagesList.get(i).getUser_id() != null && mMessagesList.get(i).getProfile_photo().equals("")){
                //R.string.dbname_users
                Query query = reference.child(getString(R.string.dbname_user_account_settings))
                        .orderByKey()
                        .equalTo(mMessagesList.get(i).getUser_id());
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //we retrive single user object to the database , so
                        // there is no need to loop we can just go --> dataSnapshot.getChildren().iterator().next();
                        DataSnapshot singleSnapshot = dataSnapshot.getChildren().iterator().next();
                        mMessagesList.get(j).setProfile_photo(singleSnapshot.getValue(UserAccountSettings.class).getProfile_photo());
                        mMessagesList.get(j).setDisplay_name(singleSnapshot.getValue(UserAccountSettings.class).getDisplay_name());
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        }

    }
    public void receivedNewMessage(String user,String msg){
        try {
            addNotification(user,msg);
            Uri sound = Uri.parse("android.resource://" +
                    ChatroomActivity.this.getPackageName() + "/" + R.raw.abc);
            Ringtone r = RingtoneManager.getRingtone(ChatroomActivity.this, sound);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mAdapter = new ChatMessageListAdapter(getApplicationContext(),
                R.layout.layout_chatmessage_listitem, mMessagesList);
        mListView.setAdapter(mAdapter);
        mListView.setSelection(mAdapter.getCount() - 1);
    }

    private void initMessagesList(){
//        if(mMessagesList.size()>0){
//            String title = mMessagesList.get(mMessagesList.size()-1).getDisplay_name();
//            String msg = mMessagesList.get(mMessagesList.size()-1).getMessage();
//            Log.v("msg",title + msg);
//
//            setDataForSimpleNotification(title,msg);
//        }

        mAdapter = new ChatMessageListAdapter(getApplicationContext(),
                R.layout.layout_chatmessage_listitem, mMessagesList);
        mListView.setAdapter(mAdapter);
        mListView.setSelection(mAdapter.getCount() - 1); //scroll to the bottom of the list
    }

    /**
     * Return the current timestamp in the form of a string
     * @return
     */
    private String getTimestamp(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));
        return sdf.format(new Date());
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /*
            ----------------------------- Firebase setup ---------------------------------
    */

    @Override
    protected void onResume() {
        super.onResume();
        checkAuthenticationState();
    }

    private void checkAuthenticationState(){
        Log.d(TAG, "checkAuthenticationState: checking authentication state.");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user == null){
            Log.d(TAG, "checkAuthenticationState: user is null, navigating back to login screen.");

            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }else{
            Log.d(TAG, "checkAuthenticationState: user is authenticated.");
        }
    }

    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: started.");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());


                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    Toast.makeText(getApplicationContext(), "Signed out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
                // ...
            }
        };

    }

    /**
     * upadte the total number of message the user has seen
     */
    private void updateNumMessages(int numMessages){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        reference
                .child(getString(R.string.dbnode_chatrooms))
                .child(mChatroom.getChatroom_id())
                .child(getString(R.string.field_users))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(getString(R.string.field_last_message_seen))
                .setValue(String.valueOf(numMessages));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMessagesReference.removeEventListener(mValueEventListener);
    }

    ValueEventListener mValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
         //   getChatroomMessages();
            Boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .getBoolean("isFirstRun", true);
            if(isFirstRun){
                getChatroomMessages();
                getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit().putBoolean("isFirstRun",
                        false).apply();
            }else {
                setChatroomMessages();

//                getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit().putBoolean("isFirstRun",
//                        false).apply();
            }

            //get the number of messages currently in the chat and update the database
            int numMessages = (int) dataSnapshot.getChildrenCount();
            updateNumMessages(numMessages);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    private void enableChatroomListener(){
         /*
            ---------- Listener that will watch the 'chatroom_messages' node ----------
         */
        mMessagesReference = FirebaseDatabase.getInstance().getReference().child(getString(R.string.dbnode_chatrooms))
                .child(mChatroom.getChatroom_id())
                .child(getString(R.string.field_chatroom_messages));

        mMessagesReference.addValueEventListener(mValueEventListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
        isActivityRunning = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
        isActivityRunning = false;
    }

    private void addNotification(String title ,String msg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int notifyID = 1;
            String CHANNEL_ID = "my_channel_01";// The id of the channel.
            CharSequence name = "Android";// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            // Create a notification and set the notification channel.
            Notification notification = new Notification.Builder(ChatroomActivity.this)
                    .setContentTitle(title)
                    .setContentText("You've received new messages." + msg)
                    .setSmallIcon(R.drawable.ic_speech_bubble)
                    .setChannelId(CHANNEL_ID)
                    .build();

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(mChannel);

            // Issue the notification.
            mNotificationManager.notify(notifyID, notification);

        }else {
            Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    //.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.ic_planet_tissue))
                    .setContentTitle(title)
                    .setContentText("You've received new messages." + msg)
                    .setSmallIcon(R.drawable.ic_speech_bubble)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri);


            NotificationManager  manager1 = (NotificationManager)this.getSystemService(NOTIFICATION_SERVICE);

            manager1.notify(0,notificationBuilder.build());
        }
    }


    private void sendNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(contentIntent);
        Notification notification = notificationBuilder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_SOUND;
        currentNotificationID++;
        int notificationId = currentNotificationID;
        if (notificationId == Integer.MAX_VALUE - 1)
            notificationId = 0;
        notificationManager.notify(notificationId, notification);
    }


//    @Override
//    public void onBackPressed() {
//        getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit().putBoolean("isFirstRun",
//                true).apply();
//        finish();
//    }

}
