package com.ho.lolive.core.nativebridge

object NativeEndpointBridge {
    init {
        System.loadLibrary("lolive_native")
    }

    @JvmStatic
    external fun nativeBaseUrl(): String

    @JvmStatic
    external fun nativePlatformsPath(): String

    @JvmStatic
    external fun nativeRoomsPathPrefix(): String

    fun platformsUrl(): String = nativeBaseUrl() + nativePlatformsPath()

    fun platformRoomsUrl(address: String): String {
        val normalizedAddress = address.trim().removePrefix("/")
        return nativeBaseUrl() + nativeRoomsPathPrefix() + normalizedAddress
    }
}
