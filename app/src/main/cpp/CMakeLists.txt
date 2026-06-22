cmake_minimum_required(VERSION 3.22.1)
project("lolive_native")

add_library(
    lolive_native
    SHARED
    native_endpoints.cpp
)
