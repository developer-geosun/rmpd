import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subscription, interval, switchMap, takeWhile } from 'rxjs';
import { DeclarationsApiService } from '../../core/services/declarations-api.service';
import { ReferenceDataApiService } from '../../core/services/reference-data-api.service';
import {
  CmrDocument,
  CmrExtractedField,
  Declaration,
  DeclarationEvent,
  DictionaryEntry,
  TransportType,
} from '../../core/models/declaration.models';
import { Party, Permit, Vehicle } from '../../core/models/api.models';

@Component({
  selector: 'app-declaration-detail',
  imports: [
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatListModule,
    MatCheckboxModule,
    MatChipsModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './declaration-detail.component.html',
  styleUrl: './declaration-detail.component.scss',
})
export class DeclarationDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(DeclarationsApiService);
  private readonly refApi = inject(ReferenceDataApiService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly declaration = signal<Declaration | null>(null);
  readonly events = signal<DeclarationEvent[]>([]);
  readonly cmr = signal<CmrDocument | null>(null);
  readonly selectedFields = signal<Set<string>>(new Set());
  readonly submitting = signal(false);
  readonly polling = signal(false);

  vehicles: Vehicle[] = [];
  permits: Permit[] = [];
  parties: Party[] = [];
  countries: DictionaryEntry[] = [];

  private pollSub?: Subscription;
  private declarationId = 0;

  readonly form = this.fb.group({
    transportType: ['LADEN' as TransportType],
    cmrNumber: [''],
    routeStartDate: ['', Validators.required],
    routeEndDate: [''],
    loadingCountry: ['', Validators.required],
    unloadingCountry: ['', Validators.required],
    vehicleId: [null as number | null, Validators.required],
    permitId: [null as number | null],
    senderPartyId: [null as number | null],
    receiverPartyId: [null as number | null],
    comment: [''],
  });

  ngOnInit(): void {
    this.declarationId = Number(this.route.snapshot.paramMap.get('id'));
    this.refApi.listVehicles().subscribe((v) => (this.vehicles = v));
    this.refApi.listPermits().subscribe((p) => (this.permits = p));
    this.refApi.listParties().subscribe((p) => (this.parties = p));
    this.api.listCountries().subscribe((c) => (this.countries = c));
    this.load();
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  load(): void {
    this.api.get(this.declarationId).subscribe({
      next: (d) => {
        this.declaration.set(d);
        this.form.patchValue({
          transportType: d.transportType ?? 'LADEN',
          cmrNumber: d.cmrNumber ?? '',
          routeStartDate: d.routeStartDate ?? '',
          routeEndDate: d.routeEndDate ?? '',
          loadingCountry: d.loadingCountry ?? '',
          unloadingCountry: d.unloadingCountry ?? '',
          vehicleId: d.vehicleId ?? null,
          permitId: d.permitId ?? null,
          senderPartyId: d.senderPartyId ?? null,
          receiverPartyId: d.receiverPartyId ?? null,
          comment: d.comment ?? '',
        });
        if (d.status === 'SUBMITTED') {
          this.startPolling();
        }
      },
      error: () => this.snackBar.open('Декларацію не знайдено', 'Закрити', { duration: 4000 }),
    });
    this.api.listEvents(this.declarationId).subscribe({
      next: (e) => this.events.set(e),
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.api.update(this.declarationId, {
      transportType: raw.transportType ?? undefined,
      cmrNumber: raw.cmrNumber || undefined,
      routeStartDate: raw.routeStartDate || undefined,
      routeEndDate: raw.routeEndDate || undefined,
      loadingCountry: raw.loadingCountry || undefined,
      unloadingCountry: raw.unloadingCountry || undefined,
      vehicleId: raw.vehicleId ?? undefined,
      permitId: raw.permitId ?? undefined,
      senderPartyId: raw.senderPartyId ?? undefined,
      receiverPartyId: raw.receiverPartyId ?? undefined,
      comment: raw.comment || undefined,
    }).subscribe({
      next: (d) => {
        this.declaration.set(d);
        this.snackBar.open('Збережено', 'OK', { duration: 2000 });
      },
      error: () => this.snackBar.open('Помилка збереження', 'Закрити', { duration: 4000 }),
    });
  }

  validate(): void {
    this.api.validate(this.declarationId).subscribe({
      next: (r) => {
        if (r.valid) {
          this.snackBar.open('Валідація пройдена', 'OK', { duration: 3000 });
          this.load();
        } else {
          this.snackBar.open(r.errors.join('; '), 'Закрити', { duration: 6000 });
        }
      },
    });
  }

  downloadXml(): void {
    this.api.downloadXml(this.declarationId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `rmpd100-${this.declarationId}.xml`;
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  submit(): void {
    this.submitting.set(true);
    this.api.submit(this.declarationId).subscribe({
      next: () => {
        this.submitting.set(false);
        this.snackBar.open('Відправлено в PUESC', 'OK', { duration: 3000 });
        this.load();
        this.startPolling();
      },
      error: (err) => {
        this.submitting.set(false);
        const msg = err?.error?.message ?? 'Помилка відправки';
        this.snackBar.open(msg, 'Закрити', { duration: 5000 });
      },
    });
  }

  onCmrSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.api.uploadCmr(this.declarationId, file).subscribe({
      next: (doc) => {
        this.cmr.set(doc);
        this.selectedFields.set(new Set(doc.extractedFields.map((f) => f.fieldKey)));
        this.snackBar.open('CMR розпізнано', 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Помилка завантаження CMR', 'Закрити', { duration: 4000 }),
    });
  }

  toggleField(key: string, checked: boolean): void {
    const next = new Set(this.selectedFields());
    if (checked) {
      next.add(key);
    } else {
      next.delete(key);
    }
    this.selectedFields.set(next);
  }

  applyCmr(): void {
    const keys = [...this.selectedFields()];
    if (keys.length === 0) {
      return;
    }
    this.api.applyCmr(this.declarationId, keys).subscribe({
      next: (d) => {
        this.declaration.set(d);
        this.form.patchValue({
          cmrNumber: d.cmrNumber ?? '',
          routeStartDate: d.routeStartDate ?? '',
          loadingCountry: d.loadingCountry ?? '',
          unloadingCountry: d.unloadingCountry ?? '',
        });
        this.snackBar.open('Поля застосовано', 'OK', { duration: 3000 });
        this.load();
      },
    });
  }

  confidenceClass(field: CmrExtractedField): string {
    return field.confidence < 0.7 ? 'warn' : 'ok';
  }

  private startPolling(): void {
    this.pollSub?.unsubscribe();
    this.polling.set(true);
    let attempts = 0;
    this.pollSub = interval(10000)
      .pipe(
        takeWhile(() => attempts < 12),
        switchMap(() => {
          attempts++;
          return this.api.poll(this.declarationId);
        }),
      )
      .subscribe({
        next: (d) => {
          this.declaration.set(d);
          if (d.status === 'REGISTERED' || d.status === 'REJECTED') {
            this.polling.set(false);
            this.pollSub?.unsubscribe();
            this.api.listEvents(this.declarationId).subscribe((e) => this.events.set(e));
            this.snackBar.open(`Статус: ${d.status}`, 'OK', { duration: 4000 });
          }
        },
      });
  }
}
