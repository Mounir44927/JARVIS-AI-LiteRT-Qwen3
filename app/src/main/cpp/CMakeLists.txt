cmake_minimum_required(VERSION 3.22.1)
project(jarvis_whisper)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(WHISPER_SOURCE_DIR
    "${CMAKE_CURRENT_SOURCE_DIR}/third_party/whisper.cpp"
    CACHE PATH "Pinned whisper.cpp source directory"
)

# Keep the native build CPU-first and Android-friendly. GPU/NPU work is deliberately deferred.
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
set(WHISPER_COREML OFF CACHE BOOL "" FORCE)

add_library(jarvis_whisper SHARED WhisperJni.cpp)

if(EXISTS "${WHISPER_SOURCE_DIR}/CMakeLists.txt")
    add_subdirectory("${WHISPER_SOURCE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/whisper-build")
    target_compile_definitions(jarvis_whisper PRIVATE WHISPER_AVAILABLE=1)
    target_include_directories(jarvis_whisper PRIVATE
        "${WHISPER_SOURCE_DIR}/include"
        "${WHISPER_SOURCE_DIR}/ggml/include"
        "${WHISPER_SOURCE_DIR}/ggml/src"
    )
    target_link_libraries(jarvis_whisper PRIVATE whisper)
else()
    message(WARNING
        "Pinned whisper.cpp source is not present. Building a stub native library. "
        "Run scripts/setup_whisper.sh before a real STT build."
    )
endif()

if(ANDROID)
    find_library(log-lib log)
    find_library(android-lib android)

    target_link_libraries(jarvis_whisper
        ${log-lib}
        ${android-lib}
    )
endif()
