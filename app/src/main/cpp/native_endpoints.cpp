#include <jni.h>
#include <cstdint>
#include <string>

namespace {
constexpr uint8_t kXorKey = 0x37;

constexpr uint8_t kBaseUrlEncoded[] = {
    0x5F, 0x43, 0x43, 0x47, 0x0D, 0x18, 0x18, 0x56, 0x47, 0x5E, 0x19, 0x5F,
    0x54, 0x5B, 0x4E, 0x4D, 0x19, 0x54, 0x58, 0x5A, 0x0D, 0x0F, 0x06, 0x18
};

constexpr uint8_t kPlatformsPathEncoded[] = {
    0x5A, 0x51, 0x18, 0x5D, 0x44, 0x58, 0x59, 0x19, 0x43, 0x4F, 0x43
};

constexpr uint8_t kRoomsPathPrefixEncoded[] = {
    0x5A, 0x51, 0x18
};

std::string decode(const uint8_t* data, size_t len) {
    std::string out;
    out.reserve(len);
    for (size_t i = 0; i < len; ++i) {
        out.push_back(static_cast<char>(data[i] ^ kXorKey));
    }
    return out;
}
}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_ho_lolive_core_nativebridge_NativeEndpointBridge_nativeBaseUrl(
    JNIEnv* env,
    jclass /* clazz */
) {
    const std::string value = decode(kBaseUrlEncoded, sizeof(kBaseUrlEncoded));
    return env->NewStringUTF(value.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ho_lolive_core_nativebridge_NativeEndpointBridge_nativePlatformsPath(
    JNIEnv* env,
    jclass /* clazz */
) {
    const std::string value = decode(kPlatformsPathEncoded, sizeof(kPlatformsPathEncoded));
    return env->NewStringUTF(value.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ho_lolive_core_nativebridge_NativeEndpointBridge_nativeRoomsPathPrefix(
    JNIEnv* env,
    jclass /* clazz */
) {
    const std::string value = decode(kRoomsPathPrefixEncoded, sizeof(kRoomsPathPrefixEncoded));
    return env->NewStringUTF(value.c_str());
}
