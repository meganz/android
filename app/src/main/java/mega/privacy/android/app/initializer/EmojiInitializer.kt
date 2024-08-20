package mega.privacy.android.app.initializer

import android.content.Context
import androidx.core.provider.FontRequest
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.FontRequestEmojiCompatConfig
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiManager
import mega.privacy.android.app.components.twemoji.EmojiManagerShortcodes
import mega.privacy.android.app.components.twemoji.TwitterEmojiProvider
import mega.privacy.android.domain.qualifier.ApplicationScope
import timber.log.Timber

/**
 * Emoji initializer
 *
 */
class EmojiInitializer : Initializer<Unit> {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface EmojiInitializerEntryPoint {
        /**
         * App scope
         *
         */
        @ApplicationScope
        fun appScope(): CoroutineScope
    }

    /**
     * Create
     *
     */
    override fun create(context: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            EmojiInitializerEntryPoint::class.java
        )
        entryPoint.appScope().launch {
            EmojiManagerShortcodes.initEmojiData(context)

            Timber.d("Use downloadable font for EmojiCompat")

            val fontRequest = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs
            )

            val config = FontRequestEmojiCompatConfig(context, fontRequest)
                .setReplaceAll(false)
                .setMetadataLoadStrategy(EmojiCompat.LOAD_STRATEGY_MANUAL)
                .registerInitCallback(object : EmojiCompat.InitCallback() {
                    override fun onInitialized() {
                        Timber.d("EmojiCompat initialized")
                    }

                    override fun onFailed(throwable: Throwable?) {
                        Timber.w("EmojiCompat initialization failed")
                    }
                })

            EmojiCompat.init(config)
            EmojiManager.install(TwitterEmojiProvider())
        }
    }

    /**
     * Dependencies
     *
     */
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}