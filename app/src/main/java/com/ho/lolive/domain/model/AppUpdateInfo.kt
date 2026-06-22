package com.ho.lolive.domain.model

data class AppUpdateInfo(
    val versionName: String,
    val versionCode: Int?,
    val releaseUrl: String,
)
