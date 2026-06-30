import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Vehicle } from '../../../core/models/api.models';

@Component({
  selector: 'app-vehicle-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data ? 'Редагувати ТЗ' : 'Нове ТЗ' }}</h2>
    <div mat-dialog-content [formGroup]="form">
      <mat-form-field appearance="outline" class="full">
        <mat-label>Країна реєстрації</mat-label>
        <input matInput formControlName="registrationCountry" maxlength="2" />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full">
        <mat-label>Номер тягача</mat-label>
        <input matInput formControlName="tractorNumber" />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full">
        <mat-label>Номер причепа</mat-label>
        <input matInput formControlName="trailerNumber" />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full">
        <mat-label>GPS ID (ZXX-XXXXXX-X)</mat-label>
        <input matInput formControlName="gpsDeviceId" />
      </mat-form-field>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Скасувати</button>
      <button mat-flat-button color="primary" (click)="save()">Зберегти</button>
    </div>
  `,
  styles: ['.full { width: 100%; }'],
})
export class VehicleDialogComponent {
  readonly data = inject<Vehicle | null>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<VehicleDialogComponent>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    registrationCountry: [this.data?.registrationCountry ?? 'UA', Validators.required],
    tractorNumber: [this.data?.tractorNumber ?? '', Validators.required],
    trailerNumber: [this.data?.trailerNumber ?? ''],
    gpsDeviceId: [this.data?.gpsDeviceId ?? 'ZXX-000000-0', Validators.required],
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.ref.close(this.form.getRawValue());
  }
}
