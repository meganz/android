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
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
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

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class MegaFirebaseMessagingService extends FirebaseMessagingService implements MegaRequestListenerInterface, MegaChatRequestListenerInterface {

    MegaApplication app;
    MegaApiAndroid megaApi;
    DatabaseHandler dbH;

    MegaChatApiAndroid megaChatApi;
    ChatSettings chatSettings;

    boolean isLoggingIn = false;
    boolean showMessageNotificationAfterPush = false;
    boolean beep = false;

    String remoteMessageType = "";

    private ChatAdvancedNotificationBuilder chatNotificationBuilder;

    Handler h;

    PowerManager.WakeLock wl;

    @Override
    public void onCreate() {
        super.onCreate();
        logDebug("onCreateFCM");

        app = (MegaApplication) getApplication();
        megaApi = app.getMegaApi();
        megaChatApi = app.getMegaChatApi();

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());

        showMessageNotificationAfterPush = false;
        beep = false;
    }

    @Override
    public void onDestroy() {
        logDebug("onDestroyFCM");
        super.onDestroy();
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        logDebug("onMessageReceived");
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
        logDebug("From: " + remoteMessage.getFrom());

        remoteMessageType = remoteMessage.getData().get("type");

        logDebug("getOriginalPriority is " + remoteMessage.getOriginalPriority() + " getPriority is " + remoteMessage.getPriority());
    
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            logDebug("Message data payload: " + remoteMessage.getData());
            UserCredentials credentials = dbH.getCredentials();
            if (credentials == null) {
                logError("There are not user credentials");
                return;
            }
            else{

                if(remoteMessageType.equals("1")){
                    logDebug("Show SharedFolder Notification");
                    //Leave the flag showMessageNotificationAfterPush as it is
                    //If true - wait until connection finish
                    //If false, no need to change it
                    logDebug("Flag showMessageNotificationAfterPush: " + showMessageNotificationAfterPush);
                    String gSession = credentials.getSession();
                    if (megaApi.getRootNode() == null) {
                        logWarning("RootNode = null");
                        performLoginProccess(gSession);
                    }
                    else{
                        logDebug("Awaiting info on listener");
                        retryPendingConnections();
                    }
                }
                else if(remoteMessageType.equals("3")){
                    logDebug("Show ContactRequest Notification");
                    //Leave the flag showMessageNotificationAfterPush as it is
                    //If true - wait until connection finish
                    //If false, no need to change it
                    logDebug("Flag showMessageNotificationAfterPush: " + showMessageNotificationAfterPush);
                    String gSession = credentials.getSession();
                    if (megaApi.getRootNode() == null) {
                        logWarning("RootNode = null");
                        performLoginProccess(gSession);
                    }
                    else{
                        logDebug("Awaiting info on listener");
                        retryPendingConnections();
                    }
                }
                else if(remoteMessageType.equals("5")) {
                    logDebug("ACCEPTANCE notification");
                    //Leave the flag showMessageNotificationAfterPush as it is
                    //If true - wait until connection finish
                    //If false, no need to change it
                    logDebug("Flag showMessageNotificationAfterPush: " + showMessageNotificationAfterPush);

                    String email = remoteMessage.getData().get("email");
                    logDebug("Acceptance CR of: " + email);

                    if (megaApi.getRootNode() == null) {
                        logWarning("RootNode = null");
                        String gSession = credentials.getSession();
                        performLoginProccess(gSession);
                    }
                    else{
                        logDebug("Awaiting info on listener");
                        retryPendingConnections();
                    }
                }
                else if(remoteMessageType.equals("4")) {
                    logDebug("CALL notification");
                    //Leave the flag showMessageNotificationAfterPush as it is
                    //If true - wait until connection finish
                    //If false, no need to change it
                    logDebug("Flag showMessageNotificationAfterPush: " + showMessageNotificationAfterPush);
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
                        boolean isIdle = pm.isDeviceIdleMode();
                        if ((!app.isActivityVisible() && megaApi.getRootNode() == null) || isIdle) {
                            logDebug("Launch foreground service!");
                            wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "MegaIncomingCallLock:");
                            wl.acquire();
                            wl.release();
                            logDebug("Start Service");
                            startService(new Intent(this, IncomingCallService.class));
                            return;
                        }
                    }
                    String gSession = credentials.getSession();
                    if (megaApi.getRootNode() == null) {
                        logWarning("RootNode = null");
                        performLoginProccess(gSession);
                    } else {
                        logDebug("RootNode is NOT null - wait CALLDATA:onChatCallUpdate");
//                        String gSession = credentials.getSession();
                        int ret = megaChatApi.getInitState();
                        logDebug("result of init ---> " + ret);
                        int status = megaChatApi.getOnlineStatus();
                        logDebug("online status ---> " + status);
                        int connectionState = megaChatApi.getConnectionState();
                        logDebug("connection state ---> " + connectionState);
                        retryPendingConnections();
                    }
                }
                else if(remoteMessageType.equals("2")){
                    logDebug("CHAT notification");
    
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
                        boolean isIdle = pm.isDeviceIdleMode();
                        logDebug("isActivityVisible: " + app.isActivityVisible());
                        logDebug("isIdle: " + isIdle);
                        wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "MegaIncomingMessageCallLock:");
                        wl.acquire();
                        wl.release();
                        if((!app.isActivityVisible() && megaApi.getRootNode() == null )|| isIdle) {

                            logDebug("Launch foreground service!");
                            Intent intent = new Intent(this,IncomingMessageService.class);
                            intent.putExtra("remoteMessage", remoteMessage);
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                                startForegroundService(intent);
                            }else{
                                startService(intent);
                            }
                            return;
                        }
                    }

                    if(app.isActivityVisible()){
                        logDebug("App on foreground --> return");
                        retryPendingConnections();
                        return;
                    }

                    if(isChatEnabled()){

                        try{
                            String silent = remoteMessage.getData().get("silent");
                            logDebug("Silent payload: " + silent);

                            if(silent!=null){
                                if(silent.equals("1")){
                                    beep = false;
                                }
                                else{
                                    beep = true;
                                }
                            }
                            else{
                                logWarning("NO DATA on the PUSH");
                                beep = true;
                            }
                        }
                        catch(Exception e){
                            logError("ERROR:remoteSilentParameter", e);
                            beep = true;
                        }

                        logDebug("Notification should beep: "+ beep);
                        showMessageNotificationAfterPush = true;

                        String gSession = credentials.getSession();
                        if (megaApi.getRootNode() == null){
                            logWarning("RootNode = null");

                            performLoginProccess(gSession);

                            chatNotificationBuilder =  ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);

                            h = new Handler(Looper.getMainLooper());
                            h.postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            boolean shown = ((MegaApplication) getApplication()).isChatNotificationReceived();
                                            if(!shown){
                                                logDebug("Show simple notification - no connection finished");
                                                chatNotificationBuilder.showSimpleNotification();
                                            }
                                            else{
                                                logDebug("Notification already shown");
                                            }
                                        }
                                    },
                                    12000
                            );
                        }
                        else{
                            //Leave the flag showMessageNotificationAfterPush as it is
                            //If true - wait until connection finish
                            //If false, no need to change it
                            logDebug("Flag showMessageNotificationAfterPush: " + showMessageNotificationAfterPush);
                            logDebug("Call to pushReceived");
                            megaChatApi.pushReceived(beep);
                            beep = false;

                            chatNotificationBuilder =  ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);

                            h = new Handler(Looper.getMainLooper());
                            h.postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            boolean shown = ((MegaApplication) getApplication()).isChatNotificationReceived();
                                            if(!shown){
                                                logDebug("Show simple notification - no connection finished");
                                                chatNotificationBuilder.showSimpleNotification();
                                            }
                                            else{
                                                logDebug("Notification already shown");
                                            }
                                        }
                                    },
                                    12000
                            );
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

            if (isChatEnabled()) {
                if (megaChatApi == null) {
                    megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
                }

                int ret = megaChatApi.getInitState();

                if(ret==MegaChatApi.INIT_NOT_DONE||ret==MegaChatApi.INIT_ERROR){
                    ret = megaChatApi.init(gSession);
                    logDebug("result of init ---> " + ret);
                    chatSettings = dbH.getChatSettings();
                    if (ret == MegaChatApi.INIT_NO_CACHE) {
                        logDebug("condition ret == MegaChatApi.INIT_NO_CACHE");

                    } else if (ret == MegaChatApi.INIT_ERROR) {
                        logDebug("condition ret == MegaChatApi.INIT_ERROR");
                        if (chatSettings == null) {
                            logWarning("ERROR----> Switch OFF chat");
                            chatSettings = new ChatSettings();
                            chatSettings.setEnabled(false+"");
                            dbH.setChatSettings(chatSettings);
                        } else {
                            logWarning("ERROR----> Switch OFF chat");
                            dbH.setEnabledChat(false + "");
                        }
                        megaChatApi.logout(this);

                    } else {
                        logDebug("Chat correctly initialized");
                    }
                }
            }

            megaApi.fastLogin(gSession, this);
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestStart: " + request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestUpdate: " + request.getRequestString());
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        logDebug("onRequestFinish: " + request.getRequestString());

        if (request.getType() == MegaRequest.TYPE_LOGIN){
            if (e.getErrorCode() == MegaError.API_OK) {
                logDebug("Fast login OK");
                logDebug("Calling fetchNodes from MegaFireBaseMessagingService");
                megaApi.fetchNodes(this);
            }
            else{
                logError("ERROR: " + e.getErrorString());
                isLoggingIn = false;
                MegaApplication.setLoggingIn(isLoggingIn);
                return;
            }
        }
        else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
            isLoggingIn = false;
            MegaApplication.setLoggingIn(isLoggingIn);
            if (e.getErrorCode() == MegaError.API_OK){
                logDebug("OK fetch nodes");
                if (isChatEnabled()) {
                    logDebug("Chat enabled-->connectInBackground");
//                    MegaApplication.isFireBaseConnection=true;
                    megaChatApi.connectInBackground(this);
                }
                else{
                    logWarning("Chat NOT enabled - sendNotification");
                }
            }
            else {
                logError("ERROR: " + e.getErrorString());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        logWarning("onRequestTemporary: " + request.getRequestString());
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        logDebug("onRequestFinish: " + request.getRequestString() +  " result: " + e.getErrorString());

        if(request.getType()==MegaChatRequest.TYPE_CONNECT){
//            MegaApplication.isFireBaseConnection=false;
            logDebug("TYPE CONNECT");
            //megaChatApi.setBackgroundStatus(true, this);
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                logDebug("Connected to chat!");
                if(showMessageNotificationAfterPush){
                    showMessageNotificationAfterPush = false;
                    logDebug("Call to pushReceived");
                    megaChatApi.pushReceived(beep);
                    beep = false;
                }
                else{
                    logDebug("Login do not started by CHAT message");
                }
            }
            else{
                logError("ERROR WHEN CONNECTING" + e.getErrorString());
            }
        }
        else if (request.getType() == MegaChatRequest.TYPE_SET_BACKGROUND_STATUS){
            logDebug("TYPE SETBACKGROUNDSTATUS");
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    public void retryPendingConnections(){
        logDebug("retryPendingConnections");
        try{
            if (megaApi != null){
                megaApi.retryPendingConnections();
            }

            if(isChatEnabled()){
                if (megaChatApi != null){
                    megaChatApi.retryPendingConnections(false, null);
                }
            }
        }
        catch (Exception e) {
            logError("Exception", e);
        }
    }
}