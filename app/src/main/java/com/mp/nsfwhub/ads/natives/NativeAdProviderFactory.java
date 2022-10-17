package com.mp.nsfwhub.ads.natives;

import android.content.Context;

import androidx.annotation.Nullable;

import com.mp.nsfwhub.data.models.Advertisement;

final public class NativeAdProviderFactory {

    @Nullable
    public static NativeAdProvider create(Context context, Advertisement ad, int count) {
        switch (ad.network) {
            case "admob":
                return new AdMobNativeAdProvider(ad, context, count);
            case "facebook":
                return new FacebookNativeAdProvider(ad, context, count);
            case "mopub":
                return new MoPubNativeAdProvider(ad, context);
            default:
                break;
        }

        return null;
    }
}
