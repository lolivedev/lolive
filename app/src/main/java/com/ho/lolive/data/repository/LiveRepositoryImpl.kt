package com.ho.lolive.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.ho.lolive.core.common.AppResult
import com.ho.lolive.core.common.Logger
import com.ho.lolive.core.nativebridge.NativeEndpointBridge
import com.ho.lolive.data.local.dao.LiveRoomDao
import com.ho.lolive.data.local.entity.LiveRoomEntity
import com.ho.lolive.data.remote.GithubApiService
import com.ho.lolive.data.remote.LiveApiService
import com.ho.lolive.data.remote.dto.GithubReleaseResponse
import com.ho.lolive.domain.model.AppUpdateInfo
import com.ho.lolive.domain.model.LivePlatform
import com.ho.lolive.domain.model.LiveRoom
import com.ho.lolive.domain.model.LiveRoomDetail
import com.ho.lolive.domain.repository.LiveRepository
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class LiveRepositoryImpl @Inject constructor(
    private val apiService: LiveApiService,
    private val githubApiService: GithubApiService,
    private val liveRoomDao: LiveRoomDao,
) : LiveRepository {

    override fun observePagedRooms(query: String): Flow<PagingData<LiveRoom>> {
        val escapedQuery = escapeForLikeQuery(query.trim())
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                initialLoadSize = 20,
                prefetchDistance = 3,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { liveRoomDao.pagingSource(escapedQuery) },
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun getPlatforms(): AppResult<List<LivePlatform>> {
        return try {
            val platforms = apiService.getPlatforms(NativeEndpointBridge.platformsUrl()).platforms
                .mapNotNull { platform ->
                    val decodedTitle = platform.title.decodeDisplayText()
                    if (isBlockedPlatformTitle(decodedTitle)) return@mapNotNull null
                    LivePlatform(
                        title = decodedTitle,
                        address = platform.address,
                        iconUrl = platform.iconUrl.normalizeImageUrl(),
                        onlineCount = platform.onlineCount.toIntOrNull() ?: 0,
                    )
                }
            AppResult.Success(platforms)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            Logger.e("getPlatforms failed", throwable)
            AppResult.Error(throwable)
        }
    }

    override suspend fun refreshRooms(platform: LivePlatform): AppResult<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val roomEntities = apiService
                .getPlatformRooms(NativeEndpointBridge.platformRoomsUrl(platform.address))
                .anchors
                .mapIndexed { index, anchor ->
                    val rawTitle = anchor.title.trim()
                    val rawStreamUrl = anchor.streamUrl.trim()
                    val streamKey = rawStreamUrl.ifBlank { rawTitle }
                    LiveRoomEntity(
                        id = "${platform.address}_${rawTitle}_${streamKey}",
                        title = rawTitle.decodeDisplayText(),
                        coverUrl = anchor.coverUrl.normalizeImageUrl(),
                        streamUrl = rawStreamUrl,
                        platformTitle = platform.title,
                        platformIconUrl = platform.iconUrl,
                        viewerCount = 0, // API does not provide per-room viewer count
                        // Keep list order stable and aligned with UI order for prev/next switching.
                        updatedAt = now - index,
                    )
                }

            // Only replace when the upstream returned rooms; otherwise keep the cached list so an
            // intermittent empty response (API hiccup) does not wipe the platform's rooms.
            if (roomEntities.isNotEmpty()) {
                liveRoomDao.replacePlatformRooms(platform.title, roomEntities)
            }
            AppResult.Success(Unit)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            Logger.e("refreshRooms failed for ${platform.title}", throwable)
            AppResult.Error(throwable)
        }
    }

    override suspend fun getRoomDetail(roomId: String): AppResult<LiveRoomDetail> {
        return try {
            val entity = liveRoomDao.findById(roomId)
                ?: return AppResult.Error(IllegalArgumentException("room not found"))
            AppResult.Success(entity.toDetail())
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            AppResult.Error(throwable)
        }
    }

    override suspend fun getPreviousRoomId(roomId: String): String? {
        return try {
            val current = liveRoomDao.findById(roomId) ?: return null
            // Cycle to the oldest room when the current one is the newest in the platform.
            liveRoomDao.findPreviousRoomId(current.platformTitle, current.updatedAt)
                ?: liveRoomDao.findOldestRoomId(current.platformTitle)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            Logger.e("getPreviousRoomId failed", throwable)
            null
        }
    }

    override suspend fun getNextRoomId(roomId: String): String? {
        return try {
            val current = liveRoomDao.findById(roomId) ?: return null
            // Cycle to the newest room when the current one is the oldest in the platform.
            liveRoomDao.findNextRoomId(current.platformTitle, current.updatedAt)
                ?: liveRoomDao.findNewestRoomId(current.platformTitle)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            Logger.e("getNextRoomId failed", throwable)
            null
        }
    }

    override suspend fun getLatestRelease(): AppResult<AppUpdateInfo> {
        return try {
            val release = githubApiService.getLatestRelease(LATEST_RELEASE_URL)
            AppResult.Success(release.toUpdateInfo())
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            Logger.e("getLatestRelease failed", throwable)
            AppResult.Error(throwable)
        }
    }

    private fun GithubReleaseResponse.toUpdateInfo(): AppUpdateInfo {
        val normalizedTag = tagName.trim()
        val versionName = normalizedTag
            .removePrefix("v")
            .substringBefore("-build.")
            .ifBlank { normalizedTag.removePrefix("v") }
            .ifBlank { "latest" }
        val versionCode = Regex("""-build\.(\d+)$""")
            .find(normalizedTag)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        return AppUpdateInfo(
            versionName = versionName,
            versionCode = versionCode,
            releaseUrl = htmlUrl.ifBlank { DEFAULT_RELEASE_WEB_URL },
        )
    }

    private fun String.normalizeImageUrl(): String {
        val value = trim()
        if (value.isEmpty()) return value
        return when {
            value.startsWith("//") -> "https:$value"
            else -> value
        }
    }

    private val urlEncodedPattern = Regex("""%[0-9a-fA-F]{2}""")

    private fun String.decodeDisplayText(): String {
        val value = trim()
        if (value.isEmpty() || !urlEncodedPattern.containsMatchIn(value)) return value
        return try {
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        } catch (_: IllegalArgumentException) {
            value
        }
    }

    private fun isBlockedPlatformTitle(title: String): Boolean {
        val normalized = normalizePlatformTitle(title)
        return BLOCKED_PLATFORM_TITLES.any { normalizePlatformTitle(it) == normalized }
    }

    private fun normalizePlatformTitle(title: String): String {
        return title.trim().replace(WHITESPACE_REGEX, "")
    }

    private fun escapeForLikeQuery(query: String): String {
        if (query.isEmpty()) return query
        val builder = StringBuilder(query.length)
        for (ch in query) {
            when (ch) {
                '\\', '%', '_' -> {
                    builder.append('\\')
                    builder.append(ch)
                }
                else -> builder.append(ch)
            }
        }
        return builder.toString()
    }

    private companion object {
        val WHITESPACE_REGEX = Regex("\\s+")
        val BLOCKED_PLATFORM_TITLES = arrayOf(
            "卫视直播",
            "映客",
        )
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/lolivedev/lolive/releases/latest"
        const val DEFAULT_RELEASE_WEB_URL =
            "https://github.com/lolivedev/lolive/releases/latest"
    }
}
