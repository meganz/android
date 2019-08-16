package mega.privacy.android.app.utils;

import java.util.Arrays;

public class DownloadInfo {

    private String url;

    private boolean highPriority;

    private long size;

    private long[] hashes;

    public DownloadInfo(String url, boolean highPriority, long size, long[] hashes) {
        this.url = url;
        this.highPriority = highPriority;
        this.size = size;
        this.hashes = hashes;
    }

    @Override
    public String toString() {
        return "DownloadInfo{" +
                "url='" + url + '\'' +
                ", highPriority=" + highPriority +
                ", size=" + size +
                ", hashes=" + Arrays.toString(hashes) +
                '}';
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
