import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Donations } from './donations';

describe('Donations', () => {
  let component: Donations;
  let fixture: ComponentFixture<Donations>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Donations],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Donations);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
