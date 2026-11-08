import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Auth } from '../../core/services/auth';
import { ServicosModule } from '../servicos-module';
import { DetalheServico } from './detalhe';

describe('DetalheServico', () => {
  let fixture: ComponentFixture<DetalheServico>;
  let http: HttpTestingController;
  const tela = () => fixture.nativeElement as HTMLElement;

  beforeEach(async () => {
    const paramMap = convertToParamMap({ id: '7' });
    await TestBed.configureTestingModule({
      imports: [ServicosModule],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(paramMap), snapshot: { paramMap, queryParamMap: convertToParamMap({}) } },
        },
        { provide: Auth, useValue: { estaLogado: () => false, usuario: () => null } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(DetalheServico);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('mostra categoria, subcategoria, preço e prestador', async () => {
    http.expectOne(`${environment.apiUrl}/servicos/7`).flush({
      id: 7,
      titulo: 'Reforma de Banheiro Completa',
      descricao: 'Troca de piso e azulejo.',
      preco: 6000,
      tipoCobranca: 'POR_TRABALHO',
      status: 'ATIVO',
      categoria: { id: 1, nome: 'Pedreiro' },
      subcategoria: { id: 3, nome: 'Revestimento' },
      prestador: { id: 1, usuarioId: 9, nome: 'Arnaldo Fonseca', cidade: 'Recife', uf: 'PE', verificado: true },
      criadoEm: '2026-11-01T10:00:00',
    });
    http.expectOne(`${environment.apiUrl}/usuarios/9/disponibilidades`).flush([
      { id: 1, diaSemana: 'SEGUNDA', horaInicio: '08:00:00', horaFim: '18:00:00' },
    ]);
    await fixture.whenStable();

    const texto = tela().textContent!;
    expect(texto).toContain('Reforma de Banheiro Completa');
    expect(texto).toContain('Pedreiro');
    expect(texto).toContain('Revestimento');
    expect(texto).toContain('Por trabalho completo');
    expect(texto).toContain('Arnaldo Fonseca');
    expect(texto).toContain('Recife - PE');
    expect(texto).toContain('Seg 08:00–18:00');
  });

  it('manda quem não está logado para o login, voltando com o formulário aberto', async () => {
    http.expectOne(`${environment.apiUrl}/servicos/7`).flush({
      id: 7,
      titulo: 'Pintura',
      preco: 50,
      tipoCobranca: 'POR_HORA',
      status: 'ATIVO',
      categoria: { id: 2, nome: 'Pintor' },
      subcategoria: { id: 8, nome: 'Paredes' },
      prestador: { id: 1, usuarioId: 9, nome: 'José', cidade: null, uf: null, verificado: false },
      criadoEm: '2026-11-01T10:00:00',
    });
    http.expectOne(`${environment.apiUrl}/usuarios/9/disponibilidades`).flush([]);
    await fixture.whenStable();

    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    tela().querySelector<HTMLButtonElement>('button.solicitar')!.click();
    expect(navegar).toHaveBeenCalledWith(['/login'], { queryParams: { voltar: '/servicos/7?solicitar=1' } });
  });

  it('mostra "não encontrado" no 404', async () => {
    http
      .expectOne(`${environment.apiUrl}/servicos/7`)
      .flush({ status: 404, mensagem: 'serviço não encontrado' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    expect(tela().textContent).toContain('Serviço não encontrado');
  });
});
