package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaAccountDetails;
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

public class GetLinkActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface {

	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;

	CoordinatorLayout fragmentContainer;

	private androidx.appcompat.app.AlertDialog passwordDialog;

	//Fragments
	GetLinkFragmentLollipop getLinkFragment;
	CopyrightFragmentLollipop copyrightFragment;

	ActionBar aB;
	Toolbar tB;

	public int visibleFragment= COPYRIGHT_FRAGMENT;

	static GetLinkActivityLollipop getLinkActivity;

	Intent intentReceived = null;

	public long handle;
	public MegaNode selectedNode;
	public int accountType;

	DatabaseHandler dbH;

    Handler handler = new Handler();
	private MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		logDebug("onCreate");
		super.onCreate(savedInstanceState);

		getLinkActivity = this;

		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		density  = getResources().getDisplayMetrics().density;

		scaleW = getScaleW(outMetrics, density);
		scaleH = getScaleH(outMetrics, density);

	    dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		if (megaApi == null){
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
		}

		if(megaApi==null||megaApi.getRootNode()==null){
			logDebug("Refresh session - sdk");
			Intent intent = new Intent(this, LoginActivityLollipop.class);
			intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}

		if (megaChatApi == null) {
			megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
		}

		if (megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR) {
			logDebug("Refresh session - karere");
			Intent intent = new Intent(this, LoginActivityLollipop.class);
			intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}

		intentReceived = getIntent();
		if (intentReceived != null){
			handle = intentReceived.getLongExtra("handle", -1);
			if(handle!=-1){
				selectedNode = megaApi.getNodeByHandle(handle);
			}
			else{
				finish();
			}
		}
		else{
			finish();
		}

		MyAccountInfo accountInfo = ((MegaApplication) getApplication()).getMyAccountInfo();
		if(accountInfo!=null){
			accountType = accountInfo.getAccountType();
			if(accountType==-1){
				accountType = MegaAccountDetails.ACCOUNT_TYPE_FREE;
			}
		}
		else{
			accountType = MegaAccountDetails.ACCOUNT_TYPE_FREE;
		}

		setContentView(R.layout.get_link_activity_layout);

		fragmentContainer = (CoordinatorLayout) findViewById(R.id.get_link_coordinator_layout);
		tB = (Toolbar) findViewById(R.id.toolbar_get_link);
		if(tB==null){
			logWarning("Tb is Null");
		}

		tB.setVisibility(View.GONE);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
		aB.setHomeButtonEnabled(true);
		aB.setDisplayHomeAsUpEnabled(true);

		if(selectedNode.isExported()){

			visibleFragment = GET_LINK_FRAGMENT;
		}
		else{

			ArrayList<MegaNode> nodeLinks = megaApi.getPublicLinks();
			if(nodeLinks==null){
				boolean showCopyright = Boolean.parseBoolean(dbH.getShowCopyright());
				logDebug("No public links: showCopyright = " + showCopyright);
				if(showCopyright){
					visibleFragment = COPYRIGHT_FRAGMENT;
				}
				else{
					visibleFragment = GET_LINK_FRAGMENT;
				}
			}
			else{
				if(nodeLinks.size()==0){
					boolean showCopyright = Boolean.parseBoolean(dbH.getShowCopyright());
					logDebug("No public links: showCopyright = " + showCopyright);
					if(showCopyright){
						visibleFragment = COPYRIGHT_FRAGMENT;
					}
					else{
						visibleFragment = GET_LINK_FRAGMENT;
					}
				}
				else{
					visibleFragment = GET_LINK_FRAGMENT;
				}
			}
		}
		showFragment(visibleFragment);

		if (isBusinessExpired()) {
			setFinishActivityAtError(true);
			sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED));
		}
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return true;
    }

	public void sendLink(String link){
		logDebug("Link: " + link);
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, link);
		startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)));
	}

	public void copyLink(String link){
		logDebug("Link: " + link);
		if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(link);
		} else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", link);
			clipboard.setPrimaryClip(clip);
		}
		showSnackbar(getString(R.string.file_properties_get_link));
	}

	public void showSnackbar(String message){
		showSnackbar(fragmentContainer, message);
	}


	public void showSetPasswordDialog(final String password, final String link){
        logDebug("showSetPasswordDialog");
		SetPasswordDialog.SetPasswordCallback callback = new SetPasswordDialog.SetPasswordCallback() {
			@Override
			public void onConfirmed(String password) {
				if(getLinkFragment != null){
					getLinkFragment.processingPass();
				}
				megaApi.encryptLinkWithPassword(link, password, GetLinkActivityLollipop.this);
			}

			@Override
			public void onCanceled() {
				if (getLinkFragment != null) {
					getLinkFragment.enablePassProtection(false);
				}
			}
		};

		SetPasswordDialog dialog = new SetPasswordDialog(this, callback, megaApi);
		dialog.show();
	}

	public void showFragment(int visibleFragment){
		logDebug("visibleFragment: " + visibleFragment);
		this.visibleFragment = visibleFragment;
		switch (visibleFragment){
			case GET_LINK_FRAGMENT:{
				logDebug("Show GET_LINK_FRAGMENT");

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					Window window = this.getWindow();
					window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
					window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
					window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
				}

				if(aB!=null){
					if(selectedNode.isExported()){

						aB.setTitle(R.string.edit_link_option);
					}
					else{

						aB.setTitle(R.string.context_get_link_menu);
					}
					aB.show();
				}

				if(getLinkFragment==null){
					getLinkFragment = new GetLinkFragmentLollipop();
				}

				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container_get_link, getLinkFragment);
				ft.commitNowAllowingStateLoss();

				break;
			}
			case COPYRIGHT_FRAGMENT:{
				logDebug("Show COPYRIGHT_FRAGMENT");

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					Window window = this.getWindow();
					window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
					window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
					window.setStatusBarColor(ContextCompat.getColor(this, R.color.transparent_black));
				}

				if(aB!=null){
					aB.hide();
				}

				if(copyrightFragment==null){
					copyrightFragment = new CopyrightFragmentLollipop();
				}

				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container_get_link, copyrightFragment);
				ft.commitNowAllowingStateLoss();
				break;
			}
		}
	}

	@Override
	public void onResume() {
		logDebug("onResume");
		super.onResume();
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		logDebug("onRequestFinish");

		if (e.getErrorCode() == MegaError.API_OK) {
			logDebug("link: " + request.getLink());

			//for megaApi.encryptLinkWithPassword() case, request.getNodeHandle() returns -1 and cause selectedNode set to null
			long handle = request.getNodeHandle();
			if(handle == -1){
			    handle = selectedNode.getHandle();
            }
            
            //refresh node
			selectedNode = megaApi.getNodeByHandle(handle);
			if(getLinkFragment!=null){
				if(getLinkFragment.isAdded()){
					getLinkFragment.requestFinish(request, e);
				}
			}
		} else if (e.getErrorCode() != MegaError.API_EBUSINESSPASTDUE){
			logWarning("Error: " + e.getErrorString());
			showSnackbar(getString(R.string.context_no_link));
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

	}
}
