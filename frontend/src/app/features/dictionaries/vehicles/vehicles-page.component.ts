import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { ReferenceDataApiService } from '../../../core/services/reference-data-api.service';
import { Vehicle, VehicleUpsert } from '../../../core/models/api.models';
import { VehicleDialogComponent } from './vehicle-dialog.component';

@Component({
  selector: 'app-vehicles-page',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatDialogModule, MatSnackBarModule],
  templateUrl: './vehicles-page.component.html',
})
export class VehiclesPageComponent implements OnInit {
  private readonly api = inject(ReferenceDataApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly items = signal<Vehicle[]>([]);
  readonly displayedColumns = ['country', 'tractor', 'trailer', 'gps', 'actions'];

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.listVehicles().subscribe({
      next: (data) => this.items.set(data),
      error: () => this.snackBar.open('Помилка завантаження ТЗ', 'Закрити', { duration: 4000 }),
    });
  }

  openCreate(): void {
    this.openDialog();
  }

  openEdit(vehicle: Vehicle): void {
    this.openDialog(vehicle);
  }

  delete(vehicle: Vehicle): void {
    if (!confirm(`Видалити ТЗ ${vehicle.tractorNumber}?`)) {
      return;
    }
    this.api.deleteVehicle(vehicle.id).subscribe({
      next: () => this.reload(),
      error: () => this.snackBar.open('Не вдалося видалити', 'Закрити', { duration: 4000 }),
    });
  }

  private openDialog(vehicle?: Vehicle): void {
    const ref = this.dialog.open(VehicleDialogComponent, {
      width: '480px',
      data: vehicle ?? null,
    });
    ref.afterClosed().subscribe((result?: VehicleUpsert) => {
      if (!result) {
        return;
      }
      const req = vehicle
        ? this.api.updateVehicle(vehicle.id, result)
        : this.api.createVehicle(result);
      req.subscribe({
        next: () => this.reload(),
        error: () => this.snackBar.open('Помилка збереження', 'Закрити', { duration: 4000 }),
      });
    });
  }
}
