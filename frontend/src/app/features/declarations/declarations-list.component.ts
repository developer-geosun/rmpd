import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { DeclarationsApiService } from '../../core/services/declarations-api.service';
import { Declaration, DeclarationStatus } from '../../core/models/declaration.models';

@Component({
  selector: 'app-declarations-list',
  imports: [
    DatePipe,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatFormFieldModule,
    MatSelectModule,
    MatProgressBarModule,
    RouterLink,
  ],
  templateUrl: './declarations-list.component.html',
})
export class DeclarationsListComponent implements OnInit {
  private readonly api = inject(DeclarationsApiService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly items = signal<Declaration[]>([]);
  readonly statusFilter = signal<DeclarationStatus | ''>('');
  readonly displayedColumns = ['id', 'status', 'progress', 'cmrNumber', 'referenceNumber', 'updatedAt', 'actions'];

  readonly statusOptions: { value: DeclarationStatus | ''; label: string }[] = [
    { value: '', label: 'Усі' },
    { value: 'DRAFT', label: 'Чернетка' },
    { value: 'VALIDATED', label: 'Перевірено' },
    { value: 'SUBMITTED', label: 'Відправлено' },
    { value: 'REGISTERED', label: 'Зареєстровано' },
    { value: 'REJECTED', label: 'Відхилено' },
    { value: 'ERROR', label: 'Помилка' },
  ];

  ngOnInit(): void {
    this.reload();
  }

  onFilterChange(value: DeclarationStatus | ''): void {
    this.statusFilter.set(value);
    this.reload();
  }

  reload(): void {
    const status = this.statusFilter();
    this.api.list(status || undefined).subscribe({
      next: (data) => this.items.set(data),
      error: () => this.snackBar.open('Помилка завантаження', 'Закрити', { duration: 4000 }),
    });
  }

  create(): void {
    this.api.create().subscribe({
      next: (d) => this.router.navigate(['/declarations', d.id]),
      error: () => this.snackBar.open('Не вдалося створити', 'Закрити', { duration: 4000 }),
    });
  }
}
