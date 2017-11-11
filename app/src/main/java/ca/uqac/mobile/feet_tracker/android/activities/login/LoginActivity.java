package ca.uqac.mobile.feet_tracker.android.activities.login;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.tracker.TrackerActivity;
import ca.uqac.mobile.feet_tracker.model.user.User;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = LoginActivity.class.getSimpleName();

    private EditText editTextUsername;
    private ProgressBar progressBarLogin;
    private Button btnLogin;

    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

        setContentView(R.layout.activity_login);

        editTextUsername = (EditText) findViewById(R.id.editTextUsername);
        progressBarLogin = (ProgressBar) findViewById(R.id.progressBarLogin);
        btnLogin = (Button) findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference();

        if(mAuth.getCurrentUser() != null){
            startActivity(new Intent(getApplicationContext(), TrackerActivity.class));
            finish();
        }
    }

    private void checkUsername(final String username, final CheckUsernameCallback callback){
        mRef.child("usernames").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() != null){
                    callback.isTaken();
                }
                else{
                    callback.isValid(username);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "onCancelled: " + databaseError.getMessage());
                progressBarLogin.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onClick(View v) {
        progressBarLogin.setVisibility(View.VISIBLE);

        String username = editTextUsername.getText().toString();

        if(!TextUtils.isEmpty(username)){
            registerUser(username);
        }

    }

    private void registerUser(final String username) {
        mAuth.signInAnonymously().addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(!task.isSuccessful()){
                    Toast.makeText(LoginActivity.this, R.string.login_connection_error, Toast.LENGTH_LONG).show();
                }

                progressBarLogin.setVisibility(View.INVISIBLE);

                final String userId = task.getResult().getUser().getUid();

                checkUsername(username, new CheckUsernameCallback() {
                    @Override
                    public void isValid(final String username) {
                        User newUser = new User(username, userId);
                        mRef.child("users").child(userId).setValue(newUser).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    mRef.child("usernames").child(username).setValue(userId);
                                    startActivity(new Intent(getApplicationContext(), TrackerActivity.class));
                                    finish();
                                }
                            }
                        });
                    }

                    @Override
                    public void isTaken() {
                        Toast.makeText(LoginActivity.this, "Username already taken...", Toast.LENGTH_LONG).show();
                        progressBarLogin.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
    }
}
