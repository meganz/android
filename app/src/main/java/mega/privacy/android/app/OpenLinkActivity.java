package mega.privacy.android.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import mega.privacy.android.app.lollipop.CreateAccountActivityLollipop;
import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;


public class OpenLinkActivity extends PinActivity {

	MegaApplication app;
	MegaApiAndroid megaApi;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		app = (MegaApplication) getApplication();
		megaApi = app.getMegaApi();
		
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
		
		log("url " + url);
		
		// Download link
		if (url != null && (url.matches("^https://mega.co.nz/#!.*!.*$") || url.matches("^https://mega.nz/#!.*!.*$"))) {
			log("open link url");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				log("Build.VERSION_CODES.LOLLIPOP");
//				Intent openIntent = new Intent(this, ManagerActivity.class);
				Intent openFileIntent = new Intent(this, FileLinkActivityLollipop.class);
				openFileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				openFileIntent.setAction(Constants.ACTION_OPEN_MEGA_LINK);
				openFileIntent.setData(Uri.parse(url));
				startActivity(openFileIntent);
				finish();
			}
			else{
//				Intent openIntent = new Intent(this, ManagerActivity.class);
				Intent openFileIntent = new Intent(this, FileLinkActivity.class);
				openFileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				openFileIntent.setAction(ManagerActivity.ACTION_OPEN_MEGA_LINK);
				openFileIntent.setData(Uri.parse(url));
				startActivity(openFileIntent);
				finish();
			}
			return;
		}
		
		// Confirmation link
		if (url != null && (url.matches("^https://mega.co.nz/#confirm.+$") || url.matches("^https://mega.nz/#confirm.+$"))) {
			log("confirmation url");
			ManagerActivity.logout(this, megaApi, true);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {	
				log("Build.VERSION_CODES.LOLLIPOP");
				Intent confirmIntent = new Intent(this, LoginActivityLollipop.class);
				confirmIntent.putExtra(LoginActivity.EXTRA_CONFIRMATION, url);
				confirmIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				confirmIntent.setAction(LoginActivityLollipop.ACTION_CONFIRM);
				startActivity(confirmIntent);
				finish();
			}
			else{
				Intent confirmIntent = new Intent(this, LoginActivity.class);
				confirmIntent.putExtra(LoginActivity.EXTRA_CONFIRMATION, url);
				confirmIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				confirmIntent.setAction(LoginActivity.ACTION_CONFIRM);
				startActivity(confirmIntent);
				finish();
			}
			return;
		}		
		
		// Export Master Key link
		if (url != null && (url.matches("^https://mega.co.nz/#backup")||url.matches("^https://mega.nz/#backup"))) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {			
				log("export master key url");			
				Intent exportIntent = new Intent(this, ManagerActivityLollipop.class);
				exportIntent.setAction(Constants.ACTION_EXPORT_MASTER_KEY);
				startActivity(exportIntent);
				finish();
				return;
			}			
		}
		
		// Folder Download link
		if (url != null && (url.matches("^https://mega.co.nz/#F!.+$") || url.matches("^https://mega.nz/#F!.+$"))) {
			log("folder link url");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {	
				log("Build.VERSION_CODES.LOLLIPOP");
				Intent openFolderIntent = new Intent(this, FolderLinkActivityLollipop.class);
				openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				openFolderIntent.setAction(Constants.ACTION_OPEN_MEGA_FOLDER_LINK);
				openFolderIntent.setData(Uri.parse(url));
				startActivity(openFolderIntent);
				finish();
			}
			else{
				Intent openFolderIntent = new Intent(this, FolderLinkActivity.class);
				openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				openFolderIntent.setAction(ManagerActivity.ACTION_OPEN_MEGA_FOLDER_LINK);
				openFolderIntent.setData(Uri.parse(url));
				startActivity(openFolderIntent);
				finish();
			}
			return;
		}

		// Create account invitation
		if (url != null && (url.matches("^https://mega.co.nz/#newsignup.+$"))||(url.matches("^https://mega.nz/#newsignup.+$"))) {
			log("new signup url");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				MegaNode rootNode = megaApi.getRootNode();
				if (rootNode == null){
					log("Not logged");
					Intent createAccountIntent = new Intent(this, CreateAccountActivityLollipop.class);
//				createAccountIntent.setAction(ManagerActivityLollipop.ACTION_EXPORT_MASTER_KEY);
					startActivity(createAccountIntent);
					finish();
				}
				else{
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
				return;
			}
		}

		// Cancel account
		if (url != null && (url.matches("^https://mega.co.nz/#cancel.+$"))||(url.matches("^https://mega.nz/#cancel.+$"))) {
			log("cancel account url");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				MegaNode rootNode = megaApi.getRootNode();
				if (rootNode == null){
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
				else{
					log("Build.VERSION_CODES.LOLLIPOP");
					//Check that the link corresponds to this account
					Intent cancelAccountIntent = new Intent(this, ManagerActivityLollipop.class);
					cancelAccountIntent.setAction(Constants.ACTION_CANCEL_ACCOUNT);
					cancelAccountIntent.setData(Uri.parse(url));
					startActivity(cancelAccountIntent);
					finish();
				}
				return;
			}
		}

		// Reset password
		if (url != null && (url.matches("^https://mega.co.nz/#recover.+$"))||(url.matches("^https://mega.nz/#recover.+$"))) {
			log("reset pass url");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				log("Build.VERSION_CODES.LOLLIPOP");
				MegaNode rootNode = megaApi.getRootNode();
				if (rootNode == null){
					log("Not logged");
					//Check that the link corresponds to this account
					Intent resetPassIntent = new Intent(this, LoginActivityLollipop.class);
					resetPassIntent.setAction(Constants.ACTION_RESET_PASS);
					resetPassIntent.setData(Uri.parse(url));
					startActivity(resetPassIntent);
					finish();
				}
				else{
					log("Logged IN");
					AlertDialog.Builder builder;
					builder = new AlertDialog.Builder(this);
					builder.setMessage(R.string.alert_not_logged_out);
					builder.setPositiveButton(getString(R.string.cam_sync_ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									finish();
								}
							});
					builder.show();
				}
				return;
			}
		}
		
		log("wrong url");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			log("Build.VERSION_CODES.LOLLIPOP");
			Intent errorIntent = new Intent(this, ManagerActivityLollipop.class);
			errorIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(errorIntent);
			finish();
		}
		else{
			Intent errorIntent = new Intent(this, ManagerActivity.class);
			errorIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(errorIntent);
			finish();
		}
	}
	
	public static void log(String message) {
		Util.log("OpenLinkActivity", message);
	}
}
