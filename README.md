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
git clone <your-repository-url>
cd QR114Scanner

# Install all dependencies
npm install
