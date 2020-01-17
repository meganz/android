package mega.privacy.android.app.utils.download;

public class DownloadInfo {

    private boolean highPriority;

    private long size;

    private long[] hashes;

    public DownloadInfo(boolean highPriority, long size, long[] hashes) {
        this.highPriority = highPriority;
        this.size = size;
        this.hashes = hashes;
    }

    public boolean isHighPriority() {
        return highPriority;
    }

    public void setHighPriority(boolean highPriority) {
        this.highPriority = highPriority;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long[] getHashes() {
        return hashes;
    }

    public void setHashes(long[] hashes) {
        this.hashes = hashes;
    }
}
