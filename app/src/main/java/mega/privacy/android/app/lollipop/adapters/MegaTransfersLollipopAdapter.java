package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaTransfer;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static mega.privacy.android.app.components.transferWidget.TransfersManagement.*;
import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.THUMB_ROUND_PIXEL;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getRoundedBitmap;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaTransfer.*;


public class MegaTransfersLollipopAdapter extends RecyclerView.Adapter<MegaTransfersLollipopAdapter.ViewHolderTransfer> implements OnClickListener, RotatableAdapter {

    private Context context;
    private MegaApiAndroid megaApi;

    private DisplayMetrics outMetrics;

    private ArrayList<MegaTransfer> tL;
    private RecyclerView listFragment;

    private SelectModeInterface selectModeInterface;
    private boolean multipleSelect;
    private SparseBooleanArray selectedItems;

    public MegaTransfersLollipopAdapter(Context _context, ArrayList<MegaTransfer> _transfers, RecyclerView _listView, SelectModeInterface selectModeInterface) {
        this.context = _context;
        this.tL = _transfers;
        this.listFragment = _listView;
        this.selectModeInterface = selectModeInterface;

        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
    }

    public static class ViewHolderTransfer extends RecyclerView.ViewHolder {
        public ViewHolderTransfer(View v) {
            super(v);
        }

        public ImageView imageView;
        public ImageView iconDownloadUploadView;
        public TextView textViewFileName;
        public ImageView imageViewCompleted;
        public TextView textViewCompleted;
        public RelativeLayout itemLayout;
        public ImageView optionRemove;
        public ImageView optionPause;
        public long document;
        public String currentPath;
        public TextView progressText;
        public TextView speedText;
    }


    public void setTransfers(ArrayList<MegaTransfer> transfers) {
        this.tL = transfers;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolderTransfer onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transfers_list, parent, false);

        ViewHolderTransfer holder = new ViewHolderTransfer(v);
        holder.itemLayout = v.findViewById(R.id.transfers_list_item_layout);
        holder.itemLayout.setOnClickListener(this);
        holder.imageView = v.findViewById(R.id.transfers_list_thumbnail);
        holder.iconDownloadUploadView = v.findViewById(R.id.transfers_list_small_icon);
        holder.textViewFileName = v.findViewById(R.id.transfers_list_filename);
        holder.progressText = v.findViewById(R.id.transfers_progress_text);
        holder.speedText = v.findViewById(R.id.transfers_speed_text);
        holder.imageViewCompleted = v.findViewById(R.id.transfers_list_completed_image);
        holder.textViewCompleted = v.findViewById(R.id.transfers_list_completed_text);
        holder.optionRemove = v.findViewById(R.id.transfers_list_option_remove);
        holder.optionRemove.setOnClickListener(this);
        holder.optionPause = v.findViewById(R.id.transfers_list_option_pause);
        holder.optionPause.setOnClickListener(this);
        v.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolderTransfer holder, int position) {
        MegaTransfer transfer = (MegaTransfer) getItem(position);

        if (transfer == null) {
            logWarning("The recovered transfer is NULL - do not update");
            return;
        }

        String fileName = transfer.getFileName();
        holder.textViewFileName.setText(fileName);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
        boolean isItemChecked = isItemChecked(position);
        int transferType = transfer.getType();
        int transferState = transfer.getState();

        if (transferType == TYPE_DOWNLOAD) {
            holder.progressText.setTextColor(ContextCompat.getColor(context, R.color.green_unlocked_rewards));
            holder.document = transfer.getNodeHandle();

            if (!isItemChecked) {
                holder.iconDownloadUploadView.setImageResource(R.drawable.ic_download_transfers);

                MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
                if (node != null) {
                    RelativeLayout.LayoutParams params3 = (RelativeLayout.LayoutParams) holder.iconDownloadUploadView.getLayoutParams();
                    params3.setMargins(0, 0, 0, 0);
                    holder.iconDownloadUploadView.setLayoutParams(params3);

                    Bitmap thumb = null;
                    if (node.hasThumbnail()) {
                        thumb = getThumbnailFromCache(node);
                        if (thumb == null) {
                            thumb = getThumbnailFromFolder(node, context);
                        }

                        if (thumb == null) {
                            try {
                                thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaTransfer(node, context, holder, megaApi, this);
                            } catch (Exception e) {
                                logError("Expection getting thumbnail", e);
                            }
                        }
                    }

                    if (thumb != null) {
                        params.height = params.width = px2dp(36, outMetrics);
                        params.setMargins(54, 0, 18, 0);

                        RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.iconDownloadUploadView.getLayoutParams();
                        params1.setMargins(0, -12, -12, 0);
                        holder.iconDownloadUploadView.setLayoutParams(params1);

                        holder.imageView.setImageBitmap(getRoundedBitmap(context, thumb, THUMB_ROUND_PIXEL));
                    } else {
                        params.height = params.width = px2dp(48, outMetrics);
                        params.setMargins(36, 0, 0, 0);

                        holder.imageView.setImageResource(MimeTypeList.typeForName(transfer.getFileName()).getIconResourceId());
                    }

                    holder.imageView.setLayoutParams(params);
                }
            }
        } else if (transferType == TYPE_UPLOAD) {
            if (!isItemChecked) {
                holder.iconDownloadUploadView.setImageResource(R.drawable.ic_upload_transfers);
                RelativeLayout.LayoutParams params3 = (RelativeLayout.LayoutParams) holder.iconDownloadUploadView.getLayoutParams();
                params3.setMargins(0, 0, 0, 0);
                holder.iconDownloadUploadView.setLayoutParams(params3);

                holder.imageView.setImageResource(MimeTypeList.typeForName(transfer.getFileName()).getIconResourceId());
                params.height = params.width = px2dp(48, outMetrics);
                params.setMargins(36, 0, 0, 0);
                holder.imageView.setLayoutParams(params);
            }

            holder.progressText.setTextColor(ContextCompat.getColor(context, R.color.business_color));
            holder.currentPath = transfer.getPath();
        }

        if (isItemChecked) {
            holder.iconDownloadUploadView.setVisibility(GONE);
            holder.imageView.setImageResource(R.drawable.ic_select_folder);
            params.height = params.width = px2dp(48, outMetrics);
            params.setMargins(36, 0, 0, 0);
            holder.imageView.setLayoutParams(params);
            holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
        } else {
            holder.itemLayout.setBackgroundColor(Color.WHITE);
            holder.iconDownloadUploadView.setVisibility(VISIBLE);
        }

        if (isMultipleSelect()) {
            holder.optionRemove.setVisibility(GONE);
            holder.optionPause.setVisibility(GONE);
        } else {
            holder.optionRemove.setVisibility(VISIBLE);
            holder.optionPause.setVisibility(VISIBLE);
            holder.optionPause.setImageResource(R.drawable.ic_pause_grey);
        }

        holder.textViewCompleted.setVisibility(GONE);
        holder.imageViewCompleted.setVisibility(GONE);
        holder.imageViewCompleted.setImageResource(R.drawable.ic_queue);
        holder.progressText.setVisibility(VISIBLE);
        holder.speedText.setVisibility(VISIBLE);

        if (megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD) || megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)) {
            holder.progressText.setText(getProgress(transfer));
            holder.speedText.setText(context.getResources().getString(R.string.transfer_paused));

            if (!isMultipleSelect() && transferState == STATE_PAUSED) {
                holder.optionPause.setImageResource(R.drawable.ic_play_grey);
            }
        } else {
            switch (transferState) {
                case STATE_PAUSED:
                    holder.progressText.setText(getProgress(transfer));
                    holder.speedText.setText(context.getResources().getString(R.string.transfer_paused));

					if (!isMultipleSelect()) {
						holder.optionPause.setImageResource(R.drawable.ic_play_grey);
					}

					break;

                case STATE_ACTIVE:
                    holder.progressText.setText(getProgress(transfer));
                    holder.speedText.setText(getSpeedString(isOnline(context)
                            ? transfer.getSpeed()
                            : 0));
                    break;

                case STATE_COMPLETING:
                case STATE_RETRYING:
                case STATE_QUEUED:
                    if ((transferType == TYPE_DOWNLOAD && isOnTransferOverQuota())
                            || (transferType == TYPE_UPLOAD && MegaApplication.getInstance().getStorageState() == MegaApiJava.STORAGE_STATE_RED)) {
                        holder.progressText.setTextColor(ContextCompat.getColor(context, R.color.reconnecting_bar));

                        if (transferType == TYPE_DOWNLOAD) {
                            holder.progressText.setText(String.format("%s %s", getProgress(transfer), context.getString(R.string.label_transfer_over_quota)));
                        } else {
                            holder.progressText.setText(String.format("%s %s", getProgress(transfer), context.getString(R.string.label_storage_over_quota)));
                        }

                        holder.speedText.setVisibility(GONE);
                    } else if (transferState == STATE_QUEUED) {
                        holder.progressText.setVisibility(GONE);
                        holder.speedText.setVisibility(GONE);
                        holder.imageViewCompleted.setVisibility(VISIBLE);
                        holder.textViewCompleted.setVisibility(VISIBLE);
                        holder.textViewCompleted.setText(context.getResources().getString(R.string.transfer_queued));
                    } else {
                        holder.progressText.setText(getProgress(transfer));

                        if (transferState == STATE_COMPLETING) {
                            holder.speedText.setText(context.getResources().getString(R.string.transfer_completing));
                        } else {
                            holder.speedText.setText(context.getResources().getString(R.string.transfer_retrying));
                        }
                    }
                    break;

                default:
                    logDebug("Default status");
                    holder.progressText.setVisibility(GONE);
                    holder.speedText.setVisibility(GONE);
                    holder.imageViewCompleted.setVisibility(VISIBLE);
                    holder.textViewCompleted.setVisibility(VISIBLE);
                    holder.textViewCompleted.setText(context.getResources().getString(R.string.transfer_unknown));
                    holder.optionPause.setVisibility(GONE);
                    break;
            }
        }

        holder.itemLayout.setTag(holder);
        holder.optionRemove.setTag(holder);
        holder.optionPause.setTag(holder);
    }

    /**
     * Get the progress of a transfer.
     *
     * @param transfer transfer to get the progress
     * @return The progress of the transfer.
     */
    private String getProgress(MegaTransfer transfer) {
        long percentage = Math.round(100.0 * transfer.getTransferredBytes() / transfer.getTotalBytes());
        String size = getSizeString(transfer.getTotalBytes());

        return context.getString(R.string.progress_size_indicator, percentage, size);
    }

    @Override
    public int getItemCount() {
        return tL.size();
    }

    public Object getItem(int position) {
        if (position >= 0) {
            return tL.get(position);
        }
        logError("Error: position NOT valid: " + position);
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");

        ViewHolderTransfer holder = (ViewHolderTransfer) v.getTag();
        if (holder == null) {
            logWarning("Holder is NULL- not action performed");
            return;
        }

        int currentPosition = holder.getAdapterPosition();
        MegaTransfer t = (MegaTransfer) getItem(currentPosition);
        if (t == null) {
            logWarning("MegaTransfer is NULL- not action performed");
            return;
        }

        switch (v.getId()) {
            case R.id.transfers_list_option_remove:
                ((ManagerActivityLollipop) context).showConfirmationCancelTransfer(t);
                break;

            case R.id.transfers_list_option_pause:
                ((ManagerActivityLollipop) context).pauseIndividualTransfer(t);
                break;

			case R.id.transfers_list_item_layout:
				if (isMultipleSelect()) {
					toggleSelection(currentPosition);
				}
				break;
        }
    }

    /**
     * Removes a transfer from adapter.
     * Also checks if the transfer to remove is selected. If so, removes it from selected items list
     * and updates the list with the new positions that the rest of selected transfers occupies after
     * the removal.
     *
     * @param position Item to remove.
     */
    public void removeItemData(int position) {
        if (isItemChecked(position)) {
            int nextIndex = selectedItems.indexOfKey(position);
            selectedItems.delete(position);

            for (int i = nextIndex; i < selectedItems.size(); i++) {
                int pos = selectedItems.keyAt(i);
                selectedItems.delete(pos);
                selectedItems.append(pos - 1, true);
            }

            selectModeInterface.notifyItemChanged();
        }

        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }

    public void updateProgress(int position, MegaTransfer transfer) {
        ViewHolderTransfer holder = (ViewHolderTransfer) listFragment.findViewHolderForAdapterPosition(position);
        if (holder == null) {
            notifyItemChanged(position);
            return;
        }

        if (holder.progressText.getVisibility() == GONE) {
            holder.progressText.setVisibility(VISIBLE);
            holder.speedText.setVisibility(VISIBLE);
            holder.textViewCompleted.setVisibility(GONE);
            holder.imageViewCompleted.setVisibility(GONE);
        }

        holder.progressText.setText(getProgress(transfer));
        holder.speedText.setText(getSpeedString(isOnline(context) ?
                transfer.getSpeed()
                : 0));
    }

    public boolean isMultipleSelect() {
        return multipleSelect;
    }

	public void setMultipleSelect(boolean multipleSelect) {
		if (this.multipleSelect != multipleSelect) {
			this.multipleSelect = multipleSelect;
			notifyDataSetChanged();
		}

		if (this.multipleSelect) {
			selectedItems = new SparseBooleanArray();
		} else if (selectedItems != null) {
			selectedItems.clear();
		}
	}

    public void hideMultipleSelect() {
        setMultipleSelect(false);
        selectModeInterface.destroyActionMode();
    }

	public void selectAll() {
		for (int i = 0; i < getItemCount(); i++) {
			if (!isItemChecked(i)) {
				toggleSelection(i);
			}
		}
	}

	public void clearSelections() {
		for (int i = 0; i < getItemCount(); i++) {
			if (isItemChecked(i)) {
				toggleSelection(i);
			}
		}
	}

    /**
     * Checks if the current position is selected.
     * If it is, deselects it. If not, selects it.
     *
     * @param pos Position to check.
     * @return True if the position is selected, false otherwise.
     */
    private boolean putOrDeletePosition(int pos) {
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
            return true;
        } else {
            selectedItems.append(pos, true);
            return false;
        }
    }

    public void toggleSelection(int pos) {
        startAnimation(pos, putOrDeletePosition(pos));
        selectModeInterface.notifyItemChanged();
    }

    private void startAnimation(final int pos, final boolean delete) {
		MegaTransfersLollipopAdapter.ViewHolderTransfer view = (MegaTransfersLollipopAdapter.ViewHolderTransfer) listFragment.findViewHolderForLayoutPosition(pos);
		if (view == null) {
			return;
		}

		Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
		flipAnimation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				if (!delete) {
					notifyItemChanged(pos);
				}
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				if (delete) {
					notifyItemChanged(pos);
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});

		view.imageView.startAnimation(flipAnimation);
    }

    /**
     * Checks if select mode is enabled. If so, checks if the position is selected.
     *
     * @param position Position to check.
     * @return True if the position is selected, false otherwise.
     */
	private boolean isItemChecked(int position) {
		return isMultipleSelect() && selectedItems != null
				&& position >= 0
				&& selectedItems.get(position);
	}

    public List<MegaTransfer> getSelectedTransfers() {
        if (selectedItems == null) {
            return null;
        }

        ArrayList<MegaTransfer> selectedTransfers = new ArrayList<>();

        for (int i = 0; i < selectedItems.size(); i++) {
            if (!selectedItems.valueAt(i)) {
                continue;
            }

            MegaTransfer transfer = tL.get(i);
            if (transfer != null) {
                selectedTransfers.add(transfer);
            }
        }

        return selectedTransfers;
    }

    public int getSelectedItemsCount() {
        if (selectedItems == null) {
            return 0;
        }

        return selectedItems.size();
    }

    @Override
    public List<Integer> getSelectedItems() {
        if (selectedItems == null) {
            return null;
        }

        List<Integer> items = new ArrayList<>(selectedItems.size());

        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }

        return items;
    }

    @Override
    public int getFolderCount() {
        return 0;
    }

    @Override
    public int getPlaceholderCount() {
        return 0;
    }

    @Override
    public int getUnhandledItem() {
        return INVALID_POSITION;
    }

    public interface SelectModeInterface {
        void destroyActionMode();
        void notifyItemChanged();
    }

}
