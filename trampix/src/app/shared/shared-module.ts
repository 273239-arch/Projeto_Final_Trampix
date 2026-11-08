import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Logo } from './logo/logo';

@NgModule({
  declarations: [Logo],
  imports: [CommonModule],
  exports: [Logo],
})
export class SharedModule {}
