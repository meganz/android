package mega.privacy.android.app.lollipop.managerSections.cu;

import android.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.arch.BaseRxViewModel;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.FileUtils.isVideoFile;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getThumbFolder;

class CuViewModel extends BaseRxViewModel {
  private static final int[] MONTH_NAME = new int[] {
      R.string.january, R.string.february, R.string.march,
      R.string.april, R.string.may, R.string.june,
      R.string.july, R.string.august, R.string.september,
      R.string.october, R.string.november, R.string.december
  };

  private final MegaApiAndroid megaApi;
  private final MegaPreferences pref;

  private final MutableLiveData<List<CuNode>> cuNodes = new MutableLiveData<>();
  private final MutableLiveData<Pair<Integer, CuNode>> nodeToOpen = new MutableLiveData<>();
  private final MutableLiveData<Pair<Integer, CuNode>> nodeToAnimate = new MutableLiveData<>();

  private final Subject<Pair<Integer, CuNode>> openNodeAction = PublishSubject.create();

  private boolean selecting;

  public CuViewModel(MegaApiAndroid megaApi, DatabaseHandler dbHandler) {
    this.megaApi = megaApi;
    pref = dbHandler.getPreferences();

    int orderBy = MegaApiJava.ORDER_MODIFICATION_DESC;
    try {
      orderBy = Integer.parseInt(pref.getPreferredSortCameraUpload());
    } catch (NumberFormatException ignored) {
    }
    loadCuNodes(orderBy);

    add(openNodeAction.throttleFirst(1, TimeUnit.SECONDS)
        .subscribe(nodeToOpen::setValue,
            throwable -> logError("openNodeAction onError", throwable)));
  }

  public LiveData<List<CuNode>> cuNodes() {
    return cuNodes;
  }

  public LiveData<Pair<Integer, CuNode>> nodeToOpen() {
    return nodeToOpen;
  }

  public LiveData<Pair<Integer, CuNode>> nodeToAnimate() {
    return nodeToAnimate;
  }

  public void setOrderBy(int orderBy) {
    loadCuNodes(orderBy);
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
    if (selecting) {
      List<CuNode> nodes = cuNodes.getValue();
      if (nodes == null || position < 0 || position >= nodes.size()
          || nodes.get(position).getNode().getHandle() != node.getNode().getHandle()) {
        return;
      }

      nodes.get(position).setSelected(!nodes.get(position).isSelected());

      nodeToAnimate.setValue(Pair.create(position, node));
    } else {
      openNodeAction.onNext(Pair.create(position, node));
    }
  }

  public void onNodeLongClicked(int position, CuNode node) {
    if (!selecting) {
      // TODO(px): toggle tool bar action on fragment
      selecting = true;
    }
    onNodeClicked(position, node);
  }

  private void loadCuNodes(int orderBy) {
    add(Single.defer(() -> Single.just(getCuNodes(orderBy)))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(cuNodes::setValue, throwable -> logError("loadCuNodes onError", throwable)));
  }

  private List<CuNode> getCuNodes(int orderBy) {
    List<CuNode> nodes = new ArrayList<>();

    LocalDateTime lastNodeModifyTime = null;
    for (MegaNode node : getCuChildren(orderBy)) {
      File thumbnail = new File(getThumbFolder(getApplication()), node.getBase64Handle() + ".jpg");
      LocalDateTime modifyTime =
          LocalDateTime.ofInstant(Instant.ofEpochSecond(node.getModificationTime()),
              ZoneId.systemDefault());

      if (lastNodeModifyTime == null
          || modifyTime.getMonthValue() != lastNodeModifyTime.getMonthValue()
          || modifyTime.getYear() != lastNodeModifyTime.getYear()) {
        lastNodeModifyTime = modifyTime;
        nodes.add(new CuNode(null, null, CuNode.TYPE_TITLE, getDateString(lastNodeModifyTime)));
      }
      nodes.add(new CuNode(node, thumbnail.exists() ? thumbnail : null,
          isVideoFile(node.getName()) ? CuNode.TYPE_VIDEO : CuNode.TYPE_IMAGE,
          getDateString(lastNodeModifyTime)));
    }

    return nodes;
  }

  private List<MegaNode> getCuChildren(int orderBy) {
    long cuHandle = -1;
    if (pref != null && pref.getCamSyncHandle() != null) {
      cuHandle = Long.parseLong(pref.getCamSyncHandle());
    }
    return cuHandle == -1 ? Collections.emptyList()
        : megaApi.getChildren(megaApi.getNodeByHandle(cuHandle), orderBy);
  }

  private String getDateString(LocalDateTime dateTime) {
    return getApplication().getString(MONTH_NAME[dateTime.getMonthValue() - 1])
        + " " + dateTime.getYear();
  }
}
