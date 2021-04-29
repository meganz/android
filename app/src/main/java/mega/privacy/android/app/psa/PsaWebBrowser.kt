package mega.privacy.android.app.psa

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.databinding.FragmentPsaWebBrowserBinding
import mega.privacy.android.app.psa.PsaManager.dismissPsa
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util

class PsaWebBrowser : Fragment() {
    private lateinit var binding: FragmentPsaWebBrowserBinding

//    private var psaId = Constants.INVALID_VALUE
    private var psaId = 0  // TODO: REMOVE this line

    private val uiHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPsaWebBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled", "HardwareIds")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val url = arguments?.getString(ARGS_URL_KEY) ?: return
//        psaId = arguments?.getInt(ARGS_ID_KEY) ?: return

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

//        loadUrl(url)
    }

    // TODO: Rename to loadPsa() and pass in the valid psa Id as well
    fun loadUrl(url: String) {
//        binding.webView.visibility = View.INVISIBLE

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
            Log.i("Alex", "e:$e")
        }
    }

//    override fun onDestroy() {
//        if (activity is BaseActivity) {
//            (activity as BaseActivity).onPsaWebViewDestroyed(binding.webView.visibility == View.VISIBLE)
//        }
//        super.onDestroy()
//    }

//    fun visible() = isResumed && binding.webView.visibility == View.VISIBLE

    /**
     * JS interface to show the PSA.
     */
    @JavascriptInterface
    fun showPSA() {
        Log.i("Alex", "show PSA 1")
//        if (isResumed) {
//        if (!isResumed) return // Alex add
        // Due to the possible delay introduced by JS showPSA,
        // If the activity is no longer the activity sit on the top at the moment
        // then don't show psa on it. Show psa even if the app(activity task) is already on the background.
        if (!Util.isTopActivity(activity?.javaClass, requireContext())) return

        uiHandler.post {
            Log.i("Alex", "show PSA 2")
            binding.webView.visibility = View.VISIBLE
                if (psaId != Constants.INVALID_VALUE) {
                    dismissPsa(psaId)
                }
        }
//        }
    }

    /**
     * JS interface to close the PSA.
     *
     * We need close this fragment here, so that when we get a new PSA, we can display it.
     */
    @JavascriptInterface
    fun hidePSA() {
        Log.i("Alex", "hide PSA 1")
        uiHandler.post {
            val currentActivity = activity
            if (currentActivity is BaseActivity && binding.webView.visibility == View.VISIBLE) {
//                currentActivity.closeDisplayingPsa()
                binding.webView.visibility = View.INVISIBLE
            }
        }
    }

    fun consumeBack(): Boolean {
        if (binding.webView.visibility == View.VISIBLE) {
            hidePSA()
            return true
        }

        return false
    }


    companion object {
        const val ARGS_URL_KEY = "URL"
        const val ARGS_ID_KEY = "ID"
        const val JS_INTERFACE = "megaAndroid"
    }
}
