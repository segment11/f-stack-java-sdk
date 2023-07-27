cmake_minimum_required(VERSION 3.25)
project(ff_jni)

set(CMAKE_CXX_STANDARD 11)

#change here
include_directories(/home/kerry/.jdks/zulu17/include)
include_directories(/home/kerry/.jdks/zulu17/include/linux)

link_directories(/usr/local/lib/x86_64-linux-gnu)
link_libraries('fstack')

add_library(ff_jni library.cpp)

