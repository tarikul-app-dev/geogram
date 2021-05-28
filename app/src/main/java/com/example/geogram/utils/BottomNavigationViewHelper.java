package com.example.geogram.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.example.geogram.like.LikesActivity;
import com.example.geogram.homee.MainActivity;

import com.example.geogram.Prof.ProfileActivity;
import com.example.geogram.R;
import com.example.geogram.searchh.SearchActivity;
import com.example.geogram.sharee.ShareActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

public class BottomNavigationViewHelper {
    private static final String TAG = "BottomNavigationViewHel";

    public static void setupBottomNavigaitonView(BottomNavigationViewEx bottomNavigationViewEx){

        Log.d(TAG,"setUpBottomNavigaitonView: setting up bottom Navigation view...");

        bottomNavigationViewEx.enableAnimation(false);
         bottomNavigationViewEx.enableItemShiftingMode(false);
         bottomNavigationViewEx.enableShiftingMode(false);
         bottomNavigationViewEx.setTextVisibility(false);
    }

    public static void enableNavigation(final Context context, final Activity callingActivity,BottomNavigationViewEx view){
        view.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()){
                    case R.id.home:
                      context.startActivity(new Intent(context, MainActivity.class));//ACTIVITY_NUM = 0
                        callingActivity.overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
                        break;
                    case R.id.search:
                        context.startActivity(new Intent(context, SearchActivity.class));//ACTIVITY_NUM = 1
                        callingActivity.overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
                        break;
                    case R.id.add:
                        context.startActivity(new Intent(context, LikesActivity.class));//ACTIVITY_NUM = 2
                        callingActivity.overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
                        break;
                    case R.id.aleart:
                        context.startActivity(new Intent(context, ShareActivity.class));//ACTIVITY_NUM = 3
                        callingActivity.overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
                        break;
                    case R.id.profile:
                        context.startActivity(new Intent(context, ProfileActivity.class));//ACTIVITY_NUM = 4
                        callingActivity.overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
                        break;

                }
                return false;
            }
        });
    }
}
