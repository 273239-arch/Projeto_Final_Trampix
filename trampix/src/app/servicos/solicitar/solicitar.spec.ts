import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { Servico } from '../../core/models/servico';
import { ServicosModule } from '../servicos-module';
import { Solicitar } from './solicitar';

const servico = (tipoCobranca: Servico['tipoCobranca']): Servico => ({
  id: 7,
  titulo: 'Instalação de chuveiro',
  preco: 80,
  tipoCobranca,
  status: 'ATIVO',
  categoria: { id: 1, nome: 'Eletricista' },
  subcategoria: { id: 2, nome: 'Tomadas' },
  prestador: { id: 1, usuarioId: 9, nome: 'José', cidade: 'Recife', uf: 'PE', verificado: false },
  criadoEm: '2026-11-01T10:00:00',
});

describe('Solicitar', () => {
  let fixture: ComponentFixture<Solicitar>;
  let http: HttpTestingController;
  const tela = () => fixture.nativeElement as HTMLElement;

  const preencher = (id: string, valor: string) => {
    const input = tela().querySelector<HTMLInputElement | HTMLTextAreaElement>(`#${id}`)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  };

  async function criar(tipo: Servico['tipoCobranca']) {
    await TestBed.configureTestingModule({
      imports: [ServicosModule],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(Solicitar);
    fixture.componentRef.setInput('servico', servico(tipo));
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  }

  it('por hora: pede hora e horas, calcula o preço e envia horaInicio + quantidadeHoras', async () => {
    await criar('POR_HORA');
    preencher('sol-data', '2999-01-10');
    preencher('sol-hora', '08:00');
    preencher('sol-horas', '3');
    await fixture.whenStable();
    expect(tela().querySelector('.estimativa')?.textContent).toMatch(/240[.,]00/);

    tela().querySelector('form')!.dispatchEvent(new Event('submit'));
    const req = http.expectOne(`${environment.apiUrl}/contratacoes`);
    expect(req.request.body).toEqual({
      servicoId: 7,
      dataInicio: '2999-01-10',
      observacao: null,
      horaInicio: '08:00',
      quantidadeHoras: 3,
    });
  });

  it('por dia: multiplica pelos dias e mostra o erro 422 do back no formulário', async () => {
    await criar('POR_DIA');
    expect(tela().querySelector('#sol-hora')).toBeNull();
    preencher('sol-data', '2999-01-10');
    preencher('sol-fim', '2999-01-12');
    await fixture.whenStable();
    expect(tela().querySelector('.estimativa')?.textContent).toMatch(/240[.,]00/);

    tela().querySelector('form')!.dispatchEvent(new Event('submit'));
    http
      .expectOne(`${environment.apiUrl}/contratacoes`)
      .flush(
        { status: 422, mensagem: 'o prestador não atende em DOMINGO (2999-01-10)' },
        { status: 422, statusText: 'Unprocessable' },
      );
    await fixture.whenStable();
    expect(tela().querySelector('.erro-geral')?.textContent).toContain('O prestador não atende em DOMINGO');
  });
});
