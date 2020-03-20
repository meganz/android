package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
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

import mega.privacy.android.app.AndroidCompletedTransfer;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaCompletedTransfersAdapter;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.DatabaseHandler.MAX_TRANSFERS;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;


public class CompletedTransfersFragmentLollipop extends Fragment {

	private Context context;
	private RecyclerView listView;
	private MegaCompletedTransfersAdapter adapter;

	private MegaApiAndroid megaApi;
	private ImageView emptyImage;
	private TextView emptyText;

	private DisplayMetrics outMetrics;

	private LinearLayoutManager mLayoutManager;

	private DatabaseHandler dbH;

	public ArrayList<AndroidCompletedTransfer> tL = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (megaApi == null) {
			megaApi = MegaApplication.getInstance().getMegaApi();
		}

		dbH = DatabaseHandler.getDbHandler(context);

		super.onCreate(savedInstanceState);
		logDebug("onCreate");
	}

	public static CompletedTransfersFragmentLollipop newInstance() {
		return new CompletedTransfersFragmentLollipop();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		super.onCreateView(inflater, container, savedInstanceState);

		if (megaApi == null) {
			megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);

		View v = inflater.inflate(R.layout.fragment_transfers, container, false);

		listView = v.findViewById(R.id.transfers_list_view);
		listView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
		mLayoutManager = new LinearLayoutManager(context);
		listView.setHasFixedSize(true);
		listView.setItemAnimator(new DefaultItemAnimator());
		listView.setLayoutManager(mLayoutManager);
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

		String textToShow = String.format(context.getString(R.string.completed_transfers_empty_new));
		try {
			textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
			textToShow = textToShow.replace("[/A]", "</font>");
			textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
			textToShow = textToShow.replace("[/B]", "</font>");
		} catch (Exception e) {
			logWarning("Exception formatting string", e);
		}
		emptyText.setText(getSpannedHtmlText(textToShow));

		setCompletedTransfers();

		adapter = new MegaCompletedTransfersAdapter(context, tL);

		listView.setAdapter(adapter);

		return v;
	}

	private void setCompletedTransfers() {
		logDebug("setCompletedTransfers");
		tL.clear();
		tL.addAll(dbH.getCompletedTransfers());
		setEmptyView();
	}

	private void setEmptyView() {
		if (tL != null && tL.size() > 0) {
			emptyImage.setVisibility(View.GONE);
			emptyText.setVisibility(View.GONE);
			listView.setVisibility(View.VISIBLE);
		} else {
			emptyImage.setVisibility(View.VISIBLE);
			emptyText.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
		}
	}

	public void updateCompletedTransfers() {
		logDebug("updateCompletedTransfers");

		setCompletedTransfers();
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		this.context = context;
	}

	public void transferFinish(AndroidCompletedTransfer transfer) {
		logDebug("transferFinish");
		if (tL != null) {
			tL.add(0, transfer);

			if (tL.size() >= MAX_TRANSFERS) {
				tL.remove(tL.size() - 1);
			}
		} else {
			tL = new ArrayList<>();
			tL.add(transfer);
		}

		if (tL.size() == 1) {
			((ManagerActivityLollipop) context).invalidateOptionsMenu();
		}

		setEmptyView();
		adapter.notifyDataSetChanged();
	}

	public boolean isAnyTransferCompleted() {
		return tL != null && !tL.isEmpty();
	}
}
