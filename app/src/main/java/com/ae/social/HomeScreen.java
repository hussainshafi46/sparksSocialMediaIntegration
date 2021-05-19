package com.ae.social;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class HomeScreen extends AppCompatActivity {

    private static final String TAG = "Social.HomeScreen";

    private ActionBarDrawerToggle toggle;
    private GoogleSignInAccount acc;
    private AccessToken accessToken;

    private String name;
    private String email;
    private String profileImage;

    private final HandlerThread handlerThread = new HandlerThread(TAG);
    private Handler handler;

    private ShapeableImageView profilePicture;
    private TextView userName;
    private TextView userMail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        init();
        acc = GoogleSignIn.getLastSignedInAccount(this);
        if (acc != null)
            setViewFromGoogle();
        accessToken = AccessToken.getCurrentAccessToken();
        if(accessToken != null)
            setViewFromFacebook();
    }

    private void init() {
        NavigationView navigationView = findViewById(R.id.navView);
        View v = navigationView.getHeaderView(0);
        profilePicture = v.findViewById(R.id.profilePicture);
        userName = v.findViewById(R.id.username);
        userMail = v.findViewById(R.id.emailId);


        DrawerLayout drawerLayout =  findViewById(R.id.drawerLayout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.logout) {
                    //Logout
                    logout();
                    Toast.makeText(HomeScreen.this, "Logout", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        RelativeLayout quote = findViewById(R.id.welcomeQuote);
        AppBarLayout appBarLayout = findViewById(R.id.appBar);
        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsingToolbar);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if(Math.abs(verticalOffset) - appBarLayout.getTotalScrollRange() == 0) {
                    // Collapsed
                    quote.setVisibility(View.INVISIBLE);
                    collapsingToolbarLayout.setTitleEnabled(true);
                } else {
                    // Expanded
                    quote.setVisibility(View.VISIBLE);
                    collapsingToolbarLayout.setTitleEnabled(false);
                }
            }
        });
    }

    private void setViewFromGoogle() {
        name = acc.getDisplayName();
        email = acc.getEmail();
        if(acc.getPhotoUrl() != null)
            profileImage = acc.getPhotoUrl().toString();
        else
            profileImage = "";
        setUI();
    }

    private void setViewFromFacebook() {
        GraphRequest request = GraphRequest.newMeRequest(accessToken, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                try {
                    name = object.getString("name");
                    email = object.getString("email");
                    profileImage = object.getJSONObject("picture").getJSONObject("data").getString("url");
                    setUI();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "email,name,picture.type(large)");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void setUI() {
        if(name != null && !TextUtils.isEmpty(name))
            userName.setText(name);
        if(email != null && !TextUtils.isEmpty(email))
            userMail.setText(email);
        handler.post(new ImageDownloader(this, profilePicture, profileImage));
    }

    private void logout() {
        if (accessToken != null)
            LoginManager.getInstance().logOut();
        if(acc != null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            GoogleSignIn.getClient(this, gso).signOut();
        }
        Intent back = new Intent(HomeScreen.this, MainActivity.class);
        back.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(back);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handlerThread.quit();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(toggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    static class ImageDownloader implements Runnable {

        private final ShapeableImageView view;
        private final String url;
        private final Activity activity;

        ImageDownloader(Activity activity, ShapeableImageView view, String url) {
            this.view = view;
            this.url = url;
            this. activity = activity;

        }

        @Override
        public void run() {
            try {
                InputStream stream = new URL(url).openStream();
                Bitmap image = BitmapFactory.decodeStream(stream);
                if (image != null)
                    activity.runOnUiThread(new ImageSetter(view,image));
                else Log.d(TAG, "run: image not found");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class ImageSetter implements Runnable {
        private final ShapeableImageView view;
        private final Bitmap bitmap;

        public ImageSetter(ShapeableImageView view, Bitmap bitmap) {
            this.view = view;
            this.bitmap = bitmap;
        }

        @Override
        public void run() {
            view.setImageBitmap(bitmap);
        }
    }
}