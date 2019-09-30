package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.DBUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;

public class MyStorageFragmentLollipop extends Fragment {

	ScrollView scrollView;
	Context context;
	MyAccountInfo myAccountInfo;

	MegaUser myUser;

	LinearLayout parentLinearLayout;

	RelativeLayout expirationAccountLayout;

	TextView typeAccountText;
	ImageView typeAccountIcon;

	TextView expirationAccountTitle;
	TextView expirationAccountText;
	ImageView transferQuotaUsedIcon;
	TextView transferQuotaUsedText;

	RelativeLayout inboxStorageLayout;

	TextView totalUsedSpace;
	TextView cloudDriveUsedText;
	TextView inboxUsedText;
	TextView incomingUsedText;
	TextView rubbishUsedText;
	TextView previousVersionsText;

	RelativeLayout previousVersionsLayout;

	ProgressBar progressBar;
	
	DisplayMetrics outMetrics;
	float density;

	MegaApiAndroid megaApi;

	@Override
	public void onCreate (Bundle savedInstanceState){
		logDebug("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		super.onCreate(savedInstanceState);
	}

	@Override
	public void onResume () {
		super.onResume();

		refreshAccountInfo();
	}

	public void checkScroll () {
		if (scrollView != null) {
			if (scrollView.canScrollVertically(-1)) {
				((ManagerActivityLollipop) context).changeActionBarElevation(true);
			}
			else {
				((ManagerActivityLollipop) context).changeActionBarElevation(false);
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);

		View v = inflater.inflate(R.layout.fragment_my_storage, container, false);

		scrollView = (ScrollView) v.findViewById(R.id.my_storage_complete_relative_layout);
		new ListenScrollChangesHelper().addViewToListen(scrollView, new ListenScrollChangesHelper.OnScrollChangeListenerCompat() {
			@Override
			public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
				if (scrollView.canScrollVertically(-1)){
					((ManagerActivityLollipop) context).changeActionBarElevation(true);
				}
				else {
					((ManagerActivityLollipop) context).changeActionBarElevation(false);
				}
			}
		});
		
		myUser = megaApi.getMyUser();
		if(myUser == null){
			return null;
		}

		parentLinearLayout = (LinearLayout) v.findViewById(R.id.my_storage_parent_linear_layout);

		/* Account plan */
		typeAccountIcon = (ImageView) v.findViewById(R.id.my_storage_account_plan_icon);
		typeAccountText = (TextView) v.findViewById(R.id.my_storage_account_plan_text);

		/* Progress bar */
		progressBar = (ProgressBar) v.findViewById(R.id.my_storage_progress_bar);
		progressBar.setProgress(0);

		/* Used space */
		totalUsedSpace = (TextView) v.findViewById(R.id.my_storage_used_space_result_text);

		/* Expiration */
		expirationAccountLayout = (RelativeLayout) v.findViewById(R.id.my_storage_account_expiration_layout);
		expirationAccountTitle = (TextView) v.findViewById(R.id.my_storage_account_expiration_title);
		expirationAccountText = (TextView) v.findViewById(R.id.my_storage_account_expiration_text);

		/* Transfer quota */
		transferQuotaUsedIcon = (ImageView) v.findViewById(R.id.my_storage_account_transfer_icon);
		transferQuotaUsedText = (TextView) v.findViewById(R.id.my_storage_account_transfer_text);

		/* Usage storage */
		inboxStorageLayout = (RelativeLayout) v.findViewById(R.id.my_storage_account_inbox_storage_layout);
		cloudDriveUsedText = (TextView) v.findViewById(R.id.my_storage_account_cloud_storage_text);
		inboxUsedText = (TextView) v.findViewById(R.id.my_storage_account_inbox_storage_text);
		incomingUsedText = (TextView) v.findViewById(R.id.my_storage_account_incoming_storage_text);
		rubbishUsedText = (TextView) v.findViewById(R.id.my_storage_account_rubbish_storage_text);
		previousVersionsText = (TextView) v.findViewById(R.id.my_storage_account_previous_versions_text);
		previousVersionsLayout = (RelativeLayout) v.findViewById(R.id.my_storage_account_previous_versions_layout);
		TextView previousVersionLbl = (TextView) v.findViewById(R.id.my_storage_account_previous_versions_title);
        previousVersionLbl.setText(getResources().getQuantityString(R.plurals.header_previous_section_item, 2));

		if(myAccountInfo==null){
			logWarning("MyAccountInfo is NULL");
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}

		setAccountDetails();

		return v;
	}

	public static MyStorageFragmentLollipop newInstance() {
		logDebug("newInstance");
		MyStorageFragmentLollipop fragment = new MyStorageFragmentLollipop();
		return fragment;
	}

	public void refreshAccountInfo(){
		logDebug("refreshAccountInfo");

		//Check if the call is recently
		logDebug("Check the last call to getAccountDetails");
		if(callToAccountDetails(context)){
			logDebug("megaApi.getAccountDetails SEND");
			((MegaApplication) ((Activity)context).getApplication()).askForAccountDetails();
		}
	}

	public void setAccountDetails(){
		logDebug("setAccountDetails");

		if((getActivity() == null) || (!isAdded())){
			logWarning("Fragment MyAccount NOT Attached!");
			return;
		}
		//Set account details
		if(myAccountInfo.getAccountType()<0||myAccountInfo.getAccountType()>4){
			typeAccountText.setText(getString(R.string.recovering_info));
			expirationAccountText.setText(getString(R.string.recovering_info));
			typeAccountIcon.setVisibility(View.GONE);
		}
		else{

			logDebug("ExpirationTime: " + getDateString(myAccountInfo.getAccountInfo().getProExpiration()));
			logDebug("Subscription cycle: " + myAccountInfo.getAccountInfo().getSubscriptionCycle());
			logDebug("Renews on: " + getDateString(myAccountInfo.getAccountInfo().getSubscriptionRenewTime()));

			switch(myAccountInfo.getAccountType()){

				case 0:{
					typeAccountText.setText(getString(R.string.free_account).toUpperCase());
					typeAccountIcon.setVisibility(View.VISIBLE);
					typeAccountIcon.setBackground(ContextCompat.getDrawable(context, R.drawable.ic_free_crest));
					expirationAccountLayout.setVisibility(View.GONE);
					break;
				}
				case 1:{
					setRenewExpireDate(getString(R.string.pro1_account), ContextCompat.getDrawable(context, R.drawable.ic_pro_1_crest));
					break;
				}
				case 2:{
					setRenewExpireDate(getString(R.string.pro2_account), ContextCompat.getDrawable(context, R.drawable.ic_pro_2_crest));
					break;
				}
				case 3:{
					setRenewExpireDate(getString(R.string.pro3_account), ContextCompat.getDrawable(context, R.drawable.ic_pro_3_crest));
					break;
				}
				case 4:{
					setRenewExpireDate(getString(R.string.prolite_account).toUpperCase(), ContextCompat.getDrawable(context, R.drawable.ic_lite_crest));
					break;
				}
			}
		}

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
			}else {
				result = Html.fromHtml(usedSpaceString);
			}

			totalUsedSpace.setText(result);
		}

		if(myAccountInfo.getAccountInfo()==null){
			logWarning("Account info NULL");
			return;
		}

		//Check size of the different nodes
		cloudDriveUsedText.setText(myAccountInfo.getFormattedUsedCloud());

		String inboxStorage = myAccountInfo.getFormattedUsedInbox();
		if(inboxStorage == null || inboxStorage.isEmpty()){
			inboxStorageLayout.setVisibility(View.GONE);
		}
		else{
			inboxStorageLayout.setVisibility(View.VISIBLE);
			inboxUsedText.setText(inboxStorage);
		}

		rubbishUsedText.setText(myAccountInfo.getFormattedUsedRubbish());
		incomingUsedText.setText(myAccountInfo.getFormattedUsedIncoming());

		if(myAccountInfo.getPreviousVersionsSize()>0){
			previousVersionsText.setText(myAccountInfo.getFormattedPreviousVersionsSize());
			previousVersionsLayout.setVisibility(View.VISIBLE);
		}
		else{
			previousVersionsLayout.setVisibility(View.GONE);
		}

		if(myAccountInfo.getAccountType()==0){
			transferQuotaUsedText.setText(context.getString(R.string.not_available));
			transferQuotaUsedText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));
		}
		else{
			if(myAccountInfo.getUsedTransferFormatted().trim().length()<=0){
				transferQuotaUsedText.setText(getString(R.string.recovering_info));
			}
			else{
				String textToShow = String.format(context.getString(R.string.my_account_of_string), myAccountInfo.getUsedTransferFormatted(), myAccountInfo.getTotalTansferFormatted());
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
			progressBar.setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.custom_progress_bar_horizontal_ok));
		}
		else if ((usedPerc >= 90) && (usedPerc <= 95)){
			progressBar.setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.custom_progress_bar_horizontal_warning));
		}
		else{
			if (usedPerc > 100){
				myAccountInfo.setUsedPerc(100);
			}
			progressBar.setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.custom_progress_bar_horizontal_exceed));
		}
		progressBar.setProgress(usedPerc);
	}

	void setRenewExpireDate(String title, Drawable drawable) {
		typeAccountText.setText(title);
		typeAccountIcon.setVisibility(View.VISIBLE);
		typeAccountIcon.setBackground(drawable);

		if(myAccountInfo.getSubscriptionStatus() == MegaAccountDetails.SUBSCRIPTION_STATUS_VALID
				&& myAccountInfo.getSubscriptionRenewTime() > 0){
			expirationAccountTitle.setText(getString(R.string.renews_on));
			expirationAccountText.setText(getDateString(myAccountInfo.getSubscriptionRenewTime()));
		}
		else if (myAccountInfo.getProExpirationTime() > 0){
			expirationAccountTitle.setText(getString(R.string.expires_on));
			expirationAccountText.setText(getDateString(myAccountInfo.getProExpirationTime()));
		}
		else {
			logError("Error. Renew date and expiration date invalids");
			expirationAccountLayout.setVisibility(View.GONE);
		}
	}

	public void refreshVersionsInfo(){

	    if(myAccountInfo==null){
	        return;
        }

		if(myAccountInfo.getPreviousVersionsSize()>0){
			previousVersionsText.setText(myAccountInfo.getFormattedPreviousVersionsSize());
			previousVersionsLayout.setVisibility(View.VISIBLE);
		}
		else{
			previousVersionsLayout.setVisibility(View.GONE);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		logDebug("onAttach");
		super.onAttach(activity);
		context = activity;
	}

	@Override
	public void onAttach(Context context) {
		logDebug("onAttach context");
		super.onAttach(context);
		this.context = context;
	}

	public int onBackPressed(){
		logDebug("onBackPressed");

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
}
