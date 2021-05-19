package com.ae.social;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "Social.MainActivity";
    // Got Facebook Request Code from Logcat
    public static final int FACEBOOK_REQUEST_CODE = 64206;
    public static final int RC_SIGN_IN = 7210;

    private GoogleSignInClient mGoogleSignInClient;

    private CallbackManager callbackManager;
    private LoginManager loginManager;

    AccessTokenTracker accessTokenTracker = new AccessTokenTracker() {
        @Override
        protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
            if (currentAccessToken == null)
                LoginManager.getInstance().logOut();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        AccessToken token = AccessToken.getCurrentAccessToken();
        if (token != null && !token.isExpired())
            signedIn();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null)
            signedIn();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        accessTokenTracker.stopTracking();
    }

    private void initViews() {
        Button facebook = findViewById(R.id.fb_login);
        Button google = findViewById(R.id.google_login);
        TextView app_name = findViewById(R.id.app_title);
        app_name.startAnimation(AnimationUtils.loadAnimation(this, R.anim.grow_down));

        facebook.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
        google.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));

        facebook.setOnClickListener(this);
        google.setOnClickListener(this);

        callbackManager = CallbackManager.Factory.create();
        loginManager = LoginManager.getInstance();
        registerFacebookCallback();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void registerFacebookCallback() {
        loginManager.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "Login Successful");
                signedIn();
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "Login Cancelled");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "Login Failed. Error: "+error.toString());
            }
        });
    }

    private void signedIn() {
        Intent intent = new Intent(MainActivity.this, HomeScreen.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            completedTask.getResult(ApiException.class);
            // If the above line doesn't throw any exception, sign-in was successful
            signedIn();
        } catch (ApiException e) {
            Log.d(TAG, "signInResult:failed code=" + e.getStatusCode());
        }
    }
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.google_login:
                signInWithGoogle();
                break;
            case R.id.fb_login:
                loginManager.logInWithReadPermissions(this, Arrays.asList("email"));
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
        else if (requestCode == FACEBOOK_REQUEST_CODE) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }

    }
}