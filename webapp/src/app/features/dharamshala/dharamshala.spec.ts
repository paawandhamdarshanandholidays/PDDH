import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { Dharamshala } from './dharamshala';

describe('Dharamshala', () => {
  let component: Dharamshala;
  let fixture: ComponentFixture<Dharamshala>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dharamshala],
      providers: [provideHttpClient()],
    }).compileComponents();

    fixture = TestBed.createComponent(Dharamshala);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
