package mega.privacy.android.app.appstate.global.initialisation.appcreate

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.providers.documentprovider.CloudDriveDocumentDataProvider
import mega.privacy.android.navigation.contract.initialisation.AsyncAppCreateInitialiser
import javax.inject.Inject

/**
 * Starts [CloudDriveDocumentDataProvider]'s connectivity collection.
 *
 * Content providers are installed before the application (and, in instrumented tests, before the
 * dependency graph) exists, so the SAF document provider must stay passive at install time; its
 * connectivity monitoring is started here. Non-critical: [CloudDriveDocumentDataProvider]'s
 * connectivity state defaults to online, so queries served before this unit runs behave the same
 * as before the first network callback.
 */
internal class CloudDriveDocumentProviderInitialiser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudDriveDocumentDataProvider: CloudDriveDocumentDataProvider,
) : AsyncAppCreateInitialiser {
    override val name = "CloudDriveDocumentProviderInitialiser"

    override suspend operator fun invoke() {
        cloudDriveDocumentDataProvider.monitorConnectivity(context)
    }
}
