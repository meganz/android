package mega.privacy.android.feature.sync.ui.exclusion

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.ExclusionsSnapshot
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionEdit
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.domain.entity.SyncMatchStrategy
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.domain.exception.SyncExclusionEditError
import mega.privacy.android.feature.sync.domain.usecase.exclusion.AddSyncExclusionUseCase
import mega.privacy.android.feature.sync.domain.usecase.exclusion.GetSyncExclusionsUseCase
import mega.privacy.android.feature.sync.domain.usecase.exclusion.RemoveSyncExclusionUseCase
import mega.privacy.android.feature.sync.domain.usecase.exclusion.ReplaceSyncExclusionUseCase
import mega.privacy.android.feature.sync.domain.usecase.exclusion.RestoreDefaultSyncExclusionsUseCase
import mega.privacy.android.feature.sync.domain.usecase.exclusion.SetSyncExclusionsUseCase
import mega.privacy.android.feature.sync.domain.usecase.exclusion.SetSyncSizeBoundsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ManageSyncExclusionsViewModelTest {

    private val getSyncExclusionsUseCase = mock<GetSyncExclusionsUseCase>()
    private val setSyncExclusionsUseCase = mock<SetSyncExclusionsUseCase>()

    @AfterEach
    fun tearDown() {
        reset(getSyncExclusionsUseCase, setSyncExclusionsUseCase)
    }

    @Test
    fun `test that init stages a copy of the file it read`() = runTest {
        stubRead(threeRules)

        val underTest = underTest()

        assertThat(underTest.uiState.value.loaded).isEqualTo(threeRules)
        assertThat(underTest.uiState.value.staged).isEqualTo(threeRules)
        assertThat(underTest.uiState.value.isLoading).isFalse()
        assertThat(underTest.uiState.value.isDirty).isFalse()
    }

    @Test
    fun `test that init carries whether the root can be written`() = runTest {
        stubRead(threeRules, isWritable = false)

        val underTest = underTest()

        assertThat(underTest.uiState.value.isWritable).isFalse()
    }

    /**
     * A failed read must not present as a file with no rules: staging an empty model and saving it
     * would replace exclusions that are still there with nothing.
     */
    @Test
    fun `test that a failed read leaves no model to save`() = runTest {
        stubReadFailure()

        val underTest = underTest()

        assertThat(underTest.uiState.value.loaded).isNull()
        assertThat(underTest.uiState.value.staged).isNull()
        assertThat(underTest.uiState.value.hasLoadFailed).isTrue()
        assertThat(underTest.uiState.value.isDirty).isFalse()
    }

    /**
     * Both states hold no model, and they call for opposite treatment: one is waited out, the
     * other is reported and retried.
     */
    @Test
    fun `test that a read still in flight is told apart from one that failed`() = runTest {
        val read = CompletableDeferred<Result<ExclusionsSnapshot>>()
        getSyncExclusionsUseCase.stub {
            onBlocking { invoke(any()) } doSuspendableAnswer { read.await() }
        }

        val underTest = underTest()

        assertThat(underTest.uiState.value.isLoading).isTrue()
        assertThat(underTest.uiState.value.hasLoadFailed).isFalse()

        read.complete(Result.failure(RuntimeException("no access")))

        assertThat(underTest.uiState.value.isLoading).isFalse()
        assertThat(underTest.uiState.value.hasLoadFailed).isTrue()
    }

    @Test
    fun `test that retrying rereads the file after a failure`() = runTest {
        stubReadFailure()
        val underTest = underTest()
        stubRead(threeRules)

        underTest.onRetryClicked()

        assertThat(underTest.uiState.value.staged).isEqualTo(threeRules)
        assertThat(underTest.uiState.value.hasLoadFailed).isFalse()
    }

    /**
     * Rereading over staged edits would drop them without the prompt that leaving the screen
     * gives, so a retry only applies to a read that failed.
     */
    @Test
    fun `test that retrying does nothing once the file has been read`() = runTest {
        stubRead(threeRules)
        val underTest = underTest()
        val added = rule(pattern = "New*")
        underTest.onRuleAdded(added)

        underTest.onRetryClicked()

        assertThat(underTest.uiState.value.staged?.rules).contains(added)
        assertThat(underTest.uiState.value.isDirty).isTrue()
    }

    @Test
    fun `test that a mutation before the file is read stages nothing`() = runTest {
        stubReadFailure()
        val underTest = underTest()

        underTest.onRuleAdded(rule(pattern = "New*"))
        underTest.onDefaultsRestored()
        underTest.onSizeBoundsChanged(SyncSizeExclusion(excludeLargerThanBytes = 1024))

        assertThat(underTest.uiState.value.staged).isNull()
        assertThat(underTest.uiState.value.isDirty).isFalse()
    }

    @Test
    fun `test that save writes nothing when the file was never read`() = runTest {
        stubReadFailure()
        val underTest = underTest()

        underTest.onSaveClicked()

        verifyNoInteractions(setSyncExclusionsUseCase)
    }

    @Test
    fun `test that adding a rule stages it without writing`() = runTest {
        stubRead(threeRules)
        val underTest = underTest()
        val added = rule(pattern = "New*")

        underTest.onRuleAdded(added)

        assertThat(underTest.uiState.value.staged?.rules).containsExactly(
            first, second, third, added,
        ).inOrder()
        assertThat(underTest.uiState.value.isDirty).isTrue()
        verifyNoInteractions(setSyncExclusionsUseCase)
    }

    @Test
    fun `test that editing a rule stages the edit without writing`() = runTest {
        stubRead(threeRules)
        val underTest = underTest()

        underTest.onRuleEdited(
            ruleIndex = 0,
            expected = first,
            edit = SyncExclusionEdit(pattern = "Renamed*"),
        )

        assertThat(underTest.uiState.value.staged?.rules?.first()?.pattern).isEqualTo("Renamed*")
        verifyNoInteractions(setSyncExclusionsUseCase)
    }

    @Test
    fun `test that a refused edit is reported and leaves the staged file alone`() = runTest {
        stubRead(threeRules)
        val underTest = underTest()

        underTest.onRuleRemoved(ruleIndex = 1, expected = rule(pattern = "Moved*"))

        assertThat(underTest.uiState.value.editError)
            .isInstanceOf(SyncExclusionEditError.StaleRuleTarget::class.java)
        assertThat(underTest.uiState.value.staged).isEqualTo(threeRules)
        assertThat(underTest.uiState.value.isDirty).isFalse()
    }

    @Test
    fun `test that a reported edit failure is cleared once shown`() = runTest {
        stubRead(threeRules)
        val underTest = underTest()
        underTest.onRuleRemoved(ruleIndex = 1, expected = rule(pattern = "Moved*"))

        underTest.onEditErrorShown()

        assertThat(underTest.uiState.value.editError).isNull()
    }

    /**
     * Dirtiness compares the whole file rather than counting edits, so a pair of edits that undo
     * each other leaves nothing to save and nothing to prompt about on the way out.
     */
    @Test
    fun `test that edits cancelling each other out leave nothing to save`() = runTest {
        stubRead(threeRules)
        val underTest = underTest()

        underTest.onRuleRemoved(ruleIndex = 2, expected = third)
        underTest.onRuleAdded(third)

        assertThat(underTest.uiState.value.staged).isEqualTo(threeRules)
        assertThat(underTest.uiState.value.isDirty).isFalse()
    }

    // region line order

    /**
     * The SDK applies the last rule that matches, so a staged edit has to hand back every other
     * line where it was. Asserted over [ParsedMegaIgnore.entries], which is the file: the `rules`
     * and `size` projections drop exactly the comments, includes and bounds a reordering would
     * disturb.
     */
    @Test
    fun `test that removing a rule keeps every other line where it was`() = runTest {
        stubRead(mixed)
        val underTest = underTest()

        underTest.onRuleRemoved(ruleIndex = 1, expected = second)

        assertThat(underTest.uiState.value.staged?.entries).containsExactly(
            comment,
            MegaIgnoreEntry.RuleEntry(first),
            largerBound,
            syncDirective,
            smallerBound,
            MegaIgnoreEntry.RuleEntry(third),
        ).inOrder()
        assertThat(underTest.uiState.value.staged?.hadBom).isTrue()
    }

    @Test
    fun `test that editing a rule keeps every other line where it was`() = runTest {
        stubRead(mixed)
        val underTest = underTest()

        underTest.onRuleEdited(
            ruleIndex = 1,
            expected = second,
            edit = SyncExclusionEdit(pattern = "Renamed*"),
        )

        assertThat(underTest.uiState.value.staged?.entries).containsExactly(
            comment,
            MegaIgnoreEntry.RuleEntry(first),
            largerBound,
            syncDirective,
            MegaIgnoreEntry.RuleEntry(second.copy(pattern = "Renamed*")),
            smallerBound,
            MegaIgnoreEntry.RuleEntry(third),
        ).inOrder()
    }

    @Test
    fun `test that restoring the defaults keeps the lines this app does not author`() = runTest {
        stubRead(mixed)
        val underTest = underTest()

        underTest.onDefaultsRestored()

        assertThat(underTest.uiState.value.staged?.entries).containsExactlyElementsIn(
            MegaIgnoreDefaults.RULES.map { MegaIgnoreEntry.RuleEntry(it) } +
                    listOf(comment, largerBound, syncDirective, smallerBound)
        ).inOrder()
    }

    /**
     * The bounds go back where the file already kept them rather than at the end, because a bound
     * line that moves past one this app cannot model changes which of them the SDK holds.
     */
    @Test
    fun `test that changing the size bounds puts them back where the file kept them`() = runTest {
        stubRead(mixed)
        val underTest = underTest()

        underTest.onSizeBoundsChanged(
            SyncSizeExclusion(excludeSmallerThanBytes = 1024, excludeLargerThanBytes = 2048),
        )

        assertThat(underTest.uiState.value.staged?.entries).containsExactly(
            comment,
            MegaIgnoreEntry.RuleEntry(first),
            MegaIgnoreEntry.SizeSmallerEntry(1024),
            MegaIgnoreEntry.SizeLargerEntry(2048),
            syncDirective,
            MegaIgnoreEntry.RuleEntry(second),
            MegaIgnoreEntry.RuleEntry(third),
        ).inOrder()
    }

    @Test
    fun `test that adding a rule keeps the lines this app does not model`() = runTest {
        stubRead(mixed)
        val underTest = underTest()

        underTest.onRuleAdded(rule(pattern = "New*"))

        assertThat(underTest.uiState.value.staged?.entries?.take(4)).containsExactly(
            comment,
            MegaIgnoreEntry.RuleEntry(first),
            largerBound,
            syncDirective,
        ).inOrder()
        assertThat(underTest.uiState.value.staged?.preservedLines)
            .containsExactlyElementsIn(mixed.preservedLines).inOrder()
    }

    // endregion

    // region saving

    @Test
    fun `test that a read-only root cannot save even with changes staged`() = runTest {
        stubRead(threeRules, isWritable = false)
        val underTest = underTest()

        underTest.onRuleAdded(rule(pattern = "New*"))

        assertThat(underTest.uiState.value.isDirty).isTrue()
        assertThat(underTest.uiState.value.canSave).isFalse()
    }

    @Test
    fun `test that a read-only root writes nothing when save is asked for`() = runTest {
        stubRead(threeRules, isWritable = false)
        val underTest = underTest()
        underTest.onRuleAdded(rule(pattern = "New*"))

        underTest.onSaveClicked()

        verifyNoInteractions(setSyncExclusionsUseCase)
    }

    @Test
    fun `test that save writes the staged file once as it stands`() = runTest {
        stubRead(threeRules)
        stubWrite(Result.success(Unit))
        val underTest = underTest()
        val added = rule(pattern = "New*")
        underTest.onRuleAdded(added)
        val staged = underTest.uiState.value.staged

        underTest.onSaveClicked()

        verify(setSyncExclusionsUseCase).invoke(SYNC_ID, LOCAL_ROOT, checkNotNull(staged))
    }

    @Test
    fun `test that a save stays in flight until the write returns`() = runTest {
        stubRead(threeRules)
        val write = CompletableDeferred<Result<Unit>>()
        stubSuspendingWrite(write)
        val underTest = underTest()
        underTest.onRuleAdded(rule(pattern = "New*"))

        underTest.onSaveClicked()

        assertThat(underTest.uiState.value.isSaving).isTrue()

        write.complete(Result.success(Unit))

        assertThat(underTest.uiState.value.isSaving).isFalse()
    }

    @Test
    fun `test that asking to save again while the write is in flight writes once`() = runTest {
        stubRead(threeRules)
        val write = CompletableDeferred<Result<Unit>>()
        stubSuspendingWrite(write)
        val underTest = underTest()
        underTest.onRuleAdded(rule(pattern = "New*"))

        underTest.onSaveClicked()
        underTest.onSaveClicked()
        write.complete(Result.success(Unit))

        verify(setSyncExclusionsUseCase, times(1)).invoke(any(), any(), any())
    }

    /**
     * The baseline a save leaves behind is the model that was written, not whatever is staged when
     * it returns — taking the latter would mark an edit made during the write as already saved,
     * and the next discard would drop it.
     */
    @Test
    fun `test that an edit staged during a save is not marked as written`() = runTest {
        stubRead(threeRules)
        val write = CompletableDeferred<Result<Unit>>()
        stubSuspendingWrite(write)
        val underTest = underTest()
        underTest.onRuleAdded(rule(pattern = "Saved*"))
        val written = underTest.uiState.value.staged
        underTest.onSaveClicked()

        underTest.onRuleAdded(rule(pattern = "Later*"))
        write.complete(Result.success(Unit))

        assertThat(underTest.uiState.value.loaded).isEqualTo(written)
        assertThat(underTest.uiState.value.isDirty).isTrue()
    }

    @Test
    fun `test that a saved file becomes the baseline the next edit is measured against`() =
        runTest {
            stubRead(threeRules)
            stubWrite(Result.success(Unit))
            val underTest = underTest()
            underTest.onRuleAdded(rule(pattern = "New*"))

            underTest.onSaveClicked()

            assertThat(underTest.uiState.value.isDirty).isFalse()
            assertThat(underTest.uiState.value.loaded).isEqualTo(underTest.uiState.value.staged)
            assertThat(underTest.uiState.value.isSaving).isFalse()
        }

    @Test
    fun `test that a failed save keeps the edits so they can be retried`() = runTest {
        stubRead(threeRules)
        stubWrite(Result.failure(RuntimeException("SAF write refused")))
        val underTest = underTest()
        val added = rule(pattern = "New*")
        underTest.onRuleAdded(added)

        underTest.onSaveClicked()

        assertThat(underTest.uiState.value.hasSaveFailed).isTrue()
        assertThat(underTest.uiState.value.staged?.rules).contains(added)
        assertThat(underTest.uiState.value.isDirty).isTrue()
        assertThat(underTest.uiState.value.isSaving).isFalse()
    }

    @Test
    fun `test that save writes nothing when nothing was changed`() = runTest {
        stubRead(threeRules)
        val underTest = underTest()

        underTest.onSaveClicked()

        verifyNoInteractions(setSyncExclusionsUseCase)
    }

    @Test
    fun `test that a reported save failure is cleared once shown`() = runTest {
        stubRead(threeRules)
        stubWrite(Result.failure(RuntimeException("SAF write refused")))
        val underTest = underTest()
        underTest.onRuleAdded(rule(pattern = "New*"))
        underTest.onSaveClicked()

        underTest.onSaveFailureShown()

        assertThat(underTest.uiState.value.hasSaveFailed).isFalse()
    }

    // endregion

    // region leaving the screen

    @Test
    fun `test that back leaves straight away when nothing was changed`() = runTest {
        stubRead(threeRules)
        val underTest = underTest()

        underTest.onBackPressed()

        assertThat(underTest.uiState.value.isDiscardPromptShown).isFalse()
        assertThat(underTest.uiState.value.isFinished).isTrue()
    }

    @Test
    fun `test that back asks before losing unsaved changes`() = runTest {
        stubRead(threeRules)
        val underTest = underTest()
        underTest.onRuleAdded(rule(pattern = "New*"))

        underTest.onBackPressed()

        assertThat(underTest.uiState.value.isDiscardPromptShown).isTrue()
        assertThat(underTest.uiState.value.isFinished).isFalse()
    }

    /**
     * The write outlives the screen, so the edits are not the user's to lose at that point and
     * offering to discard them would describe the opposite of what happens.
     */
    @Test
    fun `test that back while saving leaves without offering to discard the write`() = runTest {
        stubRead(threeRules)
        val write = CompletableDeferred<Result<Unit>>()
        stubSuspendingWrite(write)
        val underTest = underTest()
        underTest.onRuleAdded(rule(pattern = "New*"))
        underTest.onSaveClicked()

        underTest.onBackPressed()

        assertThat(underTest.uiState.value.isDiscardPromptShown).isFalse()
        assertThat(underTest.uiState.value.isFinished).isTrue()

        write.complete(Result.success(Unit))

        assertThat(underTest.uiState.value.isDirty).isFalse()
    }

    @Test
    fun `test that confirming the discard returns to the file as it was read`() = runTest {
        stubRead(threeRules)
        val underTest = underTest()
        underTest.onRuleAdded(rule(pattern = "New*"))
        underTest.onBackPressed()

        underTest.onDiscardConfirmed()

        assertThat(underTest.uiState.value.staged).isEqualTo(threeRules)
        assertThat(underTest.uiState.value.isDiscardPromptShown).isFalse()
        assertThat(underTest.uiState.value.isFinished).isTrue()
        verifyNoInteractions(setSyncExclusionsUseCase)
    }

    @Test
    fun `test that a discard that was never asked for does not drop the edits`() = runTest {
        stubRead(threeRules)
        val underTest = underTest()
        val added = rule(pattern = "New*")
        underTest.onRuleAdded(added)

        underTest.onDiscardConfirmed()

        assertThat(underTest.uiState.value.staged?.rules).contains(added)
        assertThat(underTest.uiState.value.isFinished).isFalse()
    }

    @Test
    fun `test that dismissing the discard prompt keeps the unsaved changes`() = runTest {
        stubRead(threeRules)
        val underTest = underTest()
        val added = rule(pattern = "New*")
        underTest.onRuleAdded(added)
        underTest.onBackPressed()

        underTest.onDiscardDismissed()

        assertThat(underTest.uiState.value.staged?.rules).contains(added)
        assertThat(underTest.uiState.value.isDiscardPromptShown).isFalse()
        assertThat(underTest.uiState.value.isFinished).isFalse()
    }

    // endregion

    private fun TestScope.underTest() = ManageSyncExclusionsViewModel(
        args = ManageSyncExclusionsViewModel.Args(syncId = SYNC_ID, localRoot = LOCAL_ROOT),
        applicationScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        getSyncExclusionsUseCase = getSyncExclusionsUseCase,
        setSyncExclusionsUseCase = setSyncExclusionsUseCase,
        addSyncExclusionUseCase = AddSyncExclusionUseCase(),
        replaceSyncExclusionUseCase = ReplaceSyncExclusionUseCase(),
        removeSyncExclusionUseCase = RemoveSyncExclusionUseCase(),
        restoreDefaultSyncExclusionsUseCase = RestoreDefaultSyncExclusionsUseCase(),
        setSyncSizeBoundsUseCase = SetSyncSizeBoundsUseCase(),
    )

    private fun stubRead(exclusions: ParsedMegaIgnore, isWritable: Boolean = true) {
        getSyncExclusionsUseCase.stub {
            onBlocking { invoke(any()) } doReturn Result.success(
                ExclusionsSnapshot(exclusions = exclusions, isWritable = isWritable)
            )
        }
    }

    private fun stubReadFailure() {
        getSyncExclusionsUseCase.stub {
            onBlocking { invoke(any()) } doReturn Result.failure(RuntimeException("no access"))
        }
    }

    private fun stubWrite(result: Result<Unit>) {
        setSyncExclusionsUseCase.stub {
            onBlocking { invoke(any(), any(), any()) } doReturn result
        }
    }

    private fun stubSuspendingWrite(write: CompletableDeferred<Result<Unit>>) {
        setSyncExclusionsUseCase.stub {
            onBlocking { invoke(any(), any(), any()) } doSuspendableAnswer { write.await() }
        }
    }

    private val first = rule(pattern = "First*")
    private val second = rule(pattern = "Second*")
    private val third = rule(pattern = "Third*")

    private val threeRules = ParsedMegaIgnore(
        entries = listOf(
            MegaIgnoreEntry.RuleEntry(first),
            MegaIgnoreEntry.RuleEntry(second),
            MegaIgnoreEntry.RuleEntry(third),
        )
    )

    private val comment = MegaIgnoreEntry.PreservedEntry("# written by another client")
    private val syncDirective = MegaIgnoreEntry.PreservedEntry("+sync:.megaignore")
    private val largerBound = MegaIgnoreEntry.SizeLargerEntry(102400L)
    private val smallerBound = MegaIgnoreEntry.SizeSmallerEntry(1024L)

    private val mixed = ParsedMegaIgnore(
        entries = listOf(
            comment,
            MegaIgnoreEntry.RuleEntry(first),
            largerBound,
            syncDirective,
            MegaIgnoreEntry.RuleEntry(second),
            smallerBound,
            MegaIgnoreEntry.RuleEntry(third),
        ),
        hadBom = true,
    )

    private fun rule(
        pattern: String,
        exclusionClass: SyncExclusionClass = SyncExclusionClass.Exclude,
        target: SyncExclusionTarget = SyncExclusionTarget.All,
        matchType: SyncExclusionMatchType = SyncExclusionMatchType.SubtreeName,
        strategy: SyncMatchStrategy = SyncMatchStrategy.GlobCaseInsensitive,
        enabled: Boolean = true,
    ) = SyncExclusionRule(
        pattern = pattern,
        exclusionClass = exclusionClass,
        target = target,
        matchType = matchType,
        strategy = strategy,
        enabled = enabled,
    )

    private companion object {
        const val SYNC_ID = 42L
        val LOCAL_ROOT = UriPath("content://sync/root")
    }
}
