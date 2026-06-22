package com.ho.lolive.domain.usecase

import androidx.paging.PagingData
import com.google.common.truth.Truth.assertThat
import com.ho.lolive.core.common.AppResult
import com.ho.lolive.domain.model.LivePlatform
import com.ho.lolive.domain.model.LiveRoom
import com.ho.lolive.domain.model.LiveRoomDetail
import com.ho.lolive.domain.model.Quality
import com.ho.lolive.domain.repository.LiveRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RefreshRoomsUseCaseTest {

    private val platform = LivePlatform(
        title = "Test Platform",
        address = "test-platform",
        iconUrl = "https://example.com/icon.png",
        onlineCount = 100,
    )

    @Test
    fun `invoke returns success when repository succeeds`() = runTest {
        val repository = FakeLiveRepository(refreshResult = AppResult.Success(Unit))
        val useCase = RefreshRoomsUseCase(repository)

        val result = useCase(platform)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
    }

    @Test
    fun `invoke returns error when repository fails`() = runTest {
        val repository = FakeLiveRepository(
            refreshResult = AppResult.Error(IllegalStateException("network fail")),
        )
        val useCase = RefreshRoomsUseCase(repository)

        val result = useCase(platform)

        assertThat(result).isInstanceOf(AppResult.Error::class.java)
    }
}

class GetRoomDetailUseCaseTest {

    @Test
    fun `invoke returns room detail`() = runTest {
        val room = LiveRoomDetail(
            id = "room-1",
            title = "test room",
            streamUrl = "https://example.com/live.m3u8",
            qualities = listOf(Quality("720p", "https://example.com/live.m3u8")),
            drmLicenseUrl = null,
        )
        val repository = FakeLiveRepository(detailResult = AppResult.Success(room))
        val useCase = GetRoomDetailUseCase(repository)

        val result = useCase("room-1")

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat((result as AppResult.Success).data.id).isEqualTo("room-1")
    }
}

private class FakeLiveRepository(
    private val platformsResult: AppResult<List<LivePlatform>> = AppResult.Success(emptyList()),
    private val refreshResult: AppResult<Unit> = AppResult.Success(Unit),
    private val detailResult: AppResult<LiveRoomDetail> = AppResult.Error(Throwable("missing")),
) : LiveRepository {
    override fun observePagedRooms(query: String): Flow<PagingData<LiveRoom>> = flowOf(PagingData.empty())

    override suspend fun getPlatforms(): AppResult<List<LivePlatform>> = platformsResult

    override suspend fun refreshRooms(platform: LivePlatform): AppResult<Unit> = refreshResult

    override suspend fun getRoomDetail(roomId: String): AppResult<LiveRoomDetail> = detailResult

    override suspend fun getPreviousRoomId(roomId: String): String? = null

    override suspend fun getNextRoomId(roomId: String): String? = null
}
