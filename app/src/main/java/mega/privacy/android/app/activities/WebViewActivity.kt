package mega.privacy.android.app.activities

import android.Manifest.permission.CAMERA
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View.GONE
import android.view.View.LAYER_TYPE_HARDWARE
import android.view.View.VISIBLE
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ActivityWebViewBinding
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER
import mega.privacy.android.app.utils.Constants.EMAIL_VERIFY_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.PERMISSIONS_TYPE
import mega.privacy.android.app.utils.Constants.REQUEST_CAMERA
import mega.privacy.android.app.utils.Constants.REQUEST_RECORD_AUDIO
import mega.privacy.android.app.utils.Constants.REQUEST_WRITE_STORAGE
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.FileUtil.copyFileToDCIM
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.isURLSanitized
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

/**
 * WebViewActivity
 */
class WebViewActivity : BaseActivity() {
    companion object {
        private const val IMAGE_CONTENT_TYPE = 0
        private const val VIDEO_CONTENT_TYPE = 1
        private const val FILE = "file:"
        private const val REQUEST_ALL = 7
        private const val REQUEST_WRITE_AND_RECORD_AUDIO = 8
        private const val REQUEST_CAMERA_AND_RECORD_AUDIO = 9
    }

    private lateinit var binding: ActivityWebViewBinding

    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mFileChooserParams: WebChromeClient.FileChooserParams? = null
    private var pickedImage: String? = null
    private var pickedVideo: String? = null


    private val chooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.data == null) {
                mFilePathCallback?.onReceiveValue(null)
            } else if (mFilePathCallback != null) {
                manageResult(result.data)
            }

            pickedImage = null
            pickedVideo = null
        }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val url = intent.dataString
        if (url != null && url.isURLSanitized()) {
            binding.webView.loadUrl(url)
        } else {
            Timber.e("WebViewActivity::onNewIntent Vulnerable/Malicious Url detected: $url")
            finish()
        }
    }

    /**
     * onCreate
     */
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeAndConsumeInsets()
        super.onCreate(savedInstanceState)

        if (intent == null || intent.dataString == null) {
            Timber.e("Unable to open web. Intent is null")
            finish()
        }

        val url = intent.dataString

        if (!url.isURLSanitized()) {
            Timber.e("WebViewActivity::onCreate Vulnerable/Malicious Url detected: $url")
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
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    if (!url.isURLSanitized()) {
                        Timber.e(
                            "WebViewActivity::shouldOverrideUrlLoading Vulnerable/Malicious Url detected: $url"
                        )
                        finish()
                        return false
                    }

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
                    fileChooserParams: FileChooserParams,
                ): Boolean {
                    mFilePathCallback = filePathCallback
                    mFileChooserParams = fileChooserParams

                    return if (hasAllPermissions()) launchChooserIntent() else false
                }
            }

            if (Util.matchRegexs(url, EMAIL_VERIFY_LINK_REGEXS)) {
                MegaApplication.setIsWebOpenDueToEmailVerification(true)
            }

            if (url != null) {
                loadUrl(url)
            }
            binding.webProgressView.visibility = VISIBLE
            binding.webView.isEnabled = false
        }

        WebView.setWebContentsDebuggingEnabled(true)

    }

    /**
     * onDestroy
     */
    override fun onDestroy() {
        binding.webView.destroy()
        MegaApplication.setIsWebOpenDueToEmailVerification(false)
        super.onDestroy()
    }

    /**
     * onRequestPermissionsResult
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty()) {
            return
        }


        for (result in grantResults.indices) {
            if (grantResults[result] == PackageManager.PERMISSION_DENIED) {
                val warningText =
                    getString(R.string.files_required_permissions_warning)

                if (!PermissionUtils.shouldShowRequestPermissionRationale(
                        this,
                        getRequestedPermission(requestCode)
                    )
                ) {
                    showSnackbar(PERMISSIONS_TYPE, binding.root, warningText)
                } else {
                    showSnackbar(binding.root, warningText)
                }
                break
            }
        }
    }

    /**
     * Checks if the app has all the required permissions to capture and share files.
     *
     * @return True if the app has all the required permissions, false otherwise.
     */
    private fun hasAllPermissions(): Boolean {
        val writePermission = hasPermissions(this, WRITE_EXTERNAL_STORAGE)
        val cameraPermission = hasPermissions(this, CAMERA)
        val recordAudioPermission = hasPermissions(this, RECORD_AUDIO)

        if (writePermission && cameraPermission && recordAudioPermission) {
            return true
        }

        if (!writePermission && !cameraPermission && !recordAudioPermission) {
            requestPermission(this, REQUEST_ALL, WRITE_EXTERNAL_STORAGE, CAMERA, RECORD_AUDIO)
        } else if (!writePermission && !recordAudioPermission) {
            requestPermission(
                this,
                REQUEST_WRITE_AND_RECORD_AUDIO,
                WRITE_EXTERNAL_STORAGE,
                RECORD_AUDIO
            )
        } else if (!writePermission) {
            requestPermission(this, REQUEST_WRITE_STORAGE, WRITE_EXTERNAL_STORAGE)
        } else if (!cameraPermission && !recordAudioPermission) {
            requestPermission(this, REQUEST_CAMERA_AND_RECORD_AUDIO, CAMERA, RECORD_AUDIO)
        } else if (!cameraPermission) {
            requestPermission(this, REQUEST_CAMERA, CAMERA)
        } else {
            requestPermission(this, REQUEST_RECORD_AUDIO, RECORD_AUDIO)
        }

        return false
    }

    /**
     * Gets the denied permission depending on the requestCode requested.
     *
     * @param requestCode The code that identifies the requested permissions.
     * @return The denied permission.
     */
    private fun getRequestedPermission(requestCode: Int): String {
        return when (requestCode) {
            REQUEST_ALL -> {
                if (!hasPermissions(this, WRITE_EXTERNAL_STORAGE)) {
                    WRITE_EXTERNAL_STORAGE
                } else if (!hasPermissions(this, CAMERA)) {
                    CAMERA
                } else {
                    RECORD_AUDIO
                }
            }

            REQUEST_WRITE_AND_RECORD_AUDIO -> {
                if (!hasPermissions(this, WRITE_EXTERNAL_STORAGE)) {
                    WRITE_EXTERNAL_STORAGE
                } else {
                    RECORD_AUDIO
                }
            }

            REQUEST_WRITE_STORAGE -> {
                WRITE_EXTERNAL_STORAGE
            }

            REQUEST_CAMERA_AND_RECORD_AUDIO -> {
                if (!hasPermissions(this, CAMERA)) {
                    CAMERA
                } else {
                    RECORD_AUDIO
                }
            }

            REQUEST_CAMERA -> {
                CAMERA
            }

            else -> {
                RECORD_AUDIO
            }
        }
    }

    /**
     * Gets the available intents to capture and/or share content and launches the chooser intent.
     *
     * @return True if the chooser was launched successfully, false otherwise.
     */
    private fun launchChooserIntent(): Boolean {
        val takePictureIntent: Intent? = getContentIntent(IMAGE_CONTENT_TYPE)
        val takeVideoIntent: Intent? = getContentIntent(VIDEO_CONTENT_TYPE)
        val takeAudioIntent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)

        val chooserArray = if (takePictureIntent != null && takeVideoIntent != null) {
            arrayOf(takePictureIntent, takeVideoIntent, takeAudioIntent)
        } else if (takePictureIntent != null) {
            arrayOf(takePictureIntent, takeAudioIntent)
        } else if (takeVideoIntent != null) {
            arrayOf(takeVideoIntent, takeAudioIntent)
        } else {
            arrayOf(takeAudioIntent)
        }

        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
        contentSelectionIntent.type = FileUtil.ANY_TYPE_FILE

        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, chooserArray)

        chooserLauncher.launch(chooserIntent)

        return true
    }

    /**
     * Manages the result after capture or pick a file.
     *
     * @param data Intent containing the result of the action.
     */
    private fun manageResult(data: Intent?) {
        val clipData: ClipData?
        val stringData: String?

        if (data == null) {
            clipData = null
            stringData = null
        } else {
            clipData = data.clipData
            stringData = data.dataString
        }

        val results: Array<Uri?>

        if (clipData == null && stringData == null && (pickedImage != null || pickedVideo != null)) {
            val image = if (pickedImage != null) File(pickedImage!!.removePrefix(FILE)) else null
            val video = if (pickedVideo != null) File(pickedVideo!!.removePrefix(FILE)) else null

            val picked = if (isFileAvailable(image)) {
                results = arrayOf(Uri.parse(pickedImage))
                image
            } else {
                results = arrayOf(Uri.parse(pickedVideo))
                video
            }

            copyFileToDCIM(picked)
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

    /**
     * Gets a capture image or video Intent to add to the chooser Intent.
     *
     * @param contentType IMAGE_CONTENT_TYPE if requires a capture image Intent.
     *                    VIDEO_CONTENT_TYPE if requires a capture video Intent.
     * @return The Intent if the device has camera available or null otherwise.
     */
    private fun getContentIntent(contentType: Int): Intent? {
        var contentIntent: Intent? = createContentIntent(contentType)

        if (contentIntent?.resolveActivity(packageManager) != null) {
            var file: File? = null
            try {
                @SuppressLint("SimpleDateFormat")
                val fileName = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

                file = CacheFolderManager.buildTempFile(
                    fileName + getContentExtension(contentType)
                )
            } catch (e: IOException) {
                Timber.e(e, "Error creating temp file.")
            }

            if (file != null) {
                initPickedContent(file, contentType)

                contentIntent.apply {
                    putExtra(
                        MediaStore.EXTRA_OUTPUT,
                        FileProvider.getUriForFile(
                            this@WebViewActivity,
                            AUTHORITY_STRING_FILE_PROVIDER,
                            file
                        )
                    )

                    flags = FLAG_GRANT_WRITE_URI_PERMISSION and FLAG_GRANT_READ_URI_PERMISSION
                }
            } else {
                contentIntent = null
            }
        }

        return contentIntent
    }

    /**
     * Creates the initial capture image or video Intent without data and extras
     * to add to the chooser Intent.
     *
     * @param contentType IMAGE_CONTENT_TYPE if requires a capture image Intent.
     *                    VIDEO_CONTENT_TYPE if requires a capture video Intent.
     * @return The initial capture image or video Intent.
     */
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

    /**
     * Gets the extension of the file to capture.
     *
     * @param contentType IMAGE_CONTENT_TYPE if requires an image extension.
     *                    VIDEO_CONTENT_TYPE if requires a video extension.
     * @return The extension of the file to capture.
     */
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

    /**
     * Initializes the variable pickedImage or pickedVideo with the path of file to captured.
     *
     * @param file        The file to store the captured image or video.
     * @param contentType IMAGE_CONTENT_TYPE if referred to an image file.
     *                    VIDEO_CONTENT_TYPE if referred to a video file.
     */
    private fun initPickedContent(file: File, contentType: Int) {
        if (contentType == IMAGE_CONTENT_TYPE) {
            pickedImage = FILE + file.absolutePath
        } else if (contentType == VIDEO_CONTENT_TYPE) {
            pickedVideo = FILE + file.absolutePath
        }
    }
}