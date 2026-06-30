import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Дозволені лише символи латиниці (ASCII printable). */
export function latinInputValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (value == null || value === '') {
      return null;
    }
    return /^[\x20-\x7E]*$/.test(String(value)) ? null : { latin: true };
  };
}
