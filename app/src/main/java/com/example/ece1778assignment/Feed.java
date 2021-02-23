package com.example.ece1778assignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Feed extends AppCompatActivity {
    private static final String TAG = "Feed";
    RecyclerView photoList;
    List<Post> listPhotos = new ArrayList<>();
    List<String> seen = new ArrayList<>();
    PhotoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        adapter = new PhotoAdapter(this, listPhotos);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 1, GridLayoutManager.VERTICAL, false);
        photoList = findViewById(R.id.feedPhotoRecyclerView);
        photoList.setLayoutManager(gridLayoutManager);
        photoList.setAdapter(adapter);
        setImages();
    }

    private void setImages(){
        FirebaseFirestore.getInstance().collection("photos")
//                .whereEqualTo("uid", user.getUid())
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
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    public void profileButtonOnClick(View view) {
        startActivity(new Intent(this, Profile.class));
    }
}