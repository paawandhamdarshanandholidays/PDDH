import { Routes } from '@angular/router';
import { Home } from './features/home/home';
import { DarshanBooking } from './features/darshan-booking/darshan-booking';
import { Dharamshala } from './features/dharamshala/dharamshala';
import { Donations } from './features/donations/donations';
import { Gallery } from './features/gallery/gallery';
import { Contact } from './features/contact/contact';
import { Temples } from './features/temples/temples';
import { Places } from './features/places/places';
import { Events } from './features/events/events';
import { About } from './features/about/about';
import { AdminTrips } from './features/admin-trips/admin-trips';
import { AdminLogin } from './features/admin-login/admin-login';
import { AdminContent } from './features/admin-content/admin-content';
import { Offers } from './features/offers/offers';
import { CeoDashboard } from './features/ceo-dashboard/ceo-dashboard';
import { CeoBookings } from './features/ceo-bookings/ceo-bookings';
import { RoleDashboard } from './features/role-dashboard/role-dashboard';
import { CustomerRegister } from './features/customer-register/customer-register';
import { CustomerProfile } from './features/customer-profile/customer-profile';
import { UserManagement } from './features/user-management/user-management';
import { PasswordRecovery } from './features/password-recovery/password-recovery';
import { adminGuard } from './core/guards/admin.guard';
import { ceoGuard } from './core/guards/ceo.guard';
import { authenticatedGuard, bookingsGuard, customerBookingGuard, roleDashboardGuard, userManagementGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  { path: 'home', component: Home },
  { path: 'temples', component: Temples },
  { path: 'places', component: Places },
  { path: 'events', component: Events },
  { path: 'offers', component: Offers },
  { path: 'gallery', component: Gallery },
  { path: 'booking', component: DarshanBooking, canActivate: [customerBookingGuard] },
  { path: 'about', component: About },
  { path: 'contact', component: Contact },
  { path: 'admin/login', component: AdminLogin },
  { path: 'login', component: AdminLogin, data: { role: 'CUSTOMER' } },
  { path: 'register', component: CustomerRegister },
  { path: 'forgot-password', component: PasswordRecovery },
  { path: 'ceo/login', component: AdminLogin, data: { role: 'CEO' } },
  {
    path: 'ceo/register',
    loadComponent: () => import('./features/ceo-register/ceo-register').then((module) => module.CeoRegister),
  },
  { path: 'ceo/dashboard', component: CeoDashboard, canActivate: [ceoGuard] },
  { path: 'admin/dashboard', component: RoleDashboard, canActivate: [roleDashboardGuard], data: { role: 'ADMIN' } },
  { path: 'manager/dashboard', component: RoleDashboard, canActivate: [roleDashboardGuard], data: { role: 'MANAGER' } },
  { path: 'staff/dashboard', component: RoleDashboard, canActivate: [roleDashboardGuard], data: { role: 'STAFF' } },
  { path: 'customer/dashboard', component: RoleDashboard, canActivate: [roleDashboardGuard], data: { role: 'CUSTOMER' } },
  { path: 'portal/bookings', component: CeoBookings, canActivate: [bookingsGuard] },
  { path: 'portal/users', component: UserManagement, canActivate: [userManagementGuard] },
  { path: 'portal/profile', component: CustomerProfile, canActivate: [authenticatedGuard] },
  { path: 'ceo/bookings', redirectTo: 'portal/bookings' },
  { path: 'admin/trips', component: AdminTrips, canActivate: [adminGuard] },
  { path: 'admin/content', component: AdminContent, canActivate: [adminGuard] },
  { path: 'darshan-booking', redirectTo: 'booking' },
  { path: 'stay-booking', component: Dharamshala, canActivate: [customerBookingGuard] },
  { path: 'donations', component: Donations },
  { path: '**', redirectTo: 'home' },
];
