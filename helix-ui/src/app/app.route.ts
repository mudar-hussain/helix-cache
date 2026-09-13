import { Route } from "@angular/router";

export const routes: Route[] = [
    {
        path: '',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
        children: [
            {
                path: '',
                redirectTo: 'ring',
                pathMatch: 'full',
            },
            {
                path: 'ring',
                loadComponent: () => import('./features/ring-view/ring-view.component').then(m => m.RingViewComponent),
            },
            {
                path: 'distribution',
                loadComponent: () => import('./features/distribution/distribution.component').then(m => m.DistributionComponent),
            },
            {
                path: 'hot-keys',
                loadComponent: () => import('./features/hot-keys/hot-keys.component').then(m => m.HotKeysComponent),
            },
            {
                path: 'activity',
                loadComponent: () => import('./features/activity-log/activity-log.component').then(m => m.ActivityLogComponent),
            },
        ]
    },
    { path: '**', redirectTo: '' },
];