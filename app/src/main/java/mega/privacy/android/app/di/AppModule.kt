package mega.privacy.android.app.di

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.NameNotFoundException
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
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

        return MegaApiAndroid(MegaApplication.APP_KEY, MegaApplication.USER_AGENT, path)
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

        return MegaApiAndroid(MegaApplication.APP_KEY, MegaApplication.USER_AGENT, path)
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
}
