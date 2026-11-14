if(TARGET Ogg::ogg)
    set(Ogg_FOUND TRUE)
    set(OGG_FOUND TRUE)
    return()
endif()

include("${vorbis_SOURCE_DIR}/cmake/FindOgg.cmake")
