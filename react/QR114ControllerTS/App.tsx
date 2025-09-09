// App.tsx
import "expo-dev-client";
import React from "react";
import { NavigationContainer } from "@react-navigation/native";
import { SafeAreaView, StyleSheet, StatusBar } from "react-native";
import { BLEProvider } from "./src/context/BLEContext";
import AppNavigator from "./src/navigation/AppNavigator";
import { Buffer } from "buffer";

// react-native-ble-plx requires this global Buffer variable to be set.
global.Buffer = Buffer;

export default function App() {
  return (
    <BLEProvider>
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="dark-content" />
        <NavigationContainer>
          <AppNavigator />
        </NavigationContainer>
      </SafeAreaView>
    </BLEProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
  },
});
