package mega.privacy.android.app.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View.*
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
import android.webkit.WebView
import android.webkit.WebViewClient
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.databinding.ActivityWebViewBinding
import mega.privacy.android.app.utils.Constants.EMAIL_VERIFY_LINK_REGEXS
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.Util
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class WebViewActivity : Activity() {

    companion object {
        private const val FILE_CHOOSER_RESULT_CODE = 1000
        private const val IMAGE_CONTENT_TYPE = 0
        private const val VIDEO_CONTENT_TYPE = 1
        private const val FILE = "file:"
    }

    private lateinit var binding: ActivityWebViewBinding

    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var pickedImage: String? = null
    private var pickedVideo: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent == null || intent.dataString == null) {
            logError("Unable to open web. Intent is null")
            finish()
        }

        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.webView.settings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            domStorageEnabled = true
            mixedContentMode = MIXED_CONTENT_NEVER_ALLOW
            allowContentAccess = true
            allowFileAccess = true
        }

        binding.webView.apply {
            setLayerType(LAYER_TYPE_HARDWARE, null)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    view.loadUrl(url)
                    return true
                }

                override fun onPageFinished(view: WebView, url: String) {
                    binding.webProgressView.visibility = GONE
                    binding.webView.isEnabled = true
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams
                ): Boolean {
                    mFilePathCallback = filePathCallback
                    val takePictureIntent: Intent? = getContentIntent(IMAGE_CONTENT_TYPE)
                    val takeVideoIntent: Intent? = getContentIntent(VIDEO_CONTENT_TYPE)
                    val takeAudioIntent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)

                    val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                    contentSelectionIntent.type = FileUtil.ANY_TYPE_FILE

                    val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                    chooserIntent.putExtra(
                        Intent.EXTRA_INITIAL_INTENTS,
                        arrayOf(takePictureIntent, takeVideoIntent, takeAudioIntent)
                    )

                    try {
                        startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE)
                    } catch (e: ActivityNotFoundException) {
                        logError("Error opening file chooser.", e)
                        return false
                    }

                    return true
                }
            }

            val url = intent.dataString

            if (Util.matchRegexs(url, EMAIL_VERIFY_LINK_REGEXS)) {
                MegaApplication.setIsWebOpenDueToEmailVerification(true)
            }

            loadUrl(url)
            binding.webProgressView.visibility = VISIBLE
            binding.webView.isEnabled = false
        }

        WebView.setWebContentsDebuggingEnabled(true)

    }

    override fun onDestroy() {
        super.onDestroy()
        MegaApplication.setIsWebOpenDueToEmailVerification(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data == null || requestCode != FILE_CHOOSER_RESULT_CODE) {
            return
        }

        if (resultCode == RESULT_CANCELED) {
            mFilePathCallback?.onReceiveValue(null)
        } else if (resultCode == RESULT_OK && mFilePathCallback != null) {
            manageResult(data)
        }

        pickedImage = null
        pickedVideo = null
    }

    private fun manageResult(data: Intent) {
        var clipData: ClipData?
        var stringData: String?

        try {
            clipData = data.clipData
            stringData = data.dataString
        } catch (e: Exception) {
            logWarning("Error getting intent data.", e)
            clipData = null
            stringData = null
        }

        val results: Array<Uri?>

        if (clipData == null && stringData == null && (pickedImage != null || pickedVideo != null)) {
            results = arrayOf(Uri.parse(pickedImage ?: pickedVideo))
        } else if (null != clipData) {
            results = arrayOfNulls(clipData.itemCount)

            for (i in 0 until clipData.itemCount) {
                results[i] = clipData.getItemAt(i).uri
            }
        } else {
            results = arrayOf(Uri.parse(stringData))
        }

        mFilePathCallback?.onReceiveValue(results.requireNoNulls())
        mFilePathCallback = null
    }

    private fun getContentIntent(contentType: Int): Intent? {
        var contentIntent: Intent? = createContentIntent(contentType)

        if (contentIntent?.resolveActivity(this@WebViewActivity.packageManager) != null) {
            var file: File? = null
            try {
                @SuppressLint("SimpleDateFormat")
                val fileName = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

                file = File.createTempFile(
                    fileName, getContentExtension(contentType),
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                )
            } catch (e: IOException) {
                logError("Error creating temp file.", e)
            }

            if (file != null) {
                initPickedContent(file, contentType)
                contentIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file))
            } else {
                contentIntent = null
            }
        }

        return contentIntent
    }

    private fun createContentIntent(contentType: Int): Intent? {
        return when (contentType) {
            IMAGE_CONTENT_TYPE -> {
                Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            }
            VIDEO_CONTENT_TYPE -> {
                Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            }
            else -> {
                null
            }
        }
    }

    private fun getContentExtension(contentType: Int): String? {
        return when (contentType) {
            IMAGE_CONTENT_TYPE -> {
                FileUtil.JPG_EXTENSION
            }
            VIDEO_CONTENT_TYPE -> {
                FileUtil._3GP_EXTENSION
            }
            else -> {
                null
            }
        }
    }

    private fun initPickedContent(file: File, contentType: Int) {
        if (contentType == IMAGE_CONTENT_TYPE) {
            pickedImage = FILE + file.absolutePath
        } else if (contentType == VIDEO_CONTENT_TYPE) {
            pickedVideo = FILE + file.absolutePath
        }
    }
}