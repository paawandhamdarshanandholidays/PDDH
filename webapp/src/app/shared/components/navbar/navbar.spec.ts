import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { Router } from '@angular/router';
import { vi } from 'vitest';

import { Navbar } from './navbar';

describe('Navbar', () => {
  let component: Navbar;
  let fixture: ComponentFixture<Navbar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Navbar],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Navbar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('opens the CEO login after three logo clicks', () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const click = () => component.onBrandClick(new MouseEvent('click'));

    click();
    click();
    click();

    expect(navigate).toHaveBeenLastCalledWith(['/ceo/login']);
  });

  it('shows one login link for customer and staff sign-in', () => {
    const links = Array.from(fixture.nativeElement.querySelectorAll('a')) as HTMLAnchorElement[];

    expect(links.some((link) => link.textContent?.trim() === 'Login'
      && link.getAttribute('routerLink') === '/admin/login')).toBe(true);
    expect(links.some((link) => link.textContent?.includes('Customer login'))).toBe(false);
  });
});
