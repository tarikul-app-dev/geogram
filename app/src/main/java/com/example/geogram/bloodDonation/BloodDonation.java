package com.example.geogram.bloodDonation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.geogram.Prof.ProfileActivity;
import com.example.geogram.R;
import com.example.geogram.models.User;
import com.example.geogram.models.UserAccountSettings;
import com.example.geogram.searchh.SearchActivity;
import com.example.geogram.utils.BottomNavigationViewHelper;
import com.example.geogram.utils.UserBloodListAdapter;

import com.example.geogram.utils.UserListAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BloodDonation extends Fragment {
    private static final String TAG = "BloodDonation";

    private Context mContext = getActivity();

    //widgets
    private EditText mSearchParam;
    private ListView mListView;

    //varals
    private List<UserAccountSettings> mUserList;

    //adapter class
    private UserBloodListAdapter mAdapter;

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){

        View view = inflater.inflate(R.layout.fragment_blood_donation, container, false);

        mSearchParam = (EditText) view.findViewById(R.id.search);
        mListView = (ListView) view.findViewById(R.id.listView);

        Log.d(TAG, "onCreate: started");
        //setupFirebaseAuth();
        //hideSoftKeyboard();
        //setUpBottomNavigationView();
        initTextListener();


        return view;
    }

    /**
     * we create a new method which is going to initialize
     * our text change listener for our search bar
     */
    private void initTextListener() {
        Log.d(TAG, "initTextListener: initializing");

        mUserList = new ArrayList<>();

        mSearchParam.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String text = mSearchParam.getText().toString().toLowerCase(Locale.getDefault());//Locale.getDefault()
                searchForMatch((text));
            }
        });
    }

    private void searchForMatch(String keyword) {
        Log.d(TAG, "searchForMatch: searching for a match: " + keyword);

        mUserList.clear();
        //update the users list view
        if (keyword.length() == 0) {
            Toast.makeText(getActivity(), "Please enter your searching blood...!'", Toast.LENGTH_SHORT).show();
        }
        else {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
            Query query = reference.child(getString(R.string.dbname_user_account_settings))
                    .orderByChild(getString(R.string.field_website))
                    //.child(getString(R.string.field_website))
                    //.child(getString(R.string.field_description));
                    .equalTo(keyword);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot singleSnapshot : snapshot.getChildren()) {
                        Log.d(TAG, "onDataChange: found user: " + singleSnapshot.getValue(UserAccountSettings.class).toString());

                        mUserList.add(singleSnapshot.getValue(UserAccountSettings.class));
                        //update the users list view
                        updateUsersList();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

    }

    private void updateUsersList() {
        Log.d(TAG, "updateUsersList: updating users list..");

        if (getActivity() != null) {
            mAdapter = new UserBloodListAdapter(getActivity(), R.layout.layout_blood_listitem, mUserList);

            mListView.setAdapter(mAdapter);
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "onItemClick: selected user: " + mUserList.get(i).toString());

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity(), R.style.Theme_AppCompat_Light_Dialog);

                alertDialogBuilder.setTitle("what you want to do ...?")
                        .setMessage("Dear valuable user....!\nChoose your valuable option.....")
                        .setPositiveButton("Call", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(getActivity(), "negative button", Toast.LENGTH_SHORT).show();

                            }
                        })
                        .setNeutralButton("donate Reputation", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

                //navigating to profile activity

                /*Intent intent = new Intent(getActivity(), ProfileActivity.class);
                intent.putExtra(getString(R.string.calling_activity),getString(R.string.search_activity));
                intent.putExtra(getString(R.string.intent_user),mUserList.get(i));
                startActivity(intent);

                Toast.makeText(mContext, "Pressed", Toast.LENGTH_SHORT).show();*/
            }
        });

    }

    //close the keyboard
    private void hideSoftKeyboard() {

       /* if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }*/
        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * Bottom NavigationView setUp

     private void setUpBottomNavigationView(){
     Log.d(TAG,"setUpBottomNavigaitonView: setting up bottom Navigation view...");

     BottomNavigationViewEx bottomNavigationViewEx = (BottomNavigationViewEx) findViewById(R.id.bottomNavViewBar);
     BottomNavigationViewHelper.setupBottomNavigaitonView(bottomNavigationViewEx);
     BottomNavigationViewHelper.enableNavigation(mContext,this,bottomNavigationViewEx);

     /**
     *Inthe Bottom Navigation View the switch case method
     * we count the menuItem
     Menu menu = bottomNavigationViewEx.getMenu();
     MenuItem menuItem = menu.getItem(ACTIVITY_NUM);
     menuItem.setChecked(true);
     }

    public void showPopUp(){
        final PopupMenu popup = new PopupMenu();
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                popup.inflate(R.menu.popupmenu_for_blood);
                popup.show();
                switch (menuItem.getItemId()) {
                    case R.id.item1:
                        Toast.makeText(mContext, "pressed", Toast.LENGTH_SHORT).show();
                        return true;
                }
return false;
            }
        });*/


}
