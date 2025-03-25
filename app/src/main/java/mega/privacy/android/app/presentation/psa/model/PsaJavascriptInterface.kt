package mega.privacy.android.app.presentation.psa.model

import android.webkit.JavascriptInterface

/**
 * Psa javascript interface
 *
 * @property onShowPsa
 * @property onHidePsa
 */
class PsaJavascriptInterface(
    private val onShowPsa: () -> Unit,
    private val onHidePsa: () -> Unit,
) {

    /**
     * Show psa
     *
     */
    @JavascriptInterface
    fun showPsa() {
        onShowPsa()
    }

    /**
     * Hide psa
     *
     */
    @JavascriptInterface
    fun hidePsa() {
        onHidePsa()
    }

    companion object {
        /**
         * Javascript interface name
         */
        const val INTERFACE_NAME = "megaAndroid"
    }
}