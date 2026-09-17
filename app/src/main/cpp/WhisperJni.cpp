#include <jni.h>
#include <atomic>
#include <algorithm>
#include <string>

#ifdef WHISPER_AVAILABLE
#include "whisper.h"
#endif

namespace {

#ifdef WHISPER_AVAILABLE

struct NativeWhisperContext {
    whisper_context * ctx = nullptr;
    std::atomic<bool> cancelRequested{false};
};

bool abortCallback(void * userData) {
    auto * state = static_cast<NativeWhisperContext *>(userData);
    return state != nullptr && state->cancelRequested.load(std::memory_order_relaxed);
}

NativeWhisperContext * fromHandle(jlong handle) {
    return reinterpret_cast<NativeWhisperContext *>(handle);
}

#endif

std::string missingMessage() {
    return "Whisper native backend is not provisioned. Run scripts/setup_whisper.sh.";
}

} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_jarvis_ai_voice_WhisperNative_initContext(
        JNIEnv * env, jobject /* thiz */, jstring modelPath) {
#ifdef WHISPER_AVAILABLE
    if (modelPath == nullptr) return 0;

    const char * path = env->GetStringUTFChars(modelPath, nullptr);
    if (path == nullptr) return 0;

    auto * native = new NativeWhisperContext();
    auto params = whisper_context_default_params();
    params.use_gpu = false; // CPU is the guaranteed Android path in phase 1.
    native->ctx = whisper_init_from_file_with_params(path, params);

    env->ReleaseStringUTFChars(modelPath, path);

    if (native->ctx == nullptr) {
        delete native;
        return 0;
    }
    return reinterpret_cast<jlong>(native);
#else
    (void) env;
    (void) modelPath;
    return 0;
#endif
}

extern "C" JNIEXPORT void JNICALL
Java_com_jarvis_ai_voice_WhisperNative_freeContext(
        JNIEnv * /* env */, jobject /* thiz */, jlong contextHandle) {
#ifdef WHISPER_AVAILABLE
    auto * native = fromHandle(contextHandle);
    if (native == nullptr) return;

    if (native->ctx != nullptr) {
        whisper_free(native->ctx);
        native->ctx = nullptr;
    }
    delete native;
#else
    (void) contextHandle;
#endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_jarvis_ai_voice_WhisperNative_transcribeArabic(
        JNIEnv * env,
        jobject /* thiz */,
        jlong contextHandle,
        jfloatArray audio,
        jint numThreads) {
#ifdef WHISPER_AVAILABLE
    auto * native = fromHandle(contextHandle);
    if (native == nullptr || native->ctx == nullptr || audio == nullptr) {
        return env->NewStringUTF("");
    }

    native->cancelRequested.store(false, std::memory_order_relaxed);

    const jsize sampleCount = env->GetArrayLength(audio);
    if (sampleCount <= 0) {
        return env->NewStringUTF("");
    }

    jboolean isCopy = JNI_FALSE;
    jfloat * samples = env->GetFloatArrayElements(audio, &isCopy);
    if (samples == nullptr) {
        return env->NewStringUTF("");
    }

    whisper_full_params params =
        whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.no_timestamps = true;
    params.no_context = true;
    params.single_segment = false;
    params.translate = false;
    params.language = "ar";
    params.n_threads = std::clamp(static_cast<int>(numThreads), 1, 8);
    params.temperature = 0.0f;
    params.temperature_inc = 0.0f;
    params.abort_callback = abortCallback;
    params.abort_callback_user_data = native;

    const int rc = whisper_full(
        native->ctx,
        params,
        samples,
        static_cast<int>(sampleCount)
    );

    env->ReleaseFloatArrayElements(audio, samples, JNI_ABORT);

    if (rc != 0 || native->cancelRequested.load(std::memory_order_relaxed)) {
        return env->NewStringUTF("");
    }

    const int segmentCount = whisper_full_n_segments(native->ctx);
    std::string transcript;
    transcript.reserve(static_cast<size_t>(segmentCount) * 24);

    for (int i = 0; i < segmentCount; ++i) {
        const char * text = whisper_full_get_segment_text(native->ctx, i);
        if (text != nullptr) transcript += text;
    }

    return env->NewStringUTF(transcript.c_str());
#else
    (void) env;
    (void) contextHandle;
    (void) audio;
    (void) numThreads;
    return env->NewStringUTF(missingMessage().c_str());
#endif
}

extern "C" JNIEXPORT void JNICALL
Java_com_jarvis_ai_voice_WhisperNative_cancel(
        JNIEnv * /* env */, jobject /* thiz */, jlong contextHandle) {
#ifdef WHISPER_AVAILABLE
    auto * native = fromHandle(contextHandle);
    if (native != nullptr) {
        native->cancelRequested.store(true, std::memory_order_relaxed);
    }
#else
    (void) contextHandle;
#endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_jarvis_ai_voice_WhisperNative_systemInfo(
        JNIEnv * env, jobject /* thiz */) {
#ifdef WHISPER_AVAILABLE
    return env->NewStringUTF(whisper_print_system_info());
#else
    return env->NewStringUTF(missingMessage().c_str());
#endif
}
