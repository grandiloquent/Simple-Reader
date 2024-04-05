cmake_minimum_required(VERSION 3.10.2)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++2a -fno-rtti -fexceptions -Wall")


add_library(nativelib SHARED
        nativelib.cpp
        logging.cpp
        cameraEngine.cpp
        cameraManager.cpp
        CameraWrapper.cpp
        imageReader.cpp
        utils.cpp)

add_library(shared SHARED shared.cpp)
target_link_libraries(shared android log)


target_link_libraries(nativelib
        android
        camera2ndk
        mediandk)
