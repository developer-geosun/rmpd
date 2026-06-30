import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { DeclarationsApiService } from '../../core/services/declarations-api.service';
import { Declaration } from '../../core/models/declaration.models';

@Component({
  selector: 'app-declarations-list',
  imports: [DatePipe, MatTableModule, MatButtonModule, MatIconModule, MatSnackBarModule, RouterLink],
  templateUrl: './declarations-list.component.html',
})
export class DeclarationsListComponent implements OnInit {
  private readonly api = inject(DeclarationsApiService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly items = signal<Declaration[]>([]);
  readonly displayedColumns = ['id', 'status', 'cmrNumber', 'referenceNumber', 'updatedAt', 'actions'];

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.list().subscribe({
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
