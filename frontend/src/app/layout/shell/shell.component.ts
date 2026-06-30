import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { HealthApiService } from '../../core/services/health-api.service';
import { AuthService } from '../../core/services/auth.service';
import { AppLang, LocaleService } from '../../core/services/locale.service';

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatSidenavModule,
    MatFormFieldModule,
    MatSelectModule,
    AsyncPipe,
    TranslateModule,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly healthApi = inject(HealthApiService);
  private readonly auth = inject(AuthService);
  readonly locale = inject(LocaleService);

  readonly health$ = this.healthApi.getHealth();
  readonly user = this.auth.currentUser;

  onLangChange(code: AppLang): void {
    this.locale.set(code);
  }

  logout(): void {
    this.auth.logout();
  }
}
