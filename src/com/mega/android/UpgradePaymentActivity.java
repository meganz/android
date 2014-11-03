package com.mega.android;

import org.json.JSONArray;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;

public class UpgradePaymentActivity extends ActionBarActivity{
	
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
		
		public int getNameResouce() {
			return nameResource;
		}
	}
	
	private ActionBar aB;
	
	public static String ACCOUNT_TYPE_EXTRA = "account_type";

	private AccountType accountType;

	private ImageView packageIconView;
	private TextView packageNameView;
	private TextView storageView;
	private TextView bandwidthView;
	private TextView perMonthView;
	private TextView perYearView;
	
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
		
		packageIconView = (ImageView) findViewById(R.id.package_icon);
		packageNameView = (TextView) findViewById(R.id.package_name);
		storageView = (TextView) findViewById(R.id.storage);
		bandwidthView = (TextView) findViewById(R.id.bandwidth);
		perMonthView = (TextView) findViewById(R.id.per_month);
		perYearView = (TextView) findViewById(R.id.per_year);

		accountType = AccountType.getById(getIntent().getIntExtra(
				ACCOUNT_TYPE_EXTRA, 0));

		packageIconView.setImageResource(accountType.getImageResource());
		packageNameView.setText(accountType.getNameResouce());
		
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
		updatePricing();
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
	
	/*
	 * Parse pricing information
	 */
	private void parseJson(JSONArray obj) {
//		String priceFormat = "\u20ac %s %s";
//		try {
//			for (int i = 0; i < obj.length(); i++) {
//				JSONArray item = obj.getJSONArray(i);
//				String hash = item.getString(0);
//				int type = item.getInt(1);
//				long storage = item.getLong(2) * 1024 * 1024 * 1024;
//				long bandwidth = item.getLong(3) * 1024 * 1024 * 1024;
//				int months = item.getInt(4);
//				double price = item.getDouble(5);
//				//String currency = item.getString(6);
//				if (AccountType.getById(type) != accountType) {
//					continue;
//				}
//
//				if (months == 1) {
//					monthlyPrice = price;
//					monthlyHash = hash;
//					storageView.setText(Formatter.formatShortFileSize(this,
//							storage));
//					bandwidthView.setText(Formatter.formatShortFileSize(this,
//							bandwidth));
//					perMonthView.setText(String.format(priceFormat, price,
//							getString(R.string.upgrade_per_month)));
//
//				} else {
//					yearlyPrice = price;
//					yearlyHash = hash;
//					perYearView.setText(String.format(priceFormat, price,
//							getString(R.string.upgrade_per_year)));
//				}
//			}
//		} catch (JSONException e) {
//			Util.showErrorAlertDialogFinish(OLD_MegaError.RESPONSE_ENCODING_ERROR,
//					UpgradePaymentActivity.this);
//		}
	}
	
	public void onYearlyClick(View view) {
		log("yearly");
		requestUrl(yearlyHash, yearlyPrice);
	}
	
	public void onMonthlyClick(View view) {
		log("monthly");
		requestUrl(monthlyHash, monthlyPrice);
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
	
	
}
