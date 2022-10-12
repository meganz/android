package mega.privacy.android.app;

import static mega.privacy.android.app.utils.CallUtil.showConfirmationInACall;
import static mega.privacy.android.app.utils.Constants.ACCOUNT_INVITATION_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.ACTION_CANCEL_ACCOUNT;
import static mega.privacy.android.app.utils.Constants.ACTION_CHANGE_MAIL;
import static mega.privacy.android.app.utils.Constants.ACTION_CHAT_SUMMARY;
import static mega.privacy.android.app.utils.Constants.ACTION_CONFIRM;
import static mega.privacy.android.app.utils.Constants.ACTION_EXPORT_MASTER_KEY;
import static mega.privacy.android.app.utils.Constants.ACTION_IPC;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_CHAT_LINK;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_CONTACTS_SECTION;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_HANDLE_NODE;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_FOLDER_LINK;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_LINK;
import static mega.privacy.android.app.utils.Constants.ACTION_RESET_PASS;
import static mega.privacy.android.app.utils.Constants.BUSINESS_INVITE_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.CANCEL_ACCOUNT_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.CHAT_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.CHECK_LINK_TYPE_MEETING_LINK;
import static mega.privacy.android.app.utils.Constants.CHECK_LINK_TYPE_UNKNOWN_LINK;
import static mega.privacy.android.app.utils.Constants.CONFIRMATION_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.CONTACT_HANDLE;
import static mega.privacy.android.app.utils.Constants.CONTACT_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.CREATE_ACCOUNT_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.EMAIL;
import static mega.privacy.android.app.utils.Constants.EMAIL_VERIFY_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.EXPORT_MASTER_KEY_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.EXTRA_CONFIRMATION;
import static mega.privacy.android.app.utils.Constants.FILE_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.FOLDER_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.HANDLE_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.LINK_IS_FOR_MEETING;
import static mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.MEGA_BLOG_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.MEGA_DROP_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.MEGA_REGEXS;
import static mega.privacy.android.app.utils.Constants.NEW_MESSAGE_CHAT_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.OPENED_FROM_CHAT;
import static mega.privacy.android.app.utils.Constants.PASSWORD_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.PENDING_CONTACTS_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.RESET_PASSWORD_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.REVERT_CHANGE_PASSWORD_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.VERIFY_CHANGE_MAIL_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.WEB_SESSION_LINK_REGEXS;
import static mega.privacy.android.app.utils.LinksUtil.requiresTransferSession;
import static mega.privacy.android.app.utils.Util.decodeURL;
import static mega.privacy.android.app.utils.Util.matchRegexs;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlinx.coroutines.CoroutineScope;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.activities.WebViewActivity;
import mega.privacy.android.data.database.DatabaseHandler;
import mega.privacy.android.domain.qualifier.ApplicationScope;
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler;
import mega.privacy.android.app.listeners.LoadPreviewListener;
import mega.privacy.android.app.listeners.QueryRecoveryLinkListener;
import mega.privacy.android.app.main.FileLinkActivity;
import mega.privacy.android.app.main.FolderLinkActivity;
import mega.privacy.android.app.main.LoginActivity;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.controllers.AccountController;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity;
import mega.privacy.android.app.meeting.fragments.MeetingHasEndedDialogFragment;
import mega.privacy.android.app.usecase.QuerySignupLinkUseCase;
import mega.privacy.android.app.utils.CallUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.TextUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import timber.log.Timber;

@AndroidEntryPoint
public class OpenLinkActivity extends PasscodeActivity implements MegaRequestListenerInterface, View.OnClickListener, LoadPreviewListener.OnPreviewLoadedCallback {

    @ApplicationScope
    @Inject
    CoroutineScope sharingScope;
    @Inject
    QuerySignupLinkUseCase querySignupLinkUseCase;
    @Inject
    DatabaseHandler dbH;
    @Inject
    MegaChatRequestHandler chatRequestHandler;

    private String urlConfirmationLink = null;

    private TextView processingText;
    private TextView errorText;
    private ProgressBar progressBar;
    private RelativeLayout containerOkButton;

    private boolean isLoggedIn;
    private boolean needsRefreshSession;

    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        url = intent.getDataString();
        Timber.d("Original url: %s", url);

        setContentView(R.layout.activity_open_link);

        processingText = findViewById(R.id.open_link_text);
        errorText = findViewById(R.id.open_link_error);
        errorText.setVisibility(View.GONE);
        progressBar = findViewById(R.id.open_link_bar);
        containerOkButton = findViewById(R.id.container_accept_button);
        containerOkButton.setVisibility(View.GONE);
        containerOkButton.setOnClickListener(this);

        url = decodeURL(url);

        isLoggedIn = dbH != null && dbH.getCredentials() != null;
        needsRefreshSession = megaApi.getRootNode() == null;

        // If is not a MEGA link, is not a supported link
        if (!matchRegexs(url, MEGA_REGEXS)) {
            Timber.d("The link is not a MEGA link: %s", url);
            setError(getString(R.string.open_link_not_valid_link));
            return;
        }

        // Email verification link
        if (matchRegexs(url, EMAIL_VERIFY_LINK_REGEXS)) {
            Timber.d("Open email verification link");
            MegaApplication.setIsWebOpenDueToEmailVerification(true);
            openWebLink(url);
            return;
        }

        // Web session link
        if (matchRegexs(url, WEB_SESSION_LINK_REGEXS)) {
            Timber.d("Open web session link");
            openWebLink(url);
            return;
        }

        if (matchRegexs(url, BUSINESS_INVITE_LINK_REGEXS)) {
            Timber.d("Open business invite link");
            openWebLink(url);
            return;
        }

        //MEGA DROP link
        if (matchRegexs(url, MEGA_DROP_LINK_REGEXS)) {
            Timber.d("Open MEGAdrop link");
            openWebLink(url);
            return;
        }

        // File link
        if (matchRegexs(url, FILE_LINK_REGEXS)) {
            Timber.d("Open link url");

            Intent openFileIntent = new Intent(this, FileLinkActivity.class);
            openFileIntent.putExtra(OPENED_FROM_CHAT, intent.getBooleanExtra(OPENED_FROM_CHAT, false));
            openFileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openFileIntent.setAction(ACTION_OPEN_MEGA_LINK);
            openFileIntent.setData(Uri.parse(url));
            startActivity(openFileIntent);
            finish();
            return;
        }

        // Confirmation link
        if (matchRegexs(url, CONFIRMATION_LINK_REGEXS)) {
            Timber.d("Confirmation url");
            urlConfirmationLink = url;

            app.setUrlConfirmationLink(urlConfirmationLink);

            AccountController.logout(this, megaApi, sharingScope);

            return;
        }

        // Folder Download link
        if (matchRegexs(url, FOLDER_LINK_REGEXS)) {
            Timber.d("Folder link url");

            Intent openFolderIntent = new Intent(this, FolderLinkActivity.class);
            openFolderIntent.putExtra(OPENED_FROM_CHAT, intent.getBooleanExtra(OPENED_FROM_CHAT, false));
            openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openFolderIntent.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);
            openFolderIntent.setData(Uri.parse(url));
            startActivity(openFolderIntent);
            finish();

            return;
        }

        // Chat link or Meeting link
        if (matchRegexs(url, CHAT_LINK_REGEXS)) {
            Timber.d("Open chat url");

            if (dbH != null) {
                if (isLoggedIn) {
                    Timber.d("Logged IN");
                    Intent openChatLinkIntent = new Intent(this, ManagerActivity.class);
                    openChatLinkIntent.setAction(ACTION_OPEN_CHAT_LINK);
                    openChatLinkIntent.setData(Uri.parse(url));
                    startActivity(openChatLinkIntent);
                    finish();
                } else {
                    Timber.d("Not logged");
                    int initResult = megaChatApi.getInitState();
                    if (initResult < MegaChatApi.INIT_WAITING_NEW_SESSION) {
                        initResult = megaChatApi.initAnonymous();
                        Timber.d("Chat init anonymous result: %s", initResult);
                    }

                    if (initResult != MegaChatApi.INIT_ERROR) {
                        finishAfterConnect();
                    } else {
                        Timber.e("Open chat url:initAnonymous:INIT_ERROR");
                        setError(getString(R.string.error_chat_link_init_error));
                    }
                }
            }
            return;
        }

        // Password link
        if (matchRegexs(url, PASSWORD_LINK_REGEXS)) {
            Timber.d("Link with password url");

            Intent openLinkIntent = new Intent(this, OpenPasswordLinkActivity.class);
            openLinkIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openLinkIntent.setData(Uri.parse(url));
            startActivity(openLinkIntent);
            finish();

            return;
        }

        // Create account invitation - user must be logged OUT
        if (matchRegexs(url, ACCOUNT_INVITATION_LINK_REGEXS)) {
            Timber.d("New signup url");

            if (dbH != null) {
                if (isLoggedIn) {
                    Timber.d("Logged IN");
                    setError(getString(R.string.log_out_warning));
                } else {
                    querySignupLinkUseCase.query(url)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result, throwable) -> {
                                if (throwable == null) {
                                    Timber.d("Not logged");
                                    startActivity(new Intent(this, LoginActivity.class)
                                            .putExtra(VISIBLE_FRAGMENT, CREATE_ACCOUNT_FRAGMENT)
                                            .putExtra(EMAIL, result));
                                    finish();
                                } else {
                                    Timber.e(throwable);
                                }
                            });
                }
            }
            return;
        }

        // Export Master Key link - user must be logged IN
        if (matchRegexs(url, EXPORT_MASTER_KEY_LINK_REGEXS)) {
            Timber.d("Export master key url");

            if (dbH != null) {
                if (isLoggedIn) {
                    Timber.d("Logged IN"); //Check fetch nodes is already done in ManagerActivity
                    Intent exportIntent = new Intent(this, ManagerActivity.class);
                    exportIntent.setAction(ACTION_EXPORT_MASTER_KEY);
                    startActivity(exportIntent);
                    finish();
                } else {
                    Timber.d("Not logged");
                    setError(getString(R.string.alert_not_logged_in));
                }
            }
            return;
        }

        // New mwssage chat- user must be logged IN
        if (matchRegexs(url, NEW_MESSAGE_CHAT_LINK_REGEXS)) {
            Timber.d("New message chat url");

            if (dbH != null) {
                if (isLoggedIn) {
                    Timber.d("Logged IN"); //Check fetch nodes is already done in ManagerActivity
                    Intent chatIntent = new Intent(this, ManagerActivity.class);
                    chatIntent.setAction(ACTION_CHAT_SUMMARY);
                    startActivity(chatIntent);
                    finish();
                } else {
                    Timber.d("Not logged");
                    setError(getString(R.string.alert_not_logged_in));
                }
            }
            return;
        }

        // Cancel account  - user must be logged IN
        if (matchRegexs(url, CANCEL_ACCOUNT_LINK_REGEXS)) {
            Timber.d("Cancel account url");

            if (dbH != null) {
                if (isLoggedIn) {
                    if (needsRefreshSession) {
                        Timber.d("Go to Login to fetch nodes");
                        Intent cancelAccountIntent = new Intent(this, LoginActivity.class);
                        cancelAccountIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        cancelAccountIntent.setAction(ACTION_CANCEL_ACCOUNT);
                        cancelAccountIntent.setData(Uri.parse(url));
                        startActivity(cancelAccountIntent);
                    } else {
                        Timber.d("Logged IN");
                        startActivity(new Intent(this, ManagerActivity.class)
                                .setAction(ACTION_CANCEL_ACCOUNT)
                                .setData(Uri.parse(url)));
                    }
                    finish();
                } else {
                    Timber.d("Not logged");
                    setError(getString(R.string.alert_not_logged_in));
                }
            }
            return;
        }

        // Verify change mail - user must be logged IN
        if (matchRegexs(url, VERIFY_CHANGE_MAIL_LINK_REGEXS)) {
            Timber.d("Verify mail url");

            if (dbH != null) {
                if (isLoggedIn) {
                    if (needsRefreshSession) {
                        Timber.d("Go to Login to fetch nodes");
                        Intent changeMailIntent = new Intent(this, LoginActivity.class);
                        changeMailIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        changeMailIntent.setAction(ACTION_CHANGE_MAIL);
                        changeMailIntent.setData(Uri.parse(url));
                        startActivity(changeMailIntent);
                        finish();

                    } else {
                        startActivity(new Intent(this, ManagerActivity.class)
                                .setAction(ACTION_CHANGE_MAIL)
                                .setData(Uri.parse(url)));
                    }
                } else {
                    setError(getString(R.string.change_email_not_logged_in));
                }
            }
            return;
        }

        // Reset password - two options: logged IN or OUT
        if (matchRegexs(url, RESET_PASSWORD_LINK_REGEXS)) {
            Timber.d("Reset pass url");

            //Check if link with MK or not
            if (dbH != null) {
                if (isLoggedIn) {
                    if (needsRefreshSession) {
                        Timber.d("Go to Login to fetch nodes");
                        Intent resetPassIntent = new Intent(this, LoginActivity.class);
                        resetPassIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        resetPassIntent.setAction(ACTION_RESET_PASS);
                        resetPassIntent.setData(Uri.parse(url));
                        startActivity(resetPassIntent);
                        finish();

                    } else {
                        Timber.d("Logged IN");
                        Intent resetPassIntent = new Intent(this, ManagerActivity.class);
                        resetPassIntent.setAction(ACTION_RESET_PASS);
                        resetPassIntent.setData(Uri.parse(url));
                        startActivity(resetPassIntent);
                        finish();
                    }
                } else {
                    Timber.d("Not logged");
                    megaApi.queryResetPasswordLink(url, new QueryRecoveryLinkListener(this));
                }
            }
            return;
        }

        // Pending contacts
        if (matchRegexs(url, PENDING_CONTACTS_LINK_REGEXS)) {
            Timber.d("Pending contacts url");

            if (dbH != null) {
                if (isLoggedIn) {
                    if (needsRefreshSession) {
                        Timber.d("Go to Login to fetch nodes");
                        Intent ipcIntent = new Intent(this, LoginActivity.class);
                        ipcIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        ipcIntent.setAction(ACTION_IPC);
                        startActivity(ipcIntent);
                        finish();

                    } else {
                        Timber.d("Logged IN");
                        Intent ipcIntent = new Intent(this, ManagerActivity.class);
                        ipcIntent.setAction(ACTION_IPC);
                        startActivity(ipcIntent);
                        finish();
                    }
                } else {
                    Timber.w("Not logged");
                    setError(getString(R.string.alert_not_logged_in));
                }
            }
            return;
        }

        if (matchRegexs(url, REVERT_CHANGE_PASSWORD_LINK_REGEXS)
                || matchRegexs(url, MEGA_BLOG_LINK_REGEXS)) {
            Timber.d("Open revert password change link: %s", url);

            openWebLink(url);
            return;
        }

        if (matchRegexs(url, HANDLE_LINK_REGEXS)) {
            Timber.d("Handle link url");

            Intent handleIntent = new Intent(this, ManagerActivity.class);
            handleIntent.setAction(ACTION_OPEN_HANDLE_NODE);
            handleIntent.setData(Uri.parse(url));
            startActivity(handleIntent);
            finish();
            return;
        }

        //Contact link
        if (matchRegexs(url, CONTACT_LINK_REGEXS)) { //https://mega.nz/C!
            if (dbH != null) {
                if (isLoggedIn) {
                    String[] s = url.split("C!");
                    long handle = MegaApiAndroid.base64ToHandle(s[1].trim());
                    Intent inviteContact = new Intent(this, ManagerActivity.class);
                    inviteContact.setAction(ACTION_OPEN_CONTACTS_SECTION);
                    inviteContact.putExtra(CONTACT_HANDLE, handle);
                    startActivity(inviteContact);
                    finish();
                } else {
                    Timber.w("Not logged");
                    setError(getString(R.string.alert_not_logged_in));
                }
                return;
            }
        }

        // Browser open the link which does not require app to handle
        Timber.d("Browser open link: %s", url);
        checkIfRequiresTransferSession(url);
    }

    public void finishAfterConnect() {
        megaChatApi.checkChatLink(url, new LoadPreviewListener(this, OpenLinkActivity.this, CHECK_LINK_TYPE_UNKNOWN_LINK));
    }

    private void goToChatActivity() {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.setAction(ACTION_OPEN_CHAT_LINK);
        intent.setData(Uri.parse(url));
        startActivity(intent);
        finish();
    }

    private void goToGuestLeaveMeetingActivity() {
        Intent intent = new Intent(this, LeftMeetingActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToMeetingActivity(long chatId, String meetingName) {
        CallUtil.openMeetingGuestMode(this, meetingName, chatId, url, passcodeManagement, chatRequestHandler);
        finish();
    }

    private void checkIfRequiresTransferSession(String url) {
        if (!requiresTransferSession(this, url)) {
            openWebLink(url);
        }
    }

    public void openWebLink(String url) {
        Intent openIntent = new Intent(this, WebViewActivity.class);
        openIntent.setData(Uri.parse(url));
        startActivity(openIntent);
        finish();
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        Timber.d("onRequestStart");
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        Timber.d("onRequestFinish");
        if (request.getType() == MegaRequest.TYPE_LOGOUT) {
            Timber.d("END logout sdk request - wait chat logout");

            if (MegaApplication.getUrlConfirmationLink() != null) {
                Timber.d("Confirmation link - show confirmation screen");
                if (dbH != null) {
                    dbH.clearEphemeral();
                }

                AccountController.logoutConfirmed(this, sharingScope);

                Intent confirmIntent = new Intent(this, LoginActivity.class);
                confirmIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                confirmIntent.putExtra(EXTRA_CONFIRMATION, urlConfirmationLink);
                confirmIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                confirmIntent.setAction(ACTION_CONFIRM);
                startActivity(confirmIntent);
                MegaApplication.setUrlConfirmationLink(null);
                finish();
            }
        } else if (request.getType() == MegaRequest.TYPE_QUERY_SIGNUP_LINK) {
            Timber.d("MegaRequest.TYPE_QUERY_SIGNUP_LINK");

            if (e.getErrorCode() == MegaError.API_OK) {
                MegaApplication.setUrlConfirmationLink(request.getLink());

                AccountController.logout(this, megaApi, sharingScope);
            } else {
                setError(getString(R.string.invalid_link));
            }
        }
    }

    public void setError(String string) {
        processingText.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        errorText.setText(string);
        errorText.setVisibility(View.VISIBLE);
        containerOkButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.container_accept_button: {
                this.finish();
                break;
            }
        }
    }

    @Override
    public void onPreviewLoaded(MegaChatRequest request, boolean alreadyExist) {
        long chatId = request.getChatHandle();
        boolean isFromOpenChatPreview = request.getFlag();
        int type = request.getParamType();
        boolean linkInvalid = TextUtil.isTextEmpty(request.getLink()) && chatId == MEGACHAT_INVALID_HANDLE;
        Timber.d("Chat id: %d, type: %d, flag: %s", chatId, type, isFromOpenChatPreview);

        if (linkInvalid) {
            setError(getString(R.string.invalid_link));
            return;
        }

        if (type == LINK_IS_FOR_MEETING) {
            Timber.d("It's a meeting link");
            if (CallUtil.participatingInACall()) {
                showConfirmationInACall(this, StringResourcesUtils.getString(R.string.text_join_call), passcodeManagement);
            } else {
                if (CallUtil.isMeetingEnded(request.getMegaHandleList())) {
                    Timber.d("Meeting has ended, open dialog");
                    new MeetingHasEndedDialogFragment(new MeetingHasEndedDialogFragment.ClickCallback() {
                        @Override
                        public void onViewMeetingChat() {
                        }

                        @Override
                        public void onLeave() {
                            goToGuestLeaveMeetingActivity();
                        }
                    }, true).show(getSupportFragmentManager(),
                            MeetingHasEndedDialogFragment.TAG);
                } else if (isFromOpenChatPreview) {
                    Timber.d("Meeting is in progress, open join meeting");
                    goToMeetingActivity(chatId, request.getText());
                } else {
                    Timber.d("It's a meeting, open chat preview");
                    Timber.d("openChatPreview");
                    megaChatApi.openChatPreview(url, new LoadPreviewListener(this, OpenLinkActivity.this, CHECK_LINK_TYPE_MEETING_LINK));
                }
            }

        } else {
            Timber.d("It's a chat link");
            goToChatActivity();
        }
    }

    @Override
    public void onErrorLoadingPreview(int errorCode) {
        setError(getString(R.string.invalid_link));
    }
}
