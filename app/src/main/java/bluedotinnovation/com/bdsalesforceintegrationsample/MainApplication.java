package bluedotinnovation.com.bdsalesforceintegrationsample;

import android.Manifest;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import au.com.bluedot.point.net.engine.GeoTriggeringService;
import au.com.bluedot.point.net.engine.ServiceManager;
import com.salesforce.marketingcloud.InitializationStatus;
import com.salesforce.marketingcloud.MarketingCloudConfig;
import com.salesforce.marketingcloud.MarketingCloudSdk;
import com.salesforce.marketingcloud.notifications.NotificationCustomizationOptions;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static android.app.Notification.PRIORITY_MAX;

/*
 * @author Bluedot Innovation
 * Copyright (c) 2018 Bluedot Innovation. All rights reserved.
 * MainApplication demonstrates the implementation Bluedot Point SDK and Marketing Cloud SDK.
 */
public class MainApplication extends Application
        implements MarketingCloudSdk.InitializationListener {

    public static final String NOTIFICATION_TITLE = "Location Based Notifications";
    public static final String NOTIFICATION_CONTENT =
            "--PLEASE CHANGE-- This app is utilizing the location to trigger alerts " +
                    "in both background and foreground modes when you visit your favourite locations";
    //=============================== [ Bluedot SDK ] ===============================
    private ServiceManager mServiceManager;
    private final String projectId = "<YOUR_PROJECT_ID>"; //ProjectId of Bluedot App from Canvas
    //  
    final String CHANNEL_ID = "BluedotSampleChannelId";     //Please replace with yout Channel Id
    final String CHANNEL_NAME = "BluedotSampleChannelName"; //Please replace with your Channel Name

    @Override
    public void onCreate() {
        super.onCreate();

        //init SDKs
        initCloudMobilePushSDK();
        initPointSDK();
    }

    public void initPointSDK() {
        boolean locationPermissionGranted =
                ActivityCompat.checkSelfPermission(getApplicationContext(),
                                                   Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        if (locationPermissionGranted) {
            mServiceManager = ServiceManager.getInstance(this);

            if (!mServiceManager.isBluedotServiceInitialized()) {

                mServiceManager.initialize(projectId, bdError -> {
                    if (bdError != null) {
                        logInfo("BD SDK Initialize Error " + bdError.getReason());
                        return;
                    }

                    logInfo("BD SDK initialized");

                    new GeoTriggeringService.GeoTriggerBuilder().notification(createNotification())
                            .start(
                                    getApplicationContext(), geoError -> {

                                        if (geoError != null) {
                                            logInfo("BD Geo-Trigger Error " + geoError.getReason());
                                            return;
                                        }

                                        Map<String, String> metaData = new HashMap<>();
                                        metaData.put("ContactKey", salesforceContactKey);
                                        mServiceManager.setCustomEventMetaData(metaData);

                                        logInfo("BD Geo-triggering started & Contact Key set as "
                                                        + salesforceContactKey);
                                    });
                });
            }
        } else {
            requestPermissions();
        }
    }

    private void requestPermissions() {

        Intent intent = new Intent(getApplicationContext(), RequestPermissionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    //=============================== [ Bluedot SDK end ] ===============================

    //=============================== [ Marketing Cloud SDK ]=================================
    private String salesforceContactKey;
    private final String app_id = "";//Enter your App Id
    private final String access_token = ""; //Enter Access Token
    private final String fcm_id = "";   //Enter FCM Id
    private final String mID = "";   // MID from Firebase setup

    private void initCloudMobilePushSDK() {

        MarketingCloudSdk.init(this, MarketingCloudConfig.builder()
                .setApplicationId(app_id)
                .setAccessToken(access_token)
                .setSenderId(fcm_id)
                .setMarketingCloudServerUrl(getString(R.string.marketing_cloud_url))
                .setMid(mID)
                .setNotificationCustomizationOptions(
                        NotificationCustomizationOptions.create(R.mipmap.ic_launcher, null,
                                                                (context, notificationMessage) -> CHANNEL_ID))
                .build(this), this);
    }

    @Override
    public void complete(@NonNull InitializationStatus status) {

        if (status.status() == InitializationStatus.Status.SUCCESS) {
            logInfo("Marketing Cloud SDK started");
            salesforceContactKey =
                    MarketingCloudSdk.getInstance().getRegistrationManager().getContactKey();
            if (salesforceContactKey == null || salesforceContactKey.length() == 0) {
                salesforceContactKey = UUID.randomUUID().toString();
                MarketingCloudSdk.getInstance()
                        .getRegistrationManager()
                        .edit()
                        .setContactKey(salesforceContactKey)
                        .commit();
            }
        } else if (status.status() == InitializationStatus.Status.FAILED) {
            logInfo("Marketing Cloud SDK error: " + status.toString());
        } else {
            logInfo("Marketing Cloud SDK : Unknown error");
        }
    }

    //=============================== [ Marketing Cloud SDK end]=================================

    public void logInfo(String logInfo) {
        Intent intent = new Intent();
        intent.setAction(MainActivity.TEXT_LOG_BROADCAST);
        intent.putExtra("logInfo", logInfo);
        sendBroadcast(intent);
    }

    /**
     * Creates notification channel and notification, required for foreground service notification.
     *
     * @return notification
     */
    private Notification createNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel notificationChannel =
                    new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                                            NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.enableLights(false);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(false);
            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) this.getSystemService(
                            Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);

            Notification.Builder notification =
                    new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                            .setContentTitle(NOTIFICATION_TITLE)
                            .setContentText(NOTIFICATION_CONTENT)
                            .setStyle(new Notification.BigTextStyle().bigText(NOTIFICATION_CONTENT))
                            .setOngoing(true)
                            .setCategory(Notification.CATEGORY_SERVICE)
                            .setSmallIcon(R.mipmap.ic_launcher);

            return notification.build();
        } else {

            NotificationCompat.Builder notification =
                    new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                            .setContentTitle(NOTIFICATION_TITLE)
                            .setContentText(NOTIFICATION_CONTENT)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(
                                    NOTIFICATION_CONTENT))
                            .setOngoing(true)
                            .setCategory(Notification.CATEGORY_SERVICE)
                            .setPriority(PRIORITY_MAX)
                            .setSmallIcon(R.mipmap.ic_launcher);

            return notification.build();
        }
    }
}
