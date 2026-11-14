import { Component, inject } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { UserRole } from '../../core/models/role.model';

interface DashboardAction {
  title: string;
  description: string;
  link: string;
  label: string;
}

const roleCopy: Record<UserRole, { title: string; subtitle: string; actions: DashboardAction[] }> = {
  CEO: { title: 'CEO dashboard', subtitle: 'Executive access to every Pawan Dham management area.', actions: [{ title: 'Open executive controls', description: 'Manage the business, team, listings and bookings.', link: '/ceo/dashboard', label: 'Open CEO dashboard' }] },
  ADMIN: { title: 'Administrator dashboard', subtitle: 'Oversee operations and manage manager, staff and customer profiles.', actions: [
    { title: 'Trips & journeys', description: 'Create, edit and feature the public trip catalogue.', link: '/admin/trips', label: 'Manage trips' },
    { title: 'Website content', description: 'Manage temples, places, hotels, events, offers and gallery images.', link: '/admin/content', label: 'Manage content' },
    { title: 'Team profiles', description: 'Create and manage manager, staff and customer accounts.', link: '/portal/users', label: 'Manage profiles' },
    { title: 'Customer bookings', description: 'Review incoming trip and stay requests.', link: '/portal/bookings', label: 'Review bookings' },
    { title: 'Bookings, enquiries & contact', description: 'Search customer requests and manage their status.', link: '/portal/management', label: 'Manage enquiries' },
    { title: 'User directory', description: 'Review customer contact details and add manual records.', link: '/portal/user-directory', label: 'Open directory' },
  ] },
  MANAGER: { title: 'Manager dashboard', subtitle: 'Manage day-to-day journeys, customer service and staff profiles.', actions: [
    { title: 'Trips & journeys', description: 'Keep trip information and featured journeys current.', link: '/admin/trips', label: 'Manage trips' },
    { title: 'Website content', description: 'Update destinations, stays, events, offers and images.', link: '/admin/content', label: 'Manage content' },
    { title: 'Staff & customer profiles', description: 'Create and manage accounts below the manager role.', link: '/portal/users', label: 'Manage profiles' },
    { title: 'Customer bookings', description: 'Confirm or reject trip and stay requests.', link: '/portal/bookings', label: 'Review bookings' },
    { title: 'Bookings, enquiries & contact', description: 'Search customer requests and manage their status.', link: '/portal/management', label: 'Manage enquiries' },
    { title: 'User directory', description: 'Review customer contact details and add manual records.', link: '/portal/user-directory', label: 'Open directory' },
  ] },
  STAFF: { title: 'Staff dashboard', subtitle: 'Support travellers and keep customer profiles up to date.', actions: [
    { title: 'Customer profiles', description: 'Create and manage customer accounts only.', link: '/portal/users', label: 'Manage customers' },
    { title: 'Customer bookings', description: 'Review and update trip and stay reservation requests.', link: '/portal/bookings', label: 'Review bookings' },
    { title: 'Bookings, enquiries & contact', description: 'Search customer requests and manage their status.', link: '/portal/management', label: 'Manage enquiries' },
    { title: 'User directory', description: 'Review customer contact details and add manual records.', link: '/portal/user-directory', label: 'Open directory' },
    { title: 'Browse journeys', description: 'Explore trips and offers available to customers.', link: '/places', label: 'View journeys' },
  ] },
  CUSTOMER: { title: 'Traveller dashboard', subtitle: 'Your account, your plans and your next meaningful journey.', actions: [
    { title: 'Book a journey', description: 'Choose a trip and send a secure booking request.', link: '/booking', label: 'Explore trips' },
    { title: 'Book a stay', description: 'Choose a hotel or dharamshala and request your dates.', link: '/stay-booking', label: 'Find a stay' },
    { title: 'My profile', description: 'Update your contact information and account details.', link: '/portal/profile', label: 'Manage profile' },
    { title: 'Current offers', description: 'See active savings on selected journeys.', link: '/offers', label: 'Explore offers' },
  ] },
};

@Component({
  selector: 'app-role-dashboard',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="admin-page">
      <div class="admin-heading"><div><p class="eyebrow">{{ roleLabel(role) }} workspace</p><h1>{{ content.title }}</h1><p>{{ content.subtitle }}</p></div><span class="admin-count">{{ roleLabel(role) }}</span></div>
      <div class="ceo-actions">
        @for (action of content.actions; track action.link) {
          <a class="ceo-action" [routerLink]="action.link"><span class="ceo-action__icon">↗</span><div><h2>{{ action.title }}</h2><p>{{ action.description }}</p></div><span class="text-link">{{ action.label }} →</span></a>
        }
      </div>
      <p class="admin-security-note">Your role controls the options shown here. The API must enforce the same hierarchy for every protected request.</p>
    </section>
  `,
})
export class RoleDashboard {
  private readonly route = inject(ActivatedRoute);
  readonly role = this.route.snapshot.data['role'] as UserRole;
  readonly content = roleCopy[this.role];

  roleLabel(role: UserRole): string {
    return role === 'ADMIN' ? 'Administrator' : role.charAt(0) + role.slice(1).toLowerCase();
  }
}
