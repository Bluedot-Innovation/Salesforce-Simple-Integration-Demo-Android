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
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;

import com.salesforce.marketingcloud.InitializationStatus;
import com.salesforce.marketingcloud.MarketingCloudConfig;
import com.salesforce.marketingcloud.MarketingCloudSdk;
import com.salesforce.marketingcloud.notifications.NotificationCustomizationOptions;
import com.salesforce.marketingcloud.notifications.NotificationMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import au.com.bluedot.application.model.Proximity;
import au.com.bluedot.point.ApplicationNotificationListener;
import au.com.bluedot.point.ServiceStatusListener;
import au.com.bluedot.point.net.engine.BDError;
import au.com.bluedot.point.net.engine.BeaconInfo;
import au.com.bluedot.point.net.engine.FenceInfo;
import au.com.bluedot.point.net.engine.LocationInfo;
import au.com.bluedot.point.net.engine.ServiceManager;
import au.com.bluedot.point.net.engine.ZoneInfo;

import static android.app.Notification.PRIORITY_MAX;

/*
 * @author Bluedot Innovation
 * Copyright (c) 2018 Bluedot Innovation. All rights reserved.
 * MainApplication demonstrates the implementation Bluedot Point SDK and Marketing Cloud SDK.
 */
public class MainApplication extends Application implements ServiceStatusListener, ApplicationNotificationListener, MarketingCloudSdk.InitializationListener {

    public static final String NOTIFICATION_TITLE = "Location Based Notifications";
    public static final String NOTIFICATION_CONTENT = "--PLEASE CHANGE-- This app is utilizing the location to trigger alerts " +
            "in both background and foreground modes when you visit your favourite locations";
    //=============================== [ Bluedot SDK ] ===============================
    private ServiceManager mServiceManager;
    private String apiKey = ""; //API key for the App 
    private boolean restartMode = true;
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

        int checkPermissionFine = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        int checkPermissionCoarse = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION);


        if (checkPermissionFine == PackageManager.PERMISSION_GRANTED && checkPermissionCoarse == PackageManager.PERMISSION_GRANTED) {
            mServiceManager = ServiceManager.getInstance(this);

            if (!mServiceManager.isBlueDotPointServiceRunning()) {
                // Setting Notification for foreground service, required for Android Oreo and above.
                // Setting targetAllAPIs to TRUE will display foreground notification for Android versions lower than Oreo
                mServiceManager.setForegroundServiceNotification(createNotification(), false);
                mServiceManager.sendAuthenticationRequest(apiKey, this, restartMode);


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

    @Override
    public void onBlueDotPointServiceStartedSuccess() {
        mServiceManager.subscribeForApplicationNotification(this);
        Map<String, String> metaData = new HashMap<>();
        metaData.put("ContactKey", salesforceContactKey);
        mServiceManager.setCustomEventMetaData(metaData);

        logInfo("BD SDK started & Contact Key set as "+salesforceContactKey);
    }

    @Override
    public void onBlueDotPointServiceStop() {
        mServiceManager.unsubscribeForApplicationNotification(this);

    }

    @Override
    public void onBlueDotPointServiceError(BDError bdError) {
        logInfo("BD SDK error: " + bdError.getReason());
    }

    @Override
    public void onRuleUpdate(List<ZoneInfo> list) {

    }
    //=============================== [ Bluedot SDK end ] ===============================


    //=============================== [ Marketing Cloud SDK ]=================================
    private String salesforceContactKey;
    private String app_id="";   //Enter your App Id
    private String access_token=""; //Enter Access Token
    private String fcm_id ="";   //Enter FCM Id
    private String channelId = "my_custom_channel";
    private String mID = "";   // MID from Firebase setup


    private void initCloudMobilePushSDK() {

        MarketingCloudSdk.init((Context) this, MarketingCloudConfig.builder()
                .setApplicationId(app_id)
                .setAccessToken(access_token)
                .setSenderId(fcm_id)
                .setMarketingCloudServerUrl(getString(R.string.marketing_cloud_url))
                .setMid(mID)
                .setNotificationCustomizationOptions(
                        NotificationCustomizationOptions.create(R.mipmap.ic_launcher, null,
                                new com.salesforce.marketingcloud.notifications.NotificationManager.NotificationChannelIdProvider() {
                                    @Override @NonNull public String getNotificationChannelId(@NonNull Context context,
                                                                                              @NonNull NotificationMessage notificationMessage) {
                                        // Whatever custom logic required to determine which channel should be used for the message.
                                        return CHANNEL_ID;
                                    }
                                })).build((Context) this), this);
    }

    @Override
    public void complete(@NonNull InitializationStatus status) {

        if (status.status() == InitializationStatus.Status.SUCCESS) {
            logInfo("Marketing Cloud SDK started");
            salesforceContactKey = MarketingCloudSdk.getInstance().getRegistrationManager().getContactKey();
            if (salesforceContactKey == null || salesforceContactKey.length() == 0) {
                salesforceContactKey = UUID.randomUUID().toString();
                MarketingCloudSdk.getInstance().getRegistrationManager().edit().setContactKey(salesforceContactKey).commit();
            }

        } else if (status.status() == InitializationStatus.Status.COMPLETED_WITH_DEGRADED_FUNCTIONALITY) {
            // While the SDK is usable, something happened during init that you should address.
            // This could include:

            //Google play services encountered a recoverable error

                /* The user had previously provided the location permission, but it has now been revoked.
                 Geofence and Beacon messages have been disabled.  You will need to request the location
                 permission again and re-enable Geofence and/or Beacon messaging again. */

              /* Google Play Services attempted to update your SSL providers but failed.  It should be assumed that
                  all network communications will fallback to TLS1.0.
              */
        } else if (status.status() == InitializationStatus.Status.FAILED) {
            logInfo("Marketing Cloud SDK error: " + status.toString());
        } else {
            logInfo("Marketing Cloud SDK : Unknown error");
        }


    }


    //=============================== [ Marketing Cloud SDK end]=================================

    //=============================== [ Marketing Cloud SDK and Bluedot integration ] ===============================
    @Override
    public void onCheckIntoFence(FenceInfo fenceInfo, ZoneInfo zoneInfo, LocationInfo locationInfo, Map<String, String> map, boolean isCheckOut) {
        logInfo("Fence CheckIn");
    }

    @Override
    public void onCheckedOutFromFence(FenceInfo fenceInfo, ZoneInfo zoneInfo, int dwellTime, Map<String, String> customData) {
        logInfo("Fence CheckOut");
    }

    @Override
    public void onCheckIntoBeacon(BeaconInfo beaconInfo, ZoneInfo zoneInfo, LocationInfo locationInfo, Proximity proximity, Map<String, String> map, boolean isCheckout) {
        logInfo("Beacon CheckIn");
    }


    @Override
    public void onCheckedOutFromBeacon(BeaconInfo beaconInfo, ZoneInfo zoneInfo, int dwellTime, Map<String, String> customData) {
        logInfo("Beacon CheckOut");
    }

    //=============================== [ Marketing Cloud SDK and Bluedot integration end ] ===============================

    private void logInfo(String logInfo) {
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

            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, android.app.NotificationManager.IMPORTANCE_LOW);
            notificationChannel.enableLights(false);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(false);
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);

            Notification.Builder notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                    .setContentTitle(NOTIFICATION_TITLE)
                    .setContentText(NOTIFICATION_CONTENT)
                    .setStyle(new Notification.BigTextStyle().bigText(NOTIFICATION_CONTENT))
                    .setOngoing(true)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setSmallIcon(R.mipmap.ic_launcher);

            return notification.build();
        } else {

            NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext())
                    .setContentTitle(NOTIFICATION_TITLE)
                    .setContentText(NOTIFICATION_CONTENT)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(NOTIFICATION_CONTENT))
                    .setOngoing(true)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setPriority(PRIORITY_MAX)
                    .setSmallIcon(R.mipmap.ic_launcher);

            return notification.build();
        }
    }
}
