package bluedotinnovation.com.bdsalesforceintegrationsample;

import android.Manifest;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import com.bluedot.BDSalesforceIntegrationWrapper.BDZoneEvent;
import com.bluedot.BDSalesforceIntegrationWrapper.ZoneEventReportListener;
import com.bluedot.BDSalesforceIntegrationWrapper.ZoneEventReporter;
import com.exacttarget.etpushsdk.ETException;
import com.exacttarget.etpushsdk.ETPush;
import com.exacttarget.etpushsdk.ETPushConfig;
import com.exacttarget.etpushsdk.ETPushConfigureSdkListener;
import com.exacttarget.etpushsdk.ETRequestStatus;
import com.exacttarget.etpushsdk.event.PushReceivedEvent;
import com.exacttarget.etpushsdk.util.EventBus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import au.com.bluedot.application.model.Proximity;
import au.com.bluedot.application.model.geo.Fence;
import au.com.bluedot.point.ApplicationNotificationListener;
import au.com.bluedot.point.ServiceStatusListener;
import au.com.bluedot.point.net.engine.BDError;
import au.com.bluedot.point.net.engine.BeaconInfo;
import au.com.bluedot.point.net.engine.LocationInfo;
import au.com.bluedot.point.net.engine.ServiceManager;
import au.com.bluedot.point.net.engine.ZoneInfo;

/*
 * @author Bluedot Innovation
 * Copyright (c) 2016 Bluedot Innovation. All rights reserved.
 * MainApplication demonstrates the implementation Bluedot Point SDK and related callbacks.
 */
public class MainApplication extends Application implements ServiceStatusListener, ApplicationNotificationListener, ZoneEventReportListener, ETPushConfigureSdkListener {

    public static final String LOCATION_ACCESS = "Location Access";
    public static final String IS_WATCHING = "This app is utilizing the location to trigger alerts " +
            "in both background and foreground modes when you visit your favourite locations";
    //=============================== [ Bluedot SDK ] ===============================
    private ServiceManager mServiceManager;
    private String packageName = "";   //Package name for the App
    private String apiKey = ""; //API key for the App
    private String emailId = ""; //Registration email Id
    private boolean restartMode = true;


    @Override
    public void onCreate() {
        super.onCreate();

        //init SDKs
        initETSDK();
        initPointSDK();
    }

    public void initPointSDK() {

        int checkPermission = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION);


        if(checkPermission == PackageManager.PERMISSION_GRANTED) {
            mServiceManager = ServiceManager.getInstance(this);

            // Android O handling - Set the foreground Service Notification which will fire only if running on Android O and above
            Intent actionIntent = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT );
            mServiceManager.setForegroundServiceNotification(R.mipmap.ic_launcher, LOCATION_ACCESS, IS_WATCHING, pendingIntent);

            if(!mServiceManager.isBlueDotPointServiceRunning()) {
                mServiceManager.sendAuthenticationRequest(packageName,apiKey,emailId,this,restartMode);
            }
        }
        else {
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


    //=============================== [ etPush SDK ]=================================
    private ETPush etPush;
    private String salesforceSubscriberKey;
    private String et_app_id="";
    private String et_access_token="";
    private String et_gcm_id="";

    private void initETSDK() {
        try {
            ETPush.configureSdk(new ETPushConfig.Builder(this)
                    .setEtAppId(et_app_id)
                    .setAccessToken(et_access_token)
                    .setGcmSenderId(et_gcm_id)
                    .build()
                    , this);
        } catch (ETException e) {
            e.printStackTrace();

        }
    }

    @Override
    public void onETPushConfigurationSuccess(ETPush etpush, ETRequestStatus etRequestStatus) {
        logInfo("etPush SDK started");
        this.etPush = etpush;

        // check if there is a subscriberKey assigned
        try {
            salesforceSubscriberKey = etPush.getSubscriberKey();
            if (salesforceSubscriberKey == null || salesforceSubscriberKey.length() == 0) {
                salesforceSubscriberKey = UUID.randomUUID().toString();
                etPush.setSubscriberKey(salesforceSubscriberKey);
            }
        } catch (ETException e) {
            e.printStackTrace();
        }
        EventBus.getInstance().register(this);
    }

    @SuppressWarnings("unused, unchecked")
    public void onEvent(final PushReceivedEvent event) {
        logInfo("Push Received: " + (new Date().toString()));

    }

    @Override
    public void onETPushConfigurationFailed(ETException e) {
        logInfo("etPush SDK error: " + e.getMessage());
    }
    //=============================== [ etPush SDK  end]=================================

    //=============================== [ etPush and Bluedot integration ] ===============================
    @Override
    public void onCheckIntoFence(Fence fence, ZoneInfo zoneInfo, LocationInfo locationInfo, Map<String, String> map, boolean isCheckOut) {
        logInfo("Fence CheckIn");
        try {
            BDZoneEvent bdZoneEvent = BDZoneEvent.builder()
                    .setSubscriberKey(salesforceSubscriberKey)
                    .setApiKey(apiKey)
                    .setPackageName(packageName)
                    .setUserName(emailId)
                    .setZoneId(zoneInfo.getZoneId())
                    .setZoneName(zoneInfo.getZoneName())
                    .setFenceId(fence.getID())
                    .setFenceName(fence.getName())
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
        if ( timestamp == 0) {
            return df.format(new Date());
        }
        return df.format(new Date(timestamp));
    }

    @Override
    public void onCheckedOutFromFence(Fence fence, ZoneInfo zoneInfo, int dwellTime, Map<String, String> customData) {
        logInfo("Fence CheckOut");
        try {
            BDZoneEvent bdZoneEvent = BDZoneEvent.builder()
                    .setSubscriberKey(salesforceSubscriberKey)
                    .setApiKey(apiKey)
                    .setPackageName(packageName)
                    .setUserName(emailId)
                    .setZoneId(zoneInfo.getZoneId())
                    .setZoneName(zoneInfo.getZoneName())
                    .setFenceId(fence.getID())
                    .setFenceName(fence.getName())
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
                    .setSubscriberKey(salesforceSubscriberKey)
                    .setApiKey(apiKey)
                    .setPackageName(packageName)
                    .setUserName(emailId)
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
                    .setSubscriberKey(salesforceSubscriberKey)
                    .setApiKey(apiKey)
                    .setPackageName(packageName)
                    .setUserName(emailId)
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
    //=============================== [ etPush and Bluedot integration end ] ===============================

    private void logInfo(String logInfo) {
        Intent intent = new Intent();
        intent.setAction(MainActivity.TEXT_LOG_BROADCAST);
        intent.putExtra("logInfo", logInfo);
        sendBroadcast(intent);
    }
}
