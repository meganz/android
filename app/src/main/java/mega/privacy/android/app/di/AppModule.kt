package mega.privacy.android.app.di

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Process
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.threadpool.MegaThreadFactory
import mega.privacy.android.domain.usecase.DefaultGetThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @MegaApi
    @Singleton
    @Provides
    fun provideMegaApi(@ApplicationContext context: Context): MegaApiAndroid {
        val packageInfo: PackageInfo
        var path: String? = null
        try {
            packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            path = packageInfo.applicationInfo.dataDir + "/"
        } catch (e: NameNotFoundException) {
            e.printStackTrace()
        }

        return MegaApiAndroid(MegaApplication.APP_KEY, BuildConfig.USER_AGENT, path)
    }

    @MegaApiFolder
    @Singleton
    @Provides
    fun provideMegaApiFolder(@ApplicationContext context: Context): MegaApiAndroid {
        val packageInfo: PackageInfo
        var path: String? = null
        try {
            packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            path = packageInfo.applicationInfo.dataDir + "/"
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
    fun provideDbHandler(@ApplicationContext context: Context): DatabaseHandler {
        return DatabaseHandler.getDbHandler(context)
    }

    @Singleton
    @Provides
    fun providePreferences(@ApplicationContext context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    @Singleton
    @Provides
    fun provideThreadPoolExecutor(): ThreadPoolExecutor {
        val noOfCores = Runtime.getRuntime().availableProcessors()
        return ThreadPoolExecutor(
            noOfCores * 4,
            noOfCores * 8,
            60L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>(),
            MegaThreadFactory(Process.THREAD_PRIORITY_BACKGROUND)
        )
    }

    @Provides
    fun provideGetThemeModePreference(useCase: DefaultGetThemeMode): GetThemeMode =
        useCase
}
