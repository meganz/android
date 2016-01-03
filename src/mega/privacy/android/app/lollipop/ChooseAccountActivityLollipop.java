package mega.privacy.android.app.lollipop;

import java.security.Principal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaPricing;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.OnTabChangeListener;

public class ChooseAccountActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, OnClickListener{
	
	private TextView windowTitle;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	private MegaApiAndroid megaApi;
	ActionBar aB;
	
	public ArrayList<Product> accounts;
	
	private LinearLayout mainLinearLayout;
	
	private RelativeLayout freeLayout;
	private RelativeLayout proLiteLayout;
	private RelativeLayout pro1Layout;
	private RelativeLayout pro3Layout;
	private RelativeLayout pro2Layout;
	
	private RelativeLayout proLiteTransparentLayout;
	private RelativeLayout pro1TransparentLayout;
	private RelativeLayout pro3TransparentLayout;
	private RelativeLayout pro2TransparentLayout;
	
	private TextView proLitePriceInteger;
	private TextView proLitePriceDecimal;
	private TextView proLiteStorageInteger;
	private TextView proLiteStorageGb;
	private TextView proLiteBandwidthInteger;
	private TextView proLiteBandwidthTb;
	
	private TextView pro1PriceInteger;
	private TextView pro1PriceDecimal;
	private TextView pro1StorageInteger;
	private TextView pro1StorageGb;
	private TextView pro1BandwidthInteger;
	private TextView pro1BandwidthTb;
	
	private TextView pro2PriceInteger;
	private TextView pro2PriceDecimal;
	private TextView pro2StorageInteger;
	private TextView pro2StorageGb;
	private TextView pro2BandwidthInteger;
	private TextView pro2BandwidthTb;
	
	private TextView pro3PriceInteger;
	private TextView pro3PriceDecimal;
	private TextView pro3StorageInteger;
	private TextView pro3StorageGb;
	private TextView pro3BandwidthInteger;
	private TextView pro3BandwidthTb;
	
	BitSet paymentBitSet = null;
	long paymentBitSetLong;

	long usedStorage = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate first");
		super.onCreate(savedInstanceState);
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);
		
		float scaleText;
		if (scaleH < scaleW){
			scaleText = scaleH;
		}
		else{
			scaleText = scaleW;
		}
		
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		if (dbH.getCredentials() == null){
			ManagerActivityLollipop.logout(this, megaApi, false);
			return;
		}
		
		megaApi = ((MegaApplication)getApplication()).getMegaApi();		
		
		accounts = new ArrayList<Product>();
	
		setContentView(R.layout.activity_choose_account);  
		
		mainLinearLayout = (LinearLayout) findViewById(R.id.choose_account_main_linear_layout);
		
		freeLayout = (RelativeLayout) findViewById(R.id.choose_account_free_layout);
		freeLayout.setOnClickListener(this);
		proLiteLayout = (RelativeLayout) findViewById(R.id.choose_account_prolite_layout);
		proLiteLayout.setOnClickListener(this);
		pro1Layout = (RelativeLayout) findViewById(R.id.choose_account_pro_i_layout);
		pro1Layout.setOnClickListener(this);
		pro2Layout = (RelativeLayout) findViewById(R.id.choose_account_pro_ii_layout);
		pro2Layout.setOnClickListener(this);
		pro3Layout = (RelativeLayout) findViewById(R.id.choose_account_pro_iii_layout);
		pro3Layout.setOnClickListener(this);
		
		proLiteTransparentLayout = (RelativeLayout) findViewById(R.id.choose_account_prolite_layout_transparent);
		proLiteTransparentLayout.setVisibility(View.INVISIBLE);
		pro1TransparentLayout = (RelativeLayout) findViewById(R.id.choose_account_pro_i_layout_transparent);
		pro1TransparentLayout.setVisibility(View.INVISIBLE);
		pro2TransparentLayout = (RelativeLayout) findViewById(R.id.choose_account_pro_ii_layout_transparent);
		pro2TransparentLayout.setVisibility(View.INVISIBLE);
		pro3TransparentLayout = (RelativeLayout) findViewById(R.id.choose_account_pro_iii_layout_transparent);
		pro3TransparentLayout.setVisibility(View.INVISIBLE);
		
		TextView perMonth = (TextView) findViewById(R.id.choose_account_prolite_per_month_text);
		perMonth.setText("/ " + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));
		perMonth = (TextView) findViewById(R.id.choose_account_pro_i_per_month_text);
		perMonth.setText("/ " + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));
		perMonth = (TextView) findViewById(R.id.choose_account_pro_ii_per_month_text);
		perMonth.setText("/ " + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));
		perMonth = (TextView) findViewById(R.id.choose_account_pro_iii_per_month_text);
		perMonth.setText("/ " + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));
		
		proLitePriceInteger = (TextView) findViewById(R.id.choose_account_prolite_integer_text);
		proLitePriceDecimal = (TextView) findViewById(R.id.choose_account_prolite_decimal_text);
		proLiteStorageInteger = (TextView) findViewById(R.id.choose_account_prolite_storage_value_integer);
		proLiteStorageGb = (TextView) findViewById(R.id.choose_account_prolite_storage_value_gb);
		proLiteBandwidthInteger = (TextView) findViewById(R.id.choose_account_prolite_bandwidth_value_integer);
		proLiteBandwidthTb = (TextView) findViewById(R.id.choose_account_prolite_bandwith_value_tb);
		
		pro1PriceInteger = (TextView) findViewById(R.id.choose_account_pro_i_integer_text);
		pro1PriceDecimal = (TextView) findViewById(R.id.choose_account_pro_i_decimal_text);
		pro1StorageInteger = (TextView) findViewById(R.id.choose_account_pro_i_storage_value_integer);
		pro1StorageGb = (TextView) findViewById(R.id.choose_account_pro_i_storage_value_gb);
		pro1BandwidthInteger = (TextView) findViewById(R.id.choose_account_pro_i_bandwidth_value_integer);
		pro1BandwidthTb = (TextView) findViewById(R.id.choose_account_pro_i_bandwith_value_tb);
		
		pro2PriceInteger = (TextView) findViewById(R.id.choose_account_pro_ii_integer_text);
		pro2PriceDecimal = (TextView) findViewById(R.id.choose_account_pro_ii_decimal_text);
		pro2StorageInteger = (TextView) findViewById(R.id.choose_account_pro_ii_storage_value_integer);
		pro2StorageGb = (TextView) findViewById(R.id.choose_account_pro_ii_storage_value_gb);
		pro2BandwidthInteger = (TextView) findViewById(R.id.choose_account_pro_ii_bandwidth_value_integer);
		pro2BandwidthTb = (TextView) findViewById(R.id.choose_account_pro_ii_bandwith_value_tb);
		
		pro3PriceInteger = (TextView) findViewById(R.id.choose_account_pro_iii_integer_text);
		pro3PriceDecimal = (TextView) findViewById(R.id.choose_account_pro_iii_decimal_text);
		pro3StorageInteger = (TextView) findViewById(R.id.choose_account_pro_iii_storage_value_integer);
		pro3StorageGb = (TextView) findViewById(R.id.choose_account_pro_iii_storage_value_gb);
		pro3BandwidthInteger = (TextView) findViewById(R.id.choose_account_pro_iii_bandwidth_value_integer);
		pro3BandwidthTb = (TextView) findViewById(R.id.choose_account_pro_iii_bandwith_value_tb);
		
		if (paymentBitSet == null){
			megaApi.getPaymentMethods(this);
		}
		
		megaApi.getPricing(this);
		
		
//		windowTitle = (TextView) findViewById(R.id.file_explorer_window_title);
		/*
		aB = getSupportActionBar();
		aB.setHomeButtonEnabled(false);
		aB.setDisplayShowTitleEnabled(true);
		aB.setLogo(R.drawable.ic_launcher);
		aB.setTitle(getString(R.string.choose_account_activity));
		
//		actionBarTitle = getString(R.string.section_cloud_drive);
//		windowTitle.setText(R.string.choose_account_activity);
		
		accounts = new ArrayList<Product>();	
		
		free = (ImageView) findViewById(R.id.free_image);
		proLite = (ImageView) findViewById(R.id.prolite_image);
		pro1 = (ImageView) findViewById(R.id.pro1_image);
		pro2 = (ImageView) findViewById(R.id.pro2_image);
		pro3 = (ImageView) findViewById(R.id.pro3_image);
		
		freeLayout = (RelativeLayout) findViewById(R.id.free_account_layout);
		proLiteLayout = (RelativeLayout) findViewById(R.id.prolite_account_layout);
		pro1Layout = (RelativeLayout) findViewById(R.id.pro1_account_layout);
		pro2Layout = (RelativeLayout) findViewById(R.id.pro2_account_layout);
		pro3Layout = (RelativeLayout) findViewById(R.id.pro3_account_layout);	

		free.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
		free.getLayoutParams().height = Util.px2dp((100*scaleH), outMetrics);
		proLite.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
		proLite.getLayoutParams().height = Util.px2dp((100*scaleH), outMetrics);
		pro1.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
		pro1.getLayoutParams().height = Util.px2dp((100*scaleH), outMetrics);
		pro2.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
		pro2.getLayoutParams().height = Util.px2dp((100*scaleH), outMetrics);
		pro3.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
		pro3.getLayoutParams().height = Util.px2dp((100*scaleH), outMetrics);
		
		freeTitle = (TextView) findViewById(R.id.free_title);
		proLiteTitle = (TextView) findViewById(R.id.prolite_title);
		pro1Title = (TextView) findViewById(R.id.pro1_title);
		pro2Title = (TextView) findViewById(R.id.pro2_title);
		pro3Title = (TextView) findViewById(R.id.pro3_title);
		
		freeStorageTitle = (TextView) findViewById(R.id.free_storage_title);
		proStorageLite = (TextView) findViewById(R.id.prolite_storage_title);
		proStorage1 = (TextView) findViewById(R.id.pro1_storage_title);
		proStorage2 = (TextView) findViewById(R.id.pro2_storage_title);
		proStorage3 = (TextView) findViewById(R.id.pro3_storage_title);
		
		freeStorage = (TextView) findViewById(R.id.free_storage);
		storageLite = (TextView) findViewById(R.id.prolite_storage);
		storage1 = (TextView) findViewById(R.id.pro1_storage);
		storage2 = (TextView) findViewById(R.id.pro2_storage);
		storage3 = (TextView) findViewById(R.id.pro3_storage);
		
		freeBandwithTitle = (TextView) findViewById(R.id.free_bandwidth_title);
		proBandwithLite = (TextView) findViewById(R.id.prolite_bandwidth_title);
		proBandwith1 = (TextView) findViewById(R.id.pro1_bandwidth_title);
		proBandwith2 = (TextView) findViewById(R.id.pro2_bandwidth_title);
		proBandwith3 = (TextView) findViewById(R.id.pro3_bandwidth_title);
		
		freeBandwith = (TextView) findViewById(R.id.free_bandwidth);
		bandwidthLite = (TextView) findViewById(R.id.prolite_bandwidth);
		bandwidth1 = (TextView) findViewById(R.id.pro1_bandwidth);
		bandwidth2 = (TextView) findViewById(R.id.pro2_bandwidth);
		bandwidth3 = (TextView) findViewById(R.id.pro3_bandwidth);
		
		RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) freeBandwith.getLayoutParams();
		p.addRule(RelativeLayout.ALIGN_BOTTOM, freeBandwithTitle.getId());
		freeBandwith.setLayoutParams(p);
		
		pricingPerMonthFree = (TextView) findViewById(R.id.princingfree_from);
		pricingPerMonthLite = (TextView) findViewById(R.id.princinglite_from);
		pricingPerMonth1 = (TextView) findViewById(R.id.pricing1_from);
		pricingPerMonth2 = (TextView) findViewById(R.id.pricing2_from);
		pricingPerMonth3 = (TextView) findViewById(R.id.pricing3_from);
		
		freeTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));
		proLiteTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));
		pro1Title.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));
		pro2Title.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));
		pro3Title.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));
		
		freeStorageTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		proStorageLite.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		proStorage1.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		proStorage2.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		proStorage3.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		
		freeStorage.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		storageLite.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		storage1.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		storage2.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		storage3.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		
		freeBandwithTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		proBandwithLite.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		proBandwith1.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		proBandwith2.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		proBandwith3.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		
		freeBandwith.setTextSize(TypedValue.COMPLEX_UNIT_SP, (17*scaleText));
		bandwidthLite.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		bandwidth1.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		bandwidth2.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		bandwidth3.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		
		pricingPerMonthFree.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		pricingPerMonthLite.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		pricingPerMonth1.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		pricingPerMonth2.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		pricingPerMonth3.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));

		megaApi.getPaymentMethods(this);
		megaApi.getPricing(this);*/
	}
	
	public void onFreeClick (View view){
		log("onFreeClick");		
		
		Intent intent = null;
		intent = new Intent(this,ManagerActivityLollipop.class);
		intent.putExtra("firstTimeCam", true);
		intent.putExtra("upgradeAccount", false);
		startActivity(intent);
		finish();
	}
	
	public void onUpgrade1Click(View view) {
//		((ManagerActivity)context).showpF(1, accounts);
		log("onUpgrade1Click");		
		
		Intent intent = null;
		intent = new Intent(this,ManagerActivityLollipop.class);
		intent.putExtra("upgradeAccount", true);
		intent.putExtra("accountType", 1);
		intent.putExtra("paymentBitSetLong", paymentBitSetLong);
		startActivity(intent);
		finish();
	}

	public void onUpgrade2Click(View view) {
//		((ManagerActivity)context).showpF(2, accounts);		
		log("onUpgrade2Click");		
		
		Intent intent = null;
		intent = new Intent(this,ManagerActivityLollipop.class);
		intent.putExtra("upgradeAccount", true);
		intent.putExtra("accountType", 2);
		intent.putExtra("paymentBitSetLong", paymentBitSetLong);
		startActivity(intent);
		finish();
	}

	public void onUpgrade3Click(View view) {
//		((ManagerActivity)context).showpF(3, accounts);
		log("onUpgrade3Click");		
		
		Intent intent = null;
		intent = new Intent(this,ManagerActivityLollipop.class);
		intent.putExtra("upgradeAccount", true);
		intent.putExtra("accountType", 3);
		intent.putExtra("paymentBitSetLong", paymentBitSetLong);
		startActivity(intent);
		finish();
	}
	
	public void onUpgradeLiteClick(View view){
//		((ManagerActivity)context).showpF(4, accounts);
		log("onUpgradeLiteClick");		
		
		Intent intent = null;
		intent = new Intent(this,ManagerActivityLollipop.class);
		intent.putExtra("upgradeAccount", true);
		intent.putExtra("accountType", 4);
		intent.putExtra("paymentBitSetLong", paymentBitSetLong);
		startActivity(intent);
		finish();
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
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.choose_account_free_layout:{
				onFreeClick(v);
				break;
			}
			case R.id.choose_account_prolite_layout:{
				onUpgradeLiteClick(v);
				break;
			}
			case R.id.choose_account_pro_i_layout:{
				onUpgrade1Click(v);
				break;
			}
			case R.id.choose_account_pro_ii_layout:{
				onUpgrade2Click(v);
				break;
			}
			case R.id.choose_account_pro_iii_layout:{
				onUpgrade3Click(v);
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
		DecimalFormat df = new DecimalFormat("#.##");

		if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_METHODS){
			if (e.getErrorCode() == MegaError.API_OK){
				paymentBitSet = Util.convertToBitSet(request.getNumber());
			}
		}
		if (request.getType() == MegaRequest.TYPE_GET_PRICING){
			MegaPricing p = request.getPricing();
//			usedStorage = 501;

			for (int i=0;i<p.getNumProducts();i++){
				log("p["+ i +"] = " + p.getHandle(i) + "__" + p.getAmount(i) + "___" + p.getGBStorage(i) + "___" + p.getMonths(i) + "___" + p.getProLevel(i) + "___" + p.getGBTransfer(i));

				Product account = new Product (p.getHandle(i), p.getProLevel(i), p.getMonths(i), p.getGBStorage(i), p.getAmount(i), p.getGBTransfer(i));

				if(account.getLevel()==1&&account.getMonths()==1){
					log("PRO1: "+account.getStorage());

					double price = account.getAmount()/100.00;
					String priceString = df.format(price);
					String [] s = priceString.split("\\.");
					if (s.length == 1){
						pro1PriceInteger.setText(s[0]);
						pro1PriceDecimal.setText("");
					}
					else if (s.length == 2){
						pro1PriceInteger.setText(s[0]);
						pro1PriceDecimal.setText("." + s[1] + " €");
					}
					
					pro1StorageInteger.setText(""+account.getStorage());
					pro1StorageGb.setText(" GB");
					
					pro1BandwidthInteger.setText(""+account.getTransfer()/1024);
					pro1BandwidthTb.setText(" TB");
				}
				else if(account.getLevel()==2&&account.getMonths()==1){
					log("PRO2: "+account.getStorage());
					
					double price = account.getAmount()/100.00;
					String priceString = df.format(price);
					String [] s = priceString.split("\\.");
					if (s.length == 1){
						pro2PriceInteger.setText(s[0]);
						pro2PriceDecimal.setText("");
					}
					else if (s.length == 2){
						pro2PriceInteger.setText(s[0]);
						pro2PriceDecimal.setText("." + s[1] + " €");
					}
					
					pro2StorageInteger.setText(sizeTranslation(account.getStorage(),0));
					pro2StorageGb.setText(" TB");
					
					pro2BandwidthInteger.setText(""+account.getTransfer()/1024);
					pro2BandwidthTb.setText(" TB");
				}
				else if(account.getLevel()==3&&account.getMonths()==1){	                	 
					log("PRO3: "+account.getStorage());
					
					double price = account.getAmount()/100.00;
					String priceString = df.format(price);
					String [] s = priceString.split("\\.");
					if (s.length == 1){
						pro3PriceInteger.setText(s[0]);
						pro3PriceDecimal.setText("");
					}
					else if (s.length == 2){
						pro3PriceInteger.setText(s[0]);
						pro3PriceDecimal.setText("." + s[1] + " €");
					}
					
					pro3StorageInteger.setText(sizeTranslation(account.getStorage(),0));
					pro3StorageGb.setText(" TB");
					
					pro3BandwidthInteger.setText(""+account.getTransfer()/1024);
					pro3BandwidthTb.setText(" TB");
				}
				else if (account.getLevel()==4&&account.getMonths()==1){
					log("Lite: "+account.getStorage());
					
					double price = account.getAmount()/100.00;
					String priceString = df.format(price);
					String [] s = priceString.split("\\.");
					if (s.length == 1){
						proLitePriceInteger.setText(s[0]);
						proLitePriceDecimal.setText("");
					}
					else if (s.length == 2){
						proLitePriceInteger.setText(s[0]);
						proLitePriceDecimal.setText("." + s[1] + " €");
					}
					
					proLiteStorageInteger.setText(""+account.getStorage());
					proLiteStorageGb.setText(" GB");
					
					proLiteBandwidthInteger.setText(""+account.getTransfer()/1024);
					proLiteBandwidthTb.setText(" TB");
				}
				accounts.add(account);
			}
//			/*RESULTS
//            p[0] = 1560943707714440503__999___500___1___1___1024 - PRO 1 montly
//    		p[1] = 7472683699866478542__9999___500___12___1___12288 - PRO 1 annually
//    		p[2] = 7974113413762509455__1999___2048___1___2___4096  - PRO 2 montly
//    		p[3] = 370834413380951543__19999___2048___12___2___49152 - PRO 2 annually
//    		p[4] = -2499193043825823892__2999___4096___1___3___8192 - PRO 3 montly
//    		p[5] = 7225413476571973499__29999___4096___12___3___98304 - PRO 3 annually*/
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		// TODO Auto-generated method stub
		
	}
	
	public static void log(String message) {
		Util.log("ChooseAccountActivity", message);
	}
}
