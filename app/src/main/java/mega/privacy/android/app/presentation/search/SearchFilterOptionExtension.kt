package mega.privacy.android.app.presentation.search

import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.mobile.analytics.event.SearchDateAddedLastSevenDaysClickedEvent
import mega.privacy.mobile.analytics.event.SearchDateAddedLastThirtyDaysClickedEvent
import mega.privacy.mobile.analytics.event.SearchDateAddedLastYearClickedEvent
import mega.privacy.mobile.analytics.event.SearchDateAddedOlderClickedEvent
import mega.privacy.mobile.analytics.event.SearchDateAddedThisYearClickedEvent
import mega.privacy.mobile.analytics.event.SearchDateAddedTodayClickedEvent
import mega.privacy.mobile.analytics.event.SearchFileTypeAudioOptionClickedEvent
import mega.privacy.mobile.analytics.event.SearchFileTypeDocumentsOptionClickedEvent
import mega.privacy.mobile.analytics.event.SearchFileTypeFolderOptionClickedEvent
import mega.privacy.mobile.analytics.event.SearchFileTypeImagesOptionClickedEvent
import mega.privacy.mobile.analytics.event.SearchFileTypeOtherOptionClickedEvent
import mega.privacy.mobile.analytics.event.SearchFileTypePdfOptionClickedEvent
import mega.privacy.mobile.analytics.event.SearchFileTypePresentationOptionClickedEvent
import mega.privacy.mobile.analytics.event.SearchFileTypeSpreadsheetOptionClickedEvent
import mega.privacy.mobile.analytics.event.SearchFileTypeVideoOptionClickedEvent
import mega.privacy.mobile.analytics.event.SearchLastModifiedLastSevenDaysClickedEvent
import mega.privacy.mobile.analytics.event.SearchLastModifiedLastThirtyDaysClickedEvent
import mega.privacy.mobile.analytics.event.SearchLastModifiedLastYearClickedEvent
import mega.privacy.mobile.analytics.event.SearchLastModifiedOlderClickedEvent
import mega.privacy.mobile.analytics.event.SearchLastModifiedThisYearClickedEvent
import mega.privacy.mobile.analytics.event.SearchLastModifiedTodayClickedEvent

/**
 * Track the correct event on each file type filter option
 */
internal fun TypeFilterOption.trackAsAnalyticsEvent() = Analytics.tracker.trackEvent(
    when (this) {
        TypeFilterOption.Images -> SearchFileTypeImagesOptionClickedEvent
        TypeFilterOption.Documents -> SearchFileTypeDocumentsOptionClickedEvent
        TypeFilterOption.Audio -> SearchFileTypeAudioOptionClickedEvent
        TypeFilterOption.Video -> SearchFileTypeVideoOptionClickedEvent
        TypeFilterOption.Pdf -> SearchFileTypePdfOptionClickedEvent
        TypeFilterOption.Presentation -> SearchFileTypePresentationOptionClickedEvent
        TypeFilterOption.Spreadsheet -> SearchFileTypeSpreadsheetOptionClickedEvent
        TypeFilterOption.Folder -> SearchFileTypeFolderOptionClickedEvent
        TypeFilterOption.Other -> SearchFileTypeOtherOptionClickedEvent
    }
)

/**
 * Track the correct event on each last modified filter option
 */
internal fun DateFilterOption.trackAsLastModifiedAnalyticsEvent() = Analytics.tracker.trackEvent(
    when (this) {
        DateFilterOption.Today -> SearchLastModifiedTodayClickedEvent
        DateFilterOption.Last7Days -> SearchLastModifiedLastSevenDaysClickedEvent
        DateFilterOption.Last30Days -> SearchLastModifiedLastThirtyDaysClickedEvent
        DateFilterOption.ThisYear -> SearchLastModifiedThisYearClickedEvent
        DateFilterOption.LastYear -> SearchLastModifiedLastYearClickedEvent
        DateFilterOption.Older -> SearchLastModifiedOlderClickedEvent
    }
)

/**
 * Track the correct event on each date added filter option
 */
internal fun DateFilterOption.trackAsDateAddedAnalyticsEvent() = Analytics.tracker.trackEvent(
    when (this) {
        DateFilterOption.Today -> SearchDateAddedTodayClickedEvent
        DateFilterOption.Last7Days -> SearchDateAddedLastSevenDaysClickedEvent
        DateFilterOption.Last30Days -> SearchDateAddedLastThirtyDaysClickedEvent
        DateFilterOption.ThisYear -> SearchDateAddedThisYearClickedEvent
        DateFilterOption.LastYear -> SearchDateAddedLastYearClickedEvent
        DateFilterOption.Older -> SearchDateAddedOlderClickedEvent
    }
)
