package mega.privacy.android.app.di

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.app.LegacyDatabaseMigrationImpl
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.activities.navigation.WebViewDeepLinkHandler
import mega.privacy.android.app.appstate.global.event.AppDialogsEventQueueImpl
import mega.privacy.android.app.appstate.global.event.AppDialogsEventQueueReceiver
import mega.privacy.android.app.appstate.global.event.NavigationEventQueueImpl
import mega.privacy.android.app.appstate.global.event.NavigationEventQueueReceiver
import mega.privacy.android.app.consent.ConsentDialogDestinations
import mega.privacy.android.app.nav.MegaActivityResultContractImpl
import mega.privacy.android.app.nav.MegaNavigatorImpl
import mega.privacy.android.app.presentation.container.MegaAppContainerProvider
import mega.privacy.android.app.presentation.filelink.FileLinkDeepLinkHandler
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountDeepLinkHandler
import mega.privacy.android.app.presentation.login.LoginDeepLinkHandler
import mega.privacy.android.app.presentation.login.logoutdialog.RemoteLogoutDialogDestinations
import mega.privacy.android.app.presentation.transfers.navigation.TransfersFeatureDestination
import mega.privacy.android.app.presentation.transfers.transferoverquota.view.dialog.TransferOverQuotaDialogDestinations
import mega.privacy.android.app.sslverification.SSLAppDialogDestinations
import mega.privacy.android.core.sharedcomponents.container.AppContainerProvider
import mega.privacy.android.data.database.LegacyDatabaseMigration
import mega.privacy.android.data.filewrapper.FileWrapper
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.usecase.login.DisableChatApiUseCase
import mega.privacy.android.navigation.MegaActivityResultContract
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.AppDialogDestinations
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogsEventQueue
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class AppModule {
    @MegaApi
    @Singleton
    @Provides
    fun provideMegaApi(
        @ApplicationContext context: Context,
        fileGateway: FileGateway,
    ): MegaApiAndroid {
        FileWrapper.initializeFactory(fileGateway)
        val packageInfo: PackageInfo
        var path: String? = null
        try {
            // PackageManager.PackageInfoFlags can only be used for devices
            // running Android 13 and above. For devices running below Android 13,
            // the normal getPackageInfo() is used
            packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            path = (packageInfo.applicationInfo?.dataDir + "/")
                ?: throw NullPointerException("Application info is null")
        } catch (e: NameNotFoundException) {
            e.printStackTrace()
        }

        val userAgent = "${BuildConfig.USER_AGENT} ${BuildConfig.ENVIRONMENT}".trim()
        return MegaApiAndroid(MegaApplication.APP_KEY, userAgent, path)
    }

    @MegaApiFolder
    @Singleton
    @Provides
    @Suppress("DEPRECATION")
    fun provideMegaApiFolder(@ApplicationContext context: Context): MegaApiAndroid {
        val packageInfo: PackageInfo
        var path: String? = null
        try {
            // PackageManager.PackageInfoFlags can only be used for devices
            // running Android 13 and above. For devices running below Android 13,
            // the normal getPackageInfo() is used
            packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            path = packageInfo.applicationInfo?.dataDir + "/"
        } catch (e: NameNotFoundException) {
            e.printStackTrace()
        }

        return MegaApiAndroid(MegaApplication.APP_KEY, BuildConfig.USER_AGENT, path)
    }

    @Singleton
    @Provides
    fun provideMegaChatApi(@MegaApi megaApi: MegaApiAndroid): MegaChatApiAndroid {
        return MegaChatApiAndroid(megaApi)
    }

    @Singleton
    @Provides
    fun providePreferences(@ApplicationContext context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    @Singleton
    @Provides
    internal fun provideAppNavigator(navigator: MegaNavigatorImpl): MegaNavigator = navigator

    @Singleton
    @Provides
    internal fun provideMegaActivityResultContract(
        impl: MegaActivityResultContractImpl,
    ): MegaActivityResultContract = impl

    @Singleton
    @Provides
    internal fun provideLegacyDatabaseMigration(databaseMigration: LegacyDatabaseMigrationImpl): LegacyDatabaseMigration =
        databaseMigration

    @Provides
    @ElementsIntoSet
    fun provideMainNavItems(): Set<@JvmSuppressWildcards MainNavItem> = emptySet<MainNavItem>()

    @Provides
    @IntoSet
    fun provideTransferFeatureDestinations(): FeatureDestination = TransfersFeatureDestination()

    @Provides
    @IntoMap
    @IntKey(1)
    fun provideFileLinkDeepLinkHandler(handler: FileLinkDeepLinkHandler): DeepLinkHandler = handler

    @Provides
    @IntoMap
    @IntKey(100)
    fun provideWebViewDeepLinkHandler(handler: WebViewDeepLinkHandler): DeepLinkHandler = handler

    @Provides
    fun provideOrderedDeepLinkHandlers(
        features: Map<Int, @JvmSuppressWildcards DeepLinkHandler>,
    ): List<DeepLinkHandler> = features.toSortedMap().values.toList()

    @Provides
    @ElementsIntoSet
    fun provideAppDialogDestinations(): Set<@JvmSuppressWildcards AppDialogDestinations> =
        setOf(
            SSLAppDialogDestinations,
            ConsentDialogDestinations,
            TransferOverQuotaDialogDestinations,
            RemoteLogoutDialogDestinations,
        )

    @Provides
    fun provideDisableChatApiUseCase(): DisableChatApiUseCase =
        DisableChatApiUseCase { MegaApplication.getInstance()::disableMegaChatApi }

    @Provides
    fun provideAppDialogsEventQueue(queue: AppDialogsEventQueueImpl): AppDialogsEventQueue = queue

    @Provides
    fun provideAppDialogsEventQueueReceiver(queue: AppDialogsEventQueueImpl): AppDialogsEventQueueReceiver =
        queue

    @Provides
    fun provideNavigationEventQueue(queue: NavigationEventQueueImpl): NavigationEventQueue = queue

    @Provides
    fun provideNavigationEventQueueReceiver(queue: NavigationEventQueueImpl): NavigationEventQueueReceiver =
        queue

    @Provides
    fun provideAppContainerProvider(
        provider: MegaAppContainerProvider,
    ): AppContainerProvider = provider

    @Provides
    @IntoMap
    @IntKey(5)
    fun provideCreateAccountDeepLinkHandler(handler: CreateAccountDeepLinkHandler): DeepLinkHandler =
        handler

    @Provides
    @IntoMap
    @IntKey(10)
    fun provideLoginDeepLinkHandler(handler: LoginDeepLinkHandler): DeepLinkHandler = handler
}
