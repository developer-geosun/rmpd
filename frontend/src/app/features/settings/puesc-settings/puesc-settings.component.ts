import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DeclarationsApiService } from '../../../core/services/declarations-api.service';
import { PuescConnectionTest } from '../../../core/models/declaration.models';

@Component({
  selector: 'app-puesc-settings',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  templateUrl: './puesc-settings.component.html',
})
export class PuescSettingsComponent implements OnInit {
  private readonly api = inject(DeclarationsApiService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly testResult = signal<PuescConnectionTest | null>(null);
  readonly passwordConfigured = signal(false);

  readonly form = this.fb.group({
    username: ['', [Validators.required, Validators.email]],
    password: [''],
    signingCertPath: [''],
    idSiscRop: [''],
    idSiscRof: [''],
    idSiscP: [''],
  });

  ngOnInit(): void {
    this.api.getPuescSettings().subscribe({
      next: (s) => {
        this.passwordConfigured.set(s.passwordConfigured);
        this.form.patchValue({
          username: s.username,
          signingCertPath: s.signingCertPath ?? '',
          idSiscRop: s.idSiscRop ?? '',
          idSiscRof: s.idSiscRof ?? '',
          idSiscP: s.idSiscP ?? '',
        });
      },
    });
  }

  save(): void {
    if (this.form.invalid) {
      return;
    }
    const raw = this.form.getRawValue();
    this.api
      .savePuescSettings({
        username: raw.username!,
        password: raw.password || undefined,
        signingCertPath: raw.signingCertPath || undefined,
        idSiscRop: raw.idSiscRop || undefined,
        idSiscRof: raw.idSiscRof || undefined,
        idSiscP: raw.idSiscP || undefined,
        environment: 'TEST',
      })
      .subscribe({
        next: () => {
          this.passwordConfigured.set(true);
          this.form.patchValue({ password: '' });
          this.snackBar.open('Налаштування збережено', 'OK', { duration: 3000 });
        },
        error: (err) => {
          const msg = err?.error?.message ?? 'Помилка збереження';
          this.snackBar.open(msg, 'Закрити', { duration: 5000 });
        },
      });
  }

  test(): void {
    this.api.testPuescConnection().subscribe({
      next: (r) => {
        this.testResult.set(r);
        this.snackBar.open(r.message, 'OK', { duration: 4000 });
      },
      error: () => this.snackBar.open('Тест не вдався', 'Закрити', { duration: 4000 }),
    });
  }
}
