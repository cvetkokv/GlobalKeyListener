cmake_minimum_required(VERSION 3.10)
project(keyboard_hook LANGUAGES CXX)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Set output DLL name
set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)

# Set path to JDK (update as needed)
set(JDK_PATH "C:/Program Files/Java/jdk-24")

# Include JNI headers
include_directories(
    "${JDK_PATH}/include"
    "${JDK_PATH}/include/win32"
)

# Optional: Boost include if you're using Boost header-only
include_directories("C:/libraries/boost_1_88_0")

# Add your source files
add_library(keyboard_hook SHARED keyboard_hook.cpp)

# If using MinGW, statically link libgcc and libstdc++
if (MINGW)
    target_compile_options(keyboard_hook PRIVATE -static-libgcc -static-libstdc++)
    target_link_options(keyboard_hook PRIVATE -static-libgcc -static-libstdc++)
endif()

# Optional: If you use Boost and it's statically built, you can link it here
# target_link_libraries(keyboard_hook PRIVATE -lboost_thread -lboost_system)

# Optional: Set output DLL name explicitly
set_target_properties(keyboard_hook PROPERTIES
    OUTPUT_NAME "keyboard_hook"
    PREFIX ""  # Ensures name is keyboard_hook.dll, not libkeyboard_hook.dll
)
