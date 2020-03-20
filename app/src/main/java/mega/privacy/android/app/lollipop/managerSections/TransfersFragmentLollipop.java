package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.ListIterator;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaTransfersLollipopAdapter;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaTransfer;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;


public class TransfersFragmentLollipop extends Fragment {

	private Context context;
	private RecyclerView listView;
	private MegaTransfersLollipopAdapter adapter;

	private MegaApiAndroid megaApi;
	private ImageView emptyImage;
	private TextView emptyText;

	private DisplayMetrics outMetrics;

	private LinearLayoutManager mLayoutManager;

	private ArrayList<MegaTransfer> tL = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (megaApi == null) {
			megaApi = MegaApplication.getInstance().getMegaApi();
		}

		super.onCreate(savedInstanceState);
		logDebug("onCreate");
	}

	public static TransfersFragmentLollipop newInstance() {
		return new TransfersFragmentLollipop();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		super.onCreateView(inflater, container, savedInstanceState);

		if (megaApi == null) {
			megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
		}

		((ManagerActivityLollipop) context).supportInvalidateOptionsMenu();

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);

		View v = inflater.inflate(R.layout.fragment_transfers, container, false);

		listView = v.findViewById(R.id.transfers_list_view);
		listView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
		mLayoutManager = new LinearLayoutManager(context);
		listView.setLayoutManager(mLayoutManager);
		listView.setHasFixedSize(true);
		listView.setItemAnimator(null);
		listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				if (listView != null) {
					if (listView.canScrollVertically(-1)) {
						((ManagerActivityLollipop) context).changeActionBarElevation(true);
					} else {
						((ManagerActivityLollipop) context).changeActionBarElevation(false);
					}
				}
			}
		});

		emptyImage = v.findViewById(R.id.transfers_empty_image);
		emptyText = v.findViewById(R.id.transfers_empty_text);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			emptyImage.setImageResource(R.drawable.ic_zero_landscape_saved_for_offline);
		} else {
			emptyImage.setImageResource(R.drawable.ic_zero_portrait_transfers);
		}

		String textToShow = String.format(context.getString(R.string.transfers_empty_new));
		try {
			textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
			textToShow = textToShow.replace("[/A]", "</font>");
			textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
			textToShow = textToShow.replace("[/B]", "</font>");
		} catch (Exception e) {
			logWarning("Exception formatting string", e);
		}
		emptyText.setText(getSpannedHtmlText(textToShow));

		setTransfers();

		adapter = new MegaTransfersLollipopAdapter(context, this, tL, listView);

		adapter.setMultipleSelect(false);
		listView.setAdapter(adapter);

		return v;
	}

	private void setTransfers() {
		logDebug("setTransfers");

		for (int i = 0; i < ((ManagerActivityLollipop) context).transfersInProgress.size(); i++) {
			MegaTransfer transfer = megaApi.getTransferByTag(((ManagerActivityLollipop) context).transfersInProgress.get(i));
			if (transfer != null) {
				if (!transfer.isStreamingTransfer()) {
					tL.add(transfer);
				}
			}
		}

		setEmptyView();
	}

	private void setEmptyView() {
		if (tL.size() == 0) {
			emptyImage.setVisibility(View.VISIBLE);
			emptyText.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
		} else {
			emptyImage.setVisibility(View.GONE);
			emptyText.setVisibility(View.GONE);
			listView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		this.context = context;
	}

	public void transferUpdate(MegaTransfer transfer) {
		logDebug("Node handle: " + transfer.getNodeHandle());
		try {
			ListIterator li = tL.listIterator();
			int index = 0;
			while (li.hasNext()) {
				MegaTransfer next = (MegaTransfer) li.next();
				if (next != null && next.getTag() == transfer.getTag()) {
					index = li.previousIndex();
					break;
				}
			}
			tL.set(index, transfer);
			logDebug("Update the transfer with index: " + index + ", left: " + tL.size());

			adapter.updateProgress(index, transfer);
		} catch (IndexOutOfBoundsException e) {
			logError("EXCEPTION", e);
		}
	}

	public void changeStatusButton(int tag) {
		logDebug("tag: " + tag);

		ListIterator li = tL.listIterator();
		int index = 0;
		while (li.hasNext()) {
			MegaTransfer next = (MegaTransfer) li.next();
			if (next.getTag() == tag) {
				index = li.previousIndex();
				break;
			}
		}
		MegaTransfer transfer = megaApi.getTransferByTag(tag);
		tL.set(index, transfer);
		logDebug("The transfer with index : " + index + "has been paused/resumed, left: " + tL.size());

		adapter.notifyItemChanged(index);
	}

	public void transferFinish(int transferTag) {
		logDebug("transferTag is " + transferTag);
		int position = -1;
		for (int i = 0; i < tL.size(); i++) {
			MegaTransfer transfer = tL.get(i);
			if (transfer != null && transfer.getTag() == transferTag) {
				position = i;
				break;
			}
		}

		if (position == -1) {
			return;
		}

		if (!tL.isEmpty() && position < tL.size()) {
			tL.remove(position);
		}

		adapter.removeItemData(position);

		setEmptyView();
	}

	public void transferStart(MegaTransfer transfer) {
		logDebug("Node handle: " + transfer.getNodeHandle());
		if (!transfer.isStreamingTransfer()) {
			tL.add(transfer);
		}

		adapter.notifyItemInserted(tL.size() - 1);
		setEmptyView();
	}
}
