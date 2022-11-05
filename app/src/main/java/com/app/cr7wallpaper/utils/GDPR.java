package com.app.cr7wallpaper.utils;

import android.app.Activity;

import com.app.cr7wallpaper.BuildConfig;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

public class GDPR {

    Activity activity;
    private ConsentInformation consentInformation;
    ConsentForm consentForm;

    public GDPR(Activity activity) {
        this.activity = activity;
    }

    public void updateConsentStatus() {
        if (BuildConfig.DEBUG) {
            ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA)
                    .addTestDeviceHashedId("TEST-DEVICE-HASHED-ID")
                    .build();
            ConsentRequestParameters params = new ConsentRequestParameters.Builder().setConsentDebugSettings(debugSettings).build();
            consentInformation = UserMessagingPlatform.getConsentInformation(activity);
            consentInformation.requestConsentInfoUpdate(activity, params, () -> {
                        if (consentInformation.isConsentFormAvailable()) {
                            loadForm();
                        }
                    },
                    formError -> {
                    });
        } else {
            ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
            consentInformation = UserMessagingPlatform.getConsentInformation(activity);
            consentInformation.requestConsentInfoUpdate(activity, params, () -> {
                        if (consentInformation.isConsentFormAvailable()) {
                            loadForm();
                        }
                    },
                    formError -> {
                    });
        }
    }

    public void loadForm() {
        UserMessagingPlatform.loadConsentForm(activity, consentForm -> {
                    this.consentForm = consentForm;
                    if (consentInformation.getConsentStatus() == com.google.android.ump.ConsentInformation.ConsentStatus.REQUIRED) {
                        consentForm.show(activity, formError -> {
                            loadForm();
                        });
                    }
                },
                formError -> {
                }
        );
    }


}
