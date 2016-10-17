PROGRAMA 
  : DECLARACOES? 'algoritmo' CORPO 'fim_algoritmo'
  ;
 
DECLARACOES 
  : DECL_LOCAL_GLOBAL DECLARACOES
  ;
  
DECL_LOCAL_GLOBAL 
  : DECLARACAO_LOCAL 
  | DECLARACAO_GLOBAL
  ;
  
DECLARACAO_LOCAL 
 : 'declare' VARIAVEL
 | 'constante' IDENT ':' TIPO_BASICO '=' VALOR_CONSTANTE
 | 'tipo' IDENT ':' TIPO
 ;
 
VARIAVEL 
  : IDENT DIMENSAO MAIS_VAR* ':' TIPO
  ;

MAIS_VAR 
  : SEPARADOR IDENT DIMENSAO MAIS_VAR 
  ;

IDENTIFICADOR 
  : PONTEIROS_OPCIONAIS? IDENT DIMENSAO OUTROS_IDENT?
  ;

PONTEIROS_OPCIONAIS 
  : '^' PONTEIROS_OPCIONAIS? 
  ;

OUTROS_IDENT 
  : '.' IDENTIFICADOR
  ;

DIMENSAO 
  : ('[' EXP_ARITMETICA ']')*
  ;

TIPO 
  : REGISTRO 
  | TIPO_ESTENDIDO
  ;

MAIS_IDENT 
  : SEPARADOR IDENTIFICADOR MAIS_IDENT? 
  ;

MAIS_VARIAVEIS 
  : VARIAVEL MAIS_VARIAVEIS?
  ;

TIPO_BASICO 
  : 'literal' 
  | 'inteiro' 
  | 'real' 
  | 'logico'
  ;

TIPO_BASICO_IDENT 
  : TIPO_BASICO 
  | IDENT
  ;

TIPO_ESTENDIDO 
  : PONTEIROS_OPCIONAIS? TIPO_BASICO_IDENT
  ;

VALOR_CONSTANTE 
  : CADEIA 
  | NUM_INT 
  | NUM_REAL 
  | TRUE
  | FALSE
  ;

REGISTRO 
  : 'registro' VARIAVEL MAIS_VARIAVEIS? 'fim_registro'
  ;

DECLARACAO_GLOBAL 
  : 'procedimento' IDENT AP PARAMETRO? FP DECLARACOES_LOCAIS? COMANDOS 'fim_procedimento'
  | 'funcao' IDENT AP PARAMETRO? FP ':' TIPO_ESTENDIDO DECLARACOES_LOCAIS? COMANDOS 'fim_funcao'
  ;

PARAMETRO 
  : VAR? IDENTIFICADOR MAIS_IDENT? ':' TIPO_ESTENDIDO MAIS_PARAMETROS?
  ;

MAIS_PARAMETROS 
  : SEPARADOR PARAMETRO 
  ;

DECLARACOES_LOCAIS 
  : DECLARACAO_LOCAL DECLARACOES_LOCAIS? 
  ;

CORPO 
  : DECLARACOES_LOCAIS? COMANDOS
  ;

COMANDOS 
  : CMD+ 
  ;

CMD 
  : 'leia' AP IDENTIFICADOR MAIS_IDENT? FP
  | 'escreva' AP EXPRESSAO MAIS_EXPRESSAO? FP
  | 'se' EXPRESSAO 'entao' COMANDOS SENAO_OPCIONAL? 'fim_se'
  | 'caso' EXP_ARITMETICA 'seja' SELECAO SENAO_OPCIONAL? 'fim_caso'
  | 'para' IDENT '-' <EXP_ARITMETICA 'ate' EXP_ARITMETICA 'faca' COMANDOS 'fim_para'
  | 'enquanto' EXPRESSAO 'faca' COMANDOS 'fim_enquanto'
  | 'faca' COMANDOS 'ate' EXPRESSAO
  | '^' IDENT OUTROS_IDENT? DIMENSAO '-' <EXPRESSAO
  | IDENT CHAMADA_ATRIBUICAO
  | 'retorne' EXPRESSAO
  ;
 
MAIS_EXPRESSAO 
  : SEPARADOR EXPRESSAO MAIS_EXPRESSAO? 
  ;

SENAO_OPCIONAL 
  : 'senao' COMANDOS 
  ;

CHAMADA_ATRIBUICAO 
  : AP ARGUMENTOS_OPCIONAL? FP 
  | OUTROS_IDENT? DIMENSAO - <EXPRESSAO
  ;

ARGUMENTOS_OPCIONAL 
  : EXPRESSAO MAIS_EXPRESSAO? 
  ;

SELECAO 
  : CONSTANTES ':' COMANDOS MAIS_SELECAO?
  ;

MAIS_SELECAO 
  : SELECAO 
  ;

CONSTANTES 
  : NUMERO_INTERVALO MAIS_CONSTANTES?
  ;

MAIS_CONSTANTES 
  : SEPARADOR CONSTANTES 
  ;

NUMERO_INTERVALO 
  : OP_UNARIO? NUM_INT INTERVALO_OPCIONAL?
  ;

INTERVALO_OPCIONAL 
  : '..' OP_UNARIO? NUM_INT 
  ;

EXP_ARITMETICA 
  : TERMO OUTROS_TERMOS?
  ;


TERMO 
  : FATOR OUTROS_FATORES*
  ;

OUTROS_TERMOS 
  : OP_ADICAO TERMO OUTROS_TERMOS? 
  ;

FATOR 
  : PARCELA OUTRAS_PARCELAS*
  ;

OUTROS_FATORES 
  : OP_MULTIPLICACAO FATOR
  ;

PARCELA 
  : OP_UNARIO? PARCELA_UNARIO 
  | PARCELA_NAO_UNARIO
  ;

PARCELA_UNARIO 
  : '^' IDENT OUTROS_IDENT? DIMENSAO 
  | IDENT CHAMADA_PARTES? 
  | NUM_INT 
  | NUM_REAL 
  | AP EXPRESSAO FP
  ;

PARCELA_NAO_UNARIO 
  : '&' IDENT OUTROS_IDENT? DIMENSAO 
  | CADEIA
  ;

OUTRAS_PARCELAS 
  : '%' PARCELA 
  ;

CHAMADA_PARTES 
  : AP EXPRESSAO MAIS_EXPRESSAO? FP
  | OUTROS_IDENT? DIMENSAO 
  ;

EXP_RELACIONAL 
  : EXP_ARITMETICA OP_OPCIONAL?
  ;

OP_OPCIONAL 
  : OP_RELACIONAL EXP_ARITMETICA 
  ;

EXPRESSAO 
  : TERMO_LOGICO OUTROS_TERMOS_LOGICOS*
  ;

TERMO_LOGICO 
  : FATOR_LOGICO OUTROS_FATORES_LOGICOS*
  ;

OUTROS_TERMOS_LOGICOS 
  : 'ou' TERMO_LOGICO
  ;

OUTROS_FATORES_LOGICOS 
  : 'e' FATOR_LOGICO 
  ;

FATOR_LOGICO 
  : OP_NAO? PARCELA_LOGICA
  ;

PARCELA_LOGICA 
  : TRUE
  | FALSE
  | EXP_RELACIONAL
  ;

VAR 
  : 'var' 
  ;

OP_UNARIO 
  : '-'
  ;
  
OP_RELACIONAL 
  : '=' 
  | '<>' 
  | '>=' 
  | '<=' 
  | '>' 
  | '<'
  ;

OP_NAO 
  : 'nao'
  ;

OP_MULTIPLICACAO 
  : '*'
  | '/'
  ;

OP_ADICAO 
  : '+'
  | '-'
  ;
  
TRUE 
  : 'verdadeiro'
  ;  
  
FALSE 
  : 'falso'
  ; 
  
AP 
  : '('
  ;

FP 
  : ')'
  ;

SEPARADOR 
  : ','
  ;
