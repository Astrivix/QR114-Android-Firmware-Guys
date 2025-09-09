# QR114 BLE Controller App

This is a React Native application built with Expo to scan for, connect to, and interact with the QR114 Bluetooth Low Energy (BLE) device.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started: Development Setup](#getting-started-development-setup)
- [Important Notes for BLE Development](#important-notes-for-ble-development)
- [Project Structure](#project-structure)

## Features

-   Scan for nearby BLE devices.
-   Connect to a specific device from the scanned list.
-   Initiate the pairing/bonding process.
-   Discover and display all Services and Characteristics of a connected device.

## Tech Stack

-   **Framework:** React Native with Expo
-   **Language:** TypeScript
-   **BLE Library:** `react-native-ble-plx`
-   **Navigation:** React Navigation
-   **State Management:** React Context API for global BLE state.
-   **Build System:** Expo Application Services (EAS) Development Client.

## Prerequisites

Before you begin, ensure you have the following installed and set up:

1.  **Node.js (LTS):** You must have the latest Long-Term Support version. You can check with `node -v`.
2.  **A Physical Phone (iOS or Android):** BLE functionality **cannot** be tested on simulators or emulators. You need a real device.
3.  **Expo Go App:** Install the "Expo Go" app on your physical device from the App Store or Google Play Store.
4.  **Expo CLI:** Install the command-line tool globally:
    ```bash
    npm install -g expo-cli
    ```

## Getting Started: Development Setup

This project uses native code (`react-native-ble-plx`), so you **must** use a custom Development Build. The standard Expo Go app will not work.

Follow these steps precisely to get the app running on your device for the first time.

### Step 1: Clone and Install Dependencies

First, clone the repository and install all the required npm packages.

```bash
# Clone the repository
git clone [<your-repository-url>](https://github.com/Astrivix/QR114-Firmware-Guys)
cd QR114Scanner
```

# Install all dependencies
npm install
Step 2: Build the Development Client

This is the most important step. This command compiles the native code and creates a custom version of the Expo Go app specifically for this project.

Connect your physical Android or iOS device to your computer via USB.

Ensure USB Debugging is enabled (for Android).

Run the appropriate command for your platform:

code
Bash
download
content_copy
expand_less
IGNORE_WHEN_COPYING_START
IGNORE_WHEN_COPYING_END
# For Android
npx expo run:android

# For iOS (requires a Mac with Xcode)
npx expo run:ios

This process will take several minutes. It will build the app, install it on your connected device, and then start the Metro development server.

Step 3: Running the App (After the First Build)

Once the initial build is complete, your daily workflow becomes much simpler.

Make sure your physical device is connected or on the same Wi-Fi network as your computer.

Start the Metro server:

code
Bash
download
content_copy
expand_less
IGNORE_WHEN_COPYING_START
IGNORE_WHEN_COPYING_END
npm start

Open the QR114Scanner app (the one that was installed by the build command, not the standard Expo Go app) on your phone. It will automatically connect to the development server.

Any changes you make to the TypeScript code will now instantly reload in the app. You only need to re-run npx expo run:android if you add or change other native dependencies.

Important Notes for BLE Development

Permissions are Key: The app will request Bluetooth and Location permissions when you first try to scan. You must accept these for the app to function.

Enable Bluetooth & Location: Make sure both Bluetooth and Location/GPS services are turned on on your phone before scanning. Android requires Location to be enabled for BLE scanning.

Project Structure

The codebase is organized to be clean and scalable:

code
Code
download
content_copy
expand_less
IGNORE_WHEN_COPYING_START
IGNORE_WHEN_COPYING_END
.
└── src/
    ├── components/         // Reusable UI components
    ├── context/            // React Context for global BLE state
    ├── hooks/              // Custom hooks for complex logic (useBLE.ts)
    ├── navigation/         // Navigation stack and configuration
    ├── screens/            // Top-level screen components
    └── types/              // TypeScript type definitions
code
Code
download
content_copy
expand_less
IGNORE_WHEN_COPYING_START
IGNORE_WHEN_COPYING_END
