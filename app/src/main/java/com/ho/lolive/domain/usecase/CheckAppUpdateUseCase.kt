package com.ho.lolive.domain.usecase

import com.ho.lolive.core.common.AppResult
import com.ho.lolive.domain.model.AppUpdateInfo
import com.ho.lolive.domain.repository.LiveRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class CheckAppUpdateUseCase @Inject constructor(
    private val repository: LiveRepository,
) {
    suspend operator fun invoke(
        currentVersionName: String,
        currentVersionCode: Int,
    ): AppResult<AppUpdateInfo?> {
        return try {
            when (val result = repository.getLatestRelease()) {
                is AppResult.Success -> {
                    val latest = result.data
                    val hasUpdate = isLatestNewer(
                        latest = latest,
                        currentVersionName = currentVersionName,
                        currentVersionCode = currentVersionCode,
                    )
                    AppResult.Success(if (hasUpdate) latest else null)
                }
                is AppResult.Error -> AppResult.Error(result.throwable, result.message)
                AppResult.Loading -> AppResult.Loading
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            AppResult.Error(throwable)
        }
    }

    private fun isLatestNewer(
        latest: AppUpdateInfo,
        currentVersionName: String,
        currentVersionCode: Int,
    ): Boolean {
        val latestCode = latest.versionCode
        if (latestCode != null) {
            if (latestCode > currentVersionCode) return true
            if (latestCode < currentVersionCode) return false
        }
        return compareVersionName(latest.versionName, currentVersionName) > 0
    }

    private fun compareVersionName(left: String, right: String): Int {
        val leftParts = extractVersionNumbers(left)
        val rightParts = extractVersionNumbers(right)

        // If neither version contains digits, fall back to string comparison
        if (leftParts.isEmpty() && rightParts.isEmpty()) {
            return left.compareTo(right, ignoreCase = true)
        }

        val maxSize = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until maxSize) {
            val l = leftParts.getOrElse(index) { 0 }
            val r = rightParts.getOrElse(index) { 0 }

            if (l != r) return l.compareTo(r)
        }
        return 0
    }

    private fun extractVersionNumbers(version: String): List<Int> {
        // Strip pre-release/build suffixes so e.g. "1.21-beta.3" only yields [1, 21] and does not
        // treat the trailing beta number as a version segment.
        val core = version.substringBefore('-').substringBefore('+')
        return Regex("""\d+""").findAll(core).map { it.value.toInt() }.toList()
    }
}
