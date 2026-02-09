package mega.privacy.android.core.sharedcomponents.extension

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Ported from material3 expressive, should be deleted when we start using material3 expressive directly
 */
fun Modifier.animateFloatingActionButton(
    visible: Boolean,
    alignment: Alignment,
    targetScale: Float = 0.2f,
    scaleAnimationSpec: AnimationSpec<Float>? = null,
    alphaAnimationSpec: AnimationSpec<Float>? = null,
): Modifier {
    return this.then(
        FabVisibleModifier(
            visible = visible,
            alignment = alignment,
            targetScale = targetScale,
            scaleAnimationSpec = scaleAnimationSpec,
            alphaAnimationSpec = alphaAnimationSpec,
        )
    )
}

internal data class FabVisibleModifier(
    private val visible: Boolean,
    private val alignment: Alignment,
    private val targetScale: Float,
    private val scaleAnimationSpec: AnimationSpec<Float>? = null,
    private val alphaAnimationSpec: AnimationSpec<Float>? = null,
) : ModifierNodeElement<FabVisibleNode>() {

    override fun create(): FabVisibleNode =
        FabVisibleNode(
            visible = visible,
            alignment = alignment,
            targetScale = targetScale,
            scaleAnimationSpec = scaleAnimationSpec,
            alphaAnimationSpec = alphaAnimationSpec,
        )

    override fun update(node: FabVisibleNode) {
        node.updateNode(
            visible = visible,
            alignment = alignment,
            targetScale = targetScale,
            scaleAnimationSpec = scaleAnimationSpec,
            alphaAnimationSpec = alphaAnimationSpec,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        // Show nothing in the inspector.
    }
}

internal class FabVisibleNode(
    visible: Boolean,
    private var alignment: Alignment,
    private var targetScale: Float,
    private var scaleAnimationSpec: AnimationSpec<Float>? = null,
    private var alphaAnimationSpec: AnimationSpec<Float>? = null,
) : DelegatingNode(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

    private val scaleAnimatable = Animatable(if (visible) 1f else 0f)
    private val alphaAnimatable = Animatable(if (visible) 1f else 0f)
    private val fastSpatialSpec = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 800.0f,
    ) as FiniteAnimationSpec<Float>
    private val fastEffectsSpec = spring<Float>(
        dampingRatio = 1.0f,
        stiffness = 3800.0f,
    ) as FiniteAnimationSpec<Float>

    init {
        delegate(
            CacheDrawModifierNode {
                val layer = obtainGraphicsLayer()
                // Use a larger layer size to make sure the elevation shadow doesn't get clipped
                // and offset via layer.topLeft and DrawScope.inset to preserve the visual
                // position of the FAB.
                val layerInsetSize = 16.dp.toPx()
                val layerSize =
                    Size(size.width + layerInsetSize * 2f, size.height + layerInsetSize * 2f)
                        .toIntSize()
                val nodeSize = size.toIntSize()

                layer.apply {
                    topLeft = IntOffset(-layerInsetSize.roundToInt(), -layerInsetSize.roundToInt())

                    alpha = alphaAnimatable.value

                    // Scale towards the direction of the provided alignment
                    val alignOffset = alignment.align(IntSize(1, 1), nodeSize, layoutDirection)
                    pivotOffset = alignOffset.toOffset() + Offset(layerInsetSize, layerInsetSize)
                    scaleX = lerp(targetScale, 1f, scaleAnimatable.value)
                    scaleY = lerp(targetScale, 1f, scaleAnimatable.value)

                    record(size = layerSize) {
                        inset(layerInsetSize, layerInsetSize) { this@record.drawContent() }
                    }
                }

                onDrawWithContent { drawLayer(layer) }
            }
        )
    }

    fun updateNode(
        visible: Boolean,
        alignment: Alignment,
        targetScale: Float,
        scaleAnimationSpec: AnimationSpec<Float>?,
        alphaAnimationSpec: AnimationSpec<Float>?,
    ) {
        this.alignment = alignment
        this.targetScale = targetScale
        this.scaleAnimationSpec = scaleAnimationSpec
        this.alphaAnimationSpec = alphaAnimationSpec

        coroutineScope.launch {
            scaleAnimatable.animateTo(
                targetValue = if (visible) 1f else 0f,
                animationSpec =
                    scaleAnimationSpec
                        ?: fastSpatialSpec,
            )
        }

        coroutineScope.launch {
            alphaAnimatable.animateTo(
                targetValue = if (visible) 1f else 0f,
                animationSpec =
                    alphaAnimationSpec
                        ?: fastEffectsSpec,
            )
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        if (alphaAnimatable.value == 0f) {
            return layout(0, 0) {}
        }
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}