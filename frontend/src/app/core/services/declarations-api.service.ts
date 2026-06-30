import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  CmrDocument,
  Declaration,
  DeclarationEvent,
  DeclarationUpsert,
  DictionaryEntry,
  PuescConnectionTest,
  PuescCredential,
  PuescCredentialUpsert,
  SubmitResult,
  ValidationResult,
} from '../models/declaration.models';

@Injectable({ providedIn: 'root' })
export class DeclarationsApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<Declaration[]> {
    return this.http.get<Declaration[]>('/api/v1/declarations');
  }

  create(): Observable<Declaration> {
    return this.http.post<Declaration>('/api/v1/declarations', {});
  }

  get(id: number): Observable<Declaration> {
    return this.http.get<Declaration>(`/api/v1/declarations/${id}`);
  }

  update(id: number, data: DeclarationUpsert): Observable<Declaration> {
    return this.http.put<Declaration>(`/api/v1/declarations/${id}`, data);
  }

  validate(id: number): Observable<ValidationResult> {
    return this.http.post<ValidationResult>(`/api/v1/declarations/${id}/validate`, {});
  }

  downloadXml(id: number): Observable<Blob> {
    return this.http.get(`/api/v1/declarations/${id}/xml`, { responseType: 'blob' });
  }

  submit(id: number): Observable<SubmitResult> {
    return this.http.post<SubmitResult>(`/api/v1/declarations/${id}/submit`, {});
  }

  poll(id: number): Observable<Declaration> {
    return this.http.post<Declaration>(`/api/v1/declarations/${id}/poll`, {});
  }

  listEvents(id: number): Observable<DeclarationEvent[]> {
    return this.http.get<DeclarationEvent[]>(`/api/v1/declarations/${id}/events`);
  }

  copy(id: number): Observable<Declaration> {
    return this.http.post<Declaration>(`/api/v1/declarations/${id}/copy`, {});
  }

  uploadCmr(declarationId: number, file: File): Observable<CmrDocument> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<CmrDocument>(`/api/v1/declarations/${declarationId}/cmr/upload`, form);
  }

  getCmr(declarationId: number): Observable<CmrDocument> {
    return this.http.get<CmrDocument>(`/api/v1/declarations/${declarationId}/cmr`);
  }

  applyCmr(declarationId: number, fieldKeys: string[]): Observable<Declaration> {
    return this.http.post<Declaration>(`/api/v1/declarations/${declarationId}/cmr/apply`, { fieldKeys });
  }

  getPuescSettings(): Observable<PuescCredential> {
    return this.http.get<PuescCredential>('/api/v1/settings/puesc');
  }

  savePuescSettings(data: PuescCredentialUpsert): Observable<PuescCredential> {
    return this.http.put<PuescCredential>('/api/v1/settings/puesc', data);
  }

  testPuescConnection(): Observable<PuescConnectionTest> {
    return this.http.post<PuescConnectionTest>('/api/v1/settings/puesc/test', {});
  }

  listCountries(): Observable<DictionaryEntry[]> {
    return this.http
      .get<{ type: string; entries: DictionaryEntry[] }>('/api/v1/dictionaries/country')
      .pipe(map((res) => res.entries));
  }
}
