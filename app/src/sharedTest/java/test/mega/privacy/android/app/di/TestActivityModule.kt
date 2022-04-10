package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.ActivityModule
import mega.privacy.android.app.presentation.security.PasscodeCheck
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [ActivityModule::class],
    components = [ActivityComponent::class]
)
@Module
object TestActivityModule {

    @Provides
    fun providePasscodeCheck(): PasscodeCheck = mock()
}