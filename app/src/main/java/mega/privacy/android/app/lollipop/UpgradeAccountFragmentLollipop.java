package mega.privacy.android.app.lollipop;

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
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class UpgradeAccountFragmentLollipop extends Fragment implements OnClickListener{
	

	private ActionBar aB;
	private MegaApiAndroid megaApi;
	public MyAccountInfo myAccountInfo;
	
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
	
	Context context;

	@Override
	public void onDestroy(){				

		super.onDestroy();
	}
	
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

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
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
		
		proLiteLayout = (RelativeLayout) v.findViewById(R.id.upgrade_prolite_layout);
		proLiteLayout.setOnClickListener(this);
		pro1Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_i_layout);
		pro1Layout.setOnClickListener(this);
		pro2Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_ii_layout);
		pro2Layout.setOnClickListener(this);
		pro3Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_iii_layout);
		pro3Layout.setOnClickListener(this);
		
		proLiteTransparentLayout = (RelativeLayout) v.findViewById(R.id.upgrade_prolite_layout_transparent);
		proLiteTransparentLayout.setVisibility(View.INVISIBLE);
		pro1TransparentLayout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_i_layout_transparent);
		pro1TransparentLayout.setVisibility(View.INVISIBLE);
		pro2TransparentLayout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_ii_layout_transparent);
		pro2TransparentLayout.setVisibility(View.INVISIBLE);
		pro3TransparentLayout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_iii_layout_transparent);
		pro3TransparentLayout.setVisibility(View.INVISIBLE);
		
		TextView perMonth = (TextView) v.findViewById(R.id.upgrade_prolite_per_month_text);
		perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));
		perMonth = (TextView) v.findViewById(R.id.upgrade_pro_i_per_month_text);
		perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));
		perMonth = (TextView) v.findViewById(R.id.upgrade_pro_ii_per_month_text);
		perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));
		perMonth = (TextView) v.findViewById(R.id.upgrade_pro_iii_per_month_text);
		perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));
		
		proLitePriceInteger = (TextView) v.findViewById(R.id.upgrade_prolite_integer_text);
		proLitePriceDecimal = (TextView) v.findViewById(R.id.upgrade_prolite_decimal_text);
		proLiteStorageInteger = (TextView) v.findViewById(R.id.upgrade_prolite_storage_value_integer);
		proLiteStorageGb = (TextView) v.findViewById(R.id.upgrade_prolite_storage_value_gb);
		proLiteBandwidthInteger = (TextView) v.findViewById(R.id.upgrade_prolite_bandwidth_value_integer);
		proLiteBandwidthTb = (TextView) v.findViewById(R.id.upgrade_prolite_bandwith_value_tb);
		
		pro1PriceInteger = (TextView) v.findViewById(R.id.upgrade_pro_i_integer_text);
		pro1PriceDecimal = (TextView) v.findViewById(R.id.upgrade_pro_i_decimal_text);
		pro1StorageInteger = (TextView) v.findViewById(R.id.upgrade_pro_i_storage_value_integer);
		pro1StorageGb = (TextView) v.findViewById(R.id.upgrade_pro_i_storage_value_gb);
		pro1BandwidthInteger = (TextView) v.findViewById(R.id.upgrade_pro_i_bandwidth_value_integer);
		pro1BandwidthTb = (TextView) v.findViewById(R.id.upgrade_pro_i_bandwith_value_tb);
		
		pro2PriceInteger = (TextView) v.findViewById(R.id.upgrade_pro_ii_integer_text);
		pro2PriceDecimal = (TextView) v.findViewById(R.id.upgrade_pro_ii_decimal_text);
		pro2StorageInteger = (TextView) v.findViewById(R.id.upgrade_pro_ii_storage_value_integer);
		pro2StorageGb = (TextView) v.findViewById(R.id.upgrade_pro_ii_storage_value_gb);
		pro2BandwidthInteger = (TextView) v.findViewById(R.id.upgrade_pro_ii_bandwidth_value_integer);
		pro2BandwidthTb = (TextView) v.findViewById(R.id.upgrade_pro_ii_bandwith_value_tb);
		
		pro3PriceInteger = (TextView) v.findViewById(R.id.upgrade_pro_iii_integer_text);
		pro3PriceDecimal = (TextView) v.findViewById(R.id.upgrade_pro_iii_decimal_text);
		pro3StorageInteger = (TextView) v.findViewById(R.id.upgrade_pro_iii_storage_value_integer);
		pro3StorageGb = (TextView) v.findViewById(R.id.upgrade_pro_iii_storage_value_gb);
		pro3BandwidthInteger = (TextView) v.findViewById(R.id.upgrade_pro_iii_bandwidth_value_integer);
		pro3BandwidthTb = (TextView) v.findViewById(R.id.upgrade_pro_iii_bandwith_value_tb);
		
		if (myAccountInfo.getPaymentBitSet() == null){
			megaApi.getPaymentMethods(myAccountInfo);
		}

		//Check if called recently
//		megaApi.getPricing(myAccountInfo);
		setPricing();
		showAvailableAccount();
		return v;
	}

	public void setPricing(){
		log("setPricing");

		DecimalFormat df = new DecimalFormat("#.##");

		if(myAccountInfo!=null){
			ArrayList<Product> productAccounts = myAccountInfo.getProductAccounts();

			for (int i = 0; i < productAccounts.size(); i++) {
				Product account = productAccounts.get(i);
				if (account.getLevel() == 1 && account.getMonths() == 1) {
					log("PRO1: " + account.getStorage());
					double price = account.getAmount() / 100.00;
					String priceString = df.format(price);
					String[] s = priceString.split("\\.");
					if (s.length == 1) {
						String[] s1 = priceString.split(",");
						if (s1.length == 1) {
							pro1PriceInteger.setText(s1[0]);
							pro1PriceDecimal.setText("");
						} else if (s1.length == 2) {
							pro1PriceInteger.setText(s1[0]);
							pro1PriceDecimal.setText("." + s1[1] + " €");
						}
					} else if (s.length == 2) {
						pro1PriceInteger.setText(s[0]);
						pro1PriceDecimal.setText("." + s[1] + " €");
					}

					pro1StorageInteger.setText("" + account.getStorage());
					pro1StorageGb.setText(" GB");

					pro1BandwidthInteger.setText("" + account.getTransfer() / 1024);
					pro1BandwidthTb.setText(" TB");
				} else if (account.getLevel() == 2 && account.getMonths() == 1) {
					log("PRO2: " + account.getStorage());

					double price = account.getAmount() / 100.00;
					String priceString = df.format(price);
					String[] s = priceString.split("\\.");
					if (s.length == 1) {
						String[] s1 = priceString.split(",");
						if (s1.length == 1) {
							pro2PriceInteger.setText(s1[0]);
							pro2PriceDecimal.setText("");
						} else if (s1.length == 2) {
							pro2PriceInteger.setText(s1[0]);
							pro2PriceDecimal.setText("." + s1[1] + " €");
						}
					} else if (s.length == 2) {
						pro2PriceInteger.setText(s[0]);
						pro2PriceDecimal.setText("." + s[1] + " €");
					}

					pro2StorageInteger.setText(sizeTranslation(account.getStorage(), 0));
					pro2StorageGb.setText(" TB");

					pro2BandwidthInteger.setText("" + account.getTransfer() / 1024);
					pro2BandwidthTb.setText(" TB");
				} else if (account.getLevel() == 3 && account.getMonths() == 1) {
					log("PRO3: " + account.getStorage());

					double price = account.getAmount() / 100.00;
					String priceString = df.format(price);
					String[] s = priceString.split("\\.");
					if (s.length == 1) {
						String[] s1 = priceString.split(",");
						if (s1.length == 1) {
							pro3PriceInteger.setText(s1[0]);
							pro3PriceDecimal.setText("");
						} else if (s1.length == 2) {
							pro3PriceInteger.setText(s1[0]);
							pro3PriceDecimal.setText("." + s1[1] + " €");
						}
					} else if (s.length == 2) {
						pro3PriceInteger.setText(s[0]);
						pro3PriceDecimal.setText("." + s[1] + " €");
					}

					pro3StorageInteger.setText(sizeTranslation(account.getStorage(), 0));
					pro3StorageGb.setText(" TB");

					pro3BandwidthInteger.setText("" + account.getTransfer() / 1024);
					pro3BandwidthTb.setText(" TB");
				} else if (account.getLevel() == 4 && account.getMonths() == 1) {
					log("Lite: " + account.getStorage());

					double price = account.getAmount() / 100.00;
					String priceString = df.format(price);
					String[] s = priceString.split("\\.");
					if (s.length == 1) {
						String[] s1 = priceString.split(",");
						if (s1.length == 1) {
							proLitePriceInteger.setText(s1[0]);
							proLitePriceDecimal.setText("");
						} else if (s1.length == 2) {
							proLitePriceInteger.setText(s1[0]);
							proLitePriceDecimal.setText("." + s1[1] + " €");
						}
					} else if (s.length == 2) {
						proLitePriceInteger.setText(s[0]);
						proLitePriceDecimal.setText("." + s[1] + " €");
					}

					proLiteStorageInteger.setText("" + account.getStorage());
					proLiteStorageGb.setText(" GB");

					proLiteBandwidthInteger.setText("" + account.getTransfer() / 1024);
					proLiteBandwidthTb.setText(" TB");
				}
			}
		}
	}
	
	public void showAvailableAccount(){
		log("checkAvailableAccount: "+myAccountInfo.getAccountType());

		switch(myAccountInfo.getAccountType()){

			case 1:{
				hideProLite();
				break;
			}
			case 2:{
				hideProLite();
				hideProI();
				break;
			}
			case 3:{
				hideProLite();
				hideProI();
				hideProII();
				break;
			}
			case 4:{
				break;
			}
		}
	}

	public void onUpgrade1Click() {
		if (myAccountInfo.getPaymentBitSet() != null){
			((ManagerActivityLollipop)context).showpF(1);
		}
	}

	public void onUpgrade2Click() {
		if (myAccountInfo.getPaymentBitSet() != null){
			((ManagerActivityLollipop)context).showpF(2);
		}
	}

	public void onUpgrade3Click() {
		if (myAccountInfo.getPaymentBitSet() != null){
			((ManagerActivityLollipop)context).showpF(3);
		}
	}
	
	public void onUpgradeLiteClick(){
		if (myAccountInfo.getPaymentBitSet() != null){
			((ManagerActivityLollipop)context).showpF(4);
		}
	}

	private void hideProLite(){
		proLiteTransparentLayout.setVisibility(View.VISIBLE);
	}
	
	private void hideProI(){
		pro1TransparentLayout.setVisibility(View.VISIBLE);
		
//		AlphaAnimation alpha = new AlphaAnimation(0.5F, 0.5F);
//		alpha.setDuration(0); 
//		alpha.setFillAfter(true); 
//		pro1.startAnimation(alpha);
		
	}
	
	private void hideProII(){
		pro2TransparentLayout.setVisibility(View.VISIBLE);
	}
	
	private void hideProIII(){
		pro3TransparentLayout.setVisibility(View.VISIBLE);
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
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
		aB = ((AppCompatActivity)activity).getSupportActionBar();
	}
	
	public static void log(String log) {
		Util.log("UpgradeAccountFragmentLollipop", log);
	}

	public MyAccountInfo getMyAccountInfo() {
		return myAccountInfo;
	}

	public void setMyAccountInfo(MyAccountInfo myAccountInfo) {
		this.myAccountInfo = myAccountInfo;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.upgrade_prolite_layout:{
				onUpgradeLiteClick();
				break;
			}
			case R.id.upgrade_pro_i_layout:{
				onUpgrade1Click();
				break;
			}
			case R.id.upgrade_pro_ii_layout:{
				onUpgrade2Click();
				break;
			}
			case R.id.upgrade_pro_iii_layout:{
				onUpgrade3Click();
				break;
			}
		}
	}
}
