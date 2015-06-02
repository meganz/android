package nz.mega.android;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaPricing;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class PaymentFragment extends Fragment implements MegaRequestListenerInterface, OnClickListener{
	
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
	private TextView perMonth;
	private TextView perYear;
	private TextView pricingFrom;
	private ArrayList<Long> handleUrl;
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
	private RelativeLayout paymentUpgradeComment;
	private RelativeLayout paymentGoogleWallet;
	int parameterType;	
	MegaApiAndroid megaApi;
	Context context;
	ArrayList<Product> accounts;
	PaymentFragment paymentFragment = this;
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
		
		handleUrl=new ArrayList<Long>();

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
		googlePlaySubscription.setVisibility(View.VISIBLE);

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
		
		paymentCreditCard = (RelativeLayout) v.findViewById(R.id.payment_credit_card);
		paymentCreditCard.setVisibility(View.GONE);
		
		paymentFortumo = (RelativeLayout) v.findViewById(R.id.payment_fortumo);
		paymentFortumo.setVisibility(View.GONE);
		
		paymentGoogleWallet = (RelativeLayout) v.findViewById(R.id.payment_google_wallet);
		paymentGoogleWallet.setVisibility(View.GONE);
		
		paymentCreditCard.setOnClickListener(this);
		paymentFortumo.setOnClickListener(this);
		paymentGoogleWallet.setOnClickListener(this);
		
		switch (parameterType) {
			case 1:{
				
				paymentPerMonth.setVisibility(View.VISIBLE);
				paymentPerYear.setVisibility(View.VISIBLE);
				
				for (int i=0;i<accounts.size();i++){
	
					Product account = accounts.get(i);
	
					if(account.getLevel()==1){
						aB.setTitle(getString(R.string.pro1_account));
	
						storage.setText(account.getStorage()+"GB");		            
						
						if(account.getMonths()==12){
							double perYearF=account.getAmount()/100.00;
							String perYearString =df.format(perYearF);
	
							perYear.setText(perYearString+" €");
						}
						else if(account.getMonths()==1){
							double perMonthF=account.getAmount()/100.00;
							String perMonthString =df.format(perMonthF);
	
							perMonth.setText(perMonthString+" €");
							bandwidth.setText(account.getTransfer()/1024 + " TB");
							pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
						}
					}
				}
	
				break;
			}
			case 2:{
				
				paymentPerMonth.setVisibility(View.VISIBLE);
				paymentPerYear.setVisibility(View.VISIBLE);
	
				for (int i=0;i<accounts.size();i++){
	
					Product account = accounts.get(i);
	
					if(account.getLevel()==2){
						aB.setTitle(getString(R.string.pro2_account));
	
						storage.setText(account.getStorage()/1024+"TB");		            
	
						if(account.getMonths()==12){
							double perYearF=account.getAmount()/100.00;
							String perYearString =df.format(perYearF);
	
							perYear.setText(perYearString+" €");
						}
						else if(account.getMonths()==1){
							double perMonthF=account.getAmount()/100.00;
							String perMonthString =df.format(perMonthF);
	
							perMonth.setText(perMonthString+" €");
							bandwidth.setText(sizeTranslation(account.getTransfer(),0));
							pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
						}
					}
				}
	
				break;
			}
			case 3:{
				
				paymentPerMonth.setVisibility(View.VISIBLE);
				paymentPerYear.setVisibility(View.VISIBLE);
				
				for (int i=0;i<accounts.size();i++){
	
					Product account = accounts.get(i);
	
					if(account.getLevel()==3){
						aB.setTitle(getString(R.string.pro3_account));
	
						storage.setText(account.getStorage()/1024+"TB");            
	
						if(account.getMonths()==12){
							double perYearF=account.getAmount()/100.00;
							String perYearString =df.format(perYearF);
	
							perYear.setText(perYearString+" €");
						}
						else if(account.getMonths()==1){
							double perMonthF=account.getAmount()/100.00;
							String perMonthString =df.format(perMonthF);
	
							perMonth.setText(perMonthString+" €");
							bandwidth.setText(sizeTranslation(account.getTransfer(),0));
							pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
						}
					}
				}
				break;
			}
			
			case 4:{
	
				paymentPerMonth.setVisibility(View.VISIBLE);
				paymentPerYear.setVisibility(View.VISIBLE);
				
				for (int i=0;i<accounts.size();i++){
	
					Product account = accounts.get(i);
	
					if(account.getLevel()==4){
						aB.setTitle(getString(R.string.prolite_account));
	
						storage.setText(account.getStorage()+"GB");		            
	
						if(account.getMonths()==12){
							double perYearF=account.getAmount()/100.00;
							String perYearString =df.format(perYearF);
	
							perYear.setText(perYearString+" €");
						}
						else if(account.getMonths()==1){
							double perMonthF=account.getAmount()/100.00;
							String perMonthString =df.format(perMonthF);
	
							perMonth.setText(perMonthString+" €");
							bandwidth.setText(account.getTransfer()/1024 + " TB");
							pricingFrom.setText(perMonthString + " € " + getString(R.string.per_month));
						}
					}
				}
				break;
			}
		}

		//		megaApi.getPricing(this);	

		return v;
	}	
	
	public void payYear() {
		log("yearly");
		
		paymentMonth = 0;
		
		switch(parameterType){
		
			case 1:{
				((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true);
//				((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_I_YEAR);
//				
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
				((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true);
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
				((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true);
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
				((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true);
//				((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_LITE_YEAR);
				break;
			}
			
		}
	}
	
	public void payMonth() {
		log("monthly");
		
		paymentMonth = 1;
		
		selectMemberShip.setText(getString(R.string.select_payment_method));
		selectRecurring.setVisibility(View.GONE);
		googlePlaySubscription.setVisibility(View.GONE);
		
		switch(parameterType){
			case 4:{
				((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true);
//				paymentPerMonth.setVisibility(View.GONE);
//				paymentPerYear.setVisibility(View.GONE);
//				paymentUpgradeComment.setVisibility(View.GONE);
//				paymentCreditCard.setVisibility(View.VISIBLE);
//				paymentFortumo.setVisibility(View.VISIBLE);
//				paymentGoogleWallet.setVisibility(View.VISIBLE);
				break;
			}
			case 1:{
				((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true);
//				paymentPerMonth.setVisibility(View.GONE);
//				paymentPerYear.setVisibility(View.GONE);
//				paymentUpgradeComment.setVisibility(View.GONE);
//				paymentCreditCard.setVisibility(View.VISIBLE);
//				paymentFortumo.setVisibility(View.GONE);
//				paymentGoogleWallet.setVisibility(View.VISIBLE);
				break;	
			}
			case 2:{
				((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true);
//				paymentPerMonth.setVisibility(View.GONE);
//				paymentPerYear.setVisibility(View.GONE);
//				paymentUpgradeComment.setVisibility(View.GONE);
//				paymentCreditCard.setVisibility(View.VISIBLE);
//				paymentFortumo.setVisibility(View.GONE);
//				paymentGoogleWallet.setVisibility(View.VISIBLE);
				break;	
			}
			case 3:{
				((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true);
//				paymentPerMonth.setVisibility(View.GONE);
//				paymentPerYear.setVisibility(View.GONE);
//				paymentUpgradeComment.setVisibility(View.GONE);
//				paymentCreditCard.setVisibility(View.VISIBLE);
//				paymentFortumo.setVisibility(View.GONE);
//				paymentGoogleWallet.setVisibility(View.VISIBLE);
				break;	
			}
		}

//		switch(parameterType){
//		
//			case 1:{
//				
//				((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_I_MONTH);
//				
////				for (int i=0;i<accounts.size();i++){
////					
////					Product account = accounts.get(i);
////	
////					if(account.getLevel()==1&&account.getMonths()==1){
////						
////						megaApi.getPaymentUrl(account.getHandle(),this);	
////					}
////				}
//				break;
//
//			}
//			case 2:{
//				
//				((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_II_MONTH);
//				
////				for (int i=0;i<accounts.size();i++){
////					
////					Product account = accounts.get(i);
////	
////					if(account.getLevel()==2&&account.getMonths()==1){
////						
////						megaApi.getPaymentUrl(account.getHandle(),this);	
////					}
////				}
//				break;
//
//			}
//			case 3:{
//				
//				((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_III_MONTH);
//				
////				for (int i=0;i<accounts.size();i++){
////					
////					Product account = accounts.get(i);
////	
////					if(account.getLevel()==3&&account.getMonths()==1){
////						
////						megaApi.getPaymentUrl(account.getHandle(),this);	
////					}
////				}
//				break;
//			}	
//			case 4:{
//				Toast.makeText(context, "PAY MONTH", Toast.LENGTH_LONG).show();
//				
//				AlertDialog paymentDialog;
//				
////				final ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.select_dialog_singlechoice, android.R.id.text1, new String[] {getResources().getString(R.string.cam_sync_wifi), getResources().getString(R.string.cam_sync_data)});
//				final ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.select_dialog_singlechoice, android.R.id.text1, new String[] {"Google Play", "Fortumo"});
//				AlertDialog.Builder builder = new AlertDialog.Builder(context);
////				builder.setTitle(getString(R.string.section_photo_sync));
//				builder.setTitle("Payment method");
//				
//				builder.setSingleChoiceItems(adapter,  0,  new DialogInterface.OnClickListener() {
//					
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						switch (which){
//							case 0:{
//								((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_LITE_MONTH);
//								break;
//							}
//							case 1:{
//								Toast.makeText(context, "FORTUMOOOOO", Toast.LENGTH_SHORT).show();
//								Intent intent = new Intent(((ManagerActivity)context), FortumoPayment.class);
//								startActivity(intent);
//								break;
//							}
//					}
//						dialog.dismiss();
//					}
//				});
//				
//				builder.setPositiveButton(context.getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
//					
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						dialog.dismiss();
//					}
//				});
//				
//				paymentDialog = builder.create();
//				paymentDialog.show();
//				Util.brandAlertDialog(paymentDialog);
//
////				((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_LITE_MONTH);
//				break;
//			}
//		}
	}
	
	public void setInfo (int _type, ArrayList<Product> _accounts){
		this.accounts = _accounts;
		this.parameterType = _type;
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
		
		if (request.getType() == MegaRequest.TYPE_GET_PRICING){
			MegaPricing p = request.getPricing();
			for (int i=0;i<p.getNumProducts();i++){
				Product account = new Product (p.getHandle(i), p.getProLevel(i), p.getMonths(i), p.getGBStorage(i), p.getAmount(i), p.getGBTransfer(i));
				if (account.getLevel()==4&&account.getMonths()==1){
					long planHandle = account.handle;
					megaApi.getPaymentId(planHandle, this);
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_ID){
			log("PAYMENT ID: " + request.getLink());
			Toast.makeText(context, "PAYMENTID: " + request.getLink(), Toast.LENGTH_LONG).show();
//			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(request.getLink()));
//			startActivity(browserIntent);

		}
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
//		if (paymentMonth == -1){
			((ManagerActivity)context).showUpAF();
//		}
//		else{
			paymentMonth = -1;
//			((ManagerActivity)context).showpF(parameterType, accounts, true);
//		}
		return 3;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
		aB = ((ActionBarActivity)activity).getSupportActionBar();
	}
	
	public static void log(String message) {
		Util.log("UpgradePaymentActivity", message);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.payment_google_wallet:{
				switch(parameterType){
					case 1:{
						if (paymentMonth == 1){
							((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_I_MONTH);
						}
						else if (paymentMonth == 0){
							((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_I_YEAR);
						}
						break;
					}
					case 2:{
						if (paymentMonth == 1){
							((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_II_MONTH);
						}
					}
					case 3:{
						if (paymentMonth == 1){
							((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_III_MONTH);
						}
					}
					case 4:{
						if (paymentMonth == 1){
							((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_LITE_MONTH);
						}
						else if (paymentMonth == 0){
							((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_LITE_YEAR);
						}
					}
				}
				paymentMonth = -1;
				break;
			}
			case R.id.payment_fortumo:{
				if (parameterType == 4 && paymentMonth == 1){
					Intent intent = new Intent(((ManagerActivity)context), FortumoPayment.class);
					startActivity(intent);
				}
				paymentMonth = -1;
				break;
			}
			case R.id.payment_credit_card:{
				((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true);
				paymentMonth = -1;
				break;
			}
		}
	}
}
