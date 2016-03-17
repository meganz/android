package mega.privacy.android.app.lollipop;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaPricing;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

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

public class PaymentFragmentLollipop extends Fragment implements MegaRequestListenerInterface, OnClickListener{
	
	public static int MY_ACCOUNT_FRAGMENT = 5000;
	public static int UPGRADE_ACCOUNT_FRAGMENT = 5001;
	public static int PAYMENT_FRAGMENT = 5002;
	
	private TextView title;
	private TextView perMonth;
	private TextView priceInteger;
	private TextView priceDecimal;
	private TextView storageInteger;
	private TextView storageGb;
	private TextView bandwidthInteger;
	private TextView bandwidthTb;
	
	private TextView selectPaymentMethod;
	
	RelativeLayout googlePlayLayout;
	RelativeLayout creditCardLayout;
	RelativeLayout fortumoLayout;
	RelativeLayout centiliLayout;
	
	LinearLayout googlePlaySeparator;
	LinearLayout creditCardSeparator;
	LinearLayout fortumoSeparator;
	LinearLayout centiliSeparator;
	
	
	private ActionBar aB;
	
	int parameterType=-1;	
	MegaApiAndroid megaApi;
	Context context;
	ArrayList<Product> accounts;
	PaymentFragmentLollipop paymentFragment = this;
	BitSet paymentBitSet = null;
	
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
		
//		if (paymentBitSet == null){
			megaApi.getPaymentMethods(this);
//		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		View v = null;
		v = inflater.inflate(R.layout.activity_upgrade_payment, container, false);
		
		title = (TextView) v.findViewById(R.id.payment_title_text);
		perMonth = (TextView) v.findViewById(R.id.payment_per_month_text);
		perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));
		priceInteger = (TextView) v.findViewById(R.id.payment_integer_text);
		priceDecimal = (TextView) v.findViewById(R.id.payment_decimal_text);
		storageInteger = (TextView) v.findViewById(R.id.payment_storage_value_integer);
		storageGb = (TextView) v.findViewById(R.id.payment_storage_value_gb);
		bandwidthInteger = (TextView) v.findViewById(R.id.payment_bandwidth_value_integer);
		bandwidthTb = (TextView) v.findViewById(R.id.payment_bandwith_value_tb);
		
		selectPaymentMethod = (TextView) v.findViewById(R.id.payment_text_payment_method);
		
		googlePlayLayout = (RelativeLayout) v.findViewById(R.id.payment_method_google_wallet);
		googlePlayLayout.setOnClickListener(this);
		googlePlaySeparator = (LinearLayout) v.findViewById(R.id.payment_separator);
		creditCardLayout = (RelativeLayout) v.findViewById(R.id.payment_method_credit_card);
		creditCardLayout.setOnClickListener(this);
		creditCardSeparator = (LinearLayout) v.findViewById(R.id.payment_separator_1);
		fortumoLayout = (RelativeLayout) v.findViewById(R.id.payment_method_fortumo);
		fortumoLayout.setOnClickListener(this);
		fortumoSeparator = (LinearLayout) v.findViewById(R.id.payment_separator_2);
		centiliLayout = (RelativeLayout) v.findViewById(R.id.payment_method_centili);
		centiliLayout.setOnClickListener(this);
		centiliSeparator = (LinearLayout) v.findViewById(R.id.payment_separator_3);
		
		googlePlayLayout.setVisibility(View.GONE);
		creditCardLayout.setVisibility(View.GONE);
		fortumoLayout.setVisibility(View.GONE);
		centiliLayout.setVisibility(View.GONE);
		
		googlePlaySeparator.setVisibility(View.GONE);
		creditCardSeparator.setVisibility(View.GONE);
		fortumoSeparator.setVisibility(View.GONE);
		centiliSeparator.setVisibility(View.GONE);
				
		if(paymentBitSet == null){
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
		
						if(account.getLevel()==1 && account.getMonths()==1){
							double price = account.getAmount()/100.00;
							String priceString = df.format(price);
							String [] s = priceString.split("\\.");
							if (s.length == 1){
								priceInteger.setText(s[0]);
								priceDecimal.setText("");
							}
							else if (s.length == 2){
								priceInteger.setText(s[0]);
								priceDecimal.setText("." + s[1] + " €");
							}
							
							storageInteger.setText(""+account.getStorage());
							storageGb.setText(" GB");
							
							bandwidthInteger.setText(""+account.getTransfer()/1024);
							bandwidthTb.setText(" TB");
							
							title.setText(getString(R.string.pro1_account));
							
							storageInteger.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							storageGb.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							bandwidthInteger.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							bandwidthTb.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							perMonth.setTextColor(context.getResources().getColor(R.color.upgrade_red));
						}
					}
		
					if (paymentBitSet != null){
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							googlePlayLayout.setVisibility(View.VISIBLE);
							googlePlaySeparator.setVisibility(View.VISIBLE);
						}
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
							creditCardLayout.setVisibility(View.VISIBLE);
							creditCardSeparator.setVisibility(View.VISIBLE);
						}
						fortumoLayout.setVisibility(View.GONE);
						fortumoSeparator.setVisibility(View.GONE);
						centiliLayout.setVisibility(View.GONE);
						centiliSeparator.setVisibility(View.GONE);
						
						if(!Util.isPaymentMethod(paymentBitSet, parameterType)){
							selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
						}
						else{
							selectPaymentMethod.setText(getString(R.string.select_payment_method));
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
								priceInteger.setText(s[0]);
								priceDecimal.setText("");
							}
							else if (s.length == 2){
								priceInteger.setText(s[0]);
								priceDecimal.setText("." + s[1] + " €");
							}
							
							storageInteger.setText(sizeTranslation(account.getStorage(),0));
							storageGb.setText(" TB");
							
							bandwidthInteger.setText(""+account.getTransfer()/1024);
							bandwidthTb.setText(" TB");
							
							title.setText(getString(R.string.pro2_account));
							
							storageInteger.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							storageGb.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							bandwidthInteger.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							bandwidthTb.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							perMonth.setTextColor(context.getResources().getColor(R.color.upgrade_red));
						}
					}
					
					if (paymentBitSet != null){
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							googlePlayLayout.setVisibility(View.VISIBLE);
							googlePlaySeparator.setVisibility(View.VISIBLE);
						}
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
							creditCardLayout.setVisibility(View.VISIBLE);
							creditCardSeparator.setVisibility(View.VISIBLE);
						}
						fortumoLayout.setVisibility(View.GONE);
						fortumoSeparator.setVisibility(View.GONE);
						centiliLayout.setVisibility(View.GONE);
						centiliSeparator.setVisibility(View.GONE);
						
						if(!Util.isPaymentMethod(paymentBitSet, parameterType)){
							selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
						}
						else{
							selectPaymentMethod.setText(getString(R.string.select_payment_method));
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
								priceInteger.setText(s[0]);
								priceDecimal.setText("");
							}
							else if (s.length == 2){
								priceInteger.setText(s[0]);
								priceDecimal.setText("." + s[1] + " €");
							}
							
							storageInteger.setText(sizeTranslation(account.getStorage(),0));
							storageGb.setText(" TB");
							
							bandwidthInteger.setText(""+account.getTransfer()/1024);
							bandwidthTb.setText(" TB");
							
							title.setText(getString(R.string.pro3_account));
							
							storageInteger.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							storageGb.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							bandwidthInteger.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							bandwidthTb.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							perMonth.setTextColor(context.getResources().getColor(R.color.upgrade_red));
						}
					}
					
					if (paymentBitSet != null){
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							googlePlayLayout.setVisibility(View.VISIBLE);
							googlePlaySeparator.setVisibility(View.VISIBLE);
						}
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
							creditCardLayout.setVisibility(View.VISIBLE);
							creditCardSeparator.setVisibility(View.VISIBLE);
						}
						fortumoLayout.setVisibility(View.GONE);
						fortumoSeparator.setVisibility(View.GONE);
						centiliLayout.setVisibility(View.GONE);
						centiliSeparator.setVisibility(View.GONE);
						
						if(!Util.isPaymentMethod(paymentBitSet, parameterType)){
							selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
						}
						else{
							selectPaymentMethod.setText(getString(R.string.select_payment_method));
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
								priceInteger.setText(s[0]);
								priceDecimal.setText("");
							}
							else if (s.length == 2){
								priceInteger.setText(s[0]);
								priceDecimal.setText("." + s[1] + " €");
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
					}
					
					if (paymentBitSet != null){
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							googlePlayLayout.setVisibility(View.VISIBLE);
							googlePlaySeparator.setVisibility(View.VISIBLE);
						}
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
							creditCardLayout.setVisibility(View.VISIBLE);
							creditCardSeparator.setVisibility(View.VISIBLE);
						}
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
							fortumoLayout.setVisibility(View.VISIBLE);
							fortumoSeparator.setVisibility(View.VISIBLE);
						}
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
							centiliLayout.setVisibility(View.VISIBLE);
							centiliSeparator.setVisibility(View.VISIBLE);
						}
						
						if(!Util.isPaymentMethod(paymentBitSet, parameterType)){
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

		return v;
	}	
	
	/*
	public void payYear() {
		log("yearly");
		
		paymentMonth = 0;
		
		((ManagerActivityLollipop)context).showCC(parameterType, accounts, paymentMonth, true, paymentBitSet);
		paymentMonth = -1;
		
//		switch(parameterType){
//		
//			case 1:{
//				((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true);
////				((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_I_YEAR);
////				
////				for (int i=0;i<accounts.size();i++){
////					
////					Product account = accounts.get(i);
////	
////					if(account.getLevel()==1&&account.getMonths()==12){
////						
////						megaApi.getPaymentUrl(account.getHandle(),this);	
////					}
////				}
//				break;
//			}
//			case 2:{
//				((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true);
////				for (int i=0;i<accounts.size();i++){
////					
////					Product account = accounts.get(i);
////	
////					if(account.getLevel()==2&&account.getMonths()==12){
////						
////						megaApi.getPaymentUrl(account.getHandle(),this);	
////					}
////				}
//				break;
//			}
//			case 3:{
//				((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true);
////				for (int i=0;i<accounts.size();i++){
////					
////					Product account = accounts.get(i);
////	
////					if(account.getLevel()==3&&account.getMonths()==12){
////						
////						megaApi.getPaymentUrl(account.getHandle(),this);	
////					}
////				}
//				break;
//			}			
//			case 4:{
//				((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true);
////				((ManagerActivity)context).launchPayment(ManagerActivity.SKU_PRO_LITE_YEAR);
//				break;
//			}
//			
//		}
	}
	*/
	/*
	public void payMonth() {
		log("monthly");
		
		paymentMonth = 1;
		
		((ManagerActivityLollipop)context).showCC(parameterType, accounts, paymentMonth, true, paymentBitSet);
		paymentMonth = -1;

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
	*/
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
			
			if(paymentBitSet!=null){
				if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
					googlePlayLayout.setVisibility(View.VISIBLE);
					googlePlaySeparator.setVisibility(View.VISIBLE);
				}
				if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
					creditCardLayout.setVisibility(View.VISIBLE);
					creditCardSeparator.setVisibility(View.VISIBLE);
				}
				if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
					if (parameterType == 4){
						fortumoLayout.setVisibility(View.VISIBLE);
						fortumoSeparator.setVisibility(View.VISIBLE);
					}
				}
				if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
					if (parameterType == 4){
						centiliLayout.setVisibility(View.VISIBLE);
						centiliSeparator.setVisibility(View.VISIBLE);
					}
				}
				if(!Util.isPaymentMethod(paymentBitSet, parameterType)){
					selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
				}
				else{
					selectPaymentMethod.setText(getString(R.string.select_payment_method));
				}
			}
		}		
		if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_ID){
			log("PAYMENT ID: " + request.getLink());
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
		
						if(account.getLevel()==1 && account.getMonths()==1){
							double price = account.getAmount()/100.00;
							String priceString = df.format(price);
							String [] s = priceString.split("\\.");
							if (s.length == 1){
								priceInteger.setText(s[0]);
								priceDecimal.setText("");
							}
							else if (s.length == 2){
								priceInteger.setText(s[0]);
								priceDecimal.setText("." + s[1] + " €");
							}
							
							storageInteger.setText(""+account.getStorage());
							storageGb.setText(" GB");
							
							bandwidthInteger.setText(""+account.getTransfer()/1024);
							bandwidthTb.setText(" TB");
							
							title.setText(getString(R.string.pro1_account));
							
							storageInteger.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							storageGb.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							bandwidthInteger.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							bandwidthTb.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							perMonth.setTextColor(context.getResources().getColor(R.color.upgrade_red));
						}
					}
		
					if (paymentBitSet != null){
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							googlePlayLayout.setVisibility(View.VISIBLE);
							googlePlaySeparator.setVisibility(View.VISIBLE);
						}
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
							creditCardLayout.setVisibility(View.VISIBLE);
							creditCardSeparator.setVisibility(View.VISIBLE);
						}
						fortumoLayout.setVisibility(View.GONE);
						fortumoSeparator.setVisibility(View.GONE);
						centiliLayout.setVisibility(View.GONE);
						centiliSeparator.setVisibility(View.GONE);
						
						if(!Util.isPaymentMethod(paymentBitSet, parameterType)){
							selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
						}
						else{
							selectPaymentMethod.setText(getString(R.string.select_payment_method));
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
								priceInteger.setText(s[0]);
								priceDecimal.setText("");
							}
							else if (s.length == 2){
								priceInteger.setText(s[0]);
								priceDecimal.setText("." + s[1] + " €");
							}
							
							storageInteger.setText(sizeTranslation(account.getStorage(),0));
							storageGb.setText(" TB");
							
							bandwidthInteger.setText(""+account.getTransfer()/1024);
							bandwidthTb.setText(" TB");
							
							title.setText(getString(R.string.pro2_account));
							
							storageInteger.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							storageGb.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							bandwidthInteger.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							bandwidthTb.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							perMonth.setTextColor(context.getResources().getColor(R.color.upgrade_red));
						}
					}
					
					if (paymentBitSet != null){
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							googlePlayLayout.setVisibility(View.VISIBLE);
							googlePlaySeparator.setVisibility(View.VISIBLE);
						}
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
							creditCardLayout.setVisibility(View.VISIBLE);
							creditCardSeparator.setVisibility(View.VISIBLE);
						}
						fortumoLayout.setVisibility(View.GONE);
						fortumoSeparator.setVisibility(View.GONE);
						centiliLayout.setVisibility(View.GONE);
						centiliSeparator.setVisibility(View.GONE);
						
						if(!Util.isPaymentMethod(paymentBitSet, parameterType)){
							selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
						}
						else{
							selectPaymentMethod.setText(getString(R.string.select_payment_method));
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
								priceInteger.setText(s[0]);
								priceDecimal.setText("");
							}
							else if (s.length == 2){
								priceInteger.setText(s[0]);
								priceDecimal.setText("." + s[1] + " €");
							}
							
							storageInteger.setText(sizeTranslation(account.getStorage(),0));
							storageGb.setText(" TB");
							
							bandwidthInteger.setText(""+account.getTransfer()/1024);
							bandwidthTb.setText(" TB");
							
							title.setText(getString(R.string.pro3_account));
							
							storageInteger.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							storageGb.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							bandwidthInteger.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							bandwidthTb.setTextColor(context.getResources().getColor(R.color.upgrade_red));
							perMonth.setTextColor(context.getResources().getColor(R.color.upgrade_red));
						}
					}
					
					if (paymentBitSet != null){
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							googlePlayLayout.setVisibility(View.VISIBLE);
							googlePlaySeparator.setVisibility(View.VISIBLE);
						}
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
							creditCardLayout.setVisibility(View.VISIBLE);
							creditCardSeparator.setVisibility(View.VISIBLE);
						}
						fortumoLayout.setVisibility(View.GONE);
						fortumoSeparator.setVisibility(View.GONE);
						centiliLayout.setVisibility(View.GONE);
						centiliSeparator.setVisibility(View.GONE);
						
						if(!Util.isPaymentMethod(paymentBitSet, parameterType)){
							selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
						}
						else{
							selectPaymentMethod.setText(getString(R.string.select_payment_method));
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
								priceInteger.setText(s[0]);
								priceDecimal.setText("");
							}
							else if (s.length == 2){
								priceInteger.setText(s[0]);
								priceDecimal.setText("." + s[1] + " €");
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
					}
					
					if (paymentBitSet != null){
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							googlePlayLayout.setVisibility(View.VISIBLE);
							googlePlaySeparator.setVisibility(View.VISIBLE);
						}
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
							creditCardLayout.setVisibility(View.VISIBLE);
							creditCardSeparator.setVisibility(View.VISIBLE);
						}
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
							fortumoLayout.setVisibility(View.VISIBLE);
							fortumoSeparator.setVisibility(View.VISIBLE);
						}
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
							centiliLayout.setVisibility(View.VISIBLE);
							centiliSeparator.setVisibility(View.VISIBLE);
						}
						
						if(!Util.isPaymentMethod(paymentBitSet, parameterType)){
							selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
						}
						else{
							selectPaymentMethod.setText(getString(R.string.select_payment_method));
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

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		// TODO Auto-generated method stub
		
	}
	
	public int onBackPressed(){
		log("onBackPressed");
		if (context == null){
			return 0;
		}
		else{		
			((ManagerActivityLollipop)context).showUpAF(paymentBitSet);
			return 3;
		}		
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
		aB = ((AppCompatActivity)activity).getSupportActionBar();
	}
	
	public static void log(String message) {
		Util.log("PaymentFragmentLollipop", message);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.payment_method_google_wallet:{
				((ManagerActivityLollipop)context).showmyF(MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET, parameterType, accounts, paymentBitSet);
				break;
			}
			case R.id.payment_method_credit_card:{
				((ManagerActivityLollipop)context).showmyF(MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD, parameterType, accounts, paymentBitSet);
				break;
			}
			case R.id.payment_method_fortumo:{
				((ManagerActivityLollipop)context).showmyF(MegaApiAndroid.PAYMENT_METHOD_FORTUMO, parameterType, accounts, paymentBitSet);
				break;
			}
			case R.id.payment_method_centili:{
				((ManagerActivityLollipop)context).showmyF(MegaApiAndroid.PAYMENT_METHOD_CENTILI, parameterType, accounts, paymentBitSet);
				break;
			}
		}
		/*
		switch(v.getId()){
			case R.id.payment_google_wallet:{
				switch(parameterType){
					case 1:{
						if (paymentMonth == 1){
							((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_I_MONTH);
						}
						else if (paymentMonth == 0){
							((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_I_YEAR);
						}
						break;
					}
					case 2:{
						if (paymentMonth == 1){
							((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_II_MONTH);
						}
					}
					case 3:{
						if (paymentMonth == 1){
							((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_III_MONTH);
						}
					}
					case 4:{
						if (paymentMonth == 1){
							((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_LITE_MONTH);
						}
						else if (paymentMonth == 0){
							((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_LITE_YEAR);
						}
					}
				}
				paymentMonth = -1;
				break;
			}
			case R.id.payment_fortumo:{
//				if (parameterType == 4 && paymentMonth == 1){
					((ManagerActivityLollipop)context).showFortumo();
//					Intent intent = new Intent(((ManagerActivity)context), FortumoPayment.class);
//					startActivity(intent);
//				}
				paymentMonth = -1;
				break;
			}
			case R.id.payment_credit_card:{
				paymentPerMonth.setVisibility(View.VISIBLE);
				paymentPerYear.setVisibility(View.VISIBLE);
				selectRecurring.setVisibility(View.VISIBLE);
				paymentUpgradeComment.setVisibility(View.VISIBLE);
				paymentCreditCard.setVisibility(View.GONE);
				paymentFortumo.setVisibility(View.GONE);
				
//				((ManagerActivity)context).showCC(parameterType, accounts, paymentMonth, true);
//				paymentMonth = -1;
				break;
			}
		}*/
	}
}
