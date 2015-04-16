package nz.mega.android;

import mp.MpUtils;
import mp.PaymentRequest;
import mp.PaymentResponse;
import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaPricing;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class FortumoPayment extends ActionBarActivity implements MegaRequestListenerInterface {
		
	WebView myWebView;
	MegaApiAndroid megaApi;
	
	private static String SERVICE_ID = "f7aab2cfb5cceb8f1f318645e07706ea";
	private static String APP_SECRET = "af629e219dd635e09b3972b964b65606";
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {

		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}
		
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fortumo_payment);
        
        myWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        megaApi.getPricing(this);
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
		if (request.getType() == MegaRequest.TYPE_GET_PRICING){
			MegaPricing p = request.getPricing();
			for (int i=0;i<p.getNumProducts();i++){
				Product account = new Product (p.getHandle(i), p.getProLevel(i), p.getMonths(i), p.getGBStorage(i), p.getAmount(i), p.getGBTransfer(i));
				if (account.getLevel()==1&&account.getMonths()==1){
					long planHandle = account.handle;
					megaApi.getPaymentId(planHandle, this);
					log("megaApi.getPaymentId(" + planHandle + ")");
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_ID){
			log("PAYMENT ID: " + request.getLink());
			Toast.makeText(this, "PAYMENTID: " + request.getLink(), Toast.LENGTH_LONG).show();
//			PaymentRequest.PaymentRequestBuilder builder = new PaymentRequest.PaymentRequestBuilder();
//	        builder.setService(SERVICE_ID, APP_SECRET);
//	        builder.setDisplayString("TestingFromAndroidInAPP"); 
//	        builder.setProductName(request.getLink());  // non-consumable purchases are restored using this value
////	        builder.setType(MpUtils.PRODUCT_TYPE_NON_CONSUMABLE);              // non-consumable items can be later restored
//	        builder.setType(MpUtils.PRODUCT_TYPE_SUBSCRIPTION);
//	        builder.setIcon(R.drawable.ic_launcher);
//	        PaymentRequest pr = builder.build();
//	        makePayment(pr);
			

			String urlFortumo = "http://fortumo.com/mobile_payments/f250460ec5d97fd27e361afaa366db0f?cuid=" + request.getLink();
			myWebView.loadUrl(urlFortumo);

//			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlFortumo));
//			startActivity(browserIntent);
		}
	}
	
	// Fortumo related glue-code
 	private static final int REQUEST_CODE = 1234; // Can be anything
    
    protected final void makePayment(PaymentRequest payment) {
		startActivityForResult(payment.toIntent(this), REQUEST_CODE);
	}
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == REQUEST_CODE) {
			if(data == null) {
				return;
			}
			
			// OK
			if (resultCode == RESULT_OK) {
				PaymentResponse response = new PaymentResponse(data);
				
				switch (response.getBillingStatus()) {
					case MpUtils.MESSAGE_STATUS_BILLED:{
						Toast.makeText(this, "BILLED!", Toast.LENGTH_LONG).show();
						break;
					}
					case MpUtils.MESSAGE_STATUS_FAILED:{
							Toast.makeText(this, "FAILED!", Toast.LENGTH_LONG).show();
							break;
					}
					case MpUtils.MESSAGE_STATUS_PENDING:{
							Toast.makeText(this, "PENDING!", Toast.LENGTH_LONG).show();
							break;
					}
				}
			// Cancel
			} else {
				// ..
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
    	}
    }

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		// TODO Auto-generated method stub
		
	}
	
	public static void log(String message) {
		Util.log("FortumoPayment", message);
	}
}
