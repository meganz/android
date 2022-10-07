package mega.privacy.android.app.initializer

import android.content.Context
import androidx.core.provider.FontRequest
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import androidx.startup.Initializer
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiManager
import mega.privacy.android.app.components.twemoji.EmojiManagerShortcodes
import mega.privacy.android.app.components.twemoji.TwitterEmojiProvider
import timber.log.Timber

/**
 * Emoji initializer
 *
 */
class EmojiInitializer : Initializer<Unit> {
    /**
     * Create
     *
     */
    override fun create(context: Context) {
        EmojiManagerShortcodes.initEmojiData(context)
        EmojiManager.install(TwitterEmojiProvider())

        Timber.d("Use downloadable font for EmojiCompat")

        val fontRequest = FontRequest(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            "Noto Color Emoji Compat",
            R.array.com_google_android_gms_fonts_certs)

        val config = FontRequestEmojiCompatConfig(context, fontRequest)
            .setReplaceAll(false)
            .registerInitCallback(object : EmojiCompat.InitCallback() {
                override fun onInitialized() {
                    Timber.d("EmojiCompat initialized")
                }

                override fun onFailed(throwable: Throwable?) {
                    Timber.w("EmojiCompat initialization failed")
                }
            })

        EmojiCompat.init(config)
    }

    /**
     * Dependencies
     *
     */
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}