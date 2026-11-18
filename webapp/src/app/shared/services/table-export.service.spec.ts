import { strFromU8, unzipSync } from 'fflate';
import { TableExportService } from './table-export.service';

describe('TableExportService', () => {
  it('creates a valid Excel workbook with escaped table data', async () => {
    const exportedBlobs: Blob[] = [];
    vi.spyOn(URL, 'createObjectURL').mockImplementation((blob) => {
      if (blob instanceof Blob) exportedBlobs.push(blob);
      return 'blob:export';
    });
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    const service = new TableExportService();

    await service.exportExcel('bookings', ['Reference', 'Customer'], [['booking-1', 'A & <B>']]);

    const exportedBlob = exportedBlobs[0];
    expect(exportedBlob).toBeDefined();
    if (!exportedBlob) throw new Error('Export did not create a downloadable workbook');
    expect(exportedBlob.type).toBe('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    const archive = unzipSync(new Uint8Array(await exportedBlob.arrayBuffer()));
    const worksheet = strFromU8(archive['xl/worksheets/sheet1.xml']!);
    expect(worksheet).toContain('booking-1');
    expect(worksheet).toContain('A &amp; &lt;B&gt;');
    expect(worksheet).toContain('<autoFilter ref="A1:B2"/>');
  });
});
