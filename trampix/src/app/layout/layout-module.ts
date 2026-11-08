import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';

import { SharedModule } from '../shared/shared-module';
import { LayoutRoutingModule } from './layout-routing-module';
import { Layout } from './layout/layout';

@NgModule({
  declarations: [Layout],
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
    SharedModule,
    LayoutRoutingModule,
  ],
})
export class LayoutModule {}
