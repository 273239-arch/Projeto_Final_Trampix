export type StatusSubcategoria = 'ATIVA' | 'INATIVA';

// SubcategoriaResposta no back.
export interface Subcategoria {
  id: number;
  nome: string;
  descricao?: string | null;
  categoriaId: number;
  padrao: boolean;
  status: StatusSubcategoria;
}

// CategoriaResposta no back: GET /categorias já traz as subcategorias ativas.
export interface Categoria {
  id: number;
  nome: string;
  descricao?: string | null;
  categoriaPaiId: number | null;
  subcategorias: Subcategoria[];
}
