#!/bin/bash
APK_FILE=firetweet.apk
if [ ! -f "$APK_FILE" ]; then
  wget -O $APK_FILE https://github.com/firetweet/downloads/raw/master/firetweet.apk
fi
monkeyrunner testrunner.py
