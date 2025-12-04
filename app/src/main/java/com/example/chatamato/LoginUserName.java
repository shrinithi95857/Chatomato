package com.example.chatamato;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chatamato.model.UserModel;
import com.example.chatamato.utils.FirebaseUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginUserName extends AppCompatActivity {

    private EditText usernameInput;
    private Button letMeInBtn;
    private ProgressBar progressBar;
    private String phoneNumber;
    private UserModel userModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_user_name);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        usernameInput = findViewById(R.id.login_name);
        letMeInBtn = findViewById(R.id.login_letmein);
        progressBar = findViewById(R.id.login_username_progress);

        phoneNumber = getIntent().getStringExtra("phone");
        getUsername();

        letMeInBtn.setOnClickListener(v -> setUsername());
    }

    private void setUsername() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty() || username.length() < 3) {
            usernameInput.setError("Username length should be at least 3 characters");
            return;
        }
        setInProgress(true);

        String userId = FirebaseUtil.currentUserId();
        DocumentReference userDocRef = FirebaseUtil.allUserCollectionReference().document(userId);

        userDocRef.get().addOnCompleteListener(task -> {
            setInProgress(false);
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // User document exists, update the username
                    userDocRef.update("username", username)
                            .addOnCompleteListener(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    navigateToMainActivity();
                                } else {
                                    Log.e("Firestore", "Error updating username", updateTask.getException());
                                }
                            });
                } else {
                    // User document does not exist, create a new user document
                    UserModel newUser = new UserModel(phoneNumber, username, Timestamp.now(), userId);
                    userDocRef.set(newUser)
                            .addOnCompleteListener(createTask -> {
                                if (createTask.isSuccessful()) {
                                    navigateToMainActivity();
                                } else {
                                    Log.e("Firestore", "Error creating user document", createTask.getException());
                                }
                            });
                }
            } else {
                Log.e("Firestore", "Error checking for user document", task.getException());
            }
        });
    }

    private void getUsername() {
        setInProgress(true);
        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            setInProgress(false);
            if (task.isSuccessful()) {
                userModel = task.getResult().toObject(UserModel.class);
                if (userModel != null) {
                    if (userModel.getUsername() != null) {
                        usernameInput.setText(userModel.getUsername());
                    }
                    if (userModel.getPhone() == null) {
                        userModel.setPhone(phoneNumber);
                    }
                } else {
                    userModel = new UserModel(phoneNumber, "", Timestamp.now(), FirebaseUtil.currentUserId());
                }
            } else {
                Log.e("Firestore", "Error fetching user details", task.getException());
            }
        });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginUserName.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            letMeInBtn.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            letMeInBtn.setVisibility(View.VISIBLE);
        }
    }
}
