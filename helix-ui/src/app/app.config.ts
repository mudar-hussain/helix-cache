import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async'
import { provideRouter, withHashLocation } from '@angular/router';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { routes } from './app.route';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withHashLocation()),
    provideHttpClient(withInterceptors([errorInterceptor])),
    provideAnimationsAsync(),
  ]
};
