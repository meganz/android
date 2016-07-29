package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.DBUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class MonthlyAnnualyFragmentLollipop extends Fragment implements OnClickListener{
	
	public static int MY_ACCOUNT_FRAGMENT = 5000;
	public static int UPGRADE_ACCOUNT_FRAGMENT = 5001;
	public static int PAYMENT_FRAGMENT = 5002;
	
	public static int PAYMENT_CC_MONTH = 1;
	public static int PAYMENT_CC_YEAR = 0;
	
	
	private TextView title;
	private TextView perMonth;
	private TextView priceInteger;
	private TextView priceDecimal;
	private TextView priceMonthlyInteger;
	private TextView priceMonthlyDecimal;
	private TextView priceAnnualyInteger;
	private TextView priceAnnualyDecimal;
	private LinearLayout priceSeparator;
	TextView selectComment;
	private RelativeLayout priceAnnualyLayout;
	private RelativeLayout priceMonthlyLayout;
	private LinearLayout subscribeSeparator;
	private RelativeLayout subscribeAnnualyLayout;
	private RelativeLayout subscribeMonthlyLayout;
	private TextView storageInteger;
	private TextView storageGb;
	private TextView bandwidthInteger;
	private TextView bandwidthTb;
	
	TextView monthlyLabel;
	TextView annualyLabel;
	
	private TextView selectMonthYear;
	
	private ActionBar aB;
	
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
		log("onCreate");
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

		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		View v = null;
		v = inflater.inflate(R.layout.activity_upgrade_monthly_annualy, container, false);
		
		title = (TextView) v.findViewById(R.id.monthly_annualy_title_text);
		perMonth = (TextView) v.findViewById(R.id.monthly_annualy_per_month_text);
		perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));
		priceInteger = (TextView) v.findViewById(R.id.monthly_annualy_integer_text);
		priceDecimal = (TextView) v.findViewById(R.id.monthly_annualy_decimal_text);
		priceMonthlyInteger = (TextView) v.findViewById(R.id.monthly_annualy_price_monthly_integer_text);
		priceMonthlyDecimal = (TextView) v.findViewById(R.id.monthly_annualy_price_monthly_decimal_text);
		priceAnnualyInteger = (TextView) v.findViewById(R.id.monthly_annualy_price_annualy_integer_text);
		priceAnnualyDecimal = (TextView) v.findViewById(R.id.monthly_annualy_price_annualy_decimal_text);
		priceSeparator = (LinearLayout) v.findViewById(R.id.monthly_annualy_price_separator);
		selectComment = (TextView) v.findViewById(R.id.monthly_annualy_select_comment);
		priceMonthlyLayout = (RelativeLayout) v.findViewById(R.id.monthly_annualy_monthly_layout);
		priceAnnualyLayout = (RelativeLayout) v.findViewById(R.id.monthly_annualy_annualy_layout);
		subscribeSeparator = (LinearLayout) v.findViewById(R.id.monthly_annualy_subscribe_separator);
		subscribeMonthlyLayout = (RelativeLayout) v.findViewById(R.id.monthly_annualy_subscribe_monthly);
		subscribeAnnualyLayout = (RelativeLayout) v.findViewById(R.id.monthly_annualy_subscribe_annualy);
		monthlyLabel = (TextView) v.findViewById(R.id.monthly_annualy_monthly_text);
		monthlyLabel.setText(getString(R.string.upgrade_per_month).toUpperCase(Locale.getDefault()));
		annualyLabel = (TextView) v.findViewById(R.id.monthly_annualy_annualy_text);
		annualyLabel.setText(getString(R.string.upgrade_per_year).toUpperCase(Locale.getDefault()));
		priceSeparator.setVisibility(View.VISIBLE);
		priceAnnualyLayout.setVisibility(View.VISIBLE);
		subscribeSeparator.setVisibility(View.VISIBLE);
		subscribeAnnualyLayout.setVisibility(View.VISIBLE);
		
		priceMonthlyLayout.setOnClickListener(this);
		priceAnnualyLayout.setOnClickListener(this);
		subscribeMonthlyLayout.setOnClickListener(this);
		subscribeAnnualyLayout.setOnClickListener(this);
		
		storageInteger = (TextView) v.findViewById(R.id.monthly_annualy_storage_value_integer);
		storageGb = (TextView) v.findViewById(R.id.monthly_annualy_storage_value_gb);
		bandwidthInteger = (TextView) v.findViewById(R.id.monthly_annualy_bandwidth_value_integer);
		bandwidthTb = (TextView) v.findViewById(R.id.monthly_annualy_bandwith_value_tb);
		
		selectMonthYear = (TextView) v.findViewById(R.id.monthly_annualy_select);

		setPricing();

		refreshAccountInfo();

		return v;
	}

	public void refreshAccountInfo(){
		log("refreshAccountInfo");

		log("Check the last call to callToPricing");
		if(DBUtil.callToPricing(context)){
			log("megaApi.getPricing SEND");
			megaApi.getPricing(myAccountInfo);
		}

		log("Check the last call to callToPaymentMethods");
		if(DBUtil.callToPaymentMethods(context)){
			log("megaApi.getPaymentMethods SEND");
			megaApi.getPaymentMethods(myAccountInfo);
		}
	}

	public void setPricing(){
		log("setPricing");

		DecimalFormat df = new DecimalFormat("#.##");

		ArrayList<Product> accounts = myAccountInfo.getProductAccounts();

		if (accounts == null){
			log("accounts == null");
			megaApi.getPricing(myAccountInfo);
			return;
		}

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

						storageInteger.setText(""+account.getStorage());
						storageGb.setText(" GB");

						bandwidthInteger.setText(""+account.getTransfer()/1024);
						bandwidthTb.setText(" TB");

						title.setText(getString(R.string.pro1_account));

						storageInteger.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
						storageGb.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
						bandwidthInteger.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
						bandwidthTb.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
						perMonth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
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
							log("ProIMonthly already subscribed: " + myAccountInfo.getProIMonthly().getOriginalJson());
							priceSeparator.setVisibility(View.GONE);
							priceMonthlyLayout.setVisibility(View.GONE);
							subscribeSeparator.setVisibility(View.GONE);
							subscribeMonthlyLayout.setVisibility(View.GONE);
						}

						if (myAccountInfo.getProIYearly() != null) {
							log("ProIAnnualy already subscribed: " + myAccountInfo.getProIYearly().getOriginalJson());
							priceSeparator.setVisibility(View.GONE);
							priceAnnualyLayout.setVisibility(View.GONE);
							subscribeSeparator.setVisibility(View.GONE);
							subscribeAnnualyLayout.setVisibility(View.GONE);
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

						storageInteger.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
						storageGb.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
						bandwidthInteger.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
						bandwidthTb.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
						perMonth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
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
							log("ProIIMonthly already subscribed: " + myAccountInfo.getProIIMonthly().getOriginalJson());
							priceSeparator.setVisibility(View.GONE);
							priceMonthlyLayout.setVisibility(View.GONE);
							subscribeSeparator.setVisibility(View.GONE);
							subscribeMonthlyLayout.setVisibility(View.GONE);
						}

						if (myAccountInfo.getProIIYearly() != null) {
							log("ProIIAnnualy already subscribed: " + myAccountInfo.getProIIYearly().getOriginalJson());
							priceSeparator.setVisibility(View.GONE);
							priceAnnualyLayout.setVisibility(View.GONE);
							subscribeSeparator.setVisibility(View.GONE);
							subscribeAnnualyLayout.setVisibility(View.GONE);
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

						storageInteger.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
						storageGb.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
						bandwidthInteger.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
						bandwidthTb.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
						perMonth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
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
							log("ProIIIMonthly already subscribed: " + myAccountInfo.getProIIIMonthly().getOriginalJson());
							priceSeparator.setVisibility(View.GONE);
							priceMonthlyLayout.setVisibility(View.GONE);
							subscribeSeparator.setVisibility(View.GONE);
							subscribeMonthlyLayout.setVisibility(View.GONE);
						}

						if (myAccountInfo.getProIIIYearly() != null) {
							log("ProIIIAnnualy already subscribed: " + myAccountInfo.getProIIIYearly().getOriginalJson());
							priceSeparator.setVisibility(View.GONE);
							priceAnnualyLayout.setVisibility(View.GONE);
							subscribeSeparator.setVisibility(View.GONE);
							subscribeAnnualyLayout.setVisibility(View.GONE);
						}
						break;
					}
				}

				break;
			}
			case 4:{
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

						storageInteger.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
						storageGb.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
						bandwidthInteger.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
						bandwidthTb.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
						perMonth.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
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
						priceSeparator.setVisibility(View.GONE);
						priceAnnualyLayout.setVisibility(View.GONE);
						subscribeSeparator.setVisibility(View.GONE);
						subscribeAnnualyLayout.setVisibility(View.GONE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
						priceSeparator.setVisibility(View.GONE);
						priceAnnualyLayout.setVisibility(View.GONE);
						subscribeSeparator.setVisibility(View.GONE);
						subscribeAnnualyLayout.setVisibility(View.GONE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
						if (myAccountInfo.getProLiteMonthly() != null) {
							log("ProLiteMonthly already subscribed: " + myAccountInfo.getProLiteMonthly().getOriginalJson());
							priceSeparator.setVisibility(View.GONE);
							priceMonthlyLayout.setVisibility(View.GONE);
							subscribeSeparator.setVisibility(View.GONE);
							subscribeMonthlyLayout.setVisibility(View.GONE);
						}

						if (myAccountInfo.getProLiteYearly() != null) {
							log("ProLiteAnnualy already subscribed: " + myAccountInfo.getProLiteYearly().getOriginalJson());
							priceSeparator.setVisibility(View.GONE);
							priceAnnualyLayout.setVisibility(View.GONE);
							subscribeSeparator.setVisibility(View.GONE);
							subscribeAnnualyLayout.setVisibility(View.GONE);
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

	public void setInfo (int _paymentMethod, int _type, MyAccountInfo _myAccountInfo){
		this.paymentMethod = _paymentMethod;
		this.myAccountInfo = _myAccountInfo;
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
		((ManagerActivityLollipop)context).showUpAF(-1);
		return 3;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
		aB = ((AppCompatActivity)activity).getSupportActionBar();
	}
	
	public static void log(String message) {
		Util.log("MonthlyAnnualyFragmentLollipop", message);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.monthly_annualy_monthly_layout:
			case R.id.monthly_annualy_subscribe_monthly:{
				switch(parameterType){
					case 1:{
						switch (paymentMethod){
							case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
								((ManagerActivityLollipop)context).showCC(parameterType, myAccountInfo, PAYMENT_CC_MONTH, true);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_I_MONTH);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
								break;
							}
						}
						break;
					}
					case 2:{
						switch (paymentMethod){
							case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
								((ManagerActivityLollipop)context).showCC(parameterType, myAccountInfo, PAYMENT_CC_MONTH, true);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_II_MONTH);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
								break;
							}
						}
						break;
					}
					case 3:{
						switch (paymentMethod){
							case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
								((ManagerActivityLollipop)context).showCC(parameterType, myAccountInfo, PAYMENT_CC_MONTH, true);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_III_MONTH);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
								break;
							}
						}
						break;
					}
					case 4:{
						switch (paymentMethod){
							case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
								((ManagerActivityLollipop)context).showCC(parameterType, myAccountInfo, PAYMENT_CC_MONTH, true);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_LITE_MONTH);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
								((ManagerActivityLollipop)context).showFortumo();
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
								((ManagerActivityLollipop)context).showCentili();
								break;
							}
						}
						break;
					}
				}
				break;
			}
			case R.id.monthly_annualy_annualy_layout:
			case R.id.monthly_annualy_subscribe_annualy:{
				switch(parameterType){
					case 1:{
						switch (paymentMethod){
							case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
								((ManagerActivityLollipop)context).showCC(parameterType, myAccountInfo, PAYMENT_CC_YEAR, true);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_I_YEAR);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
								break;
							}
						}
						break;
					}
					case 2:{
						switch (paymentMethod){
							case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
								((ManagerActivityLollipop)context).showCC(parameterType, myAccountInfo, PAYMENT_CC_YEAR, true);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_II_YEAR);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
								break;
							}
						}
						break;
					}
					case 3:{
						switch (paymentMethod){
							case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
								((ManagerActivityLollipop)context).showCC(parameterType, myAccountInfo, PAYMENT_CC_YEAR, true);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_III_YEAR);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
								break;
							}
						}
						break;
					}
					case 4:{
						switch (paymentMethod){
							case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
								((ManagerActivityLollipop)context).showCC(parameterType, myAccountInfo, PAYMENT_CC_YEAR, true);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_LITE_YEAR);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
								break;
							}
						}
						break;
					}
				}
				break;
			}
		}
	}
}
