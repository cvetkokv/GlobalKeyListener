cmake_minimum_required(VERSION 3.10)
project(keyboard_hook LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)  # Ensures symbols are exported for the DLL

# Create a shared library
add_library(keyboard_hook SHARED keyboard_hook.cpp)

# Set the output DLL name (optional)
set_target_properties(keyboard_hook PROPERTIES OUTPUT_NAME "keyboard_hook")

# Add Java headers (set JAVA_HOME first or hardcode it)
set(JDK_PATH "C:/Program Files/Java/jdk-24")

include_directories(
    "${JDK_PATH}/include"
    "${JDK_PATH}/include/win32"
)

# Add Boost headers (header-only, no linking needed unless you're using linked Boost libs)
include_directories("C:/libraries/boost_1_88_0")  # ← CHANGE THIS to your local Boost path

# If using Boost libs like thread/system, uncomment this:
# find_package(Boost REQUIRED COMPONENTS thread system)
# target_link_libraries(keyboard_hook Boost::thread Boost::system)
