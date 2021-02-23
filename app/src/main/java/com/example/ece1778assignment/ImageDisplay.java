package com.example.ece1778assignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ImageDisplay extends AppCompatActivity {
    private final String TAG = "ImageDisplay";
    StorageReference storageRef = FirebaseStorage.getInstance().getReference();
    String uid;
    String imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);

        uid = getIntent().getStringExtra("uid");
        ImageView deleteButton = findViewById(R.id.deleteImage);
        if (uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            deleteButton.setVisibility(View.INVISIBLE);
        }

        imagePath = getIntent().getStringExtra("imagePath");
        ImageView view = findViewById(R.id.imageView3);
        StorageReference storageReference = storageRef.child(imagePath);

        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Log.d(TAG, "Successfully loaded uri");
                Glide.with(view.getContext() /* context */)
                        .load(uri)
                        .into(view);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d("Profile Activity", "Failed to load uri");
            }
        });

        String caption = getIntent().getStringExtra("caption");
        TextView captionTV = findViewById(R.id.displayCaption);
        captionTV.setText(caption);

        String comments = getIntent().getStringExtra("comment");
        updateComments(comments);

        String hashtag = getIntent().getStringExtra("hashtag");
        TextView htTV = findViewById(R.id.displayHashtag);
        htTV.setText(hashtag);
    }

    private void updateComments(String comments){
        TextView commentTV = findViewById(R.id.displayComments);
        commentTV.setText(comments);
    }

    public void addCommentOnClick(View view) {
        TextView newCommentTV = findViewById(R.id.newComment);
        String newComment = newCommentTV.getText().toString();
        FirebaseFirestore.getInstance().collection("photos")
                .whereEqualTo("storageRef", imagePath)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                String comments = (String) document.get("comment");
                                assert(comments!=null);
                                comments = comments + FirebaseAuth.getInstance().getCurrentUser().getDisplayName() + ": " + newComment + " \n-";

                                Map<Object, String> map = new HashMap<>();
                                map.put("comment", comments);

                                Log.d(TAG, "New Comment: "+comments);

                                FirebaseFirestore.getInstance().collection("photos").document(document.getId()).set(map, SetOptions.merge());

                                updateComments(comments);
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    public void deleteImageOnClick(View view) {
        FirebaseFirestore.getInstance().collection("photos")
                .whereEqualTo("storageRef", imagePath)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int count = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                FirebaseFirestore.getInstance().collection("photos").document(document.getId()).delete();
                            }

                            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(imagePath);
                            storageReference.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Success");
                                    } else {
                                        // Task failed with an exception
                                        Exception exception = task.getException();
                                        Log.d(TAG, exception.toString());
                                    }
                                    ImageDisplay.super.onBackPressed();
                                }
                            });
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }
}