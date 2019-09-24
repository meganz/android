package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class CreditCardFragmentLollipop extends Fragment implements MegaRequestListenerInterface, OnClickListener, OnItemSelectedListener{
	
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
	
	private ActionBar aB;
	private AccountType accountType;
	private ImageView packageIcon;
	private TextView packageName;
	private TextView storage;
	private TextView bandwidth;
	private TextView billingDetails;
	private EditText address1Edit;
	String address1String = "";
	private EditText address2Edit;
	String address2String = "";
	private EditText cityEdit;
	String cityString = "";
	private EditText stateEdit;
	String stateString = "";
	private Spinner countrySpinner;
	String countryCode = "";
	private EditText postalCodeEdit;
	String postalCodeString = "";
	private TextView paymentDetails;
	private EditText firstNameEdit;
	String firstNameString = "";
	private EditText lastNameEdit;
	String lastNameString = "";
	private EditText creditCardNumberEdit;
	String creditCardNumberString = "";
	private Spinner monthSpinner;
	String monthString = "";
	private Spinner yearSpinner;
	String yearString = "";
	private EditText cvvEdit;
	String cvvString = "";
	private Button cancelButton;
	private Button proceedButton;
	private String productHandle = null;
	private long productHandleLong;
	//	private TextView perMonth;
//	private TextView perYear;
	private TextView pricingFrom;
	private TextView storageTitle;
	private TextView bandwithTitle;
//	private TextView selectMemberShip;
//	private TextView selectRecurring;
//	private TextView googlePlaySubscription;
//	private TextView perMonthTitle;
//	private TextView perYearTitle;
//	private TextView comment;
//	private RelativeLayout paymentPerMonth;
//	private RelativeLayout paymentCreditCard;
//	private RelativeLayout paymentPerYear;
//	private RelativeLayout paymentFortumo;
//	private RelativeLayout paymentUpgradeComment;
//	private RelativeLayout paymentGoogleWallet;
	int parameterType;	
	MegaApiAndroid megaApi;
	Context context;
	MyAccountInfo myAccountInfo;
	CreditCardFragmentLollipop paymentFragment = this;
	int paymentMonth = -1;

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
		logDebug("onCreate");
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

		float scaleW = getScaleW(outMetrics, density);
		float scaleH = getScaleH(outMetrics, density);

		View v = null;
		v = inflater.inflate(R.layout.activity_credit_card_payment, container, false);

		packageIcon = (ImageView) v.findViewById(R.id.pro_image_cc);

		packageIcon.getLayoutParams().width = px2dp((100*scaleW), outMetrics);
		packageIcon.getLayoutParams().height = px2dp((100*scaleW), outMetrics);

		packageName = (TextView) v.findViewById(R.id.pro_title_cc);
		pricingFrom = (TextView) v.findViewById(R.id.pricing_from_cc);

//		perMonthTitle = (TextView) v.findViewById(R.id.per_month);
//		perMonth = (TextView) v.findViewById(R.id.per_month_price);
//		perYear = (TextView) v.findViewById(R.id.per_year_price);
//		perYearTitle = (TextView) v.findViewById(R.id.per_year);
//
		accountType = AccountType.getById(parameterType);
//
		packageIcon.setImageResource(accountType.getImageResource());
		packageName.setText(accountType.getNameResource());
		packageName.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));

		pricingFrom.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleH));

		storageTitle = (TextView) v.findViewById(R.id.pro_storage_title_cc);
		storageTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleH));

		storage = (TextView) v.findViewById(R.id.pro_storage_cc);
		storage.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleH));

		bandwithTitle = (TextView) v.findViewById(R.id.pro_bandwidth_title_cc);
		bandwithTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleH));

		bandwidth = (TextView) v.findViewById(R.id.pro_bandwidth_cc);
		bandwidth.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleH));
		
		billingDetails = (TextView) v.findViewById(R.id.billing_details_cc);
		billingDetails.setText(getString(R.string.billing_details));
		billingDetails.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleH));
		
		address1Edit = (EditText) v.findViewById(R.id.address1_cc);
		address2Edit = (EditText) v.findViewById(R.id.address2_cc);
		cityEdit = (EditText) v.findViewById(R.id.city_cc);
		stateEdit = (EditText) v.findViewById(R.id.state_cc);
		postalCodeEdit = (EditText) v.findViewById(R.id.postal_code_cc);
		
		countrySpinner = (Spinner) v.findViewById(R.id.country_cc);
		List<String> countryList = getCountryList(context);
		// Populate the spinner using a customized ArrayAdapter that hides the first (dummy) entry
		ArrayAdapter<String> countryAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, countryList){
			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent){
				View v = null;
				
				// If this is the initial dummy entry, make it hidden
		        if (position == 0) {
		            TextView tv = new TextView(getContext());
		            tv.setHeight(0);
		            tv.setVisibility(View.GONE);
		            v = tv;
		        }
		        else {
		            // Pass convertView as null to prevent reuse of special case views
		            v = super.getDropDownView(position, null, parent);
		            ((TextView)v).setTextColor(Color.BLACK);
		        }
		        
		        // Hide scroll bar because it appears sometimes unnecessarily, this does not prevent scrolling 
		        parent.setVerticalScrollBarEnabled(false);
		        return v;
			}
			
		};
		countrySpinner.setOnItemSelectedListener(this);
		countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		countrySpinner.setAdapter(countryAdapter);
		
		paymentDetails = (TextView) v.findViewById(R.id.payment_details_cc);
		paymentDetails.setText(getString(R.string.payment_details));
		paymentDetails.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleH));
		
		firstNameEdit = (EditText) v.findViewById(R.id.first_name_cc);
		lastNameEdit = (EditText) v.findViewById(R.id.last_name_cc);
		creditCardNumberEdit = (EditText) v.findViewById(R.id.credit_card_number_cc);
		
		monthSpinner = (Spinner) v.findViewById(R.id.month_cc);
		List<String> monthList = getMonthListInt(context);
		// Populate the spinner using a customized ArrayAdapter that hides the first (dummy) entry
		ArrayAdapter<String> monthAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, monthList){
			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent){
				View v = null;
				
				// If this is the initial dummy entry, make it hidden
		        if (position == 0) {
		            TextView tv = new TextView(getContext());
		            tv.setHeight(0);
		            tv.setVisibility(View.GONE);
		            v = tv;
		        }
		        else {
		            // Pass convertView as null to prevent reuse of special case views
		            v = super.getDropDownView(position, null, parent);
		            ((TextView)v).setTextColor(Color.BLACK);
		        }
		        
		        // Hide scroll bar because it appears sometimes unnecessarily, this does not prevent scrolling 
		        parent.setVerticalScrollBarEnabled(false);
		        return v;
			}
			
		};
		monthSpinner.setOnItemSelectedListener(this);
		monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		monthSpinner.setAdapter(monthAdapter);
		
		yearSpinner = (Spinner) v.findViewById(R.id.year_cc);
		List<String> yearList = getYearListInt(context);
		// Populate the spinner using a customized ArrayAdapter that hides the first (dummy) entry
		ArrayAdapter<String> yearAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, yearList){
			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent){
				View v = null;
				
				// If this is the initial dummy entry, make it hidden
		        if (position == 0) {
		            TextView tv = new TextView(getContext());
		            tv.setHeight(0);
		            tv.setVisibility(View.GONE);
		            v = tv;
		        }
		        else {
		            // Pass convertView as null to prevent reuse of special case views
		            v = super.getDropDownView(position, null, parent);
		            ((TextView)v).setTextColor(Color.BLACK);
		        }
		        
		        // Hide scroll bar because it appears sometimes unnecessarily, this does not prevent scrolling 
		        parent.setVerticalScrollBarEnabled(false);
		        return v;
			}
			
		};
		yearSpinner.setOnItemSelectedListener(this);
		yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		yearSpinner.setAdapter(yearAdapter);
		
		cvvEdit = (EditText) v.findViewById(R.id.cvv_cc);
		
		cancelButton = (Button) v.findViewById(R.id.cancel_cc);
		cancelButton.setOnClickListener(this);
		proceedButton = (Button) v.findViewById(R.id.proceed_cc);
		proceedButton.setOnClickListener(this);
		
//
//		selectMemberShip = (TextView) v.findViewById(R.id.select_membership);
//		selectMemberShip.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleH));
//
//		selectRecurring = (TextView) v.findViewById(R.id.select_recurring);
//		selectRecurring.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleH));
//		selectRecurring.setVisibility(View.VISIBLE);
//
//		googlePlaySubscription = (TextView) v.findViewById(R.id.google_play_subscription);
//		googlePlaySubscription.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleH));
//		googlePlaySubscription.setVisibility(View.VISIBLE);
//
//		perMonth.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
//		perMonthTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
//
//		perYear.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
//		perYearTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
//
//		comment = (TextView) v.findViewById(R.id.comment);
//		comment.setTextSize(TypedValue.COMPLEX_UNIT_SP, (12*scaleH));
//		
//		paymentPerMonth = (RelativeLayout) v.findViewById(R.id.payment_per_month);
//		paymentPerYear = (RelativeLayout) v.findViewById(R.id.payment_per_year);
//		paymentUpgradeComment = (RelativeLayout) v.findViewById(R.id.payment_upgrade_comment);
//		paymentPerMonth.setVisibility(View.VISIBLE);
//		paymentPerYear.setVisibility(View.VISIBLE);		
//		paymentUpgradeComment.setVisibility(View.VISIBLE);
//		
//		paymentCreditCard = (RelativeLayout) v.findViewById(R.id.payment_credit_card);
//		paymentCreditCard.setVisibility(View.GONE);
//		
//		paymentFortumo = (RelativeLayout) v.findViewById(R.id.payment_fortumo);
//		paymentFortumo.setVisibility(View.GONE);
//		
//		paymentGoogleWallet = (RelativeLayout) v.findViewById(R.id.payment_google_wallet);
//		paymentGoogleWallet.setVisibility(View.GONE);
//		
//		paymentCreditCard.setOnClickListener(this);
//		paymentFortumo.setOnClickListener(this);
//		paymentGoogleWallet.setOnClickListener(this);

		ArrayList<Product> accounts = myAccountInfo.getProductAccounts();
		switch (parameterType) {

			case 1:{

				for (int i=0;i<accounts.size();i++){
	
					Product account = accounts.get(i);
	
					if(account.getLevel()==1){
						aB.setTitle(getString(R.string.pro1_account));
	
						if (paymentMonth == 1){
							if(account.getMonths()==1){
								storage.setText(account.getStorage()/1024+" TB");
								
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
								bandwidth.setText(account.getTransfer()/1024 + " TB");
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
							}
						}
						else if (paymentMonth == 0){
							if (account.getMonths() == 1){
								bandwidth.setText(account.getTransfer()/1024 + " TB");
							}
							if(account.getMonths()==12){
								storage.setText(account.getStorage()/1024+" TB");
								
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_year));
							}
						}
					}
				}
	
				break;
			}
			case 2:{
	
				for (int i=0;i<accounts.size();i++){
	
					Product account = accounts.get(i);
	
					if(account.getLevel()==2){
						aB.setTitle(getString(R.string.pro2_account));
	
						if (paymentMonth == 1){
							if(account.getMonths()==1){
								storage.setText(account.getStorage()/1024+"TB");		            
								
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
								bandwidth.setText(account.getTransfer()/1024 + " TB");
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
							}
						}
						else if (paymentMonth == 0){
							if (account.getMonths() == 1){
								bandwidth.setText(account.getTransfer()/1024 + " TB");
							}
							if(account.getMonths()==12){
								storage.setText(account.getStorage()/1024+"TB");		            
								
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_year));
							}
						}
					}
				}
	
				break;
			}
			case 3:{
				for (int i=0;i<accounts.size();i++){
	
					Product account = accounts.get(i);
	
					if(account.getLevel()==3){
						aB.setTitle(getString(R.string.pro3_account));
	
						if (paymentMonth == 1){
							if(account.getMonths()==1){
								storage.setText(account.getStorage()/1024+"TB");		            
								
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
								bandwidth.setText(account.getTransfer()/1024 + " TB");
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
							}
						}
						else if (paymentMonth == 0){
							if (account.getMonths() == 1){
								bandwidth.setText(account.getTransfer()/1024 + " TB");
							}
							if(account.getMonths()==12){
								storage.setText(account.getStorage()/1024+"TB");		            
								
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_year));
							}
						}
					}
				}
				break;
			}
			
			case 4:{
				
				for (int i=0;i<accounts.size();i++){
	
					Product account = accounts.get(i);
	
					if(account.getLevel()==4){
						aB.setTitle(getString(R.string.prolite_account));
	
						if (paymentMonth == 1){
							if(account.getMonths()==1){
								storage.setText(account.getStorage()+"GB");		            
								
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
								bandwidth.setText(account.getTransfer()/1024 + " TB");
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
							}
						}
						else if (paymentMonth == 0){
							if (account.getMonths() == 1){
								bandwidth.setText(account.getTransfer()/1024 + " TB");
							}
							if(account.getMonths()==12){
								storage.setText(account.getStorage()+"GB");		            
								
								double perMonthF=account.getAmount()/100.00;
								String perMonthString =df.format(perMonthF);
								pricingFrom.setText(perMonthString + " € " + getString(R.string.per_year));
							}
						}
					}
				}
				break;
			}
		}
//
//		//		megaApi.getPricing(myAccountInfo);

		return v;
	}	
	
	public void payYear() {
		logDebug("yearly");
		
		paymentMonth = 0;
		
		switch(parameterType){
		
			case 1:{
				
				((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_I_YEAR);
				
//				for (int i=0;i<accounts.size();i++){
//					
//					Product account = accounts.get(i);
//	
//					if(account.getLevel()==1&&account.getMonths()==12){
//						
//						megaApi.getPaymentUrl(account.getHandle(),this);	
//					}
//				}
				break;
			}
			case 2:{
//				for (int i=0;i<accounts.size();i++){
//					
//					Product account = accounts.get(i);
//	
//					if(account.getLevel()==2&&account.getMonths()==12){
//						
//						megaApi.getPaymentUrl(account.getHandle(),this);	
//					}
//				}
				break;
			}
			case 3:{
//				for (int i=0;i<accounts.size();i++){
//					
//					Product account = accounts.get(i);
//	
//					if(account.getLevel()==3&&account.getMonths()==12){
//						
//						megaApi.getPaymentUrl(account.getHandle(),this);	
//					}
//				}
				break;
			}			
			case 4:{
				((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_LITE_YEAR);
				break;
			}
			
		}
	}
	
	public void payMonth() {
//		log("monthly");
//		
//		paymentMonth = 1;
//		
//		selectMemberShip.setText(getString(R.string.select_payment_method));
//		selectRecurring.setVisibility(View.GONE);
//		googlePlaySubscription.setVisibility(View.GONE);
//		
//		switch(parameterType){
//			case 4:{
//				paymentPerMonth.setVisibility(View.GONE);
//				paymentPerYear.setVisibility(View.GONE);
//				paymentUpgradeComment.setVisibility(View.GONE);
//				paymentCreditCard.setVisibility(View.VISIBLE);
//				paymentFortumo.setVisibility(View.VISIBLE);
//				paymentGoogleWallet.setVisibility(View.VISIBLE);
//				break;
//			}
//		}
//
////		switch(parameterType){
////		
////			case 1:{
////				
////				((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_I_MONTH);
////				
//////				for (int i=0;i<accounts.size();i++){
//////					
//////					Product account = accounts.get(i);
//////	
//////					if(account.getLevel()==1&&account.getMonths()==1){
//////						
//////						megaApi.getPaymentUrl(account.getHandle(),this);	
//////					}
//////				}
////				break;
////
////			}
////			case 2:{
////				
////				((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_II_MONTH);
////				
//////				for (int i=0;i<accounts.size();i++){
//////					
//////					Product account = accounts.get(i);
//////	
//////					if(account.getLevel()==2&&account.getMonths()==1){
//////						
//////						megaApi.getPaymentUrl(account.getHandle(),this);	
//////					}
//////				}
////				break;
////
////			}
////			case 3:{
////				
////				((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_III_MONTH);
////				
//////				for (int i=0;i<accounts.size();i++){
//////					
//////					Product account = accounts.get(i);
//////	
//////					if(account.getLevel()==3&&account.getMonths()==1){
//////						
//////						megaApi.getPaymentUrl(account.getHandle(),this);	
//////					}
//////				}
////				break;
////			}	
////			case 4:{
////				Toast.makeText(context, "PAY MONTH", Toast.LENGTH_LONG).show();
////				
////				AlertDialog paymentDialog;
////				
//////				final ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.select_dialog_singlechoice, android.R.id.text1, new String[] {getResources().getString(R.string.cam_sync_wifi), getResources().getString(R.string.cam_sync_data)});
////				final ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.select_dialog_singlechoice, android.R.id.text1, new String[] {"Google Play", "Fortumo"});
////				AlertDialog.Builder builder = new AlertDialog.Builder(context);
//////				builder.setTitle(getString(R.string.section_photo_sync));
////				builder.setTitle("Payment method");
////				
////				builder.setSingleChoiceItems(adapter,  0,  new DialogInterface.OnClickListener() {
////					
////					@Override
////					public void onClick(DialogInterface dialog, int which) {
////						switch (which){
////							case 0:{
////								((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_LITE_MONTH);
////								break;
////							}
////							case 1:{
////								Toast.makeText(context, "FORTUMOOOOO", Toast.LENGTH_SHORT).show();
////								Intent intent = new Intent(((ManagerActivity)context), FortumoPayment.class);
////								startActivity(intent);
////								break;
////							}
////					}
////						dialog.dismiss();
////					}
////				});
////				
////				builder.setPositiveButton(context.getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
////					
////					@Override
////					public void onClick(DialogInterface dialog, int which) {
////						dialog.dismiss();
////					}
////				});
////				
////				paymentDialog = builder.create();
////				paymentDialog.show();
////				brandAlertDialog(paymentDialog);
////
//////				((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_LITE_MONTH);
////				break;
////			}
////		}
	}
	
	public void setInfo (int _type, int _paymentMonth){
		this.parameterType = _type;
		this.paymentMonth = _paymentMonth;

		if(myAccountInfo==null){
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}
	}

	public int getParameterType(){
		return parameterType;
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

		logDebug("REQUEST: " + request.getName() + "__" + request.getRequestString());
		if (request.getType() == MegaRequest.TYPE_CREDIT_CARD_STORE){
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("API_OK!!");
				logDebug("PRODUCTHANDLE: " + productHandleLong);
				megaApi.upgradeAccount(productHandleLong, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD, this);
			}
			else if (e.getErrorCode() == MegaError.API_EEXIST){
				logWarning("API_EEXIST");
				megaApi.upgradeAccount(productHandleLong, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD, this);
			}
			else{
				logError("ERROR: " + e.getErrorCode() + "__" + e.getErrorString());
				Toast.makeText(context, getString(R.string.credit_card_information_error) + " ERROR (" + e.getErrorCode() + ")_" + e.getErrorString(), Toast.LENGTH_LONG).show();
				((ManagerActivityLollipop)context).dismissStatusDialog();
				((ManagerActivityLollipop)context).updateInfoNumberOfSubscriptions();
				((ManagerActivityLollipop)context).showMyAccount();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_UPGRADE_ACCOUNT){
			((ManagerActivityLollipop)context).dismissStatusDialog();
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("OK payment!!!");
				Toast.makeText(context, getString(R.string.account_successfully_upgraded), Toast.LENGTH_LONG).show();
				((ManagerActivityLollipop)context).dismissStatusDialog();
				((ManagerActivityLollipop)context).updateInfoNumberOfSubscriptions();
				((ManagerActivityLollipop)context).showMyAccount();
			}
			else{
				logError("ERROR: " + e.getErrorCode() + "__" + e.getErrorString());
				Toast.makeText(context, getString(R.string.account_error_upgraded) + " ERROR (" + e.getErrorCode() + ")_" + e.getErrorString(), Toast.LENGTH_LONG).show();
				((ManagerActivityLollipop)context).dismissStatusDialog();
				((ManagerActivityLollipop)context).updateInfoNumberOfSubscriptions();
				((ManagerActivityLollipop)context).showMyAccount();
			}
		}
		
//		if (request.getType() == MegaRequest.TYPE_GET_PRICING){
//			MegaPricing p = request.getPricing();
//			for (int i=0;i<p.getNumProducts();i++){
//				Product account = new Product (p.getHandle(i), p.getProLevel(i), p.getMonths(i), p.getGBStorage(i), p.getAmount(i), p.getGBTransfer(i));
//				if (account.getLevel()==4&&account.getMonths()==1){
//					long planHandle = account.handle;
//					megaApi.getPaymentId(planHandle, this);
//				}
//			}
//		}
//		else if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_ID){
//			log("PAYMENT ID: " + request.getLink());
//			Toast.makeText(context, "PAYMENTID: " + request.getLink(), Toast.LENGTH_LONG).show();
////			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(request.getLink()));
////			startActivity(browserIntent);
//
//		}
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
//		((ManagerActivity)context).showpF(parameterType, accounts);
		((ManagerActivityLollipop)context).showUpAF();
		((ManagerActivityLollipop)context).setDisplayedAccountType(-1);
		return 3;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
		aB = ((AppCompatActivity)activity).getSupportActionBar();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.proceed_cc:{
				address1String = address1Edit.getText().toString();
				address2String = address2Edit.getText().toString();
				cityString = cityEdit.getText().toString();
				stateString = stateEdit.getText().toString();
				postalCodeString = postalCodeEdit.getText().toString();
				firstNameString = firstNameEdit.getText().toString();
				lastNameString = lastNameEdit.getText().toString();
				creditCardNumberString = creditCardNumberEdit.getText().toString();
				cvvString = cvvEdit.getText().toString();
//				countryString = "ES";
//				Toast.makeText(context, address1String + "__" + address2String + "__" + cityString + "__" + stateString + "__" + countryCode + "__" + postalCodeString + "__" + firstNameString + "__" + lastNameString + "__" + creditCardNumberString + "__" + monthString + "__" + yearString + "__" + cvvString, Toast.LENGTH_LONG).show();
				logDebug(address1String + "__" + address2String + "__" + cityString + "__" + stateString + "__" + countryCode + "__" + postalCodeString + "__" + firstNameString + "__" + lastNameString + "__" + creditCardNumberString + "__" + monthString + "__" + yearString + "__" + cvvString);

				ArrayList<Product> accounts = myAccountInfo.getProductAccounts();

				for (int i=0;i<accounts.size();i++){
					Product account = accounts.get(i);
					
					if(account.getLevel() == parameterType){
						if (paymentMonth == 1){
							if(account.getMonths()==1){
								productHandleLong = account.getHandle();
							}
						}
						else if (paymentMonth == 0){
							if(account.getMonths()==12){
								productHandleLong = account.getHandle();
							}
						}
					}
				}
				megaApi.creditCardStore(address1String, address2String, cityString, stateString, countryCode, postalCodeString, firstNameString, lastNameString, creditCardNumberString, monthString, yearString, cvvString, this);
				((ManagerActivityLollipop)context).showStatusDialog(getString(R.string.upgrading_account_message));
//				Toast.makeText(context, getString(R.string.upgrading_account_message), Toast.LENGTH_LONG).show();
//					((ManagerActivity)context).showMyAccount();
				logDebug("PRODUCT HANDLE CC: " + productHandleLong);
//				Toast.makeText(context, "PRODUCT HANDLE CC: " + productHandleLong, Toast.LENGTH_LONG).show();
				break;
			}
//				for (int i=0;i<accounts.size();i++){
//					Product account = accounts.get(i);
//					if (account.getLevel()==4&&account.getMonths()==1){
//						productHandleLong = account.handle;
//						log("PRODUCT HANDLE CC: " + productHandleLong);
//						megaApi.creditCardStore(address1String, address2String, cityString, stateString, countryString, postalCodeString, firstNameString, lastNameString, creditCardNumberString, monthString, yearString, cvvString, this);
////						megaApi.getPaymentId(planHandle, this);
//					}
//				}
//				
//			}
			case R.id.cancel_cc:{
				onBackPressed();
				break;
			}
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		// An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
		switch(parent.getId()){
			case R.id.country_cc:{
				if (position == 0){
					if (view != null){
						((TextView) view).setTextColor(Color.GRAY);
						((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
					}
				}
				else{
					String countryString = parent.getItemAtPosition(position).toString();
//					Toast.makeText(context, "CountryString: " + countryString, Toast.LENGTH_LONG).show();
					countryCode = getCountryCode(countryString);
//					Toast.makeText(context, "CountryCode: " + countryString, Toast.LENGTH_LONG).show();
				}
				break;
			}
			case R.id.month_cc:{
				if (position == 0){
					if (view != null){
						((TextView) view).setTextColor(Color.GRAY);
						((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
					}
				}
				else{
					monthString = parent.getItemAtPosition(position).toString(); 
				}
				break;
			}
			case R.id.year_cc:{
				if (position == 0){
					if (view != null){
						((TextView) view).setTextColor(Color.GRAY);
						((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
					}
				}
				else{
					yearString = parent.getItemAtPosition(position).toString(); 
				}
				break;
			}
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// Another interface callback
	}
}
