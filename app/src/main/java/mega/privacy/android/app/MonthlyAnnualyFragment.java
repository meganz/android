package mega.privacy.android.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaPricing;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class MonthlyAnnualyFragment extends Fragment implements MegaRequestListenerInterface, OnClickListener{
	
	public enum AccountType {
		
		FREE(0, R.drawable.ic_free),
		PRO_1(1, R.drawable.ic_pro_1, R.string.pro1_account),
		PRO_2(2, R.drawable.ic_pro_2, R.string.pro2_account),
		PRO_3(3, R.drawable.ic_pro_3, R.string.pro3_account),
		PRO_LITE(4, R.drawable.ic_pro_lite, R.string.prolite_account);
		
		private int id;
		private int resource;
		private int nameResource;
		
		AccountType(int id, int resource) {
			this.id = id;
			this.resource = resource;
		}
		
		AccountType(int id, int resource, int nameResource) {
			this(id, resource);
			this.nameResource = nameResource;
		}
		
		public static AccountType getById(int id) {
			for (AccountType type : AccountType.values()) {
				if (type.id == id) {
					return type;
				}
			}
			return null;
		}
		
		public int getImageResource() {
			return resource;
		}
	
		public int getNameResource() {
			return nameResource;
		}
	}
	
	public static int MY_ACCOUNT_FRAGMENT = 5000;
	public static int UPGRADE_ACCOUNT_FRAGMENT = 5001;
	public static int PAYMENT_FRAGMENT = 5002;
	
	public static int PAYMENT_CC_MONTH = 1;
	public static int PAYMENT_CC_YEAR = 0;
	
	private ActionBar aB;
	private AccountType accountType;
	private ImageView packageIcon;
	private TextView packageName;
	private TextView storage;
	private TextView bandwidth;
	private TextView perMonth;
	private TextView perYear;
	private TextView pricingFrom;
	private TextView storageTitle;
	private TextView bandwithTitle;
	private TextView selectMemberShip;
	private TextView selectRecurring;
	private TextView googlePlaySubscription;
	private TextView perMonthTitle;
	private TextView perYearTitle;
	private TextView comment;
	private RelativeLayout paymentPerMonth;
	private RelativeLayout paymentCreditCard;
	private RelativeLayout paymentPerYear;
	private RelativeLayout paymentFortumo;
	private RelativeLayout paymentCentili;
	private RelativeLayout paymentUpgradeComment;
	private RelativeLayout paymentGoogleWallet;
	int parameterType=-1;	
	MegaApiAndroid megaApi;
	Context context;
	ArrayList<Product> accounts;
	MonthlyAnnualyFragment paymentFragment = this;
	int paymentMonth = -1;
	BitSet paymentBitSet = null;
	int paymentMethod = -1;
	
	public void setInfo (int _paymentMethod, int _type, ArrayList<Product> _accounts, BitSet paymentBitSet){
		this.paymentMethod = _paymentMethod;
		this.accounts = _accounts;
		this.parameterType = _type;
		this.paymentBitSet = paymentBitSet;
	}
	
	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		
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

		DecimalFormat df = new DecimalFormat("#.##");  

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		View v = null;
		v = inflater.inflate(R.layout.activity_upgrade_payment, container, false);

		packageIcon = (ImageView) v.findViewById(R.id.pro_image);

		packageIcon.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
		packageIcon.getLayoutParams().height = Util.px2dp((100*scaleW), outMetrics);

		packageName = (TextView) v.findViewById(R.id.pro_title);
		pricingFrom = (TextView) v.findViewById(R.id.pricing_from);

		perMonthTitle = (TextView) v.findViewById(R.id.per_month);
		perMonth = (TextView) v.findViewById(R.id.per_month_price);
		perYear = (TextView) v.findViewById(R.id.per_year_price);
		perYearTitle = (TextView) v.findViewById(R.id.per_year);

		accountType = AccountType.getById(parameterType);

		packageIcon.setImageResource(accountType.getImageResource());
		packageName.setText(accountType.getNameResource());
		packageName.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));

		pricingFrom.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleH));

		storageTitle = (TextView) v.findViewById(R.id.pro_storage_title);
		storageTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleH));

		storage = (TextView) v.findViewById(R.id.pro_storage);
		storage.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleH));

		bandwithTitle = (TextView) v.findViewById(R.id.pro_bandwidth_title);
		bandwithTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleH));

		bandwidth = (TextView) v.findViewById(R.id.pro_bandwidth);
		bandwidth.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleH));

		selectMemberShip = (TextView) v.findViewById(R.id.select_membership);
		selectMemberShip.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleH));

		selectRecurring = (TextView) v.findViewById(R.id.select_recurring);
		selectRecurring.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleH));
		selectRecurring.setVisibility(View.VISIBLE);

		googlePlaySubscription = (TextView) v.findViewById(R.id.google_play_subscription);
		googlePlaySubscription.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleH));
		googlePlaySubscription.setVisibility(View.GONE);

		perMonth.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
		perMonthTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));

		perYear.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
		perYearTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));

		comment = (TextView) v.findViewById(R.id.comment);
		comment.setTextSize(TypedValue.COMPLEX_UNIT_SP, (12*scaleH));
		
		paymentPerMonth = (RelativeLayout) v.findViewById(R.id.payment_per_month);
		paymentPerYear = (RelativeLayout) v.findViewById(R.id.payment_per_year);
		paymentUpgradeComment = (RelativeLayout) v.findViewById(R.id.payment_upgrade_comment);
		paymentPerMonth.setVisibility(View.VISIBLE);
		paymentPerYear.setVisibility(View.VISIBLE);		
		paymentUpgradeComment.setVisibility(View.VISIBLE);
		paymentPerMonth.setOnClickListener(this);
		paymentPerYear.setOnClickListener(this);
		
		paymentCreditCard = (RelativeLayout) v.findViewById(R.id.payment_credit_card);
		paymentCreditCard.setVisibility(View.GONE);
		
		paymentFortumo = (RelativeLayout) v.findViewById(R.id.payment_fortumo);
		paymentFortumo.setVisibility(View.GONE);
		
		paymentCentili = (RelativeLayout) v.findViewById(R.id.payment_centili);
		paymentCentili.setVisibility(View.GONE);
		
		paymentGoogleWallet = (RelativeLayout) v.findViewById(R.id.payment_google_wallet);
		paymentGoogleWallet.setVisibility(View.GONE);
		
		paymentCreditCard.setOnClickListener(this);
		paymentFortumo.setOnClickListener(this);
		paymentCentili.setOnClickListener(this);
		paymentGoogleWallet.setOnClickListener(this);
		
		paymentMonth=-1;
		
		if (paymentBitSet == null){
			megaApi.getPaymentMethods(this);
		}
		
		if(accounts == null){
			megaApi.getPricing(this);
		}
		else{	
			switch(parameterType){
				case 1:{
					
					
					for (int i=0;i<accounts.size();i++){
						Product account = accounts.get(i);
		
						if(account.getLevel()==1){
							aB.setTitle(getString(R.string.pro1_account));
		
							storage.setText(account.getStorage()+"GB");	
							storage.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							
							if(account.getMonths()==12){
								double perYearF=account.getAmount()/100.00;
								String perYearString =df.format(perYearF);
		
								perYear.setText(perYearString+" €");
								perYear.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							}
							else if(account.getMonths()==1){
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
		
								perMonth.setText(perMonthString+" €");
								perMonth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
								bandwidth.setText(account.getTransfer()/1024 + " TB");
								bandwidth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
								pricingFrom.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							}
						}
					}
					
					paymentPerMonth.setVisibility(View.VISIBLE);
					paymentPerYear.setVisibility(View.VISIBLE);
					
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
				case 2:{
	
					for (int i=0;i<accounts.size();i++){
						Product account = accounts.get(i);
		
						if(account.getLevel()==2){
							aB.setTitle(getString(R.string.pro2_account));
		
							storage.setText(account.getStorage()/1024+"TB");
							storage.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
		
							if(account.getMonths()==12){
								double perYearF=account.getAmount()/100.00;
								String perYearString =df.format(perYearF);
		
								perYear.setText(perYearString+" €");
								perYear.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							}
							else if(account.getMonths()==1){
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
		
								perMonth.setText(perMonthString+" €");
								perMonth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
								bandwidth.setText(sizeTranslation(account.getTransfer(),0));
								bandwidth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
								pricingFrom.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							}
						}
					}
					
					paymentPerMonth.setVisibility(View.VISIBLE);
					paymentPerYear.setVisibility(View.VISIBLE);
					
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
				case 3:{
					
					for (int i=0;i<accounts.size();i++){
		
						Product account = accounts.get(i);
		
						if(account.getLevel()==3){
							aB.setTitle(getString(R.string.pro3_account));
		
							storage.setText(account.getStorage()/1024+"TB");            
							storage.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							
							if(account.getMonths()==12){
								double perYearF=account.getAmount()/100.00;
								String perYearString =df.format(perYearF);
		
								perYear.setText(perYearString+" €");
								perYear.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							}
							else if(account.getMonths()==1){
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
		
								perMonth.setText(perMonthString+" €");
								perMonth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
								bandwidth.setText(sizeTranslation(account.getTransfer(),0));
								bandwidth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
								pricingFrom.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							}
						}
					}
					
					paymentPerMonth.setVisibility(View.VISIBLE);
					paymentPerYear.setVisibility(View.VISIBLE);
					
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
				case 4:{
					for (int i=0;i<accounts.size();i++){
						
						Product account = accounts.get(i);
		
						if(account.getLevel()==4){
							aB.setTitle(getString(R.string.prolite_account));
		
							storage.setText(account.getStorage()+"GB");	
							storage.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
		
							if(account.getMonths()==12){
								double perYearF=account.getAmount()/100.00;
								String perYearString =df.format(perYearF);
		
								perYear.setText(perYearString+" €");
								perYear.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
							}
							else if(account.getMonths()==1){
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
		
								perMonth.setText(perMonthString+" €");
								perMonth.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
								bandwidth.setText(account.getTransfer()/1024 + " TB");
								bandwidth.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
								pricingFrom.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
							}
						}
					}
					
					switch (paymentMethod){
						case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
							paymentPerMonth.setVisibility(View.VISIBLE);
							paymentPerYear.setVisibility(View.GONE);
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
							paymentPerMonth.setVisibility(View.VISIBLE);
							paymentPerYear.setVisibility(View.GONE);
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
							paymentPerMonth.setVisibility(View.VISIBLE);
							paymentPerYear.setVisibility(View.VISIBLE);
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
							paymentPerMonth.setVisibility(View.VISIBLE);
							paymentPerYear.setVisibility(View.VISIBLE);
							break;
						}
					}
					
					break;
				}
			}
		}

		return v;
	}	
	
	public void payYear() {
		log("yearly");
		
		paymentMonth = 0;
		
		((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true, paymentBitSet);
		paymentMonth = -1;
	}
	
	public void payMonth() {
		log("monthly");
		
		paymentMonth = 1;
		
		((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true, paymentBitSet);
		paymentMonth = -1;
	}
	
	public void setInfo (int _type, ArrayList<Product> _accounts, BitSet paymentBitSet){
		this.accounts = _accounts;
		this.parameterType = _type;
		this.paymentBitSet = paymentBitSet;
	}
	

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
		
		log("onRequestFinish: " + request.getRequestString());
		
		if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_METHODS){

			paymentBitSet = Util.convertToBitSet(request.getNumber()); 
		}	
		if (request.getType() == MegaRequest.TYPE_GET_PRICING){
			MegaPricing p = request.getPricing();
			DecimalFormat df = new DecimalFormat("#.##"); 
			
			accounts = new ArrayList<Product>(); 

			for (int i=0;i<p.getNumProducts();i++){
				log("p["+ i +"] = " + p.getHandle(i) + "__" + p.getAmount(i) + "___" + p.getGBStorage(i) + "___" + p.getMonths(i) + "___" + p.getProLevel(i) + "___" + p.getGBTransfer(i));

				Product account = new Product (p.getHandle(i), p.getProLevel(i), p.getMonths(i), p.getGBStorage(i), p.getAmount(i), p.getGBTransfer(i));
				accounts.add(account);				
			} 
			
			switch(parameterType){
				case 1:{
					
					
					for (int i=0;i<accounts.size();i++){
						Product account = accounts.get(i);
		
						if(account.getLevel()==1){
							aB.setTitle(getString(R.string.pro1_account));
		
							storage.setText(account.getStorage()+"GB");	
							storage.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							
							if(account.getMonths()==12){
								double perYearF=account.getAmount()/100.00;
								String perYearString =df.format(perYearF);
		
								perYear.setText(perYearString+" €");
								perYear.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							}
							else if(account.getMonths()==1){
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
		
								perMonth.setText(perMonthString+" €");
								perMonth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
								bandwidth.setText(account.getTransfer()/1024 + " TB");
								bandwidth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
								pricingFrom.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							}
						}
					}
					
					paymentPerMonth.setVisibility(View.VISIBLE);
					paymentPerYear.setVisibility(View.VISIBLE);
					
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
				case 2:{
	
					for (int i=0;i<accounts.size();i++){
						Product account = accounts.get(i);
		
						if(account.getLevel()==2){
							aB.setTitle(getString(R.string.pro2_account));
		
							storage.setText(account.getStorage()/1024+"TB");
							storage.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
		
							if(account.getMonths()==12){
								double perYearF=account.getAmount()/100.00;
								String perYearString =df.format(perYearF);
		
								perYear.setText(perYearString+" €");
								perYear.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							}
							else if(account.getMonths()==1){
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
		
								perMonth.setText(perMonthString+" €");
								perMonth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
								bandwidth.setText(sizeTranslation(account.getTransfer(),0));
								bandwidth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
								pricingFrom.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							}
						}
					}
					
					paymentPerMonth.setVisibility(View.VISIBLE);
					paymentPerYear.setVisibility(View.VISIBLE);
					
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
				case 3:{
					
					for (int i=0;i<accounts.size();i++){
		
						Product account = accounts.get(i);
		
						if(account.getLevel()==3){
							aB.setTitle(getString(R.string.pro3_account));
		
							storage.setText(account.getStorage()/1024+"TB");            
							storage.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							
							if(account.getMonths()==12){
								double perYearF=account.getAmount()/100.00;
								String perYearString =df.format(perYearF);
		
								perYear.setText(perYearString+" €");
								perYear.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							}
							else if(account.getMonths()==1){
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
		
								perMonth.setText(perMonthString+" €");
								perMonth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
								bandwidth.setText(sizeTranslation(account.getTransfer(),0));
								bandwidth.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
								pricingFrom.setTextColor(context.getResources().getColor(R.color.lollipop_primary_color));
							}
						}
					}
					
					paymentPerMonth.setVisibility(View.VISIBLE);
					paymentPerYear.setVisibility(View.VISIBLE);
					
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
				case 4:{
					for (int i=0;i<accounts.size();i++){
						
						Product account = accounts.get(i);
		
						if(account.getLevel()==4){
							aB.setTitle(getString(R.string.prolite_account));
		
							storage.setText(account.getStorage()+"GB");	
							storage.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
		
							if(account.getMonths()==12){
								double perYearF=account.getAmount()/100.00;
								String perYearString =df.format(perYearF);
		
								perYear.setText(perYearString+" €");
								perYear.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
							}
							else if(account.getMonths()==1){
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
		
								perMonth.setText(perMonthString+" €");
								perMonth.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
								bandwidth.setText(account.getTransfer()/1024 + " TB");
								bandwidth.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
								pricingFrom.setTextColor(context.getResources().getColor(R.color.upgrade_orange));
							}
						}
					}
					
					switch (paymentMethod){
						case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
							paymentPerMonth.setVisibility(View.VISIBLE);
							paymentPerYear.setVisibility(View.GONE);
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
							paymentPerMonth.setVisibility(View.VISIBLE);
							paymentPerYear.setVisibility(View.GONE);
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
							paymentPerMonth.setVisibility(View.VISIBLE);
							paymentPerYear.setVisibility(View.VISIBLE);
							break;
						}
						case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
							paymentPerMonth.setVisibility(View.VISIBLE);
							paymentPerYear.setVisibility(View.VISIBLE);
							break;
						}
					}
					
					break;
				}
			}
		}
			
////			/*RESULTS
////	            p[0] = 1560943707714440503__999___500___1___1___1024 - PRO 1 montly
////        		p[1] = 7472683699866478542__9999___500___12___1___12288 - PRO 1 annually
////        		p[2] = 7974113413762509455__1999___2048___1___2___4096  - PRO 2 montly
////        		p[3] = 370834413380951543__19999___2048___12___2___49152 - PRO 2 annually
////        		p[4] = -2499193043825823892__2999___4096___1___3___8192 - PRO 3 montly
////        		p[5] = 7225413476571973499__29999___4096___12___3___98304 - PRO 3 annually*/
	}
	
	public String sizeTranslation(long size, int type) {
		switch(type){
			case 0:{
				//From GB to TB
				if(size!=1024){
					size=size/1024;
				}
								
				String value = new DecimalFormat("#").format(size) + "TB";			
				return value;
			}
		}
		return null;
	      
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		// TODO Auto-generated method stub
		
	}
	
	public int onBackPressed(){
		((ManagerActivity)context).showpF(parameterType, accounts, paymentBitSet);
		
		return 3;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
		aB = ((ActionBarActivity)activity).getSupportActionBar();
	}
	
	public static void log(String message) {
		Util.log("MonthlyAnnualyFragment", message);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		
			case R.id.payment_per_month:{
				switch(parameterType){
					case 1:{
						switch (paymentMethod){
							case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
								((ManagerActivity)context).showCC(parameterType, accounts, PAYMENT_CC_MONTH, true, paymentBitSet);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_I_MONTH);
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
								((ManagerActivity)context).showCC(parameterType, accounts, PAYMENT_CC_MONTH, true, paymentBitSet);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_II_MONTH);
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
								((ManagerActivity)context).showCC(parameterType, accounts, PAYMENT_CC_MONTH, true, paymentBitSet);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_III_MONTH);
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
								((ManagerActivity)context).showCC(parameterType, accounts, PAYMENT_CC_MONTH, true, paymentBitSet);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_LITE_MONTH);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
								((ManagerActivity)context).showFortumo();
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
								((ManagerActivity)context).showCentili();
								break;
							}
						}
						break;
					}
				}
				break;
			}
			case R.id.payment_per_year:{
				switch(parameterType){
					case 1:{
						switch (paymentMethod){
							case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
								((ManagerActivity)context).showCC(parameterType, accounts, PAYMENT_CC_YEAR, true, paymentBitSet);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_I_YEAR);
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
								((ManagerActivity)context).showCC(parameterType, accounts, PAYMENT_CC_YEAR, true, paymentBitSet);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_II_YEAR);
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
								((ManagerActivity)context).showCC(parameterType, accounts, PAYMENT_CC_YEAR, true, paymentBitSet);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_III_YEAR);
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
								((ManagerActivity)context).showCC(parameterType, accounts, PAYMENT_CC_YEAR, true, paymentBitSet);
								break;
							}
							case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
								((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_LITE_YEAR);
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