// Formato de erro do back (TratadorErros).
export interface ErroCampo {
  campo: string;
  mensagem: string;
}

export interface ErroApi {
  status: number;
  mensagem: string;
  erros?: ErroCampo[];
}
