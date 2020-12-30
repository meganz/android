package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.matchRegexs;

public class WebViewActivityLollipop extends AppCompatActivity {

    private ProgressDialog progressDialog;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fortumo_payment);

        WebView myWebView = findViewById(R.id.webview);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setLoadWithOverviewMode(true);
        myWebView.getSettings().setUseWideViewPort(true);
        myWebView.getSettings().setDomStorageEnabled(true);
        myWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, final String url) {
                progressDialog.dismiss();
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            String url = intent.getDataString();
            logDebug("URL: " + url);
            if (matchRegexs(url, EMAIL_VERIFY_LINK_REGEXS)) {
                MegaApplication.setIsWebOpenDueToEmailVerification(true);
            }
            myWebView.loadUrl(url);
            progressDialog = ProgressDialog.show(this, this.getString(R.string.embed_web_browser_loading_title), this.getString(R.string.embed_web_browser_loading_message), true);
            progressDialog.setCancelable(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MegaApplication.setIsWebOpenDueToEmailVerification(false);
    }
}
