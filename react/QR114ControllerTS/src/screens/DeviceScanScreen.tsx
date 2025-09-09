// src/screens/DeviceScanScreen.tsx
import React, { useContext } from "react";
import {
  View,
  Text,
  Button,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
} from "react-native";
import { BLEContext } from "../context/BLEContext";
import { Device } from "react-native-ble-plx";

const DeviceScanScreen = ({ navigation }: any) => {
  const bleManager = useContext(BLEContext);
  if (!bleManager) return null; // Or a loading indicator

  const { scanForDevices, isScanning, allDevices, connectToDevice } =
    bleManager;

  const handleDevicePress = (device: Device) => {
    connectToDevice(device).then(() => {
      navigation.navigate("Control");
    });
  };

  const renderItem = ({ item }: { item: Device }) => (
    <TouchableOpacity
      onPress={() => handleDevicePress(item)}
      style={styles.deviceItem}
    >
      <Text style={styles.deviceName}>{item.name}</Text>
      <Text style={styles.deviceId}>{item.id}</Text>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Button
          title={isScanning ? "Scanning..." : "Scan for Devices"}
          onPress={scanForDevices}
          disabled={isScanning}
        />
        {isScanning && <ActivityIndicator style={styles.loader} />}
      </View>
      <FlatList
        data={allDevices}
        renderItem={renderItem}
        keyExtractor={(item) => item.id}
        ListEmptyComponent={
          <Text style={styles.emptyList}>
            No devices found. Ensure Bluetooth is on and press scan.
          </Text>
        }
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20 },
  header: { alignItems: "center", marginBottom: 20 },
  loader: { marginTop: 10 },
  emptyList: { textAlign: "center", color: "#888", marginTop: 50 },
  deviceItem: {
    padding: 15,
    marginBottom: 10,
    backgroundColor: "#f0f0f0",
    borderRadius: 8,
  },
  deviceName: { fontSize: 18, fontWeight: "bold" },
  deviceId: { fontSize: 14, color: "#666" },
});

export default DeviceScanScreen;
