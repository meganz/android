package mega.privacy.android.app.lollipop.managerSections.cu;

import android.text.TextUtils;
import android.util.Pair;
import androidx.collection.LongSparseArray;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.arch.BaseRxViewModel;
import mega.privacy.android.app.listeners.BaseListener;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.FileUtils.isVideoFile;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getThumbFolder;

class CuViewModel extends BaseRxViewModel {
  private final MegaApiAndroid megaApi;
  private final DatabaseHandler dbHandler;

  private final MutableLiveData<List<CuNode>> cuNodes = new MutableLiveData<>();
  private final MutableLiveData<Pair<Integer, CuNode>> nodeToOpen = new MutableLiveData<>();
  private final MutableLiveData<Pair<Integer, CuNode>> nodeToAnimate = new MutableLiveData<>();
  private final MutableLiveData<String> actionBarTitle = new MutableLiveData<>();

  private final Subject<Pair<Integer, CuNode>> openNodeAction = PublishSubject.create();
  private final Subject<Object> creatingThumbnailFinished = PublishSubject.create();

  private final MegaRequestListenerInterface createThumbnailRequest =
      new BaseListener(getApplication()) {
        @Override public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
          if (e.getErrorCode() == MegaError.API_OK) {
            creatingThumbnailFinished.onNext(true);
          }
        }
      };

  private boolean selecting;
  private final LongSparseArray<MegaNode> selectedNodes = new LongSparseArray<>(5);

  private long[] searchDate;

  private int type;

  public CuViewModel(MegaApiAndroid megaApi, DatabaseHandler dbHandler, int type) {
    this.megaApi = megaApi;
    this.dbHandler = dbHandler;
    this.type = type;

    loadCuNodes();

    add(openNodeAction.throttleFirst(1, TimeUnit.SECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(nodeToOpen::setValue,
            throwable -> logError("openNodeAction onError", throwable)));

    add(creatingThumbnailFinished.throttleFirst(1, TimeUnit.SECONDS)
        .subscribe(ignored -> loadCuNodes(),
            throwable -> logError("creatingThumbnailFinished onError", throwable)));
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

  public LiveData<String> actionBarTitle() {
    return actionBarTitle;
  }

  public void setOrderBy(int orderBy) {
    loadCuNodes(orderBy);
  }

  public void setSearchDate(long[] searchDate, int orderBy) {
    this.searchDate = searchDate;
    loadCuNodes(orderBy);
  }

  public void setType(int type, int orderBy) {
    this.type = type;
    loadCuNodes(orderBy);
  }

  public boolean isSearchMode() {
    return searchDate != null;
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
      if (nodes.get(position).isSelected()) {
        selectedNodes.put(node.getNode().getHandle(), node.getNode());
      } else {
        selectedNodes.remove(node.getNode().getHandle());
      }
      selecting = !selectedNodes.isEmpty();

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

  public List<MegaNode> getSelectedNodes() {
    List<MegaNode> nodes = new ArrayList<>();
    for (int i = 0, n = selectedNodes.size(); i < n; i++) {
      nodes.add(selectedNodes.valueAt(i));
    }

    return nodes;
  }

  private void loadCuNodes(int orderBy) {
    loadCuNodes(Single.defer(() -> Single.just(getCuNodes(orderBy))));
  }

  private void loadCuNodes() {
    loadCuNodes(Single.defer(() -> {
      int orderBy = MegaApiJava.ORDER_MODIFICATION_DESC;
      MegaPreferences pref = dbHandler.getPreferences();
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
          cuNodes.setValue(nodes);

          String actionBarTitleWhenSearch = getSearchDateTitle(searchDate);
          if (!TextUtils.isEmpty(actionBarTitleWhenSearch)) {
            actionBarTitle.setValue(actionBarTitleWhenSearch);
          }
        }, throwable -> logError("loadCuNodes onError", throwable)));
  }

  private List<CuNode> getCuNodes(int orderBy) {
    List<CuNode> nodes = new ArrayList<>();
    List<MegaNode> nodesWithoutThumbnail = new ArrayList<>();

    LocalDate lastModifyDate = null;
    for (MegaNode node : filterNodesByDate(getCuChildren(orderBy), searchDate)) {
      File thumbnail = new File(getThumbFolder(getApplication()), node.getBase64Handle() + ".jpg");
      LocalDate modifyDate = fromEpoch(node.getModificationTime());
      String dateString = DateTimeFormatter.ofPattern("MMM uuuu").format(modifyDate);

      if (lastModifyDate == null
          || !YearMonth.from(lastModifyDate).equals(YearMonth.from(modifyDate))) {
        lastModifyDate = modifyDate;
        nodes.add(new CuNode(null, null, CuNode.TYPE_TITLE, dateString));
      }

      nodes.add(new CuNode(node, thumbnail.exists() ? thumbnail : null,
          isVideoFile(node.getName()) ? CuNode.TYPE_VIDEO : CuNode.TYPE_IMAGE, dateString));

      if (!thumbnail.exists()) {
        nodesWithoutThumbnail.add(node);
      }
    }

    for (MegaNode node : nodesWithoutThumbnail) {
      File thumbnail = new File(getThumbFolder(getApplication()), node.getBase64Handle() + ".jpg");
      megaApi.getThumbnail(node, thumbnail.getAbsolutePath(), createThumbnailRequest);
    }

    return nodes;
  }

  /**
   * Filter nodes by date.
   *
   * @param nodes all nodes
   * @param filter search filter
   * filter[0] is the search type:
   * 0 means search for nodes in one day, then filter[1] is the day in millis.
   * 1 means search for nodes in last month (filter[2] is 1), or in last year (filter[2] is 2).
   * 2 means search for nodes between two days, filter[3] and filter[4] are start and end day in
   * millis.
   */
  private List<MegaNode> filterNodesByDate(List<MegaNode> nodes, long[] filter) {
    if (filter == null) {
      return nodes;
    }

    List<MegaNode> result = new ArrayList<>();

    Function<MegaNode, Boolean> filterFunction = null;
    if (filter[0] == 1) {
      LocalDate date = fromEpoch(filter[1] / 1000);
      filterFunction = node -> date.equals(fromEpoch(node.getModificationTime()));
    } else if (filter[0] == 2) {
      if (filter[2] == 1) {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        filterFunction =
            node -> lastMonth.equals(YearMonth.from(fromEpoch(node.getModificationTime())));
      } else if (filter[2] == 2) {
        int lastYear = YearMonth.now().getYear() - 1;
        filterFunction = node -> fromEpoch(node.getModificationTime()).getYear() == lastYear;
      }
    } else if (filter[0] == 3) {
      LocalDate from = fromEpoch(filter[3] / 1000);
      LocalDate to = fromEpoch(filter[4] / 1000);
      filterFunction = node -> {
        LocalDate modifyDate = fromEpoch(node.getModificationTime());
        return !modifyDate.isBefore(from) && !modifyDate.isAfter(to);
      };
    }

    if (filterFunction == null) {
      return result;
    }

    for (MegaNode node : nodes) {
      if (filterFunction.apply(node)) {
        result.add(node);
      }
    }

    return result;
  }

  private String getSearchDateTitle(long[] filter) {
    if (filter == null) {
      return "";
    }

    if (filter[0] == 1) {
      return DateTimeFormatter.ofPattern("d MMM").format(fromEpoch(filter[1] / 1000));
    } else if (filter[0] == 2) {
      if (filter[2] == 1) {
        return DateTimeFormatter.ofPattern("MMM").format(YearMonth.now().minusMonths(1));
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

  private List<MegaNode> getCuChildren(int orderBy) {
    long cuHandle = -1;
    MegaPreferences pref = dbHandler.getPreferences();
    if (type == CameraUploadsFragment.TYPE_CAMERA) {
      if (pref != null && pref.getCamSyncHandle() != null) {
        try {
          cuHandle = Long.parseLong(pref.getCamSyncHandle());
        } catch (NumberFormatException ignored) {
        }
        if (megaApi.getNodeByHandle(cuHandle) == null) {
          cuHandle = -1;
        }
      }

      if (cuHandle == -1) {
        for (MegaNode node : megaApi.getChildren(megaApi.getRootNode())) {
          if (node.isFolder() && TextUtils.equals(
              getApplication().getString(R.string.section_photo_sync), node.getName())) {
            cuHandle = node.getHandle();
            dbHandler.setCamSyncHandle(cuHandle);
            break;
          }
        }
      }
    } else {
      if (pref != null && pref.getMegaHandleSecondaryFolder() != null) {
        try {
          cuHandle = Long.parseLong(pref.getMegaHandleSecondaryFolder());
        } catch (NumberFormatException ignored) {
        }
        if (megaApi.getNodeByHandle(cuHandle) == null) {
          cuHandle = -1;
        }
      }
    }

    return cuHandle == -1 ? Collections.emptyList()
        : megaApi.getChildren(megaApi.getNodeByHandle(cuHandle), orderBy);
  }

  private LocalDate fromEpoch(long seconds) {
    return LocalDate.from(
        LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault()));
  }
}
