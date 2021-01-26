package com.example.ece1778assignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private static final String TAG = "Main Activity";
    Intent profileIntent;
    Intent registerIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        Log.d(TAG, "onCreate");

        profileIntent = new Intent(this, Profile.class);
        registerIntent = new Intent(this, Register.class);

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (isLogedIn(mAuth)) {
            startActivity(profileIntent);
        }
    }

    public boolean isLogedIn(FirebaseAuth mAuth) {
        return mAuth.getCurrentUser() != null;
    }

    public void loginOnClick(View view) {
        Log.d(TAG, "login onClick");

        ArrayList<String> listMissing = new ArrayList<>();

        TextView emailTextView = findViewById(R.id.login_email);
        String email = emailTextView.getText().toString();
        if (email==null || !email.contains("@")) {  listMissing.add("email");  }

        TextView passwordTextView = findViewById(R.id.login_password);
        String password = passwordTextView.getText().toString();
        if (password==null || password.length()<=1 || password.length()>32) {  listMissing.add("password");  }

        if (!listMissing.isEmpty()) {
            Log.d(TAG, "listMissing not empty");
            StringBuilder text = new StringBuilder("Please enter valid values for the following fields: ");
            for (String field:
                    listMissing) {
                text.append(field).append(", ");
            }
            Toast.makeText(MainActivity.this, text.toString().substring(0, text.toString().length()-2),
                    Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).
                addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(profileIntent);
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentification failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void signupOnClick(View view) {
        Log.d(TAG, "Signup onClick");
        startActivity(registerIntent);
    }
}