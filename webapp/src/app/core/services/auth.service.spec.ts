import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('AuthService browser session', () => {
  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient()] });
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('restores a session from sessionStorage and removes legacy persistent auth', () => {
    sessionStorage.setItem('pddh.accessToken', 'session-token');
    sessionStorage.setItem('pddh.accountRole', 'CUSTOMER');
    localStorage.setItem('pddh.accessToken', 'persistent-token');
    localStorage.setItem('pddh.accountRole', 'ADMIN');

    const auth = TestBed.inject(AuthService);

    expect(auth.token).toBe('session-token');
    expect(auth.role()).toBe('CUSTOMER');
    expect(localStorage.getItem('pddh.accessToken')).toBeNull();
    expect(localStorage.getItem('pddh.accountRole')).toBeNull();
  });

  it('clears the browser session on logout', () => {
    sessionStorage.setItem('pddh.accessToken', 'session-token');
    sessionStorage.setItem('pddh.accountRole', 'CUSTOMER');

    const auth = TestBed.inject(AuthService);
    auth.logout();

    expect(auth.token).toBeNull();
    expect(auth.role()).toBeNull();
    expect(sessionStorage.getItem('pddh.accessToken')).toBeNull();
    expect(sessionStorage.getItem('pddh.accountRole')).toBeNull();
  });

  it('clears the browser session when the page is closed or navigated away', () => {
    sessionStorage.setItem('pddh.accessToken', 'session-token');
    sessionStorage.setItem('pddh.accountRole', 'CUSTOMER');
    const auth = TestBed.inject(AuthService);

    window.dispatchEvent(new PageTransitionEvent('pagehide'));

    expect(auth.token).toBeNull();
    expect(sessionStorage.getItem('pddh.accessToken')).toBeNull();
  });
});
