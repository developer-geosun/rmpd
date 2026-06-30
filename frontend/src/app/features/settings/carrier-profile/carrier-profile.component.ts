import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ReferenceDataApiService } from '../../../core/services/reference-data-api.service';

@Component({
  selector: 'app-carrier-profile',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  templateUrl: './carrier-profile.component.html',
})
export class CarrierProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ReferenceDataApiService);
  private readonly snackBar = inject(MatSnackBar);

  loading = true;

  readonly form = this.fb.nonNullable.group({
    idType: ['INNY', Validators.required],
    idNumber: ['', Validators.required],
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    country: ['', Validators.required],
    city: ['', Validators.required],
    postalCode: ['', Validators.required],
    street: ['', Validators.required],
    buildingNumber: ['', Validators.required],
    unitNumber: ['BRAK'],
  });

  ngOnInit(): void {
    this.api.getCarrierProfile().subscribe({
      next: (profile) => {
        this.form.patchValue({
          idType: profile.idType,
          idNumber: profile.idNumber,
          name: profile.name,
          email: profile.email,
          country: profile.address.country,
          city: profile.address.city,
          postalCode: profile.address.postalCode,
          street: profile.address.street,
          buildingNumber: profile.address.buildingNumber,
          unitNumber: profile.address.unitNumber ?? 'BRAK',
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Не вдалося завантажити профіль', 'Закрити', { duration: 4000 });
      },
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.api
      .updateCarrierProfile({
        idType: v.idType,
        idNumber: v.idNumber,
        name: v.name,
        email: v.email,
        address: {
          country: v.country,
          city: v.city,
          postalCode: v.postalCode,
          street: v.street,
          buildingNumber: v.buildingNumber,
          unitNumber: v.unitNumber,
        },
      })
      .subscribe({
        next: () => this.snackBar.open('Профіль збережено', 'OK', { duration: 3000 }),
        error: () => this.snackBar.open('Помилка збереження', 'Закрити', { duration: 4000 }),
      });
  }
}
