package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import mega.privacy.presentation.security.PasscodeCheck
import mega.privacy.presentation.security.PasscodeFacade

/**
 * Activity module
 *
 * Provides any dependencies needed by the hosting activities
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class ActivityModule {

    @Binds
    abstract fun bindPasscodeCheck(implementation: PasscodeFacade): PasscodeCheck

}