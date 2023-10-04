package mega.privacy.android.feature.sync.data.mock;


// Mock class to avoid dependency on SDK SRW branch
public class MegaSyncStall {

    protected static long getCPtr(MegaSyncStall obj) {
        return 1;
    }

    protected static long swigRelease(MegaSyncStall obj) {
        return 1;
    }

    @SuppressWarnings("deprecation")
    protected void finalize() {
        delete();
    }

    protected synchronized void delete() {
    }

    MegaSyncStall copy() {
        return new MegaSyncStall();
    }

    public MegaSyncStall.SyncStallReason reason() {
        return MegaSyncStall.SyncStallReason.DeleteWaitingOnMoves;
    }

    public String reasonDebugString() {
        return "BothChangedSinceLastWeek";
    }

    public String path(boolean cloudSide, int index) {
        return "/folder12/file.txt";
    }

    public long cloudNodeHandle(int index) {
        return 1323132;
    }

    public long pathCount(boolean cloudSide) {
        return 1;
    }

    public int pathProblem(boolean cloudSide, int index) {
        return 1;
    }

    public boolean couldSuggestIgnoreThisPath(boolean cloudSide, int index) {
        return true;
    }

    public boolean detectedCloudSide() {
        return true;
    }

    public final static class SyncStallReason {
        public final static MegaSyncStall.SyncStallReason NoReason = new SyncStallReason("NoReason", 0);
        public final static MegaSyncStall.SyncStallReason FileIssue = new SyncStallReason("FileIssue");
        public final static MegaSyncStall.SyncStallReason MoveOrRenameCannotOccur = new SyncStallReason("MoveOrRenameCannotOccur");
        public final static MegaSyncStall.SyncStallReason DeleteOrMoveWaitingOnScanning = new SyncStallReason("DeleteOrMoveWaitingOnScanning");
        public final static MegaSyncStall.SyncStallReason DeleteWaitingOnMoves = new SyncStallReason("DeleteWaitingOnMoves");
        public final static MegaSyncStall.SyncStallReason UploadIssue = new SyncStallReason("UploadIssue");
        public final static MegaSyncStall.SyncStallReason DownloadIssue = new SyncStallReason("DownloadIssue");
        public final static MegaSyncStall.SyncStallReason CannotCreateFolder = new SyncStallReason("CannotCreateFolder");
        public final static MegaSyncStall.SyncStallReason CannotPerformDeletion = new SyncStallReason("CannotPerformDeletion");
        public final static MegaSyncStall.SyncStallReason SyncItemExceedsSupportedTreeDepth = new SyncStallReason("SyncItemExceedsSupportedTreeDepth");
        public final static MegaSyncStall.SyncStallReason FolderMatchedAgainstFile = new SyncStallReason("FolderMatchedAgainstFile");
        public final static MegaSyncStall.SyncStallReason LocalAndRemoteChangedSinceLastSyncedState_userMustChoose = new SyncStallReason("LocalAndRemoteChangedSinceLastSyncedState_userMustChoose");
        public final static MegaSyncStall.SyncStallReason LocalAndRemotePreviouslyUnsyncedDiffer_userMustChoose = new SyncStallReason("LocalAndRemotePreviouslyUnsyncedDiffer_userMustChoose");
        public final static MegaSyncStall.SyncStallReason NamesWouldClashWhenSynced = new SyncStallReason("NamesWouldClashWhenSynced");
        public final static MegaSyncStall.SyncStallReason SyncStallReason_LastPlusOne = new SyncStallReason("SyncStallReason_LastPlusOne");

        public final int swigValue() {
            return swigValue;
        }

        public String toString() {
            return swigName;
        }

        public static MegaSyncStall.SyncStallReason swigToEnum(int swigValue) {
            return FolderMatchedAgainstFile;
        }

        private SyncStallReason(String swigName) {
            this.swigName = swigName;
            this.swigValue = swigNext++;
        }

        private SyncStallReason(String swigName, int swigValue) {
            this.swigName = swigName;
            this.swigValue = swigValue;
            swigNext = swigValue+1;
        }

        private SyncStallReason(String swigName, MegaSyncStall.SyncStallReason swigEnum) {
            this.swigName = swigName;
            this.swigValue = swigEnum.swigValue();
            swigNext = this.swigValue+1;
        }

        private static MegaSyncStall.SyncStallReason[] swigValues = { NoReason, FileIssue, MoveOrRenameCannotOccur, DeleteOrMoveWaitingOnScanning, DeleteWaitingOnMoves, UploadIssue, DownloadIssue, CannotCreateFolder, CannotPerformDeletion, SyncItemExceedsSupportedTreeDepth, FolderMatchedAgainstFile, LocalAndRemoteChangedSinceLastSyncedState_userMustChoose, LocalAndRemotePreviouslyUnsyncedDiffer_userMustChoose, NamesWouldClashWhenSynced, SyncStallReason_LastPlusOne };
        private static int swigNext = 0;
        private final int swigValue;
        private final String swigName;
    }

    public final static class SyncPathProblem {
        public final static MegaSyncStall.SyncPathProblem NoProblem = new MegaSyncStall.SyncPathProblem("NoProblem", 0);
        public final static MegaSyncStall.SyncPathProblem FileChangingFrequently = new MegaSyncStall.SyncPathProblem("FileChangingFrequently");
        public final static MegaSyncStall.SyncPathProblem IgnoreRulesUnknown = new MegaSyncStall.SyncPathProblem("IgnoreRulesUnknown");
        public final static MegaSyncStall.SyncPathProblem DetectedHardLink = new MegaSyncStall.SyncPathProblem("DetectedHardLink");
        public final static MegaSyncStall.SyncPathProblem DetectedSymlink = new MegaSyncStall.SyncPathProblem("DetectedSymlink");
        public final static MegaSyncStall.SyncPathProblem DetectedSpecialFile = new MegaSyncStall.SyncPathProblem("DetectedSpecialFile");
        public final static MegaSyncStall.SyncPathProblem DifferentFileOrFolderIsAlreadyPresent = new MegaSyncStall.SyncPathProblem("DifferentFileOrFolderIsAlreadyPresent");
        public final static MegaSyncStall.SyncPathProblem ParentFolderDoesNotExist = new MegaSyncStall.SyncPathProblem("ParentFolderDoesNotExist");
        public final static MegaSyncStall.SyncPathProblem FilesystemErrorDuringOperation = new MegaSyncStall.SyncPathProblem("FilesystemErrorDuringOperation");
        public final static MegaSyncStall.SyncPathProblem NameTooLongForFilesystem = new MegaSyncStall.SyncPathProblem("NameTooLongForFilesystem");
        public final static MegaSyncStall.SyncPathProblem CannotFingerprintFile = new MegaSyncStall.SyncPathProblem("CannotFingerprintFile");
        public final static MegaSyncStall.SyncPathProblem DestinationPathInUnresolvedArea = new MegaSyncStall.SyncPathProblem("DestinationPathInUnresolvedArea");
        public final static MegaSyncStall.SyncPathProblem MACVerificationFailure = new MegaSyncStall.SyncPathProblem("MACVerificationFailure");
        public final static MegaSyncStall.SyncPathProblem DeletedOrMovedByUser = new MegaSyncStall.SyncPathProblem("DeletedOrMovedByUser");
        public final static MegaSyncStall.SyncPathProblem FileFolderDeletedByUser = new MegaSyncStall.SyncPathProblem("FileFolderDeletedByUser");
        public final static MegaSyncStall.SyncPathProblem MoveToDebrisFolderFailed = new MegaSyncStall.SyncPathProblem("MoveToDebrisFolderFailed");
        public final static MegaSyncStall.SyncPathProblem IgnoreFileMalformed = new MegaSyncStall.SyncPathProblem("IgnoreFileMalformed");
        public final static MegaSyncStall.SyncPathProblem FilesystemErrorListingFolder = new MegaSyncStall.SyncPathProblem("FilesystemErrorListingFolder");
        public final static MegaSyncStall.SyncPathProblem FilesystemErrorIdentifyingFolderContent = new MegaSyncStall.SyncPathProblem("FilesystemErrorIdentifyingFolderContent");
        public final static MegaSyncStall.SyncPathProblem UndecryptedCloudNode = new MegaSyncStall.SyncPathProblem("UndecryptedCloudNode");
        public final static MegaSyncStall.SyncPathProblem WaitingForScanningToComplete = new MegaSyncStall.SyncPathProblem("WaitingForScanningToComplete");
        public final static MegaSyncStall.SyncPathProblem WaitingForAnotherMoveToComplete = new MegaSyncStall.SyncPathProblem("WaitingForAnotherMoveToComplete");
        public final static MegaSyncStall.SyncPathProblem SourceWasMovedElsewhere = new MegaSyncStall.SyncPathProblem("SourceWasMovedElsewhere");
        public final static MegaSyncStall.SyncPathProblem FilesystemCannotStoreThisName = new MegaSyncStall.SyncPathProblem("FilesystemCannotStoreThisName");
        public final static MegaSyncStall.SyncPathProblem CloudNodeInvalidFingerprint = new MegaSyncStall.SyncPathProblem("CloudNodeInvalidFingerprint");
        public final static MegaSyncStall.SyncPathProblem SyncPathProblem_LastPlusOne = new MegaSyncStall.SyncPathProblem("SyncPathProblem_LastPlusOne");

        public final int swigValue() {
            return swigValue;
        }

        public String toString() {
            return swigName;
        }

        public static MegaSyncStall.SyncPathProblem swigToEnum(int swigValue) {
            if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue() == swigValue)
                return swigValues[swigValue];
            for (int i = 0; i < swigValues.length; i++)
                if (swigValues[i].swigValue() == swigValue)
                    return swigValues[i];
            throw new IllegalArgumentException("No enum " + MegaSyncStall.SyncPathProblem.class + " with value " + swigValue);
        }

        private SyncPathProblem(String swigName) {
            this.swigName = swigName;
            this.swigValue = swigNext++;
        }

        private SyncPathProblem(String swigName, int swigValue) {
            this.swigName = swigName;
            this.swigValue = swigValue;
            swigNext = swigValue+1;
        }

        private SyncPathProblem(String swigName, MegaSyncStall.SyncPathProblem swigEnum) {
            this.swigName = swigName;
            this.swigValue = swigEnum.swigValue();
            swigNext = this.swigValue+1;
        }

        private static MegaSyncStall.SyncPathProblem[] swigValues = { NoProblem, FileChangingFrequently, IgnoreRulesUnknown, DetectedHardLink, DetectedSymlink, DetectedSpecialFile, DifferentFileOrFolderIsAlreadyPresent, ParentFolderDoesNotExist, FilesystemErrorDuringOperation, NameTooLongForFilesystem, CannotFingerprintFile, DestinationPathInUnresolvedArea, MACVerificationFailure, DeletedOrMovedByUser, FileFolderDeletedByUser, MoveToDebrisFolderFailed, IgnoreFileMalformed, FilesystemErrorListingFolder, FilesystemErrorIdentifyingFolderContent, UndecryptedCloudNode, WaitingForScanningToComplete, WaitingForAnotherMoveToComplete, SourceWasMovedElsewhere, FilesystemCannotStoreThisName, CloudNodeInvalidFingerprint, SyncPathProblem_LastPlusOne };
        private static int swigNext = 0;
        private final int swigValue;
        private final String swigName;
    }
}
