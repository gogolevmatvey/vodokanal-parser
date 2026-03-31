import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  { path: '', redirectTo: '/search', pathMatch: 'full' },
  { path: 'search', loadChildren: () => import('./components/search/search.module').then(m => m.SearchModule) },
  { path: 'import', loadChildren: () => import('./components/import/import.module').then(m => m.ImportModule) },
  { path: 'statistics', loadChildren: () => import('./components/statistics/statistics.module').then(m => m.StatisticsModule) }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
