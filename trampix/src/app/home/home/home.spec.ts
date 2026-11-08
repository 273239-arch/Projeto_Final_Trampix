import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { HomeModule } from '../home-module';
import { Home } from './home';

describe('Home', () => {
  let fixture: ComponentFixture<Home>;
  let http: HttpTestingController;
  const tela = () => fixture.nativeElement as HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeModule],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Home);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('mostra os cartões de serviço', async () => {
    http.expectOne(`${environment.apiUrl}/categorias`).flush([{ id: 1, nome: 'Reformas', ativa: true }]);
    http.expectOne(`${environment.apiUrl}/servicos`).flush([
      {
        id: 7,
        titulo: 'Reforma de Banheiro',
        preco: 6000,
        tipoCobranca: 'POR_TRABALHO',
        status: 'ATIVO',
        categoria: { id: 1, nome: 'Reformas', categoriaPaiId: null },
        prestador: { id: 1, usuarioId: 2, nome: 'José Silva', cidade: 'Recife', uf: 'PE', verificado: true },
        criadoEm: '2026-11-01T10:00:00',
      },
    ]);
    await fixture.whenStable();
    expect(tela().textContent).toContain('Reforma de Banheiro');
    expect(tela().textContent).toContain('José Silva');
    expect(tela().textContent).toContain('R$');
  });

  it('mostra a mensagem de lista vazia', async () => {
    http.expectOne(`${environment.apiUrl}/categorias`).flush([]);
    http.expectOne(`${environment.apiUrl}/servicos`).flush([]);
    await fixture.whenStable();
    expect(tela().textContent).toContain('Nenhum serviço disponível');
  });

  it('mostra erro e permite tentar de novo', async () => {
    http.expectOne(`${environment.apiUrl}/categorias`).flush([]);
    http
      .expectOne(`${environment.apiUrl}/servicos`)
      .flush({ status: 500, mensagem: 'erro interno no servidor' }, { status: 500, statusText: 'Erro' });
    await fixture.whenStable();
    expect(tela().textContent).toContain('Não foi possível carregar os serviços');
  });
});
