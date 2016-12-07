package mega.privacy.android.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.WebViewActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;


public class OpenLinkActivity extends PinActivity implements MegaRequestListenerInterface {

	MegaApplication app;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	DatabaseHandler dbH = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		app = (MegaApplication) getApplication();
		megaApi = app.getMegaApi();
		megaChatApi = app.getMegaChatApi();
		
		Intent intent = getIntent();
		String url = intent.getDataString();
		
		try {
			url = URLDecoder.decode(url, "UTF-8");
		} 
		catch (UnsupportedEncodingException e) {}
		url.replace(' ', '+');
		if(url.startsWith("mega://")){
			url = url.replace("mega://", "https://mega.co.nz/");
		}

		if (url.startsWith("https://www.mega.co.nz")){
			url = url.replace("https://www.mega.co.nz", "https://mega.co.nz");
		}

		if (url.startsWith("https://www.mega.nz")){
			url = url.replace("https://www.mega.nz", "https://mega.nz");
		}
		
		log("url " + url);
		
		// Download link
		if (url != null && (url.matches("^https://mega.co.nz/#!.+$") || url.matches("^https://mega.nz/#!.+$"))) {
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
		if (url != null && (url.matches("^https://mega.co.nz/#confirm.+$") || url.matches("^https://mega.nz/#confirm.+$"))) {
			log("confirmation url");
//			megaApi.localLogout();
			AccountController aC = new AccountController(this);
			aC.logout(this, megaApi, megaChatApi, true);

			Intent confirmIntent = new Intent(this, LoginActivityLollipop.class);
			confirmIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
			confirmIntent.putExtra(Constants.EXTRA_CONFIRMATION, url);
			confirmIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			confirmIntent.setAction(Constants.ACTION_CONFIRM);
			startActivity(confirmIntent);
			finish();

			return;
		}

		// Folder Download link
		if (url != null && (url.matches("^https://mega.co.nz/#F!.+$") || url.matches("^https://mega.nz/#F!.+$"))) {
			log("folder link url");

			Intent openFolderIntent = new Intent(this, FolderLinkActivityLollipop.class);
			openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openFolderIntent.setAction(Constants.ACTION_OPEN_MEGA_FOLDER_LINK);
			openFolderIntent.setData(Uri.parse(url));
			startActivity(openFolderIntent);
			finish();

			return;
		}

		// Create account invitation - user must be logged OUT
		if (url != null && (url.matches("^https://mega.co.nz/#newsignup.+$"))||(url.matches("^https://mega.nz/#newsignup.+$"))) {
			log("new signup url");

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			}
			if (dbH != null) {
				if (dbH.getCredentials() != null) {
					log("Logged IN");
					AlertDialog.Builder builder;
					builder = new AlertDialog.Builder(this);
					builder.setMessage(R.string.log_out_warning);
					builder.setPositiveButton(getString(R.string.cam_sync_ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									finish();
								}
							});
					builder.show();
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
		if (url != null && (url.matches("^https://mega.co.nz/#backup")||url.matches("^https://mega.nz/#backup"))) {
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
					AlertDialog.Builder builder;
					builder = new AlertDialog.Builder(this);
					builder.setMessage(R.string.alert_not_logged_in);
					builder.setPositiveButton(getString(R.string.cam_sync_ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									finish();
								}
							});
					builder.show();
				}
			}
			return;
		}

		// Cancel account  - user must be logged IN
		if (url != null && (url.matches("^https://mega.co.nz/#cancel.+$"))||(url.matches("^https://mega.nz/#cancel.+$"))) {
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
					AlertDialog.Builder builder;
					builder = new AlertDialog.Builder(this);
					builder.setMessage(R.string.alert_not_logged_in);
					builder.setPositiveButton(getString(R.string.cam_sync_ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									finish();
								}
							});
					builder.show();
				}
			}
			return;
		}

		// Verify change mail - user must be logged IN
		if (url != null && (url.matches("^https://mega.co.nz/#verify.+$"))||(url.matches("^https://mega.nz/#verify.+$"))) {
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
					AlertDialog.Builder builder;
					builder = new AlertDialog.Builder(this);
					builder.setMessage(R.string.alert_not_logged_in);
					builder.setPositiveButton(getString(R.string.cam_sync_ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									finish();
								}
							});
					builder.show();
				}
			}
			return;
		}

		// Reset password - two options: logged IN or OUT
		if (url != null && (url.matches("^https://mega.co.nz/#recover.+$"))||(url.matches("^https://mega.nz/#recover.+$"))) {
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
		if (url != null && (url.matches("^https://mega.co.nz/#fm/ipc"))||(url.matches("^https://mega.nz/#fm/ipc"))) {
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
					AlertDialog.Builder builder;
					builder = new AlertDialog.Builder(this);
					builder.setMessage(R.string.alert_not_logged_in);
					builder.setPositiveButton(getString(R.string.cam_sync_ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									finish();
								}
							});
					builder.show();
				}
			}
			return;
		}

		if (url != null && (url.matches("^https://mega.co.nz/#blog.+$") || url.matches("^https://mega.nz/#blog.+$"))) {
			log("blog link url");

			Intent openBlogIntent = new Intent(this, WebViewActivityLollipop.class);
			openBlogIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openBlogIntent.setData(Uri.parse(url));
			startActivity(openBlogIntent);
			finish();
			return;
		}
		
		log("wrong url");

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
				if (url != null && (url.matches("^https://mega.co.nz/#cancel.+$"))||(url.matches("^https://mega.nz/#cancel.+$"))) {
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
							Util.showAlert(this, getString(R.string.error_not_logged_with_correct_account), getString(R.string.general_error_word));
						}
					}
					else{
						log("My email is NULL in the request");
					}

				}
				else if (url != null && (url.matches("^https://mega.co.nz/#recover.+$"))||(url.matches("^https://mega.nz/#recover.+$"))) {
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
							Util.showAlert(this, getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
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
				if (url != null && (url.matches("^https://mega.co.nz/#cancel.+$"))||(url.matches("^https://mega.nz/#cancel.+$"))) {
					log("cancel account url");
					Util.showAlert(this, getString(R.string.cancel_link_expired), getString(R.string.general_error_word));
				}
				else if (url != null && (url.matches("^https://mega.co.nz/#recover.+$"))||(url.matches("^https://mega.nz/#recover.+$"))) {
					log("reset pass url");
					Util.showAlert(this, getString(R.string.recovery_link_expired), getString(R.string.general_error_word));
				}
			}
			else{
				log("Error when asking for recovery pass link");
				log(e.getErrorString() + "___" + e.getErrorCode());
				Util.showAlert(this, getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

	}
}
