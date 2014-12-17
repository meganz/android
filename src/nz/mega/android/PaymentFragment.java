package nz.mega.android;

import java.text.DecimalFormat;
import java.util.ArrayList;

import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaPricing;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
	
	public static int MY_ACCOUNT_FRAGMENT = 5000;
	public static int UPGRADE_ACCOUNT_FRAGMENT = 5001;
	public static int PAYMENT_FRAGMENT = 5002;
	
	private ActionBar aB;
	private AccountType accountType;
	private ImageView packageIcon;
	private TextView packageName;
	private TextView storage;
	private TextView bandwidth;
	private TextView perMonth;
	private TextView perYear;
	private TextView pricingFrom;
	private ArrayList<Long> handleUrl;
	private TextView storageTitle;
	private TextView bandwithTitle;
	private TextView selectMemberShip;
	private TextView selectRecurring;
	private TextView paypalSubscrition;
	private TextView perMonthTitle;
	private TextView perYearTitle;
	private TextView comment;
	int parameterType;	
	MegaApiAndroid megaApi;
	Context context;
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		handleUrl=new ArrayList<Long>();

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
		
		switch (parameterType) {
			case 1:{
				aB.setTitle(getString(R.string.pro1_account));
				break;
			}
			case 2:{
				aB.setTitle(getString(R.string.pro2_account));
				break;
			}
			case 3:{
				aB.setTitle(getString(R.string.pro3_account));
				break;
			}
		}
		
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
		pricingFrom = (TextView) v.findViewById(R.id.pricing_from);
		
		perMonthTitle = (TextView) v.findViewById(R.id.per_month);
		perMonth = (TextView) v.findViewById(R.id.per_month_price);
		perYear = (TextView) v.findViewById(R.id.per_year_price);
		perYearTitle = (TextView) v.findViewById(R.id.per_year);
		
		accountType = AccountType.getById(parameterType);

		packageIcon.setImageResource(accountType.getImageResource());
		packageName.setText(accountType.getNameResource());
		packageName.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
		
		pricingFrom.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
		
		storageTitle = (TextView) v.findViewById(R.id.pro_storage_title);
		storageTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
		
		storage = (TextView) v.findViewById(R.id.pro_storage);
		storage.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
		
		bandwithTitle = (TextView) v.findViewById(R.id.pro_bandwidth_title);
		bandwithTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
		
		bandwidth = (TextView) v.findViewById(R.id.pro_bandwidth);
		bandwidth.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
		
		selectMemberShip = (TextView) v.findViewById(R.id.select_membership);
		selectMemberShip.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
		
		selectRecurring = (TextView) v.findViewById(R.id.select_recurring);
		selectRecurring.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleH));
		
		paypalSubscrition = (TextView) v.findViewById(R.id.paypal_subscription);
		paypalSubscrition.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleH));
		
		perMonth.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
		perMonthTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
		
		perYear.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
		perYearTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleH));
		
		comment = (TextView) v.findViewById(R.id.comment);
		comment.setTextSize(TypedValue.COMPLEX_UNIT_SP, (12*scaleH));
		
		megaApi.getPricing(this);	
		
		return v;
	}	
	
	public void payYear() {
		log("yearly");
		
		switch(parameterType){
		
			case 1:{
				Long handle = handleUrl.get(1);
				megaApi.getPaymentUrl(handle,this);		
				break;
			}
			case 2:{
				Long handle = handleUrl.get(3);
				megaApi.getPaymentUrl(handle,this);			
				break;
			}
			case 3:{
				Long handle = handleUrl.get(5);
				megaApi.getPaymentUrl(handle,this);			
				break;
			}
			
		}
	}
	
	public void payMonth() {
		log("monthly");

		switch(parameterType){
		
			case 1:{
				Long handle = handleUrl.get(0);
				megaApi.getPaymentUrl(handle,this);		
				break;
			}
			case 2:{
				Long handle = handleUrl.get(2);
				megaApi.getPaymentUrl(handle,this);			
				break;
			}
			case 3:{
				Long handle = handleUrl.get(4);
				megaApi.getPaymentUrl(handle,this);	
				break;
			}
			
		}
	}
	
	public void setParameterType (int type){
		this.parameterType = type;
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

			switch(parameterType){

				case 1:{
	
					storage.setText(p.getGBStorage(parameterType)+"GB");		            
					bandwidth.setText(sizeTranslation(p.getGBTransfer(parameterType)*12,0));
					
					DecimalFormat df = new DecimalFormat("#.##");  
					double saving3 = p.getAmount(1)/12.00/100.00;
					String saving3String =df.format(saving3);
	
					pricingFrom.setText("from " + saving3String +" € per month");
	
					double perMonthF=p.getAmount(0)/100.00;
			        String perMonthString =df.format(perMonthF);
			        double perYearF=p.getAmount(1)/100.00;
					String perYearString =df.format(perYearF);
					
					perMonth.setText(perMonthString+" €");
					perYear.setText(perYearString+" €");
					
					for(int i=0; i<p.getNumProducts();i++){
						handleUrl.add(p.getHandle(i));
					}
					break;
				}
				case 2:{
					storage.setText(sizeTranslation(p.getGBStorage(parameterType),0));
		             
					bandwidth.setText(sizeTranslation(p.getGBTransfer(parameterType)*12,0));
			           
					DecimalFormat df = new DecimalFormat("#.##");  
					double saving3 = p.getAmount(3)/12.00/100.00;
			        String saving3String =df.format(saving3);
			            
			        pricingFrom.setText("from " + saving3String +" € per month");
	
			        double perMonthF=p.getAmount(2)/100.00;
			        String perMonthString =df.format(perMonthF);
			        double perYearF=p.getAmount(3)/100.00;
					String perYearString =df.format(perYearF);
					
					perMonth.setText(perMonthString+" €");
					perYear.setText(perYearString+" €");					

					for(int i=0; i<p.getNumProducts();i++){
						handleUrl.add(p.getHandle(i));
					}
					break;
				}
				case 3:{
					storage.setText(sizeTranslation(p.getGBStorage(5),0));
					
					bandwidth.setText(sizeTranslation(p.getGBTransfer(4)*12,0));
					
					DecimalFormat df = new DecimalFormat("#.##");  
					double saving3 = p.getAmount(5)/12.00/100.00;
			        String saving3String =df.format(saving3);
			            
			        pricingFrom.setText("from " + saving3String +" € per month");
			        
			        double perMonthF=p.getAmount(4)/100.00;
			        String perMonthString =df.format(perMonthF);
			        double perYearF=p.getAmount(5)/100.00;
					String perYearString =df.format(perYearF);
					
					perMonth.setText(perMonthString+" €");
					perYear.setText(perYearString+" €");					

					for(int i=0; i<p.getNumProducts();i++){
						handleUrl.add(p.getHandle(i));
					}
					break;
				}

			}
		}

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
	
	public int onBackPressed(){
		((ManagerActivity)context).showUpAF();
		return 3;
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
