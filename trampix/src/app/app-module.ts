import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID, NgModule, provideBrowserGlobalErrorListeners } from '@angular/core';
import { MAT_ICON_DEFAULT_OPTIONS } from '@angular/material/icon';
import { BrowserModule } from '@angular/platform-browser';
import { AppRoutingModule } from './app-routing-module';
import { App } from './app';
import { erroInterceptor } from './core/interceptors/erro-interceptor';
import { tokenInterceptor } from './core/interceptors/token-interceptor';

// Datas e moeda no formato brasileiro (R$ 1.234,56).
registerLocaleData(localePt);

@NgModule({
  declarations: [App],
  imports: [BrowserModule, AppRoutingModule],
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Ordem: primeiro põe o token, depois trata o erro da resposta.
    provideHttpClient(withInterceptors([tokenInterceptor, erroInterceptor])),
    { provide: LOCALE_ID, useValue: 'pt-BR' },
    // Ícones de traço fino, como no Figma. É só configuração: o MatIconModule
    // continua importado no módulo de cada página.
    { provide: MAT_ICON_DEFAULT_OPTIONS, useValue: { fontSet: 'material-symbols-outlined' } },
  ],
  bootstrap: [App],
})
export class AppModule {}
