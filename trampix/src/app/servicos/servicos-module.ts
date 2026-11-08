import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { ServicosRoutingModule } from './servicos-routing-module';
import { DetalheServico } from './detalhe/detalhe';
import { Solicitar } from './solicitar/solicitar';

@NgModule({
  declarations: [DetalheServico, Solicitar],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    ServicosRoutingModule,
  ],
})
export class ServicosModule {}
