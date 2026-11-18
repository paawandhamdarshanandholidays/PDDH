import { Component, inject, input, signal } from '@angular/core';
import { TableExportCell, TableExportService } from '../../services/table-export.service';

@Component({
  selector: 'app-table-export-actions',
  standalone: true,
  styles: [`
    .table-export-actions { display: flex; align-items: center; flex-wrap: wrap; gap: .55rem; }
    .table-export-error { flex-basis: 100%; color: #b42318; font-size: .78rem; }
  `],
  template: `
    <div class="table-export-actions">
      <button class="button button--small" type="button" (click)="exportExcel()"
        title="Export rows currently displayed in this table as an Excel workbook"
        [disabled]="disabled() || busy()">Export Excel</button>
      <button class="button button--small" type="button" (click)="exportPdf()"
        title="Export rows currently displayed in this table as a PDF"
        [disabled]="disabled() || busy()">Export PDF</button>
      @if (error()) { <span class="table-export-error" role="alert">{{ error() }}</span> }
    </div>
  `,
})
export class TableExportActions {
  private readonly tableExport = inject(TableExportService);
  readonly fileName = input.required<string>();
  readonly title = input.required<string>();
  readonly headers = input.required<readonly string[]>();
  readonly rows = input.required<readonly (readonly TableExportCell[])[]>();
  readonly disabled = input(false);
  readonly busy = signal(false);
  readonly error = signal('');

  exportExcel(): void {
    this.run(() => this.tableExport.exportExcel(this.fileName(), this.headers(), this.rows()));
  }

  exportPdf(): void {
    this.run(() => this.tableExport.exportPdf(
      this.fileName(), this.title(), this.headers(), this.rows(),
    ));
  }

  private async run(exportFile: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    this.error.set('');
    try {
      await exportFile();
    } catch (error) {
      if (!(error instanceof Error)) throw error;
      this.error.set(`Export failed: ${error.message}`);
    } finally {
      this.busy.set(false);
    }
  }
}
