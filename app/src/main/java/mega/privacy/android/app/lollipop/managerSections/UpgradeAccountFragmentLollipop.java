package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.DBUtil;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class UpgradeAccountFragmentLollipop extends Fragment implements OnClickListener{

	static int HEIGHT_ACCOUNT_LAYOUT=109;

	static int HEIGHT_PAYMENT_METHODS_LAYOUT=80;

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

	private final static int TYPE_TERA_BYTE = 0;
	private final static int TYPE_GIGA_BYTE = 1;

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
		LogUtil.logDebug("onCreate");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LogUtil.logDebug("onCreateView");

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

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

//		//Replace elevation
//		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//			scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.grid_item_separator));
//			linearLayoutMain.setBackgroundColor(ContextCompat.getColor(context, R.color.grid_item_separator));
//		}

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
		//END -- PRO III ACCOUNT

		setPricing();
		LogUtil.logDebug("setPricing ENDS");
		showAvailableAccount();

		refreshAccountInfo();

		int displayedAccountType = ((ManagerActivityLollipop)context).getDisplayedAccountType();
		LogUtil.logDebug("displayedAccountType: " + displayedAccountType);
		if(displayedAccountType!=-1){
			switch(displayedAccountType){
				case Constants.PRO_LITE:{
					onUpgradeClick(Constants.PRO_LITE);
					break;
				}
				case Constants.PRO_I:{
					onUpgradeClick(Constants.PRO_I);
					break;
				}
				case Constants.PRO_II:{
					onUpgradeClick(Constants.PRO_II);
					break;
				}
				case Constants.PRO_III:{
					onUpgradeClick(Constants.PRO_III);
					break;
				}
			}
		}

		LogUtil.logDebug("END onCreateView");
		return v;
	}

	public void refreshAccountInfo(){
		LogUtil.logDebug("refreshAccountInfo");

		LogUtil.logDebug("Check the last call to callToPricing");
		if(DBUtil.callToPricing(context)){
			LogUtil.logDebug("megaApi.getPricing SEND");
			((MegaApplication) ((Activity)context).getApplication()).askForPricing();
		}

		LogUtil.logDebug("Check the last call to callToPaymentMethods");
		if(DBUtil.callToPaymentMethods(context)){
			LogUtil.logDebug("megaApi.getPaymentMethods SEND");
			((MegaApplication) ((Activity)context).getApplication()).askForPaymentMethods();
		}
	}

	public void setPricing() {
		LogUtil.logDebug("setPricing");

		DecimalFormat df = new DecimalFormat("#.##");

		if (myAccountInfo == null) {
			myAccountInfo = ((MegaApplication) ((Activity) context).getApplication()).getMyAccountInfo();
		}

		if (myAccountInfo != null) {
			ArrayList<Product> productAccounts = myAccountInfo.getProductAccounts();

			if (productAccounts == null) {
				LogUtil.logDebug("productAccounts == null");
				((MegaApplication) ((Activity) context).getApplication()).askForPricing();
				return;
			}

			for (int i = 0; i < productAccounts.size(); i++) {
				Product account = productAccounts.get(i);
				if (account.getLevel() == Constants.PRO_I && account.getMonths() == 1) {
					LogUtil.logDebug("PRO1: " + account.getStorage());

					double price = account.getAmount() / 100.00;
					String priceString = df.format(price);
					String[] s = priceString.split("\\.");

					String textMonth = "";
					if (s.length == 1) {
						String[] s1 = priceString.split(",");
						if (s1.length == 1) {
							textMonth = s1[0];
						} else if (s1.length == 2) {
							textMonth = s1[0]+","+s1[1]+" €";
						}
					}else if (s.length == 2) {
						textMonth = s[0]+","+s[1]+" €";
					}

					String textToShowA = getString(R.string.type_month, textMonth);
					try{
						textToShowA = textToShowA.replace("[A]", "<font color=\'#ff333a\'>");
						textToShowA = textToShowA.replace("[/A]", "</font>");
					}catch (Exception e){}
					Spanned resultA = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultA = Html.fromHtml(textToShowA,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultA = Html.fromHtml(textToShowA);
					}
					monthSectionPro1.setText(resultA);

					storageSectionPro1.setText(generateByteString(account.getStorage(), TYPE_TERA_BYTE, TYPE_STORAGE_LABEL));

					bandwidthSectionPro1.setText(generateByteString(account.getTransfer(), TYPE_TERA_BYTE, TYPE_TRANSFER_LABEL));

				} else if (account.getLevel() == Constants.PRO_II && account.getMonths() == 1) {
					LogUtil.logDebug("PRO2: " + account.getStorage());

					double price = account.getAmount() / 100.00;
					String priceString = df.format(price);
					String[] s = priceString.split("\\.");

					String textMonth = "";
					if (s.length == 1) {
						String[] s1 = priceString.split(",");
						if (s1.length == 1) {
							textMonth = s1[0];
						} else if (s1.length == 2) {
							textMonth = s1[0]+","+s1[1]+" €";
						}
					}else if (s.length == 2) {
						textMonth = s[0]+","+s[1]+" €";
					}

					String textToShowA = getString(R.string.type_month, textMonth);
					try{
						textToShowA = textToShowA.replace("[A]", "<font color=\'#ff333a\'>");
						textToShowA = textToShowA.replace("[/A]", "</font>");
						textToShowA = textToShowA.replace("[B]", "<font color=\'#ff333a\'>");
						textToShowA = textToShowA.replace("[/B]", "</font>");
					}catch (Exception e){}
					Spanned resultA = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultA = Html.fromHtml(textToShowA,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultA = Html.fromHtml(textToShowA);
					}
					monthSectionPro2.setText(resultA);

					storageSectionPro2.setText(generateByteString(account.getStorage(), TYPE_TERA_BYTE, TYPE_STORAGE_LABEL));


					bandwidthSectionPro2.setText(generateByteString(account.getTransfer(), TYPE_TERA_BYTE, TYPE_TRANSFER_LABEL));

				} else if (account.getLevel() == Constants.PRO_III && account.getMonths() == 1) {
					LogUtil.logDebug("PRO3: " + account.getStorage());

					double price = account.getAmount() / 100.00;
					String priceString = df.format(price);
					String[] s = priceString.split("\\.");

					String textMonth = "";
					if (s.length == 1) {
						String[] s1 = priceString.split(",");
						if (s1.length == 1) {
							textMonth = s1[0];
						} else if (s1.length == 2) {
							textMonth = s1[0]+","+s1[1]+" €";
						}
					}else if (s.length == 2) {
						textMonth = s[0]+","+s[1]+" €";
					}

					String textToShowA = getString(R.string.type_month, textMonth);
					try{
						textToShowA = textToShowA.replace("[A]", "<font color=\'#ff333a\'>");
						textToShowA = textToShowA.replace("[/A]", "</font>");
						textToShowA = textToShowA.replace("[B]", "<font color=\'#ff333a\'>");
						textToShowA = textToShowA.replace("[/B]", "</font>");
					}catch (Exception e){}
					Spanned resultA = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultA = Html.fromHtml(textToShowA,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultA = Html.fromHtml(textToShowA);
					}
					monthSectionPro3.setText(resultA);

					storageSectionPro3.setText(generateByteString(account.getStorage(), TYPE_TERA_BYTE, TYPE_STORAGE_LABEL));

					bandwidthSectionPro3.setText(generateByteString(account.getTransfer(), TYPE_TERA_BYTE, TYPE_TRANSFER_LABEL));

				} else if (account.getLevel() == Constants.PRO_LITE && account.getMonths() == 1) {
					LogUtil.logDebug("Lite: " + account.getStorage());

					double price = account.getAmount() / 100.00;
					String priceString = df.format(price);
					String[] s = priceString.split("\\.");
					String textMonth = "";
					if (s.length == 1) {
						String[] s1 = priceString.split(",");
						if (s1.length == 1) {
							textMonth = s1[0];
						} else if (s1.length == 2) {
							textMonth = s1[0]+","+s1[1]+" €";
						}
					}else if (s.length == 2) {
						textMonth = s[0]+","+s[1]+" €";
					}

					String textToShowA = getString(R.string.type_month, textMonth);
					try{
						textToShowA = textToShowA.replace("[A]", "<font color=\'#ffa500\'>");
						textToShowA = textToShowA.replace("[/A]", "</font>");
						textToShowA = textToShowA.replace("[B]", "<font color=\'#ff333a\'>");
						textToShowA = textToShowA.replace("[/B]", "</font>");
					}catch (Exception e){}
					Spanned resultA = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultA = Html.fromHtml(textToShowA,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultA = Html.fromHtml(textToShowA);
					}
					monthSectionLite.setText(resultA);

					storageSectionLite.setText(generateByteString(account.getStorage(), TYPE_GIGA_BYTE, TYPE_STORAGE_LABEL));

					bandwidthSectionLite.setText(generateByteString(account.getTransfer(), TYPE_TERA_BYTE, TYPE_TRANSFER_LABEL));

				}
			}

			int displayedAccountType = ((ManagerActivityLollipop) context).getDisplayedAccountType();
			LogUtil.logDebug("displayedAccountType: " + displayedAccountType);
			if (displayedAccountType != -1) {
				switch (displayedAccountType) {
					case Constants.PRO_LITE: {
						onUpgradeClick(Constants.PRO_LITE);
						break;
					}
					case Constants.PRO_I: {
						onUpgradeClick(Constants.PRO_I);
						break;
					}
					case Constants.PRO_II: {
						onUpgradeClick(Constants.PRO_II);
						break;
					}
					case Constants.PRO_III: {
						onUpgradeClick(Constants.PRO_III);
						break;
					}
				}
			}
		} else {
			LogUtil.logWarning("MyAccountInfo is Null");
		}
	}
	
	public void showAvailableAccount(){
		LogUtil.logDebug("showAvailableAccount()");

		if(myAccountInfo==null){
			LogUtil.logWarning("MyAccountInfo is NULL");
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}

		LogUtil.logDebug("showAvailableAccount: " + myAccountInfo.getAccountType());

		switch(myAccountInfo.getAccountType()){

			case Constants.PRO_I:{
				hideProI();
				break;
			}
			case Constants.PRO_II:{
				hideProII();
				break;
			}
			case Constants.PRO_III:{
				hideProIII();
				break;
			}
			case Constants.PRO_LITE:{
				hideProLite();
				break;
			}
		}
	}

	public void onUpgradeClick(int account){
		LogUtil.logDebug("account: " + account);
		RelativeLayout selectPaymentMethodClicked;

		switch (account){
			case Constants.PRO_LITE:{
				LogUtil.logDebug("PRO_LITE ");
				selectPaymentMethodClicked = (RelativeLayout) selectPaymentMethodLayoutLite;
				break;
			}
			case Constants.PRO_I:{
				selectPaymentMethodClicked = (RelativeLayout) selectPaymentMethodLayoutPro1;
				break;
			}
			case Constants.PRO_II:{
				selectPaymentMethodClicked = (RelativeLayout) selectPaymentMethodLayoutPro2;
				break;
			}
			case Constants.PRO_III:{
				selectPaymentMethodClicked = (RelativeLayout) selectPaymentMethodLayoutPro3;
				break;
			}
			default:{
				selectPaymentMethodClicked = (RelativeLayout) selectPaymentMethodLayoutLite;
				break;
			}
		}

		if (myAccountInfo.getPaymentBitSet() != null){
			LogUtil.logDebug("myAccountInfo.getPaymentBitSet() != null");

			selectPaymentMethod = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_text_payment_method);
			paymentTitle = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_text_payment_title);


//			RelativeLayout.LayoutParams titleParams = (RelativeLayout.LayoutParams) selectPaymentMethod.getLayoutParams();
//			titleParams.setMargins(0,Util.scaleHeightPx(18, outMetrics),0,Util.scaleHeightPx(14, outMetrics));
//			selectPaymentMethod.setLayoutParams(titleParams);

			switch (account){
				case Constants.PRO_LITE:{
					paymentTitle.setTextColor(ContextCompat.getColor(context, R.color.upgrade_orange));
					paymentTitle.setText(getString(R.string.prolite_account));
					break;
				}
				case Constants.PRO_I:{
					paymentTitle.setTextColor(ContextCompat.getColor(context, R.color.login_warning));
					paymentTitle.setText(getString(R.string.pro1_account));
					break;
				}
				case Constants.PRO_II:{
					paymentTitle.setTextColor(ContextCompat.getColor(context, R.color.login_warning));
					paymentTitle.setText(getString(R.string.pro2_account));
					break;
				}
				case Constants.PRO_III:{
					paymentTitle.setTextColor(ContextCompat.getColor(context, R.color.login_warning));
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
                textGoogleWallet = textGoogleWallet.replace("[A]", "<font color=\'#000000\'>");
                textGoogleWallet = textGoogleWallet.replace("[/A]", "</font>");
            }
            catch (Exception e){}
            Spanned resultGoogleWallet = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                resultGoogleWallet = Html.fromHtml(textGoogleWallet,Html.FROM_HTML_MODE_LEGACY);
            }else {
                resultGoogleWallet = Html.fromHtml(textGoogleWallet);
            }
            googleWalletText.setText(resultGoogleWallet);


			creditCardLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card);
			creditCardLayout.setOnClickListener(this);

			creditCardLayer = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card_layer);
			creditCardLayer.setVisibility(View.GONE);

			creditCardText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card_text);
			String textCreditCardText = getString(R.string.payment_method_credit_card);
			try{
				textCreditCardText = textCreditCardText.replace("[A]", "<font color=\'#000000\'>");
				textCreditCardText = textCreditCardText.replace("[/A]", "</font>");
			}
			catch (Exception e){}
			Spanned resultCreditCardText = null;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
				resultCreditCardText = Html.fromHtml(textCreditCardText,Html.FROM_HTML_MODE_LEGACY);
			}else {
				resultCreditCardText = Html.fromHtml(textCreditCardText);
			}
			creditCardText.setText(resultCreditCardText);

			fortumoLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo);
			fortumoLayout.setOnClickListener(this);

			fortumoLayer = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo_layer);
			fortumoLayer.setVisibility(View.GONE);

			fortumoText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo_text);

			String textFortumoText = getString(R.string.payment_method_fortumo);
			try{
				textFortumoText = textFortumoText.replace("[A]", "<font color=\'#000000\'>");
				textFortumoText = textFortumoText.replace("[/A]", "</font>");
			}
			catch (Exception e){}
			Spanned resultFortumoText = null;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
				resultFortumoText = Html.fromHtml(textFortumoText,Html.FROM_HTML_MODE_LEGACY);
			}else {
				resultFortumoText = Html.fromHtml(textFortumoText);
			}
			fortumoText.setText(resultFortumoText);

			centiliLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_centili);
			centiliLayout.setOnClickListener(this);

			centiliLayer = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_centili_layer);
			centiliLayer.setVisibility(View.GONE);

			centiliText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_centili_text);

			String textCentiliText = getString(R.string.payment_method_centili);
			try{
				textCentiliText = textCentiliText.replace("[A]", "<font color=\'#000000\'>");
				textCentiliText = textCentiliText.replace("[/A]", "</font>");
			}
			catch (Exception e){}
			Spanned resultCentiliText = null;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
				resultCentiliText = Html.fromHtml(textCentiliText,Html.FROM_HTML_MODE_LEGACY);
			}else {
				resultCentiliText = Html.fromHtml(textCentiliText);
			}
			centiliText.setText(resultCentiliText);

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
			LogUtil.logDebug("END refreshAccountInfo");
			if (!myAccountInfo.isInventoryFinished()){
				LogUtil.logDebug("if (!myAccountInfo.isInventoryFinished())");
				googlePlayLayout.setVisibility(View.GONE);
			}
			LogUtil.logDebug("Just before show the layout");

			selectPaymentMethodClicked.setVisibility(View.VISIBLE);
			semitransparentLayer.setVisibility(View.VISIBLE);

			switch (account){
				case Constants.PRO_I:{

					new Handler().post(new Runnable() {
						@Override
						public void run() {
							LogUtil.logDebug("smeasure: " + pro2Layout.getTop());
							LogUtil.logDebug("scroll to: " + pro2Layout.getBottom());
							scrollView.smoothScrollTo(0, pro1Layout.getTop());

						}
					});
					break;
				}
				case Constants.PRO_II:{
					new Handler().post(new Runnable() {
						@Override
						public void run() {
							scrollView.smoothScrollTo(0, pro3Layout.getBottom());
						}
					});
					break;
				}
				case Constants.PRO_III:{
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
			LogUtil.logWarning("PaymentBitSet Null");
		}
	}

	private void hideProLite(){
		LogUtil.logDebug("hideProLite");
		proLiteTransparentLayout.setVisibility(View.VISIBLE);

	}

	private void hideProI(){
		LogUtil.logDebug("hideProI");
		pro1TransparentLayout.setVisibility(View.VISIBLE);
	}

	private void hideProII(){
		LogUtil.logDebug("hideProII");
		pro2TransparentLayout.setVisibility(View.VISIBLE);
	}

	private void hideProIII(){
		LogUtil.logDebug("hideProIII");
		pro3TransparentLayout.setVisibility(View.VISIBLE);
	}

	private Spanned generateByteString(long bytes, int type, int labelType) {
		String textToShow = new StringBuilder().append("[A] ")
											   .append(sizeTranslation(bytes, type))
											   .append(" ")
											   .append(sizeUnit(type))
											   .append(" [/A] ")
											   .append(storageOrTransferLabel(labelType))
											   .toString();

		try {
			textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
			textToShow = textToShow.replace("[/A]", "</font>");
		} catch (NullPointerException ex) {
			LogUtil.logError("NullPointerException happens when getting the storage string", ex);
		}

		Spanned result = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
		} else {
			result = Html.fromHtml(textToShow);
		}
		return result;
	}

	private String sizeTranslation(long size, int type) {
		LogUtil.logDebug("size: " + size + ", type: " + type);

		if (type == TYPE_TERA_BYTE) {
			size = size / 1024;
		}
		String value = new DecimalFormat("#").format(size);
		return value;
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

	private String sizeUnit(int type) {
		return type == 0 ? getString(R.string.label_file_size_tera_byte) : getString(R.string.label_file_size_giga_byte);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
	}
	
	public void showNextPaymentFragment(int paymentM){
		LogUtil.logDebug("paymentM: " + paymentM);

		if(selectPaymentMethodLayoutLite.getVisibility()==View.VISIBLE){
			parameterType=Constants.PRO_LITE;
		}
		else if(selectPaymentMethodLayoutPro1.getVisibility()==View.VISIBLE){
			parameterType=Constants.PRO_I;
		}
		else if(selectPaymentMethodLayoutPro2.getVisibility()==View.VISIBLE){
			parameterType=Constants.PRO_II;
		}
		else if(selectPaymentMethodLayoutPro3.getVisibility()==View.VISIBLE){
			parameterType=Constants.PRO_III;
		}
		else{
			parameterType=0;
		}
		paymentMethod = paymentM;
		LogUtil.logDebug("parameterType: " + parameterType);

		((ManagerActivityLollipop)context).setSelectedAccountType(parameterType);
		((ManagerActivityLollipop)context).setSelectedPaymentMethod(paymentMethod);
		showmyF(paymentM, parameterType);

//		((ManagerActivityLollipop)context).showmyF(paymentMethod, parameterType);
	}

	@Override
	public void onClick(View v) {
		LogUtil.logDebug("onClick");

		((ManagerActivityLollipop)context).setDisplayedAccountType(-1);
		switch (v.getId()){
            case R.id.button_continue:{
				LogUtil.logDebug("Button button_continue pressed");
				if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
					//MONTHLY SUBSCRIPTION
					switch (parameterType) {
						case 1: {
							//PRO I
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, Constants.PAYMENT_CC_MONTH, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(ManagerActivityLollipop.SKU_PRO_I_MONTH);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_FORTUMO: {
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_CENTILI: {
									break;
								}
							}
							break;
						}
						case 2: {
							//PRO II
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, Constants.PAYMENT_CC_MONTH, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(ManagerActivityLollipop.SKU_PRO_II_MONTH);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_FORTUMO: {
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_CENTILI: {
									break;
								}
							}
							break;
						}
						case 3: {
							//PRO III
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, Constants.PAYMENT_CC_MONTH, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(ManagerActivityLollipop.SKU_PRO_III_MONTH);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_FORTUMO: {
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_CENTILI: {
									break;
								}
							}
							break;
						}
						case 4: {
							//LITE
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, Constants.PAYMENT_CC_MONTH, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(ManagerActivityLollipop.SKU_PRO_LITE_MONTH);
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
									((ManagerActivityLollipop) context).showCC(parameterType, Constants.PAYMENT_CC_YEAR, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(ManagerActivityLollipop.SKU_PRO_I_YEAR);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_FORTUMO: {
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_CENTILI: {
									break;
								}
							}
							break;
						}
						case 2: {
							//PRO II
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, Constants.PAYMENT_CC_YEAR, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(ManagerActivityLollipop.SKU_PRO_II_YEAR);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_FORTUMO: {
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_CENTILI: {
									break;
								}
							}
							break;
						}
						case 3: {
							//PRO III
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, Constants.PAYMENT_CC_YEAR, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(ManagerActivityLollipop.SKU_PRO_III_YEAR);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_FORTUMO: {
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_CENTILI: {
									break;
								}
							}
							break;
						}
						case 4: {
							//LITE
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, Constants.PAYMENT_CC_YEAR, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(ManagerActivityLollipop.SKU_PRO_LITE_YEAR);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_FORTUMO: {
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_CENTILI: {
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
				LogUtil.logDebug("button_cancel");
				semitransparentLayer.setVisibility(View.GONE);
				selectPaymentMethodLayoutLite.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
				break;
			}
			case R.id.semitransparent_layer:{
				LogUtil.logDebug("semitransparent_layer");
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
					onUpgradeClick(Constants.PRO_LITE);
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
					onUpgradeClick(Constants.PRO_I);
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
					onUpgradeClick(Constants.PRO_II);
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
					onUpgradeClick(Constants.PRO_III);
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
			case R.id.billed_monthly:{
				break;
			}
			case R.id.billed_yearly:{
				break;
			}
		}
	}

	public void showPaymentMethods(int parameterType){
		LogUtil.logDebug("parameterType: " + parameterType);

		if(myAccountInfo==null){
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}

		if(myAccountInfo==null){
			return;
		}

		ArrayList<Product> accounts = myAccountInfo.getProductAccounts();

		if (accounts == null){
			LogUtil.logWarning("accounts == null");
			((MegaApplication) ((Activity)context).getApplication()).askForPricing();
			return;
		}

		switch(parameterType){
			case Constants.PRO_I:{
				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						LogUtil.logDebug("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
						if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							if ((myAccountInfo.getProIMonthly() != null) && (myAccountInfo.getProIYearly() != null)) {
								LogUtil.logDebug("PROI monthly: " + myAccountInfo.getProIMonthly().getOriginalJson());
								LogUtil.logDebug("PROI annualy: " + myAccountInfo.getProIYearly().getOriginalJson());
								googlePlayLayout.setVisibility(View.GONE);
							}
							else{
								googlePlayLayout.setVisibility(View.VISIBLE);
                                layoutButtons.setVisibility(View.VISIBLE);
							}
						}
					}

					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);

					if(!Util.isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}
				else{
					LogUtil.logWarning("Not payment bit set received!!!");
					selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					googlePlayLayout.setVisibility(View.GONE);
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);
				}

				break;
			}
			case Constants.PRO_II:{

				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						LogUtil.logDebug("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
						if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							if ((myAccountInfo.getProIIMonthly() != null) && (myAccountInfo.getProIIYearly() != null)) {
								LogUtil.logDebug("PROII monthly: " + myAccountInfo.getProIIMonthly().getOriginalJson());
								LogUtil.logDebug("PROII annualy: " + myAccountInfo.getProIIYearly().getOriginalJson());
								googlePlayLayout.setVisibility(View.GONE);
							}
							else{
								googlePlayLayout.setVisibility(View.VISIBLE);
                                layoutButtons.setVisibility(View.VISIBLE);

                            }
						}
					}

					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);

					if(!Util.isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}
				else{
					LogUtil.logWarning("Not payment bit set received!!!");
				}

				break;
			}
			case Constants.PRO_III:{

				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						LogUtil.logDebug("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
						if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							if ((myAccountInfo.getProIIIMonthly() != null) && (myAccountInfo.getProIIIYearly() != null)) {
								LogUtil.logDebug("PROIII monthly: " + myAccountInfo.getProIIIMonthly().getOriginalJson());
								LogUtil.logDebug("PROIII annualy: " + myAccountInfo.getProIIIYearly().getOriginalJson());
								googlePlayLayout.setVisibility(View.GONE);
							}
							else{
								googlePlayLayout.setVisibility(View.VISIBLE);
                                layoutButtons.setVisibility(View.VISIBLE);

                            }
						}
					}

					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);

					if(!Util.isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}

				break;
			}
			case Constants.PRO_LITE:{

				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						LogUtil.logDebug("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else {
						if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)) {
							if ((myAccountInfo.getProLiteMonthly() != null) && (myAccountInfo.getProLiteYearly() != null)) {
								LogUtil.logDebug("PRO Lite monthly: " + myAccountInfo.getProLiteMonthly().getOriginalJson());
								LogUtil.logDebug("PRO Lite annualy: " + myAccountInfo.getProLiteYearly().getOriginalJson());
								googlePlayLayout.setVisibility(View.GONE);
							}
							else{
								googlePlayLayout.setVisibility(View.VISIBLE);
                                layoutButtons.setVisibility(View.VISIBLE);

                            }
						}
					}

					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
						fortumoLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
						centiliLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }

					if(!Util.isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
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
		LogUtil.logDebug("setAccountDetails");

		if ((getActivity() == null) || (!isAdded())) {
			LogUtil.logWarning("Fragment MyAccount NOT Attached!");
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
			textMyAccount.setTextColor(ContextCompat.getColor(context,R.color.mail_my_account));
		} else {
			switch (myAccountInfo.getAccountType()) {

				case 0: {
					String textToShowB = getString(R.string.type_of_my_account, getString(R.string.free_account).toUpperCase());
					try{
						textToShowB = textToShowB.replace("[A]", "<font color=\'#2bb200\'>");
						textToShowB = textToShowB.replace("[/A]", "</font>");
					}
					catch (Exception e){}
					Spanned resultB = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultB = Html.fromHtml(textToShowB,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultB = Html.fromHtml(textToShowB);
					}
					textMyAccount.setText(resultB);
					break;
				}

				case 1: {
					String textToShowB = getString(R.string.type_of_my_account, getString(R.string.pro1_account).toUpperCase());
					try{
						textToShowB = textToShowB.replace("[A]", "<font color=\'#ff333a\'>");
						textToShowB = textToShowB.replace("[/A]", "</font>");
					}
					catch (Exception e){}
					Spanned resultB = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultB = Html.fromHtml(textToShowB,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultB = Html.fromHtml(textToShowB);
					}
					textMyAccount.setText(resultB);
					break;
				}

				case 2: {
					String textToShowB = getString(R.string.type_of_my_account, getString(R.string.pro2_account).toUpperCase());
					try{
						textToShowB = textToShowB.replace("[A]", "<font color=\'#ff333a\'>");
						textToShowB = textToShowB.replace("[/A]", "</font>");
					}
					catch (Exception e){}
					Spanned resultB = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultB = Html.fromHtml(textToShowB,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultB = Html.fromHtml(textToShowB);
					}
					textMyAccount.setText(resultB);
					break;
				}

				case 3: {
					String textToShowB = getString(R.string.type_of_my_account, getString(R.string.pro3_account).toUpperCase());
					try{
						textToShowB = textToShowB.replace("[A]", "<font color=\'#ff333a\'>");
						textToShowB = textToShowB.replace("[/A]", "</font>");
					}
					catch (Exception e){}
					Spanned resultB = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultB = Html.fromHtml(textToShowB,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultB = Html.fromHtml(textToShowB);
					}
					textMyAccount.setText(resultB);
					break;
				}

				case 4: {
					String textToShowB = getString(R.string.type_of_my_account, getString(R.string.prolite_account).toUpperCase());
					try{
						textToShowB = textToShowB.replace("[A]", "<font color=\'#ffa500\'>");
						textToShowB = textToShowB.replace("[/A]", "</font>");
					}
					catch (Exception e){}
					Spanned resultB = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultB = Html.fromHtml(textToShowB,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultB = Html.fromHtml(textToShowB);
					}
					textMyAccount.setText(resultB);
					break;
				}

			}
		}

	}


//	public void setPaymentMethods(int parameterType){
//		log("setPaymentMethods");
//
//		if (!myAccountInfo.isInventoryFinished()){
//			log("if (!myAccountInfo.isInventoryFinished())");
//			googlePlayLayout.setVisibility(View.GONE);
//		}
//		else{
//			if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
//				switch (parameterType){
//					case 1:{
//						if ((myAccountInfo.getProIMonthly() != null) && (myAccountInfo.getProIYearly() != null)) {
//							googlePlayLayout.setVisibility(View.GONE);
//						}
//						else{
//							googlePlayLayout.setVisibility(View.VISIBLE);
//						}
//						break;
//					}
//					case 2:{
//						if ((myAccountInfo.getProIIMonthly() != null) && (myAccountInfo.getProIIYearly() != null)) {
//							googlePlayLayout.setVisibility(View.GONE);
//						}
//						else{
//							googlePlayLayout.setVisibility(View.VISIBLE);
//						}
//						break;
//					}
//					case 3:{
//						if ((myAccountInfo.getProIIIMonthly() != null) && (myAccountInfo.getProIIIYearly() != null)) {
//							googlePlayLayout.setVisibility(View.GONE);
//						}
//						else{
//							googlePlayLayout.setVisibility(View.VISIBLE);
//						}
//						break;
//					}
//					case 4:{
//						if ((myAccountInfo.getProLiteMonthly() != null) && (myAccountInfo.getProLiteYearly() != null)) {
//							googlePlayLayout.setVisibility(View.GONE);
//						}
//						else{
//							googlePlayLayout.setVisibility(View.VISIBLE);
//						}
//						break;
//					}
//				}
//
//			}
//		}
//
//		if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
//			creditCardLayout.setVisibility(View.VISIBLE);
//		}
//		if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
//			if (parameterType == 4){
//				fortumoLayout.setVisibility(View.VISIBLE);
//			}
//		}
//		if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
//			if (parameterType == 4){
//				centiliLayout.setVisibility(View.VISIBLE);
//			}
//		}
//		if(!Util.isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
//			selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
//		}
//		else{
//			selectPaymentMethod.setText(getString(R.string.select_payment_method));
//		}
//
//	}


	public void showmyF(int paymentMethod, int parameterType){
		LogUtil.logDebug("paymentMethod " + paymentMethod + ", type " + parameterType);

		String priceMonthlyInteger = "";
		String priceMonthlyDecimal = "";
		String priceYearlyInteger = "";
		String priceYearlyDecimal = "";

		DecimalFormat df = new DecimalFormat("#.##");

		if(myAccountInfo==null){
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}

		if(myAccountInfo == null){
			return;
		}

		ArrayList<Product> accounts = myAccountInfo.getProductAccounts();

		if (accounts == null){
			LogUtil.logWarning("accounts == null");
			((MegaApplication) ((Activity)context).getApplication()).askForPricing();
			return;
		}

		switch(parameterType){
			case 1:{
				LogUtil.logDebug("case PRO I");

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
						Spanned resultMonthly = null;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
							resultMonthly = Html.fromHtml(textToShowMonthly,Html.FROM_HTML_MODE_LEGACY);
						}else {
							resultMonthly = Html.fromHtml(textToShowMonthly);
						}
						billedMonthly.setText(resultMonthly);
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
						Spanned resultYearly = null;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
							resultYearly = Html.fromHtml(textToShowYearly,Html.FROM_HTML_MODE_LEGACY);
						}else {
							resultYearly = Html.fromHtml(textToShowYearly);
						}
						billedYearly.setText(resultYearly);
					}
				}

				switch (paymentMethod){
					case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
						LogUtil.logDebug("Pro I - PAYMENT_METHOD_FORTUMO");
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
						LogUtil.logDebug("Pro I - PAYMENT_METHOD_CENTILI");
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
						LogUtil.logDebug("Pro I - PAYMENT_METHOD_CREDIT_CARD");
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
						LogUtil.logDebug("Pro I - PAYMENT_METHOD_GOOGLE_WALLET");
						fortumoLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);

						if (myAccountInfo.getProIMonthly() != null) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
								billedYearly.setChecked(true);
							}
							billedMonthly.setVisibility(View.GONE);
						}

						if (myAccountInfo.getProIYearly() != null) {
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
				LogUtil.logDebug(" case PRO II");

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
						Spanned resultMonthly = null;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
							resultMonthly = Html.fromHtml(textToShowMonthly,Html.FROM_HTML_MODE_LEGACY);
						}else {
							resultMonthly = Html.fromHtml(textToShowMonthly);
						}
						billedMonthly.setText(resultMonthly);
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
						Spanned resultYearly = null;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
							resultYearly = Html.fromHtml(textToShowYearly,Html.FROM_HTML_MODE_LEGACY);
						}else {
							resultYearly = Html.fromHtml(textToShowYearly);
						}
						billedYearly.setText(resultYearly);

					}
				}

				switch (paymentMethod){
					case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
						LogUtil.logDebug("Pro II - PAYMENT_METHOD_FORTUMO");
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
						LogUtil.logDebug("Pro II - PAYMENT_METHOD_CENTILI");
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
						LogUtil.logDebug("Pro II - PAYMENT_METHOD_CREDIT_CARD");
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
						LogUtil.logDebug("Pro II - PAYMENT_METHOD_GOOGLE_WALLET");
						fortumoLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);

						if (myAccountInfo.getProIIMonthly() != null) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
								billedYearly.setChecked(true);
							}
							billedMonthly.setVisibility(View.GONE);
						}

						if (myAccountInfo.getProIIYearly() != null) {
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
				LogUtil.logDebug("case PRO III");

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
						Spanned resultMonthly = null;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
							resultMonthly = Html.fromHtml(textToShowMonthly,Html.FROM_HTML_MODE_LEGACY);
						}else {
							resultMonthly = Html.fromHtml(textToShowMonthly);
						}
						billedMonthly.setText(resultMonthly);
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
						Spanned resultYearly = null;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
							resultYearly = Html.fromHtml(textToShowYearly,Html.FROM_HTML_MODE_LEGACY);
						}else {
							resultYearly = Html.fromHtml(textToShowYearly);
						}
						billedYearly.setText(resultYearly);

					}
				}

				switch (paymentMethod){
					case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
						LogUtil.logDebug("Pro III - PAYMENT_METHOD_FORTUMO");
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
						LogUtil.logDebug("Pro III - PAYMENT_METHOD_CENTILI");
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
						LogUtil.logDebug("Pro III - PAYMENT_METHOD_CREDIT_CARD");
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
						LogUtil.logDebug("Pro III - PAYMENT_METHOD_GOOGLE_WALLET");
						fortumoLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						if (myAccountInfo.getProIIIMonthly() != null) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
								billedYearly.setChecked(true);
							}
							billedMonthly.setVisibility(View.GONE);
						}

						if (myAccountInfo.getProIIIYearly() != null) {
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
				LogUtil.logDebug("case LITE");
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

						String textToShowMonthly = getString(R.string.billed_monthly_text, priceMonthly);
						try{
							textToShowMonthly = textToShowMonthly.replace("[A]", "<font color=\'#000000\'>");
							textToShowMonthly = textToShowMonthly.replace("[/A]", "</font>");
						}
						catch (Exception e){}
						Spanned resultMonthly = null;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
							resultMonthly = Html.fromHtml(textToShowMonthly,Html.FROM_HTML_MODE_LEGACY);
						}else {
							resultMonthly = Html.fromHtml(textToShowMonthly);
						}
						billedMonthly.setText(resultMonthly);

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


						String textToShowYearly = getString(R.string.billed_yearly_text, priceYearly);
						try{
							textToShowYearly = textToShowYearly.replace("[A]", "<font color=\'#000000\'>");
							textToShowYearly = textToShowYearly.replace("[/A]", "</font>");
						}
						catch (Exception e){}
						Spanned resultYearly = null;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
							resultYearly = Html.fromHtml(textToShowYearly,Html.FROM_HTML_MODE_LEGACY);
						}else {
							resultYearly = Html.fromHtml(textToShowYearly);
						}

						billedYearly.setText(resultYearly);
					}
				}

				switch (paymentMethod){
					case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
						LogUtil.logDebug("Lite - PAYMENT_METHOD_FORTUMO");
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
						LogUtil.logDebug("Lite - PAYMENT_METHOD_CENTILI");
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
						LogUtil.logDebug("Lite - PAYMENT_METHOD_CREDIT_CARD");
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
						LogUtil.logDebug("Lite - PAYMENT_METHOD_GOOGLE_WALLET");
						fortumoLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.accentColor)));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);

						if (myAccountInfo.getProLiteMonthly() != null) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
								billedYearly.setChecked(true);
							}
							billedMonthly.setVisibility(View.GONE);
						}
						if (myAccountInfo.getProLiteYearly() != null) {
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
