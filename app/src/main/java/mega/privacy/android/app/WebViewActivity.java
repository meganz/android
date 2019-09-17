package mega.privacy.android.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import static mega.privacy.android.app.utils.LogUtil.logDebug;

public class WebViewActivity extends Activity {

    WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logDebug("onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fortumo_payment);
        myWebView = findViewById(R.id.webview);

        Intent intent = getIntent();
        if (intent != null) {
            String url = intent.getDataString();
            logDebug("URL: " + url);
            myWebView.getSettings().setJavaScriptEnabled(true);
            myWebView.loadUrl(url);
        }
    }
}
