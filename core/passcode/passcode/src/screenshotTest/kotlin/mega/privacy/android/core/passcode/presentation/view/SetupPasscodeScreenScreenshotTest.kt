package mega.privacy.android.core.passcode.presentation.view

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import de.palm.composestateevents.consumed
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.passcode.presentation.model.PasscodeEntryStage
import mega.privacy.android.core.passcode.presentation.model.PasscodeTypeOption
import mega.privacy.android.core.passcode.presentation.model.PasscodeUIType
import mega.privacy.android.core.passcode.presentation.model.SetupPasscodeUiState

class SetupPasscodeScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SetupPasscodeFirstEntryFourDigits() {
        SetupPasscodeScreenshot(passcodeType = PasscodeUIType.Pin(false, 4))
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SetupPasscodeFirstEntrySixDigits() {
        SetupPasscodeScreenshot(passcodeType = PasscodeUIType.Pin(false, 6))
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SetupPasscodeFirstEntryAlphanumeric() {
        SetupPasscodeScreenshot(passcodeType = PasscodeUIType.Alphanumeric(false))
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SetupPasscodeConfirmation() {
        SetupPasscodeScreenshot(
            stage = PasscodeEntryStage.Confirmation("1234", showMismatchError = false)
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SetupPasscodeConfirmationWithMismatchError() {
        SetupPasscodeScreenshot(
            stage = PasscodeEntryStage.Confirmation("1234", showMismatchError = true)
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChangePasscodeFirstEntry() {
        SetupPasscodeScreenshot(isEdit = true)
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChangePasscodeConfirmation() {
        SetupPasscodeScreenshot(
            stage = PasscodeEntryStage.Confirmation("1234", showMismatchError = false),
            isEdit = true,
        )
    }

    /**
     * The sheet itself renders into a popup window that does not appear in a golden, so the
     * options it hosts are captured directly.
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun PasscodeTypeOptions() {
        AndroidThemeForPreviews {
            Column {
                PasscodeTypeOptions(
                    selectedOption = PasscodeTypeOption.FourDigits,
                    onOptionSelected = {},
                )
            }
        }
    }

    @Composable
    private fun SetupPasscodeScreenshot(
        passcodeType: PasscodeUIType = PasscodeUIType.Pin(false, 4),
        stage: PasscodeEntryStage = PasscodeEntryStage.FirstEntry,
        isEdit: Boolean = false,
    ) {
        AndroidThemeForPreviews {
            SetupPasscodeScreen(
                uiState = SetupPasscodeUiState.Data(
                    passcodeType = passcodeType,
                    stage = stage,
                    passcodeSavedEvent = consumed,
                ),
                isEdit = isEdit,
                onPasscodeEntered = {},
                onPasscodeTypeSelected = {},
                onNavigateBack = {},
            )
        }
    }
}
