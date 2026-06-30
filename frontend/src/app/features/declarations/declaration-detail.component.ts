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
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { debounceTime, filter, interval, Subscription, switchMap, takeWhile } from 'rxjs';
import { DeclarationsApiService } from '../../core/services/declarations-api.service';
import { ReferenceDataApiService } from '../../core/services/reference-data-api.service';
import { SubmitProgressDialogComponent } from './submit-progress-dialog.component';
import {
  AmendmentRequest,
  CmrDocument,
  CmrExtractedField,
  CmrPartySuggestions,
  Declaration,
  DeclarationEvent,
  DictionaryEntry,
  GpsCheckResult,
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
    MatDialogModule,
    TranslateModule,
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
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly declaration = signal<Declaration | null>(null);
  readonly events = signal<DeclarationEvent[]>([]);
  readonly cmr = signal<CmrDocument | null>(null);
  readonly selectedFields = signal<Set<string>>(new Set());
  readonly submitting = signal(false);
  readonly polling = signal(false);
  readonly saving = signal(false);
  readonly progressPercent = signal(0);
  readonly missingFields = signal<string[]>([]);
  readonly validationErrors = signal<string[]>([]);
  readonly xmlPreview = signal<string | null>(null);
  readonly showXmlPreview = signal(false);
  readonly cmrPreviewUrl = signal<string | null>(null);
  readonly cmrDragOver = signal(false);
  readonly carrier = signal<CarrierProfile | null>(null);
  readonly amendmentReason = signal('');
  readonly gpsResult = signal<GpsCheckResult | null>(null);
  readonly cmrPartyHints = signal<CmrPartySuggestions | null>(null);

  readonly amendForm = this.fb.group({
    vehicleId: [null as number | null],
    routeStartDate: [''],
    routeEndDate: [''],
    comment: ['', latinInputValidator()],
    amendmentReason: ['', Validators.required],
  });

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
    this.revokeCmrPreview();
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
          this.snackBar.open(this.translate.instant('declarations.saved'), this.translate.instant('common.ok'), {
            duration: 2000,
          });
        }
        this.refreshProgress();
      },
      error: () => {
        this.saving.set(false);
        if (!silent) {
          this.snackBar.open(this.translate.instant('declarations.saveError'), this.translate.instant('common.close'), {
            duration: 4000,
          });
        }
      },
    });
  }

  validate(): void {
    this.save(true);
    this.api.validate(this.declarationId).subscribe({
      next: (r) => {
        this.validationErrors.set(r.valid ? [] : r.errors);
        if (r.valid) {
          this.snackBar.open(this.translate.instant('declarations.validateOk'), this.translate.instant('common.ok'), {
            duration: 3000,
          });
          this.load();
        } else {
          this.snackBar.open(r.errors.join('; '), this.translate.instant('common.close'), { duration: 8000 });
        }
      },
    });
  }

  toggleXmlPreview(): void {
    if (this.showXmlPreview()) {
      this.showXmlPreview.set(false);
      return;
    }
    this.api.fetchXmlText(this.declarationId).subscribe({
      next: (text) => {
        this.xmlPreview.set(text);
        this.showXmlPreview.set(true);
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
    const dialogRef = this.dialog.open(SubmitProgressDialogComponent, {
      disableClose: true,
      data: { state: 'submitting' as const },
    });
    this.api.submit(this.declarationId).subscribe({
      next: () => {
        this.submitting.set(false);
        dialogRef.close();
        this.snackBar.open(this.translate.instant('declarations.submitted'), this.translate.instant('common.ok'), {
          duration: 3000,
        });
        this.load();
        this.startPolling();
      },
      error: (err) => {
        this.submitting.set(false);
        dialogRef.close();
        const msg = err?.error?.message ?? this.translate.instant('declarations.submitError');
        this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
      },
    });
  }

  onCmrDragOver(event: DragEvent): void {
    event.preventDefault();
    this.cmrDragOver.set(true);
  }

  onCmrDragLeave(): void {
    this.cmrDragOver.set(false);
  }

  onCmrDrop(event: DragEvent): void {
    event.preventDefault();
    this.cmrDragOver.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.uploadCmrFile(file);
    }
  }

  onCmrSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.uploadCmrFile(file);
  }

  private uploadCmrFile(file: File): void {
    this.api.uploadCmr(this.declarationId, file).subscribe({
      next: (doc) => {
        this.cmr.set(doc);
        this.selectedFields.set(new Set(doc.extractedFields.map((f) => f.fieldKey)));
        this.loadCmrPreview();
        this.loadCmrPartyHints();
        this.snackBar.open(this.translate.instant('cmr.recognized'), this.translate.instant('common.ok'), {
          duration: 3000,
        });
      },
      error: () =>
        this.snackBar.open(this.translate.instant('cmr.uploadError'), this.translate.instant('common.close'), {
          duration: 4000,
        }),
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
        this.snackBar.open(this.translate.instant('cmr.applied'), this.translate.instant('common.ok'), {
          duration: 3000,
        });
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

  isRegistered(): boolean {
    return this.declaration()?.status === 'REGISTERED';
  }

  saveAmendment(): void {
    if (this.amendForm.invalid) {
      return;
    }
    const raw = this.amendForm.getRawValue();
    const payload: AmendmentRequest = {
      vehicleId: raw.vehicleId ?? undefined,
      routeStartDate: raw.routeStartDate || undefined,
      routeEndDate: raw.routeEndDate || undefined,
      comment: raw.comment || undefined,
      amendmentReason: raw.amendmentReason || undefined,
    };
    this.api.amend(this.declarationId, payload).subscribe({
      next: () => {
        this.snackBar.open(this.translate.instant('amend.saved'), this.translate.instant('common.ok'), { duration: 3000 });
        this.load();
      },
    });
  }

  submitAmendment(): void {
    if (this.amendForm.invalid) {
      return;
    }
    const raw = this.amendForm.getRawValue();
    const payload: AmendmentRequest = {
      vehicleId: raw.vehicleId ?? undefined,
      routeStartDate: raw.routeStartDate || undefined,
      routeEndDate: raw.routeEndDate || undefined,
      comment: raw.comment || undefined,
      amendmentReason: raw.amendmentReason || undefined,
    };
    this.api.submitAmendment(this.declarationId, payload).subscribe({
      next: () => {
        this.snackBar.open(this.translate.instant('amend.submitted'), this.translate.instant('common.ok'), { duration: 3000 });
        this.load();
      },
    });
  }

  downloadAmendXml(): void {
    this.api.downloadAmendXml(this.declarationId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `rmpd-amend-${this.declarationId}.xml`;
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  runGpsCheck(): void {
    this.api.gpsCheck(this.declarationId).subscribe({
      next: (r) => {
        this.gpsResult.set(r);
        this.snackBar.open(r.message, this.translate.instant('common.ok'), { duration: 4000 });
      },
    });
  }

  submitGpsCheck(): void {
    this.api.submitGpsCheck(this.declarationId).subscribe({
      next: () => {
        this.snackBar.open(this.translate.instant('gps.submitted'), this.translate.instant('common.ok'), {
          duration: 3000,
        });
        this.load();
      },
      error: (err) => {
        const msg = err?.error?.message ?? this.translate.instant('gps.submitError');
        this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
      },
    });
  }

  downloadGpsXml(): void {
    this.api.downloadGpsCheckXml(this.declarationId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `rmpd406-${this.declarationId}.xml`;
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  applyPartyHint(role: 'sender' | 'receiver'): void {
    const hints = this.cmrPartyHints();
    const match = role === 'sender' ? hints?.senderMatch : hints?.receiverMatch;
    if (!match) {
      return;
    }
    if (role === 'sender') {
      this.form.patchValue({ senderPartyId: match.partyId });
    } else {
      this.form.patchValue({ receiverPartyId: match.partyId });
    }
    this.snackBar.open(this.translate.instant('parties.hintApplied'), this.translate.instant('common.ok'), {
      duration: 2500,
    });
  }

  private loadCmrPartyHints(): void {
    this.api.getCmrPartySuggestions(this.declarationId).subscribe({
      next: (hints) => this.cmrPartyHints.set(hints),
      error: () => this.cmrPartyHints.set(null),
    });
  }

  private load(): void {
    this.api.get(this.declarationId).subscribe({
      next: (d) => {
        this.declaration.set(d);
        this.editable = d.status === 'DRAFT' || d.status === 'VALIDATED';
        this.progressPercent.set(d.completionPercent ?? 0);
        this.patchForm(d);
        if (d.status === 'REGISTERED') {
          this.amendForm.patchValue({
            vehicleId: d.vehicleId ?? null,
            routeStartDate: d.routeStartDate ?? '',
            routeEndDate: d.routeEndDate ?? '',
            comment: d.comment ?? '',
          });
        }
        if (!this.editable) {
          this.form.disable();
        }
        if (d.status === 'SUBMITTED') {
          this.startPolling();
        }
        this.updateConditionalValidators();
        this.refreshProgress();
      },
      error: () => this.snackBar.open(this.translate.instant('declarations.notFound'), this.translate.instant('common.close'), { duration: 4000 }),
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
            this.snackBar.open(`Статус: ${d.status}`, this.translate.instant('common.ok'), { duration: 4000 });
          }
        },
      });
  }

  private loadCmrPreview(): void {
    this.revokeCmrPreview();
    this.api.fetchCmrPreview(this.declarationId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        this.cmrPreviewUrl.set(url);
      },
    });
  }

  private revokeCmrPreview(): void {
    const url = this.cmrPreviewUrl();
    if (url) {
      URL.revokeObjectURL(url);
      this.cmrPreviewUrl.set(null);
    }
  }
}
