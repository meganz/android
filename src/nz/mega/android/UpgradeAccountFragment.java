package nz.mega.android;

import java.text.DecimalFormat;
import java.util.ArrayList;

import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaPricing;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class UpgradeAccountFragment extends Fragment implements MegaRequestListenerInterface{		
	
	public ArrayList<Product> accounts;
	
	public static int MY_ACCOUNT_FRAGMENT = 5000;
	public static int UPGRADE_ACCOUNT_FRAGMENT = 5001;
	public static int PAYMENT_FRAGMENT = 5002;
	
	private ActionBar aB;
	private MegaApiAndroid megaApi;
	private TextView pro1Title;
	private TextView pro2Title;
	private TextView pro3Title;
	private TextView proStorage1;
	private TextView proStorage2;
	private TextView proStorage3;
	private TextView proBandwith1;
	private TextView proBandwith2;
	private TextView proBandwith3;
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
	private RelativeLayout pro1Layout;
	private RelativeLayout pro1LayoutTrans;
	private RelativeLayout pro3Layout;
	private RelativeLayout pro2Layout;
	private Fragment selectMembership;
	Context context;
	MegaUser myUser;
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		accounts = new ArrayList<Product>();
		
		super.onCreate(savedInstanceState);
		log("onCreate");
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		
		aB.setTitle(R.string.action_upgrade_account);
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);
		
		float scaleText;
		if (scaleH < scaleW){
			scaleText = scaleH;
		}
		else{
			scaleText = scaleW;
		}

		View v = null;
		v = inflater.inflate(R.layout.activity_upgrade, container, false);
		
		pro1 = (ImageView) v.findViewById(R.id.pro1_image);
		pro2 = (ImageView) v.findViewById(R.id.pro2_image);
		pro3 = (ImageView) v.findViewById(R.id.pro3_image);
		
		pro1Layout = (RelativeLayout) v.findViewById(R.id.pro1_layout);
		pro2Layout = (RelativeLayout) v.findViewById(R.id.pro2_layout);
		pro3Layout = (RelativeLayout) v.findViewById(R.id.pro3_layout);	
	
		pro1.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
		pro1.getLayoutParams().height = Util.px2dp((100*scaleH), outMetrics);
		pro2.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
		pro2.getLayoutParams().height = Util.px2dp((100*scaleH), outMetrics);
		pro3.getLayoutParams().width = Util.px2dp((100*scaleW), outMetrics);
		pro3.getLayoutParams().height = Util.px2dp((100*scaleH), outMetrics);
		
		pro1Title = (TextView) v.findViewById(R.id.pro1_title);
		pro2Title = (TextView) v.findViewById(R.id.pro2_title);
		pro3Title = (TextView) v.findViewById(R.id.pro3_title);
		
		proStorage1 = (TextView) v.findViewById(R.id.pro1_storage_title);
		proStorage2 = (TextView) v.findViewById(R.id.pro2_storage_title);
		proStorage3 = (TextView) v.findViewById(R.id.pro3_storage_title);
		
		storage1 = (TextView) v.findViewById(R.id.pro1_storage);
		storage2 = (TextView) v.findViewById(R.id.pro2_storage);
		storage3 = (TextView) v.findViewById(R.id.pro3_storage);
		
		proBandwith1 = (TextView) v.findViewById(R.id.pro1_bandwidth_title);
		proBandwith2 = (TextView) v.findViewById(R.id.pro2_bandwidth_title);
		proBandwith3 = (TextView) v.findViewById(R.id.pro3_bandwidth_title);
		
		bandwidth1 = (TextView) v.findViewById(R.id.pro1_bandwidth);
		bandwidth2 = (TextView) v.findViewById(R.id.pro2_bandwidth);
		bandwidth3 = (TextView) v.findViewById(R.id.pro3_bandwidth);
		
		pricingPerMonth1 = (TextView) v.findViewById(R.id.pricing1_from);
		pricingPerMonth2 = (TextView) v.findViewById(R.id.pricing2_from);
		pricingPerMonth3 = (TextView) v.findViewById(R.id.pricing3_from);
		
		pro1Title.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));
		pro2Title.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));
		pro3Title.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));
		
		proStorage1.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		proStorage2.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		proStorage3.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		
		storage1.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		storage2.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		storage3.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		
		proBandwith1.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		proBandwith2.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		proBandwith3.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		
		bandwidth1.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		bandwidth2.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		bandwidth3.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		
		pricingPerMonth1.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		pricingPerMonth2.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		pricingPerMonth3.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		
		megaApi.getAccountDetails(this);

		megaApi.getPricing(this);
		
		return v;
	}	

	public void onUpgrade1Click(View view) {
		((ManagerActivity)context).showpF(1, accounts);
	}

	public void onUpgrade2Click(View view) {
		((ManagerActivity)context).showpF(2, accounts);
	}

	public void onUpgrade3Click(View view) {
		((ManagerActivity)context).showpF(3, accounts);
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

		if (request.getType() == MegaRequest.TYPE_GET_PRICING){
			MegaPricing p = request.getPricing();

			for (int i=0;i<p.getNumProducts();i++){
				log("p["+ i +"] = " + p.getHandle(i) + "__" + p.getAmount(i) + "___" + p.getGBStorage(i) + "___" + p.getMonths(i) + "___" + p.getProLevel(i) + "___" + p.getGBTransfer(i));

				Product account = new Product (p.getHandle(i), p.getProLevel(i), p.getMonths(i), p.getGBStorage(i), p.getAmount(i), p.getGBTransfer(i));

				if(account.getLevel()==1&&account.getMonths()==12){
					storage1.setText(account.getStorage()+"GB");
					bandwidth1.setText(sizeTranslation(account.getTransfer(),0));
					double saving1 = account.getAmount()/12.00/100.00;	    	            
					String saving1String =df.format(saving1);
					pricingPerMonth1.setText("from " + saving1String +" € per month");
				}
				else if(account.getLevel()==2&&account.getMonths()==12){
					storage2.setText(sizeTranslation(account.getStorage(),0));
					bandwidth2.setText(sizeTranslation(account.getTransfer(),0));
					double saving2 = account.getAmount()/12.00/100.00;
					String saving2String =df.format(saving2);
					pricingPerMonth2.setText("from " + saving2String +" € per month");
				}
				else if(account.getLevel()==3&&account.getMonths()==12){	                	 
					storage3.setText(sizeTranslation(account.getStorage(),0));         
					bandwidth3.setText(sizeTranslation(account.getTransfer(),0));
					double saving3 = account.getAmount()/12.00/100.00;
					String saving3String =df.format(saving3);
					pricingPerMonth3.setText("from " + saving3String +" € per month");
				}

				accounts.add(account);
			}    
			/*RESULTS
	            p[0] = 1560943707714440503__999___500___1___1___1024 - PRO 1 montly
        		p[1] = 7472683699866478542__9999___500___12___1___12288 - PRO 1 annually
        		p[2] = 7974113413762509455__1999___2048___1___2___4096  - PRO 2 montly
        		p[3] = 370834413380951543__19999___2048___12___2___49152 - PRO 2 annually
        		p[4] = -2499193043825823892__2999___4096___1___3___8192 - PRO 3 montly
        		p[5] = 7225413476571973499__29999___4096___12___3___98304 - PRO 3 annually*/
		}
		if (request.getType() == MegaRequest.TYPE_ACCOUNT_DETAILS){
			log ("account_details request");
			if (e.getErrorCode() == MegaError.API_OK){

				MegaAccountDetails accountInfo = request.getMegaAccountDetails();

				int accountType = accountInfo.getProLevel();
				switch(accountType){				
	
					case 1:{
						pro1Layout.setBackgroundColor(Color.parseColor("#80ffffff"));
						pro1Layout.setClickable(false);
						
						AlphaAnimation alpha = new AlphaAnimation(0.5F, 0.5F);
						alpha.setDuration(0); 
						alpha.setFillAfter(true); 
						pro1.startAnimation(alpha);
						
						pro1Title.setTextColor(getResources().getColor(R.color.transparent_black));		
						proStorage1.setTextColor(getResources().getColor(R.color.transparent_black));	
						storage1.setTextColor(getResources().getColor(R.color.transparent_black));
						proBandwith1.setTextColor(getResources().getColor(R.color.transparent_black));						
						bandwidth1.setTextColor(getResources().getColor(R.color.transparent_black));						
						pricingPerMonth1.setTextColor(getResources().getColor(R.color.transparent_mega));
						break;
					}	
					case 2:{
						pro2Layout.setBackgroundColor(Color.parseColor("#80ffffff"));
						pro2Layout.setClickable(false);
						
						AlphaAnimation alpha = new AlphaAnimation(0.5F, 0.5F);
						alpha.setDuration(0); 
						alpha.setFillAfter(true); 
						pro2.startAnimation(alpha);
						
						pro2Title.setTextColor(getResources().getColor(R.color.transparent_black));		
						proStorage2.setTextColor(getResources().getColor(R.color.transparent_black));	
						storage2.setTextColor(getResources().getColor(R.color.transparent_black));
						proBandwith2.setTextColor(getResources().getColor(R.color.transparent_black));						
						bandwidth2.setTextColor(getResources().getColor(R.color.transparent_black));						
						pricingPerMonth2.setTextColor(getResources().getColor(R.color.transparent_mega));
						break;
					}	
					case 3:{
						pro3Layout.setBackgroundColor(Color.parseColor("#80ffffff"));
						pro3Layout.setClickable(false);
						
						AlphaAnimation alpha = new AlphaAnimation(0.5F, 0.5F);
						alpha.setDuration(0); 
						alpha.setFillAfter(true); 
						pro3.startAnimation(alpha);
						
						pro3Title.setTextColor(getResources().getColor(R.color.transparent_black));		
						proStorage3.setTextColor(getResources().getColor(R.color.transparent_black));	
						storage3.setTextColor(getResources().getColor(R.color.transparent_black));
						proBandwith3.setTextColor(getResources().getColor(R.color.transparent_black));						
						bandwidth3.setTextColor(getResources().getColor(R.color.transparent_black));						
						pricingPerMonth3.setTextColor(getResources().getColor(R.color.transparent_mega));
						break;
					}
				}
			}
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
	
	public ArrayList<Product> getAccounts(){
		return accounts;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
		aB = ((ActionBarActivity)activity).getSupportActionBar();
	}
	
	public static void log(String log) {
		Util.log("UpgradeAccountFragment", log);
	}
}
