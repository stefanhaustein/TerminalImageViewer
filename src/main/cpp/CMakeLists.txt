project(tiv LANGUAGES CXX)
cmake_minimum_required(VERSION 3.9)

set(CMAKE_MODULE_PATH
    ${CMAKE_MODULE_PATH}
    ${PROJECT_SOURCE_DIR}/cmake)

include(ConfigureCCache)
include(ConfigureNinja)

#=== C++ VERSION CHECK ===
#set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CXX_EXTENSIONS OFF)
include(ConfigureCXX_Version)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -Wpedantic -Wconversion -Wshadow")

add_executable(tiv tiv.cpp CImg.h)

include(ConfigureThreads)
include(ConfigureLTO)
