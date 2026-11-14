import { Component, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {
  readonly menuOpen = signal(false);
  private brandClickCount = 0;
  private lastBrandClickAt = 0;

  constructor(readonly auth: AuthService, private readonly router: Router) {}

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  onBrandClick(event: MouseEvent): void {
    event.preventDefault();
    const now = Date.now();
    this.brandClickCount = now - this.lastBrandClickAt <= 2000 ? this.brandClickCount + 1 : 1;
    this.lastBrandClickAt = now;
    this.closeMenu();

    if (this.brandClickCount === 3) {
      this.brandClickCount = 0;
      void this.router.navigate(['/ceo/login']);
      return;
    }

    void this.router.navigate(['/home']);
  }

  signOut(): void {
    this.auth.logout();
    this.closeMenu();
    void this.router.navigate(['/home']);
  }
}
