cmake_minimum_required(VERSION 3.10)
project(keyboard_hook_linux LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Check JAVA_HOME
if(NOT DEFINED ENV{JAVA_HOME})
    message(FATAL_ERROR "JAVA_HOME environment variable not set. Please set JAVA_HOME to your JDK path.")
endif()

# JNI headers
include_directories(
    $ENV{JAVA_HOME}/include
    $ENV{JAVA_HOME}/include/linux
)

# Boost setup
find_package(Boost REQUIRED)
include_directories(${Boost_INCLUDE_DIRS})

# Source file
add_library(keyboard_hook SHARED keyboard_hook.cpp)

# Link system and Boost libraries
target_link_libraries(keyboard_hook
    X11
    pthread
)
