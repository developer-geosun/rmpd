import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PartySuggestion } from '../models/declaration.models';
import {
  CarrierProfile,
  Party,
  PartyUpsert,
  Permit,
  PermitUpsert,
  Vehicle,
  VehicleUpsert,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ReferenceDataApiService {
  private readonly http = inject(HttpClient);

  getCarrierProfile(): Observable<CarrierProfile> {
    return this.http.get<CarrierProfile>('/api/v1/settings/carrier');
  }

  updateCarrierProfile(profile: CarrierProfile): Observable<CarrierProfile> {
    return this.http.put<CarrierProfile>('/api/v1/settings/carrier', profile);
  }

  listVehicles(): Observable<Vehicle[]> {
    return this.http.get<Vehicle[]>('/api/v1/vehicles');
  }

  createVehicle(data: VehicleUpsert): Observable<Vehicle> {
    return this.http.post<Vehicle>('/api/v1/vehicles', data);
  }

  updateVehicle(id: number, data: VehicleUpsert): Observable<Vehicle> {
    return this.http.put<Vehicle>(`/api/v1/vehicles/${id}`, data);
  }

  deleteVehicle(id: number): Observable<void> {
    return this.http.delete<void>(`/api/v1/vehicles/${id}`);
  }

  listPermits(): Observable<Permit[]> {
    return this.http.get<Permit[]>('/api/v1/permits');
  }

  createPermit(data: PermitUpsert): Observable<Permit> {
    return this.http.post<Permit>('/api/v1/permits', data);
  }

  updatePermit(id: number, data: PermitUpsert): Observable<Permit> {
    return this.http.put<Permit>(`/api/v1/permits/${id}`, data);
  }

  deletePermit(id: number): Observable<void> {
    return this.http.delete<void>(`/api/v1/permits/${id}`);
  }

  listParties(): Observable<Party[]> {
    return this.http.get<Party[]>('/api/v1/parties');
  }

  createParty(data: PartyUpsert): Observable<Party> {
    return this.http.post<Party>('/api/v1/parties', data);
  }

  updateParty(id: number, data: PartyUpsert): Observable<Party> {
    return this.http.put<Party>(`/api/v1/parties/${id}`, data);
  }

  deleteParty(id: number): Observable<void> {
    return this.http.delete<void>(`/api/v1/parties/${id}`);
  }

  searchPartySuggestions(q: string, role?: string): Observable<PartySuggestion[]> {
    let params = new HttpParams().set('q', q);
    if (role) {
      params = params.set('role', role);
    }
    return this.http.get<PartySuggestion[]>('/api/v1/parties/suggestions', { params });
  }
}
