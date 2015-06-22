package nz.mega.android;

import java.security.Principal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;

import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaPricing;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

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
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.OnTabChangeListener;

public class ChooseAccountActivity extends PinActivity implements MegaRequestListenerInterface, OnClickListener{
	
	private TextView windowTitle;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	private MegaApiAndroid megaApi;
	ActionBar aB;
	
	public ArrayList<Product> accounts;
	
	private TextView freeTitle;
	private TextView freeStorage;
	private TextView freeStorageTitle;	
	private TextView freeBandwith;
	private TextView freeBandwithTitle;
	private ImageView free;
	private RelativeLayout freeLayout;
	private TextView pricingPerMonthFree;
	
	private TextView proLiteTitle;
	private TextView pro1Title;
	private TextView pro2Title;
	private TextView pro3Title;
	private TextView proStorageLite;
	private TextView proStorage1;
	private TextView proStorage2;
	private TextView proStorage3;	
	private TextView proBandwithLite;
	private TextView proBandwith1;
	private TextView proBandwith2;
	private TextView proBandwith3;
	private TextView storageLite;
	private TextView bandwidthLite;
	private TextView pricingPerMonthLite;
	private TextView storage1;
	private TextView bandwidth1;
	private TextView pricingPerMonth1;
	private TextView storage2;
	private TextView bandwidth2;
	private TextView pricingPerMonth2;
	private TextView storage3;
	private TextView bandwidth3;
	private TextView pricingPerMonth3;
	private ImageView proLite;
	private ImageView pro1;
	private ImageView pro2;
	private ImageView pro3;
	private RelativeLayout proLiteLayout;
	private RelativeLayout pro1Layout;
	private RelativeLayout pro3Layout;
	private RelativeLayout pro2Layout;
	
	long paymentBitSetLong;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate first");
		super.onCreate(savedInstanceState);
		
		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);
		
	    float scaleText;
		if (scaleH < scaleW){
			scaleText = scaleH;
		}
		else{
			scaleText = scaleW;
		}
		
//		DatabaseHandler dbH = new DatabaseHandler(getApplicationContext());
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		if (dbH.getCredentials() == null){
			ManagerActivity.logout(this, megaApi, false);
			return;
		}
		
//		if (savedInstanceState != null){
//			folderSelected = savedInstanceState.getBoolean("folderSelected", false);
//		}
		
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		megaApi = ((MegaApplication)getApplication()).getMegaApi();		
	
		setContentView(R.layout.activity_choose_account);   		
		
//		windowTitle = (TextView) findViewById(R.id.file_explorer_window_title);
		
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
		megaApi.getPricing(this);
	}
	
	public void onFreeClick (View view){
		log("onFreeClick");		
		
		Intent intent = null;
		intent = new Intent(this,ManagerActivity.class);
		intent.putExtra("firstTimeCam", true);
		intent.putExtra("upgradeAccount", false);
		startActivity(intent);
		finish();
	}
	
	public void onUpgrade1Click(View view) {
//		((ManagerActivity)context).showpF(1, accounts);
		log("onUpgrade1Click");		
		
		Intent intent = null;
		intent = new Intent(this,ManagerActivity.class);
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
		intent = new Intent(this,ManagerActivity.class);
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
		intent = new Intent(this,ManagerActivity.class);
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
		intent = new Intent(this,ManagerActivity.class);
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
		// TODO Auto-generated method stub
		
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
			paymentBitSetLong = request.getNumber();
		}
		if (request.getType() == MegaRequest.TYPE_GET_PRICING){
			MegaPricing p = request.getPricing();

			for (int i=0;i<p.getNumProducts();i++){
				log("p["+ i +"] = " + p.getHandle(i) + "__" + p.getAmount(i) + "___" + p.getGBStorage(i) + "___" + p.getMonths(i) + "___" + p.getProLevel(i) + "___" + p.getGBTransfer(i));

				Product account = new Product (p.getHandle(i), p.getProLevel(i), p.getMonths(i), p.getGBStorage(i), p.getAmount(i), p.getGBTransfer(i));

				if(account.getLevel()==1&&account.getMonths()==1){
					storage1.setText(account.getStorage()+"GB");
					bandwidth1.setText(account.getTransfer()/1024 + " TB");
					double saving1 = account.getAmount()/100.00;	    	            
					String saving1String =df.format(saving1);
					pricingPerMonth1.setText(saving1String +" € " + getString(R.string.per_month));
				}
				else if(account.getLevel()==2&&account.getMonths()==1){
					storage2.setText(sizeTranslation(account.getStorage(),0));
					double saving2 = account.getAmount()/100.00;
					String saving2String =df.format(saving2);
					pricingPerMonth2.setText(saving2String +" € " + getString(R.string.per_month));
					bandwidth2.setText(sizeTranslation(account.getTransfer(),0));
				}
				else if(account.getLevel()==3&&account.getMonths()==1){	                	 
					storage3.setText(sizeTranslation(account.getStorage(),0));         
					double saving3 = account.getAmount()/100.00;
					String saving3String =df.format(saving3);
					pricingPerMonth3.setText(saving3String +" € " + getString(R.string.per_month));
					bandwidth3.setText(sizeTranslation(account.getTransfer(),0));
				}
				else if (account.getLevel()==4&&account.getMonths()==1){
					storageLite.setText(account.getStorage()+"GB");
					bandwidthLite.setText(account.getTransfer()/1024 + " TB");
					double savingLite = account.getAmount()/100.00;	    	            
					String savingLiteString =df.format(savingLite);
					pricingPerMonthLite.setText(savingLiteString +" € " + getString(R.string.per_month));
				}
				accounts.add(account);
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

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		// TODO Auto-generated method stub
		
	}
	
	public static void log(String message) {
		Util.log("ChooseAccountActivity", message);
	}
}
