package mega.privacy.android.app.di.avatar

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.di.AvatarModule

/**
 * Provides the use case implementation for avatar
 */
@Module(includes = [AvatarModule::class])
@InstallIn(ViewModelComponent::class)
abstract class AvatarUseCases