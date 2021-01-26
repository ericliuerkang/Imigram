package com.example.ece1778assignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class Profile extends AppCompatActivity {
    private static final String TAG = "Profile Activity";
    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;

    RecyclerView photoList;
    List<String> photoPaths;
    List<Integer> tmpPhotos;
    PhotoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        assert user != null;
        TextView userName = findViewById(R.id.textview_username);
        userName.setText(user.getDisplayName());

        db = FirebaseFirestore.getInstance();
        setProfile();

//        profilePhoto.setImageBitmap();


        photoList = findViewById(R.id.photoRecyclerView);
        tmpPhotos = new ArrayList<>();

        tmpPhotos.add(R.drawable.ic_baseline_add_photo_alternate_24);
        tmpPhotos.add(R.drawable.ic_baseline_add_photo_alternate_24);
        tmpPhotos.add(R.drawable.ic_baseline_add_photo_alternate_24);
        tmpPhotos.add(R.drawable.ic_baseline_add_photo_alternate_24);

        adapter = new PhotoAdapter(this, tmpPhotos);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        photoList.setLayoutManager(gridLayoutManager);
        photoList.setAdapter(adapter);
    }

    public void setProfile(){
        ImageView profilePhoto = findViewById(R.id.profile_photo);
        TextView bio = findViewById(R.id.textview_bio);

        db.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
//                                Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                                String path = document.getString("profile_photo");
                                Log.d(TAG, "path: "+path);
                                profilePhoto.setImageBitmap(BitmapFactory.decodeFile(path+""));
                                bio.setText(document.getString("bio"));
                            } else {
                                Log.d(TAG, "No such document");
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                        }
                    }
                });
    }

    public void signOut(View view) {
        mAuth.signOut();
        startActivity(new Intent(this, MainActivity.class));
    }

    public void addPhoto(View view) {
    }
}