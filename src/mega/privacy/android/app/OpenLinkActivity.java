package mega.privacy.android.app;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;


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
				openFileIntent.setAction(ManagerActivity.ACTION_OPEN_MEGA_LINK);
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
		if (url != null && url.matches("^https://mega.co.nz/#backup/")) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {			
				log("export master key url");			
				Intent exportIntent = new Intent(this, ManagerActivityLollipop.class);
				exportIntent.setAction(ManagerActivityLollipop.ACTION_EXPORT_MASTER_KEY);
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
				openFolderIntent.setAction(ManagerActivity.ACTION_OPEN_MEGA_FOLDER_LINK);
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
