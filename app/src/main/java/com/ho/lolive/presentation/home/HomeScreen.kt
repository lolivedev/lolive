package com.ho.lolive.presentation.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.ho.lolive.R
import com.ho.lolive.domain.model.LivePlatform
import com.ho.lolive.domain.model.LiveRoom

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onRoomClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val rooms = viewModel.pagedRooms.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showPlatformSheet by remember { mutableStateOf(false) }
    var platformSearch by remember { mutableStateOf("") }

    val selectedPlatform = uiState.platforms.firstOrNull { it.address == uiState.selectedPlatformAddress }
    val refreshing = uiState.isLoadingPlatforms || uiState.isRefreshingRooms
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            viewModel.refresh()
            rooms.refresh()
        },
    )

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFF4FBF8),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFF4FBF8), Color(0xFFE9F8F2), Color(0xFFF9FDFB)),
                    ),
                )
                .pullRefresh(pullRefreshState),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    FreshHeader(
                        query = uiState.query,
                        selectedPlatform = selectedPlatform,
                        onQueryChanged = viewModel::onQueryChanged,
                        onRefresh = {
                            viewModel.refresh()
                            rooms.refresh()
                        },
                    )
                }

                if (!uiState.networkConnected) {
                    item {
                        Text(
                            text = stringResource(id = R.string.network_lost),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(10.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }

                item {
                    PlatformSection(
                        selectedPlatform = selectedPlatform,
                        platformCount = uiState.platforms.size,
                        onOpenSelector = {
                            platformSearch = ""
                            showPlatformSheet = true
                        },
                    )
                }

                item {
                    RoomsSectionTitle(selectedPlatform = selectedPlatform)
                }

                when {
                    uiState.isLoadingPlatforms && uiState.platforms.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    uiState.platforms.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = stringResource(id = R.string.empty_platforms))
                            }
                        }
                    }

                    else -> {
                        roomsListItems(rooms, onRoomClick)
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    if (showPlatformSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val filteredPlatforms = uiState.platforms.filter {
            if (platformSearch.isBlank()) return@filter true
            it.title.contains(platformSearch, ignoreCase = true)
        }

        ModalBottomSheet(
            onDismissRequest = { showPlatformSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(id = R.string.select_platform),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${filteredPlatforms.size}/${uiState.platforms.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF4B7F71),
                    )
                }
                OutlinedTextField(
                    value = platformSearch,
                    onValueChange = { platformSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(id = R.string.platform_search_hint)) },
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (filteredPlatforms.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(id = R.string.empty_platforms),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        )
                    }
                } else {
                    items(
                        items = filteredPlatforms,
                        key = { it.address },
                    ) { platform ->
                        PlatformListItem(
                            platform = platform,
                            selected = platform.address == uiState.selectedPlatformAddress,
                            onClick = {
                                viewModel.onPlatformSelected(platform.address)
                                showPlatformSheet = false
                            },
                        )
                    }
                }
            }
        }
    }

    uiState.availableUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = {
                Text(text = stringResource(id = R.string.update_available_title))
            },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.update_available_message,
                        update.versionName,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissUpdateDialog()
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)),
                        )
                    },
                ) {
                    Text(text = stringResource(id = R.string.update_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                    Text(text = stringResource(id = R.string.close))
                }
            },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.roomsListItems(
    rooms: androidx.paging.compose.LazyPagingItems<LiveRoom>,
    onRoomClick: (String) -> Unit,
) {
    val refreshState = rooms.loadState.refresh

    when {
        refreshState is LoadState.Loading && rooms.itemCount == 0 -> {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        refreshState is LoadState.Error && rooms.itemCount == 0 -> {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = refreshState.error.message ?: stringResource(id = R.string.play_failed))
                }
            }
        }

        rooms.itemCount == 0 -> {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(id = R.string.empty_text))
                }
            }
        }

        else -> {
            items(
                count = rooms.itemCount,
                key = { index -> rooms[index]?.id ?: index },
            ) { index ->
                rooms[index]?.let { room ->
                    LiveRoomCard(room = room, onClick = { onRoomClick(room.id) })
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun FreshHeader(
    query: String,
    selectedPlatform: LivePlatform?,
    onQueryChanged: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF81E6D9), Color(0xFFA7F3D0), Color(0xFFDBFCE7)),
                ),
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = selectedPlatform?.title ?: stringResource(id = R.string.select_platform),
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF064E3B),
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF065F46))
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            placeholder = { Text(stringResource(id = R.string.search_hint)) },
        )
    }
}

@Composable
private fun PlatformSection(
    selectedPlatform: LivePlatform?,
    platformCount: Int,
    onOpenSelector: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFD8ECE4)),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = selectedPlatform?.iconUrl,
                contentDescription = selectedPlatform?.title,
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedPlatform?.title ?: stringResource(id = R.string.select_platform),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (selectedPlatform == null) {
                        stringResource(id = R.string.select_platform_hint)
                    } else {
                        "${stringResource(id = R.string.online_count, selectedPlatform.onlineCount)}  ·  ${stringResource(id = R.string.platform_section_title)} $platformCount"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5F8B7E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onOpenSelector) {
                Text(text = stringResource(id = R.string.switch_platform))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF167A62),
                )
            }
        }
    }
}

@Composable
private fun PlatformListItem(
    platform: LivePlatform,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFFD1FAE5) else Color.White,
        border = BorderStroke(
            width = if (selected) 1.4.dp else 1.dp,
            color = if (selected) Color(0xFF34D399) else Color(0xFFDCEDE7),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = platform.iconUrl,
                contentDescription = platform.title,
                modifier = Modifier.size(26.dp),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = platform.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(id = R.string.online_count, platform.onlineCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF0F766E),
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(id = R.string.selected),
                    tint = Color(0xFF059669),
                )
            }
        }
    }
}

@Composable
private fun RoomsSectionTitle(selectedPlatform: LivePlatform?) {
    Text(
        text = if (selectedPlatform == null) {
            stringResource(id = R.string.rooms_section_title)
        } else {
            "${selectedPlatform.title} · ${stringResource(id = R.string.rooms_section_title)}"
        },
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun LiveRoomCard(
    room: LiveRoom,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFD8E9E2)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsyncImage(
                model = room.coverUrl,
                contentDescription = room.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = room.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = room.platformIconUrl,
                            contentDescription = room.platformTitle,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(text = room.platformTitle, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = stringResource(id = R.string.online_count, room.viewerCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF0F766E),
                    )
                }
            }
            Divider(modifier = Modifier.padding(horizontal = 12.dp))
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
