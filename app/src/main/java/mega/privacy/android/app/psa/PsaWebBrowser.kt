package mega.privacy.android.app.psa

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.databinding.FragmentPsaWebBrowserBinding
import mega.privacy.android.app.psa.PsaManager.dismissPsa
import mega.privacy.android.app.utils.Constants

class PsaWebBrowser : Fragment() {
    private lateinit var binding: FragmentPsaWebBrowserBinding

    private var psaId = Constants.INVALID_VALUE

    private val uiHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPsaWebBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val megaApi = MegaApplication.getInstance().megaApi
        val myUserHandle = megaApi.myUserHandle ?: return

        val url = arguments?.getString(ARGS_URL_KEY) ?: return
        psaId = arguments?.getInt(ARGS_ID_KEY) ?: return

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.loadWithOverviewMode = true
        binding.webView.settings.useWideViewPort = true
        binding.webView.settings.domStorageEnabled = true

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return false
            }
        }

        binding.webView.addJavascriptInterface(this, JS_INTERFACE)

        // This is the same way SDK getting device id:
        // https://github.com/meganz/sdk/blob/develop/src/posix/fs.cpp#L1575
        // and we find out the in param of `PosixFileSystemAccess::statsid` is an empty string
        // through debugging.
        val androidId = Settings.Secure.getString(requireContext().contentResolver, "android_id")
        val finalUrl = "$url/$myUserHandle?$androidId"
        binding.webView.loadUrl(finalUrl)
    }

    override fun onDestroy() {
        val currentActivity = activity
        if (currentActivity is BaseActivity) {
            currentActivity.onPsaWebViewDestroyed(binding.webView.visibility == View.VISIBLE)
        }
        super.onDestroy()
    }

    fun visible() = isResumed && binding.webView.visibility == View.VISIBLE

    /**
     * JS interface to show the PSA.
     */
    @JavascriptInterface
    fun showPSA() {
        if (isResumed) {
            uiHandler.post {
                binding.webView.visibility = View.VISIBLE
                if (psaId != Constants.INVALID_VALUE) {
                    dismissPsa(psaId)
                }
            }
        }
    }

    /**
     * JS interface to close the PSA.
     *
     * We need close this fragment here, so that when we get a new PSA, we can display it.
     */
    @JavascriptInterface
    fun hidePSA() {
        uiHandler.post {
            val currentActivity = activity
            if (currentActivity is BaseActivity) {
                currentActivity.closeDisplayingPsa()
            }
        }
    }

    companion object {
        const val ARGS_URL_KEY = "URL"
        const val ARGS_ID_KEY = "ID"
        const val JS_INTERFACE = "megaAndroid"
    }
}
