package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.utils.DBUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class CentiliFragmentLollipop extends Fragment implements MegaRequestListenerInterface, MegaGlobalListenerInterface {
	
WebView myWebView;
	
	MegaApiAndroid megaApi;
	Context context;
	MyAccountInfo myAccountInfo;
	private ActionBar aB;

	DatabaseHandler dbH;
	
	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeGlobalListener(this);
		}
		
		super.onDestroy();
	}
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		megaApi.addGlobalListener(this);

		dbH = DatabaseHandler.getDbHandler(context);
		
		super.onCreate(savedInstanceState);
		logDebug("onCreate");
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");

		DecimalFormat df = new DecimalFormat("#.##");  

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = getScaleW(outMetrics, density);
		float scaleH = getScaleH(outMetrics, density);

		View v = null;
		v = inflater.inflate(R.layout.activity_fortumo_payment, container, false);
		
	        
        myWebView = (WebView) v.findViewById(R.id.webview);
//        WebSettings webSettings = myWebView.getSettings();
//        webSettings.setJavaScriptEnabled(true);

		if(callToPricing(context)){
			logDebug("megaApi.getPricing SEND");
			((MegaApplication) ((Activity)context).getApplication()).askForPricing();
		}else{
			getPaymentId();
		}

		if(myAccountInfo==null){
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}

		return v;
	}

	public void getPaymentId(){
		logDebug("getPaymentId");
		if(myAccountInfo==null){
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}

		if(myAccountInfo == null){
			return;
		}

		MegaAttributes attributes = dbH.getAttributes();

		ArrayList<Product> p = myAccountInfo.getProductAccounts();
		for (int i=0;i<p.size();i++){
			Product account = p.get(i);
			if ((account.getLevel()==4) && (account.getMonths()==1)){
				long planHandle = account.getHandle();
				long lastPublicHandle = attributes.getLastPublicHandle();
				if (lastPublicHandle == MegaApiJava.INVALID_HANDLE){
					megaApi.getPaymentId(planHandle, this);
				}
				else{
					megaApi.getPaymentId(planHandle, lastPublicHandle, attributes.getLastPublicHandleType(),
							attributes.getLastPublicHandleTimeStamp(), this);
				}
				logDebug("Plan handle: " + planHandle + ", Last public handle: " + lastPublicHandle);
			}
		}
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {

		logDebug("REQUEST: " + request.getName() + "__" + request.getRequestString());
		if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_ID){
			logDebug("PAYMENT ID: " + request.getLink());
//			Toast.makeText(context, "PAYMENTID: " + request.getLink(), Toast.LENGTH_LONG).show();

			/*INICIO FORTUMO*/
//			String urlFortumo = "http://fortumo.com/mobile_payments/f250460ec5d97fd27e361afaa366db0f?cuid=" + request.getLink();
//			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlFortumo));
//			startActivity(browserIntent);
//			((ManagerActivity)context).onBackPressed();
////			myWebView.loadUrl(urlFortumo);
			/*FIN FORTUMO*/
			
			/*INICIO CENTILI*/
			String urlCentili = "https://widget.centili.com/widget/WidgetModule?api=9e8eee856f4c048821954052a8d734ac&clientid=" + request.getLink();
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlCentili));
			startActivity(browserIntent);
			((ManagerActivityLollipop)context).onBackPressed();
			/*FIN CENTILI*/
		}
	}
	
	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		// TODO Auto-generated method stub
		
	}
	
//	public int onBackPressed(){
////		((ManagerActivity)context).showpF(parameterType, accounts);
//		((ManagerActivity)context).showpF(parameterType, accounts, true);
//		return 3;
//	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
		aB = ((AppCompatActivity)activity).getSupportActionBar();
	}

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
		logDebug("onUserAlertsUpdate");
	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAccountUpdate(MegaApiJava api) {
		Toast.makeText(context, "ON ACCOUNT UPDATE!!!!", Toast.LENGTH_LONG).show();
		((ManagerActivityLollipop)context).onBackPressed();
	}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api,
			ArrayList<MegaContactRequest> requests) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEvent(MegaApiJava api, MegaEvent event) {

	}
}
