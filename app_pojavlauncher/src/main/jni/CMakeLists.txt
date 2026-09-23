cmake_minimum_required(VERSION 3.22.1)
project("LELauncherNative")

set(LE_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../../../")
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

include(FetchContent)

function(le_ensure_dependency name repo tag local_dir)
    if(EXISTS "${LE_ROOT}/${local_dir}/CMakeLists.txt")
        add_subdirectory("${LE_ROOT}/${local_dir}" "${CMAKE_BINARY_DIR}/${name}")
    else()
        FetchContent_Declare(
            ${name}
            GIT_REPOSITORY ${repo}
            GIT_TAG ${tag}
            GIT_SHALLOW FALSE
        )
        FetchContent_MakeAvailable(${name})
    endif()
endfunction()

# Modern Minecraft 26.x uses SDL3 for windowing/input/platform integration.
# Keep Mojo's Android-native compatibility components alongside LE's existing
# GLFW bridge so older versions continue to have their native path.
le_ensure_dependency(
    mojoexec
    https://github.com/MojoLauncher/mojoexec.git
    5a60f9ebbd881ef7b73da4d0b20e158411ffde51
    mojoexec
)

set(GLFW_BUILD_WAYLAND OFF CACHE BOOL "" FORCE)
set(GLFW_BUILD_X11 OFF CACHE BOOL "" FORCE)
set(GLFW_BUILD_ANDROID ON CACHE BOOL "" FORCE)
set(GLFW_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GLFW_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GLFW_INSTALL OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)

le_ensure_dependency(
    glfw
    https://github.com/MojoLauncher/glfw.git
    7900e19fd2dbcad5abb4affc3b9d56c27e164c59
    glfw
)

if(TARGET glfw)
    target_compile_options(glfw PRIVATE -Wno-nullability-completeness)
endif()

set(SDL_SHARED ON CACHE BOOL "" FORCE)
set(SDL_VIDEO ON CACHE BOOL "" FORCE)
set(SDL_AUDIO ON CACHE BOOL "" FORCE)
set(SDL_STATIC OFF CACHE BOOL "" FORCE)
set(SDL_CAMERA OFF CACHE BOOL "" FORCE)
set(SDL_TESTS OFF CACHE BOOL "" FORCE)
set(SDL_EXAMPLES OFF CACHE BOOL "" FORCE)
set(SDL3_MAINPROJECT OFF CACHE BOOL "" FORCE)

le_ensure_dependency(
    sdl
    https://github.com/MojoLauncher/MojoSDL.git
    b231161d33a4663e4799fdd9d64aa603e6612735
    sdl
)

if(TARGET SDL3-shared AND TARGET mojoexec)
    target_link_libraries(SDL3-shared PRIVATE mojoexec)
endif()

find_package(bytehook REQUIRED CONFIG)

set(LE_JNI "${CMAKE_CURRENT_LIST_DIR}")

add_library(pojavexec SHARED
    bigcoreaffinity.c
    egl_bridge.c
    ctxbridges/loader_dlopen.c
    ctxbridges/gl_bridge.c
    ctxbridges/osm_bridge.c
    ctxbridges/egl_loader.c
    ctxbridges/osmesa_loader.c
    ctxbridges/swap_interval_no_egl.c
    environ/environ.c
    jvm_hooks/emui_iterator_fix_hook.c
    jvm_hooks/java_exec_hooks.c
    jvm_hooks/lwjgl_dlopen_hook.c
    input_bridge_v3.c
    jre_launcher.c
    utils.c
    stdio_is.c
    driver_helper/nsbypass.c
)

target_include_directories(pojavexec PRIVATE "${LE_JNI}")
target_link_libraries(pojavexec PRIVATE log android dl)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_definitions(pojavexec PRIVATE ADRENO_POSSIBLE)
endif()

add_library(exithook SHARED
    native_hooks/exit_hook.c
    native_hooks/chmod_hook.c
    native_hooks/sdl_hook.c
)
target_include_directories(exithook PRIVATE "${LE_JNI}")
target_link_libraries(exithook PRIVATE bytehook::bytehook pojavexec log dl)

# mojoexec provides linkerhook on arm64. Keep LE's legacy linkerhook output for 32-bit.
if(NOT ANDROID_ABI STREQUAL "arm64-v8a")
    add_library(le_linkerhook SHARED driver_helper/hook.c)
    set_target_properties(le_linkerhook PROPERTIES OUTPUT_NAME "linkerhook")
    target_link_options(le_linkerhook PRIVATE "-Wl,-z,global")
endif()

add_library(pojavexec_awt SHARED awt_bridge.c)
target_include_directories(pojavexec_awt PRIVATE "${LE_JNI}")
target_link_libraries(pojavexec_awt PRIVATE log dl)

add_library(awt_headless SHARED awt_xawt/xawt_fake.c)
target_include_directories(awt_headless PRIVATE "${LE_JNI}/awt_xawt")

add_library(awt_xawt SHARED awt_xawt/xawt_fake.c)
target_link_libraries(awt_xawt PRIVATE awt_headless)

# Match the legacy ndk-build ABI behavior.
foreach(target pojavexec exithook pojavexec_awt awt_headless awt_xawt)
    target_compile_options(${target} PRIVATE -Wno-nullability-completeness)
endforeach()
