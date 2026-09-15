import { Route } from "@angular/router";

export const routes: Route[] = [
    {
        path: '',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
        children: [
            {
                path: '',
                loadComponent: () => import('./features/ring-view/ring-view.component').then(m => m.RingViewComponent),
            },
            {
                path: 'log',
                loadComponent: () => import('./features/activity-log/activity-log.component').then(m => m.ActivityLogComponent),
            },
        ]
    },
    { path: '**', redirectTo: '' },
];