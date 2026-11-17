import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { UserDirectoryDraft, UserDirectoryService } from '../../core/services/user-directory.service';
import { UserDirectory } from './user-directory';

describe('UserDirectory', () => {
  let fixture: ComponentFixture<UserDirectory>;
  let create: ReturnType<typeof vi.fn>;
  let listPage: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    create = vi.fn((entry: UserDirectoryDraft) => of({ id: 2, ...entry }));
    listPage = vi.fn((page: number, size: number) => of({
      content: page === 0 ? [{
        id: 1,
        name: 'Asha',
        contactNumber: '123',
        email: 'asha@example.test',
        address: 'Mathura',
        referredBy: '',
        serviceProvided: '',
      }] : [],
      page,
      size,
      totalElements: 51,
      totalPages: 2,
    }));
    await TestBed.configureTestingModule({
      imports: [UserDirectory],
      providers: [{
        provide: UserDirectoryService,
        useValue: {
          listPage,
          create,
        },
      }],
    }).compileComponents();
    fixture = TestBed.createComponent(UserDirectory);
    fixture.detectChanges();
  });

  it('offers view and save choices without loading the directory before View is selected', () => {
    expect(listPage).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('View user directory');
    expect(fixture.nativeElement.textContent).toContain('Add a customer record');
  });

  it('loads sorted directory records in pages of no more than 50', () => {
    fixture.componentInstance.openDirectory();
    fixture.detectChanges();

    expect(listPage).toHaveBeenCalledWith(0, 50, 'name', 'ASC');
    expect(fixture.nativeElement.querySelector('table')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('asha@example.test');

    fixture.componentInstance.changePage(1);
    expect(listPage).toHaveBeenLastCalledWith(1, 50, 'name', 'ASC');
  });

  it('saves manually entered details and returns to the paged directory', () => {
    fixture.componentInstance.action.set('add');
    fixture.componentInstance.draft = {
      name: 'Ravi',
      contactNumber: '987',
      email: 'ravi@example.test',
      address: 'Vrindavan',
      referredBy: 'Asha',
      serviceProvided: 'Stay',
    };

    fixture.componentInstance.create();
    fixture.detectChanges();

    expect(create).toHaveBeenCalledWith(expect.objectContaining({ name: 'Ravi' }));
    expect(fixture.componentInstance.action()).toBe('view');
    expect(listPage).toHaveBeenCalled();
  });
});
