package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.DBUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.LogUtil.*;

public class MonthlyAnnualyFragmentLollipop extends Fragment implements OnClickListener{

	static int HEIGHT_ACCOUNT_LAYOUT=109;

	private TextView title;
	private TextView perMonth;
	private TextView priceInteger;
	private TextView priceDecimal;
	private TextView priceMonthlyInteger;
	private TextView priceMonthlyDecimal;
	private TextView priceAnnualyInteger;
	private TextView priceAnnualyDecimal;
	TextView selectComment;
	private RelativeLayout priceAnnualyLayout;
	private RelativeLayout priceMonthlyLayout;
	private TextView storageInteger;
	private TextView storageGb;
	private TextView bandwidthInteger;
	private TextView bandwidthTb;
	private RelativeLayout subscribeLayout;
	TextView monthlyLabel;
	TextView annualyLabel;

	LinearLayout mainLinearLayout;
	private RelativeLayout leftLayout;
	private RelativeLayout rightLayout;
	View verticalDivider;
	TableRow tableRow;
	TextView emptyTextStorage;
	TextView emptyTextBandwidth;
	LinearLayout buttonsLayout;
	RelativeLayout monthLayout;
	RelativeLayout yearLayout;

	RelativeLayout selectPaymentTitle;
	
	private TextView selectMonthYear;

	TextView commentText;
	
	int selectedSubscription = Constants.PAYMENT_CC_YEAR;
	int parameterType=-1;	
	MegaApiAndroid megaApi;
	Context context;
	MyAccountInfo myAccountInfo;
	int paymentMethod = -1;
	MonthlyAnnualyFragmentLollipop monthlyAnnualyFragment = this;

	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		logDebug("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		DecimalFormat df = new DecimalFormat("#.##");  

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		View v = null;
		v = inflater.inflate(R.layout.fragment_upgrade_monthly_annualy, container, false);

		mainLinearLayout = (LinearLayout) v.findViewById(R.id.linear_layout_monthly_annualy);
		LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mainLinearLayout.getLayoutParams();
		layoutParams.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(8, outMetrics), Util.scaleWidthPx(8, outMetrics), 0);
		mainLinearLayout.setLayoutParams(layoutParams);

		leftLayout = (RelativeLayout) v.findViewById(R.id.upgrade_monthly_annualy_left_side);
		RelativeLayout.LayoutParams leftLayoutParams = (RelativeLayout.LayoutParams) leftLayout.getLayoutParams();
		leftLayoutParams.width = Util.scaleWidthPx(125, outMetrics);
		leftLayoutParams.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
		leftLayout.setLayoutParams(leftLayoutParams);
		
		title = (TextView) v.findViewById(R.id.monthly_annualy_title_text);
		RelativeLayout.LayoutParams titleParams = (RelativeLayout.LayoutParams) title.getLayoutParams();
		titleParams.setMargins(0,0,0,Util.scaleHeightPx(11, outMetrics));
		title.setLayoutParams(titleParams);

		perMonth = (TextView) v.findViewById(R.id.monthly_annualy_per_month_text);
		perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));
		RelativeLayout.LayoutParams perMonthParams = (RelativeLayout.LayoutParams) perMonth.getLayoutParams();
		perMonthParams.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
		perMonth.setLayoutParams(perMonthParams);

		priceInteger = (TextView) v.findViewById(R.id.monthly_annualy_integer_text);
		priceDecimal = (TextView) v.findViewById(R.id.monthly_annualy_decimal_text);
		RelativeLayout.LayoutParams priceDecimalParams = (RelativeLayout.LayoutParams) priceDecimal.getLayoutParams();
		priceDecimalParams.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
		priceDecimal.setLayoutParams(priceDecimalParams);

		priceMonthlyInteger = (TextView) v.findViewById(R.id.monthly_annualy_price_monthly_integer_text);
		priceMonthlyDecimal = (TextView) v.findViewById(R.id.monthly_annualy_price_monthly_decimal_text);
		RelativeLayout.LayoutParams priceMonthlyParams = (RelativeLayout.LayoutParams) priceMonthlyDecimal.getLayoutParams();
		priceMonthlyParams.setMargins(0,0,0,Util.scaleHeightPx(5, outMetrics));
		priceMonthlyDecimal.setLayoutParams(priceMonthlyParams);

		priceAnnualyInteger = (TextView) v.findViewById(R.id.monthly_annualy_price_annualy_integer_text);
		priceAnnualyDecimal = (TextView) v.findViewById(R.id.monthly_annualy_price_annualy_decimal_text);
		RelativeLayout.LayoutParams priceAnnualParams = (RelativeLayout.LayoutParams) priceAnnualyDecimal.getLayoutParams();
		priceAnnualParams.setMargins(0,0,0,Util.scaleHeightPx(5, outMetrics));
		priceAnnualyDecimal.setLayoutParams(priceAnnualParams);

		verticalDivider = (View) v.findViewById(R.id.upgrade_monthly_annualy_vertical_divider);
		verticalDivider.getLayoutParams().width = Util.scaleWidthPx(2, outMetrics);
		verticalDivider.getLayoutParams().height = Util.scaleHeightPx(86, outMetrics);

		rightLayout = (RelativeLayout) v.findViewById(R.id.upgrade_monthly_annualy_layout_right_side);
		RelativeLayout.LayoutParams rightLayoutParams = (RelativeLayout.LayoutParams) rightLayout.getLayoutParams();
		rightLayoutParams.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
		rightLayout.setLayoutParams(rightLayoutParams);

		tableRow = (TableRow) v.findViewById(R.id.upgrade_monthly_annualy_table_row);
		TableLayout.LayoutParams tableRowParams = (TableLayout.LayoutParams) tableRow.getLayoutParams();
		tableRowParams.setMargins(0,0,0,Util.scaleHeightPx(25, outMetrics));
		tableRow.setLayoutParams(tableRowParams);

		selectComment = (TextView) v.findViewById(R.id.monthly_annualy_select_comment);
		priceMonthlyLayout = (RelativeLayout) v.findViewById(R.id.monthly_annualy_monthly_layout);
		priceAnnualyLayout = (RelativeLayout) v.findViewById(R.id.monthly_annualy_annualy_layout);
		monthlyLabel = (TextView) v.findViewById(R.id.monthly_annualy_monthly_text);
		monthlyLabel.setText(getString(R.string.upgrade_per_month).toUpperCase(Locale.getDefault()));
		annualyLabel = (TextView) v.findViewById(R.id.monthly_annualy_annualy_text);
		annualyLabel.setText(getString(R.string.upgrade_per_year).toUpperCase(Locale.getDefault()));
		priceAnnualyLayout.setVisibility(View.VISIBLE);
		
		priceMonthlyLayout.setOnClickListener(this);
		priceAnnualyLayout.setOnClickListener(this);
		
		storageInteger = (TextView) v.findViewById(R.id.monthly_annualy_storage_value_integer);
		TableRow.LayoutParams storageValueParams = (TableRow.LayoutParams) storageInteger.getLayoutParams();
		storageValueParams.width = Util.scaleWidthPx(40, outMetrics);
		storageInteger.setLayoutParams(storageValueParams);

		emptyTextStorage = (TextView) v.findViewById(R.id.monthly_annualy_storage_empty_text);
		TableRow.LayoutParams emptyTextParams = (TableRow.LayoutParams) emptyTextStorage.getLayoutParams();
		emptyTextParams.width = Util.scaleWidthPx(12, outMetrics);
		emptyTextStorage.setLayoutParams(emptyTextParams);

		storageGb = (TextView) v.findViewById(R.id.monthly_annualy_storage_value_gb);

		bandwidthInteger = (TextView) v.findViewById(R.id.monthly_annualy_bandwidth_value_integer);
		TableRow.LayoutParams bandwidthValueParams = (TableRow.LayoutParams) bandwidthInteger.getLayoutParams();
		bandwidthValueParams.width = Util.scaleWidthPx(40, outMetrics);
		bandwidthInteger.setLayoutParams(bandwidthValueParams);

		emptyTextBandwidth = (TextView) v.findViewById(R.id.monthly_annualy_bandwith_empty_text);
		TableRow.LayoutParams emptyTextBandwidthParams = (TableRow.LayoutParams) emptyTextBandwidth.getLayoutParams();
		emptyTextBandwidthParams.width = Util.scaleWidthPx(12, outMetrics);
		emptyTextBandwidth.setLayoutParams(emptyTextBandwidthParams);

		bandwidthTb = (TextView) v.findViewById(R.id.monthly_annualy_bandwith_value_tb);

		selectPaymentTitle = (RelativeLayout) v.findViewById(R.id.monthly_annualy_layout_select_inside);
		LinearLayout.LayoutParams paymentTitleParams = (LinearLayout.LayoutParams) selectPaymentTitle.getLayoutParams();
		paymentTitleParams.setMargins(0, Util.scaleHeightPx(26, outMetrics), 0, Util.scaleHeightPx(27, outMetrics));
		selectPaymentTitle.setLayoutParams(paymentTitleParams);
		
		selectMonthYear = (TextView) v.findViewById(R.id.monthly_annualy_select);
		RelativeLayout.LayoutParams textPaymentParams = (RelativeLayout.LayoutParams) selectMonthYear.getLayoutParams();
		textPaymentParams.setMargins(0, 0, 0, Util.scaleHeightPx(9, outMetrics));
		selectMonthYear.setLayoutParams(textPaymentParams);

		buttonsLayout = (LinearLayout) v.findViewById(R.id.monthly_annualy_buttons_layout);
		LinearLayout.LayoutParams buttonsLayoutParams = (LinearLayout.LayoutParams) buttonsLayout.getLayoutParams();
		buttonsLayoutParams.height = Util.scaleHeightPx(128, outMetrics);
		buttonsLayout.setLayoutParams(buttonsLayoutParams);

		monthLayout = (RelativeLayout) v.findViewById(R.id.monthly_annualy_monthly_layout);
		LinearLayout.LayoutParams monthLayoutParams = (LinearLayout.LayoutParams) monthLayout.getLayoutParams();
		monthLayoutParams.width = Util.scaleWidthPx(144, outMetrics);
		monthLayoutParams.setMargins(Util.scaleWidthPx(8, outMetrics), 0, 0, 0);
		monthLayout.setLayoutParams(monthLayoutParams);

		yearLayout = (RelativeLayout) v.findViewById(R.id.monthly_annualy_monthly_layout);
		LinearLayout.LayoutParams yearLayoutParams = (LinearLayout.LayoutParams) yearLayout.getLayoutParams();
		yearLayoutParams.width = Util.scaleWidthPx(144, outMetrics);
		yearLayoutParams.setMargins(Util.scaleWidthPx(8, outMetrics), 0, 0, 0);
		yearLayout.setLayoutParams(yearLayoutParams);

		subscribeLayout = (RelativeLayout) v.findViewById(R.id.monthly_annualy_subscribe_layout);
		subscribeLayout.setOnClickListener(this);
		LinearLayout.LayoutParams subscribeParams = (LinearLayout.LayoutParams) subscribeLayout.getLayoutParams();
		subscribeParams.height = Util.scaleHeightPx(52, outMetrics);
		subscribeParams.setMargins(0, 0, Util.scaleWidthPx(15, outMetrics), 0);
		subscribeLayout.setLayoutParams(subscribeParams);

		commentText = (TextView) v.findViewById(R.id.monthly_annualy_layout_subscribe_comment_text);
		LinearLayout.LayoutParams commentParams = (LinearLayout.LayoutParams) commentText.getLayoutParams();
		commentParams.setMargins(Util.scaleWidthPx(21, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(21, outMetrics), Util.scaleHeightPx(8, outMetrics));
		commentText.setLayoutParams(commentParams);

		setPricing();

		refreshAccountInfo();

		return v;
	}

	public void refreshAccountInfo(){

		logDebug("Check the last call to callToPricing");
		if(DBUtil.callToPricing()){
			((MegaApplication) ((Activity)context).getApplication()).askForPricing();
		}

		logDebug("Check the last call to callToPaymentMethods");
		if(DBUtil.callToPaymentMethods()){
			((MegaApplication) ((Activity)context).getApplication()).askForPaymentMethods();
		}
	}

	public void setPricing(){
		logDebug("setPricing");

		DecimalFormat df = new DecimalFormat("#.##");

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

		logDebug("parameterType: " + parameterType);

		switch(parameterType){
			case 1:{
				for (int i=0;i<accounts.size();i++){

					Product account = accounts.get(i);

					if(account.getLevel()==1 && account.getMonths()==1){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceInteger.setText(s1[0]);
								priceDecimal.setText("");
								priceMonthlyInteger.setText(s1[0]);
								priceMonthlyDecimal.setText("");
							}
							else if (s1.length == 2){
								priceInteger.setText(s1[0]);
								priceDecimal.setText("." + s1[1] + " €");
								priceMonthlyInteger.setText(s1[0]);
								priceMonthlyDecimal.setText("." + s1[1] + " €");
							}
						}
						else if (s.length == 2){
							priceInteger.setText(s[0]);
							priceDecimal.setText("." + s[1] + " €");
							priceMonthlyInteger.setText(s[0]);
							priceMonthlyDecimal.setText("." + s[1] + " €");
						}

						storageInteger.setText(""+account.getStorage()/1024);
						storageGb.setText(" TB");

						bandwidthInteger.setText(""+account.getTransfer()/1024);
						bandwidthTb.setText(" TB");

						title.setText(getString(R.string.pro1_account));

						title.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						storageInteger.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						storageGb.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						bandwidthInteger.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						bandwidthTb.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						perMonth.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
					}
					if (account.getLevel()==1 && account.getMonths()==12){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceAnnualyInteger.setText(s1[0]);
								priceAnnualyDecimal.setText("");
							}
							else if (s1.length == 2){
								priceAnnualyInteger.setText(s1[0]);
								priceAnnualyDecimal.setText("." + s1[1] + " €");
							}
						}
						else if (s.length == 2){
							priceAnnualyInteger.setText(s[0]);
							priceAnnualyDecimal.setText("." + s[1] + " €");
						}
					}
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
						if (myAccountInfo.getProIMonthly() != null) {
							logDebug("ProIMonthly already subscribed: " + myAccountInfo.getProIMonthly().getOriginalJson());
							priceMonthlyLayout.setVisibility(View.GONE);
						}

						if (myAccountInfo.getProIYearly() != null) {
							logDebug("ProIAnnualy already subscribed: " + myAccountInfo.getProIYearly().getOriginalJson());
							priceAnnualyLayout.setVisibility(View.GONE);
							if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
								priceMonthlyLayout.setBackgroundResource(R.drawable.red_border_upgrade_account);
							}
							else{
								priceMonthlyLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.red_border_upgrade_account));
							}

							selectedSubscription = Constants.PAYMENT_CC_MONTH;
						}
						break;
					}
				}

				break;
			}
			case 2:{
				for (int i=0;i<accounts.size();i++){

					Product account = accounts.get(i);

					if(account.getLevel()==2 && account.getMonths()==1){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceInteger.setText(s1[0]);
								priceDecimal.setText("");
								priceMonthlyInteger.setText(s1[0]);
								priceMonthlyDecimal.setText("");
							}
							else if (s1.length == 2){
								priceInteger.setText(s1[0]);
								priceDecimal.setText("." + s1[1] + " €");
								priceMonthlyInteger.setText(s1[0]);
								priceMonthlyDecimal.setText("." + s1[1] + " €");
							}
						}
						else if (s.length == 2){
							priceInteger.setText(s[0]);
							priceDecimal.setText("." + s[1] + " €");
							priceMonthlyInteger.setText(s[0]);
							priceMonthlyDecimal.setText("." + s[1] + " €");
						}

						storageInteger.setText(sizeTranslation(account.getStorage(),0));
						storageGb.setText(" TB");

						bandwidthInteger.setText(""+account.getTransfer()/1024);
						bandwidthTb.setText(" TB");

						title.setText(getString(R.string.pro2_account));

						title.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						storageInteger.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						storageGb.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						bandwidthInteger.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						bandwidthTb.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						perMonth.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
					}
					if (account.getLevel()==2 && account.getMonths()==12){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceAnnualyInteger.setText(s1[0]);
								priceAnnualyDecimal.setText("");
							}
							else if (s1.length == 2){
								priceAnnualyInteger.setText(s1[0]);
								priceAnnualyDecimal.setText("." + s1[1] + " €");
							}
						}
						else if (s.length == 2){
							priceAnnualyInteger.setText(s[0]);
							priceAnnualyDecimal.setText("." + s[1] + " €");
						}
					}
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
						if (myAccountInfo.getProIIMonthly() != null) {
							logDebug("ProIIMonthly already subscribed: " + myAccountInfo.getProIIMonthly().getOriginalJson());
							priceMonthlyLayout.setVisibility(View.GONE);
						}

						if (myAccountInfo.getProIIYearly() != null) {
							logDebug("ProIIAnnualy already subscribed: " + myAccountInfo.getProIIYearly().getOriginalJson());
							priceAnnualyLayout.setVisibility(View.GONE);

							if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
								priceMonthlyLayout.setBackgroundResource(R.drawable.red_border_upgrade_account);
							}
							else{
								priceMonthlyLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.red_border_upgrade_account));
							}
							selectedSubscription = Constants.PAYMENT_CC_MONTH;
						}
						break;
					}
				}

				break;
			}
			case 3:{
				for (int i=0;i<accounts.size();i++){

					Product account = accounts.get(i);

					if(account.getLevel()==3 && account.getMonths()==1){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceInteger.setText(s1[0]);
								priceDecimal.setText("");
								priceMonthlyInteger.setText(s1[0]);
								priceMonthlyDecimal.setText("");
							}
							else if (s1.length == 2){
								priceInteger.setText(s1[0]);
								priceDecimal.setText("." + s1[1] + " €");
								priceMonthlyInteger.setText(s1[0]);
								priceMonthlyDecimal.setText("." + s1[1] + " €");
							}
						}
						else if (s.length == 2){
							priceInteger.setText(s[0]);
							priceDecimal.setText("." + s[1] + " €");
							priceMonthlyInteger.setText(s[0]);
							priceMonthlyDecimal.setText("." + s[1] + " €");
						}


						storageInteger.setText(sizeTranslation(account.getStorage(),0));
						storageGb.setText(" TB");

						bandwidthInteger.setText(""+account.getTransfer()/1024);
						bandwidthTb.setText(" TB");

						title.setText(getString(R.string.pro3_account));

						title.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						storageInteger.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						storageGb.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						bandwidthInteger.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						bandwidthTb.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
						perMonth.setTextColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
					}
					if (account.getLevel()==3 && account.getMonths()==12){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceAnnualyInteger.setText(s1[0]);
								priceAnnualyDecimal.setText("");
							}
							else if (s1.length == 2){
								priceAnnualyInteger.setText(s1[0]);
								priceAnnualyDecimal.setText("." + s1[1] + " €");
							}
						}
						else if (s.length == 2){
							priceAnnualyInteger.setText(s[0]);
							priceAnnualyDecimal.setText("." + s[1] + " €");
						}

					}
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
						if (myAccountInfo.getProIIIMonthly() != null) {
							logDebug("ProIIIMonthly already subscribed: " + myAccountInfo.getProIIIMonthly().getOriginalJson());
							priceMonthlyLayout.setVisibility(View.GONE);
						}

						if (myAccountInfo.getProIIIYearly() != null) {
							logDebug("ProIIIAnnualy already subscribed: " + myAccountInfo.getProIIIYearly().getOriginalJson());
							priceAnnualyLayout.setVisibility(View.GONE);
							if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
								priceMonthlyLayout.setBackgroundResource(R.drawable.red_border_upgrade_account);
							}
							else{
								priceMonthlyLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.red_border_upgrade_account));
							}
							selectedSubscription = Constants.PAYMENT_CC_MONTH;
						}
						break;
					}
				}

				break;
			}
			case 4:{
				logDebug("case 4 -> accounts.size() " + accounts.size());

				for (int i=0;i<accounts.size();i++){

					Product account = accounts.get(i);

					if(account.getLevel()==4 && account.getMonths()==1){
						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceInteger.setText(s1[0]);
								priceDecimal.setText("");
								priceMonthlyInteger.setText(s1[0]);
								priceMonthlyDecimal.setText("");
							}
							else if (s1.length == 2){
								priceInteger.setText(s1[0]);
								priceDecimal.setText("." + s1[1] + " €");
								priceMonthlyInteger.setText(s1[0]);
								priceMonthlyDecimal.setText("." + s1[1] + " €");
							}
						}
						else if (s.length == 2){
							priceInteger.setText(s[0]);
							priceDecimal.setText("." + s[1] + " €");
							priceMonthlyInteger.setText(s[0]);
							priceMonthlyDecimal.setText("." + s[1] + " €");
						}

						storageInteger.setText(""+account.getStorage());
						storageGb.setText(" GB");

						bandwidthInteger.setText(""+account.getTransfer()/1024);
						bandwidthTb.setText(" TB");

						title.setText(getString(R.string.prolite_account));
						title.setTextColor(ContextCompat.getColor(context, R.color.upgrade_orange));
						storageInteger.setTextColor(ContextCompat.getColor(context, R.color.upgrade_orange));
						storageGb.setTextColor(ContextCompat.getColor(context, R.color.upgrade_orange));
						bandwidthInteger.setTextColor(ContextCompat.getColor(context, R.color.upgrade_orange));
						bandwidthTb.setTextColor(ContextCompat.getColor(context, R.color.upgrade_orange));
						perMonth.setTextColor(ContextCompat.getColor(context, R.color.upgrade_orange));
					}
					if (account.getLevel()==4 && account.getMonths()==12){

						double price = account.getAmount()/100.00;
						String priceString = df.format(price);
						String [] s = priceString.split("\\.");
						if (s.length == 1){
							String [] s1 = priceString.split(",");
							if (s1.length == 1){
								priceAnnualyInteger.setText(s1[0]);
								priceAnnualyDecimal.setText("");
							}
							else if (s1.length == 2){
								priceAnnualyInteger.setText(s1[0]);
								priceAnnualyDecimal.setText("." + s1[1] + " €");
							}
						}
						else if (s.length == 2){
							priceAnnualyInteger.setText(s[0]);
							priceAnnualyDecimal.setText("." + s[1] + " €");
						}
					}
				}

				switch (paymentMethod){
					case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
						priceAnnualyLayout.setVisibility(View.GONE);

						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
							priceMonthlyLayout.setBackgroundResource(R.drawable.red_border_upgrade_account);
						}
						else{
							priceMonthlyLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.red_border_upgrade_account));
						}
						selectedSubscription = Constants.PAYMENT_CC_MONTH;
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
						priceAnnualyLayout.setVisibility(View.GONE);

						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
							priceMonthlyLayout.setBackgroundResource(R.drawable.red_border_upgrade_account);
						}
						else{
							priceMonthlyLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.red_border_upgrade_account));
						}
						selectedSubscription = Constants.PAYMENT_CC_MONTH;
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
						logDebug("PAYMENT_METHOD_GOOGLE_WALLET");

						if (myAccountInfo.getProLiteMonthly() != null) {
							logDebug("ProLiteMonthly already subscribed: " + myAccountInfo.getProLiteMonthly().getOriginalJson());
							priceMonthlyLayout.setVisibility(View.GONE);
						}

						if (myAccountInfo.getProLiteYearly() != null) {
							logDebug("ProLiteAnnualy already subscribed: " + myAccountInfo.getProLiteYearly().getOriginalJson());
							priceAnnualyLayout.setVisibility(View.GONE);
							if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
								priceMonthlyLayout.setBackgroundResource(R.drawable.red_border_upgrade_account);
							}
							else{
								priceMonthlyLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.red_border_upgrade_account));
							}
							selectedSubscription = Constants.PAYMENT_CC_MONTH;
						}

						break;
					}
				}

				break;
			}
		}

//			/*RESULTS
//	            p[0] = 1560943707714440503__999___500___1___1___1024 - PRO 1 montly
//        		p[1] = 7472683699866478542__9999___500___12___1___12288 - PRO 1 annually
//        		p[2] = 7974113413762509455__1999___2048___1___2___4096  - PRO 2 montly
//        		p[3] = 370834413380951543__19999___2048___12___2___49152 - PRO 2 annually
//        		p[4] = -2499193043825823892__2999___4096___1___3___8192 - PRO 3 montly
//        		p[5] = 7225413476571973499__29999___4096___12___3___98304 - PRO 3 annually*/

	}

	public void setInfo (int _paymentMethod, int _type){
		this.paymentMethod = _paymentMethod;
		this.parameterType = _type;
	}

	public String sizeTranslation(long size, int type) {
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

	public int onBackPressed(){
		((ManagerActivityLollipop)context).showUpAF();
		return 3;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.monthly_annualy_subscribe_layout: {
				logDebug("Button Subscribe pressed");
				if (selectedSubscription == Constants.PAYMENT_CC_MONTH) {
					logDebug("procced with PAYMENT_CC_MONTH");
					//MONTHLY SUBSCRIPTION
					switch (parameterType) {
						case 1: {
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
					logDebug("procced with PAYMENT_CC_YEAR");
					switch (parameterType) {
						case 1: {
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
			case R.id.monthly_annualy_annualy_layout:
			{
				logDebug("PAYMENT_CC_YEAR selected");
				selectedSubscription = Constants.PAYMENT_CC_YEAR;
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
					priceAnnualyLayout.setBackgroundResource(R.drawable.red_border_upgrade_account);
				}
				else{
					priceAnnualyLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.red_border_upgrade_account));
				}
				priceMonthlyLayout.setBackground(null);
				break;
			}
			case R.id.monthly_annualy_monthly_layout:{
				logDebug("PAYMENT_CC_MONTH selected");
				selectedSubscription = Constants.PAYMENT_CC_MONTH;
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
					priceMonthlyLayout.setBackgroundResource(R.drawable.red_border_upgrade_account);
				}
				else{
					priceMonthlyLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.red_border_upgrade_account));
				}
				priceAnnualyLayout.setBackground(null);
				break;
			}
		}
	}
}
