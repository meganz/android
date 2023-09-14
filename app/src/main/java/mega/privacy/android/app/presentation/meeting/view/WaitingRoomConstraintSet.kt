package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility

/**
 * Main function to create Waiting Room [ConstraintSet]
 *
 * @param isLandscape   Flag to check if UI is in Landscape mode
 * @param showGuestUi   Flag to check if Guest UI should be shown
 * @return              [ConstraintSet]
 */
fun createWaitingRoomConstraintSet(
    isLandscape: Boolean,
    showGuestUi: Boolean,
): ConstraintSet = ConstraintSet {
    val closeButton = createRefFor("closeButton")
    val infoButton = createRefFor("infoButton")
    val titleText = createRefFor("titleText")
    val timestampText = createRefFor("timestampText")
    val alertText = createRefFor("alertText")
    val videoPreview = createRefFor("videoPreview")
    val guestBackground = createRefFor("guestBackground")
    val guestInputs = createRefFor("guestInputs")
    val controls = createRefFor("controls")
    val joinButton = createRefFor("joinButton")

    val topGuideline = createGuidelineFromTop(if (isLandscape) 0.06f else 0.08f)
    val videoStartGuideline = createGuidelineFromStart(if (isLandscape) 0.29f else 0.16f)
    val videoEndGuideline = createGuidelineFromEnd(if (isLandscape) 0.29f else 0.16f)
    val guestGuideline = if (isLandscape) {
        timestampText.bottom
    } else {
        createGuidelineFromBottom(0.4f)
    }

    constrain(closeButton) {
        top.linkTo(parent.top)
        start.linkTo(parent.start)
    }

    constrain(infoButton) {
        top.linkTo(parent.top)
        end.linkTo(parent.end)
    }

    constrain(titleText) {
        top.linkTo(topGuideline)
        linkTo(parent.start, parent.end)
    }

    constrain(timestampText) {
        top.linkTo(titleText.bottom, 2.dp)
        linkTo(parent.start, parent.end)
    }

    constrain(alertText) {
        top.linkTo(timestampText.bottom, 24.dp)
        linkTo(parent.start, parent.end)
        visibility = if (showGuestUi) {
            Visibility.Invisible
        } else {
            Visibility.Visible
        }
    }

    constrain(videoPreview) {
        top.linkTo(alertText.bottom, 24.dp)
        linkTo(videoStartGuideline, videoEndGuideline)
        width = Dimension.fillToConstraints
        if (isLandscape) {
            height = Dimension.fillToConstraints
            bottom.linkTo(parent.bottom)
        } else {
            height = Dimension.ratio("55:83")
        }
    }

    constrain(controls) {
        if (showGuestUi) {
            top.linkTo(guestInputs.bottom, 25.dp)
        } else if (isLandscape) {
            bottom.linkTo(parent.bottom, 12.dp)
        } else {
            top.linkTo(videoPreview.bottom, 65.dp)
        }
        linkTo(videoStartGuideline, videoEndGuideline)
        width = Dimension.fillToConstraints
    }

    if (showGuestUi) {
        constrain(guestBackground) {
            top.linkTo(guestGuideline)
            bottom.linkTo(parent.bottom)
            linkTo(parent.start, parent.end)
            width = Dimension.fillToConstraints
            height = Dimension.fillToConstraints
        }

        constrain(guestInputs) {
            top.linkTo(guestBackground.top, 8.dp)
            linkTo(videoStartGuideline, videoEndGuideline)
            width = Dimension.fillToConstraints
        }

        constrain(joinButton) {
            top.linkTo(controls.bottom, if (isLandscape) 30.dp else 40.dp)
            linkTo(videoStartGuideline, videoEndGuideline)
            width = Dimension.fillToConstraints
        }
    }
}
