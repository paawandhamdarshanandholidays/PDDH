import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { UserDirectoryDraft, UserDirectoryService } from '../../core/services/user-directory.service';
import { UserDirectory } from './user-directory';

describe('UserDirectory', () => {
  let fixture: ComponentFixture<UserDirectory>;
  let create: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    create = vi.fn((entry: UserDirectoryDraft) => of({ id: 2, ...entry }));
    await TestBed.configureTestingModule({
      imports: [UserDirectory],
      providers: [{
        provide: UserDirectoryService,
        useValue: {
          list: () => of([{
            id: 1,
            name: 'Asha',
            contactNumber: '123',
            email: 'asha@example.test',
            address: 'Mathura',
            referredBy: '',
            serviceProvided: '',
          }]),
          create,
        },
      }],
    }).compileComponents();
    fixture = TestBed.createComponent(UserDirectory);
    fixture.detectChanges();
  });

  it('renders customer records in a searchable table', () => {
    expect(fixture.nativeElement.querySelector('table')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('asha@example.test');
  });

  it('saves manually entered details and appends the response', () => {
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
    expect(fixture.componentInstance.entries()).toHaveLength(2);
  });
});
