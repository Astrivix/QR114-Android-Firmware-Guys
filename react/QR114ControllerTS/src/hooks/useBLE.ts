// src/hooks/useBLE.ts
import { useState, useMemo, useCallback } from "react";
import { PermissionsAndroid, Platform } from "react-native";
import { BleManager, Device, BleError } from "react-native-ble-plx";
import { ServiceWithCharacteristics } from "../types/ble";

const useBLE = () => {
  const bleManager = useMemo(() => new BleManager(), []);
  const [allDevices, setAllDevices] = useState<Device[]>([]);
  const [connectedDevice, setConnectedDevice] = useState<Device | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [discoveredData, setDiscoveredData] = useState<
    ServiceWithCharacteristics[]
  >([]);

  const requestPermissions = useCallback(async (): Promise<boolean> => {
    if (Platform.OS === "android") {
      if (Platform.Version >= 31) {
        // Android 12+
        const permissions = await PermissionsAndroid.requestMultiple([
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
        ]);
        return (
          permissions["android.permission.BLUETOOTH_CONNECT"] === "granted" &&
          permissions["android.permission.BLUETOOTH_SCAN"] === "granted"
        );
      }
      // Android 6-11
      const locationPermission = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION
      );
      return locationPermission === "granted";
    }
    return true; // iOS permissions are handled in Info.plist
  }, []);

  const scanForDevices = useCallback(async () => {
    const permissionsGranted = await requestPermissions();
    if (!permissionsGranted) {
      console.log("Permissions not granted. Cannot scan.");
      return;
    }

    setAllDevices([]);
    setIsScanning(true);
    bleManager.startDeviceScan(null, null, (error, device) => {
      if (error) {
        console.error("Scan Error:", error);
        setIsScanning(false);
        return;
      }
      if (device && device.name) {
        setAllDevices((prevState) => {
          if (!prevState.find((d) => d.id === device.id)) {
            return [...prevState, device];
          }
          return prevState;
        });
      }
    });
    // Stop scanning after 10 seconds
    setTimeout(() => {
      bleManager.stopDeviceScan();
      setIsScanning(false);
    }, 10000);
  }, [bleManager, requestPermissions]);

  const connectToDevice = useCallback(
    async (device: Device) => {
      try {
        bleManager.stopDeviceScan();
        setIsScanning(false);
        console.log(`Connecting to ${device.name}...`);
        const connected = await bleManager.connectToDevice(device.id);

        connected.onDisconnected(
          (error: BleError | null, disconnectedDevice: Device) => {
            console.log(`Disconnected from ${disconnectedDevice.name}`);
            setConnectedDevice(null);
            setDiscoveredData([]);
          }
        );

        setConnectedDevice(connected);
        console.log(`Successfully connected to ${device.name}.`);
      } catch (e) {
        console.error("Failed to connect", e);
      }
    },
    [bleManager]
  );

  const discoverAllServicesAndCharacteristics = useCallback(async () => {
    if (!connectedDevice) return;
    try {
      console.log("Starting service discovery... This may trigger pairing.");
      await connectedDevice.discoverAllServicesAndCharacteristics();
      const services = await connectedDevice.services();

      const servicesWithChars: ServiceWithCharacteristics[] = await Promise.all(
        services.map(async (service) => {
          const characteristics = await service.characteristics();
          return { service, characteristics };
        })
      );

      setDiscoveredData(servicesWithChars);
      console.log("Discovery complete.");
    } catch (error) {
      console.error("Failed to discover services/characteristics:", error);
    }
  }, [connectedDevice]);

  const disconnectFromDevice = useCallback(async () => {
    if (connectedDevice) {
      await bleManager.cancelDeviceConnection(connectedDevice.id);
    }
  }, [bleManager, connectedDevice]);

  return {
    scanForDevices,
    isScanning,
    allDevices,
    connectToDevice,
    connectedDevice,
    disconnectFromDevice,
    discoverAllServicesAndCharacteristics,
    discoveredData,
  };
};

export default useBLE;
