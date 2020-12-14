package mega.privacy.android.app.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.ANY_TYPE_FILE;
import static mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION;
import static mega.privacy.android.app.utils.FileUtil._3GP_EXTENSION;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.matchRegexs;

public class WebViewActivity extends Activity {

    private static final int FILE_CHOOSER_RESULT_CODE = 1000;
    private static final int IMAGE_CONTENT_TYPE = 0;
    private static final int VIDEO_CONTENT_TYPE = 1;

    private ProgressDialog progressDialog;

    private ValueCallback<Uri[]> mFilePathCallback;
    private String picked_image;
    private String picked_video;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fortumo_payment);

        WebView myWebView = findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        myWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        //Not enabled by default targeting Build.VERSION_CODES.R and above
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);

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

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                mFilePathCallback = filePathCallback;

                Intent takePictureIntent = getContentIntent(IMAGE_CONTENT_TYPE);
                Intent takeVideoIntent = getContentIntent(VIDEO_CONTENT_TYPE);
                Intent takeAudioIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType(ANY_TYPE_FILE);

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePictureIntent, takeVideoIntent, takeAudioIntent});

                try {
                    startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE);
                } catch (ActivityNotFoundException e) {
                    logError("Error opening file chooser. ", e);
                    return false;
                }

                return true;
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

        WebView.setWebContentsDebuggingEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (resultCode == RESULT_CANCELED) {
                mFilePathCallback.onReceiveValue(null);
            } else if (resultCode == RESULT_OK) {
                if (mFilePathCallback == null) {
                    return;
                }

                ClipData clipData;
                String stringData;
                try {
                    clipData = data.getClipData();
                    stringData = data.getDataString();
                } catch (Exception e) {
                    logWarning("Error getting intent data.", e);
                    clipData = null;
                    stringData = null;
                }

                Uri[] results;

                if (clipData == null && stringData == null && (picked_image != null || picked_video != null)) {
                    results = new Uri[]{Uri.parse(picked_image != null ? picked_image : picked_video)};
                } else if (null != clipData) {
                    final int numSelectedFiles = clipData.getItemCount();
                    results = new Uri[numSelectedFiles];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        results[i] = clipData.getItemAt(i).getUri();
                    }
                } else {
                    results = new Uri[]{Uri.parse(stringData)};
                }

                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
            }

            picked_image = null;
            picked_video = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MegaApplication.setIsWebOpenDueToEmailVerification(false);
    }

    private Intent getContentIntent(int contentType) {
        Intent contentIntent = createContentIntent(contentType);

        if (contentIntent.resolveActivity(WebViewActivity.this.getPackageManager()) != null) {
            File file = null;
            try {
                @SuppressLint("SimpleDateFormat")
                String file_name = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                file = File.createTempFile(file_name, getContentExtension(contentType),
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            } catch (IOException e) {
                logError("Error creating temp file.", e);
            }

            if (file != null) {
                initPickedContent(file, contentType);
                contentIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            } else {
                contentIntent = null;
            }
        }

        return contentIntent;
    }

    private Intent createContentIntent(int contentType) {
        if (contentType == IMAGE_CONTENT_TYPE) {
            return new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        } else if (contentType == VIDEO_CONTENT_TYPE) {
            return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        }

        return null;
    }

    private String getContentExtension(int contentType) {
        if (contentType == IMAGE_CONTENT_TYPE) {
            return JPG_EXTENSION;
        } else if (contentType == VIDEO_CONTENT_TYPE) {
            return _3GP_EXTENSION;
        }

        return null;
    }

    private void initPickedContent(File file, int contentType) {
        if (contentType == IMAGE_CONTENT_TYPE) {
            picked_image = "file:" + file.getAbsolutePath();
        } else if (contentType == VIDEO_CONTENT_TYPE) {
            picked_video = "file:" + file.getAbsolutePath();
        }
    }
}
