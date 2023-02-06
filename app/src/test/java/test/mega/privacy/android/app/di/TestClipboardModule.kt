package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.domain.di.ClipboardModule
import mega.privacy.android.domain.repository.ClipboardRepository
import mega.privacy.android.domain.usecase.CopyToClipBoard
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [ClipboardModule::class],
    components = [SingletonComponent::class]
)
@Module
object TestClipboardModule {

    private val copyToClipBoard = mock<CopyToClipBoard>()

    @Provides
    fun provideCopyToClipboard(clipboardRepository: ClipboardRepository): CopyToClipBoard =
        copyToClipBoard
}