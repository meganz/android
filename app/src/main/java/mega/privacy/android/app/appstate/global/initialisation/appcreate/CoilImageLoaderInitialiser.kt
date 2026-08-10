package mega.privacy.android.app.appstate.global.initialisation.appcreate

import coil3.SingletonImageLoader
import mega.privacy.android.app.fetcher.MegaImageLoaderFactory
import mega.privacy.android.navigation.contract.initialisation.SynchronousAppCreateInitialiser
import javax.inject.Inject

/**
 * Installs [MegaImageLoaderFactory] as the Coil singleton image loader factory.
 *
 * Synchronous: must complete during `Application.onCreate`, before any Activity can trigger an
 * image load — a load before this runs would freeze Coil's default loader, which lacks MEGA's
 * fetchers.
 */
internal class CoilImageLoaderInitialiser @Inject constructor(
    private val megaImageLoaderFactory: MegaImageLoaderFactory,
) : SynchronousAppCreateInitialiser {
    override val name = "CoilImageLoaderInitialiser"

    override operator fun invoke() {
        SingletonImageLoader.setSafe(megaImageLoaderFactory)
    }
}
