// src/navigation/AppNavigator.tsx
import React from "react";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import DeviceScanScreen from "../screens/DeviceScanScreen";
import DeviceControlScreen from "../screens/DeviceControlScreen";

const Stack = createNativeStackNavigator();

const AppNavigator = () => {
  return (
    <Stack.Navigator>
      <Stack.Screen
        name="Scan"
        component={DeviceScanScreen}
        options={{ title: "Scan for Devices" }}
      />
      <Stack.Screen
        name="Control"
        component={DeviceControlScreen}
        options={{ title: "Device Control" }}
      />
    </Stack.Navigator>
  );
};

export default AppNavigator;
