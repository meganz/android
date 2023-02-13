package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck

@Module(includes = [InternalAvatarModule::class])
@DisableInstallInCheck
abstract class AvatarModule