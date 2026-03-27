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
import mega.privacy.android.feature.photos.model.PhotosNodeContentItem
import mega.privacy.android.feature.photos.model.PhotosNodeContentItem.PhotoNodeItem
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import java.time.LocalDateTime

@Composable
internal fun rememberTimelineLazyListState(selectedTimePeriod: MediaTimePeriod): TimelineLazyListState {
    val lazyGridState = rememberLazyGridState()
    val listStates = mapOf(
        MediaTimePeriod.Years to rememberLazyListState(),
        MediaTimePeriod.Months to rememberLazyListState(),
        MediaTimePeriod.Days to rememberLazyListState(),
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
    private val selectedTimePeriod: MediaTimePeriod,
    internal val lazyGridState: LazyGridState,
    internal val lazyListState: LazyListState,
) {

    internal val isScrollInProgress: Boolean
        get() = when (selectedTimePeriod) {
            MediaTimePeriod.All -> lazyGridState.isScrollInProgress
            else -> lazyListState.isScrollInProgress
        }

    internal val isScrollingDown: State<Boolean>
        @Composable get() = when (selectedTimePeriod) {
            MediaTimePeriod.All -> lazyGridState.isScrollingDown()
            else -> lazyListState.isScrollingDown()
        }

    internal val isScrolledToEnd: State<Boolean>
        @Composable get() = when (selectedTimePeriod) {
            MediaTimePeriod.All -> lazyGridState.isScrolledToEnd()
            else -> lazyListState.isScrolledToEnd()
        }

    internal val isScrolledToTop: State<Boolean>
        @Composable get() = when (selectedTimePeriod) {
            MediaTimePeriod.All -> lazyGridState.isScrolledToTop()
            else -> lazyListState.isScrolledToTop()
        }

    internal val totalItemsCount: Int
        get() = when (selectedTimePeriod) {
            MediaTimePeriod.All -> lazyGridState.layoutInfo.totalItemsCount
            else -> lazyListState.layoutInfo.totalItemsCount
        }

    internal val firstVisibleItemIndex: Int
        get() = when (selectedTimePeriod) {
            MediaTimePeriod.All -> lazyGridState.firstVisibleItemIndex
            else -> lazyListState.firstVisibleItemIndex
        }

    internal suspend fun scrollToItem(index: Int = 0, scrollOffset: Int = 0) {
        when (selectedTimePeriod) {
            MediaTimePeriod.All -> lazyGridState.scrollToItem(
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
        displayedPhotos: List<PhotosNodeContentItem>,
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
        targetPeriod: MediaTimePeriod,
        displayedPhotos: List<PhotosNodeContentItem>,
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
            MediaTimePeriod.All -> {
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
                    MediaTimePeriod.Years -> yearsCardPhotos
                    MediaTimePeriod.Months -> monthsCardPhotos
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
        targetPeriod: MediaTimePeriod,
        targetModificationTime: LocalDateTime,
        displayedPhotos: List<PhotosNodeContentItem>,
        daysCardPhotos: List<PhotosNodeListCard>,
        monthsCardPhotos: List<PhotosNodeListCard>,
        yearsCardPhotos: List<PhotosNodeListCard>,
    ): Int {
        return when (targetPeriod) {
            MediaTimePeriod.Days -> {
                daysCardPhotos.indexOfFirst {
                    it.photoItem.photo.modificationTime.year == targetModificationTime.year &&
                            it.photoItem.photo.modificationTime.month == targetModificationTime.month &&
                            it.photoItem.photo.modificationTime.dayOfMonth == targetModificationTime.dayOfMonth
                }
            }

            MediaTimePeriod.Months -> {
                monthsCardPhotos.indexOfFirst {
                    it.photoItem.photo.modificationTime.year == targetModificationTime.year &&
                            it.photoItem.photo.modificationTime.month == targetModificationTime.month
                }
            }

            MediaTimePeriod.Years -> {
                yearsCardPhotos.indexOfFirst {
                    it.photoItem.photo.modificationTime.year == targetModificationTime.year
                }
            }

            MediaTimePeriod.All -> {
                val firstVisibleGridItem =
                    findFirstVisibleGridItem<PhotosNodeContentItem.HeaderItem>(
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

    private inline fun <reified T : PhotosNodeContentItem> findFirstVisibleGridItem(
        isCUBannerVisible: Boolean,
        firstVisibleItemIndex: Int,
        displayedPhotos: List<PhotosNodeContentItem>,
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

                    is PhotosNodeContentItem.HeaderItem -> GridFirstVisibleItemResult(
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
