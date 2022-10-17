package com.mp.nsfwhub.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SyncStateContract;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.gowtham.library.utils.TrimType;
import com.mp.nsfwhub.data.models.Balance;
import com.mp.nsfwhub.utils.TrimVideo;
import com.pixplicity.easyprefs.library.Prefs;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mp.nsfwhub.BuildConfig;
import com.mp.nsfwhub.MainApplication;
import com.mp.nsfwhub.R;
import com.mp.nsfwhub.SharedConstants;
import com.mp.nsfwhub.data.api.REST;
import com.mp.nsfwhub.data.models.Advertisement;
import com.mp.nsfwhub.data.models.User;
import com.mp.nsfwhub.data.models.Wrappers;
import com.mp.nsfwhub.utils.LocaleUtil;
import com.mp.nsfwhub.utils.TempUtil;

import io.agora.rtc.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private LanguageActivity.LanguageActivityViewModel sModel;

    private final Handler mHandler = new Handler();
    private SplashActivityViewModel mModel;
    private final RequestListener<GifDrawable> mRequestListener = new RequestListener<GifDrawable>() {

        @Override
        public boolean onLoadFailed(
                @Nullable GlideException e,
                Object model,
                Target<GifDrawable> target,
                boolean first
        ) {
            return false;
        }

        @Override
        public boolean onResourceReady(
                GifDrawable resource,
                Object model,
                Target<GifDrawable> target,
                DataSource source,
                boolean first
        ) {
            resource.setLoopCount(1);
            resource.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {

                @Override
                public void onAnimationEnd(Drawable drawable) {
                    super.onAnimationEnd(drawable);
                    mModel.animated = true;
                    afterAnimation();
                }
            });
            return false;
        }
    };
    private final Runnable mDelayRunnable = () -> {
        mModel.delayed = true;
        afterDelay();
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mModel = new ViewModelProvider(this).get(SplashActivityViewModel.class);
        ImageView logo = findViewById(R.id.logo);

        try {

            if (Prefs.getString(SharedConstants.PREF_PREFERRED_LANGUAGES,"").equals("")){
                Prefs.putStringSet(SharedConstants.PREF_PREFERRED_LANGUAGES, Collections.singleton("eng"));
            }
        }catch (Exception e) {

        }



        if (logo != null) {
            Glide.with(this)
                    .asGif()
                    .load(R.drawable.logo_splash)
                    .listener(mRequestListener)
                    .into(logo);
        } else {
            mModel.animated = true;
        }
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.walletBalance()
                .enqueue(new Callback<Wrappers.Single<Balance>>() {

                    @Override
                    public void onFailure(@Nullable Call<Wrappers.Single<Balance>> call, @Nullable Throwable t) {
                        Log.e(TAG, "Could not fetch wallet balance.", t);

                    }

                    @Override
                    @SuppressLint("SetTextI18n")
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<Balance>> call,
                            @Nullable Response<Wrappers.Single<Balance>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Fetching wallet balance returned " + code + ".");
                        if (response != null && response.isSuccessful()) {
                            Log.d("sfdsf", response.body().data.schedule + "c");
                            Prefs.putInt("wallet", response.body().data.balance);
                            Prefs.putInt("schedule", response.body().data.schedule);
                        }
                    }
                });
        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction()) && TextUtils.equals(intent.getType(), "video/mp4")) {
            mModel.stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
    }

    public static class LanguageActivityViewModel extends ViewModel {

        public LanguageActivityViewModel() {
            Set<String> selected = Prefs.getStringSet(SharedConstants.PREF_PREFERRED_LANGUAGES, null);
            if (selected == null) {
                selected = new HashSet<>();
            }

            languages = selected;
        }

        public final Set<String> languages;
    }


    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mDelayRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mModel.animated) {
            if (mModel.delayed) {
                afterDelay();
            } else {
                afterAnimation();
            }
        }
    }

    private void afterAnimation() {
        int delay = getResources().getInteger(R.integer.splash_delay);
        if (delay <= 0) {
            mModel.delayed = true;
            afterDelay();
        } else {
            mHandler.postDelayed(mDelayRunnable, delay);
        }
    }

    private void afterDelay() {
        boolean intro = Prefs.getBoolean(SharedConstants.PREF_INTRO_SHOWN, false);
        if (getResources().getBoolean(R.bool.skip_intro_screen) || intro) {
            mModel.continued = true;
            continueWith();
        } else {
            Prefs.putBoolean(SharedConstants.PREF_INTRO_SHOWN, true);
            startIntroActivity();
        }
    }

    private void continueWith() {
        String token = Prefs.getString(SharedConstants.PREF_SERVER_TOKEN, null);
        if (TextUtils.isEmpty(token)) {
            continueWithProfile(null);
        } else {
            REST rest = MainApplication.getContainer().get(REST.class);
            rest.profileShow()
                    .enqueue(new Callback<Wrappers.Single<User>>() {

                        @Override
                        public void onResponse(
                                @Nullable Call<Wrappers.Single<User>> call,
                                @Nullable Response<Wrappers.Single<User>> response
                        ) {
                            int code = response != null ? response.code() : -1;
                            Log.w(TAG, "Checking token validity with server returned " + code + ".");
                            if (response != null && response.isSuccessful()) {
                                continueWithProfile(response.body().data);
                            } else {
                                continueWithProfile(null);
                            }
                        }

                        @Override
                        public void onFailure(
                                @Nullable Call<Wrappers.Single<User>> call,
                                @Nullable Throwable t
                        ) {
                            Log.e(TAG, "Failed to validate token status.", t);
                            continueWithProfile(null);
                        }
                    });
        }
    }

    private void continueWithAds(@Nullable User user) {
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnCompleteListener(result -> {
                    Uri link = getIntent().getData();
                    if (link == null) {
                        String uri = getIntent().getStringExtra("start_uri");
                        if (!TextUtils.isEmpty(uri)) {
                            link = Uri.parse(uri);
                        }
                    }

                    if (result.isSuccessful()) {
                        PendingDynamicLinkData data = result.getResult();
                        if (data != null) {
                            Uri uri = data.getLink();
                            if (uri != null) {
                                link = uri;
                            }
                        }
                    }

                    Log.v(TAG, "Found deep link " + link);
                    continueWithDeepLink(user, link);
                });
    }

    private void continueWithDeepLink(@Nullable User user, @Nullable Uri data) {
        boolean languages = Prefs.getBoolean(SharedConstants.PREF_LANGUAGES_OFFERED, false);
        if (getResources().getBoolean(R.bool.skip_language_screen) || languages) {
            startMainActivity(user, data);
        } else {
            Prefs.putBoolean(SharedConstants.PREF_LANGUAGES_OFFERED, true);
            startLanguageActivity(user);
        }

        finish();
    }

    private void continueWithProfile(@Nullable User user) {
        if (user != null && mModel.stream != null) {
            startSharing(mModel.stream);
            return;
        }

        long ads = Prefs.getLong(SharedConstants.PREF_ADS_SYNCED_AT, 0);
        if (BuildConfig.DEBUG || ads < System.currentTimeMillis() - SharedConstants.SYNC_ADS_INTERVAL) {
            Prefs.putLong(SharedConstants.PREF_ADS_SYNCED_AT, System.currentTimeMillis());
            REST rest = MainApplication.getContainer().get(REST.class);
            rest.advertisementsIndex(1)
                    .enqueue(new Callback<Wrappers.Paginated<Advertisement>>() {

                        @Override
                        public void onFailure(
                                @Nullable Call<Wrappers.Paginated<Advertisement>> call,
                                @Nullable Throwable t
                        ) {
                            Log.e(TAG, "Could not fetch advertisements from server.", t);
                            continueWithAds(user);
                        }

                        @Override
                        public void onResponse(
                                @Nullable Call<Wrappers.Paginated<Advertisement>> call,
                                @Nullable Response<Wrappers.Paginated<Advertisement>> response
                        ) {
                            int code = response != null ? response.code() : -1;
                            Log.w(TAG, "Fetching advertisements from server returned " + code + ".");
                            if (code == 200) {
                                List<Advertisement> ads = response.body().data;
                                ObjectMapper om = MainApplication.getContainer().get(ObjectMapper.class);
                                try {
                                    String json = om.writeValueAsString(ads);
                                    Prefs.putString(SharedConstants.PREF_ADS_CONFIG, json);
                                } catch (JsonProcessingException e) {
                                    Log.e(TAG, "Error in saving ads as JSON.", e);
                                }
                            }

                            continueWithAds(user);
                        }
                    });
        } else {
            continueWithAds(user);
        }
    }

    private void startIntroActivity() {
        startActivity(new Intent(this, IntroductionActivity.class));
    }

    private void startLanguageActivity(@Nullable User user) {
        Intent intent = new Intent(this, LanguageActivity.class);
        intent.putExtra(LanguageActivity.EXTRA_SPLASH, true);
        intent.putExtra(LanguageActivity.EXTRA_USER, user);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void startMainActivity(@Nullable User user, @Nullable Uri data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_USER, user);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (data != null) {
            intent.setData(data);
        }

        startActivity(intent);
    }

    private void startSharing(Uri uri) {
        File copy = TempUtil.createCopy(this, uri, ".mp4");
//        Intent intent = new Intent(this, TrimmerActivity.class);
//        intent.putExtra(TrimmerActivity.EXTRA_VIDEO, copy.getAbsolutePath());
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        proceedToTrimmer(Uri.parse(copy.getAbsolutePath()));

//        startActivity(intent);
        finish();
    }
    private void proceedToTrimmer(Uri uri) {

        File copy = TempUtil.createCopy(this, uri, ".mp4");
        copy.getParentFile().getAbsolutePath();
        openTrimActivity(String.valueOf(uri));

       /* File copy = TempUtil.createCopy(this, uri, ".mp4");
        if (getResources().getBoolean(R.bool.skip_trimming_screen)) {
            if (getResources().getBoolean(R.bool.skip_adjustment_screen)) {
                proceedToAdjustment(copy.getAbsolutePath(), null);
            } else {
                proceedToFilter(copy.getAbsolutePath());
            }
        } else {
            Intent intent = new Intent(this, TrimmerActivity.class);
            if (mModel.audio != null) {
                intent.putExtra(TrimmerActivity.EXTRA_AUDIO, mModel.audio.getPath());
            }

            intent.putExtra(TrimmerActivity.EXTRA_SONG_ID, mModel.songId);
            intent.putExtra(TrimmerActivity.EXTRA_VIDEO, copy.getAbsolutePath());
            startActivity(intent);
        }*/

        finish();
    }

    private void openTrimActivity(String data) {
        int trimType = 0;
        if (trimType == 0) {
            TrimVideo.activity(data)
//                  .setCompressOption(new CompressOption()) //pass empty constructor for default compress option
                    .setDestination(SplashActivity.this.getCacheDir().getAbsolutePath())
                    .setTrimType(TrimType.FIXED_DURATION)
                    .setMinToMax(5, 60)
                    .start(this);
        } /*else if (trimType == 1) {
            TrimVideo.activity(data)
                    .setTrimType(TrimType.FIXED_DURATION)
                    .setFixedDuration(getEdtValueLong(edtFixedGap))
                    .start(this);
        } else if (trimType == 2) {
            TrimVideo.activity(data)
                    .setTrimType(TrimType.MIN_DURATION)
                    .setMinDuration(getEdtValueLong(edtMinGap))
                    .start(this);
        } else {
            TrimVideo.activity(data)
                    .setTrimType(TrimType.MIN_MAX_DURATION)
                    .setMinToMax(getEdtValueLong(edtMinFrom), getEdtValueLong(edtMAxTo))
                    .start(this);
        }*/
    }
    public static class SplashActivityViewModel extends ViewModel {

        public boolean animated = false;
        public boolean continued = false;
        public boolean delayed = false;
        public Uri stream;
    }
}
