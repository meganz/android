package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.memory.PoolConfig
import com.facebook.imagepipeline.memory.PoolFactory
import mega.privacy.android.app.utils.ContextUtils.getAvailableMemory
import mega.privacy.android.app.utils.FrescoNativeMemoryChunkPoolParams

/**
 * Initialize Fresco library
 */
class FrescoInitializer : Initializer<Unit> {
    /**
     * Create
     *
     */
    override fun create(context: Context) {
        val poolFactory = PoolFactory(
            PoolConfig.newBuilder()
                .setNativeMemoryChunkPoolParams(FrescoNativeMemoryChunkPoolParams.get(context.getAvailableMemory()))
                .build()
        )
        Fresco.initialize(context, ImagePipelineConfig.newBuilder(context)
            .setPoolFactory(poolFactory)
            .setDownsampleEnabled(true)
            .build())
    }

    /**
     * Dependencies
     *
     */
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(LoggerInitializer::class.java)
}