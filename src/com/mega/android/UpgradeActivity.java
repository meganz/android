package com.mega.android;

import com.mega.android.utils.Util;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;

public class UpgradeActivity extends PinActivity{

	private ActionBar aB;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upgrade);
		
		aB = getSupportActionBar();
		aB.setHomeButtonEnabled(true);
		aB.setLogo(R.drawable.ic_action_navigation_accept);
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
}
