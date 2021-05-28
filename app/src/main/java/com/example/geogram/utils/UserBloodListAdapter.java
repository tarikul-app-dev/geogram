package com.example.geogram.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.geogram.R;
import com.example.geogram.models.User;
import com.example.geogram.models.UserAccountSettings;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserBloodListAdapter extends ArrayAdapter<UserAccountSettings> {

    private static final String TAG = "UserBloodListAdapter";

    private LayoutInflater mInflater;
    private List<UserAccountSettings> mUsers = null;
    private int layoutResource;
    private Context mContext;

    public UserBloodListAdapter(@NonNull Context context, int resource, @NonNull List<UserAccountSettings> objects) {
        super(context, resource, objects);

        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutResource = resource;
        this.mUsers = objects;
    }

    private static class ViewHolder {
        TextView display_name, last_location, blood_group, phone;
        CircleImageView profileImage;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        final ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(layoutResource, parent, false);

            holder = new ViewHolder();

            holder.display_name = (TextView) convertView.findViewById(R.id.displayName);
            holder.last_location = (TextView) convertView.findViewById(R.id.last_location);
            holder.blood_group = (TextView) convertView.findViewById(R.id.blood_group);
            holder.phone = (TextView) convertView.findViewById(R.id.phone);
            holder.profileImage = (CircleImageView) convertView.findViewById(R.id.profile_imagee);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.display_name.setText(getItem(position).getDisplay_name());
        holder.blood_group.setText(getItem(position).getWebsite());
        holder.last_location.setText(getItem(position).getDescription());
        holder.phone.setText((getItem(position).getPhone_number()));

        holder.phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String call = holder.phone.getText().toString();
                if (call.isEmpty()) {
                    Toast.makeText(mContext, "select virtual speaking option ...!", Toast.LENGTH_LONG).show();
                } else {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + call));
                    mContext.startActivity(callIntent);
                }

            }
        });

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(mContext.getString(R.string.dbname_user_account_settings))
                .orderByChild(mContext.getString(R.string.field_user_id))
                .equalTo(getItem(position).getUser_id());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot singleSnapshot : snapshot.getChildren()) {
                    Log.d(TAG, "onDataChange: found user" +
                            singleSnapshot.getValue(UserAccountSettings.class).toString());

                    ImageLoader imageLoader = ImageLoader.getInstance();

                    imageLoader.displayImage(singleSnapshot.
                                    getValue(UserAccountSettings.class).getProfile_photo()
                            , holder.profileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return convertView;
    }
}
