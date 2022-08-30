package mega.privacy.android.app.psa

import android.annotation.SuppressLint
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
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.databinding.FragmentPsaWebBrowserBinding
import mega.privacy.android.app.psa.PsaManager.dismissPsa
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import timber.log.Timber

class PsaWebBrowser : Fragment() {
    private lateinit var binding: FragmentPsaWebBrowserBinding

    private var psaId = Constants.INVALID_VALUE

    private val uiHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPsaWebBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled", "HardwareIds")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.loadWithOverviewMode = true
        binding.webView.settings.useWideViewPort = true
        binding.webView.settings.domStorageEnabled = true

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (AlarmReceiver.wakeLock.isHeld) {
                    AlarmReceiver.wakeLock.release()
                }
            }
        }

        binding.webView.addJavascriptInterface(this, JS_INTERFACE)
    }

    fun loadPsa(url: String, psaId: Int) {
        binding.webView.visibility = View.INVISIBLE
        this.psaId = psaId

        try {
            val megaApi = MegaApplication.getInstance().megaApi
            val myUserHandle = megaApi.myUserHandle ?: return

            // This is the same way SDK getting device id:
            // https://github.com/meganz/sdk/blob/develop/src/posix/fs.cpp#L1575
            // and we find out the in param of `PosixFileSystemAccess::statsid` is an empty string
            // through debugging.
            val androidId =
                Settings.Secure.getString(requireContext().contentResolver, "android_id")
            val finalUrl = "$url/$myUserHandle?$androidId"
            binding.webView.loadUrl(finalUrl)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /**
     * JS interface to show the PSA.
     */
    @JavascriptInterface
    fun showPSA() {
        // Due to the possible delay introduced by JS showPSA,
        // If the activity is no longer the activity sit on the top at the moment
        // then don't show psa on it. Show psa even if the app(activity task) is already on the background.
        if (!Util.isTopActivity(activity?.javaClass?.name, requireContext())) return
        requireActivity().onBackPressedDispatcher.addCallback(
            this, onBackPressedCallback
        )
        uiHandler.post {
            binding.webView.visibility = View.VISIBLE
            onBackPressedCallback.isEnabled = true
            if (psaId != Constants.INVALID_VALUE) {
                dismissPsa(psaId)
            }
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            hidePSA()
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
            if (currentActivity is BaseActivity && binding.webView.visibility == View.VISIBLE) {
                binding.webView.visibility = View.INVISIBLE
                onBackPressedCallback.isEnabled = false
                onBackPressedCallback.remove()
            }
        }
    }

    @Deprecated("All activities and fragments should handle their own " +
            "onBackPressedDispatcher callbacks independent from any other fragments or activities.")
    fun consumeBack() = onBackPressedCallback.isEnabled

    companion object {
        const val ARGS_URL_KEY = "URL"
        const val ARGS_ID_KEY = "ID"
        const val JS_INTERFACE = "megaAndroid"
    }
}
