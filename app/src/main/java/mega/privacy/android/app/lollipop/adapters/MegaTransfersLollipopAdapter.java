package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaTransfer;

import static mega.privacy.android.app.components.transferWidget.TransfersManagement.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaTransfer.*;


public class MegaTransfersLollipopAdapter extends RecyclerView.Adapter<MegaTransfersLollipopAdapter.ViewHolderTransfer> implements OnClickListener {

	private Context context;
	private MegaApiAndroid megaApi;

	private DisplayMetrics outMetrics;

	private ArrayList<MegaTransfer> tL;
	private RecyclerView listFragment;

	private boolean multipleSelect;

	public MegaTransfersLollipopAdapter(Context _context, ArrayList<MegaTransfer> _transfers, RecyclerView _listView) {
		this.context = _context;
		this.tL = _transfers;
		this.listFragment = _listView;

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

		int transferType = transfer.getType();
		int transferState = transfer.getState();

		if (transferType == TYPE_DOWNLOAD) {
			holder.iconDownloadUploadView.setImageResource(R.drawable.ic_download_transfers);
			holder.progressText.setTextColor(ContextCompat.getColor(context, R.color.download_green));
			MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
			holder.document = transfer.getNodeHandle();

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

					holder.imageView.setImageBitmap(thumb);
				} else {
					params.height = params.width = px2dp(48, outMetrics);
					params.setMargins(36, 0, 0, 0);

					holder.imageView.setImageResource(MimeTypeList.typeForName(transfer.getFileName()).getIconResourceId());
				}

				holder.imageView.setLayoutParams(params);
			}
		} else if (transferType == TYPE_UPLOAD) {
			holder.iconDownloadUploadView.setImageResource(R.drawable.ic_upload_transfers);
			holder.progressText.setTextColor(ContextCompat.getColor(context, R.color.upload_blue));
			holder.imageView.setImageResource(MimeTypeList.typeForName(transfer.getFileName()).getIconResourceId());
			holder.currentPath = transfer.getPath();

			params.height = params.width = px2dp(48, outMetrics);
			params.setMargins(36, 0, 0, 0);
			holder.imageView.setLayoutParams(params);

			RelativeLayout.LayoutParams params3 = (RelativeLayout.LayoutParams) holder.iconDownloadUploadView.getLayoutParams();
			params3.setMargins(0, 0, 0, 0);
			holder.iconDownloadUploadView.setLayoutParams(params3);
		}

		holder.textViewCompleted.setVisibility(View.GONE);
		holder.imageViewCompleted.setVisibility(View.GONE);
		holder.imageViewCompleted.setImageResource(R.drawable.ic_queue);
		holder.progressText.setVisibility(View.VISIBLE);
		holder.speedText.setVisibility(View.VISIBLE);
		holder.optionRemove.setVisibility(View.VISIBLE);
		holder.optionPause.setVisibility(View.VISIBLE);
		holder.optionPause.setImageResource(R.drawable.ic_pause_grey);

		switch (transferState) {
			case STATE_PAUSED:
				holder.progressText.setText(getProgress(transfer));
				holder.speedText.setText(context.getResources().getString(R.string.transfer_paused));
				holder.optionPause.setImageResource(R.drawable.ic_play_grey);
				break;

			case STATE_ACTIVE:
				holder.progressText.setText(getProgress(transfer));
				holder.speedText.setText(getSpeedString(transfer.getSpeed()));
				break;

			case STATE_COMPLETING:
			case STATE_RETRYING:
			case STATE_QUEUED:
				if ((transferType == TYPE_DOWNLOAD && isOnTransferOverQuota())
						|| (transferType == TYPE_UPLOAD && MegaApplication.getInstance().getStorageState() == MegaApiJava.STORAGE_STATE_RED)) {
					holder.progressText.setTextColor(ContextCompat.getColor(context, R.color.over_quota_yellow));

					if (transferType == TYPE_DOWNLOAD) {
						holder.progressText.setText(String.format("%s %s", getProgress(transfer), context.getString(R.string.label_transfer_over_quota)));
					} else {
						holder.progressText.setText(String.format("%s %s", getProgress(transfer), context.getString(R.string.label_storage_over_quota)));
					}

					holder.speedText.setVisibility(View.GONE);
				} else if (transferState == STATE_QUEUED) {
					holder.progressText.setVisibility(View.GONE);
					holder.speedText.setVisibility(View.GONE);
					holder.imageViewCompleted.setVisibility(View.VISIBLE);
					holder.textViewCompleted.setVisibility(View.VISIBLE);
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
				holder.progressText.setVisibility(View.GONE);
				holder.speedText.setVisibility(View.GONE);
				holder.imageViewCompleted.setVisibility(View.VISIBLE);
                holder.textViewCompleted.setVisibility(View.VISIBLE);
				holder.textViewCompleted.setText(context.getResources().getString(R.string.transfer_unknown));
				holder.optionPause.setVisibility(View.GONE);
				break;
		}

		holder.itemLayout.setBackgroundColor(Color.WHITE);
		holder.optionRemove.setTag(holder);
		holder.optionPause.setTag(holder);
	}

    private String getProgress(MegaTransfer transfer) {
        return Math.round(100.0 * transfer.getTransferredBytes() / transfer.getTotalBytes()) + "%";
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
				((ManagerActivityLollipop) context).showConfirmationCancelTransfer(t, true);
				break;

			case R.id.transfers_list_option_pause:
				((ManagerActivityLollipop) context).pauseIndividualTransfer(t);
				break;
		}
	}

	public void removeItemData(int position) {
		notifyItemRemoved(position);
		notifyItemRangeChanged(position, getItemCount());
	}

	public void updateProgress(int position, MegaTransfer transfer) {
		ViewHolderTransfer holder = (ViewHolderTransfer) listFragment.findViewHolderForAdapterPosition(position);
		if (holder == null) {
			notifyItemChanged(position);
			return;
		}

		if (holder.progressText.getVisibility() == View.GONE) {
			holder.progressText.setVisibility(View.VISIBLE);
			holder.speedText.setVisibility(View.VISIBLE);
			holder.textViewCompleted.setVisibility(View.GONE);
			holder.imageViewCompleted.setVisibility(View.GONE);
		}

        holder.progressText.setText(getProgress(transfer));
        holder.speedText.setText(getSpeedString(transfer.getSpeed()));
	}

	public boolean isMultipleSelect() {
		return multipleSelect;
	}

	public void setMultipleSelect(boolean multipleSelect) {
		if (this.multipleSelect != multipleSelect) {
			this.multipleSelect = multipleSelect;
			notifyDataSetChanged();
		}
	}
}
