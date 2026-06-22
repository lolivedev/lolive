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
import com.ho.lolive.data.remote.LiveApiService
import com.ho.lolive.domain.model.LivePlatform
import com.ho.lolive.domain.model.LiveRoom
import com.ho.lolive.domain.model.LiveRoomDetail
import com.ho.lolive.domain.repository.LiveRepository
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class LiveRepositoryImpl @Inject constructor(
    private val apiService: LiveApiService,
    private val liveRoomDao: LiveRoomDao,
) : LiveRepository {

    override fun observePagedRooms(query: String): Flow<PagingData<LiveRoom>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                initialLoadSize = 20,
                prefetchDistance = 3,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { liveRoomDao.pagingSource(query) },
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
                    val streamKey = anchor.streamUrl.trim().ifBlank { rawTitle }
                    LiveRoomEntity(
                        id = "${platform.address}_${rawTitle}_${streamKey}",
                        title = rawTitle.decodeDisplayText(),
                        coverUrl = anchor.coverUrl.normalizeImageUrl(),
                        streamUrl = anchor.streamUrl,
                        platformTitle = platform.title,
                        platformIconUrl = platform.iconUrl,
                        viewerCount = 0, // API does not provide per-room viewer count
                        // Keep list order stable and aligned with UI order for prev/next switching.
                        updatedAt = now - index,
                    )
                }

            liveRoomDao.replaceAll(roomEntities)
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
        val current = liveRoomDao.findById(roomId) ?: return null
        val ids = liveRoomDao.orderedRoomIdsByPlatform(current.platformTitle)
        if (ids.isEmpty()) return null
        val index = ids.indexOf(roomId)
        if (index < 0) return null
        return ids[(index - 1 + ids.size) % ids.size]
    }

    override suspend fun getNextRoomId(roomId: String): String? {
        val current = liveRoomDao.findById(roomId) ?: return null
        val ids = liveRoomDao.orderedRoomIdsByPlatform(current.platformTitle)
        if (ids.isEmpty()) return null
        val index = ids.indexOf(roomId)
        if (index < 0) return null
        return ids[(index + 1) % ids.size]
    }

    private fun String.normalizeImageUrl(): String {
        val value = trim()
        if (value.isEmpty()) return value
        return when {
            value.startsWith("//") -> "https:$value"
            else -> value
        }
    }

    private fun String.decodeDisplayText(): String {
        val value = trim()
        if (value.isEmpty()) return value
        if (!value.contains('%') && !value.contains('+')) return value
        return try {
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        } catch (_: IllegalArgumentException) {
            value
        }
    }

    private fun isBlockedPlatformTitle(title: String): Boolean {
        val normalizedCandidates = setOf(
            normalizePlatformTitle(title),
            normalizePlatformTitle(title.toUtf8Text(GBK_CHARSET)),
            normalizePlatformTitle(title.toUtf8Text(StandardCharsets.ISO_8859_1)),
        )
        return BLOCKED_PLATFORM_TITLES.any { blockedTitle ->
            normalizePlatformTitle(blockedTitle) in normalizedCandidates
        }
    }

    private fun normalizePlatformTitle(title: String): String {
        return title.trim().replace(WHITESPACE_REGEX, "")
    }

    private fun String.toUtf8Text(sourceCharset: Charset): String {
        return try {
            String(toByteArray(sourceCharset), StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            this
        }
    }

    private companion object {
        val WHITESPACE_REGEX = Regex("\\s+")
        val BLOCKED_PLATFORM_TITLES = arrayOf(
            "卫视直播",
            "映客",
        )
        val GBK_CHARSET: Charset = Charset.forName("GBK")
    }
}
