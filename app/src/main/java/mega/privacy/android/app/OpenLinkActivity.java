package mega.privacy.android.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.WebViewActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;


import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class OpenLinkActivity extends PinActivityLollipop implements MegaRequestListenerInterface, View.OnClickListener {

	private MegaApplication app;
	private MegaApiAndroid megaApi;
	private MegaChatApiAndroid megaChatApi;
	private DatabaseHandler dbH = null;

	private String urlConfirmationLink = null;

	private TextView processingText;
	private TextView errorText;
	private ProgressBar progressBar;
	private RelativeLayout containerOkButton;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		app = (MegaApplication) getApplication();
		megaApi = app.getMegaApi();
		megaChatApi = app.getMegaChatApi();

		Intent intent = getIntent();
		String url = intent.getDataString();
		logDebug("Original url: " + url);

		setContentView(R.layout.activity_open_link);

		processingText = findViewById(R.id.open_link_text);
		errorText = findViewById(R.id.open_link_error);
		errorText.setVisibility(View.GONE);
		progressBar = findViewById(R.id.open_link_bar);
		containerOkButton = findViewById(R.id.container_accept_button);
		containerOkButton.setVisibility(View.GONE);
		containerOkButton.setOnClickListener(this);

		decodeURL(url);

		// File link
		if (matchRegexs(url, FILE_LINK_REGEXS)) {
			logDebug("Open link url");

			Intent openFileIntent = new Intent(this, FileLinkActivityLollipop.class);
			openFileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openFileIntent.setAction(ACTION_OPEN_MEGA_LINK);
			openFileIntent.setData(Uri.parse(url));
			startActivity(openFileIntent);
			finish();
			return;
		}

		// Confirmation link
		if (matchRegexs(url, CONFIRMATION_LINK_REGEXS)) {
			logDebug("Confirmation url");
			urlConfirmationLink = url;

			AccountController aC = new AccountController(this);
			MegaApplication.setUrlConfirmationLink(urlConfirmationLink);

			aC.logout(this, megaApi);

			return;
		}

		// Folder Download link
        if (matchRegexs(url, FOLDER_LINK_REGEXS)) {
			logDebug("Folder link url");

			Intent openFolderIntent = new Intent(this, FolderLinkActivityLollipop.class);
			openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openFolderIntent.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);
			openFolderIntent.setData(Uri.parse(url));
			startActivity(openFolderIntent);
			finish();

			return;
		}

		// Chat link
        if (matchRegexs(url, CHAT_LINK_REGEXS)) {
			logDebug("Open chat url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					logDebug("Logged IN");
					Intent openChatLinkIntent = new Intent(this, ManagerActivityLollipop.class);
					openChatLinkIntent.setAction(ACTION_OPEN_CHAT_LINK);
					openChatLinkIntent.setData(Uri.parse(url));
					startActivity(openChatLinkIntent);
					finish();
				}
				else{
					logDebug("Not logged");
					int initResult = megaChatApi.getInitState();
					if(initResult<MegaChatApi.INIT_WAITING_NEW_SESSION){
						initResult = megaChatApi.initAnonymous();
					}

					if(initResult!= MegaChatApi.INIT_ERROR){
						Intent openChatLinkIntent = new Intent(this, ChatActivityLollipop.class);
						openChatLinkIntent.setAction(ACTION_OPEN_CHAT_LINK);
						openChatLinkIntent.setData(Uri.parse(url));
						startActivity(openChatLinkIntent);
						finish();
					}
					else{
						logError("Open chat url:initAnonymous:INIT_ERROR");
						setError(getString(R.string.error_chat_link_init_error));
					}
				}
			}
			return;
		}

		// Password link
		if (matchRegexs(url, PASSWORD_LINK_REGEXS)) {
			logDebug("Link with password url");

			Intent openLinkIntent = new Intent(this, OpenPasswordLinkActivity.class);
			openLinkIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openLinkIntent.setData(Uri.parse(url));
			startActivity(openLinkIntent);
			finish();

			return;
		}

		// Create account invitation - user must be logged OUT
		if (matchRegexs(url, ACCOUNT_INVITATION_LINK_REGEXS)) {
			logDebug("New signup url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					logDebug("Logged IN");
					setError(getString(R.string.log_out_warning));
				}
				else{
					logDebug("Not logged");
					Intent createAccountIntent = new Intent(this, LoginActivityLollipop.class);
					createAccountIntent.putExtra(VISIBLE_FRAGMENT, CREATE_ACCOUNT_FRAGMENT);
					startActivity(createAccountIntent);
					finish();
				}
			}
			return;
		}

		// Export Master Key link - user must be logged IN
		if (matchRegexs(url, EXPORT_MASTER_KEY_LINK_REGEXS)) {
			logDebug("Export master key url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					logDebug("Logged IN"); //Check fetch nodes is already done in ManagerActivity
					Intent exportIntent = new Intent(this, ManagerActivityLollipop.class);
					exportIntent.setAction(ACTION_EXPORT_MASTER_KEY);
					startActivity(exportIntent);
					finish();
				} else {
					logDebug("Not logged");
					setError(getString(R.string.alert_not_logged_in));
				}
			}
			return;
		}

		// New mwssage chat- user must be logged IN
		if (matchRegexs(url, NEW_MESSAGE_CHAT_LINK_REGEXS)) {
			logDebug("New message chat url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					logDebug("Logged IN"); //Check fetch nodes is already done in ManagerActivity
					Intent chatIntent = new Intent(this, ManagerActivityLollipop.class);
					chatIntent.setAction(ACTION_CHAT_SUMMARY);
					startActivity(chatIntent);
					finish();
				} else {
					logDebug("Not logged");
					setError(getString(R.string.alert_not_logged_in));
				}
			}
			return;
		}

		// Cancel account  - user must be logged IN
		if (matchRegexs(url, CANCEL_ACCOUNT_LINK_REGEXS)) {
			logDebug("Cancel account url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					MegaNode rootNode = megaApi.getRootNode();
					if (rootNode == null) {
						logDebug("Go to Login to fetch nodes");
						Intent cancelAccountIntent = new Intent(this, LoginActivityLollipop.class);
						cancelAccountIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
						cancelAccountIntent.setAction(ACTION_CANCEL_ACCOUNT);
						cancelAccountIntent.setData(Uri.parse(url));
						startActivity(cancelAccountIntent);
						finish();

					} else {
						logDebug("Logged IN");
						megaApi.queryCancelLink(url, this);
					}
				} else {
					logDebug("Not logged");
					setError(getString(R.string.alert_not_logged_in));
				}
			}
			return;
		}

		// Verify change mail - user must be logged IN
		if (matchRegexs(url, VERIFY_CHANGE_MAIL_LINK_REGEXS)) {
			logDebug("Verify mail url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					MegaNode rootNode = megaApi.getRootNode();
					if (rootNode == null) {
						logDebug("Go to Login to fetch nodes");
						Intent changeMailIntent = new Intent(this, LoginActivityLollipop.class);
						changeMailIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
						changeMailIntent.setAction(ACTION_CHANGE_MAIL);
						changeMailIntent.setData(Uri.parse(url));
						startActivity(changeMailIntent);
						finish();

					} else {
						logDebug("Logged IN");
						Intent changeMailIntent = new Intent(this, ManagerActivityLollipop.class);
						changeMailIntent.setAction(ACTION_CHANGE_MAIL);
						changeMailIntent.setData(Uri.parse(url));
						startActivity(changeMailIntent);
						finish();
					}
				} else {
					logWarning("Not logged");
					setError(getString(R.string.alert_not_logged_in));
				}
			}
			return;
		}

		// Reset password - two options: logged IN or OUT
		if (matchRegexs(url, RESET_PASSWORD_LINK_REGEXS)) {
			logDebug("Reset pass url");

			//Check if link with MK or not
			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					MegaNode rootNode = megaApi.getRootNode();
					if (rootNode == null) {
						logDebug("Go to Login to fetch nodes");
						Intent resetPassIntent = new Intent(this, LoginActivityLollipop.class);
						resetPassIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
						resetPassIntent.setAction(ACTION_RESET_PASS);
						resetPassIntent.setData(Uri.parse(url));
						startActivity(resetPassIntent);
						finish();

					} else {
						logDebug("Logged IN");
						Intent resetPassIntent = new Intent(this, ManagerActivityLollipop.class);
						resetPassIntent.setAction(ACTION_RESET_PASS);
						resetPassIntent.setData(Uri.parse(url));
						startActivity(resetPassIntent);
						finish();
					}
				} else {
					logDebug("Not logged");
					megaApi.queryResetPasswordLink(url, this);
				}
			}
			return;
		}

		// Pending contacts
		if (matchRegexs(url, PENDING_CONTACTS_LINK_REGEXS)) {
			logDebug("Pending contacts url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					MegaNode rootNode = megaApi.getRootNode();
					if (rootNode == null) {
						logDebug("Go to Login to fetch nodes");
						Intent ipcIntent = new Intent(this, LoginActivityLollipop.class);
						ipcIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
						ipcIntent.setAction(ACTION_IPC);
						startActivity(ipcIntent);
						finish();

					} else {
						logDebug("Logged IN");
						Intent ipcIntent = new Intent(this, ManagerActivityLollipop.class);
						ipcIntent.setAction(ACTION_IPC);
						startActivity(ipcIntent);
						finish();
					}
				} else {
					logWarning("Not logged");
					setError(getString(R.string.alert_not_logged_in));
				}
			}
			return;
		}

		if (matchRegexs(url, REVERT_CHANGE_PASSWORD_LINK_REGEXS)
				|| matchRegexs(url, MEGA_BLOG_LINK_REGEXS)) {
			logDebug("Open revert password change link: " + url);

			Intent openIntent = new Intent(this, WebViewActivityLollipop.class);
			openIntent.setData(Uri.parse(url));
			startActivity(openIntent);
			finish();
			return;
		}

		if (matchRegexs(url, HANDLE_LINK_REGEXS)) {
			logDebug("Handle link url");

			Intent handleIntent = new Intent(this, ManagerActivityLollipop.class);
			handleIntent.setAction(ACTION_OPEN_HANDLE_NODE);
			handleIntent.setData(Uri.parse(url));
			startActivity(handleIntent);
			finish();
			return;
		}

		//Contact link
		if (matchRegexs(url, CONTACT_LINK_REGEXS)) { //https://mega.nz/C!
			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					String[] s = url.split("C!");
					long handle = MegaApiAndroid.base64ToHandle(s[1].trim());
					Intent inviteContact = new Intent(this, ManagerActivityLollipop.class);
					inviteContact.setAction(ACTION_OPEN_CONTACTS_SECTION);
					inviteContact.putExtra(CONTACT_HANDLE, handle);
					startActivity(inviteContact);
					finish();
				} else {
					logWarning("Not logged");
					setError(getString(R.string.alert_not_logged_in));
				}
				return;
			}
		}

		//MEGA DROP link
		if (matchRegexs(url, MEGA_DROP_LINK_REGEXS)) { //https://mega.nz/megadrop
			setError(getString(R.string.error_MEGAdrop_not_supported));
			return;
		}

		// Browser open the link which does not require app to handle
		logDebug("Browser open link: " + url);
		Intent openIntent = new Intent(this, WebViewActivityLollipop.class);
		openIntent.setData(Uri.parse(url));
		startActivity(openIntent);
		finish();
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestStart");
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		logDebug("onRequestFinish");
		if(request.getType() == MegaRequest.TYPE_QUERY_RECOVERY_LINK){
			logDebug("TYPE_GET_RECOVERY_LINK");

			if (e.getErrorCode() == MegaError.API_OK){
				String url = request.getLink();
				if (matchRegexs(url, CANCEL_ACCOUNT_LINK_REGEXS)) {
					logDebug("Cancel account url");

					String myEmail = request.getEmail();
					if(myEmail!=null){
						if(myEmail.equals(megaApi.getMyEmail())){
							logDebug("The email matchs!!!");
							Intent cancelAccountIntent = new Intent(this, ManagerActivityLollipop.class);
							cancelAccountIntent.setAction(ACTION_CANCEL_ACCOUNT);
							cancelAccountIntent.setData(Uri.parse(url));
							startActivity(cancelAccountIntent);
							finish();
						}
						else{
							logWarning("Not logged with the correct account");
							logWarning(e.getErrorString() + "___" + e.getErrorCode());
							setError(getString(R.string.error_not_logged_with_correct_account));
						}
					}
					else{
						logWarning("My email is NULL in the request");
					}

				}
				else if (matchRegexs(url, RESET_PASSWORD_LINK_REGEXS)) {
					logDebug("Reset pass url");
					logDebug("The recovery link has been sent");

					boolean mk = request.getFlag();
					if(mk){
						logDebug("Link with master key");
						if(url!=null){
							Intent resetPassIntent = new Intent(this, LoginActivityLollipop.class);
							resetPassIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
							resetPassIntent.setAction(ACTION_RESET_PASS);
							resetPassIntent.setData(Uri.parse(url));
							startActivity(resetPassIntent);
							finish();
						}
						else{
							logWarning("LINK is null");
							logWarning(e.getErrorString() + "___" + e.getErrorCode());
							setError(getString(R.string.general_text_error));
						}
					}
					else{
						logDebug("Link without master key - park account");
						Intent resetPassIntent = new Intent(this, LoginActivityLollipop.class);
						resetPassIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
						resetPassIntent.setAction(ACTION_PARK_ACCOUNT);
						resetPassIntent.setData(Uri.parse(url));
						startActivity(resetPassIntent);
						finish();
					}
				}
			}
			else if(e.getErrorCode() == MegaError.API_EEXPIRED){
				logWarning("Error expired link");
				logWarning(e.getErrorString() + "___" + e.getErrorCode());
				String url = request.getLink();
				if (matchRegexs(url, CANCEL_ACCOUNT_LINK_REGEXS)) {
					logDebug("Cancel account url");
					setError(getString(R.string.cancel_link_expired));
				}
				else if (matchRegexs(url, RESET_PASSWORD_LINK_REGEXS)) {
					logDebug("Reset pass url");
					setError(getString(R.string.recovery_link_expired));
				}
			}
			else{
				logError("Error when asking for recovery pass link");
				logError(e.getErrorString() + "___" + e.getErrorCode());
				setError(getString(R.string.invalid_link));
			}
		}
		else if(request.getType() == MegaRequest.TYPE_LOGOUT){
			if(isChatEnabled()){
				logDebug("END logout sdk request - wait chat logout");

				if(MegaApplication.getUrlConfirmationLink()!=null){
					logDebug("Confirmation link - show confirmation screen");
					if (dbH == null){
						dbH = DatabaseHandler.getDbHandler(getApplicationContext());
					}
					if (dbH != null){
						dbH.clearEphemeral();
					}

					AccountController aC = new AccountController(this);
					aC.logoutConfirmed(this);

					Intent confirmIntent = new Intent(this, LoginActivityLollipop.class);
					confirmIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
					confirmIntent.putExtra(EXTRA_CONFIRMATION, urlConfirmationLink);
					confirmIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					confirmIntent.setAction(ACTION_CONFIRM);
					startActivity(confirmIntent);
					MegaApplication.setUrlConfirmationLink(null);
					finish();
				}
			}
			else{
				logDebug("END logout sdk request - chat disabled");
				if (dbH == null){
					dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				}
				if (dbH != null){
					dbH.clearEphemeral();
				}

				AccountController aC = new AccountController(this);
				aC.logoutConfirmed(this);

				Intent confirmIntent = new Intent(this, LoginActivityLollipop.class);
				confirmIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
				confirmIntent.putExtra(EXTRA_CONFIRMATION, urlConfirmationLink);
				confirmIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				confirmIntent.setAction(ACTION_CONFIRM);
				startActivity(confirmIntent);
				MegaApplication.setUrlConfirmationLink(null);
				finish();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_QUERY_SIGNUP_LINK){
			logDebug("MegaRequest.TYPE_QUERY_SIGNUP_LINK");

			if(e.getErrorCode() == MegaError.API_OK){
				AccountController aC = new AccountController(this);
				MegaApplication.setUrlConfirmationLink(request.getLink());

				aC.logout(this, megaApi);
			}
			else{
				setError(getString(R.string.invalid_link));
			}
		}
	}

	public void setError (String string) {
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
}
