package mega.privacy.android.feature.photos.presentation.timeline.state

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.model.PhotosNodeContentType.PhotoNodeItem
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoModificationTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import java.time.LocalDateTime

@Composable
internal fun rememberTimelineLazyListState(selectedTimePeriod: PhotoModificationTimePeriod): TimelineLazyListState {
    val lazyGridState = rememberLazyGridState()
    val listStates = mapOf(
        PhotoModificationTimePeriod.Years to rememberLazyListState(),
        PhotoModificationTimePeriod.Months to rememberLazyListState(),
        PhotoModificationTimePeriod.Days to rememberLazyListState(),
    )
    val lazyListState = listStates[selectedTimePeriod] ?: rememberLazyListState()

    return remember(
        selectedTimePeriod,
        lazyGridState,
        lazyListState
    ) {
        TimelineLazyListState(
            selectedTimePeriod = selectedTimePeriod,
            lazyGridState = lazyGridState,
            lazyListState = lazyListState,
        )
    }
}

@Stable
internal class TimelineLazyListState(
    private val selectedTimePeriod: PhotoModificationTimePeriod,
    internal val lazyGridState: LazyGridState,
    internal val lazyListState: LazyListState,
) {

    internal val isScrollInProgress: Boolean
        get() = when (selectedTimePeriod) {
            PhotoModificationTimePeriod.All -> lazyGridState.isScrollInProgress
            else -> lazyListState.isScrollInProgress
        }

    internal val isScrollingDown: State<Boolean>
        @Composable get() = when (selectedTimePeriod) {
            PhotoModificationTimePeriod.All -> lazyGridState.isScrollingDown()
            else -> lazyListState.isScrollingDown()
        }

    internal val isScrolledToEnd: State<Boolean>
        @Composable get() = when (selectedTimePeriod) {
            PhotoModificationTimePeriod.All -> lazyGridState.isScrolledToEnd()
            else -> lazyListState.isScrolledToEnd()
        }

    internal val isScrolledToTop: State<Boolean>
        @Composable get() = when (selectedTimePeriod) {
            PhotoModificationTimePeriod.All -> lazyGridState.isScrolledToTop()
            else -> lazyListState.isScrolledToTop()
        }

    internal val totalItemsCount: Int
        get() = when (selectedTimePeriod) {
            PhotoModificationTimePeriod.All -> lazyGridState.layoutInfo.totalItemsCount
            else -> lazyListState.layoutInfo.totalItemsCount
        }

    internal val firstVisibleItemIndex: Int
        get() = when (selectedTimePeriod) {
            PhotoModificationTimePeriod.All -> lazyGridState.firstVisibleItemIndex
            else -> lazyListState.firstVisibleItemIndex
        }

    internal suspend fun scrollToItem(index: Int = 0, scrollOffset: Int = 0) {
        when (selectedTimePeriod) {
            PhotoModificationTimePeriod.All -> lazyGridState.scrollToItem(
                index = index,
                scrollOffset = scrollOffset
            )

            else -> lazyListState.scrollToItem(
                index = index,
                scrollOffset = scrollOffset
            )
        }
    }

    internal fun calculateScrollIndexBasedOnItemClick(
        photo: PhotosNodeListCard,
        displayedPhotos: List<PhotosNodeContentType>,
        daysCardPhotos: List<PhotosNodeListCard>,
        monthsCardPhotos: List<PhotosNodeListCard>,
    ): Int = when (photo) {
        is PhotosNodeListCard.Years -> {
            val photo = monthsCardPhotos.find {
                it as PhotosNodeListCard.Months
                it.photoItem.photo.modificationTime.year == photo.photoItem.photo.modificationTime.year &&
                        it.photoItem.photo.modificationTime.month == photo.photoItem.photo.modificationTime.month &&
                        it.photoItem.photo.modificationTime.dayOfMonth == photo.photoItem.photo.modificationTime.dayOfMonth
            }
            monthsCardPhotos.indexOf(photo)
        }

        is PhotosNodeListCard.Months -> {
            val photo = daysCardPhotos.find {
                it as PhotosNodeListCard.Days
                it.photoItem.photo.modificationTime.year == photo.photoItem.photo.modificationTime.year &&
                        it.photoItem.photo.modificationTime.month == photo.photoItem.photo.modificationTime.month &&
                        it.photoItem.photo.modificationTime.dayOfMonth == photo.photoItem.photo.modificationTime.dayOfMonth
            }
            daysCardPhotos.indexOf(photo)
        }

        is PhotosNodeListCard.Days -> {
            val photo = displayedPhotos.find { it.key == photo.photoItem.photo.hashCode() }
            displayedPhotos.indexOf(photo)
        }
    }

    internal fun calculateScrollIndexBasedOnTimePeriodClick(
        targetPeriod: PhotoModificationTimePeriod,
        displayedPhotos: List<PhotosNodeContentType>,
        daysCardPhotos: List<PhotosNodeListCard>,
        monthsCardPhotos: List<PhotosNodeListCard>,
        yearsCardPhotos: List<PhotosNodeListCard>,
    ): Int {
        // To avoid recalculation everytime these properties are read.
        val firstVisibleItemIndex = firstVisibleItemIndex
        val totalItemsCount = totalItemsCount
        // Best case
        if (firstVisibleItemIndex == 0) return 0

        val (isCUBannerVisible, targetModificationTime) = when (selectedTimePeriod) {
            PhotoModificationTimePeriod.All -> {
                val isCUBannerVisible = totalItemsCount > displayedPhotos.size
                val firstVisibleGridItem = findFirstVisibleGridItem<PhotoNodeItem>(
                    isCUBannerVisible = isCUBannerVisible,
                    firstVisibleItemIndex = firstVisibleItemIndex,
                    displayedPhotos = displayedPhotos
                )
                isCUBannerVisible to firstVisibleGridItem?.modificationTime
            }

            else -> {
                val items = when (selectedTimePeriod) {
                    PhotoModificationTimePeriod.Years -> yearsCardPhotos
                    PhotoModificationTimePeriod.Months -> monthsCardPhotos
                    else -> daysCardPhotos
                }
                val isCUBannerVisible = totalItemsCount > items.size
                val itemStartIndex =
                    if (isCUBannerVisible) firstVisibleItemIndex - 1 else firstVisibleItemIndex
                val targetModificationTime =
                    if (itemStartIndex >= 0 && itemStartIndex < items.size) {
                        items[itemStartIndex].photoItem.photo.modificationTime
                    } else null
                isCUBannerVisible to targetModificationTime
            }
        }
        return if (targetModificationTime != null) {
            calculateScrollIndexBasedOnTimePeriodClick(
                isCUBannerVisible = isCUBannerVisible,
                targetPeriod = targetPeriod,
                targetModificationTime = targetModificationTime,
                displayedPhotos = displayedPhotos,
                daysCardPhotos = daysCardPhotos,
                monthsCardPhotos = monthsCardPhotos,
                yearsCardPhotos = yearsCardPhotos,
            )
        } else -1
    }

    private fun calculateScrollIndexBasedOnTimePeriodClick(
        isCUBannerVisible: Boolean,
        targetPeriod: PhotoModificationTimePeriod,
        targetModificationTime: LocalDateTime,
        displayedPhotos: List<PhotosNodeContentType>,
        daysCardPhotos: List<PhotosNodeListCard>,
        monthsCardPhotos: List<PhotosNodeListCard>,
        yearsCardPhotos: List<PhotosNodeListCard>,
    ): Int {
        return when (targetPeriod) {
            PhotoModificationTimePeriod.Days -> {
                daysCardPhotos.indexOfFirst {
                    it.photoItem.photo.modificationTime.year == targetModificationTime.year &&
                            it.photoItem.photo.modificationTime.month == targetModificationTime.month &&
                            it.photoItem.photo.modificationTime.dayOfMonth == targetModificationTime.dayOfMonth
                }
            }

            PhotoModificationTimePeriod.Months -> {
                monthsCardPhotos.indexOfFirst {
                    it.photoItem.photo.modificationTime.year == targetModificationTime.year &&
                            it.photoItem.photo.modificationTime.month == targetModificationTime.month
                }
            }

            PhotoModificationTimePeriod.Years -> {
                yearsCardPhotos.indexOfFirst {
                    it.photoItem.photo.modificationTime.year == targetModificationTime.year
                }
            }

            PhotoModificationTimePeriod.All -> {
                val firstVisibleGridItem =
                    findFirstVisibleGridItem<PhotosNodeContentType.HeaderItem>(
                        isCUBannerVisible = isCUBannerVisible,
                        firstVisibleItemIndex = if (isCUBannerVisible) 1 else 0,
                        displayedPhotos = displayedPhotos,
                        filter = {
                            it.time.month == targetModificationTime.month && it.time.year == targetModificationTime.year
                        }
                    )
                firstVisibleGridItem?.index ?: 0
            }
        }
    }

    private inline fun <reified T : PhotosNodeContentType> findFirstVisibleGridItem(
        isCUBannerVisible: Boolean,
        firstVisibleItemIndex: Int,
        displayedPhotos: List<PhotosNodeContentType>,
        filter: (T) -> Boolean = { true },
    ): GridFirstVisibleItemResult? {
        val startIndex = if (isCUBannerVisible) {
            // When the firstVisibleItemIndex is 0 and the CU banner is visible, we need to use the next index.
            // Because the photo item starts at bannerIndex + 1.
            if (firstVisibleItemIndex == 0) 1 else firstVisibleItemIndex - 1
        } else firstVisibleItemIndex
        if (startIndex < 0) return null

        for (i in startIndex until displayedPhotos.size) {
            val item = displayedPhotos[i]
            if (item is T && filter(item)) {
                return when (item) {
                    is PhotoNodeItem -> GridFirstVisibleItemResult(
                        index = i,
                        modificationTime = item.node.photo.modificationTime
                    )

                    is PhotosNodeContentType.HeaderItem -> GridFirstVisibleItemResult(
                        index = i,
                        modificationTime = item.time
                    )
                }
            }
        }
        return null
    }

    private data class GridFirstVisibleItemResult(
        val index: Int,
        val modificationTime: LocalDateTime,
    )
}

@Composable
private fun LazyGridState.isScrollingDown(): State<Boolean> {
    var nextIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var nextScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (nextIndex != firstVisibleItemIndex) {
                nextIndex < firstVisibleItemIndex
            } else {
                nextScrollOffset <= firstVisibleItemScrollOffset
            }.also {
                nextIndex = firstVisibleItemIndex
                nextScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }
}

@Composable
private fun LazyListState.isScrollingDown(): State<Boolean> {
    var nextIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var nextScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (nextIndex != firstVisibleItemIndex) {
                nextIndex < firstVisibleItemIndex
            } else {
                nextScrollOffset <= firstVisibleItemScrollOffset
            }.also {
                nextIndex = firstVisibleItemIndex
                nextScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }
}

@Composable
private fun LazyGridState.isScrolledToEnd() = remember(this) {
    derivedStateOf {
        layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
    }
}

@Composable
private fun LazyListState.isScrolledToEnd() = remember(this) {
    derivedStateOf {
        layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
    }
}

@Composable
private fun LazyGridState.isScrolledToTop() = remember(this) {
    derivedStateOf {
        firstVisibleItemIndex <= 1 && firstVisibleItemScrollOffset == 0
    }
}

@Composable
private fun LazyListState.isScrolledToTop() = remember(this) {
    derivedStateOf {
        firstVisibleItemIndex <= 1 && firstVisibleItemScrollOffset == 0
    }
}
