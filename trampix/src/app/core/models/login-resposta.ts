import { Usuario } from './usuario';

export interface LoginRequisicao {
  email: string;
  senha: string;
}

export interface LoginResposta {
  token: string;
  tipo: string; // "Bearer"
  expiraEm: string; // ISO-8601
  usuario: Usuario;
}
