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

export type RoutePointType = 'ENTRY' | 'EXIT';

export interface RoutePoint {
  type: RoutePointType;
  name: string;
  country: string;
}

export interface DeclarationProgress {
  completionPercent: number;
  missingFields: string[];
}

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
  termsAccepted?: boolean;
  completionPercent?: number;
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
  termsAccepted?: boolean;
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
  idSiscRop?: string;
  idSiscRof?: string;
  idSiscP?: string;
  active: boolean;
  lastTestAt?: string;
  lastTestOk?: boolean;
}

export interface PuescCredentialUpsert {
  environment?: 'TEST' | 'PROD';
  username: string;
  password?: string;
  signingCertPath?: string;
  idSiscRop?: string;
  idSiscRof?: string;
  idSiscP?: string;
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
  previewUrl?: string;
}

export interface DictionaryEntry {
  code: string;
  labelPl: string;
  labelEn: string;
}

export interface AmendmentRequest {
  vehicleId?: number;
  routeStartDate?: string;
  routeEndDate?: string;
  comment?: string;
  amendmentReason?: string;
}

export interface GpsCheckResult {
  valid: boolean;
  gpsDeviceId?: string;
  latitude?: number;
  longitude?: number;
  recordedAt?: string;
  source?: string;
  positionStale: boolean;
  message: string;
}

export interface CmrBatchItemResult {
  declarationId: number | null;
  filename: string;
  success: boolean;
  error?: string;
  extractedFieldCount: number;
}

export interface CmrBatchResult {
  total: number;
  succeeded: number;
  items: CmrBatchItemResult[];
}

export interface PartySuggestion {
  partyId: number;
  partyRole: string;
  name: string;
  idNumber: string;
  matchScore: number;
  source: string;
}

export interface CmrPartySuggestions {
  extractedSenderName?: string;
  extractedReceiverName?: string;
  senderMatch?: PartySuggestion;
  receiverMatch?: PartySuggestion;
}
