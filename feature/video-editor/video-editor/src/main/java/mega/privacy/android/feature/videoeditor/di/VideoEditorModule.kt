package mega.privacy.android.feature.videoeditor.di

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import mega.privacy.android.feature.videoeditor.navigation.VideoEditorFeatureGraph
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.ToolRegistry
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.EditorTool
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.crop.CropTool
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.rotate.RotateTool
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.speed.SpeedTool
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.trim.TrimTool
import mega.privacy.android.navigation.contract.FeatureDestination
import javax.inject.Singleton

/**
 * Hilt bindings for the video editor feature: the navigation destination, the
 * multibound set of [EditorTool]s, and the [ToolRegistry] assembled from it.
 *
 * The interface holds the abstract [Multibinds] declaration; its [companion]
 * object holds the concrete [Provides] methods, so a single module covers both.
 * Each tool contributes itself with an `@Provides @IntoSet` binding; when the
 * set is empty the editor renders without tools.
 */
@Module
@InstallIn(SingletonComponent::class)
interface VideoEditorModule {

    @Multibinds
    @OptIn(UnstableApi::class)
    fun editorTools(): Set<EditorTool>

    companion object {

        @Provides
        @IntoSet
        fun provideVideoEditorFeatureDestination(): FeatureDestination = VideoEditorFeatureGraph()

        @Provides
        @Singleton
        @OptIn(UnstableApi::class)
        fun provideToolRegistry(tools: Set<@JvmSuppressWildcards EditorTool>): ToolRegistry =
            ToolRegistry(tools.toList())

        @Provides
        @IntoSet
        @OptIn(UnstableApi::class)
        fun provideTrimTool(): EditorTool = TrimTool

        @Provides
        @IntoSet
        @OptIn(UnstableApi::class)
        fun provideCropTool(): EditorTool = CropTool

        @Provides
        @IntoSet
        @OptIn(UnstableApi::class)
        fun provideRotateTool(): EditorTool = RotateTool

        @Provides
        @IntoSet
        @OptIn(UnstableApi::class)
        fun provideSpeedTool(): EditorTool = SpeedTool
    }
}
