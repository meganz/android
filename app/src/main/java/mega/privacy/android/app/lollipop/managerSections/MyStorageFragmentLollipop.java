package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.utils.ColorUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.LogUtil.*;

public class MyStorageFragmentLollipop extends Fragment {

	private ScrollView scrollView;
	private Context context;
	private MyAccountInfo myAccountInfo;

	private MegaUser myUser;

	private TextView transferQuotaUsedText;

	private RelativeLayout inboxStorageLayout;

	private TextView totalUsedSpace;
	private TextView cloudDriveUsedText;
	private TextView inboxUsedText;
	private TextView incomingUsedText;
	private TextView rubbishUsedText;
	private TextView previousVersionsText;
	private LinearLayout rubbishSeparator;

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

		megaApi.getFileVersionsOption((ManagerActivityLollipop)context);

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
		rubbishSeparator = v.findViewById(R.id.rubbish_separator);

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

	public void setAccountDetails(){
		logDebug("setAccountDetails");

		if((getActivity() == null) || (!isAdded())){
			logWarning("Fragment MyAccount NOT Attached!");
			return;
		}

		if(myAccountInfo.getUsedFormatted().trim().length()<=0){
			totalUsedSpace.setText(getString(R.string.recovering_info));
			totalUsedSpace.setTextColor(getResources().getColor(R.color.grey_087_white_087));
			totalUsedSpace.setTypeface(normalTypeface);
		}
		else{
			totalUsedSpace.setTextColor(ColorUtils.getThemeColor(context, R.attr.colorSecondary));

			if (megaApi.isBusinessAccount()) {
				totalUsedSpace.setText(myAccountInfo.getUsedFormatted());
				totalUsedSpace.setTypeface(sansSerifLightBoldTypeface);
			} else {
				String usedSpaceString = String.format(context.getString(R.string.my_account_of_string), myAccountInfo.getUsedFormatted(), myAccountInfo.getTotalFormatted());
				try {
					usedSpaceString = usedSpaceString.replace("[A]", "<b><font face=\"sans-serif-light\">");
					usedSpaceString = usedSpaceString.replace("[/A]", "</font></b>");
					usedSpaceString = usedSpaceString.replace("[B]", "<font color=" +
							ColorUtils.getColorHexString(requireActivity(), R.color.grey_900_grey_100)
							+ ">");
					usedSpaceString = usedSpaceString.replace("[/B]", "</font>");
				} catch (Exception e) {
					logWarning("Exception formatting string", e);
				}

				totalUsedSpace.setText(HtmlCompat.fromHtml(usedSpaceString, HtmlCompat.FROM_HTML_MODE_LEGACY));
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

		refreshVersionsInfo();

		if(myAccountInfo.getAccountType()==0){
			transferQuotaUsedText.setText(context.getString(R.string.not_available));
			transferQuotaUsedText.setTextColor(ContextCompat.getColor(context, R.color.grey_087_white_087));
			transferQuotaUsedText.setTypeface(normalTypeface);
		} else if(myAccountInfo.getUsedTransferFormatted().trim().length()<=0){
			transferQuotaUsedText.setText(getString(R.string.recovering_info));
			transferQuotaUsedText.setTextColor(getResources().getColor(R.color.grey_087_white_087));
			transferQuotaUsedText.setTypeface(normalTypeface);
		} else{
			transferQuotaUsedText.setTextColor(ColorUtils.getThemeColor(context, R.attr.colorSecondary));

			if (megaApi.isBusinessAccount()) {
				transferQuotaUsedText.setText(myAccountInfo.getUsedTransferFormatted());
				transferQuotaUsedText.setTypeface(sansSerifLightBoldTypeface);
			} else {
				String textToShow = String.format(context.getString(R.string.my_account_of_string), myAccountInfo.getUsedTransferFormatted(), myAccountInfo.getTotalTansferFormatted());
				try {
					textToShow = textToShow.replace("[A]", "<b><font face=\"sans-serif-light\">");
					textToShow = textToShow.replace("[/A]", "</font></b>");
					textToShow = textToShow.replace("[B]", "<font color=\'#000000\'>");
					textToShow = textToShow.replace("[/B]", "</font>");
				} catch (Exception e) {
					logWarning("Exception formatting string", e);
				}

				transferQuotaUsedText.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));
			}
		}
	}

	public void refreshVersionsInfo(){

	    if(myAccountInfo==null){
	        return;
        }

		if(MegaApplication.isDisableFileVersions() == 0){
			rubbishSeparator.setVisibility(View.VISIBLE);
			previousVersionsText.setText(myAccountInfo.getFormattedPreviousVersionsSize());
			previousVersionsLayout.setVisibility(View.VISIBLE);
		}
		else{
			rubbishSeparator.setVisibility(View.GONE);
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

}
