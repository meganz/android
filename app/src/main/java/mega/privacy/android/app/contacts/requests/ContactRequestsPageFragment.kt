package mega.privacy.android.app.contacts.requests

import mega.privacy.android.icon.pack.R.drawable as IconPack
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.components.MarqueeTextView
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.contacts.requests.ContactRequestsFragment.Companion.EXTRA_IS_OUTGOING
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestListAdapter
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.contacts.requests.data.ContactRequestsState
import mega.privacy.android.app.databinding.PageContactRequestsBinding
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import mega.privacy.android.domain.entity.user.ContactAvatar
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * Child fragment that represents the UI showing list of incoming/outgoing contact requests.
 */
@AndroidEntryPoint
class ContactRequestsPageFragment : Fragment() {

    /**
     * Get theme mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private lateinit var binding: PageContactRequestsBinding

    private val isOutgoing by lazy { arguments?.getBoolean(EXTRA_IS_OUTGOING, false) ?: false }
    private val viewModel by viewModels<ContactRequestsViewModel>({ requireParentFragment() })
    private val adapter by lazy { ContactRequestListAdapter(viewModel::selectItem) }
    private val incomingActions = listOf(
        BottomSheetActionInfo(
            R.string.contact_accept,
            R.drawable.ic_check_circle_medium_regular_outline,
            false,
            ContactRequestAction.Accept,
            "contact_request_options_sheet:action_accept",
        ),
        BottomSheetActionInfo(
            R.string.contact_ignore,
            IconPack.ic_slash_circle_medium_regular_outline,
            false,
            ContactRequestAction.Ignore,
            "contact_request_options_sheet:action_ignore",
        ),
        BottomSheetActionInfo(
            R.string.contact_decline,
            IconPack.ic_x_circle_medium_regular_outline,
            true,
            ContactRequestAction.Deny,
            "contact_request_options_sheet:action_deny",
        )
    )

    private val outgoingActions = listOf(
        BottomSheetActionInfo(
            R.string.contact_reinvite,
            R.drawable.ic_reinvite,
            false,
            ContactRequestAction.Remind,
            "contact_request_options_sheet:action_remind",
        ),
        BottomSheetActionInfo(
            R.string.general_remove,
            IconPack.ic_x_medium_regular_outline,
            true,
            ContactRequestAction.Delete,
            "contact_request_options_sheet:action_delete",
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PageContactRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
    }

    override fun onDestroyView() {
        binding.list.clearOnScrollListeners()
        super.onDestroyView()
    }

    private fun setupObservers() {
        val requestLiveData = if (isOutgoing) {
            viewModel.getOutgoingRequest()
        } else {
            viewModel.getIncomingRequest()
        }

        requestLiveData.observe(viewLifecycleOwner) { items ->
            showEmptyView(items.isNullOrEmpty(), isOutgoing)
            adapter.submitList(items)
        }
    }

    private fun setupView() {
        binding.list.adapter = adapter
        binding.list.setHasFixedSize(true)
        binding.list.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.contact_list_divider,
                        null
                    )!!
                )
            }
        )
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val showElevation = recyclerView.canScrollVertically(RecyclerView.NO_POSITION)
                (parentFragment as ContactRequestsFragment?)?.showElevation(showElevation)
            }
        })

        setupBottomSheet()
    }

    @OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
    private fun setupBottomSheet() {
        binding.composeContactRequestBottomSheet.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val state by viewModel.state.collectAsStateWithLifecycle()
                val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
                LaunchedEffect(sheetState.isVisible) {
                    if (!sheetState.isVisible) {
                        viewModel.deselectItem()
                    }
                }
                val coroutineScope = rememberCoroutineScope()

                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    (state as? ContactRequestsState.Data)?.selectedItem?.takeIf { it.isOutgoing == isOutgoing }
                        ?.let { item ->
                            coroutineScope.launch { sheetState.show() }
                            BottomSheet(
                                modifier = Modifier.semantics {
                                    testTagsAsResourceId = true
                                },
                                modalSheetState = sheetState,
                                sheetBody = {
                                    ContactRequestOptionsView(
                                        item = item,
                                        actions = (if (isOutgoing) outgoingActions else incomingActions)
                                            .map { info ->
                                                ComposableBottomSheetAction(
                                                    bottomSheetItem(
                                                        info
                                                    ) { action ->
                                                        viewModel.handleContactRequest(
                                                            item.handle,
                                                            action
                                                        )
                                                        coroutineScope.launch { sheetState.hide() }
                                                    }
                                                )
                                            },
                                    )
                                },
                                expandedRoundedCorners = true,
                            )
                        }
                }
            }
        }
    }

    @Composable
    private fun ContactRequestOptionsView(
        item: ContactRequestItem,
        actions: List<ComposableBottomSheetAction>,
    ) {
        AndroidView(
            factory = { context ->
                LayoutInflater.from(context)
                    .inflate(R.layout.item_contact_request, null)
            },
            update = { view ->
                view.findViewById<EmojiTextView>(R.id.txt_title).text =
                    item.email
                view.findViewById<MarqueeTextView>(R.id.txt_subtitle).text =
                    item.createdTime
                view.findViewById<ImageView>(R.id.img_thumbnail).load(
                    data = ContactAvatar(
                        email = item.email,
                        id = UserId(item.handle)
                    )
                ) {
                    transformations(CircleCropTransformation())
                    placeholder(item.placeholder)
                }
                view.findViewById<ImageButton>(R.id.btn_more).isVisible =
                    false
            }
        )
        MegaDivider(dividerType = DividerType.SmallStartPadding)
        actions.forEachIndexed { index, action ->
            if (index > 0) MegaDivider(dividerType = DividerType.BigStartPadding)
            action.view()
        }

    }

    /**
     * Show empty view required when there are no elements.
     *
     * @param show          Flag to either show or hide empty view
     * @param isOutgoing    Flag to show incoming/outgoing empty text
     */
    private fun showEmptyView(show: Boolean, isOutgoing: Boolean = false) {
        if (!show) {
            binding.viewEmpty.isVisible = false
        } else {
            val textRes: Int
            val drawableRes: Int

            if (isOutgoing) {
                textRes = R.string.sent_requests_empty
                drawableRes = R.drawable.ic_zero_data_sent_requests
            } else {
                textRes = R.string.received_requests_empty
                drawableRes = R.drawable.ic_zero_data_received_requests
            }

            binding.viewEmpty.setCompoundDrawablesWithIntrinsicBounds(0, drawableRes, 0, 0)
            binding.viewEmpty.text = getString(textRes)
                .formatColorTag(requireContext(), 'A', R.color.grey_900_grey_100)
                .formatColorTag(requireContext(), 'B', R.color.grey_300_grey_600)
                .toSpannedHtmlText()
            binding.viewEmpty.isVisible = true
        }
    }

    companion object {
        /**
         * New instance
         *
         * @param isOutgoing
         */
        fun newInstance(isOutgoing: Boolean): ContactRequestsPageFragment =
            ContactRequestsPageFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(EXTRA_IS_OUTGOING, isOutgoing)
                }
            }
    }

    /**
     * Contact Request bottom sheet action.
     *
     * @property view The Composable view to be displayed in the bottom sheet.
     * @property group
     */
    data class ComposableBottomSheetAction(
        val view: @Composable () -> Unit,
        val group: String = "default",
    )

    /**
     * Bottom sheet action info
     *
     * @property text
     * @property icon
     * @property isDestructive
     * @property action
     */
    internal data class BottomSheetActionInfo(
        @StringRes val text: Int,
        @DrawableRes val icon: Int,
        val isDestructive: Boolean,
        val action: ContactRequestAction,
        val testTag: String,
    )

    private fun bottomSheetItem(
        info: BottomSheetActionInfo,
        onClick: (ContactRequestAction) -> Unit,
    ): @Composable () -> Unit = {
        MenuActionListTile(
            text = stringResource(id = info.text),
            icon = painterResource(id = info.icon),
            modifier = Modifier
                .testTag(info.testTag)
                .clickable { onClick(info.action) },
            isDestructive = info.isDestructive,
        )
    }
}
