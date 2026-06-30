import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const isAuthEndpoint = req.url.includes('/api/v1/auth/login') || req.url.includes('/api/v1/auth/refresh');

  if (isAuthEndpoint || !auth.accessToken) {
    return next(req);
  }

  const authed = req.clone({
    setHeaders: { Authorization: `Bearer ${auth.accessToken}` },
  });

  return next(authed).pipe(
    catchError((error) => {
      if (error.status !== 401 || req.url.includes('/api/v1/auth/refresh')) {
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        switchMap((tokens) =>
          next(
            req.clone({
              setHeaders: { Authorization: `Bearer ${tokens.accessToken}` },
            }),
          ),
        ),
        catchError(() => {
          auth.logout();
          return throwError(() => error);
        }),
      );
    }),
  );
};
