package mega.privacy.android.app.fcm;

/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class MegaFirebaseMessagingService extends FirebaseMessagingService implements MegaRequestListenerInterface, MegaChatRequestListenerInterface {

    public static final String SILENT = "silent";
    public static final String NO_BEEP = "1";
    MegaApplication app;
    MegaApiAndroid megaApi;
    DatabaseHandler dbH;

    MegaChatApiAndroid megaChatApi;
    ChatSettings chatSettings;

    boolean isLoggingIn = false;
    boolean showMessageNotificationAfterPush = false;
    boolean beep = false;

    String remoteMessageType = "";

    PowerManager.WakeLock wl;

    private static final int AWAKE_CPU_FOR = 60 * 1000;

    @Override
    public void onCreate() {
        super.onCreate();
        log("onCreateFCM");

        app = (MegaApplication) getApplication();
        megaApi = app.getMegaApi();
        megaChatApi = app.getMegaChatApi();

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());

        showMessageNotificationAfterPush = false;
        beep = false;
    }

    @Override
    public void onDestroy() {
        log("onDestroyFCM");
        super.onDestroy();
    }

    private void awakeCpu(RemoteMessage remoteMessage) {
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if(pm != null) {
            log("wake lock acquire");
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wake:MegaIncomingMessageCallLock");
            wl.setReferenceCounted(false);
            wl.acquire(AWAKE_CPU_FOR);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(shouldBeep(remoteMessage)) {
                startForegroundService(new Intent(this, KeepAliveService.class));
            }
        } else {
            if(shouldBeep(remoteMessage)) {
                startService(new Intent(this, KeepAliveService.class));
            }
        }
    }

    private boolean shouldBeep(RemoteMessage message) {
        boolean beep;
        try{
            String silent = message.getData().get(SILENT);
            log("Silent payload: "+silent);

            if(silent!=null){
                if(silent.equals(NO_BEEP)){
                    beep = false;
                }
                else{
                    beep = true;
                }
            }
            else{
                log("NO DATA on the PUSH");
                beep = true;
            }
        }
        catch(Exception e){
            log("ERROR:remoteSilentParameter");
            beep = true;
        }
        return beep;
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        log("onMessageReceived");
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        log("From: " + remoteMessage.getFrom());

        remoteMessageType = remoteMessage.getData().get("type");
    
        log("getOriginalPriority is " + remoteMessage.getOriginalPriority() + " getPriority is " + remoteMessage.getPriority());
    
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            log("Message data payload: " + remoteMessage.getData());
            UserCredentials credentials = dbH.getCredentials();
            if (credentials == null) {
                log("There are not user credentials");
                return;
            }
            else{
                if(remoteMessageType.equals("1")){
                    log("show SharedFolder Notification");
                    //Leave the flag showMessageNotificationAfterPush as it is
                    //If true - wait until connection finish
                    //If false, no need to change it
                    log("Flag showMessageNotificationAfterPush: "+showMessageNotificationAfterPush);
                    String gSession = credentials.getSession();
                    if (megaApi.getRootNode() == null) {
                        log("RootNode = null");
                        performLoginProccess(gSession);
                    }
                    else{
                        log("Awaiting info on listener");
                        retryPendingConnections();
                    }
                }
                else if(remoteMessageType.equals("3")){
                    log("show ContactRequest Notification");
                    //Leave the flag showMessageNotificationAfterPush as it is
                    //If true - wait until connection finish
                    //If false, no need to change it
                    log("Flag showMessageNotificationAfterPush: "+showMessageNotificationAfterPush);
                    String gSession = credentials.getSession();
                    if (megaApi.getRootNode() == null) {
                        log("RootNode = null");
                        performLoginProccess(gSession);
                    }
                    else{
                        log("Awaiting info on listener");
                        retryPendingConnections();
                    }
                }
                else if(remoteMessageType.equals("5")) {
                    log("ACCEPTANCE notification");
                    //Leave the flag showMessageNotificationAfterPush as it is
                    //If true - wait until connection finish
                    //If false, no need to change it
                    log("Flag showMessageNotificationAfterPush: "+showMessageNotificationAfterPush);

                    String email = remoteMessage.getData().get("email");
                    log("Acceptance CR of: "+email);

                    if (megaApi.getRootNode() == null) {
                        log("RootNode = null");
                        String gSession = credentials.getSession();
                        performLoginProccess(gSession);
                    }
                    else{
                        log("Awaiting info on listener");
                        retryPendingConnections();
                    }
                }
                else if(remoteMessageType.equals("4")) {
                    log("CALL notification");
                    //Leave the flag showMessageNotificationAfterPush as it is
                    //If true - wait until connection finish
                    //If false, no need to change it
                    log("Flag showMessageNotificationAfterPush: "+showMessageNotificationAfterPush);
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
                        boolean isIdle = pm.isDeviceIdleMode();
                        if ((!app.isActivityVisible() && megaApi.getRootNode() == null) || isIdle) {
                            log("launch foreground service!");
                            wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "MegaIncomingCallLock:");
                            wl.acquire();
                            wl.release();
                            log("startService-MegaFirebaseMessagingService:onMessageReceived ");
                            startService(new Intent(this, IncomingCallService.class));
                            return;
                        }
                    }
                    String gSession = credentials.getSession();
                    if (megaApi.getRootNode() == null) {
                        log("RootNode = null");
                        performLoginProccess(gSession);
                    } else {
                        log("RootNode is NOT null - wait CALLDATA:onChatCallUpdate");
//                        String gSession = credentials.getSession();
                        int ret = megaChatApi.getInitState();
                        log("result of init ---> " + ret);
                        int status = megaChatApi.getOnlineStatus();
                        log("online status ---> " + status);
                        int connectionState = megaChatApi.getConnectionState();
                        log("connection state ---> "+connectionState);
                        retryPendingConnections();
                    }
                }
                else if(remoteMessageType.equals("2")){
                    awakeCpu(remoteMessage);
                    log("CHAT notification");
                    if(MegaApplication.isActivityVisible()){
                        log("App on foreground --> return");
                        retryPendingConnections();
                        return;
                    }

                    if(Util.isChatEnabled()){
                        beep = shouldBeep(remoteMessage);

                        log("notification should beep: "+ beep);
                        showMessageNotificationAfterPush = true;

                        String gSession = credentials.getSession();
                        if (megaApi.getRootNode() == null){
                            log("RootNode = null");
                            performLoginProccess(gSession);
                        }
                        else{
                            //Leave the flag showMessageNotificationAfterPush as it is
                            //If true - wait until connection finish
                            //If false, no need to change it
                            log("Flag showMessageNotificationAfterPush: "+showMessageNotificationAfterPush);
                            log("(2)Call to pushReceived");
                            megaChatApi.pushReceived(beep);
                            beep = false;
                        }
                    }
                }

            }
        }
    }
    // [END receive_message]

    public void performLoginProccess(String gSession){
        isLoggingIn = MegaApplication.isLoggingIn();
        if (!isLoggingIn){
            isLoggingIn  = true;
            MegaApplication.setLoggingIn(isLoggingIn);

            if (Util.isChatEnabled()) {
                if (megaChatApi == null) {
                    megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
                }

                int ret = megaChatApi.getInitState();

                if(ret==MegaChatApi.INIT_NOT_DONE||ret==MegaChatApi.INIT_ERROR){
                    ret = megaChatApi.init(gSession);
                    log("result of init ---> " + ret);
                    chatSettings = dbH.getChatSettings();
                    if (ret == MegaChatApi.INIT_NO_CACHE) {
                        log("condition ret == MegaChatApi.INIT_NO_CACHE");

                    } else if (ret == MegaChatApi.INIT_ERROR) {
                        log("condition ret == MegaChatApi.INIT_ERROR");
                        if (chatSettings == null) {
                            log("ERROR----> Switch OFF chat");
                            chatSettings = new ChatSettings();
                            chatSettings.setEnabled(false+"");
                            dbH.setChatSettings(chatSettings);
                        } else {
                            log("ERROR----> Switch OFF chat");
                            dbH.setEnabledChat(false + "");
                        }
                        megaChatApi.logout(this);

                    } else {
                        log("Chat correctly initialized");
                    }
                }
            }

            megaApi.fastLogin(gSession, this);
        }
    }

    public static void log(String message) {
        Util.log("MegaFirebaseMessagingService", "FCM " + message);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart: " + request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        log("onRequestUpdate: " + request.getRequestString());
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish: " + request.getRequestString());

        if (request.getType() == MegaRequest.TYPE_LOGIN){
            if (e.getErrorCode() == MegaError.API_OK) {
                log("Fast login OK");
                log("Calling fetchNodes from MegaFireBaseMessagingService");
                megaApi.fetchNodes(this);
            }
            else{
                log("ERROR: " + e.getErrorString());
                isLoggingIn = false;
                MegaApplication.setLoggingIn(isLoggingIn);
                return;
            }
        }
        else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
            isLoggingIn = false;
            MegaApplication.setLoggingIn(isLoggingIn);
            if (e.getErrorCode() == MegaError.API_OK){
                log("OK fetch nodes");
                if (Util.isChatEnabled()) {
                    log("Chat enabled-->connectInBackground");
//                    MegaApplication.isFireBaseConnection=true;
                    megaChatApi.connectInBackground(this);
                }
                else{
                    log("Chat NOT enabled - sendNotification");
                }
            }
            else {
                log("ERROR: " + e.getErrorString());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestTemporary: " + request.getRequestString());
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish: "+request.getRequestString()+ " result: "+e.getErrorString());

        if(request.getType()==MegaChatRequest.TYPE_CONNECT){
//            MegaApplication.isFireBaseConnection=false;
            log("TYPE CONNECT");
            //megaChatApi.setBackgroundStatus(true, this);
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                log("Connected to chat!");
                if(showMessageNotificationAfterPush){
                    showMessageNotificationAfterPush = false;
                    log("Call to pushReceived");
                    megaChatApi.pushReceived(beep);
                    beep = false;
                }
                else{
                    log("Login do not started by CHAT message");
                }
            }
            else{
                log("EEEERRRRROR WHEN CONNECTING" + e.getErrorString());
            }
        }
        else if (request.getType() == MegaChatRequest.TYPE_SET_BACKGROUND_STATUS){
            log("TYPE SETBACKGROUNDSTATUS");
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    public void retryPendingConnections(){
        log("retryPendingConnections");
        try{
            if (megaApi != null){
                megaApi.retryPendingConnections();
            }

            if(Util.isChatEnabled()){
                if (megaChatApi != null){
                    megaChatApi.retryPendingConnections(false, null);
                }
            }
        }
        catch (Exception e){
            log("retryPendingConnections:Exception: "+e.getMessage());
        }
    }
}