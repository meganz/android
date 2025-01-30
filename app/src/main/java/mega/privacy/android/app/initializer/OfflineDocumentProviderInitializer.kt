package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.initializer.OfflineDocumentProviderInitializer.DocumentProviderEntryPoint
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.offline.GetOfflineDocumentProviderRootFolderUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.MonitorLogoutUseCase

/**
 * OfflineDocumentProvider initializer
 *
 */
class OfflineDocumentProviderInitializer : Initializer<Unit> {
    /**
     * OfflineDocumentProvider initializer entry point
     *
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DocumentProviderEntryPoint {
        /**
         * Provides [GetOfflineDocumentProviderRootFolderUseCase] to create Document provider Root Folder for the User.
         */
        fun provideGetDocumentProviderRootFolderUseCase(): GetOfflineDocumentProviderRootFolderUseCase

        /**
         * Provides [MonitorLogoutUseCase] to monitor logout.
         */
        fun provideMonitorLogoutUseCase(): MonitorLogoutUseCase

        /**
         * Provides [GetAccountCredentialsUseCase] to get logged in user's credentials
         */
        fun provideGetAccountCredentialsUseCase(): GetAccountCredentialsUseCase

        /**
         * Provides [CoroutineScope] for application scope.
         */
        @ApplicationScope
        fun applicationScope(): CoroutineScope
    }

    /**
     * Create
     *
     */
    override fun create(context: Context) {
        DependencyContainer.init(context.applicationContext)
    }


    /**
     * Dependencies
     *
     */
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(WorkManagerInitializer::class.java)
}

/**
 * Dependency container for OfflineDocumentProvider
 */
object DependencyContainer {
    /**
     * Application scope
     */
    lateinit var applicationScope: CoroutineScope

    /**
     * Get document provider root folder use case
     */
    lateinit var getOfflineDocumentProviderRootFolderUseCase: GetOfflineDocumentProviderRootFolderUseCase

    /**
     * Monitor logout use case
     */
    lateinit var monitorLogoutUseCase: MonitorLogoutUseCase

    /**
     * Get Account Credentials use case
     */
    lateinit var getAccountCredentialsUseCase: GetAccountCredentialsUseCase

    /**
     * Init
     */
    fun init(context: Context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(context, DocumentProviderEntryPoint::class.java)
        applicationScope = entryPoint.applicationScope()
        getOfflineDocumentProviderRootFolderUseCase =
            entryPoint.provideGetDocumentProviderRootFolderUseCase()
        monitorLogoutUseCase = entryPoint.provideMonitorLogoutUseCase()
        getAccountCredentialsUseCase = entryPoint.provideGetAccountCredentialsUseCase()
    }
}
