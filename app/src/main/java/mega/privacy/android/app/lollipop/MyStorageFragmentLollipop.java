package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.DBUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;


public class MyStorageFragmentLollipop extends Fragment implements OnClickListener, MegaRequestListenerInterface{

	Context context;
	MyAccountInfo myAccountInfo;

	MegaUser myUser;

	TextView typeAccount;
	TextView expirationAccount;
	TextView usedSpace;
	TextView lastSession;
	TextView connections;
	TextView fingerprint;

	RelativeLayout typeLayout;
	LinearLayout expirationLayout;
	LinearLayout lastSessionLayout;
	LinearLayout connectionsLayout;
	LinearLayout fingerprintLayout;

	LinearLayout parentLinearLayout;
	
	DisplayMetrics outMetrics;
	float density;

	MegaApiAndroid megaApi;

	@Override
	public void onCreate (Bundle savedInstanceState){
		log("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		super.onCreate(savedInstanceState);
	}
	
	public void onDestroy()
	{
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		super.onDestroy();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		log("onCreateView");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);

		View v = inflater.inflate(R.layout.fragment_my_storage, container, false);
		
		myUser = megaApi.getMyUser();
		if(myUser == null){
			return null;
		}

		typeLayout = (RelativeLayout) v.findViewById(R.id.my_storage_account_type_layout);
		LinearLayout.LayoutParams typeLayoutParams = (LinearLayout.LayoutParams)typeLayout.getLayoutParams();
		typeLayoutParams.setMargins(Util.scaleWidthPx(60, outMetrics), Util.scaleHeightPx(27, outMetrics), 0, 0);
		typeLayout.setLayoutParams(typeLayoutParams);

		typeAccount = (TextView) v.findViewById(R.id.my_storage_account_type_text);
		LinearLayout.LayoutParams typeAccountParams = (LinearLayout.LayoutParams)typeAccount.getLayoutParams();
		typeAccountParams.setMargins(0, Util.scaleHeightPx(5, outMetrics), 0, 0);
		typeAccount.setLayoutParams(typeAccountParams);

		usedSpace = (TextView) v.findViewById(R.id.my_storage_used_space);

		expirationLayout = (LinearLayout) v.findViewById(R.id.my_storage_expiration_layout);
		LinearLayout.LayoutParams expirationParams = (LinearLayout.LayoutParams)expirationLayout.getLayoutParams();
		expirationParams.setMargins(Util.scaleWidthPx(60, outMetrics), Util.scaleHeightPx(30, outMetrics), 0, 0);
		expirationLayout.setLayoutParams(expirationParams);
		expirationAccount = (TextView) v.findViewById(R.id.my_storage_expiration);

		lastSessionLayout = (LinearLayout) v.findViewById(R.id.my_storage_last_session_layout);
		LinearLayout.LayoutParams lastSessionParams = (LinearLayout.LayoutParams)lastSessionLayout.getLayoutParams();
		lastSessionParams.setMargins(Util.scaleWidthPx(60, outMetrics), Util.scaleHeightPx(30, outMetrics), 0, 0);
		lastSessionLayout.setLayoutParams(lastSessionParams);

		lastSession = (TextView) v.findViewById(R.id.my_storage_last_session);

		fingerprintLayout = (LinearLayout) v.findViewById(R.id.my_storage_fingerprint_layout);
		LinearLayout.LayoutParams fingerprintParams = (LinearLayout.LayoutParams)fingerprintLayout.getLayoutParams();
		fingerprintParams.setMargins(Util.scaleWidthPx(60, outMetrics), Util.scaleHeightPx(30, outMetrics), 0, 0);
		fingerprintLayout.setLayoutParams(fingerprintParams);

		fingerprint = (TextView) v.findViewById(R.id.my_storage_fingerprint);

		connectionsLayout = (LinearLayout) v.findViewById(R.id.my_storage_connections_layout);
		LinearLayout.LayoutParams connectionsParams = (LinearLayout.LayoutParams)connectionsLayout.getLayoutParams();
		connectionsParams.setMargins(Util.scaleWidthPx(60, outMetrics), Util.scaleHeightPx(30, outMetrics), 0, 0);
		connectionsLayout.setLayoutParams(connectionsParams);

		connections = (TextView) v.findViewById(R.id.my_storage_connections);

		expirationLayout.setVisibility(View.GONE);

		parentLinearLayout = (LinearLayout) v.findViewById(R.id.my_storage_parent_linear_layout);

		if(myAccountInfo==null){
			log("MyAccountInfo is NULL");
			myAccountInfo = ((ManagerActivityLollipop)context).getMyAccountInfo();
		}

		fingerprintLayout.setVisibility(View.GONE);
		if (megaApi.getMyFingerprint() != null){
			if (megaApi.getMyFingerprint().compareTo("") != 0){
				fingerprintLayout.setVisibility(View.VISIBLE);
				String fingerprintString = megaApi.getMyFingerprint();
				String fingerprintUIString = "";
				for (int i=0;i<fingerprintString.length();i++){
					if (i != 0){
						if ((i % 20) == 0){
							fingerprintUIString = fingerprintUIString + "\n" + fingerprintString.charAt(i);
						}
						else if ((i % 4) == 0){
							fingerprintUIString = fingerprintUIString + " " + fingerprintString.charAt(i);
						}
						else{
							fingerprintUIString = fingerprintUIString + fingerprintString.charAt(i);
						}
					}
					else{
						fingerprintUIString = fingerprintUIString + fingerprintString.charAt(i);
					}
				}

				fingerprint.setText(fingerprintUIString);
			}
		}
		
		ArrayList<MegaUser> contacts = megaApi.getContacts();
		ArrayList<MegaUser> visibleContacts=new ArrayList<MegaUser>();

		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
				visibleContacts.add(contacts.get(i));
			}
		}		
		connections.setText(visibleContacts.size()+" " + context.getResources().getQuantityString(R.plurals.general_num_contacts, visibleContacts.size()));

		setAccountDetails();

		refreshAccountInfo();

		return v;
	}

	public static MyStorageFragmentLollipop newInstance() {
		log("newInstance");
		MyStorageFragmentLollipop fragment = new MyStorageFragmentLollipop();
		return fragment;
	}

	public void refreshAccountInfo(){
		log("refreshAccountInfo");

		//Check if the call is recently
		log("Check the last call to getAccountDetails");
		if(DBUtil.callToAccountDetails(context)){
			log("megaApi.getAccountDetails SEND");
			megaApi.getAccountDetails(myAccountInfo);
		}
		log("Check the last call to getExtendedAccountDetails");
		if(DBUtil.callToExtendedAccountDetails(context)){
			log("megaApi.getExtendedAccountDetails SEND");
			megaApi.getExtendedAccountDetails(true, false, false, myAccountInfo);
		}
		log("Check the last call to callToPaymentMethods");
		if(DBUtil.callToPaymentMethods(context)){
			log("megaApi.getPaymentMethods SEND");
			megaApi.getPaymentMethods(myAccountInfo);
		}
	}

	public void setAccountDetails(){
		log("setAccountDetails");

		if((getActivity() == null) || (!isAdded())){
			log("Fragment MyAccount NOT Attached!");
			return;
		}
		//Set account details
		if(myAccountInfo.getAccountType()<0||myAccountInfo.getAccountType()>4){
//			typeAccount.setText(getString(R.string.recovering_info));
		}
		else{
			switch(myAccountInfo.getAccountType()){

				case 0:{
//					typeAccount.setText(R.string.my_account_free);
					expirationLayout.setVisibility(View.GONE);
					break;
				}

				case 1:{
//					typeAccount.setText(getString(R.string.my_account_pro1));
					expirationLayout.setVisibility(View.VISIBLE);
					expirationAccount.setText(Util.getDateString(myAccountInfo.getAccountInfo().getProExpiration()));
					break;
				}

				case 2:{
//					typeAccount.setText(getString(R.string.my_account_pro2));
					expirationLayout.setVisibility(View.VISIBLE);
					expirationAccount.setText(Util.getDateString(myAccountInfo.getAccountInfo().getProExpiration()));
					break;
				}

				case 3:{
//					typeAccount.setText(getString(R.string.my_account_pro3));
					expirationLayout.setVisibility(View.VISIBLE);
					expirationAccount.setText(Util.getDateString(myAccountInfo.getAccountInfo().getProExpiration()));
					break;
				}

				case 4:{
//					typeAccount.setText(getString(R.string.my_account_prolite));
					expirationLayout.setVisibility(View.VISIBLE);
					expirationAccount.setText(Util.getDateString(myAccountInfo.getAccountInfo().getProExpiration()));
					break;
				}

			}
		}


//		if (getPaymentMethodsBoolean == true){
//			if (upgradeButton != null){
//				if ((myAccountInfo.getAccountInfo().getSubscriptionStatus() == MegaAccountDetails.SUBSCRIPTION_STATUS_NONE) || (myAccountInfo.getAccountInfo().getSubscriptionStatus() == MegaAccountDetails.SUBSCRIPTION_STATUS_INVALID)){
//					Time now = new Time();
//					now.setToNow();
//					if (myAccountInfo.getAccountType() != 0){
//						if (now.toMillis(false) >= (myAccountInfo.getAccountInfo().getProExpiration()*1000)){
//							if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD) || Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
//								upgradeButton.setVisibility(View.VISIBLE);
//							}
//						}
//					}
//					else{
//						if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD) || Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
//							upgradeButton.setVisibility(View.VISIBLE);
//						}
//					}
//				}
//			}
//		}
		if(myAccountInfo.getUsedFormatted().trim().length()<=0){
			usedSpace.setText(getString(R.string.recovering_info));
		}
		else{
			String usedSpaceString = myAccountInfo.getUsedFormatted() + " " + getString(R.string.general_x_of_x) + " " + myAccountInfo.getTotalFormatted();
			usedSpace.setText(usedSpaceString);
		}

		if(myAccountInfo.getLastSessionFormattedDate()!=null) {
			if (myAccountInfo.getLastSessionFormattedDate().trim().length() <= 0) {
				lastSession.setText(getString(R.string.recovering_info));
			} else {
				lastSession.setText(myAccountInfo.getLastSessionFormattedDate());
			}
		}
		else{
			lastSession.setText(getString(R.string.recovering_info));
		}
		///////////
	}

	@Override
	public void onAttach(Activity activity) {
		log("onAttach");
		super.onAttach(activity);
		context = activity;
	}

	@Override
	public void onAttach(Context context) {
		log("onAttach context");
		super.onAttach(context);
		this.context = context;
	}

	@Override
	public void onClick(View v) {
		log("onClick");
		switch (v.getId()) {

		}
	}

	public int onBackPressed(){
		log("onBackPressed");

//		if(exportMKLayout.getVisibility()==View.VISIBLE){
//			log("Master Key layout is VISIBLE");
//			hideMKLayout();
//			return 1;
//		}

		return 0;
	}

	public String getDescription(ArrayList<MegaNode> nodes){
		int numFolders = 0;
		int numFiles = 0;

		for (int i=0;i<nodes.size();i++){
			MegaNode c = nodes.get(i);
			if (c.isFolder()){
				numFolders++;
			}
			else{
				numFiles++;
			}
		}

		String info = "";
		if (numFolders > 0){
			info = numFolders +  " " + context.getResources().getQuantityString(R.plurals.general_num_shared_folders, numFolders);
			if (numFiles > 0){
				info = info + ", " + numFiles + " " + context.getResources().getQuantityString(R.plurals.general_num_shared_folders, numFiles);
			}
		}
		else {
			if (numFiles == 0){
				info = numFiles +  " " + context.getResources().getQuantityString(R.plurals.general_num_shared_folders, numFolders);
			}
			else{
				info = numFiles +  " " + context.getResources().getQuantityString(R.plurals.general_num_shared_folders, numFiles);
			}
		}

		return info;
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		log("onRequestFinish");

	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError");
	}

	public static void log(String log) {
		Util.log("MyStorageFragmentLollipop", log);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

	}

	public MyAccountInfo getMyAccountInfo() {
		return myAccountInfo;
	}

}
