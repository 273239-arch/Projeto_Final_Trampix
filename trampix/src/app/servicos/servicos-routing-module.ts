import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DetalheServico } from './detalhe/detalhe';

const routes: Routes = [
  { path: ':id', component: DetalheServico },
  { path: '', pathMatch: 'full', redirectTo: '/' },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ServicosRoutingModule {}
