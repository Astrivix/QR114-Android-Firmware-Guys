// src/context/BLEContext.tsx
import React, { createContext, ReactNode } from "react";
import useBLE from "../hooks/useBLE";

// Infer the return type of useBLE for strong typing
type BLEContextType = ReturnType<typeof useBLE>;

export const BLEContext = createContext<BLEContextType | null>(null);

export const BLEProvider = ({ children }: { children: ReactNode }) => {
  const bleManager = useBLE();
  return (
    <BLEContext.Provider value={bleManager}>{children}</BLEContext.Provider>
  );
};
