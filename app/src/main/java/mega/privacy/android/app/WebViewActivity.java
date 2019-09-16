package mega.privacy.android.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import mega.privacy.android.app.utils.LogUtil;

public class WebViewActivity extends Activity {

    WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtil.logDebug("onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fortumo_payment);
        myWebView = findViewById(R.id.webview);

        Intent intent = getIntent();
        if (intent != null) {
            String url = intent.getDataString();
            LogUtil.logDebug("URL: " + url);
            myWebView.getSettings().setJavaScriptEnabled(true);
            myWebView.loadUrl(url);
        }
    }
}
