import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { ReferenceDataApiService } from '../../../core/services/reference-data-api.service';
import { Party, PartyUpsert } from '../../../core/models/api.models';
import { PartyDialogComponent } from './party-dialog.component';

@Component({
  selector: 'app-parties-page',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatSnackBarModule],
  templateUrl: './parties-page.component.html',
})
export class PartiesPageComponent implements OnInit {
  private readonly api = inject(ReferenceDataApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly items = signal<Party[]>([]);
  readonly displayedColumns = ['role', 'name', 'idNumber', 'actions'];

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.listParties().subscribe({
      next: (data) => this.items.set(data),
      error: () => this.snackBar.open('Помилка завантаження', 'Закрити', { duration: 4000 }),
    });
  }

  openCreate(): void {
    this.openDialog();
  }

  openEdit(item: Party): void {
    this.openDialog(item);
  }

  delete(item: Party): void {
    if (!confirm(`Видалити ${item.name}?`)) return;
    this.api.deleteParty(item.id).subscribe({ next: () => this.reload() });
  }

  private openDialog(item?: Party): void {
    const ref = this.dialog.open(PartyDialogComponent, { width: '560px', data: item ?? null });
    ref.afterClosed().subscribe((result?: PartyUpsert) => {
      if (!result) return;
      const req = item ? this.api.updateParty(item.id, result) : this.api.createParty(result);
      req.subscribe({ next: () => this.reload() });
    });
  }
}
