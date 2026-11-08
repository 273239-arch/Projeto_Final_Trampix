import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
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
});
