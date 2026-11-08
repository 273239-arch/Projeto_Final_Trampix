package com.trampix.recursos;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.trampix.entidades.Categoria;
import com.trampix.entidades.Subcategoria;
import com.trampix.entidades.enums.StatusSubcategoria;
import com.trampix.repositorios.CategoriaRepositorio;
import com.trampix.repositorios.SubcategoriaRepositorio;

// cria as categorias e subcategorias padrão ao subir a aplicação
@Component
public class CriadorCategorias implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CriadorCategorias.class);

    private record Sub(String nome, String descricao) {
    }

    private record Cat(String nome, List<Sub> subcategorias) {
    }

    private static final List<Cat> PADROES = List.of(
            new Cat("Pedreiro", List.of(
                    new Sub("Fundação", "Sapata, bloco, alicerce, viga e pilar."),
                    new Sub("Alvenaria", "Levantamento de paredes, muros e divisórias."),
                    new Sub("Revestimento", "Assentamento de cerâmica, porcelanato e azulejo."),
                    new Sub("Telhado", "Construção, reforma de coberturas e calhas."),
                    new Sub("Demolição", "Quebra de paredes, remoção de entulho e abertura de vãos."),
                    new Sub("Reparos", "Pequenos remendos, reboco, contrapiso e infiltrações."))),
            new Cat("Pintor", List.of(
                    new Sub("Paredes", "Pintura lisa interna, externa e tetos."),
                    new Sub("Massa", "Aplicação de massa corrida, acrílica e lixamento."),
                    new Sub("Textura", "Grafiato, cimento queimado, marmorato e texturas projetadas."),
                    new Sub("Madeira", "Verniz, seladora e laqueação em portas ou decks."),
                    new Sub("Ferro", "Pintura anticorrosiva em portões, grades e esquadrias."))),
            new Cat("Encanador", List.of(
                    new Sub("Instalação", "Tubulações de água fria, água quente e esgoto (obras)."),
                    new Sub("Vazamento", "Troca de torneira, registro, sifão e reparo de descarga."),
                    new Sub("Caça-vazamento", "Localização eletrônica de vazamento oculto (geofone)."),
                    new Sub("Desentupimento", "Desentupir pia, ralo, vaso, esgoto e caixa de gordura."),
                    new Sub("Gás", "Instalação de aquecedor e teste de estanqueidade de gás."))),
            new Cat("Eletricista", List.of(
                    new Sub("Fiação", "Troca de cabos, enfiação geral e balanceamento de carga."),
                    new Sub("Quadro", "Montagem de disjuntores, DR, DPS e caixa de luz."),
                    new Sub("Iluminação", "Instalação de fitas LED, spots, lustres e luminárias."),
                    new Sub("Tomadas", "Troca e instalação de tomadas, interruptores e chuveiros."),
                    new Sub("Manutenção", "Conserto de curtos-circuitos e tomadas queimadas."))),
            new Cat("Marceneiro e Carpinteiro", List.of(
                    new Sub("Móveis", "Reparo e ajuste de portas de armários e gavetas."),
                    new Sub("Instalação", "Montagem de painéis, prateleiras e nichos personalizados."),
                    new Sub("Estruturas", "Telhados de madeira, decks, pergolados e escadas."),
                    new Sub("Esquadrias", "Instalação de portas de madeira, batentes e fechaduras."))),
            new Cat("Marido de Aluguel", List.of(
                    new Sub("Suportes", "Instalação de suporte de TV, quadros, cortinas e varais."),
                    new Sub("Ajustes", "Troca de puxadores, dobradiças e trincos de portas."),
                    new Sub("Montagem", "Montagem de móveis comprados na internet (guarda-roupas, mesas)."),
                    new Sub("Instalações rápidas",
                            "Troca de lâmpadas, instalação de filtro de água e acessórios de banheiro."))),
            new Cat("Gesseiro e Drywall", List.of(
                    new Sub("Forro", "Instalação de teto de gesso acartonado ou placas."),
                    new Sub("Drywall", "Paredes divisórias e nichos em gesso acartonado."),
                    new Sub("Decoração", "Sancas, molduras, cortineiros e iluminação embutida."))),
            new Cat("Climatização", List.of(
                    new Sub("Instalação", "Fixação, furação e conexão do sistema split."),
                    new Sub("Limpeza", "Higienização química de filtros, evaporadora e condensadora."),
                    new Sub("Carga de gás", "Identificação de vazamento de fluido e recarga."))),
            new Cat("Vidraceiro e Serralheiro", List.of(
                    new Sub("Vidros", "Box de banheiro, janelas de vidro temperado e espelhos."),
                    new Sub("Estruturas metálicas", "Solda, fabricação e reparo de portões, grades e corrimão."))));

    private final CategoriaRepositorio categoriaRepositorio;
    private final SubcategoriaRepositorio subcategoriaRepositorio;

    public CriadorCategorias(CategoriaRepositorio categoriaRepositorio,
            SubcategoriaRepositorio subcategoriaRepositorio) {
        this.categoriaRepositorio = categoriaRepositorio;
        this.subcategoriaRepositorio = subcategoriaRepositorio;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int novasCategorias = 0;
        int novasSubcategorias = 0;
        for (Cat padrao : PADROES) {
            // busca a categoria pelo nome ou cria
            Categoria categoria = categoriaRepositorio.findFirstByNomeIgnoreCase(padrao.nome())
                    .orElse(null);
            if (categoria == null) {
                categoria = new Categoria();
                categoria.setNome(padrao.nome());
                categoria = categoriaRepositorio.save(categoria);
                novasCategorias++;
            }
            for (Sub sub : padrao.subcategorias()) {
                // só insere se ainda não existir
                if (subcategoriaRepositorio.findByCategoriaIdAndNomeNormalizado(categoria.getId(),
                        Subcategoria.normalizar(sub.nome())).isPresent()) {
                    continue;
                }
                Subcategoria nova = new Subcategoria();
                nova.setNome(sub.nome());
                nova.setDescricao(sub.descricao());
                nova.setCategoria(categoria);
                nova.setPadrao(true);
                nova.setStatus(StatusSubcategoria.ATIVA);
                subcategoriaRepositorio.save(nova);
                novasSubcategorias++;
            }
        }
        log.info("Categorias padrão: {} categorias e {} subcategorias inseridas.", novasCategorias, novasSubcategorias);
    }
}
