package com.example.geogram.homee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.example.geogram.Login.LoginActivity;
import com.example.geogram.R;
import com.example.geogram.models.Photo;
import com.example.geogram.models.UserAccountSettings;
import com.example.geogram.utils.MainfeedListAdapter;
import com.example.geogram.utils.SectionsPagerAdapter;
import com.example.geogram.utils.UnivarsalImageHolder;
import com.example.geogram.utils.ViewCommentsFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import com.example.geogram.utils.BottomNavigationViewHelper;
import com.nostra13.universalimageloader.core.ImageLoader;

public class MainActivity extends AppCompatActivity implements MainfeedListAdapter.OnLoadMoreItemListener {

    @Override
    public void onLoadMoreItems() {
        Log.d(TAG, "onLoadMoreItems: displaying more photos");
        HomeFragment fragment = (HomeFragment)getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.viewpager_container + ":" + mViewPager.getCurrentItem());//its a default tag ,comment fragment e tag create korecilam but ekhane default ta e use korsi
        if(fragment != null){
            fragment.displayMorePhotos();
        }
    }

    private Context mContext = MainActivity.this;
    private static final int ACTIVITY_NUM = 0;
    private static final int MAIN_ACTIVITY = 1;
    private static final String TAG = "MainActivity";
    // Initialize Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    //widgets
    private ViewPager mViewPager;
    private FrameLayout mFrameLayout;
    private RelativeLayout mRelativeLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG,"onCreate: starting...");

        mViewPager = (ViewPager) findViewById(R.id.viewpager_container);
        mFrameLayout = (FrameLayout) findViewById(R.id.container);
        mRelativeLayout = (RelativeLayout) findViewById(R.id.relLayoutParent);

        initImageLoader();
        setUpBottomNavigationView();
        setupViewPager();
        setupFirebaseAuth();
        //mAuth.signOut();
    }
    
    public void onCommentThreadSelected(Photo photo,String callingActivity){
        Log.d(TAG, "onCommentThreadSelected: selected a comment thread...");

        ViewCommentsFragment fragment = new ViewCommentsFragment();
        Bundle args = new Bundle();
        args.putParcelable(getString(R.string.photo),photo);
        args.putString(getString(R.string.home_activity),getString(R.string.home_activity)); //key and value
        fragment.setArguments(args);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container,fragment);
        transaction.addToBackStack(getString(R.string.view_comments_fragment));
        transaction.commit();

    }

    public void hideLayout(){
        Log.d(TAG, "hideLayout: hidding layout");
        mRelativeLayout.setVisibility(View.GONE);
        mFrameLayout.setVisibility(View.VISIBLE);
    }

    public void showLayout(){
        Log.d(TAG, "hideLayout: showing layout");
        mRelativeLayout.setVisibility(View.VISIBLE);
        mFrameLayout.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(mFrameLayout.getVisibility() == View.VISIBLE){
            showLayout();
        }
    }

    private void initImageLoader() {
        UnivarsalImageHolder univarsalImageHolder = new UnivarsalImageHolder(mContext);
        ImageLoader.getInstance().init(univarsalImageHolder.getConfig());
    }

    /**
     * Response for adding the 3 tabs: 3 tabs means the fragments
     * camera,home,message
     *
     */
    private void setupViewPager(){
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getSupportFragmentManager());
         adapter.addFragment(new CameraFragment());//index 0
         adapter.addFragment(new HomeFragment());//index 1
         adapter.addFragment(new MessagesFragment());//index 2
        mViewPager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        tabLayout.getTabAt(0).setIcon(R.drawable.camera);
        tabLayout.getTabAt(1).setText("ONE STOP");
        tabLayout.getTabAt(2).setIcon(R.drawable.send);
    }
    /**
     * Bottom NavigationView setUp
     */
    private void setUpBottomNavigationView(){
        Log.d(TAG,"setUpBottomNavigaitonView: setting up bottom Navigation view...");

        BottomNavigationViewEx bottomNavigationViewEx = (BottomNavigationViewEx) findViewById(R.id.bottomNavViewBar);
        BottomNavigationViewHelper.setupBottomNavigaitonView(bottomNavigationViewEx);
        BottomNavigationViewHelper.enableNavigation(mContext,this,bottomNavigationViewEx);

        /**
         *Inthe Bottom Navigation View the switch case method
         * we count the menuItem
         */
        Menu menu = bottomNavigationViewEx.getMenu();
        MenuItem menuItem = menu.getItem(ACTIVITY_NUM);
        menuItem.setChecked(true);
    }


    /*
   ----------------------------firebase-----------------------
    */
    private void checkCurrentUser(FirebaseUser user){
        Log.d(TAG, "checkCurrentUser: checking if user is logged in or not");

        if(user==null ){
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }
    }
    /*
     Setting up Firebase Autentication object
    */
    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth...");
        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                //check if the user is logged in
                checkCurrentUser(user);
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

        mViewPager.setCurrentItem(MAIN_ACTIVITY);

        checkCurrentUser(mAuth.getCurrentUser());
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mAuthListener!=null){
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }


}