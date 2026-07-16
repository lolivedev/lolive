package com.ho.lolive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import coil.load
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.ho.lolive.domain.model.AppUpdateInfo
import com.ho.lolive.domain.model.LivePlatform
import com.ho.lolive.presentation.home.HomeUiState
import com.ho.lolive.presentation.home.HomeViewModel
import com.ho.lolive.presentation.home.UpdateCheckToast
import com.ho.lolive.presentation.xml.PlatformSelectorBottomSheet
import com.ho.lolive.presentation.xml.RoomPagingAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var rootView: View
    private lateinit var toolbar: MaterialToolbar
    private lateinit var searchEdit: TextInputEditText
    private lateinit var platformIcon: ImageView
    private lateinit var platformTitle: TextView
    private lateinit var platformMeta: TextView
    private lateinit var switchPlatformButton: MaterialButton
    private lateinit var roomsSectionTitle: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var listLoading: View
    private lateinit var emptyState: View
    private lateinit var emptyText: TextView
    private lateinit var emptyActionButton: MaterialButton

    private val roomAdapter = RoomPagingAdapter { roomId ->
        startActivity(DetailActivity.createIntent(this, roomId))
    }

    private var latestLoadStates: CombinedLoadStates? = null
    private var latestUpdateInfo: AppUpdateInfo? = null
    private var latestState: HomeUiState = HomeUiState()
    private var lastShownError: String? = null
    private var updateDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        bindViews()
        setupInsets()
        setupList()
        setupActions()
        observeUi()
    }

    private fun bindViews() {
        rootView = findViewById(R.id.mainRoot)
        toolbar = findViewById(R.id.toolbar)
        searchEdit = findViewById(R.id.searchEdit)
        platformIcon = findViewById(R.id.platformIcon)
        platformTitle = findViewById(R.id.platformTitle)
        platformMeta = findViewById(R.id.platformMeta)
        switchPlatformButton = findViewById(R.id.switchPlatformButton)
        roomsSectionTitle = findViewById(R.id.roomsSectionTitle)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        recyclerView = findViewById(R.id.roomsRecycler)
        listLoading = findViewById(R.id.listLoading)
        emptyState = findViewById(R.id.emptyState)
        emptyText = findViewById(R.id.emptyText)
        emptyActionButton = findViewById(R.id.emptyActionButton)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbar.updatePadding(top = bars.top)
            rootView.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }
    }

    private fun setupList() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = null
        recyclerView.adapter = roomAdapter
        roomAdapter.addLoadStateListener { states ->
            latestLoadStates = states
            renderListState()
        }
    }

    private fun setupActions() {
        toolbar.inflateMenu(R.menu.main_actions)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_refresh -> {
                    playRefreshActionAnimation()
                    onPlatformRefreshRequested()
                    true
                }
                R.id.action_check_update -> {
                    if (viewModel.uiState.value.isCheckingUpdate) {
                        Toast.makeText(this, R.string.update_checking, Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.checkUpdateManually()
                    }
                    true
                }
                else -> false
            }
        }

        switchPlatformButton.setOnClickListener {
            showPlatformSelector()
        }
        emptyActionButton.setOnClickListener {
            onRefreshRequested()
        }
        swipeRefresh.setOnRefreshListener {
            onRefreshRequested()
        }
        searchEdit.doAfterTextChanged { editable ->
            viewModel.onQueryChanged(editable?.toString().orEmpty())
        }
    }

    private fun observeUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pagedRooms.collect { pagingData ->
                        roomAdapter.submitData(pagingData)
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        latestState = state
                        renderHomeState(state)
                    }
                }
            }
        }
    }

    private fun onRefreshRequested() {
        // Keep pull-to-refresh gesture but do not show the top spinner.
        swipeRefresh.isRefreshing = false
        viewModel.refresh()
    }

    private fun onPlatformRefreshRequested() {
        swipeRefresh.isRefreshing = false
        viewModel.refreshPlatforms()
    }

    private fun playRefreshActionAnimation() {
        val refreshActionView = toolbar.findViewById<View>(R.id.action_refresh) ?: return
        refreshActionView.animate().cancel()
        refreshActionView.rotation = 0f
        refreshActionView.animate()
            .rotationBy(360f)
            .setDuration(420L)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    private fun renderHomeState(state: HomeUiState) {

        val selectedPlatform = state.platforms.firstOrNull { it.address == state.selectedPlatformAddress }
        renderPlatformCard(selectedPlatform, state.platforms.size)
        renderRoomsSectionTitle(selectedPlatform)

        // Use in-list loading UI only; suppress SwipeRefreshLayout spinner.
        swipeRefresh.isRefreshing = false

        val errorMessage = state.errorMessage?.takeIf { it.isNotBlank() }
        if (errorMessage != null && errorMessage != lastShownError) {
            lastShownError = errorMessage
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        } else if (errorMessage == null) {
            lastShownError = null
        }

        state.updateCheckToast?.let { toast ->
            val messageRes = when (toast) {
                UpdateCheckToast.ALREADY_LATEST -> R.string.update_already_latest
                UpdateCheckToast.CHECK_FAILED -> R.string.update_check_failed
            }
            Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
            viewModel.consumeUpdateCheckToast()
        }

        if (state.availableUpdate != latestUpdateInfo) {
            latestUpdateInfo = state.availableUpdate
            state.availableUpdate?.let { update ->
                showUpdateDialog(update)
            }
        }

        renderListState()
    }

    private fun renderPlatformCard(platform: LivePlatform?, totalPlatforms: Int) {
        platformIcon.load(platform?.iconUrl) {
            crossfade(true)
            placeholder(R.drawable.logo)
            error(R.drawable.logo)
        }
        platformTitle.text = platform?.title ?: getString(R.string.select_platform)
        platformMeta.text = if (platform == null) {
            getString(R.string.select_platform_hint)
        } else {
            getString(
                R.string.platform_meta_format,
                platform.onlineCount,
                totalPlatforms,
            )
        }
    }

    private fun renderRoomsSectionTitle(platform: LivePlatform?) {
        roomsSectionTitle.text = if (platform == null) {
            getString(R.string.rooms_section_title)
        } else {
            getString(R.string.rooms_section_title_with_platform, platform.title)
        }
    }

    private fun renderListState() {
        val state = latestState
        val refreshState = latestLoadStates?.refresh
        val isLoading = state.isLoadingPlatforms || state.isRefreshingRooms || refreshState is LoadState.Loading
        val adapterEmpty = roomAdapter.itemCount == 0

        listLoading.isVisible = isLoading && adapterEmpty

        val refreshError = (refreshState as? LoadState.Error)?.error?.localizedMessage
        val stateError = state.errorMessage?.takeIf { it.isNotBlank() }
        val emptyMessage = when {
            !refreshError.isNullOrBlank() -> refreshError
            !stateError.isNullOrBlank() -> stateError
            state.platforms.isEmpty() -> getString(R.string.empty_platforms)
            else -> getString(R.string.empty_text)
        }

        val showEmpty = !isLoading && adapterEmpty
        emptyState.isVisible = showEmpty
        emptyText.text = emptyMessage
        emptyActionButton.text = if (!stateError.isNullOrBlank() || !refreshError.isNullOrBlank()) {
            getString(R.string.retry)
        } else {
            getString(R.string.refresh)
        }
    }

    private fun showPlatformSelector() {
        val state = viewModel.uiState.value
        if (state.platforms.isEmpty()) {
            Toast.makeText(this, R.string.empty_platforms, Toast.LENGTH_SHORT).show()
            return
        }
        PlatformSelectorBottomSheet.show(
            context = this,
            allPlatforms = state.platforms,
            selectedAddress = state.selectedPlatformAddress,
        ) { address ->
            viewModel.onPlatformSelected(address)
        }
    }

    private fun showUpdateDialog(update: AppUpdateInfo) {
        if (updateDialogShowing) return
        updateDialogShowing = true
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.update_available_title))
            .setMessage(getString(R.string.update_available_message, update.versionName))
            .setPositiveButton(getString(R.string.update_now)) { _, _ ->
                // 去更新：只关弹窗，不忽略版本，下次启动仍可提醒。
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)))
                viewModel.dismissUpdateDialog(ignoreVersion = false)
            }
            .setNegativeButton(getString(R.string.update_later)) { _, _ ->
                // 稍后：忽略该版本，避免自动检查反复弹窗。
                viewModel.dismissUpdateDialog(ignoreVersion = true)
            }
            .setOnCancelListener {
                // 点遮罩/返回：与稍后一致，忽略该版本。
                viewModel.dismissUpdateDialog(ignoreVersion = true)
            }
            .setOnDismissListener {
                updateDialogShowing = false
            }
            .setCancelable(true)
            .show()
    }

    override fun onDestroy() {
        recyclerView.adapter = null
        super.onDestroy()
    }
}
