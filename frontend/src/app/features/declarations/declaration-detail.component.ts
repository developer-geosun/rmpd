import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatStepperModule } from '@angular/material/stepper';
import { debounceTime, filter, interval, Subscription, switchMap, takeWhile } from 'rxjs';
import { DeclarationsApiService } from '../../core/services/declarations-api.service';
import { ReferenceDataApiService } from '../../core/services/reference-data-api.service';
import {
  CmrDocument,
  CmrExtractedField,
  Declaration,
  DeclarationEvent,
  DictionaryEntry,
  RoutePoint,
  TransportType,
} from '../../core/models/declaration.models';
import { CarrierProfile, Party, Permit, Vehicle } from '../../core/models/api.models';
import { latinInputValidator } from '../../core/validators/latin.validator';

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
    MatStepperModule,
    MatProgressBarModule,
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
  readonly saving = signal(false);
  readonly progressPercent = signal(0);
  readonly missingFields = signal<string[]>([]);
  readonly carrier = signal<CarrierProfile | null>(null);

  vehicles: Vehicle[] = [];
  permits: Permit[] = [];
  parties: Party[] = [];
  countries: DictionaryEntry[] = [];

  private pollSub?: Subscription;
  private autoSaveSub?: Subscription;
  private declarationId = 0;
  private editable = true;

  readonly transportTypes: { value: TransportType; label: string }[] = [
    { value: 'LADEN', label: 'Laden (вантажне)' },
    { value: 'EMPTY', label: 'Empty (порожнє)' },
    { value: 'TRANSIT', label: 'Transit (транзит)' },
    { value: 'CABOTAGE', label: 'Cabotage (каботаж)' },
  ];

  readonly routePointTypes = [
    { value: 'ENTRY', label: 'В\'їзд (ENTRY)' },
    { value: 'EXIT', label: 'Виїзд (EXIT)' },
  ];

  readonly form = this.fb.group({
    vehicleId: [null as number | null, Validators.required],
    permitId: [null as number | null],
    transportType: ['LADEN' as TransportType, Validators.required],
    cmrNumber: ['', latinInputValidator()],
    routeStartDate: ['', Validators.required],
    routeEndDate: ['', Validators.required],
    loadingCountry: ['', Validators.required],
    unloadingCountry: ['', Validators.required],
    routePoints: this.fb.array<FormGroup>([]),
    senderPartyId: [null as number | null],
    receiverPartyId: [null as number | null],
    comment: ['', latinInputValidator()],
    termsAccepted: [false],
  });

  requiresParties(): boolean {
    const type = this.form.controls.transportType.value;
    return type === 'LADEN' || type === 'CABOTAGE' || type == null;
  }

  requiresPermit(): boolean {
    const type = this.form.controls.transportType.value;
    return type === 'LADEN' || type === 'CABOTAGE';
  }

  senderParties(): Party[] {
    return this.parties.filter((p) => p.partyRole === 'SENDER' || p.partyRole === 'BOTH');
  }

  receiverParties(): Party[] {
    return this.parties.filter((p) => p.partyRole === 'RECEIVER' || p.partyRole === 'BOTH');
  }

  get routePoints(): FormArray<FormGroup> {
    return this.form.controls.routePoints;
  }

  ngOnInit(): void {
    this.declarationId = Number(this.route.snapshot.paramMap.get('id'));
    this.refApi.listVehicles().subscribe((v) => (this.vehicles = v));
    this.refApi.listPermits().subscribe((p) => (this.permits = p));
    this.refApi.listParties().subscribe((p) => (this.parties = p));
    this.refApi.getCarrierProfile().subscribe((c) => this.carrier.set(c));
    this.api.listCountries().subscribe((c) => (this.countries = c));
    this.setupAutoSave();
    this.form.controls.transportType.valueChanges.subscribe(() => this.updateConditionalValidators());
    this.load();
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
    this.autoSaveSub?.unsubscribe();
  }

  addRoutePoint(point?: RoutePoint): void {
    this.routePoints.push(
      this.fb.group({
        type: [point?.type ?? 'ENTRY', Validators.required],
        name: [point?.name ?? '', [Validators.required, latinInputValidator()]],
        country: [point?.country ?? 'PL', Validators.required],
      }),
    );
  }

  removeRoutePoint(index: number): void {
    this.routePoints.removeAt(index);
  }

  onStepChange(): void {
    if (this.editable) {
      this.save(true);
    }
  }

  save(silent = false): void {
    if (!this.editable || this.saving()) {
      return;
    }
    this.saving.set(true);
    const payload = this.buildPayload();
    this.api.update(this.declarationId, payload).subscribe({
      next: (d) => {
        this.declaration.set(d);
        this.progressPercent.set(d.completionPercent ?? 0);
        this.saving.set(false);
        if (!silent) {
          this.snackBar.open('Збережено', 'OK', { duration: 2000 });
        }
        this.refreshProgress();
      },
      error: () => {
        this.saving.set(false);
        if (!silent) {
          this.snackBar.open('Помилка збереження', 'Закрити', { duration: 4000 });
        }
      },
    });
  }

  validate(): void {
    this.save(true);
    this.api.validate(this.declarationId).subscribe({
      next: (r) => {
        if (r.valid) {
          this.snackBar.open('Валідація пройдена', 'OK', { duration: 3000 });
          this.load();
        } else {
          this.snackBar.open(r.errors.join('; '), 'Закрити', { duration: 8000 });
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

  vehicleLabel(v: Vehicle): string {
    return `${v.tractorNumber}${v.trailerNumber ? ' / ' + v.trailerNumber : ''} (${v.registrationCountry})`;
  }

  partyLabel(p: Party): string {
    return `${p.name} (${p.idNumber})`;
  }

  countryLabel(code: string | null | undefined): string {
    if (!code) {
      return '—';
    }
    const c = this.countries.find((x) => x.code === code);
    return c ? `${code} — ${c.labelPl}` : code;
  }

  private load(): void {
    this.api.get(this.declarationId).subscribe({
      next: (d) => {
        this.declaration.set(d);
        this.editable = d.status === 'DRAFT' || d.status === 'VALIDATED';
        this.progressPercent.set(d.completionPercent ?? 0);
        this.patchForm(d);
        if (!this.editable) {
          this.form.disable();
        }
        if (d.status === 'SUBMITTED') {
          this.startPolling();
        }
        this.updateConditionalValidators();
        this.refreshProgress();
      },
      error: () => this.snackBar.open('Декларацію не знайдено', 'Закрити', { duration: 4000 }),
    });
    this.api.listEvents(this.declarationId).subscribe({
      next: (e) => this.events.set(e),
    });
  }

  private patchForm(d: Declaration): void {
    this.routePoints.clear();
    const points = this.parseRoutePoints(d.routePointsJson);
    if (points.length === 0) {
      this.addRoutePoint();
    } else {
      points.forEach((p) => this.addRoutePoint(p));
    }
    this.form.patchValue({
      vehicleId: d.vehicleId ?? null,
      permitId: d.permitId ?? null,
      transportType: d.transportType ?? 'LADEN',
      cmrNumber: d.cmrNumber ?? '',
      routeStartDate: d.routeStartDate ?? '',
      routeEndDate: d.routeEndDate ?? '',
      loadingCountry: d.loadingCountry ?? '',
      unloadingCountry: d.unloadingCountry ?? '',
      senderPartyId: d.senderPartyId ?? null,
      receiverPartyId: d.receiverPartyId ?? null,
      comment: d.comment ?? '',
      termsAccepted: d.termsAccepted ?? false,
    });
  }

  private parseRoutePoints(json?: string): RoutePoint[] {
    if (!json) {
      return [];
    }
    try {
      return JSON.parse(json) as RoutePoint[];
    } catch {
      return [];
    }
  }

  private buildPayload() {
    const raw = this.form.getRawValue();
    const routePoints: RoutePoint[] = raw.routePoints
      .filter((p) => p['name'])
      .map((p) => ({
        type: p['type'] as RoutePoint['type'],
        name: p['name']!,
        country: (p['country'] ?? 'PL').toUpperCase(),
      }));
    return {
      transportType: raw.transportType ?? undefined,
      cmrNumber: raw.cmrNumber || undefined,
      routeStartDate: raw.routeStartDate || undefined,
      routeEndDate: raw.routeEndDate || undefined,
      loadingCountry: raw.loadingCountry || undefined,
      unloadingCountry: raw.unloadingCountry || undefined,
      vehicleId: raw.vehicleId ?? undefined,
      permitId: raw.permitId ?? undefined,
      senderPartyId: this.requiresParties() ? raw.senderPartyId ?? undefined : undefined,
      receiverPartyId: this.requiresParties() ? raw.receiverPartyId ?? undefined : undefined,
      routePointsJson: routePoints.length ? JSON.stringify(routePoints) : undefined,
      comment: raw.comment || undefined,
      termsAccepted: raw.termsAccepted ?? false,
    };
  }

  private setupAutoSave(): void {
    this.autoSaveSub = this.form.valueChanges
      .pipe(
        debounceTime(30_000),
        filter(() => this.editable),
      )
      .subscribe(() => this.save(true));
  }

  private updateConditionalValidators(): void {
    const permit = this.form.controls.permitId;
    const sender = this.form.controls.senderPartyId;
    const receiver = this.form.controls.receiverPartyId;
    if (this.requiresPermit()) {
      permit.setValidators(Validators.required);
    } else {
      permit.clearValidators();
      permit.setValue(null);
    }
    if (this.requiresParties()) {
      sender.setValidators(Validators.required);
      receiver.setValidators(Validators.required);
    } else {
      sender.clearValidators();
      receiver.clearValidators();
      sender.setValue(null);
      receiver.setValue(null);
    }
    permit.updateValueAndValidity({ emitEvent: false });
    sender.updateValueAndValidity({ emitEvent: false });
    receiver.updateValueAndValidity({ emitEvent: false });
  }

  private refreshProgress(): void {
    this.api.progress(this.declarationId).subscribe({
      next: (p) => {
        this.progressPercent.set(p.completionPercent);
        this.missingFields.set(p.missingFields);
      },
    });
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
