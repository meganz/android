package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
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
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.DBUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class MyStorageFragmentLollipop extends Fragment {

	private ScrollView scrollView;
	private Context context;
	private MyAccountInfo myAccountInfo;

	private MegaUser myUser;

	private LinearLayout parentLinearLayout;
	private TextView transferQuotaUsedText;

	private RelativeLayout inboxStorageLayout;

	private TextView totalUsedSpace;
	private TextView cloudDriveUsedText;
	private TextView inboxUsedText;
	private TextView incomingUsedText;
	private TextView rubbishUsedText;
	private TextView previousVersionsText;

	private RelativeLayout previousVersionsLayout;
	
	private DisplayMetrics outMetrics;

	private MegaApiAndroid megaApi;

	private Typeface sansSerifLightBoldTypeface;
	private Typeface normalTypeface;

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

		scrollView = v.findViewById(R.id.my_storage_complete_relative_layout);
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

		parentLinearLayout = v.findViewById(R.id.my_storage_parent_linear_layout);

		/* Used space */
		totalUsedSpace = v.findViewById(R.id.used_storage_text);

		/* Transfer quota */
		transferQuotaUsedText = v.findViewById(R.id.used_transfer_text);

		/* Usage storage */
		cloudDriveUsedText = v.findViewById(R.id.my_storage_account_cloud_storage_text);
		inboxStorageLayout = v.findViewById(R.id.inbox_storage_container);
		inboxUsedText = v.findViewById(R.id.my_storage_account_inbox_storage_text);
		incomingUsedText = v.findViewById(R.id.my_storage_account_incoming_storage_text);
		rubbishUsedText = v.findViewById(R.id.my_storage_account_rubbish_storage_text);
		previousVersionsLayout = v.findViewById(R.id.previous_versions_storage_container);
		previousVersionsText = v.findViewById(R.id.my_storage_account_previous_versions_text);

		if(myAccountInfo==null){
			logWarning("MyAccountInfo is NULL");
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}

		sansSerifLightBoldTypeface = Typeface.create("sans-serif-light", Typeface.BOLD);
		normalTypeface = totalUsedSpace.getTypeface();

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

		if(myAccountInfo.getUsedFormatted().trim().length()<=0){
			totalUsedSpace.setText(getString(R.string.recovering_info));
			totalUsedSpace.setTextColor(getResources().getColor(R.color.black_85_alpha));
			totalUsedSpace.setTypeface(normalTypeface);
		}
		else{
			totalUsedSpace.setTextColor(getResources().getColor(R.color.accentColor));

			if (megaApi.isBusinessAccount()) {
				totalUsedSpace.setText(myAccountInfo.getUsedFormatted());
				totalUsedSpace.setTypeface(sansSerifLightBoldTypeface);
			} else {
				String usedSpaceString = String.format(context.getString(R.string.my_account_of_string), myAccountInfo.getUsedFormatted(), myAccountInfo.getTotalFormatted());
				try {
					usedSpaceString = usedSpaceString.replace("[A]", "<font color=\'#000000\'>");
					usedSpaceString = usedSpaceString.replace("[/A]", "</font>");
				} catch (Exception e) {
				}
				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(usedSpaceString, Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(usedSpaceString);
				}

				totalUsedSpace.setText(result);
			}
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
			transferQuotaUsedText.setTextColor(ContextCompat.getColor(context, R.color.black_85_alpha));
			transferQuotaUsedText.setTypeface(normalTypeface);
		} else if(myAccountInfo.getUsedTransferFormatted().trim().length()<=0){
			transferQuotaUsedText.setText(getString(R.string.recovering_info));
			transferQuotaUsedText.setTextColor(getResources().getColor(R.color.black_85_alpha));
			transferQuotaUsedText.setTypeface(normalTypeface);
		} else{
			transferQuotaUsedText.setTextColor(getResources().getColor(R.color.accentColor));

			if (megaApi.isBusinessAccount()) {
				transferQuotaUsedText.setText(myAccountInfo.getUsedTransferFormatted());
				transferQuotaUsedText.setTypeface(sansSerifLightBoldTypeface);
			} else {
				String textToShow = String.format(context.getString(R.string.my_account_of_string), myAccountInfo.getUsedTransferFormatted(), myAccountInfo.getTotalTansferFormatted());
				try {
					textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
					textToShow = textToShow.replace("[/A]", "</font>");
				} catch (Exception e) {
				}
				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}

				transferQuotaUsedText.setText(result);
			}
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
