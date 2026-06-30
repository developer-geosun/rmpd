import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (req.url.includes('/auth/login')) {
        return throwError(() => err);
      }
      const message =
        typeof err.error?.message === 'string'
          ? err.error.message
          : err.status === 429
            ? 'Забагато запитів, спробуйте пізніше'
            : err.status === 0
              ? 'Немає з\'єднання з сервером'
              : `Помилка ${err.status}`;
      snackBar.open(message, 'Закрити', { duration: 5000 });
      return throwError(() => err);
    }),
  );
};
