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


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.utils.Constants;
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

    MegaApplication app;
    MegaApiAndroid megaApi;
    DatabaseHandler dbH;

    MegaChatApiAndroid megaChatApi;
    ChatSettings chatSettings;

    boolean isLoggingIn = false;

    String remoteMessageType = "";

    @Override
    public void onCreate() {
        super.onCreate();
        log("onCreateFCM");

        app = (MegaApplication) getApplication();
        megaApi = app.getMegaApi();
        megaChatApi = app.getMegaChatApi();
        dbH = DatabaseHandler.getDbHandler(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        log("onDestroyFCM");
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
        log("onMessageReceived");
        MegaApplication.isFireBaseConnection=true;
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        log("From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            log("Message data payload: " + remoteMessage.getData());
            UserCredentials credentials = dbH.getCredentials();
            if (credentials == null) {
                log("There are not user credentials");
                return;
            }
            else{
                remoteMessageType = remoteMessage.getData().get("type");
                String gSession = credentials.getSession();
                if (megaApi.getRootNode() == null){
                    log("RootNode = null");
                    isLoggingIn = MegaApplication.isLoggingIn();
                    if (!isLoggingIn){
                        isLoggingIn  = true;
                        MegaApplication.setLoggingIn(isLoggingIn);

                        if (Util.isChatEnabled()) {
                            if (megaChatApi == null) {
                                megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
                            }
                            int ret = megaChatApi.init(gSession);
                            log("result of init ---> " + ret);
                            chatSettings = dbH.getChatSettings();
                            if (ret == MegaChatApi.INIT_NO_CACHE) {
                                log("condition ret == MegaChatApi.INIT_NO_CACHE");
                                megaApi.invalidateCache();

                            } else if (ret == MegaChatApi.INIT_ERROR) {
                                log("condition ret == MegaChatApi.INIT_ERROR");
                                if (chatSettings == null) {
                                    log("ERROR----> Switch OFF chat");
                                    chatSettings = new ChatSettings(false + "", true + "", "", true + "");
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

                        megaApi.fastLogin(gSession, this);
                    }
                }
                else{
                    sendNotification(remoteMessage.getData().get("type"));
                }
            }
        }
//
//        // Check if message contains a notification payload.
//        if (remoteMessage.getNotification() != null) {
//            log("Message Notification Body: " + remoteMessage.getNotification().getBody());
//            sendNotification(remoteMessage.getNotification().getBody());
//        }
//
//        if (megaApi.getRootNode() != null) {
//            log("nullll!!!!!!!");
//        }
//        else{
//            log("Tengo root node!!!!!");
//        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param type type received
     */
    private void sendNotification(String type) {
        log("sendNotification: " + type);
//        Intent intent = new Intent(this, ManagerActivityLollipop.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//        String notificationContent = "";
//        String notificationTitle = "";
//        int notificationId = 0;
//
//        try{
//            String email = "";
//            if (megaApi != null) {
//                if (megaApi.getMyUser() != null) {
//                    if (megaApi.getMyUser().getEmail() != null) {
//                        email = megaApi.getMyUser().getEmail();
//                    }
//                }
//            }
//
//
//            int typeInt = Integer.parseInt(type);
//            switch (typeInt){
//                case 1:{
//                    notificationTitle = "Cloud activity (" + email + ")";
//                    notificationContent = "A folder has been shared with you";
//                    notificationId = Constants.NOTIFICATION_PUSH_CLOUD_DRIVE;
//                    intent.setAction(Constants.ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION);
//                    break;
//                }
//                case 2:{
//                    notificationTitle = "Chat activity (" + email + ")";
//                    notificationContent = "You have received a message";
//                    notificationId = Constants.NOTIFICATION_PUSH_CHAT;
//                    intent.setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE);
//                    break;
//                }
//                case 3:{
//                    notificationTitle = "Contact activity (" + email + ")";
//                    notificationContent = "You have a new contact request";
//                    notificationId = Constants.NOTIFICATION_PUSH_CONTACT;
//                    intent.setAction(Constants.ACTION_IPC);
//                    break;
//                }
//            }
//
//
//            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
//                    PendingIntent.FLAG_ONE_SHOT);
//
//            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
//                    .setSmallIcon(R.drawable.ic_stat_notify_download)
//                    .setContentTitle(notificationTitle)
//                    .setContentText(notificationContent)
//                    .setAutoCancel(true)
//                    .setSound(defaultSoundUri)
//                    .setContentIntent(pendingIntent);
//
//            NotificationManager notificationManager =
//                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//            notificationManager.notify(notificationId, notificationBuilder.build());
//
//
//        }
//        catch(Exception e){}
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
                    log("Chat enabled-->connect");
//                    MegaApplication.isFireBaseConnection=true;
                    megaChatApi.connectInBackground(this);
                }
                else{
                    log("Chat NOT enabled - sendNotification");
                }
                sendNotification(remoteMessageType);
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
                MegaApplication.setChatConnection(true);
            }
            else{
                log("EEEERRRRROR WHEN CONNECTING " + e.getErrorString());
            }
        }
        else if (request.getType() == MegaChatRequest.TYPE_SET_BACKGROUND_STATUS){
            log("TYPE SETBACKGROUNDSTATUS");
        }

    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }
}