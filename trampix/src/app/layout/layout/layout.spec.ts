import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { Auth } from '../../core/services/auth';
import { LayoutModule } from '../layout-module';
import { Layout } from './layout';

describe('Layout', () => {
  let fixture: ComponentFixture<Layout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LayoutModule],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Layout);
    await fixture.whenStable();
  });

  it('tem cabeçalho com busca, conteúdo e rodapé', () => {
    const tela = fixture.nativeElement as HTMLElement;
    expect(tela.querySelector('input[type="search"]')?.getAttribute('placeholder')).toBe(
      'Buscar serviços...',
    );
    expect(tela.querySelector('router-outlet')).not.toBeNull();
    expect(tela.querySelector('footer')).not.toBeNull();
  });

  it('sem login, não mostra os links da conta', () => {
    const tela = fixture.nativeElement as HTMLElement;
    expect(tela.textContent).not.toContain('Meus pedidos');
  });
});

describe('Layout logado', () => {
  it('mostra o nome, "Meus pedidos", "Sair" e, para prestador, "Pedidos recebidos"', async () => {
    await TestBed.configureTestingModule({
      imports: [LayoutModule],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Auth, useValue: { usuario: () => ({ id: 5, nome: 'Ana Souza' }), logout: () => undefined } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(Layout);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne(`${environment.apiUrl}/usuarios/5/prestador`).flush({ id: 1 });
    await fixture.whenStable();

    const conta = (fixture.nativeElement as HTMLElement).querySelector('nav.conta')!;
    expect(conta.textContent).toContain('Ana Souza');
    expect(conta.textContent).toContain('Meus pedidos');
    expect(conta.textContent).toContain('Pedidos recebidos');
    expect(conta.textContent).toContain('Sair');
  });
});
