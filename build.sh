#!/bin/sh

mode=Debug

if [ "$2" = "release" ]; then
	mode=Release
fi

if [ "$1" = "core" ]; then
	target=xwalk_core_library
elif [ "$1" = "runtime" ]; then
	target=xwalk_runtime_lib_apk
elif [ "$1" = "test" ]; then
	target=xwalk_app_template_apk
fi

ninja -C out/$mode $target
