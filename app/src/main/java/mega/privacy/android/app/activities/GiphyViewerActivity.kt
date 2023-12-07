package mega.privacy.android.app.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.GiphyPickerActivity.Companion.GIF_DATA
import mega.privacy.android.app.databinding.ActivityGiphyViewerBinding
import mega.privacy.android.app.objects.GifData
import mega.privacy.android.app.utils.Constants.ACTION_PREVIEW_GIPHY
import mega.privacy.android.app.utils.FrescoUtils.loadGif
import mega.privacy.android.app.utils.GiphyUtil.Companion.getOriginalGiphySrc
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.isScreenInPortrait

class GiphyViewerActivity : PasscodeActivity() {

    private lateinit var binding: ActivityGiphyViewerBinding

    private var gifData: GifData? = null

    private var picking = true

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            setResult(RESULT_CANCELED)
            retryConnectionsAndSignalPresence()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window?.statusBarColor = ContextCompat.getColor(this,R.color.black)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window?.setDecorFitsSystemWindows(true)
            val wic: WindowInsetsController? = window?.decorView?.windowInsetsController

            if (!Util.isDarkMode(applicationContext)) {
                wic?.setSystemBarsAppearance(
                    0,
                    APPEARANCE_LIGHT_STATUS_BARS
                )
            }

            wic?.setSystemBarsAppearance(
                APPEARANCE_LIGHT_NAVIGATION_BARS,
                APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window?.decorView?.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        binding = ActivityGiphyViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.action.equals(ACTION_PREVIEW_GIPHY)) {
            picking = false
            onBackPressedCallback.isEnabled = false
            binding.sendFab.visibility = View.GONE
        } else {
            binding.sendFab.setOnClickListener { sendGifToChat() }
        }

        gifData = with(intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableExtra(GIF_DATA, GifData::class.java)
            } else {
                @Suppress("DEPRECATION")
                getParcelableExtra(GIF_DATA)
            }
        }

        updateGifDimensionsView()

        loadGif(binding.gifImage, binding.gifProgressBar, false, null, getOriginalGiphySrc(gifData?.webpUrl))
    }

    /**
     * Updates the dimensions of the view where the GIF will be shown to show the right ones
     * depending on the available space on screen and orientation.
     */
    private fun updateGifDimensionsView() {
        val params = binding.gifImage.layoutParams ?: return
        val gifWidth = gifData?.width
        val gifHeight = gifData?.height
        val gifScreenWidth: Int
        val gifScreenHeight: Int

        if (isScreenInPortrait(this@GiphyViewerActivity)) {
            gifScreenWidth = outMetrics.widthPixels

            gifScreenHeight = if (gifWidth == gifHeight) {
                gifScreenWidth
            } else {
                val factor = gifScreenWidth.toFloat() / (gifWidth ?: 0).toFloat()
                ((gifHeight ?: 0) * factor).toInt()
            }

            if (gifScreenHeight > 0) {
                params.height = gifScreenHeight
            }
        } else {
            gifScreenHeight = outMetrics.heightPixels

            gifScreenWidth = if (gifWidth == gifHeight) {
                gifScreenHeight
            } else {
                val factor = gifScreenHeight.toFloat() / (gifHeight ?: 0).toFloat()
                ((gifWidth ?: 0) * factor).toInt()
            }

            if (gifScreenWidth > 0) {
                params.width = gifScreenWidth
            }
        }

        binding.gifImage.layoutParams = params
    }

    /**
     * Confirms the selected GIF has to be sent to the chat.
     */
    private fun sendGifToChat() {
        if (picking) {
            setResult(RESULT_OK, Intent().putExtra(GIF_DATA, gifData))
        }
        finish()
    }
}