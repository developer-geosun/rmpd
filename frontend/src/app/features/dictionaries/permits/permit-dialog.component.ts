import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Permit } from '../../../core/models/api.models';

@Component({
  selector: 'app-permit-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data ? 'Редагувати дозвіл' : 'Новий дозвіл' }}</h2>
    <div mat-dialog-content [formGroup]="form">
      <mat-form-field appearance="outline" class="full"><mat-label>Тип</mat-label><input matInput formControlName="permitType" /></mat-form-field>
      <mat-form-field appearance="outline" class="full"><mat-label>Номер</mat-label><input matInput formControlName="permitNumber" /></mat-form-field>
      <mat-form-field appearance="outline" class="full"><mat-label>Дійсний до</mat-label><input matInput type="date" formControlName="validUntil" /></mat-form-field>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Скасувати</button>
      <button mat-flat-button color="primary" (click)="save()">Зберегти</button>
    </div>
  `,
  styles: ['.full { width: 100%; }'],
})
export class PermitDialogComponent {
  readonly data = inject<Permit | null>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<PermitDialogComponent>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    permitType: [this.data?.permitType ?? 'EKMT', Validators.required],
    permitNumber: [this.data?.permitNumber ?? '', Validators.required],
    validUntil: [this.data?.validUntil ?? '', Validators.required],
  });

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.ref.close(this.form.getRawValue());
  }
}
