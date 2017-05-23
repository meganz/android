package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;


public class GetLinkActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface {

	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;

	CoordinatorLayout fragmentContainer;

	//Fragments
	GetLinkFragmentLollipop getLinkFragment;
	CopyrightFragmentLollipop copyrightFragment;

	ActionBar aB;
	Toolbar tB;

	public int visibleFragment= Constants.COPYRIGHT_FRAGMENT;

	static GetLinkActivityLollipop getLinkActivity;

	Intent intentReceived = null;

	public long handle;
	public MegaNode selectedNode;
	public int accountType;

	DatabaseHandler dbH;

    Handler handler = new Handler();
	private MegaApiAndroid megaApi;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		super.onCreate(savedInstanceState);

		getLinkActivity = this;

		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		density  = getResources().getDisplayMetrics().density;

		scaleW = Util.getScaleW(outMetrics, density);
		scaleH = Util.getScaleH(outMetrics, density);

	    dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		if (megaApi == null){
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
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
			accountType = intentReceived.getIntExtra("account", 0);
		}
		else{
			finish();
		}

		setContentView(R.layout.get_link_activity_layout);

		fragmentContainer = (CoordinatorLayout) findViewById(R.id.get_link_coordinator_layout);
		tB = (Toolbar) findViewById(R.id.toolbar_get_link);
		if(tB==null){
			log("Tb is Null");
		}

		tB.setVisibility(View.GONE);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
		aB.setHomeButtonEnabled(true);
		aB.setDisplayHomeAsUpEnabled(true);

		if(selectedNode.isExported()){

			visibleFragment = Constants.GET_LINK_FRAGMENT;
		}
		else{

			visibleFragment = Constants.COPYRIGHT_FRAGMENT;
		}
		showFragment(visibleFragment);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

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
		log("sendLink");
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, link);
		startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)));
	}

	public void copyLink(String link){
		log("copyLink");
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
		Snackbar snackbar = Snackbar.make(fragmentContainer,message,Snackbar.LENGTH_LONG);
		TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
		snackbarTextView.setMaxLines(5);
		snackbar.show();
	}

	public void showFragment(int visibleFragment){
		log("showFragment: "+visibleFragment);
		this.visibleFragment = visibleFragment;
		switch (visibleFragment){
			case Constants.GET_LINK_FRAGMENT:{
				log("show GET_LINK_FRAGMENT");

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
			case Constants.COPYRIGHT_FRAGMENT:{
				log("Show COPYRIGHT_FRAGMENT");

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
		log("onResume");
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
		log("onRequestFinish");

		if (e.getErrorCode() == MegaError.API_OK) {
			log("link: " + request.getLink());
			selectedNode = megaApi.getNodeByHandle(request.getNodeHandle());
			if(getLinkFragment!=null){
				if(getLinkFragment.isAdded()){
					if (request.getType() == MegaRequest.TYPE_EXPORT) {
						log("export request finished");
						getLinkFragment.requestFinish(request, e);
					}
				}
			}
		} else {
			log("Error: " + e.getErrorString());
			showSnackbar(getString(R.string.context_no_link));
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

	}

	public static void log(String message) {
		Util.log("GetLinkActivityLollipop", message);
	}

}
