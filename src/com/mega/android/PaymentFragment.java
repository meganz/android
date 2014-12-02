package com.mega.android;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;

public class PaymentFragment extends Fragment implements MegaRequestListenerInterface{
	
	public enum AccountType {
		
		FREE(0, R.drawable.ic_free),
		PRO_1(1, R.drawable.ic_pro_1, R.string.pro1_account),
		PRO_2(2, R.drawable.ic_pro_2, R.string.pro2_account),
		PRO_3(3, R.drawable.ic_pro_3, R.string.pro3_account);
		
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
	
	private ActionBar aB;
	private AccountType accountType;
	private ImageView packageIcon;
	private TextView packageName;
	private TextView storage;
	private TextView bandwidth;
	private TextView perMonth;
	private TextView perYear;
	private TextView pricingFrom;
//	private TextView perMonthTitle;
	int parameterType;	
	MegaApiAndroid megaApi;
	Context context;
	
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
		
		parameterType = this.getArguments().getInt("type");

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		
		aB.setTitle(R.string.section_account);
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
		storage = (TextView) v.findViewById(R.id.pro_storage);
		bandwidth = (TextView) v.findViewById(R.id.pro_bandwidth);
		pricingFrom = (TextView) v.findViewById(R.id.pricing_from);
		
//		perMonthTitle = (TextView) findViewById(R.id.per_month);
		perMonth = (TextView) v.findViewById(R.id.per_month_price);
		perYear = (TextView) v.findViewById(R.id.per_year_price);
		
		accountType = AccountType.getById(parameterType);

		packageIcon.setImageResource(accountType.getImageResource());
		packageName.setText(accountType.getNameResource());
		
		switch(parameterType){
		
			case 1:{
				storage.setText("500 GB");
				bandwidth.setText("12 TB");
				pricingFrom.setText("from 8,33€ per month");
		            
				perMonth.setText("9.99€");
				perYear.setText("99.99€");
				break;
			}
			
			case 2:{
				storage.setText("2 TB");
				bandwidth.setText("48 TB");
				pricingFrom.setText("from 16.50€ per month");
	           
				perMonth.setText("19.99€");
				perYear.setText("199.99€");
				break;
			}
			
			case 3:{
				storage.setText("4 TB");
				bandwidth.setText("96 TB");
				pricingFrom.setText("from 25€ per month");
				perMonth.setText("29.99€");
				perYear.setText("299.99€");
				break;
			}
			
		}
		
		return v;
	}	
	
	public void payYear() {
		log("yearly");
		
		switch(parameterType){
		
			case 1:{
				String handle = "7472683699866478542";
				megaApi.getPaymentUrl(Long.valueOf(handle),this);		
				break;
			}
			case 2:{
				String handle = "370834413380951543";
				megaApi.getPaymentUrl(Long.valueOf(handle),this);		
				break;
			}
			case 3:{
				String handle = "7225413476571973499";
				megaApi.getPaymentUrl(Long.valueOf(handle),this);		
				break;
			}
			
		}
	}
	
	public void payMonth() {
		log("monthly");

		switch(parameterType){
		
			case 1:{
				String handle = "1560943707714440503";
				megaApi.getPaymentUrl(Long.valueOf(handle),this);		
				break;
			}
			case 2:{
				String handle = "7974113413762509455";
				megaApi.getPaymentUrl(Long.valueOf(handle),this);		
				break;
			}
			case 3:{
				String handle = "-2499193043825823892";
				megaApi.getPaymentUrl(Long.valueOf(handle),this);		
				break;
			}
			
		}
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
		 
		if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_URL){
	            log("PAYMENT URL: " + request.getLink());
	            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(request.getLink()));
	            startActivity(browserIntent);
	            
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
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
		aB = ((ActionBarActivity)activity).getSupportActionBar();
	}
	
	public static void log(String message) {
		Util.log("UpgradePaymentActivity", message);
	}
}
