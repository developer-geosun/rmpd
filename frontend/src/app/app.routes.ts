import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';
import { ShellComponent } from './layout/shell/shell.component';
import { HomeComponent } from './features/home/home.component';
import { LoginComponent } from './features/auth/login/login.component';
import { DeclarationsListComponent } from './features/declarations/declarations-list.component';
import { DeclarationDetailComponent } from './features/declarations/declaration-detail.component';
import { PuescSettingsComponent } from './features/settings/puesc-settings/puesc-settings.component';
import { DictionariesShellComponent } from './features/dictionaries/dictionaries-shell.component';
import { VehiclesPageComponent } from './features/dictionaries/vehicles/vehicles-page.component';
import { PermitsPageComponent } from './features/dictionaries/permits/permits-page.component';
import { PartiesPageComponent } from './features/dictionaries/parties/parties-page.component';
import { CarrierProfileComponent } from './features/settings/carrier-profile/carrier-profile.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [guestGuard],
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', component: HomeComponent },
      {
        path: 'declarations',
        children: [
          { path: '', component: DeclarationsListComponent },
          { path: ':id', component: DeclarationDetailComponent },
        ],
      },
      {
        path: 'settings/puesc',
        component: PuescSettingsComponent,
        data: { title: 'PUESC' },
      },
      {
        path: 'dictionaries',
        component: DictionariesShellComponent,
        children: [
          { path: '', redirectTo: 'vehicles', pathMatch: 'full' },
          { path: 'vehicles', component: VehiclesPageComponent },
          { path: 'permits', component: PermitsPageComponent },
          { path: 'parties', component: PartiesPageComponent },
          { path: 'carrier', component: CarrierProfileComponent },
          { path: 'puesc', component: PuescSettingsComponent },
        ],
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
