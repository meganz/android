package mega.privacy.android.app.fragments.managerFragments.cu;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Pair;
import androidx.collection.LongSparseArray;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.arch.BaseRxViewModel;
import mega.privacy.android.app.listeners.BaseListener;
import mega.privacy.android.app.repo.MegaNodeRepo;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.MegaPreferences.MEDIUM;
import static mega.privacy.android.app.constants.SettingsConstants.DEFAULT_CONVENTION_QUEUE_SIZE;
import static mega.privacy.android.app.utils.FileUtils.buildDefaultDownloadDir;
import static mega.privacy.android.app.utils.FileUtils.isVideoFile;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.RxUtil.ignore;
import static mega.privacy.android.app.utils.RxUtil.logErr;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getThumbFolder;
import static mega.privacy.android.app.utils.Util.fromEpoch;

class CuViewModel extends BaseRxViewModel {
    private final MegaApiAndroid mMegaApi;
    private final DatabaseHandler mDbHandler;
    private final int mType;
    private final MegaNodeRepo mRepo;

    private final MutableLiveData<List<CuNode>> mCuNodes = new MutableLiveData<>();
    private final MutableLiveData<Pair<Integer, CuNode>> mNodeToOpen = new MutableLiveData<>();
    private final MutableLiveData<Pair<Integer, CuNode>> mNodeToAnimate = new MutableLiveData<>();
    private final MutableLiveData<String> mActionBarTitle = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mActionMode = new MutableLiveData<>();

    private final Subject<Pair<Integer, CuNode>> mOpenNodeAction = PublishSubject.create();
    private final Subject<Object> mCreatingThumbnailFinished = PublishSubject.create();

    private final MegaRequestListenerInterface mCreateThumbnailRequest =
            new BaseListener(getApplication()) {
                @Override
                public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
                    if (e.getErrorCode() == MegaError.API_OK) {
                        mCreatingThumbnailFinished.onNext(true);
                    }
                }
            };

    private boolean mSelecting;
    private final LongSparseArray<MegaNode> mSelectedNodes = new LongSparseArray<>(5);

    private long[] mSearchDate;
    private int mRealNodeCount;

    public CuViewModel(MegaApiAndroid megaApi, DatabaseHandler dbHandler, int type) {
        mMegaApi = megaApi;
        mDbHandler = dbHandler;
        mType = type;
        mRepo = new MegaNodeRepo(megaApi, dbHandler, getApplication());

        loadCuNodes();

        add(mOpenNodeAction.throttleFirst(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mNodeToOpen::setValue, logErr("openNodeAction")));

        add(mCreatingThumbnailFinished.throttleLatest(1, TimeUnit.SECONDS, true)
                .subscribe(ignored -> loadCuNodes(), logErr("creatingThumbnailFinished")));
    }

    public LiveData<List<CuNode>> cuNodes() {
        return mCuNodes;
    }

    public LiveData<Pair<Integer, CuNode>> nodeToOpen() {
        return mNodeToOpen;
    }

    public LiveData<Pair<Integer, CuNode>> nodeToAnimate() {
        return mNodeToAnimate;
    }

    public LiveData<String> actionBarTitle() {
        return mActionBarTitle;
    }

    public LiveData<Boolean> actionMode() {
        return mActionMode;
    }

    public void loadCuNodes(int orderBy) {
        loadCuNodes(Single.defer(() -> Single.just(getCuNodes(orderBy))));
    }

    public void setSearchDate(long[] searchDate, int orderBy) {
        this.mSearchDate = searchDate;
        loadCuNodes(orderBy);
    }

    public boolean isSearchMode() {
        return mSearchDate != null;
    }

    public long[] getSearchResultNodeHandles() {
        List<CuNode> nodes = mCuNodes.getValue();
        if (!isSearchMode() || nodes == null || nodes.isEmpty()) {
            return new long[0];
        }

        List<Long> handleList = new ArrayList<>();
        for (CuNode node : nodes) {
            if (node.getNode() != null) {
                handleList.add(node.getNode().getHandle());
            }
        }

        long[] handles = new long[handleList.size()];
        for (int i = 0, n = handleList.size(); i < n; i++) {
            handles[i] = handleList.get(i);
        }
        return handles;
    }

    public boolean isSelecting() {
        return mSelecting;
    }

    /**
     * Handle node click & long click event.
     *
     * In selection mode, we need need animate the selection icon, so we don't
     * trigger nodes update through {@code cuNodes.setValue(nodes); }, we only
     * update node's selected property here, for consistency.
     *
     * @param position clicked node position in RV
     * @param node clicked node
     */
    public void onNodeClicked(int position, CuNode node) {
        if (mSelecting) {
            List<CuNode> nodes = mCuNodes.getValue();
            if (nodes == null || position < 0 || position >= nodes.size()
                    || nodes.get(position).getNode() == null
                    || nodes.get(position).getNode().getHandle() != node.getNode().getHandle()) {
                return;
            }

            nodes.get(position).setSelected(!nodes.get(position).isSelected());
            if (nodes.get(position).isSelected()) {
                mSelectedNodes.put(node.getNode().getHandle(), node.getNode());
            } else {
                mSelectedNodes.remove(node.getNode().getHandle());
            }
            mSelecting = !mSelectedNodes.isEmpty();
            mActionMode.setValue(mSelecting);

            mNodeToAnimate.setValue(Pair.create(position, node));
        } else {
            mOpenNodeAction.onNext(Pair.create(position, node));
        }
    }

    public void onNodeLongClicked(int position, CuNode node) {
        mSelecting = true;
        onNodeClicked(position, node);
    }

    public List<MegaNode> getSelectedNodes() {
        List<MegaNode> nodes = new ArrayList<>();
        for (int i = 0, n = mSelectedNodes.size(); i < n; i++) {
            nodes.add(mSelectedNodes.valueAt(i));
        }

        return nodes;
    }

    public int getSelectedNodesCount() {
        return mSelectedNodes.size();
    }

    public int getRealNodesCount() {
        return mRealNodeCount;
    }

    public void selectAll() {
        List<CuNode> nodes = mCuNodes.getValue();
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        for (int i = 0; i < nodes.size(); i++) {
            CuNode node = nodes.get(i);
            if (node.getNode() != null) {
                if (!node.isSelected()) {
                    mNodeToAnimate.setValue(Pair.create(i, node));
                }
                node.setSelected(true);
                mSelectedNodes.put(node.getNode().getHandle(), node.getNode());
            }
        }

        mSelecting = true;
        mCuNodes.setValue(nodes);
        mActionMode.setValue(true);
    }

    public void clearSelection() {
        if (mSelecting) {
            mSelecting = false;
            mActionMode.setValue(false);

            List<CuNode> nodes = mCuNodes.getValue();
            if (nodes == null || nodes.isEmpty()) {
                return;
            }

            for (int i = 0; i < nodes.size(); i++) {
                CuNode node = nodes.get(i);
                if (node.isSelected()) {
                    mNodeToAnimate.setValue(Pair.create(i, node));
                }
                node.setSelected(false);
            }
            mSelectedNodes.clear();

            mCuNodes.setValue(nodes);
        }
    }

    public void setInitialPreferences() {
        add(Completable.fromCallable(
                () -> {
                    logDebug("setInitialPreferences");

                    mDbHandler.setFirstTime(false);
                    mDbHandler.setStorageAskAlways(true);
                    File defaultDownloadLocation = buildDefaultDownloadDir(getApplication());
                    defaultDownloadLocation.mkdirs();

                    mDbHandler.setStorageDownloadLocation(
                            defaultDownloadLocation.getAbsolutePath());
                    mDbHandler.setPinLockEnabled(false);
                    mDbHandler.setPinLockCode("");

                    ArrayList<MegaNode> nodeLinks = mMegaApi.getPublicLinks();
                    if (nodeLinks == null || nodeLinks.size() == 0) {
                        logDebug("No public links: showCopyright set true");
                        mDbHandler.setShowCopyright(true);
                    } else {
                        logDebug("Already public links: showCopyright set false");
                        mDbHandler.setShowCopyright(false);
                    }
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(ignore(), logErr("setInitialPreferences")));
    }

    public void setCamSyncEnabled(boolean enabled) {
        add(Completable.fromCallable(
                () -> {
                    mDbHandler.setCamSyncEnabled(enabled);
                    return enabled;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(ignore(), logErr("setCamSyncEnabled")));
    }

    public void enableCuForBusinessFirstTime(boolean enableCellularSync, boolean syncVideo) {
        add(Completable.fromCallable(
                () -> {
                    File localFile =
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DCIM);
                    mDbHandler.setCamSyncLocalPath(localFile.getAbsolutePath());
                    mDbHandler.setCameraFolderExternalSDCard(false);
                    mDbHandler.setCamSyncWifi(!enableCellularSync);
                    mDbHandler.setCamSyncFileUpload(
                            syncVideo ? MegaPreferences.PHOTOS_AND_VIDEOS
                                    : MegaPreferences.ONLY_PHOTOS);

                    mDbHandler.setCameraUploadVideoQuality(MEDIUM);
                    mDbHandler.setConversionOnCharging(true);
                    mDbHandler.setChargingOnSize(DEFAULT_CONVENTION_QUEUE_SIZE);
                    // After target and local folder setup, then enable CU.
                    mDbHandler.setCamSyncEnabled(true);
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(ignore(), logErr("enableCuForBusinessFirstTime")));
    }

    public LiveData<Boolean> camSyncEnabled() {
        MutableLiveData<Boolean> camSyncEnabled = new MutableLiveData<>();

        add(Single.fromCallable(
                () -> Boolean.parseBoolean(mDbHandler.getPreferences().getCamSyncEnabled()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(camSyncEnabled::setValue, logErr("camSyncEnabled")));

        return camSyncEnabled;
    }

    private void loadCuNodes() {
        loadCuNodes(Single.defer(() -> {
            int orderBy = MegaApiJava.ORDER_MODIFICATION_DESC;
            MegaPreferences pref = mDbHandler.getPreferences();
            if (pref != null) {
                try {
                    orderBy = Integer.parseInt(pref.getPreferredSortCameraUpload());
                } catch (NumberFormatException ignored) {
                }
            }
            return Single.just(orderBy);
        }).map(this::getCuNodes));
    }

    private void loadCuNodes(Single<List<CuNode>> source) {
        add(source.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(nodes -> {
                    mCuNodes.setValue(nodes);

                    String actionBarTitleWhenSearch = getSearchDateTitle(mSearchDate);
                    if (!TextUtils.isEmpty(actionBarTitleWhenSearch)) {
                        mActionBarTitle.setValue(actionBarTitleWhenSearch);
                    }
                }, logErr("loadCuNodes")));
    }

    private List<CuNode> getCuNodes(int orderBy) {
        List<CuNode> nodes = new ArrayList<>();
        List<MegaNode> nodesWithoutThumbnail = new ArrayList<>();

        LocalDate lastModifyDate = null;
        List<Pair<Integer, MegaNode>> realNodes = mRepo.getCuChildren(mType, orderBy, mSearchDate);
        for (Pair<Integer, MegaNode> pair : realNodes) {
            MegaNode node = pair.second;
            File thumbnail =
                    new File(getThumbFolder(getApplication()), node.getBase64Handle() + ".jpg");
            LocalDate modifyDate = fromEpoch(node.getModificationTime());
            String dateString = DateTimeFormatter.ofPattern("MMMM uuuu").format(modifyDate);

            if (lastModifyDate == null
                    || !YearMonth.from(lastModifyDate).equals(YearMonth.from(modifyDate))) {
                lastModifyDate = modifyDate;
                nodes.add(new CuNode(null, -1, null, CuNode.TYPE_TITLE, dateString, false));
            }

            nodes.add(new CuNode(node, pair.first, thumbnail.exists() ? thumbnail : null,
                    isVideoFile(node.getName()) ? CuNode.TYPE_VIDEO : CuNode.TYPE_IMAGE, dateString,
                    mSelectedNodes.containsKey(node.getHandle())));

            if (!thumbnail.exists()) {
                nodesWithoutThumbnail.add(node);
            }
        }
        mRealNodeCount = realNodes.size();

        for (MegaNode node : nodesWithoutThumbnail) {
            File thumbnail =
                    new File(getThumbFolder(getApplication()), node.getBase64Handle() + ".jpg");
            mMegaApi.getThumbnail(node, thumbnail.getAbsolutePath(), mCreateThumbnailRequest);
        }

        return nodes;
    }

    private String getSearchDateTitle(long[] filter) {
        if (filter == null) {
            return "";
        }

        if (filter[0] == 1) {
            return DateTimeFormatter.ofPattern("d MMM").format(fromEpoch(filter[1] / 1000));
        } else if (filter[0] == 2) {
            if (filter[2] == 1) {
                return DateTimeFormatter.ofPattern("MMMM").format(YearMonth.now().minusMonths(1));
            } else if (filter[2] == 2) {
                return String.valueOf(YearMonth.now().getYear() - 1);
            } else {
                return "";
            }
        } else if (filter[0] == 3) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM");
            LocalDate from = fromEpoch(filter[3] / 1000);
            LocalDate to = fromEpoch(filter[4] / 1000);
            return formatter.format(from) + " - " + formatter.format(to);
        } else {
            return "";
        }
    }
}
