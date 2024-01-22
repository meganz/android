package mega.privacy.android.app.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.facade.debugWorkInfo
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WorkManagerModule {
    @Provides
    @Singleton
    internal fun provideWorkManager(@ApplicationContext applicationContext: Context): WorkManager =
        WorkManager.getInstance(applicationContext)
}
