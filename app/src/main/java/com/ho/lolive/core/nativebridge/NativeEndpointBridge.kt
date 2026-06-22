package com.ho.lolive.core.nativebridge

object NativeEndpointBridge {
    init {
        System.loadLibrary("lolive_native")
    }

    // The decoded endpoints are constants; cache them so each URL build does not cross JNI again.
    private val baseUrl by lazy { nativeBaseUrl() }
    private val platformsPath by lazy { nativePlatformsPath() }
    private val roomsPathPrefix by lazy { nativeRoomsPathPrefix() }

    @JvmStatic
    external fun nativeBaseUrl(): String

    @JvmStatic
    external fun nativePlatformsPath(): String

    @JvmStatic
    external fun nativeRoomsPathPrefix(): String

    fun platformsUrl(): String = baseUrl + platformsPath

    fun platformRoomsUrl(address: String): String {
        val normalizedAddress = address.trim().removePrefix("/")
        return baseUrl + roomsPathPrefix + normalizedAddress
    }
}
