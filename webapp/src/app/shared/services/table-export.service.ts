import { Injectable } from '@angular/core';

export type TableExportCell = string | number | boolean | null | undefined;

@Injectable({ providedIn: 'root' })
export class TableExportService {
  async exportExcel(
    fileName: string,
    headers: readonly string[],
    rows: readonly (readonly TableExportCell[])[],
  ): Promise<void> {
    const { strToU8, zipSync } = await import('fflate');
    const allRows = [
      [...headers],
      ...rows.map((row) => headers.map((_, index) => String(row[index] ?? ''))),
    ];
    const columnName = (column: number): string => {
      let name = '';
      while (column > 0) {
        const remainder = (column - 1) % 26;
        name = String.fromCharCode(65 + remainder) + name;
        column = Math.floor((column - 1) / 26);
      }
      return name;
    };
    const cellXml = (value: string, address: string, header: boolean): string =>
      `<c r="${address}" t="inlineStr"${header ? ' s="1"' : ''}><is><t xml:space="preserve">${this.escapeXml(value)}</t></is></c>`;
    const rowXml = allRows.map((row, rowIndex) =>
      `<row r="${rowIndex + 1}">${row.map((value, columnIndex) =>
        cellXml(value, `${columnName(columnIndex + 1)}${rowIndex + 1}`, rowIndex === 0)).join('')}</row>`,
    ).join('');
    const columnsXml = headers.map((header, index) => {
      const sampleWidth = rows.slice(0, 100).reduce((max, row) =>
        Math.max(max, String(row[index] ?? '').split(/\r?\n/)[0].length), header.length);
      return `<col min="${index + 1}" max="${index + 1}" width="${Math.min(Math.max(sampleWidth + 2, 12), 40)}" customWidth="1"/>`;
    }).join('');
    const lastCell = `${columnName(headers.length)}${allRows.length}`;
    const files = {
      '[Content_Types].xml': strToU8(
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
        + '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
        + '<Default Extension="xml" ContentType="application/xml"/>'
        + '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
        + '<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
        + '<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>'
        + '</Types>',
      ),
      '_rels/.rels': strToU8(
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        + '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>'
        + '</Relationships>',
      ),
      'xl/workbook.xml': strToU8(
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
        + '<sheets><sheet name="Export" sheetId="1" r:id="rId1"/></sheets></workbook>',
      ),
      'xl/_rels/workbook.xml.rels': strToU8(
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        + '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>'
        + '<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>'
        + '</Relationships>',
      ),
      'xl/styles.xml': strToU8(
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + '<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">'
        + '<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><color rgb="FFFFFFFF"/><sz val="11"/><name val="Calibri"/></font></fonts>'
        + '<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF334155"/><bgColor indexed="64"/></patternFill></fill></fills>'
        + '<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>'
        + '<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>'
        + '<cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/></cellXfs>'
        + '<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>',
      ),
      'xl/worksheets/sheet1.xml': strToU8(
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
        + `<dimension ref="A1:${lastCell}"/><sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>`
        + `<cols>${columnsXml}</cols><sheetData>${rowXml}</sheetData><autoFilter ref="A1:${lastCell}"/></worksheet>`,
      ),
    };
    const zipped = zipSync(files, { level: 6 });
    const bytes = new Uint8Array(zipped.byteLength);
    bytes.set(zipped);
    this.download(new Blob([bytes.buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    }), `${fileName}.xlsx`);
  }

  async exportPdf(
    fileName: string,
    title: string,
    headers: readonly string[],
    rows: readonly (readonly TableExportCell[])[],
  ): Promise<void> {
    const [{ jsPDF }, { default: autoTable }] = await Promise.all([
      import('jspdf'),
      import('jspdf-autotable'),
    ]);
    const document = new jsPDF({
      orientation: headers.length > 5 ? 'landscape' : 'portrait',
      unit: 'pt',
      format: 'a4',
    });
    document.setFontSize(15);
    document.text(title, 40, 35);
    autoTable(document, {
      head: [[...headers]],
      body: rows.map((row) => headers.map((_, index) => String(row[index] ?? ''))),
      startY: 48,
      styles: { fontSize: 8, cellPadding: 4, overflow: 'linebreak' },
      headStyles: { fillColor: [51, 65, 85] },
      margin: { left: 32, right: 32 },
      horizontalPageBreak: true,
    });
    document.save(`${fileName}.pdf`);
  }

  private download(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.append(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  private escapeXml(value: string): string {
    return value.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }
}
