import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { ReferenceDataApiService } from '../../../core/services/reference-data-api.service';
import { Permit, PermitUpsert } from '../../../core/models/api.models';
import { PermitDialogComponent } from './permit-dialog.component';

@Component({
  selector: 'app-permits-page',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatSnackBarModule],
  templateUrl: './permits-page.component.html',
})
export class PermitsPageComponent implements OnInit {
  private readonly api = inject(ReferenceDataApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly items = signal<Permit[]>([]);
  readonly displayedColumns = ['type', 'number', 'validUntil', 'actions'];

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.listPermits().subscribe({
      next: (data) => this.items.set(data),
      error: () => this.snackBar.open('Помилка завантаження', 'Закрити', { duration: 4000 }),
    });
  }

  openCreate(): void {
    this.openDialog();
  }

  openEdit(item: Permit): void {
    this.openDialog(item);
  }

  delete(item: Permit): void {
    if (!confirm(`Видалити дозвіл ${item.permitNumber}?`)) return;
    this.api.deletePermit(item.id).subscribe({ next: () => this.reload() });
  }

  private openDialog(item?: Permit): void {
    const ref = this.dialog.open(PermitDialogComponent, { width: '480px', data: item ?? null });
    ref.afterClosed().subscribe((result?: PermitUpsert) => {
      if (!result) return;
      const req = item ? this.api.updatePermit(item.id, result) : this.api.createPermit(result);
      req.subscribe({ next: () => this.reload() });
    });
  }
}
