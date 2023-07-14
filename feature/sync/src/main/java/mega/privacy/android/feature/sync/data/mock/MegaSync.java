package mega.privacy.android.feature.sync.data.mock;

import static mega.privacy.android.feature.sync.data.mock.MegaSync.SyncRunningState.RUNSTATE_RUNNING;

/*
 * Mock class to avoid dependency on real Sync SDK
 * This class will be removed in next MR since real Sync SDK is now added
 */
public class MegaSync {

    public long getMegaHandle() {
        return 1234;
    }

    public String getLocalFolder() {
        return "folderName";
    }

    public String getName() {
        return "syncName";
    }

    public long getBackupId() {
        return 1234;
    }

    public int getError() {
        return 1234;
    }

    public int getWarning() {
        return 1234;
    }

    public int getType() {
        return SyncType.TYPE_TWOWAY.swigValue;
    }

    public int getRunState() {
        return RUNSTATE_RUNNING.swigValue;
    }

    public final static class SyncType {
        public final static MegaSync.SyncType TYPE_UNKNOWN = new MegaSync.SyncType("TYPE_UNKNOWN", 0);
        public final static MegaSync.SyncType TYPE_TWOWAY = new MegaSync.SyncType("TYPE_TWOWAY", 3);
        public final static MegaSync.SyncType TYPE_BACKUP = new MegaSync.SyncType("TYPE_BACKUP");

        public String toString() {
            return swigName;
        }

        private SyncType(String swigName) {
            this.swigName = swigName;
            this.swigValue = swigNext++;
        }

        private SyncType(String swigName, int swigValue) {
            this.swigName = swigName;
            this.swigValue = swigValue;
            swigNext = swigValue+1;
        }

        private static SyncType[] swigValues = { TYPE_UNKNOWN, TYPE_TWOWAY, TYPE_BACKUP };
        private static int swigNext = 0;
        private final int swigValue;
        private final String swigName;
    }

    public final static class SyncRunningState {
        public final static MegaSync.SyncRunningState RUNSTATE_PENDING = new MegaSync.SyncRunningState("RUNSTATE_PENDING");
        public final static MegaSync.SyncRunningState RUNSTATE_LOADING = new MegaSync.SyncRunningState("RUNSTATE_LOADING");
        public final static MegaSync.SyncRunningState RUNSTATE_RUNNING = new MegaSync.SyncRunningState("RUNSTATE_RUNNING");
        public final static MegaSync.SyncRunningState RUNSTATE_PAUSED = new MegaSync.SyncRunningState("RUNSTATE_PAUSED");
        public final static MegaSync.SyncRunningState RUNSTATE_SUSPENDED = new MegaSync.SyncRunningState("RUNSTATE_SUSPENDED");
        public final static MegaSync.SyncRunningState RUNSTATE_DISABLED = new MegaSync.SyncRunningState("RUNSTATE_DISABLED");

        public String toString() {
            return swigName;
        }

        private SyncRunningState(String swigName) {
            this.swigName = swigName;
            this.swigValue = swigNext++;
        }

        private static int swigNext = 0;
        private final int swigValue;
        private final String swigName;
    }
}
