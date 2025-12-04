package com.example.chatamato;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chatamato.utils.AndroidUtil;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class LoginOtpActivity extends AppCompatActivity {

    String phoneNumber;
    Long timeoutSeconds = 60L;
    String verificationCode;
    PhoneAuthProvider.ForceResendingToken resendingToken;

    EditText otpInput;
    Button nextBtn;
    ProgressBar progressBar;
    TextView resendOtpTextView;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_otp);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        otpInput = findViewById(R.id.login_otp);
        nextBtn = findViewById(R.id.login_next_button);
        progressBar = findViewById(R.id.login_otp_progress);
        resendOtpTextView = findViewById(R.id.login_resend_otp);

        // Retrieve phone number from intent
        if (getIntent() != null && getIntent().getExtras() != null) {
            phoneNumber = getIntent().getExtras().getString("phone", "");
        } else {
            phoneNumber = "";
            AndroidUtil.showToast(getApplicationContext(), "No phone number received");
        }

        sendOtp(phoneNumber, false);

        // Handle OTP verification
        nextBtn.setOnClickListener(v -> {
            String enteredOtp = otpInput.getText().toString().trim();
            if (enteredOtp.isEmpty() || enteredOtp.length() < 6) {
                otpInput.setError("Enter a valid OTP");
                return;
            }
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationCode, enteredOtp);
            signIn(credential);
        });

        // Resend OTP
        resendOtpTextView.setOnClickListener(v -> sendOtp(phoneNumber, true));
    }

    void sendOtp(String phoneNumber, boolean isResend) {
        startResendTimer();
        setInProgress(true);
        PhoneAuthOptions.Builder builder =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                signIn(phoneAuthCredential);
                                setInProgress(false);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                AndroidUtil.showToast(getApplicationContext(), "OTP verification failed: " + e.getMessage());
                                setInProgress(false);
                            }

                            @Override
                            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                super.onCodeSent(s, forceResendingToken);
                                verificationCode = s;
                                resendingToken = forceResendingToken;
                                AndroidUtil.showToast(getApplicationContext(), "OTP sent successfully");
                                setInProgress(false);
                            }
                        });

        if (isResend) {
            PhoneAuthProvider.verifyPhoneNumber(builder.setForceResendingToken(resendingToken).build());
        } else {
            PhoneAuthProvider.verifyPhoneNumber(builder.build());
        }
    }

    void setInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            nextBtn.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            nextBtn.setVisibility(View.VISIBLE);
        }
    }

    void signIn(PhoneAuthCredential phoneAuthCredential) {
        setInProgress(true);
        mAuth.signInWithCredential(phoneAuthCredential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                AndroidUtil.showToast(getApplicationContext(), "Sign-in successful");
                Intent intent = new Intent(LoginOtpActivity.this, LoginUserName.class);
                intent.putExtra("phone", phoneNumber); // Pass phone number if needed
                startActivity(intent);
                finish();
            } else {
                AndroidUtil.showToast(getApplicationContext(), "Sign-in failed: " + task.getException().getMessage());
            }
            setInProgress(false);
        });
    }

    void startResendTimer() {
        resendOtpTextView.setEnabled(false);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                timeoutSeconds--;
                runOnUiThread(() -> resendOtpTextView.setText("Resend OTP in " + timeoutSeconds + " seconds"));

                if (timeoutSeconds <= 0) {
                    timeoutSeconds = 60L;
                    timer.cancel();
                    runOnUiThread(() -> resendOtpTextView.setEnabled(true));
                }
            }
        }, 0, 1000);
    }



}
