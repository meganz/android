package mega.privacy.android.app.di.account

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.di.ChangePasswordModule

/**
 * Dagger module for Use Cases in Change Password Activity
 */
@Module(includes = [ChangePasswordModule::class])
@InstallIn(ViewModelComponent::class)
internal abstract class ChangePasswordUseCases