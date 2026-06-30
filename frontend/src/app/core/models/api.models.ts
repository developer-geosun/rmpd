export type UserRole = 'ADMIN' | 'DISPATCHER' | 'VIEWER';

export interface UserInfo {
  id: number;
  email: string;
  role: UserRole;
  carrierId: number;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: UserInfo;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface Address {
  country: string;
  city: string;
  postalCode: string;
  street: string;
  buildingNumber: string;
  unitNumber?: string;
}

export interface CarrierProfile {
  idType: string;
  idNumber: string;
  name: string;
  address: Address;
  email: string;
}

export interface Vehicle {
  id: number;
  registrationCountry: string;
  tractorNumber: string;
  trailerNumber?: string;
  gpsDeviceId: string;
  createdAt?: string;
}

export interface VehicleUpsert {
  registrationCountry: string;
  tractorNumber: string;
  trailerNumber?: string;
  gpsDeviceId: string;
}

export interface Permit {
  id: number;
  permitType: string;
  permitNumber: string;
  validUntil: string;
  createdAt?: string;
}

export interface PermitUpsert {
  permitType: string;
  permitNumber: string;
  validUntil: string;
}

export type PartyRole = 'SENDER' | 'RECEIVER' | 'BOTH';

export interface Party {
  id: number;
  partyRole: PartyRole;
  idType: string;
  idNumber: string;
  name: string;
  address: Address;
  createdAt?: string;
}

export interface PartyUpsert {
  partyRole: PartyRole;
  idType: string;
  idNumber: string;
  name: string;
  address: Address;
}
