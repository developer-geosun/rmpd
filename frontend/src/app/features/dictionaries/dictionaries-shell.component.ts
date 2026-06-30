import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';

@Component({
  selector: 'app-dictionaries-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatTabsModule],
  template: `
    <nav class="tabs">
      <a routerLink="vehicles" routerLinkActive="active">ТЗ</a>
      <a routerLink="permits" routerLinkActive="active">Дозволи</a>
      <a routerLink="parties" routerLinkActive="active">Контрагенти</a>
      <a routerLink="carrier" routerLinkActive="active">Профіль перевізника</a>
    </nav>
    <router-outlet />
  `,
  styles: [
    `
      .tabs {
        display: flex;
        gap: 1rem;
        margin-bottom: 1rem;
      }
      .tabs a {
        text-decoration: none;
        padding: 0.5rem 0.75rem;
        border-radius: 4px;
      }
      .tabs a.active {
        background: rgba(25, 118, 210, 0.12);
        color: #1976d2;
      }
    `,
  ],
})
export class DictionariesShellComponent {}
