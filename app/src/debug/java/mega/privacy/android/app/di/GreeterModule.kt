package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.utils.greeter.DebugGreeter
import mega.privacy.android.app.utils.greeter.Greeter

@Module
@InstallIn(SingletonComponent::class)
interface GreeterModule {
    @Binds
    fun bindGreeter(greeter: DebugGreeter): Greeter
}
