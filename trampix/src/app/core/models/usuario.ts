export type TipoDocumento = 'CPF' | 'CNPJ';
export type Perfil = 'USUARIO' | 'ADMIN';
export type StatusUsuario = 'ATIVO' | 'INATIVO' | 'SUSPENSO' | 'BANIDO';

// Usuário como o back devolve (sem senha nem hash).
export interface Usuario {
  id: number;
  nome: string;
  email: string;
  documento?: string | null;
  tipoDocumento?: TipoDocumento | null;
  dataNascimento?: string | null; // yyyy-MM-dd
  telefone?: string | null;
  perfil: Perfil;
  status: StatusUsuario;
  cep?: string | null;
  logradouro?: string | null;
  numero?: string | null;
  complemento?: string | null;
  bairro?: string | null;
  cidade?: string | null;
  uf?: string | null;
  criadoEm?: string;
  atualizadoEm?: string;
}

// Corpo do POST /usuarios.
export interface NovoUsuario {
  nome: string;
  email: string;
  senha: string;
  tipoDocumento: TipoDocumento;
  documento: string;
  telefone: string;
  dataNascimento?: string | null;
}
