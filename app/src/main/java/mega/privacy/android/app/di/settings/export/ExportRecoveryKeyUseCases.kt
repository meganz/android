package mega.privacy.android.app.di.settings.export

import mega.privacy.android.domain.di.RecoveryKeyModule as DomainRecoveryKeyModule
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Dagger module for Use Cases in Export Recovery Key Activity
 */
@Module(includes = [DomainRecoveryKeyModule::class])
@InstallIn(ViewModelComponent::class)
internal abstract class ExportRecoveryKeyUseCases