package nz.mega.android.receivers;

import mp.MpUtils;
import nz.mega.android.CameraSyncService;
import nz.mega.android.utils.Util;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;


public class PaymentStatusReceiver extends BroadcastReceiver {
	
	public PaymentStatusReceiver() {}

	@Override
	public void onReceive(Context context, Intent intent){
//		Cursor cursor = context.getContentResolver().query(intent.getData(), null,null, null, null);
//	    cursor.moveToFirst();
//	    String image_path = cursor.getString(cursor.getColumnIndex("_data"));
//	    log("CameraEventReceiver_New Photo is Saved as : -" + image_path);
	    
		log("PaymentStatusReceiver");
		Bundle extras = intent.getExtras();   
	    log("- billing_status:  " + extras.getInt("billing_status"));
	    log("- credit_amount:   " + extras.getString("credit_amount"));
	    log("- credit_name:     " + extras.getString("credit_name"));
	    log("- message_id:      " + extras.getString("message_id") );
	    log("- payment_code:    " + extras.getString("payment_code"));
	    log("- price_amount:    " + extras.getString("price_amount"));
	    log("- price_currency:  " + extras.getString("price_currency"));
	    log("- product_name:    " + extras.getString("product_name"));
	    log("- service_id:      " + extras.getString("service_id"));
	    log("- user_id:         " + extras.getString("user_id"));
	 
	    int billingStatus = extras.getInt("billing_status");
	    if(billingStatus == MpUtils.MESSAGE_STATUS_BILLED) {
	      int coins = Integer.parseInt(intent.getStringExtra("credit_amount"));
//	      Wallet.addCoins(context, coins);
	    }
	}
	
	public static void log(String message) {
		Util.log("PaymentStatusReceiver", message);
	}
}
