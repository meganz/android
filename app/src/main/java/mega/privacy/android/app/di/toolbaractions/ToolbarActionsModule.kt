package mega.privacy.android.app.di.toolbaractions

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.presentation.validator.toolbaractions.modifier.AudioToolbarActionsModifier
import mega.privacy.android.app.presentation.validator.toolbaractions.modifier.CloudDriveSyncsToolbarActionsModifier
import mega.privacy.android.app.presentation.validator.toolbaractions.modifier.DocumentSectionToolbarActionsModifier
import mega.privacy.android.app.presentation.validator.toolbaractions.modifier.ToolbarActionsModifier

@Module
@InstallIn(SingletonComponent::class)
interface ToolbarActionsModule {

    @Binds
    @IntoSet
    fun bindCloudDriveSyncsToolbarActionsModifier(impl: CloudDriveSyncsToolbarActionsModifier): ToolbarActionsModifier

    @Binds
    @IntoSet
    fun bindAudioToolbarActionsModifier(impl: AudioToolbarActionsModifier): ToolbarActionsModifier

    @Binds
    @IntoSet
    fun bindDocumentSectionToolbarActionsModifier(impl: DocumentSectionToolbarActionsModifier): ToolbarActionsModifier
}
