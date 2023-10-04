package mega.privacy.android.feature.sync.data.mock;

// Mock class to avoid dependency on SDK SRW branch
public class MegaSyncStallList {

    private transient long swigCPtr;
    protected transient boolean swigCMemOwn;

    protected MegaSyncStallList(long cPtr, boolean cMemoryOwn) {
        swigCMemOwn = cMemoryOwn;
        swigCPtr = cPtr;
    }

    protected static long getCPtr(MegaSyncStallList obj) {
        return 1;
    }

    protected static long swigRelease(MegaSyncStallList obj) {
        return 1;
    }

    @SuppressWarnings("deprecation")
    protected void finalize() {
        delete();
    }

    protected synchronized void delete() {
        if (swigCPtr != 0) {
            if (swigCMemOwn) {
                swigCMemOwn = false;
            }
            swigCPtr = 0;
        }
    }

    public MegaSyncStallList() {

    }

    MegaSyncStallList copy() {
        return new MegaSyncStallList();
    }

    public MegaSyncStall get(long index) {
        return new MegaSyncStall();
    }

    public long size() {
        return 1;
    }
}
