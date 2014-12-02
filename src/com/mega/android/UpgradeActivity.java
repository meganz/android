package com.mega.android;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaPricing;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;

public class UpgradeActivity extends PinActivity implements MegaRequestListenerInterface{

	private ActionBar aB;
	private MegaApiAndroid megaApi;
	private TextView storage1;
	private TextView bandwidth1;
	private TextView pricingPerMonth1;
	private TextView storage2;
	private TextView bandwidth2;
	private TextView pricingPerMonth2;
	private TextView storage3;
	private TextView bandwidth3;
	private TextView pricingPerMonth3;
	private ImageView pro1;
	private ImageView pro2;
	private ImageView pro3;
	private Fragment selectMembership;
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upgrade);
		
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		aB = getSupportActionBar();
		aB.setHomeButtonEnabled(true);
		aB.setLogo(R.drawable.ic_action_navigation_accept);
		
		pro1 = (ImageView) findViewById(R.id.pro1_image);
		pro2 = (ImageView) findViewById(R.id.pro2_image);
		pro3 = (ImageView) findViewById(R.id.pro3_image);
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = getResources().getDisplayMetrics().density;
		
		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);
		
		pro1.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
		pro1.getLayoutParams().height = Util.px2dp((100*scaleW), outMetrics);
		pro2.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
		pro2.getLayoutParams().height = Util.px2dp((100*scaleW), outMetrics);
		pro3.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
		pro3.getLayoutParams().height = Util.px2dp((100*scaleW), outMetrics);
		
		storage1 = (TextView) findViewById(R.id.pro1_storage);
		storage2 = (TextView) findViewById(R.id.pro2_storage);
		storage3 = (TextView) findViewById(R.id.pro3_storage);
		
		bandwidth1 = (TextView) findViewById(R.id.pro1_bandwidth);
		bandwidth2 = (TextView) findViewById(R.id.pro2_bandwidth);
		bandwidth3 = (TextView) findViewById(R.id.pro3_bandwidth);
		
		pricingPerMonth1 = (TextView) findViewById(R.id.pricing1_from);
		pricingPerMonth2 = (TextView) findViewById(R.id.pricing2_from);
		pricingPerMonth3 = (TextView) findViewById(R.id.pricing3_from);
		
		megaApi.getAccountDetails(this);
		megaApi.getPricing(this);
	}
	
	/*
	 * Start account upgrade activity for selected type
	 */
	private void upgradePayment(int accountType) {
		Intent intent = new Intent(this, UpgradePaymentActivity.class);
		intent.putExtra(UpgradePaymentActivity.ACCOUNT_TYPE_EXTRA, accountType);
		startActivity(intent);
	}
	
	public void onUpgrade1Click(View view) {
		upgradePayment(1);
	}

	public void onUpgrade2Click(View view) {
		upgradePayment(2);
	}

	public void onUpgrade3Click(View view) {
		upgradePayment(3);
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
		Util.log("UpgradeActivity", message);
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
	            storage1.setText("500 GB");
	            storage2.setText("2 TB");
	            storage3.setText("4 TB");
	            
	            bandwidth1.setText("12 TB");
	            bandwidth2.setText("48 TB");
	            bandwidth3.setText("96 TB");
	            
	            pricingPerMonth1.setText("from 8,33€ per month");
	            pricingPerMonth2.setText("from 16.50€ per month");
	            pricingPerMonth3.setText("from 25€ per month");
	            
	            log("P.SIZE(): " + p.getNumProducts());
	            for (int i=0;i<p.getNumProducts();i++){
	                log("p["+ i +"] = " + p.getHandle(i) + "__" + p.getAmount(i) + "___" + p.getGBStorage(i) + "___" + p.getMonths(i) + "___" + p.getProLevel(i)); 
	            }            
	           
	        }
	        else if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_URL){
	            log("PAYMENT URL: " + request.getLink());
	            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(request.getLink()));
	            startActivity(browserIntent);
	            
	        }

		
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,MegaError e) {
		// TODO Auto-generated method stub
		
	}
	
	/*
	 * 		 
		 
	        */
	 
}
