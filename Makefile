APK_FILE := ./firetweet/build/outputs/apk/firetweet-fdroid-debug.apk

define pkg_variables
	$(eval PACKAGE := $(shell aapt dump badging $(APK_FILE)|awk -F" " '/package/ {print $$2}'|awk -F"'" '/name=/ {print $$2}'))
	$(eval MAIN_ACTIVITY := $(shell aapt dump badging $(APK_FILE)|awk -F" " '/launchable-activity/ {print $$2}'|awk -F"'" '/name=/ {print $$2}' | grep MainActivity))
endef

.PHONY: all

all: build-debug install run

compile-debug:
	./gradlew \
		firetweet:compileFdroidDebugSources \
		firetweet:compileFdroidDebugAndroidTestSources \
		firetweet.component.common:compileDebugSources \
		firetweet.component.common:compileDebugAndroidTestSources \
		firetweet.component.jsonserializer:compileDebugSources \
		firetweet.component.jsonserializer:compileDebugAndroidTestSources \
		firetweet.component.nyan:compileDebugSources \
		firetweet.component.nyan:compileDebugAndroidTestSources \
		firetweet.component.querybuilder:compileDebugSources \
		firetweet.component.querybuilder:compileDebugAndroidTestSources \
		firetweet.component.twitter4j:compileDebugSources \
		firetweet.component.twitter4j:compileDebugAndroidTestSources \
		firetweet.component.twitter4j.streaming:compileDebugSources \
		firetweet.component.twitter4j.streaming:compileDebugAndroidTestSources \
		firetweet.extension.push.xiaomi:compileDebugSources \
		firetweet.extension.push.xiaomi:compileDebugAndroidTestSources \
		firetweet.extension.streaming:compileDebugSources \
		firetweet.extension.streaming:compileDebugAndroidTestSources \
		firetweet.extension.twitlonger:compileDebugSources \
		firetweet.extension.twitlonger:compileDebugAndroidTestSources \
		firetweet.library.extension:compileDebugSources \
		firetweet.library.extension:compileDebugAndroidTestSources \
		firetweet.wear:compileDebugSources \
		firetweet.wear:compileDebugAndroidTestSources

build-debug:
	./gradlew assembleFdroidDebug

$(APK_FILE): build-debug

install: $(APK_FILE)
	$(call pkg_variables)
	adb install -r $(APK_FILE)

uninstall:
	$(call pkg_variables)
	adb uninstall $(PACKAGE)

run:
	$(call pkg_variables)
	adb shell am start -n $(PACKAGE)/$(MAIN_ACTIVITY)
