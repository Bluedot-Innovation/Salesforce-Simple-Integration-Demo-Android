package bluedotinnovation.com.bdsalesforceintegrationsample;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;

import com.bluedot.BDSalesforceIntegrationWrapper.ZoneEventReportListener;
import com.bluedot.BDSalesforceIntegrationWrapper.ZoneEventReporter;
import com.exacttarget.etpushsdk.ETException;
import com.exacttarget.etpushsdk.ETPush;
import com.exacttarget.etpushsdk.ETPushConfig;
import com.exacttarget.etpushsdk.ETPushConfigureSdkListener;
import com.exacttarget.etpushsdk.ETRequestStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import au.com.bluedot.application.model.Proximity;
import au.com.bluedot.application.model.geo.Fence;
import au.com.bluedot.point.ApplicationNotificationListener;
import au.com.bluedot.point.ServiceStatusListener;
import au.com.bluedot.point.net.engine.BDError;
import au.com.bluedot.point.net.engine.BeaconInfo;
import au.com.bluedot.point.net.engine.ServiceManager;
import au.com.bluedot.point.net.engine.ZoneInfo;

/*
 * @author Bluedot Innovation
 * Copyright (c) 2016 Bluedot Innovation. All rights reserved.
 * MainApplication demonstrates the implementation Bluedot Point SDK and related callbacks.
 */
public class MainApplication extends Application implements ServiceStatusListener, ApplicationNotificationListener, ZoneEventReportListener, ETPushConfigureSdkListener {

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
            if (salesforceSubscriberKey == null || salesforceSubscriberKey.length() == 0){
                salesforceSubscriberKey = UUID.randomUUID().toString();
                etPush.setSubscriberKey(salesforceSubscriberKey);
            }
        } catch (ETException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onETPushConfigurationFailed(ETException e) {

    }
    //=============================== [ etPush SDK  end]=================================


    //=============================== [ etPush and Bluedot integration ] ===============================
    @Override
    public void onCheckIntoFence(Fence fence, ZoneInfo zoneInfo, Location location, Map<String, String> customData, boolean isCheckOut) {
        ZoneEventReporter.getInstance().reportCheckIn(salesforceSubscriberKey, zoneInfo.getZoneId(), apiKey, packageName, emailId);
    }

    @Override
    public void onCheckedOutFromFence(Fence fence, ZoneInfo zoneInfo, int dwellTime, Map<String, String> customData) {

        ZoneEventReporter.getInstance().reportCheckOut(salesforceSubscriberKey, zoneInfo.getZoneId(), apiKey, packageName, emailId);
    }

    @Override
    public void onCheckIntoBeacon(BeaconInfo beaconInfo, ZoneInfo zoneInfo, Location location, Proximity proximity, Map<String, String> customData, boolean isCheckOut) {
        ZoneEventReporter.getInstance().reportCheckIn(salesforceSubscriberKey, zoneInfo.getZoneId(), apiKey, packageName, emailId);
    }

    @Override
    public void onCheckedOutFromBeacon(BeaconInfo beaconInfo, ZoneInfo zoneInfo, int dwellTime, Map<String, String> customData) {

        ZoneEventReporter.getInstance().reportCheckOut(salesforceSubscriberKey, zoneInfo.getZoneId(), apiKey, packageName, emailId);
    }

    @Override
    public void onReportSuccess() {
        logInfo("Zone Event Report Success");
    }

    @Override
    public void onReportError(Error error) {
        logInfo("Zone Event Report Fail " + error.getMessage());
    }
    //=============================== [ etPush and Bluedot integration end ] ===============================

    private void logInfo(String logInfo){
        Intent intent = new Intent();
        intent.setAction(MainActivity.TEXT_LOG_BROADCAST);
        intent.putExtra("logInfo", logInfo);
        sendBroadcast(intent);
    }
}
