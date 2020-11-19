package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.listeners.SessionTransferURLListener;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.utils.ColorUtils;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.billing.PaymentUtils.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.DBUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class UpgradeAccountFragmentLollipop extends Fragment implements OnClickListener{

	private MegaApiAndroid megaApi;
	public MyAccountInfo myAccountInfo;

	DisplayMetrics outMetrics;

	private ScrollView scrollView;
	private RelativeLayout semitransparentLayer;
	private TextView textMyAccount;

	private LinearLayout linearLayoutMain;

	int parameterType=-1;
	int paymentMethod = -1;

	private RelativeLayout proLiteLayout;
	private RelativeLayout pro1Layout;
	private RelativeLayout pro2Layout;
	private RelativeLayout pro3Layout;
	private RelativeLayout businessLayout;

	private RelativeLayout proLiteTransparentLayout;
	private RelativeLayout pro1TransparentLayout;
	private RelativeLayout pro3TransparentLayout;
	private RelativeLayout pro2TransparentLayout;

	private TextView monthSectionLite;
	private TextView storageSectionLite;
	private TextView bandwidthSectionLite;
	private TextView monthSectionPro1;
	private TextView storageSectionPro1;
	private TextView bandwidthSectionPro1;
	private TextView monthSectionPro2;
	private TextView storageSectionPro2;
	private TextView bandwidthSectionPro2;
	private TextView monthSectionPro3;
	private TextView storageSectionPro3;
	private TextView bandwidthSectionPro3;
	private TextView monthSectionBusiness;
	private TextView storageSectionBusiness;
	private TextView bandwidthSectionBusiness;
	private TextView labelCustomPlan;

	//Payment layout
	View selectPaymentMethodLayoutLite;
	View selectPaymentMethodLayoutPro1;
	View selectPaymentMethodLayoutPro2;
	View selectPaymentMethodLayoutPro3;
	private TextView selectPaymentMethod;
	private TextView paymentTitle;

	RelativeLayout googlePlayLayout;
	RelativeLayout creditCardLayout;
	RelativeLayout fortumoLayout;
	RelativeLayout centiliLayout;

	RelativeLayout googlePlayLayer;
	RelativeLayout creditCardLayer;
	RelativeLayout fortumoLayer;
	RelativeLayout centiliLayer;

	LinearLayout optionsBilling;
	RadioGroup billingPeriod;
	RadioButton billedMonthly;
	RadioButton billedYearly;
	LinearLayout layoutButtons;
	TextView buttonCancel;
    TextView buttonContinue;

	TextView fortumoText;
	TextView centiliText;
	TextView creditCardText;
	TextView googleWalletText;

	Context context;

	private final static int TYPE_STORAGE_LABEL = 0;
	private final static int TYPE_TRANSFER_LABEL = 1;

	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		super.onCreate(savedInstanceState);
		logDebug("onCreate");
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

		View v = inflater.inflate(R.layout.fragment_upgrade_account, container, false);
		scrollView = (ScrollView) v.findViewById(R.id.scroll_view_upgrade);

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
		linearLayoutMain = (LinearLayout) v.findViewById(R.id.linear_layout_upgrade);

		textMyAccount = (TextView) v.findViewById(R.id.text_of_my_account);
		semitransparentLayer = (RelativeLayout) v.findViewById(R.id.semitransparent_layer);
		semitransparentLayer.setOnClickListener(this);
		setAccountDetails();


		//PRO LITE ACCOUNT
		proLiteLayout = (RelativeLayout) v.findViewById(R.id.upgrade_prolite_layout);
		proLiteLayout.setOnClickListener(this);
		monthSectionLite = (TextView) v.findViewById(R.id.month_lite);
		storageSectionLite = (TextView) v.findViewById(R.id.storage_lite);
		bandwidthSectionLite = (TextView) v.findViewById(R.id.bandwidth_lite);
		selectPaymentMethodLayoutLite = v.findViewById(R.id.available_payment_methods_prolite);
		proLiteTransparentLayout = (RelativeLayout) v.findViewById(R.id.upgrade_prolite_layout_transparent);
		proLiteTransparentLayout.setVisibility(View.GONE);
		//END -- PRO LITE ACCOUNT

		//PRO I ACCOUNT
		pro1Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_i_layout);
		pro1Layout.setOnClickListener(this);
		monthSectionPro1 = (TextView) v.findViewById(R.id.month_pro_i);
		storageSectionPro1 = (TextView) v.findViewById(R.id.storage_pro_i);
		bandwidthSectionPro1 = (TextView) v.findViewById(R.id.bandwidth_pro_i);
		selectPaymentMethodLayoutPro1 = v.findViewById(R.id.available_payment_methods_pro_i);
		pro1TransparentLayout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_i_layout_transparent);
		pro1TransparentLayout.setVisibility(View.GONE);
		//END -- PRO I ACCOUNT

		//PRO II ACCOUNT
		pro2Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_ii_layout);
		pro2Layout.setOnClickListener(this);
		monthSectionPro2 = (TextView) v.findViewById(R.id.month_pro_ii);
		storageSectionPro2 = (TextView) v.findViewById(R.id.storage_pro_ii);
		bandwidthSectionPro2 = (TextView) v.findViewById(R.id.bandwidth_pro_ii);
		selectPaymentMethodLayoutPro2 = v.findViewById(R.id.available_payment_methods_pro_ii);
		pro2TransparentLayout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_ii_layout_transparent);
		pro2TransparentLayout.setVisibility(View.GONE);
		//END -- PRO II ACCOUNT

		//PRO III ACCOUNT
		pro3Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_iii_layout);
		pro3Layout.setOnClickListener(this);
		monthSectionPro3 = (TextView) v.findViewById(R.id.month_pro_iii);
		storageSectionPro3 = (TextView) v.findViewById(R.id.storage_pro_iii);
		bandwidthSectionPro3 = (TextView) v.findViewById(R.id.bandwidth_pro_iii);
		selectPaymentMethodLayoutPro3 = v.findViewById(R.id.available_payment_methods_pro_iii);
		pro3TransparentLayout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_iii_layout_transparent);
		pro3TransparentLayout.setVisibility(View.GONE);
		labelCustomPlan = v.findViewById(R.id.lbl_custom_plan);
		labelCustomPlan.setVisibility(View.GONE);
		labelCustomPlan.setOnClickListener(this);
		String strColor = getHexValue(getResources().getColor(R.color.accentColor));
		String textToShowB = getString(R.string.label_custom_plan);
		textToShowB = textToShowB.replace("[A]", "<font color=\'" + strColor + "\'>");
		textToShowB = textToShowB.replace("[/A]", "</font>");
		Spanned resultB;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			resultB = Html.fromHtml(textToShowB, Html.FROM_HTML_MODE_LEGACY);
		} else {
			resultB = Html.fromHtml(textToShowB);
		}
		labelCustomPlan.setText(resultB);
		//END -- PRO III ACCOUNT

		//BUSINESS ACCOUNT
		businessLayout = v.findViewById(R.id.upgrade_business_layout);
		businessLayout.setOnClickListener(this);
		monthSectionBusiness = v.findViewById(R.id.month_business);
		storageSectionBusiness = v.findViewById(R.id.storage_business);
		bandwidthSectionBusiness = v.findViewById(R.id.bandwidth_business);
		//END -- BUSINESS ACCOUNT

		setPricing();
		showAvailableAccount();

		refreshAccountInfo();

		int displayedAccountType = ((ManagerActivityLollipop)context).getDisplayedAccountType();
		if(displayedAccountType!=-1){
			switch(displayedAccountType){
				case PRO_LITE:{
					onUpgradeClick(PRO_LITE);
					break;
				}
				case PRO_I:{
					onUpgradeClick(PRO_I);
					break;
				}
				case PRO_II:{
					onUpgradeClick(PRO_II);
					break;
				}
				case PRO_III:{
					onUpgradeClick(PRO_III);
					break;
				}
			}
		}

		logDebug("END onCreateView");
		return v;
	}

	public void refreshAccountInfo(){
		logDebug("refreshAccountInfo");

		logDebug("Check the last call to callToPricing");
		if(callToPricing()){
			logDebug("megaApi.getPricing SEND");
			((MegaApplication) ((Activity)context).getApplication()).askForPricing();
		}

		logDebug("Check the last call to callToPaymentMethods");
		if(callToPaymentMethods()){
			logDebug("megaApi.getPaymentMethods SEND");
			((MegaApplication) ((Activity)context).getApplication()).askForPaymentMethods();
		}
	}

	public void setPricing() {
		logDebug("setPricing");

		DecimalFormat df = new DecimalFormat("0.00");

		if (myAccountInfo == null) {
			myAccountInfo = ((MegaApplication) ((Activity) context).getApplication()).getMyAccountInfo();
		}

		if (myAccountInfo != null) {
			ArrayList<Product> productAccounts = myAccountInfo.getProductAccounts();

			if (productAccounts == null) {
				logDebug("productAccounts == null");
				((MegaApplication) ((Activity) context).getApplication()).askForPricing();
				return;
			}

			for (int i = 0; i < productAccounts.size(); i++) {
				Product account = productAccounts.get(i);

				if (account.getMonths() == 1) {
					String textToShow = getPriceString(df, account, false);

					switch (account.getLevel()) {
						case PRO_I: {
							try{
								textToShow = textToShow.replace("[A]", "<font color=\'#ff333a\'>");
								textToShow = textToShow.replace("[/A]", "</font>");
							}catch (Exception e){
								logError("NullPointerException happens when getting the storage string", e);
							}

							monthSectionPro1.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat. FROM_HTML_MODE_LEGACY));
							storageSectionPro1.setText(generateByteString(account.getStorage(), TYPE_STORAGE_LABEL));
							bandwidthSectionPro1.setText(generateByteString(account.getTransfer(), TYPE_TRANSFER_LABEL));

							break;
						}
						case PRO_II: {
							try{
								textToShow = textToShow.replace("[A]", "<font color=\'#ff333a\'>");
								textToShow = textToShow.replace("[/A]", "</font>");
								textToShow = textToShow.replace("[B]", "<font color=\'#ff333a\'>");
								textToShow = textToShow.replace("[/B]", "</font>");
							}catch (Exception e){
								logError("NullPointerException happens when getting the storage string", e);
							}

							monthSectionPro2.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));
							storageSectionPro2.setText(generateByteString(account.getStorage(), TYPE_STORAGE_LABEL));
							bandwidthSectionPro2.setText(generateByteString(account.getTransfer(), TYPE_TRANSFER_LABEL));

							break;
						}
						case PRO_III: {
							try{
								textToShow = textToShow.replace("[A]", "<font color=\'#ff333a\'>");
								textToShow = textToShow.replace("[/A]", "</font>");
								textToShow = textToShow.replace("[B]", "<font color=\'#ff333a\'>");
								textToShow = textToShow.replace("[/B]", "</font>");
							}catch (Exception e){
								logError("NullPointerException happens when getting the storage string", e);
							}

							monthSectionPro3.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));
							storageSectionPro3.setText(generateByteString(account.getStorage(), TYPE_STORAGE_LABEL));
							bandwidthSectionPro3.setText(generateByteString(account.getTransfer(), TYPE_TRANSFER_LABEL));

							break;
						}
						case PRO_LITE: {
							try{
								textToShow = textToShow.replace("[A]", "<font color=\'#ffa500\'>");
								textToShow = textToShow.replace("[/A]", "</font>");
								textToShow = textToShow.replace("[B]", "<font color=\'#ff333a\'>");
								textToShow = textToShow.replace("[/B]", "</font>");
							}catch (Exception e){
								logError("NullPointerException happens when getting the storage string", e);
							}

							monthSectionLite.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));
							storageSectionLite.setText(generateByteString(account.getStorage(), TYPE_STORAGE_LABEL));
							bandwidthSectionLite.setText(generateByteString(account.getTransfer(), TYPE_TRANSFER_LABEL));

							break;
						}
						case BUSINESS: {
							textToShow = getPriceString(df, account, true);
                            // The initial amount of storage space for business account is 15TB
							String businessStorageSpace = getString(R.string.storage_space_amount, getSizeStringGBBased(BUSINESS_ACCOUNT_STORAGE_SPACE_AMOUNT));
							String businessTransferQuota = getString(R.string.unlimited_transfer_quota);

							try{
								textToShow = textToShow.replace("[A]", "<font color=\'#2ba6de\'>");
								textToShow = textToShow.replace("[/A]", "</font>");
								textToShow = textToShow.replace("[B]", "<font color=\'#2ba6de\'>");
								textToShow = textToShow.replace("[/B]", "</font>");
								businessStorageSpace = businessStorageSpace.replace("[A]", "<font color=\'#7a7a7a\'>");
								businessStorageSpace = businessStorageSpace.replace("[/A]", "</font>");
								businessTransferQuota = businessTransferQuota.replace("[A]", "<font color=\'#7a7a7a\'>");
								businessTransferQuota = businessTransferQuota.replace("[/A]", "</font>");
							}catch (Exception e){
								logError("NullPointerException happens when getting the storage string", e);
							}

							monthSectionBusiness.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));
							storageSectionBusiness.setText(HtmlCompat.fromHtml(businessStorageSpace, HtmlCompat.FROM_HTML_MODE_LEGACY));
							bandwidthSectionBusiness.setText(HtmlCompat.fromHtml(businessTransferQuota, HtmlCompat.FROM_HTML_MODE_LEGACY));

							break;
						}
					}
				}
			}

			int displayedAccountType = ((ManagerActivityLollipop) context).getDisplayedAccountType();
			logDebug("displayedAccountType: " + displayedAccountType);
			if (displayedAccountType != -1) {
				switch (displayedAccountType) {
					case PRO_LITE: {
						onUpgradeClick(PRO_LITE);
						break;
					}
					case PRO_I: {
						onUpgradeClick(PRO_I);
						break;
					}
					case PRO_II: {
						onUpgradeClick(PRO_II);
						break;
					}
					case PRO_III: {
						onUpgradeClick(PRO_III);
						break;
					}
				}
			}
		} else {
			logWarning("MyAccountInfo is Null");
		}
	}

	private String getPriceString(DecimalFormat df, Product account, boolean isBusiness){
		double price = account.getAmount() / 100.00;
		String priceString = df.format(price);
		String[] s = priceString.split("\\.");

		String monthPrice = "";
		if (s.length == 1) {
			String[] s1 = priceString.split(",");
			if (s1.length == 1) {
				monthPrice = s1[0]+" €";
			} else if (s1.length == 2) {
				monthPrice = s1[0]+","+s1[1]+" €";
			}
		}else if (s.length == 2) {
			monthPrice = s[0]+","+s[1]+" €";
		}

		if (isBusiness) {
			return getString(R.string.type_business_month, monthPrice);
		}

		return getString(R.string.type_month, monthPrice);
	}
	
	public void showAvailableAccount(){
		logDebug("showAvailableAccount()");

		if(myAccountInfo==null){
			logWarning("MyAccountInfo is NULL");
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}

		logDebug("showAvailableAccount: " + myAccountInfo.getAccountType());

		switch(myAccountInfo.getAccountType()){

			case PRO_I:{
				hideProI();
				break;
			}
			case PRO_II:{
				hideProII();
				break;
			}
			case PRO_III:{
				hideProIII();
				break;
			}
			case PRO_LITE:{
				hideProLite();
				break;
			}
		}
	}

	public void onUpgradeClick(int account){
		logDebug("account: " + account);
		RelativeLayout selectPaymentMethodClicked;

		switch (account){
			case PRO_LITE:{
				logDebug("PRO_LITE ");
				selectPaymentMethodClicked = (RelativeLayout) selectPaymentMethodLayoutLite;
				break;
			}
			case PRO_I:{
				selectPaymentMethodClicked = (RelativeLayout) selectPaymentMethodLayoutPro1;
				break;
			}
			case PRO_II:{
				selectPaymentMethodClicked = (RelativeLayout) selectPaymentMethodLayoutPro2;
				break;
			}
			case PRO_III:{
				selectPaymentMethodClicked = (RelativeLayout) selectPaymentMethodLayoutPro3;
				break;
			}
			default:{
				selectPaymentMethodClicked = (RelativeLayout) selectPaymentMethodLayoutLite;
				break;
			}
		}

		if (myAccountInfo.getPaymentBitSet() != null){
			logDebug("myAccountInfo.getPaymentBitSet() != null");

			selectPaymentMethod = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_text_payment_method);
			paymentTitle = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_text_payment_title);


//			RelativeLayout.LayoutParams titleParams = (RelativeLayout.LayoutParams) selectPaymentMethod.getLayoutParams();
//			titleParams.setMargins(0,scaleHeightPx(18, outMetrics),0,scaleHeightPx(14, outMetrics));
//			selectPaymentMethod.setLayoutParams(titleParams);

			switch (account){
				case PRO_LITE:{
					paymentTitle.setTextColor(ContextCompat.getColor(context, R.color.lite_account));
					paymentTitle.setText(getString(R.string.lite_account));
					break;
				}
				case PRO_I:{
					paymentTitle.setTextColor(ContextCompat.getColor(context, R.color.pro_account));
					paymentTitle.setText(getString(R.string.pro1_account));
					break;
				}
				case PRO_II:{
					paymentTitle.setTextColor(ContextCompat.getColor(context, R.color.pro_account));
					paymentTitle.setText(getString(R.string.pro2_account));
					break;
				}
				case PRO_III:{
					paymentTitle.setTextColor(ContextCompat.getColor(context, R.color.pro_account));
					paymentTitle.setText(getString(R.string.pro3_account));
					break;
				}
				default:{
					break;
				}
			}

			googlePlayLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_google_wallet);
			googlePlayLayout.setOnClickListener(this);

			googlePlayLayer = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_google_wallet_layer);
			googlePlayLayer.setVisibility(View.GONE);

			googleWalletText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_google_wallet_text);

            String textGoogleWallet = getString(R.string.payment_method_google_wallet);
            try{
                textGoogleWallet = textGoogleWallet.replace("[A]", "<font color=\'"
						+ ColorUtils.getColorHexString(context, R.color.black_white)
						+ "\'>");
                textGoogleWallet = textGoogleWallet.replace("[/A]", "</font>");
            }
            catch (Exception e){}

            googleWalletText.setText(HtmlCompat.fromHtml(textGoogleWallet, HtmlCompat.FROM_HTML_MODE_LEGACY));


			creditCardLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card);
			creditCardLayout.setOnClickListener(this);

			creditCardLayer = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card_layer);
			creditCardLayer.setVisibility(View.GONE);

			creditCardText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card_text);
			String textCreditCardText = getString(R.string.payment_method_credit_card);
			try{
				textCreditCardText = textCreditCardText.replace("[A]", "<font color=\'"
						+ ColorUtils.getColorHexString(context, R.color.black_white)
						+ "\'>");
				textCreditCardText = textCreditCardText.replace("[/A]", "</font>");
			}
			catch (Exception e){}

			creditCardText.setText(HtmlCompat.fromHtml(textCreditCardText, HtmlCompat.FROM_HTML_MODE_LEGACY));

			fortumoLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo);
			fortumoLayout.setOnClickListener(this);

			fortumoLayer = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo_layer);
			fortumoLayer.setVisibility(View.GONE);

			fortumoText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo_text);

			String textFortumoText = getString(R.string.payment_method_fortumo);
			try{
				textFortumoText = textFortumoText.replace("[A]", "<font color=\'"
						+ ColorUtils.getColorHexString(context, R.color.black_white)
						+ "\'>");
				textFortumoText = textFortumoText.replace("[/A]", "</font>");
			}
			catch (Exception e){}

			fortumoText.setText(HtmlCompat.fromHtml(textFortumoText, HtmlCompat.FROM_HTML_MODE_LEGACY));

			centiliLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_centili);
			centiliLayout.setOnClickListener(this);

			centiliLayer = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_centili_layer);
			centiliLayer.setVisibility(View.GONE);

			centiliText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_centili_text);

			String textCentiliText = getString(R.string.payment_method_centili);
			try{
				textCentiliText = textCentiliText.replace("[A]", "<font color=\'"
						+ ColorUtils.getColorHexString(context, R.color.black_white)
						+ "\'>");
				textCentiliText = textCentiliText.replace("[/A]", "</font>");
			}
			catch (Exception e){}

			centiliText.setText(HtmlCompat.fromHtml(textCentiliText, HtmlCompat.FROM_HTML_MODE_LEGACY));

			optionsBilling = (LinearLayout) selectPaymentMethodClicked.findViewById(R.id.options);

			billingPeriod = (RadioGroup) selectPaymentMethodClicked.findViewById(R.id.billing_period);
			billedMonthly = (RadioButton) selectPaymentMethodClicked.findViewById(R.id.billed_monthly);
			billedMonthly.setOnClickListener(this);
			billedYearly = (RadioButton) selectPaymentMethodClicked.findViewById(R.id.billed_yearly);
			billedYearly.setOnClickListener(this);

			layoutButtons = (LinearLayout) selectPaymentMethodClicked.findViewById(R.id.layout_buttons);
			buttonCancel = (TextView) selectPaymentMethodClicked.findViewById(R.id.button_cancel);
			buttonCancel.setOnClickListener(this);
            buttonContinue = (TextView) selectPaymentMethodClicked.findViewById(R.id.button_continue);
            buttonContinue.setOnClickListener(this);

            buttonContinue.setEnabled(false);
			buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.invite_button_deactivated)));

			googlePlayLayout.setVisibility(View.GONE);
			creditCardLayout.setVisibility(View.GONE);
			fortumoLayout.setVisibility(View.GONE);
			centiliLayout.setVisibility(View.GONE);
            layoutButtons.setVisibility(View.GONE);
			optionsBilling.setVisibility(View.GONE);

			showPaymentMethods(account);

			refreshAccountInfo();
			logDebug("END refreshAccountInfo");
			if (!myAccountInfo.isInventoryFinished()){
				logDebug("if (!myAccountInfo.isInventoryFinished())");
				googlePlayLayout.setVisibility(View.GONE);
			}
			logDebug("Just before show the layout");

			selectPaymentMethodClicked.setVisibility(View.VISIBLE);
			semitransparentLayer.setVisibility(View.VISIBLE);

			switch (account){
				case PRO_I:{

					new Handler().post(new Runnable() {
						@Override
						public void run() {
							logDebug("smeasure: " + pro2Layout.getTop());
							logDebug("scroll to: " + pro2Layout.getBottom());
							scrollView.smoothScrollTo(0, pro1Layout.getTop());

						}
					});
					break;
				}
				case PRO_II:{
					new Handler().post(new Runnable() {
						@Override
						public void run() {
							scrollView.smoothScrollTo(0, pro3Layout.getBottom());
						}
					});
					break;
				}
				case PRO_III:{
					new Handler().post(new Runnable() {
						@Override
						public void run() {
							scrollView.smoothScrollTo(0, pro3Layout.getBottom());
						}
					});
					break;
				}
			}
		}
		else{
			logWarning("PaymentBitSet Null");
		}
	}

	private void hideProLite(){
		logDebug("hideProLite");
		proLiteTransparentLayout.setVisibility(View.VISIBLE);
	}

	private void hideProI(){
		logDebug("hideProI");
		pro1TransparentLayout.setVisibility(View.VISIBLE);
	}

	private void hideProII(){
		logDebug("hideProII");
		pro2TransparentLayout.setVisibility(View.VISIBLE);
	}

	private void hideProIII(){
		logDebug("hideProIII");
		pro3TransparentLayout.setVisibility(View.VISIBLE);
		labelCustomPlan.setVisibility(View.VISIBLE);
	}

	private Spanned generateByteString(long gb, int labelType) {
		String textToShow = new StringBuilder().append("[A] ")
											   .append(getSizeStringGBBased(gb))
											   .append(" [/A] ")
											   .append(storageOrTransferLabel(labelType))
											   .toString();

		try {
			textToShow = textToShow.replace("[A]", "<font color=\'"
					+ ColorUtils.getColorHexString(context, R.color.black_white)
					+ "\'>");
			textToShow = textToShow.replace("[/A]", "</font>");
		} catch (NullPointerException ex) {
			logError("NullPointerException happens when getting the storage string", ex);
		}

		return HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY);
	}

	private String storageOrTransferLabel(int labelType) {
		switch (labelType) {
			case TYPE_STORAGE_LABEL:
				return getString(R.string.label_storage_upgrade_account);
			case TYPE_TRANSFER_LABEL:
				return getString(R.string.label_transfer_quota_upgrade_account);
			default:
				return "";
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
	}
	
	public void showNextPaymentFragment(int paymentM){
		logDebug("paymentM: " + paymentM);

		if(selectPaymentMethodLayoutLite.getVisibility()==View.VISIBLE){
			parameterType=PRO_LITE;
		}
		else if(selectPaymentMethodLayoutPro1.getVisibility()==View.VISIBLE){
			parameterType=PRO_I;
		}
		else if(selectPaymentMethodLayoutPro2.getVisibility()==View.VISIBLE){
			parameterType=PRO_II;
		}
		else if(selectPaymentMethodLayoutPro3.getVisibility()==View.VISIBLE){
			parameterType=PRO_III;
		}
		else{
			parameterType=0;
		}
		paymentMethod = paymentM;
		logDebug("parameterType: " + parameterType);

		((ManagerActivityLollipop)context).setSelectedAccountType(parameterType);
		((ManagerActivityLollipop)context).setSelectedPaymentMethod(paymentMethod);
		showmyF(paymentM, parameterType);

//		((ManagerActivityLollipop)context).showmyF(paymentMethod, parameterType);
	}

	private void contactForCustomPlan() {
		logDebug("Send Feedback");
        ((ManagerActivityLollipop) context).askForCustomizedPlan();
	}

	@Override
	public void onClick(View v) {
		logDebug("onClick");

		((ManagerActivityLollipop)context).setDisplayedAccountType(-1);
		switch (v.getId()){
			case R.id.lbl_custom_plan: {
                contactForCustomPlan();
				break;
			}
            case R.id.button_continue:{
				logDebug("Button button_continue pressed");
				if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
					//MONTHLY SUBSCRIPTION
					switch (parameterType) {
						case 1: {
							//PRO I
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_MONTH, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_I_MONTH);
									break;
								}
							}
							break;
						}
						case 2: {
							//PRO II
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_MONTH, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_II_MONTH);
									break;
								}
							}
							break;
						}
						case 3: {
							//PRO III
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_MONTH, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_III_MONTH);
									break;
								}
							}
							break;
						}
						case 4: {
							//LITE
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_MONTH, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_LITE_MONTH);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_FORTUMO: {
									((ManagerActivityLollipop) context).showFortumo();
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_CENTILI: {
									((ManagerActivityLollipop) context).showCentili();
									break;
								}
							}
							break;
						}
					}
				} else {
					//YEARLY SUBSCRIPTION
					switch (parameterType) {
						case 1: {
							//PRO I
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_YEAR, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_I_YEAR);
									break;
								}
							}
							break;
						}
						case 2: {
							//PRO II
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_YEAR, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_II_YEAR);
									break;
								}
							}
							break;
						}
						case 3: {
							//PRO III
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_YEAR, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_III_YEAR);
									break;
								}
							}
							break;
						}
						case 4: {
							//LITE
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_YEAR, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_LITE_YEAR);
									break;
								}
							}
							break;
						}
					}
				}

                break;
            }
			case R.id.button_cancel:{
				logDebug("button_cancel");
				semitransparentLayer.setVisibility(View.GONE);
				selectPaymentMethodLayoutLite.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
				break;
			}
			case R.id.semitransparent_layer:{
				logDebug("semitransparent_layer");
				semitransparentLayer.setVisibility(View.GONE);
				selectPaymentMethodLayoutLite.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
				break;
			}
			case R.id.upgrade_prolite_layout:{
				if(selectPaymentMethodLayoutLite.getVisibility()==View.VISIBLE){
					selectPaymentMethodLayoutLite.setVisibility(View.GONE);
					semitransparentLayer.setVisibility(View.GONE);
				}else{
					selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
					onUpgradeClick(PRO_LITE);
				}
				break;
			}
			case R.id.upgrade_pro_i_layout:{
				if (selectPaymentMethodLayoutPro1.getVisibility() == View.VISIBLE) {
					selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
				} else {
					selectPaymentMethodLayoutLite.setVisibility(View.GONE);
					semitransparentLayer.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
					onUpgradeClick(PRO_I);
				}
				break;
			}
			case R.id.upgrade_pro_ii_layout:{
				if (selectPaymentMethodLayoutPro2.getVisibility() == View.VISIBLE) {
					selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
				} else {
					selectPaymentMethodLayoutLite.setVisibility(View.GONE);
					semitransparentLayer.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
					onUpgradeClick(PRO_II);
				}
				break;
			}
			case R.id.upgrade_pro_iii_layout:{
				if (selectPaymentMethodLayoutPro3.getVisibility() == View.VISIBLE) {
					selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
				} else {
					selectPaymentMethodLayoutLite.setVisibility(View.GONE);
					semitransparentLayer.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
					onUpgradeClick(PRO_III);
				}
				break;
			}
			case R.id.payment_method_google_wallet:{
				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET);
				break;
			}
			case R.id.payment_method_credit_card:{
				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD);
				break;
			}
			case R.id.payment_method_fortumo:{
				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_FORTUMO);
				break;
			}
			case R.id.payment_method_centili:{
				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_CENTILI);
				break;
			}
			case R.id.upgrade_business_layout:{
				megaApi.getSessionTransferURL(REGISTER_BUSINESS_ACCOUNT, new SessionTransferURLListener(context));
				break;
			}
		}
	}

	public void showPaymentMethods(int parameterType){
		logDebug("parameterType: " + parameterType);

		if(myAccountInfo==null){
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}

		if(myAccountInfo==null){
			return;
		}

		ArrayList<Product> accounts = myAccountInfo.getProductAccounts();

		if (accounts == null){
			logWarning("accounts == null");
			((MegaApplication) ((Activity)context).getApplication()).askForPricing();
			return;
		}

		switch(parameterType){
			case PRO_I:{
				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						logDebug("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
                        if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)) {
                            googlePlayLayout.setVisibility(View.VISIBLE);
                            layoutButtons.setVisibility(View.VISIBLE);
                        }
					}

					if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);

					if(!isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}
				else{
					logWarning("Not payment bit set received!!!");
					selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					googlePlayLayout.setVisibility(View.GONE);
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);
				}

				break;
			}
			case PRO_II:{

				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						logDebug("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
                        if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)) {
                            googlePlayLayout.setVisibility(View.VISIBLE);
                            layoutButtons.setVisibility(View.VISIBLE);
                        }
					}

					if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);

					if(!isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}
				else{
					logWarning("Not payment bit set received!!!");
				}

				break;
			}
			case PRO_III:{

				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						logDebug("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
                        if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)) {
                            googlePlayLayout.setVisibility(View.VISIBLE);
                            layoutButtons.setVisibility(View.VISIBLE);
                        }
					}

					if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);

					if(!isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}

				break;
			}
			case PRO_LITE:{

				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						logDebug("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else {
                        if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)) {
                            googlePlayLayout.setVisibility(View.VISIBLE);
                            layoutButtons.setVisibility(View.VISIBLE);
                        }
					}

					if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
						fortumoLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
						centiliLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }

					if(!isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}
				break;
			}
		}
	}


	public void setAccountDetails() {
		logDebug("setAccountDetails");

		if ((getActivity() == null) || (!isAdded())) {
			logWarning("Fragment MyAccount NOT Attached!");
			return;
		}

		if (myAccountInfo == null) {
			myAccountInfo = ((MegaApplication) ((Activity) context).getApplication()).getMyAccountInfo();
		}

		if (myAccountInfo == null) {
			return;
		}
		//Set account details
		if (myAccountInfo.getAccountType() < 0 || myAccountInfo.getAccountType() > 4) {
			textMyAccount.setText(getString(R.string.recovering_info));
			textMyAccount.setTextColor(ContextCompat.getColor(context,R.color.grey_054_white_054));
		} else {
			switch (myAccountInfo.getAccountType()) {

				case 0: {
					String textToShowB = getString(R.string.type_of_my_account, getString(R.string.free_account));
					try{
						textToShowB = textToShowB.replace("[A]", "<font color=\'#2bb200\'>");
						textToShowB = textToShowB.replace("[/A]", "</font>");
					}
					catch (Exception e){}

					textMyAccount.setText(HtmlCompat.fromHtml(textToShowB, HtmlCompat.FROM_HTML_MODE_LEGACY));
					break;
				}

				case 1: {
					String textToShowB = getString(R.string.type_of_my_account, getString(R.string.pro1_account));
					try{
						textToShowB = textToShowB.replace("[A]", "<font color=\'#ff333a\'>");
						textToShowB = textToShowB.replace("[/A]", "</font>");
					}
					catch (Exception e){}

					textMyAccount.setText(HtmlCompat.fromHtml(textToShowB, HtmlCompat.FROM_HTML_MODE_LEGACY));
					break;
				}

				case 2: {
					String textToShowB = getString(R.string.type_of_my_account, getString(R.string.pro2_account));
					try{
						textToShowB = textToShowB.replace("[A]", "<font color=\'#ff333a\'>");
						textToShowB = textToShowB.replace("[/A]", "</font>");
					}
					catch (Exception e){}

					textMyAccount.setText(HtmlCompat.fromHtml(textToShowB, HtmlCompat.FROM_HTML_MODE_LEGACY));
					break;
				}

				case 3: {
					String textToShowB = getString(R.string.type_of_my_account, getString(R.string.pro3_account));
					try{
						textToShowB = textToShowB.replace("[A]", "<font color=\'#ff333a\'>");
						textToShowB = textToShowB.replace("[/A]", "</font>");
					}
					catch (Exception e){}

					textMyAccount.setText(HtmlCompat.fromHtml(textToShowB, HtmlCompat.FROM_HTML_MODE_LEGACY));
					break;
				}

				case 4: {
					String textToShowB = getString(R.string.type_of_my_account, getString(R.string.lite_account));
					try{
						textToShowB = textToShowB.replace("[A]", "<font color=\'#ffa500\'>");
						textToShowB = textToShowB.replace("[/A]", "</font>");
					}
					catch (Exception e){}

					textMyAccount.setText(HtmlCompat.fromHtml(textToShowB, HtmlCompat.FROM_HTML_MODE_LEGACY));
					break;
				}

			}
		}

	}

	public void showmyF(int paymentMethod, int parameterType){
		logDebug("paymentMethod " + paymentMethod + ", type " + parameterType);

		String priceMonthlyInteger = "";
		String priceMonthlyDecimal = "";
		String priceYearlyInteger = "";
		String priceYearlyDecimal = "";

		DecimalFormat df = new DecimalFormat("0.00");

		if(myAccountInfo==null){
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}

		if(myAccountInfo == null){
			return;
		}

		ArrayList<Product> accounts = myAccountInfo.getProductAccounts();

		if (accounts == null){
			logWarning("accounts == null");
			((MegaApplication) ((Activity)context).getApplication()).askForPricing();
			return;
		}

		switch(parameterType){
			case 1:{
				logDebug("case PRO I");

				for (int i=0;i<accounts.size();i++){

					Product account = accounts.get(i);

					if(account.getLevel()==1 && account.getMonths()==1){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceMonthlyInteger = s1[0];
								priceMonthlyDecimal = "";
							}
							else if (s1.length == 2){
								priceMonthlyInteger = s1[0];
								priceMonthlyDecimal = "." + s1[1] + " €";
							}
						}
						else if (s.length == 2){
							priceMonthlyInteger = s[0];
							priceMonthlyDecimal = "." + s[1] + " €";
						}
						String priceMonthly = priceMonthlyInteger+priceMonthlyDecimal;

						String textToShowMonthly = getString(R.string.billed_monthly_text, priceMonthly);
						try{
							textToShowMonthly = textToShowMonthly.replace("[A]", "<font color=\'#000000\'>");
							textToShowMonthly = textToShowMonthly.replace("[/A]", "</font>");
						}
						catch (Exception e){}

						billedMonthly.setText(HtmlCompat.fromHtml(textToShowMonthly, HtmlCompat.FROM_HTML_MODE_LEGACY));
					}
					if (account.getLevel()==1 && account.getMonths()==12){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceYearlyInteger = s1[0];
								priceYearlyDecimal = "";
							}
							else if (s1.length == 2){
								priceYearlyInteger = s1[0];
								priceYearlyDecimal = "." + s1[1] + " €";
							}
						}
						else if (s.length == 2){
							priceYearlyInteger = s[0];
							priceYearlyDecimal = "." + s[1] + " €";
						}

						String priceYearly = priceYearlyInteger+priceYearlyDecimal;

						String textToShowYearly = getString(R.string.billed_yearly_text, priceYearly);
						try{
							textToShowYearly = textToShowYearly.replace("[A]", "<font color=\'#000000\'>");
							textToShowYearly = textToShowYearly.replace("[/A]", "</font>");
						}
						catch (Exception e){}

						billedYearly.setText(HtmlCompat.fromHtml(textToShowYearly, HtmlCompat.FROM_HTML_MODE_LEGACY));
					}
				}

				switch (paymentMethod){
					case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
						logDebug("Pro I - PAYMENT_METHOD_FORTUMO");
						creditCardLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						fortumoLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
						logDebug("Pro I - PAYMENT_METHOD_CENTILI");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
						logDebug("Pro I - PAYMENT_METHOD_CREDIT_CARD");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
						logDebug("Pro I - PAYMENT_METHOD_GOOGLE_WALLET");
						fortumoLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);

						if (myAccountInfo.isPurchasedAlready(SKU_PRO_I_MONTH)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
								billedYearly.setChecked(true);
							}
							billedMonthly.setVisibility(View.GONE);
						}

						if (myAccountInfo.isPurchasedAlready(SKU_PRO_I_YEAR)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_yearly){
								billedMonthly.setChecked(true);
							}
							billedYearly.setVisibility(View.GONE);
						}
						break;
					}
				}

				break;
			}
			case 2:{
				logDebug(" case PRO II");

				for (int i=0;i<accounts.size();i++){

					Product account = accounts.get(i);

					if(account.getLevel()==2 && account.getMonths()==1){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceMonthlyInteger = s1[0];
								priceMonthlyDecimal = "";
							}
							else if (s1.length == 2){
								priceMonthlyInteger = s1[0];
								priceMonthlyDecimal = "." + s1[1] + " €";
							}
						}
						else if (s.length == 2){
							priceMonthlyInteger = s[0];
							priceMonthlyDecimal = "." + s[1] + " €";
						}

						String priceMonthly = priceMonthlyInteger+priceMonthlyDecimal;

						String textToShowMonthly = getString(R.string.billed_monthly_text, priceMonthly);
						try{
							textToShowMonthly = textToShowMonthly.replace("[A]", "<font color=\'#000000\'>");
							textToShowMonthly = textToShowMonthly.replace("[/A]", "</font>");
						}
						catch (Exception e){}

						billedMonthly.setText(HtmlCompat.fromHtml(textToShowMonthly, HtmlCompat.FROM_HTML_MODE_LEGACY));
					}
					if (account.getLevel()==2 && account.getMonths()==12){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceYearlyInteger = s1[0];
								priceYearlyDecimal = "";
							}
							else if (s1.length == 2){
								priceYearlyInteger = s1[0];
								priceYearlyDecimal = "." + s1[1] + " €";
							}
						}
						else if (s.length == 2){
							priceYearlyInteger = s[0];
							priceYearlyDecimal = "." + s[1] + " €";
						}
						String priceYearly = priceYearlyInteger+priceYearlyDecimal;

						String textToShowYearly = getString(R.string.billed_yearly_text, priceYearly);
						try{
							textToShowYearly = textToShowYearly.replace("[A]", "<font color=\'#000000\'>");
							textToShowYearly = textToShowYearly.replace("[/A]", "</font>");
						}
						catch (Exception e){}

						billedYearly.setText(HtmlCompat.fromHtml(textToShowYearly, HtmlCompat.FROM_HTML_MODE_LEGACY));

					}
				}

				switch (paymentMethod){
					case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
						logDebug("Pro II - PAYMENT_METHOD_FORTUMO");
						creditCardLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						fortumoLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
						logDebug("Pro II - PAYMENT_METHOD_CENTILI");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
						logDebug("Pro II - PAYMENT_METHOD_CREDIT_CARD");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
						logDebug("Pro II - PAYMENT_METHOD_GOOGLE_WALLET");
						fortumoLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);

						if (myAccountInfo.isPurchasedAlready(SKU_PRO_II_MONTH)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
								billedYearly.setChecked(true);
							}
							billedMonthly.setVisibility(View.GONE);
						}

						if (myAccountInfo.isPurchasedAlready(SKU_PRO_II_YEAR)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_yearly){
								billedMonthly.setChecked(true);
							}
							billedYearly.setVisibility(View.GONE);
						}
						break;
					}
				}

				break;
			}
			case 3:{
				logDebug("case PRO III");

				for (int i=0;i<accounts.size();i++){

					Product account = accounts.get(i);

					if(account.getLevel()==3 && account.getMonths()==1){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceMonthlyInteger = s1[0];
								priceMonthlyDecimal = "";
							}
							else if (s1.length == 2){
								priceMonthlyInteger = s1[0];
								priceMonthlyDecimal = "." + s1[1] + " €";
							}
						}
						else if (s.length == 2){
							priceMonthlyInteger = s[0];
							priceMonthlyDecimal = "." + s[1] + " €";
						}

						String priceMonthly = priceMonthlyInteger+priceMonthlyDecimal;

						String textToShowMonthly = getString(R.string.billed_monthly_text, priceMonthly);
						try{
							textToShowMonthly = textToShowMonthly.replace("[A]", "<font color=\'#000000\'>");
							textToShowMonthly = textToShowMonthly.replace("[/A]", "</font>");
						}
						catch (Exception e){}

						billedMonthly.setText(HtmlCompat.fromHtml(textToShowMonthly, HtmlCompat.FROM_HTML_MODE_LEGACY));
					}
					if (account.getLevel()==3 && account.getMonths()==12){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceYearlyInteger = s1[0];
								priceYearlyDecimal = "";
							}
							else if (s1.length == 2){
								priceYearlyInteger = s1[0];
								priceYearlyDecimal = "." + s1[1] + " €";
							}
						}
						else if (s.length == 2){
							priceYearlyInteger = s[0];
							priceYearlyDecimal = "." + s[1] + " €";
						}

						String priceYearly = priceYearlyInteger+priceYearlyDecimal;

						String textToShowYearly = getString(R.string.billed_yearly_text, priceYearly);
						try{
							textToShowYearly = textToShowYearly.replace("[A]", "<font color=\'#000000\'>");
							textToShowYearly = textToShowYearly.replace("[/A]", "</font>");
						}
						catch (Exception e){}

						billedYearly.setText(HtmlCompat.fromHtml(textToShowYearly, HtmlCompat.FROM_HTML_MODE_LEGACY));

					}
				}

				switch (paymentMethod){
					case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
						logDebug("Pro III - PAYMENT_METHOD_FORTUMO");
						creditCardLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						fortumoLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
						logDebug("Pro III - PAYMENT_METHOD_CENTILI");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
						logDebug("Pro III - PAYMENT_METHOD_CREDIT_CARD");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
						logDebug("Pro III - PAYMENT_METHOD_GOOGLE_WALLET");
						fortumoLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						if (myAccountInfo.isPurchasedAlready(SKU_PRO_III_MONTH)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
								billedYearly.setChecked(true);
							}
							billedMonthly.setVisibility(View.GONE);
						}

						if (myAccountInfo.isPurchasedAlready(SKU_PRO_III_YEAR)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_yearly){
								billedMonthly.setChecked(true);
							}
							billedYearly.setVisibility(View.GONE);
						}
						break;
					}
				}

				break;
			}
			case 4:{
				logDebug("case LITE");
				for (int i=0;i<accounts.size();i++){

					Product account = accounts.get(i);

					if(account.getLevel()==4 && account.getMonths()==1){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceMonthlyInteger = s1[0];
								priceMonthlyDecimal = "";
							}else if (s1.length == 2){
								priceMonthlyInteger = s1[0];
								priceMonthlyDecimal = "," + s1[1] + " €";
							}
						}else if (s.length == 2){
							priceMonthlyInteger = s[0];
							priceMonthlyDecimal = "," + s[1] + " €";
						}

						String priceMonthly = priceMonthlyInteger+priceMonthlyDecimal;

						String textToShowMonthly = paymentMethod == MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET ?
								getString(R.string.billed_monthly_text, priceMonthly) : getString(R.string.billed_one_off_month, priceMonthly);
						try{
							textToShowMonthly = textToShowMonthly.replace("[A]", "<font color=\'#000000\'>");
							textToShowMonthly = textToShowMonthly.replace("[/A]", "</font>");
						}
						catch (Exception e){}

						billedMonthly.setText(HtmlCompat.fromHtml(textToShowMonthly, HtmlCompat.FROM_HTML_MODE_LEGACY));

					}
					if (account.getLevel()==4 && account.getMonths()==12){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceYearlyInteger = s1[0];
								priceYearlyDecimal = "";
							}
							else if (s1.length == 2){
								priceYearlyInteger = s1[0];
								priceYearlyDecimal = "," + s1[1] + " €";
							}
						}
						else if (s.length == 2){
							priceYearlyInteger = s[0];
							priceYearlyDecimal = "," + s[1] + " €";
						}

						String priceYearly = priceYearlyInteger+priceYearlyDecimal;

						String textToShowYearly = paymentMethod == MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET ?
								getString(R.string.billed_yearly_text, priceYearly) : getString(R.string.billed_one_off_year, priceYearly);
						try{
							textToShowYearly = textToShowYearly.replace("[A]", "<font color=\'#000000\'>");
							textToShowYearly = textToShowYearly.replace("[/A]", "</font>");
						}
						catch (Exception e){}

						billedYearly.setText(HtmlCompat.fromHtml(textToShowYearly, HtmlCompat.FROM_HTML_MODE_LEGACY));
					}
				}

				switch (paymentMethod){
					case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
						logDebug("Lite - PAYMENT_METHOD_FORTUMO");
						creditCardLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						fortumoLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedMonthly.setChecked(true);
						billedYearly.setVisibility(View.GONE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
						logDebug("Lite - PAYMENT_METHOD_CENTILI");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedMonthly.setChecked(true);
						billedYearly.setVisibility(View.GONE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
						logDebug("Lite - PAYMENT_METHOD_CREDIT_CARD");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						billedYearly.setChecked(true);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
						logDebug("Lite - PAYMENT_METHOD_GOOGLE_WALLET");
						fortumoLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);

						if (myAccountInfo.isPurchasedAlready(SKU_PRO_LITE_MONTH)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
								billedYearly.setChecked(true);
							}
							billedMonthly.setVisibility(View.GONE);
						}
						if (myAccountInfo.isPurchasedAlready(SKU_PRO_LITE_YEAR)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_yearly){
								billedMonthly.setChecked(true);
							}
							billedYearly.setVisibility(View.GONE);
						}
						break;
					}
				}
				break;
			}
		}
	}
}
