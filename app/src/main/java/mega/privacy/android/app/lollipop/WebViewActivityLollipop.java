package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;


public class WebViewActivityLollipop extends Activity{

	WebView myWebView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		super.onCreate(savedInstanceState);

	   	setContentView(R.layout.activity_fortumo_payment);
		myWebView = (WebView) findViewById(R.id.webview);

		Intent intent = getIntent();
		if (intent != null) {
			String url = intent.getDataString();
			log("URL: " + url);
			myWebView.getSettings().setJavaScriptEnabled(true);
			myWebView.loadUrl(url);
		}
	}

	public static void log(String message) {
		Util.log("WebViewActivityLollipop", message);
	}
}
