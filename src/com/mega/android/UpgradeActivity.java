package com.mega.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
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
	private TextView storage;
	private TextView bandwidth;
	private TextView pricingPerMonth;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upgrade);
		
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		aB = getSupportActionBar();
		aB.setHomeButtonEnabled(true);
		aB.setLogo(R.drawable.ic_action_navigation_accept);
		
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
	 * 		 megaApi.getPaymentUrl(p.getHandle(1), this);
		 
	        */
	 
}
