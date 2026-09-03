package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.navigation.contract.initialisation.CriticalAsyncAppCreateInitialiser
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import javax.inject.Inject

/**
 * Configures the folder-links MEGA SDK instance, mirroring the core instance's configuration.
 *
 * Critical: must run after [SdkSetupInitialiser] so the account auth token and buffer size can
 * be copied from the already configured core instance.
 *
 * The SDK dependencies are lazy so that constructing this initialiser never constructs the SDK
 * instances, whose creation blocks until the native library has finished loading.
 */
internal class FolderApiSetupInitialiser @Inject constructor(
    @MegaApi private val megaApiLazy: dagger.Lazy<MegaApiAndroid>,
    @MegaApiFolder private val megaApiFolderLazy: dagger.Lazy<MegaApiAndroid>,
) : CriticalAsyncAppCreateInitialiser {
    override val name = "FolderApiSetupInitialiser"

    override suspend operator fun invoke() {
        val megaApi = megaApiLazy.get()
        megaApiFolderLazy.get().apply {
            if (isLoggedIn(megaApi)) {
                Timber.d("Logged in. Setting account auth token for folder links.")
                accountAuth = megaApi.accountAuth
            }
            retrySSLerrors(true)
            downloadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
            uploadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
            httpServerSetMaxBufferSize(megaApi.httpServerGetMaxBufferSize())
        }
    }

    private fun isLoggedIn(megaApi: MegaApiAndroid): Boolean = megaApi.isLoggedIn != 0
}
