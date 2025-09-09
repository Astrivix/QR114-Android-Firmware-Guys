// src/types/ble.ts
import { Characteristic, Service } from "react-native-ble-plx";

// This custom type will hold a service and its characteristics together.
export interface ServiceWithCharacteristics {
  service: Service;
  characteristics: Characteristic[];
}
