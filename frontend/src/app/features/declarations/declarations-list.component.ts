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
import { TranslateModule, TranslateService } from '@ngx-translate/core';
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
    TranslateModule,
  ],
  templateUrl: './declarations-list.component.html',
})
export class DeclarationsListComponent implements OnInit {
  private readonly api = inject(DeclarationsApiService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);

  readonly items = signal<Declaration[]>([]);
  readonly statusFilter = signal<DeclarationStatus | ''>('');
  readonly batchUploading = signal(false);
  readonly displayedColumns = ['id', 'status', 'progress', 'cmrNumber', 'referenceNumber', 'updatedAt', 'actions'];

  readonly statusOptions: { value: DeclarationStatus | ''; labelKey: string }[] = [
    { value: '', labelKey: 'declarations.all' },
    { value: 'DRAFT', labelKey: 'DRAFT' },
    { value: 'VALIDATED', labelKey: 'VALIDATED' },
    { value: 'SUBMITTED', labelKey: 'SUBMITTED' },
    { value: 'REGISTERED', labelKey: 'REGISTERED' },
    { value: 'REJECTED', labelKey: 'REJECTED' },
    { value: 'ERROR', labelKey: 'ERROR' },
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
      error: () =>
        this.snackBar.open(this.translate.instant('declarations.loadError'), this.translate.instant('common.close'), {
          duration: 4000,
        }),
    });
  }

  create(): void {
    this.api.create().subscribe({
      next: (d) => this.router.navigate(['/declarations', d.id]),
      error: () =>
        this.snackBar.open(this.translate.instant('declarations.createError'), this.translate.instant('common.close'), {
          duration: 4000,
        }),
    });
  }

  copy(id: number, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.api.copy(id).subscribe({
      next: (d) => {
        this.snackBar.open(this.translate.instant('declarations.copyOk'), this.translate.instant('common.ok'), {
          duration: 2500,
        });
        this.router.navigate(['/declarations', d.id]);
      },
      error: () =>
        this.snackBar.open(this.translate.instant('declarations.copyError'), this.translate.instant('common.close'), {
          duration: 4000,
        }),
    });
  }

  onBatchSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];
    input.value = '';
    if (files.length === 0) {
      return;
    }
    this.batchUploading.set(true);
    this.api.batchUploadCmr(files).subscribe({
      next: (result) => {
        this.batchUploading.set(false);
        this.snackBar.open(
          this.translate.instant('cmr.batchDone', { ok: result.succeeded, total: result.total }),
          this.translate.instant('common.ok'),
          { duration: 4000 },
        );
        this.reload();
      },
      error: () => {
        this.batchUploading.set(false);
        this.snackBar.open(this.translate.instant('cmr.batchError'), this.translate.instant('common.close'), {
          duration: 4000,
        });
      },
    });
  }
}
