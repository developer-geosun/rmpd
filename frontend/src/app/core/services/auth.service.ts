import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, throwError, catchError, switchMap } from 'rxjs';
import { AuthTokens, LoginRequest, UserInfo } from '../models/api.models';

const ACCESS_KEY = 'rmpd_access_token';
const REFRESH_KEY = 'rmpd_refresh_token';
const USER_KEY = 'rmpd_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly currentUser = signal<UserInfo | null>(this.readUser());

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
  ) {}

  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.accessToken;
  }

  login(request: LoginRequest): Observable<AuthTokens> {
    return this.http.post<AuthTokens>('/api/v1/auth/login', request).pipe(
      tap((tokens) => this.store(tokens)),
    );
  }

  refresh(): Observable<AuthTokens> {
    const refreshToken = localStorage.getItem(REFRESH_KEY);
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token'));
    }
    return this.http
      .post<AuthTokens>('/api/v1/auth/refresh', { refreshToken })
      .pipe(tap((tokens) => this.store(tokens)));
  }

  logout(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUser.set(null);
    void this.router.navigate(['/login']);
  }

  private store(tokens: AuthTokens): void {
    localStorage.setItem(ACCESS_KEY, tokens.accessToken);
    localStorage.setItem(REFRESH_KEY, tokens.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(tokens.user));
    this.currentUser.set(tokens.user);
  }

  private readUser(): UserInfo | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as UserInfo;
    } catch {
      return null;
    }
  }
}
