package mega.privacy.android.app;

import android.content.Context;
import android.os.StatFs;

import java.io.File;
import java.util.List;

import mega.privacy.android.app.jobservices.SyncRecord;
import mega.privacy.android.app.utils.conversion.VideoCompressionCallback;

import static mega.privacy.android.app.utils.LogUtil.*;

public class VideoCompressor extends VideoDownsampling{

    private List<SyncRecord> pendingList;

    private String outputRoot;

    private VideoCompressionCallback updater;

    private long totalInputSize;

    private long totalRead;

    private int totalCount;

    private int currentFileIndex;

    public void stop() {
        setRunning(false);
        logDebug("Video compressor stopped");
    }

    public VideoCompressor(Context context, VideoCompressionCallback callback) {
        super(context);
        this.updater = callback;
    }

    public void setPendingList(List<SyncRecord> pendingList) {
        this.pendingList = pendingList;
        totalCount = pendingList.size();
        logDebug("Total compression videos count is " + totalCount);
        calculateTotalSize();
    }

    private void calculateTotalSize() {
        for (int i = 0; i < totalCount; i++) {
            totalInputSize += new File(pendingList.get(i).getLocalPath()).length();
        }
        logDebug("Total compression size is " + totalInputSize);
    }

    public long getTotalInputSize() {
        return this.totalInputSize;
    }

    public void setOutputRoot(String root) {
        this.outputRoot = root;
    }

    private boolean notEnoughSpace(long size) {
        double availableFreeSpace = Double.MAX_VALUE;
        try {
            StatFs stat = new StatFs(outputRoot);
            availableFreeSpace = stat.getAvailableBytes();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return (size > availableFreeSpace);
    }

    public void start() {
        setRunning(true);
        for (int i = 0; i < totalCount && isRunning(); i++) {
            currentFileIndex = i + 1;
            SyncRecord record = pendingList.get(i);
            logDebug("Video compressor start: " + record.toString());
            String path = record.getLocalPath();
            File src = new File(path);
            long size = src.length();
            if (notEnoughSpace(size)) {
                updater.onInsufficientSpace();
                return;
            }

            VideoUpload video = new VideoUpload(path, record.getNewPath(), size, -1);
            try {
                prepareAndChangeResolution(video);
                if (isRunning()) {
                    updater.onCompressSuccessful(record);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                logError("Exception", ex);
                updater.onCompressFailed(record);
                currentFileIndex++;
                totalRead += size;
            }
        }
        updater.onCompressFinished(totalCount + "/" + totalCount);
        stop();
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getCurrentFileIndex() {
        return currentFileIndex;
    }

    public long getTotalRead() {
        return totalRead;
    }
}
