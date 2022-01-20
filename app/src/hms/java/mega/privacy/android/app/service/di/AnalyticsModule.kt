package mega.privacy.android.app.service.di

import com.huawei.agconnect.crash.AGConnectCrash
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.middlelayer.reporter.CrashReporter
import mega.privacy.android.app.middlelayer.reporter.PerformanceReporter
import mega.privacy.android.app.service.reporter.AppGalleryCrashReporter
import mega.privacy.android.app.service.reporter.AppGalleryPerformanceReporter
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AnalyticsModule {

    @Provides
    fun provideAppGalleryConnectCrash(): AGConnectCrash =
        AGConnectCrash.getInstance()

    @Singleton
    @Provides
    fun provideCrashReporter(agConnectCrash: AGConnectCrash): CrashReporter =
        AppGalleryCrashReporter(agConnectCrash)

    @Singleton
    @Provides
    fun providePerformanceReporter(): PerformanceReporter =
        AppGalleryPerformanceReporter()
}
