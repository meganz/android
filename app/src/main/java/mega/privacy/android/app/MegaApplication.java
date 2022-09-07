package mega.privacy.android.app;

import static android.app.ApplicationExitInfo.REASON_CRASH_NATIVE;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_TYPE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CALL_COMPOSITION_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_FINISH_ACTIVITY;
import static mega.privacy.android.app.constants.EventConstants.EVENT_RINGING_STATUS_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_STATUS_CHANGE;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.CacheFolderManager.clearPublicCache;
import static mega.privacy.android.app.utils.CallUtil.callStatusToString;
import static mega.privacy.android.app.utils.CallUtil.clearIncomingCallNotification;
import static mega.privacy.android.app.utils.CallUtil.incomingCall;
import static mega.privacy.android.app.utils.CallUtil.ongoingCall;
import static mega.privacy.android.app.utils.CallUtil.openMeetingRinging;
import static mega.privacy.android.app.utils.CallUtil.participatingInACall;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.API_SERVER;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.API_SERVER_PREFERENCES;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.PRODUCTION_SERVER_VALUE;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.SANDBOX3_SERVER_VALUE;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.getApiServerFromValue;
import static mega.privacy.android.app.utils.Constants.ACTION_CONFIRM;
import static mega.privacy.android.app.utils.Constants.ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION;
import static mega.privacy.android.app.utils.Constants.ACTION_LOG_OUT;
import static mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CALL_IN_PROGRESS;
import static mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CALL_OUTGOING;
import static mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CALL_RINGING;
import static mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CREATING_JOINING_MEETING;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.EXTRA_CONFIRMATION;
import static mega.privacy.android.app.utils.Constants.EXTRA_VOLUME_STREAM_TYPE;
import static mega.privacy.android.app.utils.Constants.EXTRA_VOLUME_STREAM_VALUE;
import static mega.privacy.android.app.utils.Constants.INVALID_VOLUME;
import static mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CLOUDDRIVE_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CLOUDDRIVE_NAME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_GENERAL_PUSH_CHAT;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_PUSH_CLOUD_DRIVE;
import static mega.privacy.android.app.utils.Constants.UPDATE_ACCOUNT_DETAILS;
import static mega.privacy.android.app.utils.Constants.UPDATE_CREDIT_CARD_SUBSCRIPTION;
import static mega.privacy.android.app.utils.Constants.UPDATE_GET_PRICING;
import static mega.privacy.android.app.utils.Constants.UPDATE_PAYMENT_METHODS;
import static mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.VOLUME_CHANGED_ACTION;
import static mega.privacy.android.app.utils.ContactUtil.getMegaUserNameDB;
import static mega.privacy.android.app.utils.DBUtil.callToAccountDetails;
import static mega.privacy.android.app.utils.DBUtil.callToExtendedAccountDetails;
import static mega.privacy.android.app.utils.DBUtil.callToPaymentMethods;
import static mega.privacy.android.app.utils.IncomingCallNotification.shouldNotify;
import static mega.privacy.android.app.utils.IncomingCallNotification.toSystemSettingNotification;
import static mega.privacy.android.app.utils.JobUtil.scheduleCameraUploadJob;
import static mega.privacy.android.app.utils.TimeUtils.DATE_LONG_FORMAT;
import static mega.privacy.android.app.utils.TimeUtils.formatDateAndTime;
import static mega.privacy.android.app.utils.Util.checkAppUpgrade;
import static mega.privacy.android.app.utils.Util.convertToBitSet;
import static mega.privacy.android.app.utils.Util.isSimplifiedChinese;
import static mega.privacy.android.app.utils.Util.toCDATA;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;
import static nz.mega.sdk.MegaChatCall.CALL_STATUS_USER_NO_PRESENT;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.ApplicationExitInfo;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StrictMode;
import android.text.Spanned;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.provider.FontRequest;
import androidx.core.text.HtmlCompat;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.lifecycle.Observer;
import androidx.multidex.MultiDexApplication;
import androidx.work.Configuration;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.memory.PoolConfig;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.jeremyliao.liveeventbus.LiveEventBus;

import org.webrtc.ContextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlinx.coroutines.CoroutineScope;
import me.leolin.shortcutbadger.ShortcutBadger;
import mega.privacy.android.app.components.ChatManagement;
import mega.privacy.android.app.components.PushNotificationSettingManagement;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiManagerShortcodes;
import mega.privacy.android.app.components.twemoji.TwitterEmojiProvider;
import mega.privacy.android.app.data.mapper.ChatRequestMapperKt;
import mega.privacy.android.app.di.ApplicationScope;
import mega.privacy.android.app.di.MegaApi;
import mega.privacy.android.app.di.MegaApiFolder;
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder;
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType;
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.GetCookieSettingsUseCase;
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler;
import mega.privacy.android.app.globalmanagement.MyAccountInfo;
import mega.privacy.android.app.globalmanagement.SortOrderManagement;
import mega.privacy.android.app.globalmanagement.TransfersManagement;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.listeners.GetCameraUploadAttributeListener;
import mega.privacy.android.app.listeners.GlobalChatListener;
import mega.privacy.android.app.listeners.GlobalListener;
import mega.privacy.android.app.main.LoginActivity;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.controllers.AccountController;
import mega.privacy.android.app.main.megachat.AppRTCAudioManager;
import mega.privacy.android.app.main.megachat.BadgeIntentService;
import mega.privacy.android.app.meeting.CallService;
import mega.privacy.android.app.meeting.CallSoundsController;
import mega.privacy.android.app.meeting.listeners.MeetingListener;
import mega.privacy.android.app.middlelayer.BuildFlavorHelper;
import mega.privacy.android.app.middlelayer.reporter.CrashReporter;
import mega.privacy.android.app.middlelayer.reporter.PerformanceReporter;
import mega.privacy.android.app.objects.PasscodeManagement;
import mega.privacy.android.app.presentation.logging.InitialiseLoggingUseCaseJavaWrapper;
import mega.privacy.android.app.presentation.theme.ThemeModeState;
import mega.privacy.android.app.protobuf.TombstoneProtos;
import mega.privacy.android.app.receivers.NetworkStateReceiver;
import mega.privacy.android.app.usecase.call.EndCallUseCase;
import mega.privacy.android.app.usecase.call.GetCallSoundsUseCase;
import mega.privacy.android.app.utils.CUBackupInitializeChecker;
import mega.privacy.android.app.utils.CallUtil;
import mega.privacy.android.app.utils.FrescoNativeMemoryChunkPoolParams;
import mega.privacy.android.domain.usecase.InitialiseLogging;
import nz.mega.sdk.MegaAccountSession;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatNotificationListenerInterface;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatSession;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaPricing;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;

@HiltAndroidApp
public class MegaApplication extends MultiDexApplication implements MegaChatRequestListenerInterface, MegaChatNotificationListenerInterface, MegaChatListenerInterface, Configuration.Provider {

    final String TAG = "MegaApplication";

    private static PushNotificationSettingManagement pushNotificationSettingManagement;
    private static ChatManagement chatManagement;

    @MegaApi
    @Inject
    MegaApiAndroid megaApi;
    @MegaApiFolder
    @Inject
    MegaApiAndroid megaApiFolder;
    @Inject
    MegaChatApiAndroid megaChatApi;
    @Inject
    DatabaseHandler dbH;
    @Inject
    GetCookieSettingsUseCase getCookieSettingsUseCase;
    @Inject
    SortOrderManagement sortOrderManagement;
    @Inject
    MyAccountInfo myAccountInfo;
    @Inject
    PasscodeManagement passcodeManagement;
    @Inject
    CrashReporter crashReporter;
    @Inject
    PerformanceReporter performanceReporter;
    @Inject
    InitialiseLogging initialiseLoggingUseCase;
    @Inject
    GetCallSoundsUseCase getCallSoundsUseCase;
    @Inject
    EndCallUseCase endCallUseCase;
    @Inject
    HiltWorkerFactory workerFactory;
    @Inject
    ThemeModeState themeModeState;
    @ApplicationScope
    @Inject
    CoroutineScope sharingScope;
    @Inject
    public TransfersManagement transfersManagement;
    @Inject
    ActivityLifecycleHandler activityLifecycleHandler;

    String localIpAddress = "";
    BackgroundRequestListener requestListener;
    final static public String APP_KEY = "6tioyn8ka5l6hty";
    final static private String APP_SECRET = "hfzgdtrma231qdm";

    boolean esid = false;

    private int storageState = MegaApiJava.STORAGE_STATE_UNKNOWN; //Default value

    private static boolean isLoggingIn = false;
    private static boolean isLoggingOut = false;

    private static boolean isHeartBeatAlive = false;

    private static boolean showInfoChatMessages = false;

    private static long openChatId = -1;

    private static boolean closedChat = true;

    private static long openCallChatId = -1;

    private static boolean showRichLinkWarning = false;
    private static int counterNotNowRichLinkWarning = -1;
    private static boolean enabledRichLinks = false;

    private static boolean enabledGeoLocation = false;

    private static int disableFileVersions = -1;

    private static boolean recentChatVisible = false;

    private static String urlConfirmationLink = null;

    private static boolean registeredChatListeners = false;

    private static boolean isVerifySMSShowed = false;

    private static boolean isBlockedDueToWeakAccount = false;
    private static boolean isWebOpenDueToEmailVerification = false;
    private static boolean isLoggingRunning = false;
    private static boolean isWaitingForCall = false;
    public static boolean isSpeakerOn = false;
    private static boolean areAdvertisingCookiesEnabled = false;
    private static long userWaitingForCall = MEGACHAT_INVALID_HANDLE;

    private static boolean verifyingCredentials;

    private BroadcastReceiver logoutReceiver;
    private AppRTCAudioManager rtcAudioManager = null;

    private AppRTCAudioManager rtcAudioManagerRingInCall;
    private static MegaApplication singleApplicationInstance;
    private PowerManager.WakeLock wakeLock;

    private MeetingListener meetingListener = new MeetingListener();
    private GlobalChatListener globalChatListener = new GlobalChatListener(this);

    private final CallSoundsController soundsController = new CallSoundsController();

    public static void smsVerifyShowed(boolean isShowed) {
        isVerifySMSShowed = isShowed;
    }

    class BackgroundRequestListener implements MegaRequestListenerInterface {

        @Override
        public void onRequestStart(MegaApiJava api, MegaRequest request) {
            Timber.d("BackgroundRequestListener:onRequestStart: %s", request.getRequestString());
        }

        @Override
        public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
            Timber.d("BackgroundRequestListener:onRequestUpdate: %s", request.getRequestString());
        }

        @Override
        public void onRequestFinish(MegaApiJava api, MegaRequest request,
                                    MegaError e) {
            Timber.d("BackgroundRequestListener:onRequestFinish: %s____%d___%d", request.getRequestString(), e.getErrorCode(), request.getParamType());

            if (e.getErrorCode() == MegaError.API_EPAYWALL) {
                showOverDiskQuotaPaywallWarning();
                return;
            }

            if (e.getErrorCode() == MegaError.API_EBUSINESSPASTDUE) {
                sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED));
                return;
            }

            if (request.getType() == MegaRequest.TYPE_LOGOUT) {
                Timber.d("Logout finished: %s(%d)", e.getErrorString(), e.getErrorCode());
                if (e.getErrorCode() == MegaError.API_OK) {
                    Timber.d("END logout sdk request - wait chat logout");
                    setLoggingOut(false);
                } else if (e.getErrorCode() == MegaError.API_EINCOMPLETE) {
                    if (request.getParamType() == MegaError.API_ESSL) {
                        Timber.w("SSL verification failed");
                        Intent intent = new Intent(BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED);
                        sendBroadcast(intent);
                    }
                } else if (e.getErrorCode() == MegaError.API_ESID) {
                    Timber.w("TYPE_LOGOUT:API_ESID");
                    myAccountInfo.resetDefaults();

                    esid = true;

                    AccountController.localLogoutApp(getApplicationContext(), sharingScope);
                } else if (e.getErrorCode() == MegaError.API_EBLOCKED) {
                    api.localLogout();
                    megaChatApi.logout();
                }
            } else if (request.getType() == MegaRequest.TYPE_FETCH_NODES) {
                Timber.d("TYPE_FETCH_NODES");
                if (e.getErrorCode() == MegaError.API_OK) {
                    askForFullAccountInfo();

                    GetAttrUserListener listener = new GetAttrUserListener(getApplicationContext());
                    megaApi.shouldShowRichLinkWarning(listener);
                    megaApi.isRichPreviewsEnabled(listener);

                    listener = new GetAttrUserListener(getApplicationContext(), true);
                    if (dbH != null && dbH.getMyChatFilesFolderHandle() == INVALID_HANDLE) {
                        megaApi.getMyChatFilesFolder(listener);
                    }

                    //Ask for MU and CU folder when App in init state
                    Timber.d("Get CameraUpload attribute on fetch nodes.");
                    megaApi.getUserAttribute(USER_ATTR_CAMERA_UPLOADS_FOLDER, new GetCameraUploadAttributeListener(getApplicationContext()));

                    // Init CU sync data after login successfully
                    new CUBackupInitializeChecker(megaApi).initCuSync();

                    //Login check resumed pending transfers
                    transfersManagement.checkResumedPendingTransfers();
                    scheduleCameraUploadJob(getApplicationContext());
                }
            } else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER) {
                if (request.getParamType() == MegaApiJava.USER_ATTR_PUSH_SETTINGS) {
                    if (e.getErrorCode() == MegaError.API_OK || e.getErrorCode() == MegaError.API_ENOENT) {
                        pushNotificationSettingManagement.sendPushNotificationSettings(request.getMegaPushNotificationSettings());
                    }
                } else if (e.getErrorCode() == MegaError.API_OK) {
                    if (request.getParamType() == MegaApiJava.USER_ATTR_FIRSTNAME || request.getParamType() == MegaApiJava.USER_ATTR_LASTNAME) {
                        if (megaApi != null && request.getEmail() != null) {
                            MegaUser user = megaApi.getContact(request.getEmail());
                            if (user != null) {
                                Timber.d("User handle: %s", user.getHandle());
                                Timber.d("Visibility: %s", user.getVisibility()); //If user visibity == MegaUser.VISIBILITY_UNKNOW then, non contact
                                if (user.getVisibility() != MegaUser.VISIBILITY_VISIBLE) {
                                    Timber.d("Non-contact");
                                    if (request.getParamType() == MegaApiJava.USER_ATTR_FIRSTNAME) {
                                        dbH.setNonContactEmail(request.getEmail(), user.getHandle() + "");
                                        dbH.setNonContactFirstName(request.getText(), user.getHandle() + "");
                                    } else if (request.getParamType() == MegaApiJava.USER_ATTR_LASTNAME) {
                                        dbH.setNonContactLastName(request.getText(), user.getHandle() + "");
                                    }
                                } else {
                                    Timber.d("The user is or was CONTACT:");
                                }
                            } else {
                                Timber.w("User is NULL");
                            }
                        }
                    }
                }
            } else if (request.getType() == MegaRequest.TYPE_SET_ATTR_USER) {
                if (request.getParamType() == MegaApiJava.USER_ATTR_PUSH_SETTINGS) {
                    if (e.getErrorCode() == MegaError.API_OK) {
                        pushNotificationSettingManagement.sendPushNotificationSettings(request.getMegaPushNotificationSettings());
                    } else {
                        Timber.e("Chat notification settings cannot be updated");
                    }
                }
            } else if (request.getType() == MegaRequest.TYPE_GET_PRICING) {
                if (e.getErrorCode() == MegaError.API_OK) {
                    MegaPricing p = request.getPricing();

                    dbH.setPricingTimestamp();

                    myAccountInfo.setProductAccounts(p, request.getCurrency());
                    myAccountInfo.setPricing(p);

                    Intent intent = new Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
                    intent.putExtra(ACTION_TYPE, UPDATE_GET_PRICING);
                    sendBroadcast(intent);
                } else {
                    Timber.e("Error TYPE_GET_PRICING: %s", e.getErrorCode());
                }
            } else if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_METHODS) {
                Timber.d("Payment methods request");
                if (myAccountInfo != null) {
                    myAccountInfo.setGetPaymentMethodsBoolean(true);
                }

                if (e.getErrorCode() == MegaError.API_OK) {
                    dbH.setPaymentMethodsTimeStamp();
                    if (myAccountInfo != null) {
                        myAccountInfo.setPaymentBitSet(convertToBitSet(request.getNumber()));
                    }

                    Intent intent = new Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
                    intent.putExtra(ACTION_TYPE, UPDATE_PAYMENT_METHODS);
                    sendBroadcast(intent);
                }
            } else if (request.getType() == MegaRequest.TYPE_CREDIT_CARD_QUERY_SUBSCRIPTIONS) {
                if (e.getErrorCode() == MegaError.API_OK) {
                    if (myAccountInfo != null) {
                        myAccountInfo.setNumberOfSubscriptions(request.getNumber());
                        Timber.d("NUMBER OF SUBS: %s", myAccountInfo.getNumberOfSubscriptions());
                    }

                    Intent intent = new Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
                    intent.putExtra(ACTION_TYPE, UPDATE_CREDIT_CARD_SUBSCRIPTION);
                    sendBroadcast(intent);
                }
            } else if (request.getType() == MegaRequest.TYPE_ACCOUNT_DETAILS) {
                Timber.d("Account details request");
                if (e.getErrorCode() == MegaError.API_OK) {

                    boolean storage = (request.getNumDetails() & MyAccountInfo.HAS_STORAGE_DETAILS) != 0;
                    if (storage && megaApi.getRootNode() != null) {
                        dbH.setAccountDetailsTimeStamp();
                    }

                    if (myAccountInfo != null && request.getMegaAccountDetails() != null) {
                        myAccountInfo.setAccountInfo(request.getMegaAccountDetails());
                        myAccountInfo.setAccountDetails(request.getNumDetails());

                        boolean sessions = (request.getNumDetails() & MyAccountInfo.HAS_SESSIONS_DETAILS) != 0;
                        if (sessions) {
                            MegaAccountSession megaAccountSession = request.getMegaAccountDetails().getSession(0);

                            if (megaAccountSession != null) {
                                Timber.d("getMegaAccountSESSION not Null");
                                dbH.setExtendedAccountDetailsTimestamp();
                                long mostRecentSession = megaAccountSession.getMostRecentUsage();

                                String date = formatDateAndTime(getApplicationContext(), mostRecentSession, DATE_LONG_FORMAT);

                                myAccountInfo.setLastSessionFormattedDate(date);
                                myAccountInfo.setCreateSessionTimeStamp(megaAccountSession.getCreationTimestamp());
                            }
                        }

                        Timber.d("onRequest TYPE_ACCOUNT_DETAILS: %s", myAccountInfo.getUsedPercentage());
                    }

                    sendBroadcastUpdateAccountDetails();
                }
            } else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS) {
                dbH.setTransferQueueStatus(request.getFlag());
            }
        }

        @Override
        public void onRequestTemporaryError(MegaApiJava api,
                                            MegaRequest request, MegaError e) {
            Timber.d("BackgroundRequestListener: onRequestTemporaryError: %s", request.getRequestString());
        }

    }

    public void sendBroadcastUpdateAccountDetails() {
        sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
                .putExtra(ACTION_TYPE, UPDATE_ACCOUNT_DETAILS));
    }

    private final int interval = 3000;
    private Handler keepAliveHandler = new Handler();
    int backgroundStatus = -1;

    private Runnable keepAliveRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (activityLifecycleHandler.isActivityVisible()) {
                    Timber.d("KEEPALIVE: %s", System.currentTimeMillis());
                    if (megaChatApi != null) {
                        backgroundStatus = megaChatApi.getBackgroundStatus();
                        Timber.d("backgroundStatus_activityVisible: %s", backgroundStatus);
                        if (backgroundStatus != -1 && backgroundStatus != 0) {
                            megaChatApi.setBackgroundStatus(false);
                        }
                    }

                } else {
                    Timber.d("KEEPALIVEAWAY: %s", System.currentTimeMillis());
                    if (megaChatApi != null) {
                        backgroundStatus = megaChatApi.getBackgroundStatus();
                        Timber.d("backgroundStatus_!activityVisible: %s", backgroundStatus);
                        if (backgroundStatus != -1 && backgroundStatus != 1) {
                            megaChatApi.setBackgroundStatus(true);
                        }
                    }
                }

                keepAliveHandler.postAtTime(keepAliveRunnable, System.currentTimeMillis() + interval);
                keepAliveHandler.postDelayed(keepAliveRunnable, interval);
            } catch (Exception exc) {
                Timber.e(exc, "Exception in keepAliveRunnable");
            }
        }
    };

    public void handleUncaughtException(Throwable throwable) {
        Timber.e(throwable, "UNCAUGHT EXCEPTION");
        crashReporter.report(throwable);
    }

    private final Observer<MegaChatCall> callStatusObserver = call -> {
        int callStatus = call.getStatus();
        boolean isOutgoing = call.isOutgoing();
        boolean isRinging = call.isRinging();
        long callId = call.getCallId();
        long chatId = call.getChatid();
        if (chatId == MEGACHAT_INVALID_HANDLE || callId == MEGACHAT_INVALID_HANDLE) {
            Timber.e("Error in chatId or callId");
            return;
        }

        Timber.d("Call status is %s, chat id is %d, call id is %d", callStatusToString(callStatus), chatId, callId);

        switch (callStatus) {
            case MegaChatCall.CALL_STATUS_CONNECTING:
                if ((isOutgoing && getChatManagement().isRequestSent(callId)))
                    removeRTCAudioManager();
                break;
            case MegaChatCall.CALL_STATUS_USER_NO_PRESENT:
            case MegaChatCall.CALL_STATUS_JOINING:
            case MegaChatCall.CALL_STATUS_IN_PROGRESS:
                MegaHandleList listAllCalls = megaChatApi.getChatCalls();
                if (listAllCalls == null || listAllCalls.size() == 0) {
                    Timber.e("Calls not found");
                    return;
                }

                if (callStatus == CALL_STATUS_USER_NO_PRESENT) {
                    if (isRinging) {
                        Timber.d("Is incoming call");
                        incomingCall(listAllCalls, chatId, callStatus);
                    } else {
                        MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);
                        if (chatRoom != null && chatRoom.isGroup()) {
                            Timber.d("Check if the incoming group call notification should be displayed");
                            getChatManagement().checkActiveGroupChat(chatId);
                        }
                    }
                }

                if ((callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS || callStatus == MegaChatCall.CALL_STATUS_JOINING)) {
                    getChatManagement().addNotificationShown(chatId);
                    Timber.d("Is ongoing call");
                    ongoingCall(chatId, callId, (isOutgoing && getChatManagement().isRequestSent(callId)) ? AUDIO_MANAGER_CALL_OUTGOING : AUDIO_MANAGER_CALL_IN_PROGRESS);
                }
                break;

            case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:
                Timber.d("The user participation in the call has ended. The termination code is %s", CallUtil.terminationCodeForCallToString(call.getTermCode()));
                getChatManagement().controlCallFinished(callId, chatId);
                break;

            case MegaChatCall.CALL_STATUS_DESTROYED:
                int endCallReason = call.getEndCallReason();
                Timber.d("Call has ended. End call reason is %s", CallUtil.endCallReasonToString(endCallReason));
                getChatManagement().controlCallFinished(callId, chatId);
                boolean isIgnored = call.isIgnored();
                checkCallDestroyed(chatId, callId, endCallReason, isIgnored);
                break;
        }
    };

    private final
    Observer<MegaChatCall> callCompositionObserver = call -> {
        MegaChatRoom chatRoom = megaChatApi.getChatRoom(call.getChatid());
        if (chatRoom != null && call.getCallCompositionChange() == 1 && call.getNumParticipants() > 1) {
            Timber.d("Stop sound");
            if (megaChatApi.getMyUserHandle() == call.getPeeridCallCompositionChange()) {
                clearIncomingCallNotification(call.getCallId());
                getChatManagement().removeValues(call.getChatid());
                if (call.getStatus() == CALL_STATUS_USER_NO_PRESENT) {
                    LiveEventBus.get(EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT, Long.class).post(call.getChatid());
                }
            }
        }
    };

    private final Observer<MegaChatCall> callRingingStatusObserver = call -> {
        int callStatus = call.getStatus();
        boolean isRinging = call.isRinging();
        MegaHandleList listAllCalls = megaChatApi.getChatCalls();
        if (listAllCalls == null || listAllCalls.size() == 0) {
            Timber.e("Calls not found");
            return;
        }
        if (isRinging) {
            Timber.d("Is incoming call");
            incomingCall(listAllCalls, call.getChatid(), callStatus);
        } else {
            clearIncomingCallNotification(call.getCallId());
            getChatManagement().removeValues(call.getChatid());
        }
    };

    private final Observer<Pair> sessionStatusObserver = callAndSession -> {
        MegaChatSession session = (MegaChatSession) callAndSession.second;
        int sessionStatus = session.getStatus();
        MegaChatCall call = (MegaChatCall) callAndSession.first;
        if (call == null)
            return;

        MegaChatRoom chat = megaChatApi.getChatRoom(call.getChatid());
        if (chat != null) {
            if (sessionStatus == MegaChatSession.SESSION_STATUS_IN_PROGRESS &&
                    (chat.isGroup() || chat.isMeeting() || session.getPeerid() != megaApi.getMyUserHandleBinary())) {
                Timber.d("Session is in progress");
                getChatManagement().setRequestSentCall(call.getCallId(), false);
                updateRTCAudioMangerTypeStatus(AUDIO_MANAGER_CALL_IN_PROGRESS);
            }
        }
    };

    /**
     * Broadcast for controlling changes in the volume.
     */
    BroadcastReceiver volumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;

            if (intent.getAction().equals(VOLUME_CHANGED_ACTION) && rtcAudioManagerRingInCall != null) {
                int type = (Integer) intent.getExtras().get(EXTRA_VOLUME_STREAM_TYPE);
                if (type != AudioManager.STREAM_RING)
                    return;

                int newVolume = (Integer) intent.getExtras().get(EXTRA_VOLUME_STREAM_VALUE);
                if (newVolume != INVALID_VOLUME) {
                    rtcAudioManagerRingInCall.checkVolume(newVolume);
                }
            }
        }
    };

    public boolean isAnIncomingCallRinging() {
        return rtcAudioManagerRingInCall != null;
    }

    BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;

            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                muteOrUnmute(true);
            }
        }
    };

    public void muteOrUnmute(boolean mute) {
        if (rtcAudioManagerRingInCall != null) {
            rtcAudioManagerRingInCall.muteOrUnmuteIncomingCall(mute);
        }
    }

    public static MegaApplication getInstance() {
        return singleApplicationInstance;
    }

    @Override
    public void onCreate() {
        singleApplicationInstance = this;

        super.onCreate();

        setStrictModePolicies();

        initialiseLogging();

        themeModeState.initialise();

        // Setup handler and RxJava for uncaught exceptions.
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> handleUncaughtException(e));
        RxJavaPlugins.setErrorHandler(this::handleUncaughtException);

        registerActivityLifecycleCallbacks(activityLifecycleHandler);

        isVerifySMSShowed = false;

        keepAliveHandler.postAtTime(keepAliveRunnable, System.currentTimeMillis() + interval);
        keepAliveHandler.postDelayed(keepAliveRunnable, interval);

        checkAppUpgrade();
        checkMegaStandbyBucket();
        getTombstoneInfo();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkForUnsafeIntentLaunch();
        }

        setupMegaApi();
        setupMegaApiFolder();
        setupMegaChatApi();

        LiveEventBus.config().enableLogger(false);

        storageState = dbH.getStorageState();
        pushNotificationSettingManagement = new PushNotificationSettingManagement();

        chatManagement = new ChatManagement(endCallUseCase);

        //Logout check resumed pending transfers
        transfersManagement.checkResumedPendingTransfers();

        int apiServerValue = getSharedPreferences(API_SERVER_PREFERENCES, MODE_PRIVATE)
                .getInt(API_SERVER, PRODUCTION_SERVER_VALUE);

        if (apiServerValue != PRODUCTION_SERVER_VALUE) {
            if (apiServerValue == SANDBOX3_SERVER_VALUE) {
                megaApi.setPublicKeyPinning(false);
            }

            String apiServer = getApiServerFromValue(apiServerValue);
            megaApi.changeApiUrl(apiServer);
            megaApiFolder.changeApiUrl(apiServer);
        }

        boolean useHttpsOnly = false;
        if (dbH != null) {
            useHttpsOnly = Boolean.parseBoolean(dbH.getUseHttpsOnly());
            Timber.d("Value of useHttpsOnly: %s", useHttpsOnly);
            megaApi.useHttpsOnly(useHttpsOnly);
        }

        myAccountInfo.resetDefaults();

        if (dbH != null) {
            dbH.resetExtendedAccountDetailsTimestamp();
        }

        registerReceiver(new NetworkStateReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall.class).observeForever(callStatusObserver);
        LiveEventBus.get(EVENT_RINGING_STATUS_CHANGE, MegaChatCall.class).observeForever(callRingingStatusObserver);
        LiveEventBus.get(EVENT_SESSION_STATUS_CHANGE, Pair.class).observeForever(sessionStatusObserver);
        LiveEventBus.get(EVENT_CALL_COMPOSITION_CHANGE, MegaChatCall.class).observeForever(callCompositionObserver);

        logoutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    if (intent.getAction().equals(ACTION_LOG_OUT)) {
                        storageState = MegaApiJava.STORAGE_STATE_UNKNOWN; //Default value
                    }
                }
            }
        };

        registerReceiver(logoutReceiver, new IntentFilter(ACTION_LOG_OUT));

        EmojiManager.install(new TwitterEmojiProvider());
        EmojiManagerShortcodes.initEmojiData(getApplicationContext());
        EmojiManager.install(new TwitterEmojiProvider());
        final EmojiCompat.Config config;
        Timber.d("Use downloadable font for EmojiCompat");
        // Use a downloadable font for EmojiCompat
        final FontRequest fontRequest = new FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs);
        config = new FontRequestEmojiCompatConfig(getApplicationContext(), fontRequest)
                .setReplaceAll(false)
                .registerInitCallback(new EmojiCompat.InitCallback() {
                    @Override
                    public void onInitialized() {
                        Timber.d("EmojiCompat initialized");
                    }

                    @Override
                    public void onFailed(@Nullable Throwable throwable) {
                        Timber.w("EmojiCompat initialization failed");
                    }
                });
        EmojiCompat.init(config);

        // clear the cache files stored in the external cache folder.
        clearPublicCache(this);

        ContextUtils.initialize(getApplicationContext());

        initFresco();

    }

    private void setStrictModePolicies() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder()
                            .detectAll()
                            .penaltyLog()
                            .build()
            );

            StrictMode.setVmPolicy(
                    new StrictMode.VmPolicy.Builder()
                            .detectAll()
                            .penaltyLog()
                            .build()
            );
        }
    }

    private void initialiseLogging() {
        new InitialiseLoggingUseCaseJavaWrapper(initialiseLoggingUseCase).invokeUseCase(BuildConfig.DEBUG);
    }


    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }

    /**
     * Initialize Fresco library
     */
    private void initFresco() {
        long maxMemory = mega.privacy.android.app.utils.ContextUtils.getAvailableMemory(this);
        PoolFactory poolFactory = new PoolFactory(
                PoolConfig.newBuilder()
                        .setNativeMemoryChunkPoolParams(FrescoNativeMemoryChunkPoolParams.get(maxMemory))
                        .build()
        );

        Fresco.initialize(this, ImagePipelineConfig.newBuilder(this)
                .setPoolFactory(poolFactory)
                .setDownsampleEnabled(true)
                .build());
    }

    public void askForFullAccountInfo() {
        Timber.d("askForFullAccountInfo");
        megaApi.getPaymentMethods(null);

        if (storageState == MegaApiAndroid.STORAGE_STATE_UNKNOWN) {
            megaApi.getAccountDetails();
        } else {
            megaApi.getSpecificAccountDetails(false, true, true);
        }

        megaApi.getPricing(null);
        megaApi.creditCardQuerySubscriptions(null);
    }

    public void askForPricing() {
        megaApi.getPricing(null);
    }

    public void askForPaymentMethods() {
        Timber.d("askForPaymentMethods");
        megaApi.getPaymentMethods(null);
    }

    public void askForAccountDetails() {
        Timber.d("askForAccountDetails");
        if (dbH != null) {
            dbH.resetAccountDetailsTimeStamp();
        }
        megaApi.getAccountDetails(null);
    }

    public void askForCCSubscriptions() {

        megaApi.creditCardQuerySubscriptions(null);
    }

    public void askForExtendedAccountDetails() {
        Timber.d("askForExtendedAccountDetails");
        if (dbH != null) {
            dbH.resetExtendedAccountDetailsTimestamp();
        }
        megaApi.getExtendedAccountDetails(true, false, false, null);
    }

    public void refreshAccountInfo() {
        //Check if the call is recently
        if (callToAccountDetails() || myAccountInfo.getUsedFormatted().trim().length() <= 0) {
            Timber.d("megaApi.getAccountDetails SEND");
            askForAccountDetails();
        }

        if (callToExtendedAccountDetails()) {
            Timber.d("megaApi.getExtendedAccountDetails SEND");
            askForExtendedAccountDetails();
        }

        if (callToPaymentMethods()) {
            Timber.d("megaApi.getPaymentMethods SEND");
            askForPaymentMethods();
        }
    }

    public MegaApiAndroid getMegaApiFolder() {
        return megaApiFolder;
    }

    public void disableMegaChatApi() {
        try {
            if (megaChatApi != null) {
                megaChatApi.removeChatRequestListener(this);
                megaChatApi.removeChatNotificationListener(this);
                megaChatApi.removeChatListener(globalChatListener);
                megaChatApi.removeChatCallListener(meetingListener);
                registeredChatListeners = false;
            }
        } catch (Exception ignored) {
        }
    }

    private void setupMegaApi() {
        megaApi.retrySSLerrors(true);

        megaApi.setDownloadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
        megaApi.setUploadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);

        requestListener = new BackgroundRequestListener();
        Timber.d("ADD REQUESTLISTENER");
        megaApi.addRequestListener(requestListener);

        megaApi.addGlobalListener(new GlobalListener());

        setSDKLanguage();

        // Set the proper resource limit to try avoid issues when the number of parallel transfers is very big.
        final int DESIRABLE_R_LIMIT = 20000; // SDK team recommended value
        int currentLimit = megaApi.platformGetRLimitNumFile();
        Timber.d("Current resource limit is set to %s", currentLimit);
        if (currentLimit < DESIRABLE_R_LIMIT) {
            Timber.d("Resource limit is under desirable value. Trying to increase the resource limit...");
            if (!megaApi.platformSetRLimitNumFile(DESIRABLE_R_LIMIT)) {
                Timber.w("Error setting resource limit.");
            }

            // Check new resource limit after set it in order to see if had been set successfully to the
            // desired value or maybe to a lower value limited by the system.
            Timber.d("Resource limit is set to %s", megaApi.platformGetRLimitNumFile());
        }
    }

    /**
     * Set the language code used by the app.
     * Language code is from current system setting.
     * Need to distinguish simplified and traditional Chinese.
     */
    private void setSDKLanguage() {
        Locale locale = Locale.getDefault();
        String langCode;

        // If it's Chinese
        if (Locale.CHINESE.toLanguageTag().equals(locale.getLanguage())) {
            langCode = isSimplifiedChinese() ?
                    Locale.SIMPLIFIED_CHINESE.toLanguageTag() :
                    Locale.TRADITIONAL_CHINESE.toLanguageTag();
        } else {
            langCode = locale.toString();
        }

        boolean result = megaApi.setLanguage(langCode);

        if (!result) {
            langCode = locale.getLanguage();
            result = megaApi.setLanguage(langCode);
        }

        Timber.d("Result: %s Language: %s", result, langCode);
    }

    /**
     * Setup the MegaApiAndroid instance for folder link.
     */
    private void setupMegaApiFolder() {
        // If logged in set the account auth token
        if (megaApi.isLoggedIn() != 0) {
            Timber.d("Logged in. Setting account auth token for folder links.");
            megaApiFolder.setAccountAuth(megaApi.getAccountAuth());
        }

        megaApiFolder.retrySSLerrors(true);

        megaApiFolder.setDownloadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
        megaApiFolder.setUploadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
    }

    private void setupMegaChatApi() {
        if (!registeredChatListeners) {
            Timber.d("Add listeners of megaChatApi");
            megaChatApi.addChatRequestListener(this);
            megaChatApi.addChatNotificationListener(this);
            megaChatApi.addChatListener(globalChatListener);
            megaChatApi.addChatCallListener(meetingListener);
            registeredChatListeners = true;
            checkCallSounds();
        }
    }

    /**
     * Check the changes of the meeting to play the right sound
     */
    private void checkCallSounds() {
        getCallSoundsUseCase.get()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((next) -> soundsController.playSound(next));
    }

    /**
     * Check current enabled cookies and set the corresponding flags to true/false
     */
    public void checkEnabledCookies() {
        getCookieSettingsUseCase.get()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((cookies, throwable) -> {
                    if (throwable == null) {
                        setAdvertisingCookiesEnabled(cookies.contains(CookieType.ADVERTISEMENT));

                        boolean analyticsCookiesEnabled = cookies.contains(CookieType.ANALYTICS);
                        crashReporter.setEnabled(analyticsCookiesEnabled);
                        performanceReporter.setEnabled(analyticsCookiesEnabled);
                    }
                });
    }

    public MegaApiAndroid getMegaApi() {
        return megaApi;
    }

    public MegaChatApiAndroid getMegaChatApi() {
        setupMegaChatApi();
        return megaChatApi;
    }

    public DatabaseHandler getDbH() {
        return dbH;
    }

    public static boolean isShowInfoChatMessages() {
        return showInfoChatMessages;
    }

    public static void setShowInfoChatMessages(boolean showInfoChatMessages) {
        MegaApplication.showInfoChatMessages = showInfoChatMessages;
    }

    public static String getUrlConfirmationLink() {
        return urlConfirmationLink;
    }

    public static void setUrlConfirmationLink(String urlConfirmationLink) {
        MegaApplication.urlConfirmationLink = urlConfirmationLink;
    }

    public static boolean isLoggingIn() {
        return isLoggingIn;
    }

    public static void setLoggingIn(boolean loggingIn) {
        isLoggingIn = loggingIn;
        setLoggingOut(false);
    }

    public static boolean isLoggingOut() {
        return isLoggingOut;
    }

    public static void setLoggingOut(boolean loggingOut) {
        isLoggingOut = loggingOut;
    }

    public static boolean isIsHeartBeatAlive() {
        return isHeartBeatAlive;
    }

    public static void setHeartBeatAlive(boolean heartBeatAlive) {
        isHeartBeatAlive = heartBeatAlive;
    }

    public static void setOpenChatId(long openChatId) {
        MegaApplication.openChatId = openChatId;
    }

    public static long getOpenCallChatId() {
        return openCallChatId;
    }

    public static void setOpenCallChatId(long value) {
        Timber.d("New open call chat ID: %s", value);
        openCallChatId = value;
    }

    public boolean isRecentChatVisible() {
        if (activityLifecycleHandler.isActivityVisible()) {
            return recentChatVisible;
        } else {
            return false;
        }
    }

    public static void setRecentChatVisible(boolean recentChatVisible) {
        Timber.d("setRecentChatVisible: %s", recentChatVisible);
        MegaApplication.recentChatVisible = recentChatVisible;
    }

    public static long getOpenChatId() {
        return openChatId;
    }

    public String getLocalIpAddress() {
        return localIpAddress;
    }

    public void setLocalIpAddress(String ip) {
        localIpAddress = ip;
    }

    public void showSharedFolderNotification(MegaNode n) {
        Timber.d("showSharedFolderNotification");

        try {
            ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
            String name = "";
            for (int j = 0; j < sharesIncoming.size(); j++) {
                MegaShare mS = sharesIncoming.get(j);
                if (mS.getNodeHandle() == n.getHandle()) {
                    MegaUser user = megaApi.getContact(mS.getUser());

                    name = getMegaUserNameDB(user);
                    if (name == null) name = "";
                }
            }

            String source = "<b>" + n.getName() + "</b> " + getString(R.string.incoming_folder_notification) + " " + toCDATA(name);
            Spanned notificationContent = HtmlCompat.fromHtml(source, HtmlCompat.FROM_HTML_MODE_LEGACY);

            String notificationChannelId = NOTIFICATION_CHANNEL_CLOUDDRIVE_ID;

            Intent intent = new Intent(this, ManagerActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .setAction(ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

            String notificationTitle = getString(n.hasChanged(MegaNode.CHANGE_TYPE_NEW)
                    ? R.string.title_incoming_folder_notification
                    : R.string.context_permissions_changed);

            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(notificationChannelId,
                        NOTIFICATION_CHANNEL_CLOUDDRIVE_NAME, NotificationManager.IMPORTANCE_HIGH);

                channel.setShowBadge(true);
                notificationManager.createNotificationChannel(channel);
            }

            Drawable d = getResources().getDrawable(R.drawable.ic_folder_incoming, getTheme());
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, notificationChannelId)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationContent)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(notificationContent))
                    .setAutoCancel(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentIntent(pendingIntent)
                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                    .setLargeIcon(((BitmapDrawable) d).getBitmap())
                    .setPriority(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                            // Use NotificationManager for devices running Android Nougat or above (API >= 24)
                            ? NotificationManager.IMPORTANCE_HIGH
                            // Otherwise, use NotificationCompat for devices running Android Marshmallow (API 23)
                            : NotificationCompat.PRIORITY_HIGH);

            notificationManager.notify(NOTIFICATION_PUSH_CLOUD_DRIVE, notificationBuilder.build());
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public void sendSignalPresenceActivity() {
        Timber.d("sendSignalPresenceActivity");
        if (megaChatApi != null) {
            if (megaChatApi.isSignalActivityRequired()) {
                megaChatApi.signalPresenceActivity();
            }
        }
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
        Timber.d("onRequestStart (CHAT): %s", request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        Timber.d("onRequestFinish (CHAT): %s_%d", request.getRequestString(), e.getErrorCode());
        if (request.getType() == MegaChatRequest.TYPE_SET_BACKGROUND_STATUS) {
            Timber.d("SET_BACKGROUND_STATUS: %s", request.getFlag());
        } else if (request.getType() == MegaChatRequest.TYPE_LOGOUT) {
            Timber.d("CHAT_TYPE_LOGOUT: %d__%s", e.getErrorCode(), e.getErrorString());

            resetDefaults();

            try {
                if (megaChatApi != null) {
                    megaChatApi.removeChatRequestListener(this);
                    megaChatApi.removeChatNotificationListener(this);
                    megaChatApi.removeChatListener(globalChatListener);
                    megaChatApi.removeChatCallListener(meetingListener);
                    registeredChatListeners = false;
                }
            } catch (Exception exc) {
            }

            try {
                ShortcutBadger.applyCount(getApplicationContext(), 0);

                startService(new Intent(getApplicationContext(), BadgeIntentService.class).putExtra("badgeCount", 0));
            } catch (Exception exc) {
                Timber.e(exc, "EXCEPTION removing badge indicator");
            }

            if (megaApi != null) {
                int loggedState = megaApi.isLoggedIn();
                Timber.d("Login status on %s", loggedState);
                if (loggedState == 0) {
                    AccountController.logoutConfirmed(this, sharingScope);
                    //Need to finish ManagerActivity to avoid unexpected behaviours after forced logouts.
                    LiveEventBus.get(EVENT_FINISH_ACTIVITY, Boolean.class).post(true);

                    if (isLoggingRunning()) {
                        Timber.d("Already in Login Activity, not necessary to launch it again");
                        return;
                    }

                    Intent loginIntent = new Intent(this, LoginActivity.class);

                    if (getUrlConfirmationLink() != null) {
                        loginIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        loginIntent.putExtra(EXTRA_CONFIRMATION, getUrlConfirmationLink());
                        if (activityLifecycleHandler.isActivityVisible()) {
                            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        } else {
                            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        }
                        loginIntent.setAction(ACTION_CONFIRM);
                        setUrlConfirmationLink(null);
                    } else if (activityLifecycleHandler.isActivityVisible()) {
                        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    } else {
                        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }

                    startActivity(loginIntent);
                } else {
                    Timber.d("Disable chat finish logout");
                }
            } else {
                AccountController.logoutConfirmed(this, sharingScope);

                if (activityLifecycleHandler.isActivityVisible()) {
                    Timber.d("Launch intent to login screen");
                    Intent tourIntent = new Intent(this, LoginActivity.class);
                    tourIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    this.startActivity(tourIntent);
                }
            }
        } else if (request.getType() == MegaChatRequest.TYPE_PUSH_RECEIVED) {
            Timber.d("TYPE_PUSH_RECEIVED: %d__%s", e.getErrorCode(), e.getErrorString());

            //Temporary HMS code to show pushes until AND-13803 is resolved.
            if (BuildFlavorHelper.INSTANCE.isHMS()) {
                if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                    Timber.d("OK:TYPE_PUSH_RECEIVED");
                    if (!getMegaApi().isEphemeralPlusPlus()) {
                        ChatAdvancedNotificationBuilder.newInstance(this)
                                .generateChatNotification(ChatRequestMapperKt.toChatRequest(request));
                    }
                } else {
                    Timber.w("Error TYPE_PUSH_RECEIVED: %s", e.getErrorString());
                }
            }
        } else if (request.getType() == MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT) {
            chatManagement.removeJoiningChatId(request.getChatHandle());
            chatManagement.removeJoiningChatId(request.getUserHandle());
        } else if (request.getType() == MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM
                && request.getUserHandle() == INVALID_HANDLE) {
            chatManagement.removeLeavingChatId(request.getChatHandle());
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        Timber.w("onRequestTemporaryError (CHAT): %s", e.getErrorString());
    }

    /**
     * Method for showing an incoming group or one-to-one call notification.
     *
     * @param incomingCall The incoming call
     */
    public void showOneCallNotification(MegaChatCall incomingCall) {
        Timber.d("Show incoming call notification and start to sound. Chat ID is %s", incomingCall.getChatid());
        createOrUpdateAudioManager(false, AUDIO_MANAGER_CALL_RINGING);
        getChatManagement().addNotificationShown(incomingCall.getChatid());
        ChatAdvancedNotificationBuilder notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this);
        notificationBuilder.showOneCallNotification(incomingCall);
    }

    public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {
        if (!item.isGroup())
            return;

        if ((item.hasChanged(MegaChatListItem.CHANGE_TYPE_OWN_PRIV))) {
            if (item.getOwnPrivilege() != MegaChatRoom.PRIV_RM) {
                getChatManagement().checkActiveGroupChat(item.getChatId());
            } else {
                getChatManagement().removeActiveChatAndNotificationShown(item.getChatId());
            }
        }

        if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_CLOSED)) {
            getChatManagement().removeActiveChatAndNotificationShown(item.getChatId());
        }
    }

    @Override
    public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {

    }

    @Override
    public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userhandlev, int status, boolean inProgress) {

    }

    @Override
    public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {
        if (config.isPending() == false) {
            Timber.d("Launch local broadcast");
            Intent intent = new Intent(BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {
    }

    @Override
    public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {
    }

    @Override
    public void onDbError(MegaChatApiJava api, int error, String msg) {
    }

    public void updateAppBadge() {
        Timber.d("updateAppBadge");

        int totalHistoric = 0;
        int totalIpc = 0;
        if (megaApi != null && megaApi.getRootNode() != null) {
            totalHistoric = megaApi.getNumUnreadUserAlerts();
            ArrayList<MegaContactRequest> requests = megaApi.getIncomingContactRequests();
            if (requests != null) {
                totalIpc = requests.size();
            }
        }

        int chatUnread = 0;
        if (megaChatApi != null) {
            chatUnread = megaChatApi.getUnreadChats();
        }

        int totalNotifications = totalHistoric + totalIpc + chatUnread;
        //Add Android version check if needed
        if (totalNotifications == 0) {
            //Remove badge indicator - no unread chats
            ShortcutBadger.applyCount(getApplicationContext(), 0);
            //Xiaomi support
            startService(new Intent(getApplicationContext(), BadgeIntentService.class).putExtra("badgeCount", 0));
        } else {
            //Show badge with indicator = unread
            ShortcutBadger.applyCount(getApplicationContext(), Math.abs(totalNotifications));
            //Xiaomi support
            startService(new Intent(getApplicationContext(), BadgeIntentService.class).putExtra("badgeCount", totalNotifications));
        }
    }

    @Override
    public void onChatNotification(MegaChatApiJava api, long chatid, MegaChatMessage msg) {
        Timber.d("onChatNotification");

        updateAppBadge();

        if (MegaApplication.getOpenChatId() == chatid) {
            Timber.d("Do not update/show notification - opened chat");
            return;
        }

        if (isRecentChatVisible()) {
            Timber.d("Do not show notification - recent chats shown");
            return;
        }

        if (activityLifecycleHandler.isActivityVisible()) {

            try {
                if (msg != null) {

                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    mNotificationManager.cancel(NOTIFICATION_GENERAL_PUSH_CHAT);

                    if (msg.getStatus() == MegaChatMessage.STATUS_NOT_SEEN) {
                        if (msg.getType() == MegaChatMessage.TYPE_NORMAL || msg.getType() == MegaChatMessage.TYPE_CONTACT_ATTACHMENT || msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT || msg.getType() == MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT) {
                            if (msg.isDeleted()) {
                                Timber.d("Message deleted");

                                megaChatApi.pushReceived(false);
                            } else if (msg.isEdited()) {
                                Timber.d("Message edited");
                                megaChatApi.pushReceived(false);
                            } else {
                                Timber.d("New normal message");
                                megaChatApi.pushReceived(true);
                            }
                        } else if (msg.getType() == MegaChatMessage.TYPE_TRUNCATE) {
                            Timber.d("New TRUNCATE message");
                            megaChatApi.pushReceived(false);
                        }
                    } else {
                        Timber.d("Message SEEN");
                        megaChatApi.pushReceived(false);
                    }
                }
            } catch (Exception e) {
                Timber.e(e, "EXCEPTION when showing chat notification");
            }
        } else {
            Timber.d("Do not notify chat messages: app in background");
        }
    }

    public void checkOneCall(long incomingCallChatId) {
        Timber.d("One call : Chat ID is %d, openCall Chat ID is %d", incomingCallChatId, openCallChatId);
        if (openCallChatId == incomingCallChatId) {
            Timber.d("The call is already opened");
            return;
        }

        MegaChatCall callToLaunch = megaChatApi.getChatCall(incomingCallChatId);
        if (callToLaunch == null) {
            Timber.w("Call is null");
            return;
        }

        int callStatus = callToLaunch.getStatus();
        if (callStatus > MegaChatCall.CALL_STATUS_IN_PROGRESS) {
            Timber.w("Launch not in correct status: %s", callStatus);
            return;
        }

        MegaChatRoom chatRoom = megaChatApi.getChatRoom(incomingCallChatId);
        if (chatRoom == null) {
            Timber.w("Chat room is null");
            return;
        }

        if (!CallUtil.isOneToOneCall(chatRoom) && callToLaunch.getStatus() == CALL_STATUS_USER_NO_PRESENT && callToLaunch.isRinging() && (!getChatManagement().isOpeningMeetingLink(incomingCallChatId))) {
            Timber.d("Group call or meeting, the notification should be displayed");
            showOneCallNotification(callToLaunch);
            return;
        }

        checkOneToOneIncomingCall(callToLaunch);
    }

    /**
     * Check whether an incoming 1-to-1 call should show notification or incoming call screen
     *
     * @param callToLaunch The incoming call
     */
    private void checkOneToOneIncomingCall(MegaChatCall callToLaunch) {
        if (shouldNotify(this) && !activityLifecycleHandler.isActivityVisible()) {
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ":MegaIncomingCallPowerLock");
            }
            if (!wakeLock.isHeld()) {
                wakeLock.acquire(10 * 1000);
            }

            Timber.d("The notification should be displayed. Chat ID of incoming call %s", callToLaunch.getChatid());
            showOneCallNotification(callToLaunch);
        } else {
            Timber.d("The call screen should be displayed. Chat ID of incoming call %s", callToLaunch.getChatid());
            MegaApplication.getInstance().createOrUpdateAudioManager(false, AUDIO_MANAGER_CALL_RINGING);
            launchCallActivity(callToLaunch);
        }
    }

    public void checkSeveralCall(MegaHandleList listAllCalls, int callStatus, boolean isRinging, long incomingCallChatId) {
        Timber.d("Several calls = %d- Current call Status: %s", listAllCalls.size(), callStatusToString(callStatus));
        if (isRinging) {
            if (participatingInACall()) {
                Timber.d("Several calls: show notification");
                checkQueuedCalls(incomingCallChatId);
                return;
            }

            MegaChatRoom chatRoom = megaChatApi.getChatRoom(incomingCallChatId);
            if (callStatus == CALL_STATUS_USER_NO_PRESENT && chatRoom != null) {
                if (!CallUtil.isOneToOneCall(chatRoom) && !getChatManagement().isOpeningMeetingLink(incomingCallChatId)) {
                    Timber.d("Show incoming group call notification");
                    MegaChatCall incomingCall = megaChatApi.getChatCall(incomingCallChatId);
                    if (incomingCall != null) {
                        showOneCallNotification(incomingCall);
                    }
                    return;
                }

                if (CallUtil.isOneToOneCall(chatRoom) && openCallChatId != chatRoom.getChatId()) {
                    Timber.d("Show incoming one to one call screen");
                    MegaChatCall callToLaunch = megaChatApi.getChatCall(chatRoom.getChatId());
                    checkOneToOneIncomingCall(callToLaunch);
                    return;
                }
            }
        }

        MegaHandleList handleList = megaChatApi.getChatCalls(callStatus);
        if (handleList == null || handleList.size() == 0)
            return;

        MegaChatCall callToLaunch = null;

        for (int i = 0; i < handleList.size(); i++) {
            if (openCallChatId != handleList.get(i)) {
                if (megaChatApi.getChatCall(handleList.get(i)) != null && !megaChatApi.getChatCall(handleList.get(i)).isOnHold()) {
                    callToLaunch = megaChatApi.getChatCall(handleList.get(i));
                }
            } else {
                Timber.d("The call is already opened");
            }
        }

        if (callToLaunch != null) {
            checkOneToOneIncomingCall(callToLaunch);
        }
    }

    private void checkCallDestroyed(long chatId, long callId, int endCallReason, boolean isIgnored) {
        getChatManagement().setOpeningMeetingLink(chatId, false);

        if (shouldNotify(this)) {
            toSystemSettingNotification(this);
        }

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        getChatManagement().removeNotificationShown(chatId);

        try {
            if (endCallReason == MegaChatCall.END_CALL_REASON_NO_ANSWER && !isIgnored) {
                MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);
                if (chatRoom != null && !chatRoom.isGroup() && !chatRoom.isMeeting() && megaApi.isChatNotifiable(chatId)) {
                    try {
                        Timber.d("Show missed call notification");
                        ChatAdvancedNotificationBuilder.newInstance(this)
                                .showMissedCallNotification(chatId, callId);
                    } catch (Exception e) {
                        Timber.e(e, "EXCEPTION when showing missed call notification");
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e, "EXCEPTION when showing missed call notification");
        }
    }

    /**
     * Get the current standby bucket of the app.
     * The system determines the standby state of the app based on app usage patterns.
     *
     * @return the current standby bucket of the app：
     * STANDBY_BUCKET_ACTIVE,
     * STANDBY_BUCKET_WORKING_SET,
     * STANDBY_BUCKET_FREQUENT,
     * STANDBY_BUCKET_RARE,
     * STANDBY_BUCKET_RESTRICTED,
     * STANDBY_BUCKET_NEVER
     */
    public int checkMegaStandbyBucket() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            if (usageStatsManager != null) {
                int standbyBucket = usageStatsManager.getAppStandbyBucket();
                Timber.d("getAppStandbyBucket(): %s", standbyBucket);
                return standbyBucket;
            }
        }
        return -1;
    }

    /**
     * Get the tombstone information.
     */
    public void getTombstoneInfo() {
        new Thread(() -> {
            Timber.d("getTombstoneInfo");
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ApplicationExitInfo> exitReasons;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                exitReasons = activityManager.getHistoricalProcessExitReasons(/* packageName = */ null, /* pid = */ 0, /* maxNum = */ 3);
                for (ApplicationExitInfo aei : exitReasons) {
                    if (aei.getReason() == REASON_CRASH_NATIVE) {
                        // Get the tombstone input stream.
                        try {
                            InputStream tombstoneInputStream = aei.getTraceInputStream();
                            if (tombstoneInputStream != null) {
                                // The tombstone parser built with protoc uses the tombstone schema, then parses the trace.
                                TombstoneProtos.Tombstone tombstone = TombstoneProtos.Tombstone.parseFrom(tombstoneInputStream);
                                Timber.e("Tombstone Info%s", tombstone.toString());
                                tombstoneInputStream.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void checkForUnsafeIntentLaunch() {
        boolean isDebug = ((this.getApplicationInfo().flags &
                ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        if (isDebug) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    // Other StrictMode checks that you've previously added.
                    .detectUnsafeIntentLaunch()
                    .penaltyLog()
                    .build());
        }
    }

    public AppRTCAudioManager getAudioManager() {
        return rtcAudioManager;
    }

    public void createOrUpdateAudioManager(boolean isSpeakerOn, int type) {
        Timber.d("Create or update audio manager, type is %s", type);
        chatManagement.registerScreenReceiver();

        if (type == AUDIO_MANAGER_CALL_RINGING) {
            if (rtcAudioManagerRingInCall != null) {
                removeRTCAudioManagerRingIn();
            }

            registerReceiver(volumeReceiver, new IntentFilter(VOLUME_CHANGED_ACTION));
            registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
            Timber.d("Creating RTC Audio Manager (ringing mode)");
            rtcAudioManagerRingInCall = AppRTCAudioManager.create(this, false, AUDIO_MANAGER_CALL_RINGING);
        } else {
            if (rtcAudioManager != null) {
                rtcAudioManager.setTypeAudioManager(type);
                return;
            }

            Timber.d("Creating RTC Audio Manager (%d mode)", type);
            removeRTCAudioManagerRingIn();
            MegaApplication.isSpeakerOn = isSpeakerOn;
            rtcAudioManager = AppRTCAudioManager.create(this, isSpeakerOn, type);
            if (type != AUDIO_MANAGER_CREATING_JOINING_MEETING) {
                startProximitySensor();
            }
        }
    }

    /**
     * Remove the incoming call AppRTCAudioManager.
     */
    public void removeRTCAudioManagerRingIn() {
        if (rtcAudioManagerRingInCall == null)
            return;

        try {
            Timber.d("Removing RTC Audio Manager");
            rtcAudioManagerRingInCall.stop();
            rtcAudioManagerRingInCall = null;
            unregisterReceiver(volumeReceiver);
            unregisterReceiver(becomingNoisyReceiver);
        } catch (Exception e) {
            Timber.e(e, "Exception stopping speaker audio manager");
        }
    }

    /**
     * Remove the ongoing call AppRTCAudioManager.
     */
    public void removeRTCAudioManager() {
        if (rtcAudioManager == null)
            return;

        try {
            Timber.d("Removing RTC Audio Manager");
            rtcAudioManager.stop();
            rtcAudioManager = null;
        } catch (Exception e) {
            Timber.e(e, "Exception stopping speaker audio manager");
        }
    }

    /**
     * Method for updating the call status of the Audio Manger.
     *
     * @param callStatus Call status.
     */
    private void updateRTCAudioMangerTypeStatus(int callStatus) {
        removeRTCAudioManagerRingIn();
        stopSounds();
        if (rtcAudioManager != null) {
            rtcAudioManager.setTypeAudioManager(callStatus);
        }
    }

    /**
     * Method for updating the call status of the Speaker status .
     *
     * @param isSpeakerOn If the speaker is on.
     * @param typeStatus  type AudioManager.
     */
    public void updateSpeakerStatus(boolean isSpeakerOn, int typeStatus) {
        if (rtcAudioManager != null) {
            rtcAudioManager.updateSpeakerStatus(isSpeakerOn, typeStatus);
        }
    }

    /**
     * Activate the proximity sensor.
     */
    public void startProximitySensor() {
        if (rtcAudioManager != null && rtcAudioManager.startProximitySensor()) {
            Timber.d("Proximity sensor started");
            rtcAudioManager.setOnProximitySensorListener(isNear -> {
                chatManagement.controlProximitySensor(isNear);
            });
        }
    }

    /**
     * Deactivates the proximity sensor
     */
    public void unregisterProximitySensor() {
        if (rtcAudioManager != null) {
            Timber.d("Stopping proximity sensor...");
            rtcAudioManager.unregisterProximitySensor();
        }
    }

    /*
     * Method for stopping the sound of incoming or outgoing calls.
     */
    public void stopSounds() {
        if (rtcAudioManager != null) {
            rtcAudioManager.stopAudioSignals();
        }
        if (rtcAudioManagerRingInCall != null) {
            rtcAudioManagerRingInCall.stopAudioSignals();
        }
    }

    public void openCallService(long chatId) {
        if (chatId != MEGACHAT_INVALID_HANDLE) {
            Timber.d("Start call Service. Chat iD = %s", chatId);
            Intent intentService = new Intent(this, CallService.class);
            intentService.putExtra(CHAT_ID, chatId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.startForegroundService(intentService);
            } else {
                this.startService(intentService);
            }
        }
    }

    public void checkQueuedCalls(long incomingCallChatId) {
        try {
            ChatAdvancedNotificationBuilder notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this);
            notificationBuilder.checkQueuedCalls(incomingCallChatId);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public void launchCallActivity(MegaChatCall call) {
        Timber.d("Show the call screen: %s, callId = %d", callStatusToString(call.getStatus()), call.getCallId());
        openMeetingRinging(this, call.getChatid(), passcodeManagement);
    }

    /**
     * Resets all SingleObjects to their default values.
     */
    private void resetDefaults() {
        sortOrderManagement.resetDefaults();
        passcodeManagement.resetDefaults();
        myAccountInfo.resetDefaults();
        transfersManagement.resetDefaults();
    }

    public static boolean isShowRichLinkWarning() {
        return showRichLinkWarning;
    }

    public static void setShowRichLinkWarning(boolean showRichLinkWarning) {
        MegaApplication.showRichLinkWarning = showRichLinkWarning;
    }

    public static boolean isEnabledGeoLocation() {
        return enabledGeoLocation;
    }

    public static void setEnabledGeoLocation(boolean enabledGeoLocation) {
        MegaApplication.enabledGeoLocation = enabledGeoLocation;
    }

    public static int getCounterNotNowRichLinkWarning() {
        return counterNotNowRichLinkWarning;
    }

    public static void setCounterNotNowRichLinkWarning(int counterNotNowRichLinkWarning) {
        MegaApplication.counterNotNowRichLinkWarning = counterNotNowRichLinkWarning;
    }

    public static boolean isEnabledRichLinks() {
        return enabledRichLinks;
    }

    public static void setEnabledRichLinks(boolean enabledRichLinks) {
        MegaApplication.enabledRichLinks = enabledRichLinks;
    }

    public static int isDisableFileVersions() {
        return disableFileVersions;
    }

    public static void setDisableFileVersions(boolean disableFileVersions) {
        if (disableFileVersions) {
            MegaApplication.disableFileVersions = 1;
        } else {
            MegaApplication.disableFileVersions = 0;
        }
    }

    public boolean isEsid() {
        return esid;
    }

    public void setEsid(boolean esid) {
        this.esid = esid;
    }

    public static boolean isClosedChat() {
        return closedChat;
    }

    public static void setClosedChat(boolean closedChat) {
        MegaApplication.closedChat = closedChat;
    }

    public MyAccountInfo getMyAccountInfo() {
        return myAccountInfo;
    }

    public void resetMyAccountInfo() {
        myAccountInfo.resetDefaults();
    }

    public int getStorageState() {
        return storageState;
    }

    public void setStorageState(int state) {
        this.storageState = state;
    }

    public static boolean isVerifySMSShowed() {
        return isVerifySMSShowed;
    }

    public static void setIsBlockedDueToWeakAccount(boolean isBlockedDueToWeakAccount) {
        MegaApplication.isBlockedDueToWeakAccount = isBlockedDueToWeakAccount;
    }

    public static boolean isBlockedDueToWeakAccount() {
        return isBlockedDueToWeakAccount;
    }

    public static void setIsWebOpenDueToEmailVerification(boolean isWebOpenDueToEmailVerification) {
        MegaApplication.isWebOpenDueToEmailVerification = isWebOpenDueToEmailVerification;
    }

    public static boolean isWebOpenDueToEmailVerification() {
        return isWebOpenDueToEmailVerification;
    }

    public void setIsLoggingRunning(boolean isLoggingRunning) {
        MegaApplication.isLoggingRunning = isLoggingRunning;
    }

    public boolean isLoggingRunning() {
        return isLoggingRunning;
    }

    public static PushNotificationSettingManagement getPushNotificationSettingManagement() {
        return pushNotificationSettingManagement;
    }

    public static ChatManagement getChatManagement() {
        return chatManagement;
    }

    public static void setVerifyingCredentials(boolean verifyingCredentials) {
        MegaApplication.verifyingCredentials = verifyingCredentials;
    }

    public static boolean isVerifyingCredentials() {
        return MegaApplication.verifyingCredentials;
    }

    public Activity getCurrentActivity() {
        return activityLifecycleHandler.getCurrentActivity();
    }

    public static boolean isWaitingForCall() {
        return isWaitingForCall;
    }

    public static void setIsWaitingForCall(boolean isWaitingForCall) {
        MegaApplication.isWaitingForCall = isWaitingForCall;
    }

    public static long getUserWaitingForCall() {
        return userWaitingForCall;
    }

    public static void setUserWaitingForCall(long userWaitingForCall) {
        MegaApplication.userWaitingForCall = userWaitingForCall;
    }

    public static boolean areAdvertisingCookiesEnabled() {
        return areAdvertisingCookiesEnabled;
    }

    public static void setAdvertisingCookiesEnabled(boolean enabled) {
        areAdvertisingCookiesEnabled = enabled;
    }
}
