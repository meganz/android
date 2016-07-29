package mega.privacy.android.app;

import android.app.Activity;
import android.os.Bundle;

import mega.privacy.android.app.utils.Util;


public class ConfirmEmailActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_confirm_email);
	}

	public static void log(String log) {
		Util.log("ConfirmEmailActivity", log);
	}

}
