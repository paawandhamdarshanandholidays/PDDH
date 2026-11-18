import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TableExportService } from '../../services/table-export.service';
import { TableExportActions } from './table-export-actions';

describe('TableExportActions', () => {
  let fixture: ComponentFixture<TableExportActions>;
  let exportExcel: ReturnType<typeof vi.fn>;
  let exportPdf: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    exportExcel = vi.fn().mockResolvedValue(undefined);
    exportPdf = vi.fn().mockResolvedValue(undefined);
    await TestBed.configureTestingModule({
      imports: [TableExportActions],
      providers: [{
        provide: TableExportService,
        useValue: { exportExcel, exportPdf },
      }],
    }).compileComponents();
    fixture = TestBed.createComponent(TableExportActions);
    fixture.componentRef.setInput('fileName', 'bookings');
    fixture.componentRef.setInput('title', 'Bookings');
    fixture.componentRef.setInput('headers', ['Reference', 'Status']);
    fixture.componentRef.setInput('rows', [['booking-1', 'CONFIRMED']]);
    fixture.detectChanges();
  });

  it('exports the provided table data to Excel or PDF', async () => {
    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    buttons[0]?.click();
    await fixture.whenStable();
    buttons[1]?.click();
    await fixture.whenStable();

    expect(exportExcel).toHaveBeenCalledWith('bookings', ['Reference', 'Status'], [['booking-1', 'CONFIRMED']]);
    expect(exportPdf).toHaveBeenCalledWith('bookings', 'Bookings', ['Reference', 'Status'], [['booking-1', 'CONFIRMED']]);
  });

  it('shows export failures rather than silently ignoring them', async () => {
    exportExcel.mockRejectedValueOnce(new Error('File creation failed'));
    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    button.click();
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Export failed: File creation failed');
  });
});
