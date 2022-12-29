package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.utils.greeter.Greeter

@Module
@InstallIn(SingletonComponent::class)
object GreeterModule {
    @Provides
    fun provideGreeter() = Greeter { /* No op */ }
}
