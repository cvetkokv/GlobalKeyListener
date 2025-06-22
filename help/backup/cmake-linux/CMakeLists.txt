cmake_minimum_required(VERSION 3.10)
project(keyboard_hook_linux LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Java headers (make sure JAVA_HOME is set)
if(NOT DEFINED ENV{JAVA_HOME})
    message(FATAL_ERROR "JAVA_HOME not set. Please set JAVA_HOME to your JDK path.")
endif()

include_directories(
    $ENV{JAVA_HOME}/include
    $ENV{JAVA_HOME}/include/linux
)

# Boost (optional: only if lockfree queue not header-only)
# include_directories("/usr/include/boost") # Adjust as needed

# Build shared object
add_library(keyboard_hook SHARED keyboard_hook.cpp)

# Link against X11 and pthread for Linux key capture
target_link_libraries(keyboard_hook X11 pthread)
