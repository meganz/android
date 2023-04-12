package mega.privacy.android.app.presentation.slideshow.view

import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times

/**
 * Creates a [PhotoState] that is remembered across compositions.
 *
 * Changes to the provided values for [initialScale] will **not** result in the state being
 * recreated or changed in any way if it has already
 * been created.
 *
 * @param initialScale the initial value for [PhotoState.currentScale]
 * @param initialOffset the initial value for [PhotoState.currentOffset]
 */
@Composable
fun rememberPhotoState(
    @FloatRange(from = 1.0) initialScale: Float = 1f,
    initialOffset: Offset = Offset.Zero,
    minimumScale: Float = 1f,
    maximumScale: Float = 3f,
): PhotoState = rememberSaveable(saver = PhotoState.Saver) {
    PhotoState(
        currentScale = initialScale,
        currentOffset = initialOffset,
        minimumScale = minimumScale,
        maximumScale = maximumScale,
    )
}

/**
 * A state object that can be hoisted to control and observe scaling and scrolling for [PhotoBox].
 *
 * In most cases, this will be created via [rememberPhotoState].
 *
 * @param currentScale the initial value for [PhotoState.currentScale]
 * @param currentOffset the initial value for [PhotoState.currentOffset]
 */
@Stable
class PhotoState(
    @FloatRange(from = 1.0) currentScale: Float = 1f,
    currentOffset: Offset = Offset.Zero,
    private val minimumScale: Float = 1f,
    internal val maximumScale: Float = 3f,
) {

    internal var layoutSize: Size = Size.Zero

    private var photoIntrinsicSize: Size = Size.Unspecified

    /**
     * Set the intrinsic size of the photo.
     * If the value is set to [Size.Unspecified], it is assumed to be layout size.
     */
    fun setPhotoIntrinsicSize(size: Size) {
        photoIntrinsicSize = size
    }

    private var _currentScale by mutableStateOf(currentScale)

    @get:FloatRange(from = 1.0)
    internal var currentScale: Float
        get() = _currentScale
        internal set(value) {
            val coerceValue = value.coerceIn(minimumScale, maximumScale)
            if (coerceValue != _currentScale) {
                _currentScale = coerceValue
            }
        }

    private var _currentOffset by mutableStateOf(currentOffset)

    internal var currentOffset: Offset
        get() = _currentOffset
        internal set(value) {
            val (scrollableX, scrollableY) = calculateScrollableBounds()
            val coerceValue = Offset(
                value.x.coerceIn(-scrollableX, scrollableX),
                value.y.coerceIn(-scrollableY, scrollableY),
            )
            if (coerceValue != _currentOffset) {
                _currentOffset = coerceValue
            }
        }

    private fun calculateScrollableBounds(): Offset {
        val content = if (photoIntrinsicSize.isSpecified) {
            val contentScale = ContentScale.Fit
            photoIntrinsicSize * contentScale.computeScaleFactor(photoIntrinsicSize, layoutSize)
        } else {
            layoutSize
        }
        return Offset(
            x = ((content.width * currentScale - layoutSize.width) / 2).coerceAtLeast(0f),
            y = ((content.height * currentScale - layoutSize.height) / 2).coerceAtLeast(0f),
        )
    }

    /**
     * Is scaled
     */
    val isScaled: Boolean
        get() = currentScale != 1f
                || (currentOffset.x != Offset.Zero.x
                && currentOffset.y != Offset.Zero.y)


    /**
     * Reset scale
     */
    fun resetScale() {
        currentScale = 1f
        currentOffset = Offset.Zero
    }

    /**
     * Animate to the initial state.
     */
    suspend fun animateToInitialState() {
        val initialScale = currentScale
        val targetScale = minimumScale
        val initialOffset = currentOffset
        val targetOffset = Offset.Zero
        if (initialScale != targetScale || initialOffset != targetOffset) {
            val scaleDiff = targetScale - initialScale
            val offsetDiff = targetOffset - initialOffset
            val anim = AnimationState(initialValue = 0f)
            anim.animateTo(targetValue = 1f) {
                currentScale = initialScale + scaleDiff * value
                currentOffset = initialOffset + offsetDiff * value
            }
        }
    }

    /**
     * Animate to the given scale to the center of the layout.
     *
     * @param scale the scale to animate to. Must be between 1f and [maximumScale] (inclusive).
     */
    suspend fun animateScale(
        @FloatRange(from = 1.0) scale: Float,
    ) {
        val initialScale = currentScale
        if (initialScale != scale) {
            val diff = scale - initialScale
            val anim = AnimationState(initialValue = 0f)
            anim.animateTo(targetValue = 1f) {
                currentScale = initialScale + diff * value
            }
        }
    }

    internal suspend fun performFling(
        initialVelocity: Offset,
        decay: DecayAnimationSpec<Offset> = exponentialDecay(),
    ) {
        val initialValue = currentOffset
        val anim = AnimationState(
            typeConverter = Offset.VectorConverter,
            initialValue = initialValue,
            initialVelocity = initialVelocity,
        )
        anim.animateDecay(decay) {
            currentOffset = value

            if (isOutOfBounds(value) || velocity.getDistance() <= 3000) {
                cancelAnimation()
            }
        }
    }

    private fun isOutOfBounds(offset: Offset): Boolean {
        val (scrollableX, scrollableY) = calculateScrollableBounds()
        return offset.x !in -scrollableX..scrollableX && offset.y !in (-scrollableY..scrollableY)
    }

    internal companion object {
        /**
         * The default [Saver] implementation for [PhotoState].
         */
        val Saver: Saver<PhotoState, *> = listSaver(
            save = {
                listOf<Any>(
                    it.currentScale,
                    it.currentOffset.x,
                    it.currentOffset.y,
                    it.minimumScale,
                    it.maximumScale,
                )
            },
            restore = {
                PhotoState(
                    currentScale = it[0] as Float,
                    currentOffset = Offset(
                        x = it[1] as Float,
                        y = it[2] as Float,
                    ),
                    minimumScale = it[3] as Float,
                    maximumScale = it[4] as Float,
                )
            }
        )
    }
}
