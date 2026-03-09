#!/bin/bash
# TherapyTrack Android SDK Setup Script

echo "📦 Installing Android SDK..."

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Installing OpenJDK..."
    brew install openjdk@17
fi

# Set JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH=$JAVA_HOME/bin:$PATH

# Install Android SDK
SDK_DIR="$HOME/android-sdk"
mkdir -p "$SDK_DIR/cmdline-tools"

if [ ! -d "$SDK_DIR/cmdline-tools/latest" ]; then
    echo "⬇️  Downloading Android command-line tools..."
    cd "$SDK_DIR/cmdline-tools"
    curl -sL "https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip" -o cmdline-tools.zip
    unzip -q cmdline-tools.zip
    mv cmdline-tools latest
    rm cmdline-tools.zip
fi

# Accept licenses and install SDK components
yes | "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null 2>&1
echo "📱 Installing SDK platforms and build tools..."
"$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" "platforms;android-34" "build-tools;34.0.0" "platform-tools" > /dev/null 2>&1

# Update local.properties
echo "sdk.dir=$SDK_DIR" > /Users/admin/Projects/therapytrack_android/local.properties

echo "✅ Android SDK setup complete!"
echo "SDK location: $SDK_DIR"
