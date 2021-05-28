package com.example.geogram.Prof;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.geogram.Login.LoginActivity;
import com.example.geogram.R;
import com.example.geogram.homee.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignOutFragment extends Fragment {
    private static final String TAG = "SignOutFragment";

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private ProgressBar mProgressBar;
    private TextView tvSignout,tvSigningout;
    private Button btnConfirmSignout;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_signout,container,false);
        tvSignout = (TextView) view.findViewById(R.id.tvConfirmSignout);
        btnConfirmSignout = (Button) view.findViewById(R.id.btnConfirmSignout);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressbar);
        tvSigningout = (TextView) view.findViewById(R.id.tvsigningout);

        mProgressBar.setVisibility(View.GONE);
        tvSigningout.setVisibility(View.GONE);

        setupFirebaseAuth();

        btnConfirmSignout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: attemping to sign out...");

                mProgressBar.setVisibility(View.GONE);
                tvSigningout.setVisibility(View.GONE);
               mAuth.signOut();
               getActivity().finish();
            }
        });
        return view;
    }

    /*
  ----------------------------firebase-----------------------
   */
    /*
     Setting up Firebase Auth object
    */
    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth...");
        mAuth = FirebaseAuth.getInstance();

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

                    Log.d(TAG, "onAuthStateChanged: Navigating back to login screen...");
                    Intent intent = new Intent(getActivity(),LoginActivity.class);
                    //new task start hobe and j intent ta ase seta clear hobe.clear na hole onk somoy
                    //hang kora r ekta posibility theke jai.
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
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
        if(mAuthListener!=null){
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
