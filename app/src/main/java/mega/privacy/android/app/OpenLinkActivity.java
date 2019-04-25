package mega.privacy.android.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.WebViewActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;


public class OpenLinkActivity extends PinActivityLollipop implements MegaRequestListenerInterface, View.OnClickListener {

	MegaApplication app;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	DatabaseHandler dbH = null;

	String urlConfirmationLink = null;

	static OpenLinkActivity openLinkActivity = null;

	RelativeLayout relativeContainer;
	TextView processingText;
	TextView errorText;
	ProgressBar progressBar;
	RelativeLayout containerOkButton;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		app = (MegaApplication) getApplication();
		megaApi = app.getMegaApi();
		megaChatApi = app.getMegaChatApi();
		
		Intent intent = getIntent();
		String url = intent.getDataString();
		log("Original url: " + url);

		openLinkActivity = this;

		setContentView(R.layout.activity_open_link);

		relativeContainer = (RelativeLayout) findViewById(R.id.relative_container_open_link);
		processingText = (TextView) findViewById(R.id.open_link_text);
		errorText = (TextView) findViewById(R.id.open_link_error);
		errorText.setVisibility(View.GONE);
		progressBar = (ProgressBar) findViewById(R.id.open_link_bar);
		containerOkButton = (RelativeLayout) findViewById(R.id.container_accept_button);
		containerOkButton.setVisibility(View.GONE);
		containerOkButton.setOnClickListener(this);
		
		try {
			url = URLDecoder.decode(url, "UTF-8");
		} 
		catch (UnsupportedEncodingException e) {}
		url.replace(' ', '+');
		if(url.startsWith("mega://")){
			url = url.replace("mega://", "https://mega.nz/");
		}

		if (url.startsWith("https://www.mega.co.nz")){
			url = url.replace("https://www.mega.co.nz", "https://mega.co.nz");
		}

		if (url.startsWith("https://www.mega.nz")){
			url = url.replace("https://www.mega.nz", "https://mega.nz");
		}

		if (url.endsWith("/")) {
			url = url.substring(0, url.length()-1);
		}
		
		log("url " + url);
		
		// File link
		if (url != null && (url.matches("^https://mega\\.co\\.nz/#!.+$") || url.matches("^https://mega\\.nz/#!.+$"))) {
			log("open link url");

			Intent openFileIntent = new Intent(this, FileLinkActivityLollipop.class);
			openFileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openFileIntent.setAction(Constants.ACTION_OPEN_MEGA_LINK);
			openFileIntent.setData(Uri.parse(url));
			startActivity(openFileIntent);
			finish();
			return;
		}
		
		// Confirmation link
		if (url != null && (url.matches("^https://mega\\.co\\.nz/#confirm.+$") || url.matches("^https://mega\\.nz/#confirm.+$"))) {
			log("confirmation url");
//			megaApi.localLogout();
			urlConfirmationLink = url;

//			megaApi.querySignupLink(url, this);
			AccountController aC = new AccountController(this);
			MegaApplication.setUrlConfirmationLink(urlConfirmationLink);

			aC.logout(this, megaApi);

			return;
		}

		// Folder Download link
		if (url != null && (url.matches("^https://mega\\.co\\.nz/#F!.+$") || url.matches("^https://mega\\.nz/#F!.+$"))) {
			log("folder link url");

			Intent openFolderIntent = new Intent(this, FolderLinkActivityLollipop.class);
			openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openFolderIntent.setAction(Constants.ACTION_OPEN_MEGA_FOLDER_LINK);
			openFolderIntent.setData(Uri.parse(url));
			startActivity(openFolderIntent);
			finish();

			return;
		}

		// Chat link
		if (url != null && (url.matches("^https://mega\\.co\\.nz/chat/.+$") || url.matches("^https://mega\\.nz/chat/.+$"))) {
			log("open chat url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					log("Logged IN");
					Intent openChatLinkIntent = new Intent(this, ManagerActivityLollipop.class);
					openChatLinkIntent.setAction(Constants.ACTION_OPEN_CHAT_LINK);
					openChatLinkIntent.setData(Uri.parse(url));
					startActivity(openChatLinkIntent);
					finish();
				}
				else{
					log("Not logged");
					int initResult = megaChatApi.getInitState();
					if(initResult<MegaChatApi.INIT_WAITING_NEW_SESSION){
						initResult = megaChatApi.initAnonymous();
					}

					if(initResult!= MegaChatApi.INIT_ERROR){
						Intent openChatLinkIntent = new Intent(this, ChatActivityLollipop.class);
						openChatLinkIntent.setAction(Constants.ACTION_OPEN_CHAT_LINK);
						openChatLinkIntent.setData(Uri.parse(url));
						startActivity(openChatLinkIntent);
						finish();
					}
					else{
						log("open chat url:initAnonymous:INIT_ERROR");
						setError(getString(R.string.error_chat_link_init_error));
					}
				}
			}
			return;
		}

		// Password link
		if (url != null && (url.matches("^https://mega\\.co\\.nz/#P!.+$") || url.matches("^https://mega\\.nz/#P!.+$"))) {
			log("link with password url");

			Intent openLinkIntent = new Intent(this, OpenPasswordLinkActivity.class);
			openLinkIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openLinkIntent.setData(Uri.parse(url));
			startActivity(openLinkIntent);
			finish();

			return;
		}

		// Create account invitation - user must be logged OUT
		if (url != null && (url.matches("^https://mega\\.co\\.nz/#newsignup.+$"))||(url.matches("^https://mega\\.nz/#newsignup.+$"))) {
			log("new signup url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					log("Logged IN");
//					AlertDialog.Builder builder;
//					builder = new AlertDialog.Builder(this);
//					builder.setMessage(R.string.log_out_warning);
//					builder.setPositiveButton(getString(R.string.cam_sync_ok),
//							new DialogInterface.OnClickListener() {
//								public void onClick(DialogInterface dialog, int whichButton) {
//									finish();
//								}
//							});
//					builder.show();
					setError(getString(R.string.log_out_warning));
				}
				else{
					log("Not logged");
					Intent createAccountIntent = new Intent(this, LoginActivityLollipop.class);
					createAccountIntent.putExtra("visibleFragment", Constants.CREATE_ACCOUNT_FRAGMENT);
					startActivity(createAccountIntent);
					finish();
				}
			}
			return;
		}

		// Export Master Key link - user must be logged IN
		if (url != null && (url.matches("^https://mega\\.co\\.nz/#backup")||url.matches("^https://mega\\.nz/#backup"))) {
			log("export master key url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					log("Logged IN"); //Check fetch nodes is already done in ManagerActivity
					Intent exportIntent = new Intent(this, ManagerActivityLollipop.class);
					exportIntent.setAction(Constants.ACTION_EXPORT_MASTER_KEY);
					startActivity(exportIntent);
					finish();
				} else {
					log("Not logged");
//					AlertDialog.Builder builder;
//					builder = new AlertDialog.Builder(this);
//					builder.setMessage(R.string.alert_not_logged_in);
//					builder.setPositiveButton(getString(R.string.cam_sync_ok),
//							new DialogInterface.OnClickListener() {
//								public void onClick(DialogInterface dialog, int whichButton) {
//									finish();
//								}
//							});
//					builder.show();
					setError(getString(R.string.alert_not_logged_in));
				}
			}
			return;
		}

		// New mwssage chat- user must be logged IN
		if (url != null && (url.matches("^https://mega\\.co\\.nz/#fm/chat")||url.matches("^https://mega\\.nz/#fm/chat"))) {
			log("new message chat url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					log("Logged IN"); //Check fetch nodes is already done in ManagerActivity
					Intent chatIntent = new Intent(this, ManagerActivityLollipop.class);
					chatIntent.setAction(Constants.ACTION_CHAT_SUMMARY);
					startActivity(chatIntent);
					finish();
				} else {
					log("Not logged");
//					AlertDialog.Builder builder;
//					builder = new AlertDialog.Builder(this);
//					builder.setMessage(R.string.alert_not_logged_in);
//					builder.setPositiveButton(getString(R.string.cam_sync_ok),	new DialogInterface.OnClickListener() {
//								public void onClick(DialogInterface dialog, int whichButton) {
//									Intent intent = new Intent(openLinkActivity, LoginActivityLollipop.class);
//									intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
//									intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//									startActivity(intent);
//									finish();
//								}
//							});
//					builder.show();
					setError(getString(R.string.alert_not_logged_in));
				}
			}
			return;
		}

		// Cancel account  - user must be logged IN
		if (url != null && (url.matches("^https://mega\\.co\\.nz/#cancel.+$"))||(url.matches("^https://mega\\.nz/#cancel.+$"))) {
			log("cancel account url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					MegaNode rootNode = megaApi.getRootNode();
					if (rootNode == null) {
						log("Go to Login to fetch nodes");
						Intent cancelAccountIntent = new Intent(this, LoginActivityLollipop.class);
						cancelAccountIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
						cancelAccountIntent.setAction(Constants.ACTION_CANCEL_ACCOUNT);
						cancelAccountIntent.setData(Uri.parse(url));
						startActivity(cancelAccountIntent);
						finish();

					} else {
						log("Logged IN");
						megaApi.queryCancelLink(url, this);
					}
				} else {
					log("Not logged");
//					AlertDialog.Builder builder;
//					builder = new AlertDialog.Builder(this);
//					builder.setMessage(R.string.alert_not_logged_in);
//					builder.setPositiveButton(getString(R.string.cam_sync_ok),
//							new DialogInterface.OnClickListener() {
//								public void onClick(DialogInterface dialog, int whichButton) {
//									finish();
//								}
//							});
//					builder.show();
					setError(getString(R.string.alert_not_logged_in));
				}
			}
			return;
		}

		// Verify change mail - user must be logged IN
		if (url != null && (url.matches("^https://mega\\.co\\.nz/#verify.+$"))||(url.matches("^https://mega\\.nz/#verify.+$"))) {
			log("verify mail url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					MegaNode rootNode = megaApi.getRootNode();
					if (rootNode == null) {
						log("Go to Login to fetch nodes");
						Intent changeMailIntent = new Intent(this, LoginActivityLollipop.class);
						changeMailIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
						changeMailIntent.setAction(Constants.ACTION_CHANGE_MAIL);
						changeMailIntent.setData(Uri.parse(url));
						startActivity(changeMailIntent);
						finish();

					} else {
						log("Logged IN");
						Intent changeMailIntent = new Intent(this, ManagerActivityLollipop.class);
						changeMailIntent.setAction(Constants.ACTION_CHANGE_MAIL);
						changeMailIntent.setData(Uri.parse(url));
						startActivity(changeMailIntent);
						finish();
					}
				} else {
					log("Not logged");
//					AlertDialog.Builder builder;
//					builder = new AlertDialog.Builder(this);
//					builder.setMessage(R.string.alert_not_logged_in);
//					builder.setPositiveButton(getString(R.string.cam_sync_ok),
//							new DialogInterface.OnClickListener() {
//								public void onClick(DialogInterface dialog, int whichButton) {
//									finish();
//								}
//							});
//					builder.show();
					setError(getString(R.string.alert_not_logged_in));
				}
			}
			return;
		}

		// Reset password - two options: logged IN or OUT
		if (url != null && (url.matches("^https://mega\\.co\\.nz/#recover.+$"))||(url.matches("^https://mega\\.nz/#recover.+$"))) {
			log("reset pass url");
			//Check if link with MK or not
			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					MegaNode rootNode = megaApi.getRootNode();
					if (rootNode == null) {
						log("Go to Login to fetch nodes");
						Intent resetPassIntent = new Intent(this, LoginActivityLollipop.class);
						resetPassIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
						resetPassIntent.setAction(Constants.ACTION_RESET_PASS);
						resetPassIntent.setData(Uri.parse(url));
						startActivity(resetPassIntent);
						finish();

					} else {
						log("Logged IN");
						Intent resetPassIntent = new Intent(this, ManagerActivityLollipop.class);
						resetPassIntent.setAction(Constants.ACTION_RESET_PASS);
						resetPassIntent.setData(Uri.parse(url));
						startActivity(resetPassIntent);
						finish();
					}
				} else {
					log("Not logged");
					megaApi.queryResetPasswordLink(url, this);
				}
			}
			return;
		}

		// Pending contacts
		if (url != null && (url.matches("^https://mega\\.co\\.nz/#fm/ipc"))||(url.matches("^https://mega\\.nz/#fm/ipc"))) {
			log("pending contacts url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					MegaNode rootNode = megaApi.getRootNode();
					if (rootNode == null) {
						log("Go to Login to fetch nodes");
						Intent ipcIntent = new Intent(this, LoginActivityLollipop.class);
						ipcIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
						ipcIntent.setAction(Constants.ACTION_IPC);
						startActivity(ipcIntent);
						finish();

					} else {
						log("Logged IN");
						Intent ipcIntent = new Intent(this, ManagerActivityLollipop.class);
						ipcIntent.setAction(Constants.ACTION_IPC);
						startActivity(ipcIntent);
						finish();
					}
				} else {
					log("Not logged");
//					AlertDialog.Builder builder;
//					builder = new AlertDialog.Builder(this);
//					builder.setMessage(R.string.alert_not_logged_in);
//					builder.setPositiveButton(getString(R.string.cam_sync_ok),
//							new DialogInterface.OnClickListener() {
//								public void onClick(DialogInterface dialog, int whichButton) {
//									finish();
//								}
//							});
//					builder.show();
					setError(getString(R.string.alert_not_logged_in));
				}
			}
			return;
		}

		if (url != null && (url.matches("^https://mega\\.co\\.nz/#blog") || url.matches("^https://mega\\.nz/#blog") || url.matches("^https://mega\\.nz/blog") || url.matches("^https://mega\\.co\\.nz/#blog.+$") || url.matches("^https://mega\\.nz/#blog.+$") || url.matches("^https://mega\\.nz/blog.+$") ) ) {
			log("blog link url");

			Intent openBlogIntent = new Intent(this, WebViewActivityLollipop.class);
			openBlogIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openBlogIntent.setData(Uri.parse(url));
			startActivity(openBlogIntent);
			finish();
			return;
		}

		if (url != null && (url.matches("^https://mega\\.co\\.nz/#help") || url.matches("^https://mega\\.nz/#help") || url.matches("^https://mega\\.nz/help"))) {
			log("help link url");

			Intent openHelpIntent = new Intent(this, WebViewActivityLollipop.class);
			openHelpIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openHelpIntent.setData(Uri.parse(url));
			startActivity(openHelpIntent);
			finish();
			return;
		}

		if (url != null && (url.matches("^https://mega\\.co\\.nz/#sync") || url.matches("^https://mega\\.nz/#sync") || url.matches("^https://mega\\.nz/sync"))) {
			log("sync link url");

			Intent openSyncIntent = new Intent(this, WebViewActivityLollipop.class);
			openSyncIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openSyncIntent.setData(Uri.parse(url));
			startActivity(openSyncIntent);
			finish();
			return;
		}

		if (url != null && (url.matches("^https://mega\\.co\\.nz/#terms") || url.matches("^https://mega\\.nz/#terms") || url.matches("^https://mega\\.nz/terms"))) {
			log("terms link url");

			Intent openTermsIntent = new Intent(this, WebViewActivityLollipop.class);
			openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openTermsIntent.setData(Uri.parse(url));
			startActivity(openTermsIntent);
			finish();
			return;
		}

		if (url != null && (url.matches("^https://mega\\.co\\.nz/#privacy") || url.matches("^https://mega\\.nz/#privacy") || url.matches("^https://mega\\.nz/privacy"))) {
			log("privacy link url");

			Intent openPrivacyIntent = new Intent(this, WebViewActivityLollipop.class);
			openPrivacyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openPrivacyIntent.setData(Uri.parse(url));
			startActivity(openPrivacyIntent);
			finish();
			return;
		}

		if (url != null && (url.matches("^https://mega\\.co\\.nz/#gdpr") || url.matches("^https://mega\\.nz/#gdpr") || url.matches("^https://mega\\.nz/gdpr"))) {
			log("gdpr link url");

			Intent openGdprIntent = new Intent(this, WebViewActivityLollipop.class);
			openGdprIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openGdprIntent.setData(Uri.parse(url));
			startActivity(openGdprIntent);
			finish();
			return;
		}

		if (url != null && (url.matches("^https://mega\\.co\\.nz/#.+$") || url.matches("^https://mega\\.nz/#.+$"))) {
			log("handle link url");

			Intent handleIntent = new Intent(this, ManagerActivityLollipop.class);
			handleIntent.setAction(Constants.ACTION_OPEN_HANDLE_NODE);
			handleIntent.setData(Uri.parse(url));
			startActivity(handleIntent);
			finish();
			return;
		}

		//Contact link
		if (url != null && url.matches("^https://mega\\.nz/C!.+$")) { //https://mega.nz/C!
			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					String[] s = url.split("C!");
					long handle = MegaApiAndroid.base64ToHandle(s[1].trim());
					Intent inviteContact = new Intent(this, ManagerActivityLollipop.class);
					inviteContact.setAction(Constants.ACTION_OPEN_CONTACTS_SECTION);
					inviteContact.putExtra("handle", handle);
					startActivity(inviteContact);
					finish();
				} else {
					log("Not logged");
					setError(getString(R.string.alert_not_logged_in));
				}
				return;
			}
		}

		//MEGA DROP link
		if (url != null && (url.matches("^https://mega\\.co\\.nz/megadrop/.+$") || url.matches("^https://mega\\.nz/megadrop/.+$"))) { //https://mega.nz/megadrop

			setError(getString(R.string.error_MEGAdrop_not_supported));
			return;
		}

		log("wrong url: " + url);

		Intent errorIntent = new Intent(this, ManagerActivityLollipop.class);
		errorIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(errorIntent);
		finish();
	}
	
	public static void log(String message) {
		Util.log("OpenLinkActivity", message);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart");
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		log("onRequestFinish");
		if(request.getType() == MegaRequest.TYPE_QUERY_RECOVERY_LINK){
			log("TYPE_GET_RECOVERY_LINK");

			if (e.getErrorCode() == MegaError.API_OK){
				String url = request.getLink();
				if (url != null && (url.matches("^https://mega\\.co\\.nz/#cancel.+$"))||(url.matches("^https://mega\\.nz/#cancel.+$"))) {
					log("cancel account url");
					String myEmail = request.getEmail();
					if(myEmail!=null){
						if(myEmail.equals(megaApi.getMyEmail())){
							log("The email matchs!!!");
							Intent cancelAccountIntent = new Intent(this, ManagerActivityLollipop.class);
							cancelAccountIntent.setAction(Constants.ACTION_CANCEL_ACCOUNT);
							cancelAccountIntent.setData(Uri.parse(url));
							startActivity(cancelAccountIntent);
							finish();
						}
						else{
							log("Not logged with the correct account");
							log(e.getErrorString() + "___" + e.getErrorCode());
//							Util.showAlert(this, getString(R.string.error_not_logged_with_correct_account), getString(R.string.general_error_word));
							setError(getString(R.string.error_not_logged_with_correct_account));
						}
					}
					else{
						log("My email is NULL in the request");
					}

				}
				else if (url != null && (url.matches("^https://mega\\.co\\.nz/#recover.+$"))||(url.matches("^https://mega\\.nz/#recover.+$"))) {
					log("reset pass url");
					log("The recovery link has been sent");
					boolean mk = request.getFlag();
					if(mk){
						log("Link with master key");
						if(url!=null){
							Intent resetPassIntent = new Intent(this, LoginActivityLollipop.class);
							resetPassIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
							resetPassIntent.setAction(Constants.ACTION_RESET_PASS);
							resetPassIntent.setData(Uri.parse(url));
							startActivity(resetPassIntent);
							finish();
						}
						else{
							log("LINK is null");
							log(e.getErrorString() + "___" + e.getErrorCode());
//							Util.showAlert(this, getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
							setError(getString(R.string.general_text_error));
						}
					}
					else{
						log("Link without master key - park account");
						Intent resetPassIntent = new Intent(this, LoginActivityLollipop.class);
						resetPassIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
						resetPassIntent.setAction(Constants.ACTION_PARK_ACCOUNT);
						resetPassIntent.setData(Uri.parse(url));
						startActivity(resetPassIntent);
						finish();
					}
				}
			}
			else if(e.getErrorCode() == MegaError.API_EEXPIRED){
				log("Error expired link");
				log(e.getErrorString() + "___" + e.getErrorCode());
				String url = request.getLink();
				if (url != null && (url.matches("^https://mega\\.co\\.nz/#cancel.+$"))||(url.matches("^https://mega\\.nz/#cancel.+$"))) {
					log("cancel account url");
//					Util.showAlert(this, getString(R.string.cancel_link_expired), getString(R.string.general_error_word));
					setError(getString(R.string.cancel_link_expired));
				}
				else if (url != null && (url.matches("^https://mega\\.co\\.nz/#recover.+$"))||(url.matches("^https://mega\\.nz/#recover.+$"))) {
					log("reset pass url");
//					Util.showAlert(this, getString(R.string.recovery_link_expired), getString(R.string.general_error_word));
					setError(getString(R.string.recovery_link_expired));
				}
			}
			else{
				log("Error when asking for recovery pass link");
				log(e.getErrorString() + "___" + e.getErrorCode());
//				Util.showAlert(this, getString(R.string.invalid_link), getString(R.string.general_error_word));
				setError(getString(R.string.invalid_link));
			}
		}
		else if(request.getType() == MegaRequest.TYPE_LOGOUT){
			if(Util.isChatEnabled()){
				log("END logout sdk request - wait chat logout");

				if(MegaApplication.getUrlConfirmationLink()!=null){
					log("Confirmation link - show confirmation screen");
					if (dbH == null){
						dbH = DatabaseHandler.getDbHandler(getApplicationContext());
					}
					if (dbH != null){
						dbH.clearEphemeral();
					}

					AccountController aC = new AccountController(this);
					aC.logoutConfirmed(this);

					Intent confirmIntent = new Intent(this, LoginActivityLollipop.class);
					confirmIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
					confirmIntent.putExtra(Constants.EXTRA_CONFIRMATION, urlConfirmationLink);
					confirmIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					confirmIntent.setAction(Constants.ACTION_CONFIRM);
					startActivity(confirmIntent);
					MegaApplication.setUrlConfirmationLink(null);
					finish();
				}
			}
			else{
				log("END logout sdk request - chat disabled");
				if (dbH == null){
					dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				}
				if (dbH != null){
					dbH.clearEphemeral();
				}

				AccountController aC = new AccountController(this);
				aC.logoutConfirmed(this);

				Intent confirmIntent = new Intent(this, LoginActivityLollipop.class);
				confirmIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
				confirmIntent.putExtra(Constants.EXTRA_CONFIRMATION, urlConfirmationLink);
				confirmIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				confirmIntent.setAction(Constants.ACTION_CONFIRM);
				startActivity(confirmIntent);
				MegaApplication.setUrlConfirmationLink(null);
				finish();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_QUERY_SIGNUP_LINK){
			log("MegaRequest.TYPE_QUERY_SIGNUP_LINK");

			if(e.getErrorCode() == MegaError.API_OK){
				AccountController aC = new AccountController(this);
				MegaApplication.setUrlConfirmationLink(request.getLink());

				aC.logout(this, megaApi);
			}
			else{
				setError(getString(R.string.invalid_link));

//				AlertDialog.Builder builder;
//				builder = new AlertDialog.Builder(this);
//				builder.setMessage(R.string.invalid_link);
//				builder.setPositiveButton(getString(R.string.cam_sync_ok),
//						new DialogInterface.OnClickListener() {
//							public void onClick(DialogInterface dialog, int whichButton) {
//								finish();
//							}
//						});
//				builder.show();

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
