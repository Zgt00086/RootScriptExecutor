#!/bin/bash

# Root Script Executor Build Script
# This script builds the Android application

set -e

echo "=== Root Script Executor Build Script ==="
echo ""

# Check if in project directory
if [ ! -f "settings.gradle" ]; then
    echo "Error: Not in project root directory"
    echo "Please run this script from /root/RootScriptExecutor/"
    exit 1
fi

# Check for Java
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed"
    exit 1
fi

# Check for Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "Warning: ANDROID_HOME is not set"
    echo "Trying to find Android SDK..."
    
    # Common Android SDK locations
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    elif [ -d "/usr/local/android-sdk" ]; then
        export ANDROID_HOME="/usr/local/android-sdk"
    elif [ -d "/opt/android-sdk" ]; then
        export ANDROID_HOME="/opt/android-sdk"
    else
        echo "Error: Could not find Android SDK"
        echo "Please set ANDROID_HOME environment variable"
        exit 1
    fi
fi

echo "Android SDK: $ANDROID_HOME"

# Check for required SDK components
if [ ! -d "$ANDROID_HOME/build-tools" ]; then
    echo "Error: Android build-tools not found"
    echo "Please install Android build-tools"
    exit 1
fi

echo ""
echo "=== Cleaning previous builds ==="
./gradlew clean

echo ""
echo "=== Building debug APK ==="
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "=== Build Successful ==="
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        echo "APK Location: $APK_PATH"
        echo "APK Size: $APK_SIZE"
        
        # Show APK information
        echo ""
        echo "=== APK Information ==="
        if command -v aapt &> /dev/null; then
            aapt dump badging "$APK_PATH" | grep -E "(package|launchable-activity|sdkVersion|targetSdkVersion)"
        fi
        
        # Copy to accessible location
        echo ""
        echo "=== Copying APK to /sdcard/ ==="
        cp "$APK_PATH" /sdcard/RootScriptExecutor-debug.apk
        echo "APK copied to: /sdcard/RootScriptExecutor-debug.apk"
        
        # Create installation instructions
        echo ""
        echo "=== Installation Instructions ==="
        echo "1. Transfer APK to Android device:"
        echo "   adb push /sdcard/RootScriptExecutor-debug.apk /sdcard/"
        echo ""
        echo "2. Install on device:"
        echo "   adb install /sdcard/RootScriptExecutor-debug.apk"
        echo ""
        echo "3. Alternative: Install directly:"
        echo "   adb install $APK_PATH"
        echo ""
        echo "4. Launch application:"
        echo "   adb shell am start -n com.example.rootscriptexecutor/.MainActivity"
        
    else
        echo "Error: APK file not found at expected location"
        exit 1
    fi
else
    echo ""
    echo "=== Build Failed ==="
    exit 1
fi

echo ""
echo "=== Build Script Completed ==="