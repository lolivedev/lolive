package com.ho.lolive.data.local

import android.content.Context
import com.ho.lolive.domain.model.AppUpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 持久化用户「稍后/关闭」过的更新版本，避免自动检查反复弹窗。
 * 手动检查更新不受此限制。
 */
@Singleton
class UpdatePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isVersionIgnored(update: AppUpdateInfo): Boolean {
        return prefs.getString(KEY_IGNORED_VERSION, null) == versionKey(update)
    }

    fun ignoreVersion(update: AppUpdateInfo) {
        prefs.edit()
            .putString(KEY_IGNORED_VERSION, versionKey(update))
            .apply()
    }

    private fun versionKey(update: AppUpdateInfo): String {
        return "${update.versionCode ?: -1}|${update.versionName.trim()}"
    }

    private companion object {
        const val PREFS_NAME = "lolive_prefs"
        const val KEY_IGNORED_VERSION = "ignored_update_version"
    }
}
