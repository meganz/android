package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import mega.privacy.android.app.R;

import static mega.privacy.android.app.utils.Constants.MEGA_BLOG_LINK_REGEXS;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.Util.matchRegexs;


public class WebViewActivityLollipop extends Activity {

    private WebView myWebView;
    private ProgressDialog progressDialog;
    private Activity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fortumo_payment);

        activity = this;

        myWebView = findViewById(R.id.webview);


        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setLoadWithOverviewMode(true);
        myWebView.getSettings().setUseWideViewPort(true);
        myWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, final String url) {
                // Blog pages currently not support mobile website, which would trigger redirecting to mega.nz
                // We could remove this condition after blog page support mobile web page
                if (!matchRegexs(url, MEGA_BLOG_LINK_REGEXS)) {
                    progressDialog.dismiss();
                }
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            String url = intent.getDataString();
            logDebug("URL: " + url);
            myWebView.loadUrl(url);
            progressDialog = ProgressDialog.show(activity, this.getString(R.string.embed_web_browser_loading_title), this.getString(R.string.embed_web_browser_loading_message), true);
            progressDialog.setCancelable(false);
        }
    }
}
