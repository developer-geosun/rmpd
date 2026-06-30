import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Party, PartyRole } from '../../../core/models/api.models';

@Component({
  selector: 'app-party-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data ? 'Редагувати контрагента' : 'Новий контрагент' }}</h2>
    <div mat-dialog-content [formGroup]="form">
      <mat-form-field appearance="outline" class="full">
        <mat-label>Роль</mat-label>
        <mat-select formControlName="partyRole">
          <mat-option value="SENDER">Відправник</mat-option>
          <mat-option value="RECEIVER">Отримувач</mat-option>
          <mat-option value="BOTH">Обидва</mat-option>
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline" class="full"><mat-label>Тип ID</mat-label><input matInput formControlName="idType" /></mat-form-field>
      <mat-form-field appearance="outline" class="full"><mat-label>Номер ID</mat-label><input matInput formControlName="idNumber" /></mat-form-field>
      <mat-form-field appearance="outline" class="full"><mat-label>Назва</mat-label><input matInput formControlName="name" /></mat-form-field>
      <mat-form-field appearance="outline" class="full"><mat-label>Країна</mat-label><input matInput formControlName="country" /></mat-form-field>
      <mat-form-field appearance="outline" class="full"><mat-label>Місто</mat-label><input matInput formControlName="city" /></mat-form-field>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Скасувати</button>
      <button mat-flat-button color="primary" (click)="save()">Зберегти</button>
    </div>
  `,
  styles: ['.full { width: 100%; }'],
})
export class PartyDialogComponent {
  readonly data = inject<Party | null>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<PartyDialogComponent>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    partyRole: [this.data?.partyRole ?? ('SENDER' as PartyRole), Validators.required],
    idType: [this.data?.idType ?? 'INNY', Validators.required],
    idNumber: [this.data?.idNumber ?? '', Validators.required],
    name: [this.data?.name ?? '', Validators.required],
    country: [this.data?.address.country ?? 'UA', Validators.required],
    city: [this.data?.address.city ?? '', Validators.required],
    postalCode: [this.data?.address.postalCode ?? '00000', Validators.required],
    street: [this.data?.address.street ?? 'BRAK', Validators.required],
    buildingNumber: [this.data?.address.buildingNumber ?? '1', Validators.required],
    unitNumber: [this.data?.address.unitNumber ?? 'BRAK'],
  });

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.ref.close({
      partyRole: v.partyRole,
      idType: v.idType,
      idNumber: v.idNumber,
      name: v.name,
      address: {
        country: v.country,
        city: v.city,
        postalCode: v.postalCode,
        street: v.street,
        buildingNumber: v.buildingNumber,
        unitNumber: v.unitNumber,
      },
    });
  }
}
