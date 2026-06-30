import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';

export type SubmitDialogState = 'submitting' | 'done' | 'error';

@Component({
  selector: 'app-submit-progress-dialog',
  imports: [MatDialogModule, MatProgressSpinnerModule, TranslateModule],
  template: `
    <h2 mat-dialog-title>{{ 'declarations.submit' | translate }}</h2>
    <mat-dialog-content class="dialog-body">
      @if (data.state === 'submitting') {
        <mat-spinner diameter="48" />
        <p>{{ 'declarations.submitting' | translate }}</p>
      } @else if (data.state === 'done') {
        <p>{{ 'declarations.submitted' | translate }}</p>
        @if (data.sysRef) {
          <p>sysRef: {{ data.sysRef }}</p>
        }
      } @else {
        <p>{{ data.message || ('declarations.submitError' | translate) }}</p>
      }
    </mat-dialog-content>
    @if (data.state !== 'submitting') {
      <mat-dialog-actions align="end">
        <button mat-button mat-dialog-close type="button">{{ 'common.ok' | translate }}</button>
      </mat-dialog-actions>
    }
  `,
  styles: `
    .dialog-body {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
      min-width: 16rem;
      padding-top: 0.5rem;
    }
  `,
})
export class SubmitProgressDialogComponent {
  readonly data = inject<{ state: SubmitDialogState; sysRef?: string; message?: string }>(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<SubmitProgressDialogComponent>);
}
