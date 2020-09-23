package mega.privacy.android.app.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ProgressBar
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.GiphyActivity.Companion.GIF_DATA
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.objects.GifData
import mega.privacy.android.app.utils.FrescoUtils.loadGif
import mega.privacy.android.app.utils.Util.isScreenInPortrait

class GifViewerActivity: PinActivityLollipop() {

    private var gifView: SimpleDraweeView? = null
    private var pB: ProgressBar? = null
    private var sendFab: FloatingActionButton? = null

    private var gifData: GifData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_giphy_viewer)
        window.statusBarColor = resources.getColor(R.color.black)

        gifView = findViewById(R.id.gif_image)
        pB = findViewById(R.id.gif_progress_bar)
        sendFab = findViewById(R.id.send_fab)
        sendFab?.setOnClickListener { sendGifToChat() }

        gifData = intent.getParcelableExtra(GIF_DATA)
        updateGifDimensionsView()

        loadGif(gifView, pB, null, Uri.parse(gifData?.webpUrl))
    }

    /**
     * Updates the dimensions of the view where the GIF will be shown to show the right ones
     * depending on the available space on screen and orientation.
     */
    private fun updateGifDimensionsView() {
        val params = gifView?.layoutParams
        val gifWidth = gifData?.width
        val gifHeight = gifData?.height
        val gifScreenWidth: Int
        val gifScreenHeight: Int

        if (isScreenInPortrait(this@GifViewerActivity)) {
            gifScreenWidth = outMetrics.widthPixels

            gifScreenHeight = if (gifWidth == gifHeight) {
                gifScreenWidth
            } else {
                val factor = gifScreenWidth.toFloat() / (gifWidth ?: 0).toFloat()
                ((gifHeight ?: 0) * factor).toInt()
            }

            if (gifScreenHeight > 0) {
                params?.height = gifScreenHeight
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
                params?.width = gifScreenWidth
            }
        }

        gifView?.layoutParams = params
    }

    /**
     * Confirms the selected GIF has to be sent to the chat.
     */
    private fun sendGifToChat() {
        setResult(RESULT_OK, Intent().putExtra(GIF_DATA, gifData))
        finish()
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed()
    }
}