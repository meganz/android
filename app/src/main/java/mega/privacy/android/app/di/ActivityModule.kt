package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import mega.privacy.android.app.presentation.container.AppContainerWrapper
import mega.privacy.android.app.presentation.psa.legacy.ActivityAppContainerWrapper
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.presentation.security.PasscodeFacade

/**
 * Activity module
 *
 * Provides any dependencies needed by the hosting activities
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class ActivityModule {

    /**
     * Bind passcode check
     *
     * @param implementation
     */
    @Binds
    abstract fun bindPasscodeCheck(implementation: PasscodeFacade): PasscodeCheck

    /**
     * Bind app container wrapper
     *
     * @param implementation
     */
    @Binds
    abstract fun bindAppContainerWrapper(implementation: ActivityAppContainerWrapper): AppContainerWrapper


}