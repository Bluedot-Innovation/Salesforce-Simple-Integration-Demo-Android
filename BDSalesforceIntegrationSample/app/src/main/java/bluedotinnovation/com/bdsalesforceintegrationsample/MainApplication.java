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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;

import com.bluedot.BDSalesforceIntegrationWrapper.BDZoneEvent;
import com.bluedot.BDSalesforceIntegrationWrapper.ZoneEventReportListener;
import com.bluedot.BDSalesforceIntegrationWrapper.ZoneEventReporter;
import com.salesforce.marketingcloud.InitializationStatus;
import com.salesforce.marketingcloud.MarketingCloudConfig;
import com.salesforce.marketingcloud.MarketingCloudSdk;

import java.text.SimpleDateFormat;
import java.util.Date;
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
public class MainApplication extends Application implements ServiceStatusListener, ApplicationNotificationListener, ZoneEventReportListener, MarketingCloudSdk.InitializationListener {

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
        ZoneEventReporter.getInstance().setZoneEventReportListener(this);
        logInfo("BD SDK started");
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
    private String gcm_id="";   //Enter GCM Id


    private void initCloudMobilePushSDK() {
        MarketingCloudSdk.init(this, MarketingCloudConfig.builder()
                .setApplicationId(app_id)
                .setAccessToken(access_token)
                .setGcmSenderId(gcm_id)
                .setNotificationSmallIconResId(R.mipmap.ic_launcher)
                .setNotificationChannelName(CHANNEL_NAME) // Required if Android target API is >= 26
                //Enable any other feature desired.
                .build(), this);

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
        try {
            BDZoneEvent bdZoneEvent = BDZoneEvent.builder()
                    .setSubscriberKey(salesforceContactKey)
                    .setApiKey(apiKey)
                    .setZoneId(zoneInfo.getZoneId())
                    .setZoneName(zoneInfo.getZoneName())
                    .setFenceId(fenceInfo.getId())
                    .setFenceName(fenceInfo.getName())
                    .setCheckInTime(get8601formattedDate(locationInfo.getTimeStamp()))
                    .setCheckInLatitude(locationInfo.getLatitude())
                    .setCheckInLongitude(locationInfo.getLongitude())
                    .setCheckInBearing(locationInfo.getBearing())
                    .setCheckInSpeed(locationInfo.getSpeed())
                    .setCustomData(map)
                    .build();
            ZoneEventReporter.getInstance().reportCheckIn(bdZoneEvent);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private String get8601formattedDate(long timestamp) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (timestamp == 0) {
            return df.format(new Date());
        }
        return df.format(new Date(timestamp));
    }

    @Override
    public void onCheckedOutFromFence(FenceInfo fenceInfo, ZoneInfo zoneInfo, int dwellTime, Map<String, String> customData) {
        logInfo("Fence CheckOut");
        try {
            BDZoneEvent bdZoneEvent = BDZoneEvent.builder()
                    .setSubscriberKey(salesforceContactKey)
                    .setApiKey(apiKey)
                    .setZoneId(zoneInfo.getZoneId())
                    .setZoneName(zoneInfo.getZoneName())
                    .setFenceId(fenceInfo.getId())
                    .setFenceName(fenceInfo.getName())
                    .setCheckOutTime(get8601formattedDate(0))
                    .setDwellTime(dwellTime)
                    .setCustomData(customData)
                    .build();
            ZoneEventReporter.getInstance().reportCheckOut(bdZoneEvent);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCheckIntoBeacon(BeaconInfo beaconInfo, ZoneInfo zoneInfo, LocationInfo locationInfo, Proximity proximity, Map<String, String> map, boolean isCheckout) {
        logInfo("Beacon CheckIn");
        try {
            BDZoneEvent bdZoneEvent = BDZoneEvent.builder()
                    .setSubscriberKey(salesforceContactKey)
                    .setApiKey(apiKey)
                    .setZoneId(zoneInfo.getZoneId())
                    .setZoneName(zoneInfo.getZoneName())
                    .setBeaconId(beaconInfo.getId())
                    .setBeaconName(beaconInfo.getName())
                    .setCheckInTime(get8601formattedDate(locationInfo.getTimeStamp()))
                    .setCheckInLatitude(locationInfo.getLatitude())
                    .setCheckInLongitude(locationInfo.getLongitude())
                    .setCheckInBearing(locationInfo.getBearing())
                    .setCheckInSpeed(locationInfo.getSpeed())
                    .setCustomData(map)
                    .build();
            ZoneEventReporter.getInstance().reportCheckIn(bdZoneEvent);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onCheckedOutFromBeacon(BeaconInfo beaconInfo, ZoneInfo zoneInfo, int dwellTime, Map<String, String> customData) {
        logInfo("Beacon CheckOut");
        try {
            BDZoneEvent bdZoneEvent = BDZoneEvent.builder()
                    .setSubscriberKey(salesforceContactKey)
                    .setApiKey(apiKey)
                    .setZoneId(zoneInfo.getZoneId())
                    .setZoneName(zoneInfo.getZoneName())
                    .setBeaconId(beaconInfo.getId())
                    .setBeaconName(beaconInfo.getName())
                    .setCheckOutTime(get8601formattedDate(0))
                    .setDwellTime(dwellTime)
                    .setCustomData(customData)
                    .build();
            ZoneEventReporter.getInstance().reportCheckOut(bdZoneEvent);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReportSuccess() {
        logInfo("Zone Event Report Success: " + (new Date().toString()));
    }

    @Override
    public void onReportError(Error error) {
        logInfo("Zone Event Report Fail " + error.getMessage());
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

            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            notificationChannel.enableLights(false);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(false);
            NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
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
