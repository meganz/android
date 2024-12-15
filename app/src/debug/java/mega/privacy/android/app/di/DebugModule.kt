package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.presentation.settings.model.SetFeatureFlagPlaceHolder

@Module
@InstallIn(SingletonComponent::class)
object DebugModule {

    @Provides
    fun provideSetFeatureFlagPlaceHolder(): SetFeatureFlagPlaceHolder =
        SetFeatureFlagPlaceHolder { _, _ -> }
}