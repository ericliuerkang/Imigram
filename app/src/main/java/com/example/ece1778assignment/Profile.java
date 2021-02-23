package com.example.ece1778assignment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

public class Profile extends AppCompatActivity {
    //Defined parameters
    private static final String TAG = "Profile Activity";

    //Authentication stuff
    FirebaseAuth mAuth;
    FirebaseUser user;

    //Firebase firestore
    FirebaseFirestore db;

    //Firebase storage
    FirebaseStorage storage;
    StorageReference storageRef;

    //Recyclerview stuff
    RecyclerView photoList;
    List<Post> listPhotos = new ArrayList<>();
    List<String> seen = new ArrayList<>();
    PhotoAdapter adapter;

    int setImagesStatus;

    public void feedOnClick(View view) {
        startActivity(new Intent(this, Feed.class));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "on create");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //initialize authentication stuff
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        assert user != null;

        //initialize firestore and storage
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        Log.d(TAG, "initialized firstore and storage");

        //setup recyclerview stuff
        adapter = new PhotoAdapter(this, listPhotos);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        photoList = findViewById(R.id.photoRecyclerView);
        photoList.setLayoutManager(gridLayoutManager);
        photoList.setAdapter(adapter);
        Log.d(TAG, "initialized adapter");

        //setup username, profile photo, bio, images dispaly
        TextView userName = findViewById(R.id.textview_username);
        userName.setText(user.getDisplayName());
        setProfile();
        Log.d(TAG, "set profile");
        setImages();
        Log.d(TAG, "set images");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "on resume");
        setProfile();
        setImages();
    }

    public void setProfile(){
        /*
        set the profile uri and bio String from the "users" collection of firestore
        set up the profile photo with glide
         */

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
                                String path = document.getString("profile_photo");
                                Log.d(TAG, "path: "+path);
                                if (path == null){return;}
                                StorageReference storageReference = storageRef.child(path);

                                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        Log.d(TAG, "profile photo load success");
                                        Glide.with(profilePhoto.getContext() /* context */)
                                                .load(uri)
                                                .into(profilePhoto);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        Log.d(TAG, "Failed to load profile photo");
                                    }
                                });

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

    //sign out button onClick
    public void signOut(View view) {
        mAuth.signOut();
        startActivity(new Intent(this, MainActivity.class));
    }

    public void addPhotoOnClick(View view) {
        startActivity(new Intent(this, AddPhoto.class));
    }

    private void setImages(){
        CountDownLatch done = new CountDownLatch(1);
        db.collection("photos")
                .whereEqualTo("uid", user.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int count = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                count ++;
                                String uid = (String) document.get("uid");
                                String path = (String) document.get("storageRef");
                                String caption = (String) document.get("caption");
                                String comments = (String) document.get("comment");
                                String hashtag = (String) document.get("hashtag");
                                long timestamp = (Long) document.get("timestamp");
                                Post post = new Post(uid, path, caption, hashtag, comments, timestamp);
                                if (! seen.contains(path)){
                                    seen.add(path);
                                    listPhotos.add(post);
                                }
                            }
                            Collections.sort(listPhotos);
                            Log.d(TAG, "Count: "+String.valueOf(count));
                            adapter.notifyDataSetChanged();
                            done.countDown();
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
        try{
            done.await();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}