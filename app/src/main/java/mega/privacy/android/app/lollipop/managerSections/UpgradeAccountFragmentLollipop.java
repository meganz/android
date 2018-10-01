package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.DBUtil;
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

	private RelativeLayout proLiteLayout;
	private RelativeLayout pro1Layout;
	private RelativeLayout pro2Layout;
	private RelativeLayout pro3Layout;

	TextView upgradeComment;

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
//	RelativeLayout closeLayout;
	private TextView selectPaymentMethod;
	private TextView paymentTitle;

	RelativeLayout googlePlayLayout;
	RelativeLayout creditCardLayout;
	RelativeLayout fortumoLayout;
	RelativeLayout centiliLayout;
	LinearLayout optionsBilling;
	RadioGroup billingPeriod;
	RadioButton billedMonthly;
	RadioButton billedYearly;
	LinearLayout layoutButtons;

	ImageView closeIcon;
	ImageView fortumoIcon;
	TextView fortumoText;
	ImageView centiliIcon;
	TextView centiliText;
	ImageView creditCardIcon;
	TextView creditCardText;
	ImageView googleWalletIcon;
	TextView googleWalletText;

	Context context;


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
		log("onCreate");
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
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		View v = inflater.inflate(R.layout.fragment_upgrade_account, container, false);
		scrollView = (ScrollView) v.findViewById(R.id.scroll_view_upgrade);
		textMyAccount = (TextView) v.findViewById(R.id.text_of_my_account);
		semitransparentLayer = (RelativeLayout) v.findViewById(R.id.semitransparent_layer);
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

		upgradeComment = (TextView) v.findViewById(R.id.upgrade_account_comment);
		String text = getString(R.string.upgrade_account_comment);
		try{
			text = text.replace("[A]", "<font color=\'#ff333a\'>");
			text = text.replace("[/A]", "</font>");
		}
		catch (Exception e){}
		Spanned result = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			result = Html.fromHtml(text,Html.FROM_HTML_MODE_LEGACY);
		}else {
			result = Html.fromHtml(text);
		}
		upgradeComment.setText(result);

		setPricing();
		log("setPricing ENDS");
		showAvailableAccount();

		refreshAccountInfo();

		int displayedAccountType = ((ManagerActivityLollipop)context).getDisplayedAccountType();
		log("displayedAccountType: "+displayedAccountType);
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

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		log("END onCreateView");
		return v;
	}

	public void refreshAccountInfo(){
		log("refreshAccountInfo");

		log("Check the last call to callToPricing");
		if(DBUtil.callToPricing(context)){
			log("megaApi.getPricing SEND");
			((MegaApplication) ((Activity)context).getApplication()).askForPricing();
		}

		log("Check the last call to callToPaymentMethods");
		if(DBUtil.callToPaymentMethods(context)){
			log("megaApi.getPaymentMethods SEND");
			((MegaApplication) ((Activity)context).getApplication()).askForPaymentMethods();
		}
	}

	public void setPricing() {
		log("setPricing");

		DecimalFormat df = new DecimalFormat("#.##");

		if (myAccountInfo == null) {
			myAccountInfo = ((MegaApplication) ((Activity) context).getApplication()).getMyAccountInfo();
		}

		if (myAccountInfo != null) {
			ArrayList<Product> productAccounts = myAccountInfo.getProductAccounts();

			if (productAccounts == null) {
				log("productAccounts == null");
				((MegaApplication) ((Activity) context).getApplication()).askForPricing();
				return;
			}

			for (int i = 0; i < productAccounts.size(); i++) {
				Product account = productAccounts.get(i);
				if (account.getLevel() == Constants.PRO_I && account.getMonths() == 1) {
					log("PRO1: " + account.getStorage());

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
					monthSectionPro1.setText(resultA);

					String textToShowB = "[A] "+(account.getStorage() / 1024)+" TB [/A] "+getString(R.string.tab_my_account_storage);
					try{
						textToShowB = textToShowB.replace("[A]", "<font color=\'#000000\'>");
						textToShowB = textToShowB.replace("[/A]", "</font>");
					}
					catch (Exception e){}
					Spanned resultB = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultB = Html.fromHtml(textToShowB,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultB = Html.fromHtml(textToShowB);
					}
					storageSectionPro1.setText(resultB);


					String textToShowC = "[A] "+(account.getTransfer() / 1024)+" TB [/A] "+getString(R.string.transfer_quota);
					try{
						textToShowC = textToShowC.replace("[A]", "<font color=\'#000000\'>");
						textToShowC = textToShowC.replace("[/A]", "</font>");
					}catch (Exception e){}
					Spanned resultC = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultC = Html.fromHtml(textToShowC,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultC = Html.fromHtml(textToShowC);
					}
					bandwidthSectionPro1.setText(resultC);

				} else if (account.getLevel() == Constants.PRO_II && account.getMonths() == 1) {
					log("PRO2: " + account.getStorage());

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

					String textToShowB = "[A] "+(sizeTranslation(account.getStorage(), 0))+" TB [/A] "+getString(R.string.tab_my_account_storage);
					try{
						textToShowB = textToShowB.replace("[A]", "<font color=\'#000000\'>");
						textToShowB = textToShowB.replace("[/A]", "</font>");
					}
					catch (Exception e){}
					Spanned resultB = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultB = Html.fromHtml(textToShowB,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultB = Html.fromHtml(textToShowB);
					}
					storageSectionPro2.setText(resultB);


					String textToShowC = "[A] "+(account.getTransfer() / 1024)+" TB [/A] "+getString(R.string.transfer_quota);
					try{
						textToShowC = textToShowC.replace("[A]", "<font color=\'#000000\'>");
						textToShowC = textToShowC.replace("[/A]", "</font>");
					}catch (Exception e){}
					Spanned resultC = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultC = Html.fromHtml(textToShowC,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultC = Html.fromHtml(textToShowC);
					}
					bandwidthSectionPro2.setText(resultC);

				} else if (account.getLevel() == Constants.PRO_III && account.getMonths() == 1) {
					log("PRO3: " + account.getStorage());

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

					String textToShowB = "[A] "+(sizeTranslation(account.getStorage(), 0))+" TB [/A] "+getString(R.string.tab_my_account_storage);
					try{
						textToShowB = textToShowB.replace("[A]", "<font color=\'#000000\'>");
						textToShowB = textToShowB.replace("[/A]", "</font>");
					}
					catch (Exception e){}
					Spanned resultB = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultB = Html.fromHtml(textToShowB,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultB = Html.fromHtml(textToShowB);
					}
					storageSectionPro3.setText(resultB);


					String textToShowC = "[A] "+(account.getTransfer() / 1024)+" TB [/A] "+getString(R.string.transfer_quota);
					try{
						textToShowC = textToShowC.replace("[A]", "<font color=\'#000000\'>");
						textToShowC = textToShowC.replace("[/A]", "</font>");
					}catch (Exception e){}
					Spanned resultC = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultC = Html.fromHtml(textToShowC,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultC = Html.fromHtml(textToShowC);
					}
					bandwidthSectionPro3.setText(resultC);

				} else if (account.getLevel() == Constants.PRO_LITE && account.getMonths() == 1) {
					log("Lite: " + account.getStorage());

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

					String textToShowB = "[A] "+account.getStorage()+" GB [/A] "+getString(R.string.tab_my_account_storage);
					try{
						textToShowB = textToShowB.replace("[A]", "<font color=\'#000000\'>");
						textToShowB = textToShowB.replace("[/A]", "</font>");
					}
					catch (Exception e){}
					Spanned resultB = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultB = Html.fromHtml(textToShowB,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultB = Html.fromHtml(textToShowB);
					}
					storageSectionLite.setText(resultB);


					String textToShowC = "[A] "+(account.getTransfer() / 1024)+" TB [/A] "+getString(R.string.transfer_quota);
					try{
						textToShowC = textToShowC.replace("[A]", "<font color=\'#000000\'>");
						textToShowC = textToShowC.replace("[/A]", "</font>");
					}catch (Exception e){}
					Spanned resultC = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						resultC = Html.fromHtml(textToShowC,Html.FROM_HTML_MODE_LEGACY);
					}else {
						resultC = Html.fromHtml(textToShowC);
					}
					bandwidthSectionLite.setText(resultC);

				}
			}

			int displayedAccountType = ((ManagerActivityLollipop) context).getDisplayedAccountType();
			log("displayedAccountType: " + displayedAccountType);
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
			log("MyAccountInfo is Null");
		}
	}
	
	public void showAvailableAccount(){
		log("showAvailableAccount()");

		if(myAccountInfo==null){
			log("MyAccountInfo is NULL");
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}

		log("showAvailableAccount: "+myAccountInfo.getAccountType());

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
		log("onUpgradeClick: "+account);
		LinearLayout selectPaymentMethodClicked;

		switch (account){
			case Constants.PRO_LITE:{
				log("onUpgradeClick:PRO_LITE ");

				selectPaymentMethodClicked = (LinearLayout) selectPaymentMethodLayoutLite;
				break;
			}
			case Constants.PRO_I:{
				selectPaymentMethodClicked = (LinearLayout) selectPaymentMethodLayoutPro1;
				break;
			}
			case Constants.PRO_II:{
				selectPaymentMethodClicked = (LinearLayout) selectPaymentMethodLayoutPro2;
				break;
			}
			case Constants.PRO_III:{
				selectPaymentMethodClicked = (LinearLayout) selectPaymentMethodLayoutPro3;
				break;
			}
			default:{
				selectPaymentMethodClicked = (LinearLayout) selectPaymentMethodLayoutLite;
				break;
			}
		}

		if (myAccountInfo.getPaymentBitSet() != null){
			log("onUpgradeClick:myAccountInfo.getPaymentBitSet() != null");

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

			LinearLayout.LayoutParams googlePlayParams = (LinearLayout.LayoutParams) googlePlayLayout.getLayoutParams();
			googlePlayParams.height = Util.scaleHeightPx(HEIGHT_PAYMENT_METHODS_LAYOUT, outMetrics);
			googlePlayLayout.setLayoutParams(googlePlayParams);

			googleWalletIcon = (ImageView) selectPaymentMethodClicked.findViewById(R.id.payment_method_google_wallet_icon);

			RelativeLayout.LayoutParams googleIconParams = (RelativeLayout.LayoutParams) googleWalletIcon.getLayoutParams();
			googleIconParams.height = Util.scaleHeightPx(40, outMetrics);
			googleIconParams.width = Util.scaleWidthPx(40, outMetrics);
			googleIconParams.setMargins(Util.scaleWidthPx(16, outMetrics),Util.scaleHeightPx(8, outMetrics),0,Util.scaleHeightPx(8, outMetrics));

			googleWalletIcon.setLayoutParams(googleIconParams);

			googleWalletText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_google_wallet_text);
			RelativeLayout.LayoutParams googleTextParams = (RelativeLayout.LayoutParams) googleWalletText.getLayoutParams();
			googleTextParams.setMargins(Util.scaleWidthPx(16, outMetrics),0,0,0);
			googleWalletText.setLayoutParams(googleTextParams);

			creditCardLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card);
			creditCardLayout.setOnClickListener(this);

			LinearLayout.LayoutParams creditCardParams = (LinearLayout.LayoutParams) creditCardLayout.getLayoutParams();
			creditCardParams.height = Util.scaleHeightPx(HEIGHT_PAYMENT_METHODS_LAYOUT, outMetrics);
			creditCardLayout.setLayoutParams(creditCardParams);

			creditCardIcon = (ImageView) selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card_icon);

			RelativeLayout.LayoutParams creditCardIconParams = (RelativeLayout.LayoutParams) creditCardIcon.getLayoutParams();
			creditCardIconParams.height = Util.scaleHeightPx(40, outMetrics);
			creditCardIconParams.width = Util.scaleWidthPx(40, outMetrics);
			creditCardIconParams.setMargins(Util.scaleWidthPx(16, outMetrics),Util.scaleHeightPx(8, outMetrics),0,Util.scaleHeightPx(8, outMetrics));

			creditCardIcon.setLayoutParams(creditCardIconParams);

			creditCardText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card_text);
			RelativeLayout.LayoutParams creditCardTextParams = (RelativeLayout.LayoutParams) creditCardText.getLayoutParams();
			creditCardTextParams.setMargins(Util.scaleWidthPx(16, outMetrics),0,0,0);
			creditCardText.setLayoutParams(creditCardTextParams);

			fortumoLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo);
			fortumoLayout.setOnClickListener(this);

			LinearLayout.LayoutParams fortumoParams = (LinearLayout.LayoutParams) fortumoLayout.getLayoutParams();
			fortumoParams.height = Util.scaleHeightPx(HEIGHT_PAYMENT_METHODS_LAYOUT, outMetrics);
			fortumoLayout.setLayoutParams(fortumoParams);

			fortumoIcon = (ImageView) selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo_icon);

			RelativeLayout.LayoutParams fortumoIconParams = (RelativeLayout.LayoutParams) fortumoIcon.getLayoutParams();
			fortumoIconParams.height = Util.scaleHeightPx(40, outMetrics);
			fortumoIconParams.width = Util.scaleWidthPx(40, outMetrics);
			fortumoIconParams.setMargins(Util.scaleWidthPx(16, outMetrics),Util.scaleHeightPx(8, outMetrics),0,Util.scaleHeightPx(8, outMetrics));

			fortumoIcon.setLayoutParams(fortumoIconParams);

			fortumoText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo_text);
			RelativeLayout.LayoutParams fortumoTextParams = (RelativeLayout.LayoutParams) fortumoText.getLayoutParams();
			fortumoTextParams.setMargins(Util.scaleWidthPx(16, outMetrics),0,0,0);
			fortumoText.setLayoutParams(fortumoTextParams);

			centiliLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_centili);
			centiliLayout.setOnClickListener(this);

			LinearLayout.LayoutParams centiliParams = (LinearLayout.LayoutParams) centiliLayout.getLayoutParams();
			centiliParams.height = Util.scaleHeightPx(HEIGHT_PAYMENT_METHODS_LAYOUT, outMetrics);
			centiliLayout.setLayoutParams(centiliParams);

			centiliIcon = (ImageView) selectPaymentMethodClicked.findViewById(R.id.payment_method_centili_icon);

			RelativeLayout.LayoutParams centiliIconParams = (RelativeLayout.LayoutParams) centiliIcon.getLayoutParams();
			centiliIconParams.height = Util.scaleHeightPx(40, outMetrics);
			centiliIconParams.width = Util.scaleWidthPx(40, outMetrics);
			centiliIconParams.setMargins(Util.scaleWidthPx(16, outMetrics),Util.scaleHeightPx(8, outMetrics),0,Util.scaleHeightPx(8, outMetrics));

			centiliIcon.setLayoutParams(fortumoIconParams);

			centiliText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_centili_text);
			RelativeLayout.LayoutParams centiliTextParams = (RelativeLayout.LayoutParams) centiliText.getLayoutParams();
			centiliTextParams.setMargins(Util.scaleWidthPx(16, outMetrics),0,0,0);
			centiliText.setLayoutParams(centiliTextParams);

			optionsBilling = (LinearLayout) selectPaymentMethodClicked.findViewById(R.id.options);

			billingPeriod = (RadioGroup) selectPaymentMethodClicked.findViewById(R.id.billing_period);

			billedMonthly = (RadioButton) selectPaymentMethodClicked.findViewById(R.id.billed_monthly);
			billedMonthly.setOnClickListener(this);
			billedYearly = (RadioButton) selectPaymentMethodClicked.findViewById(R.id.billed_yearly);
			billedYearly.setOnClickListener(this);
			layoutButtons = (LinearLayout) selectPaymentMethodClicked.findViewById(R.id.layout_buttons);


//			closeLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.close_layout);
//			closeLayout.setOnClickListener(this);

//			LinearLayout.LayoutParams closeLayoutParams = (LinearLayout.LayoutParams) closeLayout.getLayoutParams();
//			closeLayoutParams.setMargins(0,Util.scaleHeightPx(5, outMetrics),0,0);
//			closeLayout.setLayoutParams(closeLayoutParams);
//
//			closeIcon = (ImageView) selectPaymentMethodClicked.findViewById(R.id.close_layout_icon);
//
//			RelativeLayout.LayoutParams closeIconParams = (RelativeLayout.LayoutParams) closeIcon.getLayoutParams();
//			closeIconParams.setMargins(0,0,Util.scaleWidthPx(16, outMetrics),Util.scaleHeightPx(8, outMetrics));
//			closeIcon.setLayoutParams(closeIconParams);
//
//			closeLayout.setVisibility(View.VISIBLE);
			googlePlayLayout.setVisibility(View.GONE);
			creditCardLayout.setVisibility(View.GONE);
			fortumoLayout.setVisibility(View.GONE);
			centiliLayout.setVisibility(View.GONE);
			optionsBilling.setVisibility(View.GONE);

			showPaymentMethods(account);

			refreshAccountInfo();
			log("END refreshAccountInfo");
			if (!myAccountInfo.isInventoryFinished()){
				log("if (!myAccountInfo.isInventoryFinished())");
				googlePlayLayout.setVisibility(View.GONE);
			}
			log("Just before show the layout");

			selectPaymentMethodClicked.setVisibility(View.VISIBLE);
			semitransparentLayer.setVisibility(View.VISIBLE);

			switch (account){
				case Constants.PRO_I:{

					new Handler().post(new Runnable() {
						@Override
						public void run() {
							log("smeasure: "+pro2Layout.getTop());
							log("scroll to: "+pro2Layout.getBottom());
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
			log("PaymentBitSet Null");
		}
	}

	private void hideProLite(){
		log("hideProLite");
		proLiteTransparentLayout.setVisibility(View.VISIBLE);
	}

	private void hideProI(){
		log("hideProI");
		pro1TransparentLayout.setVisibility(View.VISIBLE);
	}

	private void hideProII(){
		log("hideProII");
		pro2TransparentLayout.setVisibility(View.VISIBLE);
	}

	private void hideProIII(){
		log("hideProIII");
		pro3TransparentLayout.setVisibility(View.VISIBLE);
	}

	public String sizeTranslation(long size, int type) {
		log("sizeTranslation");

		switch(type){
			case 0:{
				//From GB to TB
				if(size!=1024){
					size=size/1024;
				}
								
				String value = new DecimalFormat("#").format(size);			
				return value;
			}
		}
		return null;
	      
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
	}
	
	public static void log(String log) {
		Util.log("UpgradeAccountFragmentLollipop", log);
	}

	public void showNextPaymentFragment(int paymentMethod){
		log("showNextPaymentFragment: paymentMethod: "+paymentMethod);

		int parameterType;
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
		((ManagerActivityLollipop)context).setSelectedAccountType(parameterType);
		((ManagerActivityLollipop)context).setSelectedPaymentMethod(paymentMethod);
		((ManagerActivityLollipop)context).showmyF(paymentMethod, parameterType);
	}

	@Override
	public void onClick(View v) {
		log("onClick");

		((ManagerActivityLollipop)context).setDisplayedAccountType(-1);
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		switch (v.getId()){
			case R.id.upgrade_prolite_layout:{
				log("onClick()-upgrade_prolite_layout");

				if(selectPaymentMethodLayoutLite.getVisibility()==View.VISIBLE){
					log("onClick()-upgrade_prolite_layout VISIBLE");

					selectPaymentMethodLayoutLite.setVisibility(View.GONE);
					semitransparentLayer.setVisibility(View.GONE);
				}
				else{
					log("onClick()-upgrade_prolite_layout GONE");

					selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
					onUpgradeClick(Constants.PRO_LITE);
				}
				break;
			}
			case R.id.billed_monthly:{
				log("onClick()-billed Monthly");
				layoutButtons.setVisibility(View.VISIBLE);
				break;
			}
			case R.id.billed_yearly:{
				log("onClick()-billed Yearly");
				layoutButtons.setVisibility(View.VISIBLE);
				break;
			}
//			case R.id.close_layout:{
//				log("onClick()-close_layout");
//				selectPaymentMethodLayoutLite.setVisibility(View.GONE);
//				semitransparentLayer.setVisibility(View.GONE);
//
//				selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
//				selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
//				selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
//				break;
//			}
			case R.id.upgrade_pro_i_layout:{
				log("onClick()-upgrade_pro_i_layout");

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
				log("onClick()-upgrade_pro_ii_layout");

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
				log("onClick()-upgrade_pro_iii_layout");

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
				log("onClick()-payment_method_google_wallet");
				optionsBilling.setVisibility(View.VISIBLE);
				setPricingBillingPeriod(MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET);

//				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET);
				break;
			}
			case R.id.payment_method_credit_card:{
				log("onClick()-payment_method_credit_card");

				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD);
				break;
			}
			case R.id.payment_method_fortumo:{
				log("onClick()-payment_method_fortumo");

				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_FORTUMO);
				break;
			}
			case R.id.payment_method_centili:{
				log("onClick()-payment_method_centili");

				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_CENTILI);
				break;
			}
		}
	}

	public void showPaymentMethods(int parameterType){
		log("showPaymentMethods");

		if(myAccountInfo==null){
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}

		if(myAccountInfo==null){
			return;
		}

		ArrayList<Product> accounts = myAccountInfo.getProductAccounts();

		if (accounts == null){
			log("accounts == null");
			((MegaApplication) ((Activity)context).getApplication()).askForPricing();
			return;
		}

		switch(parameterType){
			case Constants.PRO_I:{
				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						log("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
						if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							if ((myAccountInfo.getProIMonthly() != null) && (myAccountInfo.getProIYearly() != null)) {
								log("PROI monthly: " + myAccountInfo.getProIMonthly().getOriginalJson());
								log("PROI annualy: " + myAccountInfo.getProIYearly().getOriginalJson());
								googlePlayLayout.setVisibility(View.GONE);
							}
							else{
								googlePlayLayout.setVisibility(View.VISIBLE);
							}
						}
					}

					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
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
					log("not payment bit set received!!!");
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
						log("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
						if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							if ((myAccountInfo.getProIIMonthly() != null) && (myAccountInfo.getProIIYearly() != null)) {
								log("PROII monthly: " + myAccountInfo.getProIIMonthly().getOriginalJson());
								log("PROII annualy: " + myAccountInfo.getProIIYearly().getOriginalJson());
								googlePlayLayout.setVisibility(View.GONE);
							}
							else{
								googlePlayLayout.setVisibility(View.VISIBLE);
							}
						}
					}

					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
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
					log("not payment bit set received!!!");
				}

				break;
			}
			case Constants.PRO_III:{

				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						log("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
						if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							if ((myAccountInfo.getProIIIMonthly() != null) && (myAccountInfo.getProIIIYearly() != null)) {
								log("PROIII monthly: " + myAccountInfo.getProIIIMonthly().getOriginalJson());
								log("PROIII annualy: " + myAccountInfo.getProIIIYearly().getOriginalJson());
								googlePlayLayout.setVisibility(View.GONE);
							}
							else{
								googlePlayLayout.setVisibility(View.VISIBLE);
							}
						}
					}

					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
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
						log("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else {
						if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)) {
							if ((myAccountInfo.getProLiteMonthly() != null) && (myAccountInfo.getProLiteYearly() != null)) {
								log("PRO Lite monthly: " + myAccountInfo.getProLiteMonthly().getOriginalJson());
								log("PRO Lite annualy: " + myAccountInfo.getProLiteYearly().getOriginalJson());
								googlePlayLayout.setVisibility(View.GONE);
							}
							else{
								googlePlayLayout.setVisibility(View.VISIBLE);
							}
						}
					}

					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
					}
					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
						fortumoLayout.setVisibility(View.VISIBLE);
					}
					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
						centiliLayout.setVisibility(View.VISIBLE);
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
		log("setAccountDetails");

		if ((getActivity() == null) || (!isAdded())) {
			log("Fragment MyAccount NOT Attached!");
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



	public void setPricingBillingPeriod(int paymentMethod){
		log("setPricingBillingPeriod: paymentMethod: "+paymentMethod);

		DecimalFormat df = new DecimalFormat("#.##");
		String priceMonthlyInteger = "";
		String priceMonthlyDecimal = "";
		String priceYearlyInteger = "";
		String priceYearlyDecimal = "";

		if (myAccountInfo == null) {
			myAccountInfo = ((MegaApplication) ((Activity) context).getApplication()).getMyAccountInfo();
		}

		if (myAccountInfo != null) {
			ArrayList<Product> productAccounts = myAccountInfo.getProductAccounts();

			if (productAccounts == null) {
				log("productAccounts == null");
				((MegaApplication) ((Activity) context).getApplication()).askForPricing();
				return;
			}

			for (int i = 0; i < productAccounts.size(); i++) {
				Product account = productAccounts.get(i);
				if (account.getLevel() == Constants.PRO_I && account.getMonths() == 1) {
					log("PRO1: " + account.getStorage());
					if(account.getMonths()==1){
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

					}else if(account.getMonths() == 12){
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
					switch (paymentMethod){
						case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
							break;
						}
					}

				} else if (account.getLevel() == Constants.PRO_II && account.getMonths() == 1) {
					log("PRO2: " + account.getStorage());
					if(account.getMonths()==1){
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

					}else if(account.getMonths() == 12){
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

					switch (paymentMethod){
						case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
							break;
						}
					}

				} else if (account.getLevel() == Constants.PRO_III && account.getMonths() == 1) {
					log("PRO3: " + account.getStorage());

					if(account.getMonths()==1){
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


					}else if(account.getMonths() == 12){
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

					switch (paymentMethod){
						case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
							break;
						}
					}

					break;


				} else if (account.getLevel() == Constants.PRO_LITE) {
					log("Lite: " + account.getStorage());

						if(account.getMonths()==1){
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

						}else if(account.getMonths() == 12){
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

					switch (paymentMethod){
						case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
							break;
						}
					}

					break;
				}
			}

		} else {
			log("MyAccountInfo is Null");
		}
	}

}
