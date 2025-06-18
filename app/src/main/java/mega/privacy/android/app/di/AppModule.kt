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
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.app.LegacyDatabaseMigrationImpl
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.nav.MegaNavigatorImpl
import mega.privacy.android.data.database.LegacyDatabaseMigration
import mega.privacy.android.data.filewrapper.FileWrapper
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
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
    internal fun provideLegacyDatabaseMigration(databaseMigration: LegacyDatabaseMigrationImpl): LegacyDatabaseMigration =
        databaseMigration

    @Provides
    @ElementsIntoSet
    fun provideMainNavItems(): Set<@JvmSuppressWildcards MainNavItem> = emptySet<MainNavItem>()

    @Provides
    @ElementsIntoSet
    fun provideFeatureDestinations(): Set<@JvmSuppressWildcards FeatureDestination> =
        emptySet<FeatureDestination>()
}
