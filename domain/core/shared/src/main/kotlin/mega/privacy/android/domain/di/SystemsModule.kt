package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.qualifier.SystemTime

@Module
@InstallIn(SingletonComponent::class)
internal object SystemsModule {
    @Provides
    @SystemTime
    fun provideSystemTime(): () -> Long = { System.currentTimeMillis() }
}