import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-placeholder',
  imports: [MatCardModule],
  template: `
    <mat-card>
      <mat-card-title>{{ title }}</mat-card-title>
      <mat-card-content>
        <p>Модуль буде реалізовано у фазі 1.</p>
      </mat-card-content>
    </mat-card>
  `,
})
export class PlaceholderComponent {
  private readonly route = inject(ActivatedRoute);
  title = this.route.snapshot.data['title'] ?? 'Скоро';
}
