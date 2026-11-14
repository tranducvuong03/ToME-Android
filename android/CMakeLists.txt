cmake_minimum_required(VERSION 3.22)
project(te4_android LANGUAGES C CXX)
set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT ANDROID)
    message(FATAL_ERROR "This project is intended to be built with the Android NDK.")
endif()

if(NOT DEFINED TE4_ROOT)
    get_filename_component(TE4_ROOT "${CMAKE_CURRENT_LIST_DIR}/.." ABSOLUTE)
endif()
if(NOT DEFINED SDL2_SOURCE_DIR OR NOT EXISTS "${SDL2_SOURCE_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "SDL2_SOURCE_DIR must point to the checked-out SDL2 source.")
endif()

include(FetchContent)
list(PREPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_LIST_DIR}/cmake")

set(SDL_SHARED ON CACHE BOOL "" FORCE)
set(SDL_STATIC OFF CACHE BOOL "" FORCE)
set(SDL_DISABLE_INSTALL ON CACHE BOOL "" FORCE)
add_subdirectory("${SDL2_SOURCE_DIR}" "${CMAKE_BINARY_DIR}/SDL2" EXCLUDE_FROM_ALL)

set(BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)
set(SDL2IMAGE_DEPS_SHARED OFF CACHE BOOL "" FORCE)
set(SDL2IMAGE_VENDORED ON CACHE BOOL "" FORCE)
set(SDL2IMAGE_INSTALL OFF CACHE BOOL "" FORCE)
set(SDL2IMAGE_SAMPLES OFF CACHE BOOL "" FORCE)
set(SDL2IMAGE_TESTS OFF CACHE BOOL "" FORCE)
set(SDL2IMAGE_BACKEND_STB OFF CACHE BOOL "" FORCE)
set(SDL2IMAGE_PNG ON CACHE BOOL "" FORCE)
set(SDL2IMAGE_PNG_SHARED OFF CACHE BOOL "" FORCE)
set(PNG_SHARED OFF CACHE BOOL "" FORCE)
set(PNG_STATIC ON CACHE BOOL "" FORCE)
set(PNG_EXECUTABLES OFF CACHE BOOL "" FORCE)
set(PNG_TESTS OFF CACHE BOOL "" FORCE)
set(SKIP_INSTALL_ALL ON CACHE BOOL "" FORCE)
set(SDL2IMAGE_AVIF OFF CACHE BOOL "" FORCE)
set(SDL2IMAGE_JPG OFF CACHE BOOL "" FORCE)
set(SDL2IMAGE_TIF OFF CACHE BOOL "" FORCE)
set(SDL2IMAGE_WEBP OFF CACHE BOOL "" FORCE)
FetchContent_Declare(SDL2_image
    GIT_REPOSITORY https://github.com/libsdl-org/SDL_image.git
    GIT_TAG release-2.8.2
    GIT_SUBMODULES external/libpng external/zlib
    GIT_SUBMODULES_RECURSE TRUE
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(SDL2_image)
target_include_directories(SDL2_image PRIVATE "${sdl2_image_BINARY_DIR}/external/libpng")

set(SDL2TTF_VENDORED ON CACHE BOOL "" FORCE)
set(SDL2TTF_INSTALL OFF CACHE BOOL "" FORCE)
set(SDL2TTF_SAMPLES OFF CACHE BOOL "" FORCE)
set(SDL2TTF_HARFBUZZ OFF CACHE BOOL "" FORCE)
FetchContent_Declare(SDL2_ttf
    GIT_REPOSITORY https://github.com/libsdl-org/SDL_ttf.git
    GIT_TAG release-2.22.0
    GIT_SUBMODULES external/freetype
    GIT_SUBMODULES_RECURSE TRUE
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(SDL2_ttf)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(ANDROID ON CACHE BOOL "" FORCE)
set(STATICLIB ON CACHE BOOL "" FORCE)
set(NOX11 ON CACHE BOOL "" FORCE)
set(NO_LOADER ON CACHE BOOL "" FORCE)
set(NO_INIT_CONSTRUCTOR ON CACHE BOOL "" FORCE)
set(USE_ANDROID_LOG ON CACHE BOOL "" FORCE)
set(GBM OFF CACHE BOOL "" FORCE)
FetchContent_Declare(gl4es
    GIT_REPOSITORY https://github.com/ptitSeb/gl4es.git
    GIT_TAG v1.1.6
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(gl4es)
set_target_properties(GL PROPERTIES
    ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/gl4es"
)

set(ALSOFT_UTILS OFF CACHE BOOL "" FORCE)
set(ALSOFT_EXAMPLES OFF CACHE BOOL "" FORCE)
set(ALSOFT_TESTS OFF CACHE BOOL "" FORCE)
set(ALSOFT_INSTALL OFF CACHE BOOL "" FORCE)
set(ALSOFT_INSTALL_CONFIG OFF CACHE BOOL "" FORCE)
set(ALSOFT_INSTALL_HRTF_DATA OFF CACHE BOOL "" FORCE)
set(ALSOFT_INSTALL_AMBDEC_PRESETS OFF CACHE BOOL "" FORCE)
set(ALSOFT_INSTALL_EXAMPLES OFF CACHE BOOL "" FORCE)
set(ALSOFT_INSTALL_UTILS OFF CACHE BOOL "" FORCE)
set(ALSOFT_BACKEND_AAUDIO OFF CACHE BOOL "" FORCE)
set(ALSOFT_BACKEND_OPENSL ON CACHE BOOL "" FORCE)
set(LIBTYPE STATIC CACHE STRING "" FORCE)
FetchContent_Declare(openal_soft
    GIT_REPOSITORY https://github.com/kcat/openal-soft.git
    GIT_TAG 1.23.1
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(openal_soft)

set(BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(INSTALL_DOCS OFF CACHE BOOL "" FORCE)
set(INSTALL_PKG_CONFIG_MODULE OFF CACHE BOOL "" FORCE)
set(INSTALL_CMAKE_PACKAGE_MODULE OFF CACHE BOOL "" FORCE)
FetchContent_Declare(ogg
    GIT_REPOSITORY https://github.com/xiph/ogg.git
    GIT_TAG v1.3.5
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(ogg)
FetchContent_Declare(vorbis
    GIT_REPOSITORY https://github.com/xiph/vorbis.git
    GIT_TAG v1.3.7
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(vorbis)

set(TE4_SRC "${TE4_ROOT}/src")
file(GLOB TE4_CORE_SOURCES CONFIGURE_DEPENDS "${TE4_SRC}/*.c")
file(GLOB TE4_LUA_SOURCES CONFIGURE_DEPENDS "${TE4_SRC}/lua/*.c")
file(GLOB TE4_LPEG_SOURCES CONFIGURE_DEPENDS "${TE4_SRC}/lpeg/*.c")
file(GLOB TE4_FOV_SOURCES CONFIGURE_DEPENDS "${TE4_SRC}/fov/*.c")
file(GLOB TE4_SOCKET_SOURCES CONFIGURE_DEPENDS "${TE4_SRC}/luasocket/*.c")
list(REMOVE_ITEM TE4_SOCKET_SOURCES "${TE4_SRC}/luasocket/wsocket.c")
file(GLOB TE4_PROFILER_SOURCES CONFIGURE_DEPENDS "${TE4_SRC}/luaprofiler/*.c")
file(GLOB TE4_TCOD_SOURCES CONFIGURE_DEPENDS "${TE4_SRC}/libtcod_import/*.c")
file(GLOB TE4_EXPAT_SOURCES CONFIGURE_DEPENDS "${TE4_SRC}/expat/*.c")
file(GLOB TE4_LXP_SOURCES CONFIGURE_DEPENDS "${TE4_SRC}/lxp/*.c")
file(GLOB TE4_MD5_SOURCES CONFIGURE_DEPENDS "${TE4_SRC}/luamd5/*.c")
file(GLOB TE4_LZLIB_SOURCES CONFIGURE_DEPENDS "${TE4_SRC}/lzlib/*.c")
file(GLOB TE4_BITOP_SOURCES CONFIGURE_DEPENDS "${TE4_SRC}/luabitop/*.c")
file(GLOB TE4_BZIP_SOURCES CONFIGURE_DEPENDS "${TE4_SRC}/bzip2/*.c")
file(GLOB TE4_WFC_SOURCES CONFIGURE_DEPENDS "${TE4_SRC}/wfc/*.cpp")
file(GLOB TE4_PHYSFS_SOURCES CONFIGURE_DEPENDS
    "${TE4_SRC}/physfs/*.c"
    "${TE4_SRC}/physfs/archivers/*.c"
    "${TE4_SRC}/physfs/platform/unix.c"
    "${TE4_SRC}/physfs/platform/posix.c"
    "${TE4_SRC}/zlib/mzip.c"
    "${TE4_SRC}/zlib/ioapi.c"
)

add_library(te4deps STATIC
    ${TE4_LUA_SOURCES}
    ${TE4_LPEG_SOURCES}
    ${TE4_FOV_SOURCES}
    ${TE4_SOCKET_SOURCES}
    ${TE4_PROFILER_SOURCES}
    ${TE4_TCOD_SOURCES}
    ${TE4_EXPAT_SOURCES}
    ${TE4_LXP_SOURCES}
    ${TE4_MD5_SOURCES}
    ${TE4_LZLIB_SOURCES}
    ${TE4_BITOP_SOURCES}
    ${TE4_BZIP_SOURCES}
    ${TE4_WFC_SOURCES}
    "${TE4_SRC}/utf8proc/utf8proc.c"
    ${TE4_PHYSFS_SOURCES}
)
target_include_directories(te4deps PUBLIC
    "${TE4_SRC}"
    "${TE4_SRC}/lua"
    "${TE4_SRC}/luasocket"
    "${TE4_SRC}/fov"
    "${TE4_SRC}/expat"
    "${TE4_SRC}/lxp"
    "${TE4_SRC}/libtcod_import"
    "${TE4_SRC}/physfs"
    "${TE4_SRC}/zlib"
    "${TE4_SRC}/bzip2"
    "${TE4_SRC}/wfc"
    "${TE4_SRC}/utf8proc"
)
target_compile_definitions(te4deps PUBLIC
    TE4_LUA_ALLOW_GENERIC_IO
    PHYSFS_SUPPORTS_ZIP
    PHYSFS_PLATFORM_POSIX
    HAVE_UNISTD_H
    HAVE_MEMMOVE
)
target_link_libraries(te4deps PUBLIC SDL2 SDL2_ttf SDL2_image zlibstatic)

add_library(main SHARED
    ${TE4_CORE_SOURCES}
    "${TE4_ROOT}/android/android_input.c"
    "${TE4_ROOT}/android/glu_compat.c"
)
set_target_properties(main PROPERTIES LINKER_LANGUAGE CXX)
target_include_directories(main PRIVATE
    "${TE4_SRC}"
    "${TE4_SRC}/luasocket"
    "${TE4_SRC}/fov"
    "${TE4_SRC}/expat"
    "${TE4_SRC}/lxp"
    "${TE4_SRC}/libtcod_import"
    "${TE4_SRC}/physfs"
    "${TE4_SRC}/zlib"
    "${TE4_SRC}/bzip2"
    "${TE4_SRC}/wfc"
    "${TE4_SRC}/utf8proc"
    "${sdl2_image_SOURCE_DIR}/external/libpng"
    "${sdl2_image_BINARY_DIR}/external/libpng"
    "${FETCHCONTENT_BASE_DIR}/gl4es-src/include"
)
target_compile_definitions(main PRIVATE
    GLEW_STATIC
    TE4CORE_VERSION=17
    TENGINE_HOME_PATH=\".t-engine\"
    SELFEXE_ANDROID
)
target_compile_options(main PRIVATE -Wno-deprecated-declarations -Wno-unused-function)
target_link_libraries(main PRIVATE
    te4deps
    SDL2
    SDL2_image
    SDL2_ttf
    OpenAL
    PNG::PNG
    vorbisfile
    vorbis
    ogg
    GL
    m
    log
    dl
    android
    EGL
    GLESv2
    OpenSLES
)

