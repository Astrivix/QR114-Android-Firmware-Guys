// src/screens/DeviceControlScreen.tsx
import React, { useContext, useState } from "react";
import {
  View,
  Text,
  Button,
  ScrollView,
  StyleSheet,
  ActivityIndicator,
  Platform,
} from "react-native";
import { BLEContext } from "../context/BLEContext";

const DeviceControlScreen = () => {
  const bleManager = useContext(BLEContext);
  const [isDiscovering, setIsDiscovering] = useState(false);
  if (!bleManager) return null;

  const {
    connectedDevice,
    disconnectFromDevice,
    discoverAllServicesAndCharacteristics,
    discoveredData,
  } = bleManager;

  const handleDiscover = async () => {
    setIsDiscovering(true);
    await discoverAllServicesAndCharacteristics();
    setIsDiscovering(false);
  };

  if (!connectedDevice) {
    return (
      <View style={styles.container}>
        <Text style={styles.statusText}>
          Device disconnected. Go back to scan.
        </Text>
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>Connected to:</Text>
      <Text style={styles.deviceName}>{connectedDevice.name}</Text>

      <View style={styles.buttonContainer}>
        <Button
          title="Discover All Data (Pair Device)"
          onPress={handleDiscover}
          disabled={isDiscovering}
        />
      </View>

      {isDiscovering && (
        <ActivityIndicator size="large" style={{ marginVertical: 20 }} />
      )}

      {discoveredData.map(({ service, characteristics }) => (
        <View key={service.uuid} style={styles.serviceContainer}>
          <Text style={styles.uuidTitle}>Service UUID:</Text>
          <Text style={styles.uuidText}>{service.uuid}</Text>

          <Text style={styles.charTitle}>Characteristics:</Text>
          {characteristics.map((char) => (
            <View key={char.uuid} style={styles.charContainer}>
              <Text style={styles.uuidText}>{char.uuid}</Text>
            </View>
          ))}
        </View>
      ))}

      <View style={styles.buttonContainer}>
        <Button title="Disconnect" onPress={disconnectFromDevice} color="red" />
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: { padding: 20, paddingBottom: 50 },
  title: { fontSize: 20, textAlign: "center" },
  deviceName: {
    fontSize: 22,
    fontWeight: "bold",
    textAlign: "center",
    marginBottom: 20,
  },
  statusText: { fontSize: 18, textAlign: "center", marginTop: 50 },
  buttonContainer: { marginVertical: 20 },
  serviceContainer: {
    backgroundColor: "#fff",
    padding: 15,
    marginBottom: 15,
    borderRadius: 8,
    elevation: 2,
  },
  uuidTitle: { fontSize: 16, fontWeight: "bold" },
  uuidText: {
    fontSize: 14,
    color: "#333",
    marginBottom: 10,
    fontFamily: Platform.OS === "ios" ? "Courier" : "monospace",
  },
  charTitle: { fontSize: 15, fontWeight: "bold", color: "#555", marginTop: 10 },
  charContainer: {
    marginLeft: 15,
    marginTop: 5,
    borderLeftWidth: 1,
    borderLeftColor: "#ccc",
    paddingLeft: 10,
  },
});

export default DeviceControlScreen;
