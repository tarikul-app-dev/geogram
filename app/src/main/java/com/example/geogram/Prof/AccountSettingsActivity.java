package com.example.geogram.Prof;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.geogram.OnlinePlatform.shoppingPlatform;
import com.example.geogram.OnlineQuizing.OnlineQuizFragment;
import com.example.geogram.R;
import com.example.geogram.bloodDonation.BloodDonation;
import com.example.geogram.utils.BottomNavigationViewHelper;
import com.example.geogram.utils.FirebaseModels;
import com.example.geogram.utils.SectionStatePagerAdapter;
import com.example.geogram.utils.UnivarsalImageHolder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.util.ArrayList;

public class AccountSettingsActivity extends AppCompatActivity {

    private static final String TAG = "AccountSettingsActivity";
    private static final int ACTIVITY_NUM = 4;
    private Context mContext;
    public SectionStatePagerAdapter pagerAdapter;
    private ViewPager mViewPager;
    private RelativeLayout mRelativeLayout;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accountsettings);

        mContext = AccountSettingsActivity.this;

        Log.d(TAG, "onCreate: Started...");


        mViewPager = (ViewPager) findViewById(R.id.viewpager_container);
        mRelativeLayout = (RelativeLayout) findViewById(R.id.rellayout1);

        setupSettingsList();
        setUpBottomNavigationView();
        setupFragments();

        getIncomingIntent();
        //set up backArrow for navigating back to profile Activity

        ImageView backArrow = (ImageView) findViewById(R.id.backArrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: navigating back to 'Profile Activity'");
                finish();
            }
        });
    }
    private void getIncomingIntent(){
        Intent intent = getIntent();

        if(intent.hasExtra(getString(R.string.selected_image)) //handeling null pointer exception
                 ||intent.hasExtra(getString(R.string.selected_bitmap))) {
            //if there is an imageUrl attached as an extra then it was chosen from the gallery/photo fragment
            Log.d(TAG, "getIncomingIntent: New incoming imageUrl.");

            if (intent.getStringExtra(getString(R.string.return_to_fragment)).equals(getString(R.string.edit_profile_fragment))) {

                if (intent.hasExtra(getString(R.string.selected_image))) {
                    //set the new profile picture
                    FirebaseModels firebaseModels = new FirebaseModels(AccountSettingsActivity.this);
                    firebaseModels.uploadNewPhoto(getString(R.string.profile_photo), null, 0,
                            intent.getStringExtra(getString(R.string.selected_image)), null);
                } else if (intent.hasExtra(getString(R.string.selected_bitmap))) {
                    //set the new profile picture
                    FirebaseModels firebaseModels = new FirebaseModels(AccountSettingsActivity.this);
                    firebaseModels.uploadNewPhoto(getString(R.string.profile_photo), null, 0,
                            null, (Bitmap) intent.getParcelableExtra(getString(R.string.selected_bitmap)));
                }

            }
        }

        if(intent.hasExtra(getString(R.string.calling_activity))){
            Log.d(TAG, "getIncomingIntent: receive incoming intent from " + getString(R.string.profile_activity));
            setViewPager(pagerAdapter.getFragmentNumber(getString(R.string.edit_profile_fragment)));
        }
    }

    /**
     * set up fragments
     */
    private void setupFragments(){
        pagerAdapter = new SectionStatePagerAdapter(getSupportFragmentManager());
        pagerAdapter.addFragment(new SignOutFragment(),getString(R.string.sign_out));//fragment 0
        pagerAdapter.addFragment(new EditProfileFragment(),getString(R.string.edit_profile));//fragment 1
        pagerAdapter.addFragment(new BloodDonation(),getString(R.string.blood_donation));//fragment 2
        pagerAdapter.addFragment(new OnlineQuizFragment(),getString(R.string.online_quiz));//fragment 3
        pagerAdapter.addFragment(new shoppingPlatform(),getString(R.string.online_shopping_platform));//fragment 4
    }

    public void setViewPager(int fragmentNumber){
        mRelativeLayout.setVisibility(View.GONE);
        Log.d(TAG, "setViewPager: navigating to fragment #: "+fragmentNumber);
        mViewPager.setAdapter(pagerAdapter);
        mViewPager.setCurrentItem(fragmentNumber);
    }

    private void setupSettingsList() {
        Log.d(TAG, "setupSettingsList: initialing 'Accouunt Settings' list.");

        ListView listView = (ListView) findViewById(R.id.listAccountSettings);
        ArrayList<String> options = new ArrayList<>();

        options.add(getString(R.string.sign_out));//fragment 0
        options.add(getString(R.string.edit_profile));//fragment 1
        options.add(getString(R.string.blood_donation));//fragment 2
        options.add(getString(R.string.online_quiz));//fragment 3
        options.add(getString(R.string.online_shopping_platform));//fragment 4

        ArrayAdapter adapter = new ArrayAdapter(mContext, android.R.layout.simple_list_item_1, options);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Log.d(TAG, "onItemClick: navigating to fragment #"+position);
                 setViewPager(position);
            }
        });
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



}
