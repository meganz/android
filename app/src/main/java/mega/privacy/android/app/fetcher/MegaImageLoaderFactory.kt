package mega.privacy.android.app.fetcher

import android.os.Build
import android.os.Build.VERSION.SDK_INT
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import coil3.video.VideoFrameDecoder
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Builds the app-wide Coil [ImageLoader] with MEGA's fetchers, keyers and decoders.
 */
internal class MegaImageLoaderFactory @Inject constructor(
    private val thumbnailFactory: MegaThumbnailFetcher.Factory,
    private val avatarFactory: MegaAvatarFetcher.Factory,
    private val mediaThumbnailFactory: MediaThumbnailFetcher.Factory,
) : SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= Build.VERSION_CODES.P) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            OkHttpClient()
                        }
                    )
                )
                add(VideoFrameDecoder.Factory())
                add(SvgDecoder.Factory())
                add(thumbnailFactory)
                add(avatarFactory)
                add(mediaThumbnailFactory)
                add(MegaThumbnailKeyer)
                add(MegaAvatarKeyer)
                add(MediaThumbnailKeyer)
            }
            .build()
}
