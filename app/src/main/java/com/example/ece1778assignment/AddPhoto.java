package com.example.ece1778assignment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

public class AddPhoto extends AppCompatActivity {
    private static final String TAG = "AddPhoto";
    static final int REQUEST_IMAGE_CAPTURE = 1;

    ImageView addPhotoPhoto;
    Bitmap photo;

    FirebaseUser user;

    //Firebase firestore
    FirebaseFirestore db;

    //Firebase storage
    FirebaseStorage storage;
    StorageReference storageRef;

    String ht = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_photo);

        addPhotoPhoto = findViewById(R.id.addPhotoPhoto);
        addPhotoPhoto.setImageResource(R.drawable.ic_baseline_add_photo_alternate_24);

        user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;

        //initialize firestore and storage
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    //add photo button onClick
    public void addPhoto(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(AddPhoto.this, "Requesting Camera Access", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA
                    },
                    REQUEST_IMAGE_CAPTURE);
        }
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult");

        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null){
            Bundle bundle = data.getExtras();
            photo = (Bitmap) bundle.get("data");
            addPhotoPhoto.setImageBitmap(photo);
        }
    }

    private void saveImage(Bitmap image, String caption, String hashtag, String uid){
        String pid = UUID.randomUUID().toString();
        String name = pid+".jpg";
        String path = uid+"/"+name;
        StorageReference profileRef = storageRef.child(path);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 10, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = profileRef.putBytes(data);
        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                Log.d(TAG, "saveImage task completed");
                if (!task.isSuccessful()){
                    Log.d(TAG, "task failed");
                }
                storePath(pid, caption, hashtag, uid);
            }
        });
    }

    private void storePath(String pid, String caption, String hashtag, String uid){
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("storageRef", uid+"/"+pid+".jpg");
        map.put("caption", caption);
        map.put("hashtag", hashtag);
        map.put("comment", "-");
        map.put("timestamp", currentTimeMillis());

        db.collection("photos").document(pid)
                .set(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Photo path added to firestore");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }

    public void postOnClick(View view) {
        TextView captionTV = findViewById(R.id.captionTV);
        String caption = captionTV.getText().toString();
        TextView htTV = findViewById(R.id.hashTag);
        String hashtag = htTV.getText().toString();
        saveImage(photo, caption, hashtag, user.getUid());
        startActivity(new Intent(this, Profile.class));
    }

    public void generateHashtagOnClick(View view) {
        ht = "";
        if (photo == null) {
            Toast.makeText(AddPhoto.this, "Take a photo first", Toast.LENGTH_SHORT).show();
            return;
        }

        TextView htTV = findViewById(R.id.hashTag);

        InputImage image = InputImage.fromBitmap(photo, 0);

        ImageLabelerOptions options =
                new ImageLabelerOptions.Builder()
                        .setConfidenceThreshold(0.7f)
                        .build();
        ImageLabeler labeler = ImageLabeling.getClient(options);

        labeler.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                    @Override
                    public void onSuccess(List<ImageLabel> labels) {
                        Log.d("Hashtag Generator", "success");
                        for (ImageLabel label : labels) {
                            String text = label.getText();
                            float confidence = label.getConfidence();
                            assert(confidence>=0.7);
                            ht = ht + "#" + text + "\n";
                        }
                        htTV.setText(ht);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        ht = "failed";
                        Log.d("Hashtag Generator", e.toString());
                    }
                });

//        while (ht.length()==0){
//            try {
//                TimeUnit.MICROSECONDS.sleep(5);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        Log.d("Hashtag Generator", "returning: "+ ht);
//        return ht;

    }
}