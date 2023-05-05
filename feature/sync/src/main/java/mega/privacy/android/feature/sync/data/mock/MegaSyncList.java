package mega.privacy.android.feature.sync.data.mock;

/*
 * Mock class to avoid dependency on real Sync SDK
 */
public class MegaSyncList {

    public static MegaSyncList createInstance() {
        return  new MegaSyncList();
    }

    public MegaSync get(int i) {
        return new MegaSync();
    }

    public int size() {
        return 1;
    }
}
