import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';
import { ShellComponent } from './layout/shell/shell.component';
import { HomeComponent } from './features/home/home.component';
import { LoginComponent } from './features/auth/login/login.component';
import { PlaceholderComponent } from './features/placeholder/placeholder.component';
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
        component: PlaceholderComponent,
        data: { title: 'Декларації RMPD100' },
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
        ],
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
