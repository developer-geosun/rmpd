export type DeclarationStatus =
  | 'DRAFT'
  | 'VALIDATED'
  | 'SIGNED'
  | 'SUBMITTED'
  | 'ACCEPTED'
  | 'REGISTERED'
  | 'REJECTED'
  | 'ERROR';

export type TransportType = 'LADEN' | 'EMPTY' | 'TRANSIT' | 'CABOTAGE';

export interface Declaration {
  id: number;
  status: DeclarationStatus;
  transportType?: TransportType;
  cmrNumber?: string;
  routeStartDate?: string;
  routeEndDate?: string;
  loadingCountry?: string;
  unloadingCountry?: string;
  vehicleId?: number;
  permitId?: number;
  senderPartyId?: number;
  receiverPartyId?: number;
  routePointsJson?: string;
  puescSysRef?: string;
  referenceNumber?: string;
  comment?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DeclarationUpsert {
  transportType?: TransportType;
  cmrNumber?: string;
  routeStartDate?: string;
  routeEndDate?: string;
  loadingCountry?: string;
  unloadingCountry?: string;
  vehicleId?: number;
  permitId?: number;
  senderPartyId?: number;
  receiverPartyId?: number;
  routePointsJson?: string;
  comment?: string;
}

export interface DeclarationEvent {
  id: number;
  eventType: string;
  payloadJson?: string;
  createdAt: string;
}

export interface ValidationResult {
  valid: boolean;
  errors: string[];
}

export interface SubmitResult {
  sysRef: string;
  status: string;
  message: string;
}

export interface PuescCredential {
  environment: 'TEST' | 'PROD';
  username: string;
  passwordConfigured: boolean;
  signingCertPath?: string;
  active: boolean;
  lastTestAt?: string;
  lastTestOk?: boolean;
}

export interface PuescCredentialUpsert {
  environment?: 'TEST' | 'PROD';
  username: string;
  password?: string;
  signingCertPath?: string;
}

export interface PuescConnectionTest {
  success: boolean;
  message: string;
  mock: boolean;
}

export interface CmrExtractedField {
  fieldKey: string;
  value: string;
  confidence: number;
}

export interface CmrDocument {
  id: number;
  originalFilename: string;
  mimeType: string;
  fileSizeBytes: number;
  extractedFields: CmrExtractedField[];
  appliedAt?: string;
}

export interface DictionaryEntry {
  code: string;
  labelPl: string;
  labelEn: string;
}
