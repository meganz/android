package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.utils.DBUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;


public class MyStorageFragmentLollipop extends Fragment implements MegaRequestListenerInterface{

	Context context;
	MyAccountInfo myAccountInfo;

	MegaUser myUser;

	LinearLayout parentLinearLayout;

	RelativeLayout expirationAccountLayout;

	TextView typeAccountText;
	TextView expirationAccountTitle;
	TextView expirationAccountText;
	TextView storageAvailableText;
	TextView transferQuotaUsedText;

	TextView totalUsedSpace;

	TextView cloudDriveUsedText;
	TextView inboxUsedText;
	TextView incomingUsedText;
	TextView rubbishUsedText;
	TextView availableSpaceText;

	ProgressBar progressBar;
	
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

		parentLinearLayout = (LinearLayout) v.findViewById(R.id.my_storage_parent_linear_layout);

		typeAccountText = (TextView) v.findViewById(R.id.my_storage_account_plan_text);
		storageAvailableText = (TextView) v.findViewById(R.id.my_storage_account_space_text);
		expirationAccountLayout = (RelativeLayout) v.findViewById(R.id.my_storage_account_expiration_layout);
		expirationAccountTitle = (TextView) v.findViewById(R.id.my_storage_account_expiration_title);
		expirationAccountText = (TextView) v.findViewById(R.id.my_storage_account_expiration_text);
		transferQuotaUsedText = (TextView) v.findViewById(R.id.my_storage_account_transfer_text);

		totalUsedSpace = (TextView) v.findViewById(R.id.my_storage_used_space_result_text);

		cloudDriveUsedText = (TextView) v.findViewById(R.id.my_storage_account_cloud_storage_text);
		inboxUsedText = (TextView) v.findViewById(R.id.my_storage_account_inbox_storage_text);
		incomingUsedText = (TextView) v.findViewById(R.id.my_storage_account_incoming_storage_text);
		rubbishUsedText = (TextView) v.findViewById(R.id.my_storage_account_rubbish_storage_text);
		availableSpaceText = (TextView) v.findViewById(R.id.my_storage_account_available_storage_text);

		progressBar = (ProgressBar) v.findViewById(R.id.my_storage_progress_bar);
		progressBar.setProgress(0);

//		RelativeLayout.LayoutParams bottomParams = (RelativeLayout.LayoutParams)progressBar.getLayoutParams();
//		bottomParams.setMargins(0, 0, 0, Util.scaleHeightPx(32, outMetrics));
//		progressBar.setLayoutParams(bottomParams);

		if(myAccountInfo==null){
			log("MyAccountInfo is NULL");
			myAccountInfo = ((ManagerActivityLollipop)context).getMyAccountInfo();
		}

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		setAccountDetails();
//
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
			typeAccountText.setText(getString(R.string.recovering_info));
			expirationAccountText.setText(getString(R.string.recovering_info));
			storageAvailableText.setText(getString(R.string.recovering_info));
		}
		else{
			storageAvailableText.setText(myAccountInfo.getTotalFormatted());

			log("ExpirationTime: "+Util.getDateString(myAccountInfo.getAccountInfo().getProExpiration()));
			log("Subscription cycle: "+myAccountInfo.getAccountInfo().getSubscriptionCycle());
			log("Renews on: "+Util.getDateString(myAccountInfo.getAccountInfo().getSubscriptionRenewTime()));

			switch(myAccountInfo.getAccountType()){

				case 0:{
					typeAccountText.setText(R.string.free_account);
					expirationAccountLayout.setVisibility(View.GONE);
					break;
				}
				case 1:{
					typeAccountText.setText(getString(R.string.pro1_account));
					if(myAccountInfo.getAccountInfo().getSubscriptionStatus()== MegaAccountDetails.SUBSCRIPTION_STATUS_VALID){
						expirationAccountTitle.setText(getString(R.string.renews_on));
						expirationAccountText.setText(Util.getDateString(myAccountInfo.getAccountInfo().getSubscriptionRenewTime()));
					}
					else{
						expirationAccountTitle.setText(getString(R.string.expires_on));
						expirationAccountText.setText(Util.getDateString(myAccountInfo.getAccountInfo().getProExpiration()));
					}
					break;
				}
				case 2:{
					typeAccountText.setText(getString(R.string.pro2_account));
					if(myAccountInfo.getAccountInfo().getSubscriptionStatus()== MegaAccountDetails.SUBSCRIPTION_STATUS_VALID){
						expirationAccountTitle.setText(getString(R.string.renews_on));
						expirationAccountText.setText(Util.getDateString(myAccountInfo.getAccountInfo().getSubscriptionRenewTime()));
					}
					else{
						expirationAccountTitle.setText(getString(R.string.expires_on));
						expirationAccountText.setText(Util.getDateString(myAccountInfo.getAccountInfo().getProExpiration()));
					}
					break;
				}
				case 3:{
					typeAccountText.setText(getString(R.string.pro3_account));
					if(myAccountInfo.getAccountInfo().getSubscriptionStatus()== MegaAccountDetails.SUBSCRIPTION_STATUS_VALID){
						expirationAccountTitle.setText(getString(R.string.renews_on));
						expirationAccountText.setText(Util.getDateString(myAccountInfo.getAccountInfo().getSubscriptionRenewTime()));
					}
					else{
						expirationAccountTitle.setText(getString(R.string.expires_on));
						expirationAccountText.setText(Util.getDateString(myAccountInfo.getAccountInfo().getProExpiration()));
					}
					break;
				}
				case 4:{
					typeAccountText.setText(getString(R.string.prolite_account));
					if(myAccountInfo.getAccountInfo().getSubscriptionStatus()== MegaAccountDetails.SUBSCRIPTION_STATUS_VALID){
						expirationAccountTitle.setText(getString(R.string.renews_on));
						expirationAccountText.setText(Util.getDateString(myAccountInfo.getAccountInfo().getSubscriptionRenewTime()));
					}
					else{
						expirationAccountTitle.setText(getString(R.string.expires_on));
						expirationAccountText.setText(Util.getDateString(myAccountInfo.getAccountInfo().getProExpiration()));
					}
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
			totalUsedSpace.setText(getString(R.string.recovering_info));
		}
		else{
			String usedSpaceString = String.format(context.getString(R.string.my_account_of_string), myAccountInfo.getUsedFormatted(), myAccountInfo.getTotalFormatted());
			try{
				usedSpaceString = usedSpaceString.replace("[A]", "<font color=\'#777777\'>");
				usedSpaceString = usedSpaceString.replace("[/A]", "</font>");
			}
			catch (Exception e){}
			Spanned result = null;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
				result = Html.fromHtml(usedSpaceString,Html.FROM_HTML_MODE_LEGACY);
			} else {
				result = Html.fromHtml(usedSpaceString);
			}

			totalUsedSpace.setText(result);
		}

		if(myAccountInfo.getAccountInfo()==null){
			log("Account info NULL");
			return;
		}

		//Check size of the different nodes
		cloudDriveUsedText.setText(myAccountInfo.getFormattedUsedCloud());
		inboxUsedText.setText(myAccountInfo.getFormattedUsedInbox());
		rubbishUsedText.setText(myAccountInfo.getFormattedUsedRubbish());
		incomingUsedText.setText(myAccountInfo.getFormattedUsedIncoming());
		availableSpaceText.setText(myAccountInfo.getFormattedAvailableSpace());

		if(myAccountInfo.getAccountType()==0){
			transferQuotaUsedText.setText(context.getString(R.string.not_available));
			transferQuotaUsedText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));
		}
		else{
			if(myAccountInfo.getAccountInfo().getTransferOwnUsed()<0){
				transferQuotaUsedText.setText(getString(R.string.recovering_info));
			}
			else{
				long transferQuotaUsed = myAccountInfo.getAccountInfo().getTransferOwnUsed();
				long transferQuotaMax = myAccountInfo.getAccountInfo().getTransferMax();

				String textToShow = String.format(context.getString(R.string.my_account_of_string), Util.getSizeString(transferQuotaUsed), Util.getSizeString(transferQuotaMax));
				try{
					textToShow = textToShow.replace("[A]", "<font color=\'#777777\'>");
					textToShow = textToShow.replace("[/A]", "</font>");
				}
				catch (Exception e){}
				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}

				transferQuotaUsedText.setText(result);
			}
		}

		int usedPerc = myAccountInfo.getUsedPerc();
		if (usedPerc < 90){
			progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_ok));
		}
		else if ((usedPerc >= 90) && (usedPerc <= 95)){
			progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_warning));
		}
		else{
			if (usedPerc > 100){
				myAccountInfo.setUsedPerc(100);
			}
			progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_exceed));
		}
		progressBar.setProgress(usedPerc);
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

	public int onBackPressed(){
		log("onBackPressed");

//		if(exportMKLayout.getVisibility()==View.VISIBLE){
//			log("Master Key layout is VISIBLE");
//			hideMKLayout();
//			return 1;
//		}
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
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
