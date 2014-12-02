package com.mega.android;

import org.json.JSONArray;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;

public class UpgradePaymentActivity extends ActionBarActivity implements MegaRequestListenerInterface{
	
	/*
	 * Account type enum
	 */
	public enum AccountType {
		
		FREE(0, R.drawable.ic_free),
		PRO_1(1, R.drawable.ic_pro_1, R.string.general_pro_1),
		PRO_2(2, R.drawable.ic_pro_2, R.string.general_pro_2),
		PRO_3(3, R.drawable.ic_pro_3, R.string.general_pro_3);
		
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
	
	public static String ACCOUNT_TYPE_EXTRA = "account_type";

	private AccountType accountType;

	private ImageView packageIcon;
	private TextView packageName;
	private TextView storage;
	private TextView bandwidth;
	private TextView perMonth;
	private TextView perYear;
	private TextView pricingFrom;
//	private TextView perMonthTitle;
	int typeAccount;
	
	private double monthlyPrice, yearlyPrice;
	private String monthlyHash, yearlyHash;
	
	MegaApiAndroid megaApi;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upgrade_payment);


		aB = getSupportActionBar();
		aB.setHomeButtonEnabled(true);
		aB.setLogo(R.drawable.ic_action_navigation_accept);
		
		packageIcon = (ImageView) findViewById(R.id.pro_image);
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = getResources().getDisplayMetrics().density;
		
		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);
		
		packageIcon.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
		packageIcon.getLayoutParams().height = Util.px2dp((100*scaleW), outMetrics);
		
		packageName = (TextView) findViewById(R.id.pro_title);
		storage = (TextView) findViewById(R.id.pro_storage);
		bandwidth = (TextView) findViewById(R.id.pro_bandwidth);
		pricingFrom = (TextView) findViewById(R.id.pricing_from);
		
//		perMonthTitle = (TextView) findViewById(R.id.per_month);
		perMonth = (TextView) findViewById(R.id.per_month_price);
		perYear = (TextView) findViewById(R.id.per_year_price);

		accountType = AccountType.getById(getIntent().getIntExtra(ACCOUNT_TYPE_EXTRA, 0));
		typeAccount = getIntent().getIntExtra(ACCOUNT_TYPE_EXTRA, 0);

		packageIcon.setImageResource(accountType.getImageResource());
		packageName.setText(accountType.getNameResource());
		
		switch(typeAccount){
		
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
		
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
//		updatePricing();
//		megaApi.getPaymentUrl(p.getHandle(1), this);
	}
	
	/*
	 * Get pricing information
	 */
	private void updatePricing() {
//		final ProgressDialog updateProgress = Util.createProgress(this, R.string.general_updating);
//		updateProgress.show();
//		OLD_MegaApi.getPricing(megaApi, new GenericObjectListener() {
//
//			@Override
//			public void onResult(Object responseObject, OLD_MegaError error) {
//				updateProgress.dismiss();
//				log(responseObject + " of obj " + error + " of Error");
//				if (error != null) {
//					Util.showErrorAlertDialogFinish(error,
//							UpgradePaymentActivity.this);
//				} else {
//					parseJson((JSONArray) responseObject);
//				}
//			}
//		});
	}
	
	public void onYearlyClick(View view) {
		log("yearly");
		
		switch(typeAccount){
		
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
	
	public void onMonthlyClick(View view) {
		log("monthly");

		switch(typeAccount){
		
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
	
	/*
	 * Request URL for payment
	 */
	private void requestUrl(String hash, double price) {
//		if (hash == null) {
//			return;
//		}
//		final ProgressDialog updateProgress = Util.createProgress(this,
//				R.string.general_updating);
//		updateProgress.show();
//		OLD_MegaApi.getPaymentUrl(megaApi, hash, price, new GenericObjectListener() {
//			
//			@Override
//			public void onResult(Object responseObject, OLD_MegaError error) {
//				updateProgress.dismiss();
//				if (error != null) {
//					Util.showErrorAlertDialogFinish(error, UpgradePaymentActivity.this);
//				} else {
//					String url = (String)responseObject;
//					log(url + " OF URL!");
//					// Start browser with payment URL
//					Util.openUrl(UpgradePaymentActivity.this, url);
//				}
//			}
//		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    // Respond to the action bar's Up/Home button
		    case android.R.id.home:{
		    	finish();
		    	return true;
		    }
		}	    
	    return super.onOptionsItemSelected(item);
	}

	public static void log(String message) {
		Util.log("UpgradePaymentActivity", message);
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
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_URL){
            log("PAYMENT URL: " + request.getLink());
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(request.getLink()));
            startActivity(browserIntent);
            
        }
		
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		// TODO Auto-generated method stub
		
	}
	
	
}
