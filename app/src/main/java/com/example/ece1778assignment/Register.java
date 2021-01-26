package com.example.ece1778assignment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Register extends AppCompatActivity {
    private static final String TAG = "Register Activity";
    private FirebaseAuth mAuth;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    ImageView profilePhoto;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    Map<String, Object> userInfo = new HashMap<>();
    Bitmap photo;
    String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Log.d(TAG, "Register onCreate");

        mAuth = FirebaseAuth.getInstance();
        Intent intent = getIntent();

        profilePhoto = findViewById(R.id.register_profile_photo);
    }

    public void createAccount(View view) {
        Log.d(TAG, "create account");

        ArrayList<String> listMissing = new ArrayList<>();

        //get email and password from TextView
        TextView emailTextView = findViewById(R.id.register_email);
        String email = emailTextView.getText().toString();
        if (email==null || !email.contains("@")) {  listMissing.add("email");  }

        TextView passwordTextView = findViewById(R.id.register_password);
        String password = passwordTextView.getText().toString();
        if (password==null || password.length()<=1 || password.length()>32) {  listMissing.add("password");  }

        TextView usernameTextView = findViewById(R.id.register_username);
        String username = usernameTextView.getText().toString();
        Log.d(TAG, "got username: "+username);
        if (username==null || username.length()<1 || username.length()>50) {  listMissing.add("username");  }

        TextView bioTextView = findViewById(R.id.register_bio);
        String bio = bioTextView.getText().toString();

        TextView passwordConfirmTextView = findViewById(R.id.register_password_confirm);
        if (!passwordConfirmTextView.getText().toString().equals(password)) {
            Log.w(TAG, "password entries mismatch");
            Toast.makeText(Register.this, "Password entries mismatch, please try again",
                    Toast.LENGTH_SHORT).show();
            return;
        }


        if (!listMissing.isEmpty()) {
            Log.d(TAG, "listMissing not empty");
            StringBuilder text = new StringBuilder("Please enter valid values for the following fields: ");
            for (String field:
                 listMissing) {
                text.append(field+", ");
            }
            Toast.makeText(Register.this, text.toString().substring(0, text.toString().length()-2),
                    Toast.LENGTH_LONG).show();
            return;
        }

        userInfo.put("username", username);
        userInfo.put("bio", bio);




        Log.d(TAG, "starting createUser");
        mAuth.createUserWithEmailAndPassword(Objects.requireNonNull(email), Objects.requireNonNull(password))
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "onComplete called");
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            assert user != null;
                            saveToDB(user, userInfo);
                            updateProfile(user, username);
                            mAuth.signInWithEmailAndPassword(email, password).
                                    addOnCompleteListener(Register.this, new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "signInWithEmail:success");
                                                FirebaseUser user = mAuth.getCurrentUser();
                                                startActivity(new Intent(Register.this, Profile.class));
                                            } else {
                                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                            }
                                        }
                                    });
                        } else {
//                            Log.d(TAG, "createUserWithEmail:failure");
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
//                            Toast.makeText(Register.this, "Authentication failed.",
//                                    Toast.LENGTH_LONG).show();

                            try {
                                throw task.getException();
                            } catch(FirebaseAuthInvalidCredentialsException e) {
                                Toast.makeText(Register.this, "Invalid Credentials, please double check", Toast.LENGTH_SHORT).show();
                            } catch(FirebaseAuthUserCollisionException e) {
                                Log.w(TAG, "The account already exists");
                                Toast.makeText(Register.this, "The account already exists, please log in", Toast.LENGTH_SHORT).show();
                            } catch(Exception e) {
                                Log.e(TAG, e.getMessage());
                            }

                        }
                    }
                });
    }

    private void saveToDB(FirebaseUser user, Map<String, Object> map) {

        if (photo != null && map.get("profile_photo")==null) {
            map.put("profile_photo", saveImage(photo, user.getUid()));
        }

        db.collection("users").document(user.getUid())
                .set(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }

    private void updateProfile(FirebaseUser user, String username){
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(username)
//                .setPhotoUri()
                .build();
        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile updated.");
                        }
                    }
                });
    }

    private void updateProfile(FirebaseUser user, String username, Uri uri){
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .setPhotoUri(uri)
                .build();
        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile updated.");
                        }
                    }
                });
    }

    public void setProfilePhoto(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(Register.this, "Requesting Camera Access", Toast.LENGTH_SHORT).show();
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
            profilePhoto.setImageBitmap(photo);
        }
    }

//    private String saveImage(Bitmap finalBitmap, String uid) {
//
//        String root = Environment.getExternalStorageDirectory().toString();
//        File myDir = new File(root + "/"+uid);
//        myDir.mkdirs();
//
//        String fname = "profile_photo"+".jpg";
//
//        File file = new File(myDir, fname);
//        String path = file.getAbsolutePath();
////        if (file.exists()) file.delete ();
//        try {
//            FileOutputStream out = new FileOutputStream(file);
//            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
//            out.flush();
//            out.close();
//            Log.d(TAG, "Success, file path: "+path);
//        } catch (Exception e) {
//            Log.d(TAG, "faaaaaaaaaaaaaaak"+e.toString());
//            e.printStackTrace();
//        }
//        return root+"/"+uid+"/"+fname;
//    }

    private String saveImage(Bitmap bitmapImage, String uid){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,uid+"profile.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "Directory: "+directory.getAbsolutePath());
        return directory.getAbsolutePath()+"/"+uid+"profile.jpg";
    }
}