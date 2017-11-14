package ca.uqac.mobile.feet_tracker.android.activities.tracker;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.devtools.DevToolsActivity;
import ca.uqac.mobile.feet_tracker.android.activities.login.LoginActivity;
import ca.uqac.mobile.feet_tracker.android.activities.router.RouterActivity;
import ca.uqac.mobile.feet_tracker.android.activities.trainer.TrainerActivity;

public class TrackerActivity extends AppCompatActivity {

    FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    firebaseUser = user;
                }
                else {
                    Intent intent = new Intent(TrackerActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);

        Button btnTrainer = (Button) findViewById(R.id.btnTrainer);
        btnTrainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Uncomment
                Intent intent = new Intent(TrackerActivity.this, TrainerActivity.class);
                intent.putExtra("userUid", firebaseUser.getUid());
                startActivity(intent);
            }
        });

        Button btnRouter = (Button) findViewById(R.id.btnRouter);
        btnRouter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TrackerActivity.this, RouterActivity.class);
                startActivity(intent);
            }
        });

        Button btnDevTools = (Button) findViewById(R.id.btnDevTools);
        btnDevTools.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TrackerActivity.this, DevToolsActivity.class);
                startActivity(intent);
            }
        });

    }
}
