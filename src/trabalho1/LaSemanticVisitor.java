/*
TODO:
  check if sub identifiers are "declared"
  match functions
  check attributions
*/
package trabalho1;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import org.antlr.v4.runtime.Token;

public class LaSemanticVisitor extends LaParserBaseVisitor<Object> {

  private static final String INTEIRO = "inteiro";
  private static final String REAL = "real";
  private static final String LOGICO = "logico";
  private static final String LITERAL = "literal";
  // Uso de caracter não reconhecido pela linguagem como flag de autoGerado
  private static final String AUTOGENERATED_FLAG = "@";

  private HashMap<String, Tipo> tipos;
  private LinkedList<HashMap<String, Simbolo>> pilha;

  private int autoGeneratedTypes;
  private boolean scopeCanReturn;

  public LaSemanticVisitor() {
    super();
    tipos = new HashMap<String, Tipo>();
    pilha = new LinkedList<HashMap<String, Simbolo>>();

    autoGeneratedTypes = 0;
    scopeCanReturn = false;
  }

  private void printStack () {
    for ( HashMap<String, Simbolo> tabela : pilha ) {
      Lac.errorBuffer.println("-----");
      Lac.errorBuffer.println(tabela.keySet().toString());

    }
  }

  private boolean pilhaContemSimbolo(String simboloId) {
    for( HashMap<String, Simbolo> tabela : pilha ) {
      if ( tabela.containsKey(simboloId) ) {
        return true;
      }
    }
    return false;
  }

  private boolean checarSePilhaContemSimbolo(String ident, int linha) {
    if( !pilhaContemSimbolo(ident) ) {
      Lac.errorBuffer.println(Mensagens.erroIdentificadorNaoDeclarado(linha , ident ));
      return false;
    }
    return true;
  }

  private boolean checarSePilhaContemSimbolo(Token ident) {
    return checarSePilhaContemSimbolo(ident.getText(), ident.getLine());
  }

  private boolean checarSePilhaContemSimbolo(Simbolo simbolo) {
    return checarSePilhaContemSimbolo(simbolo.getNome(), simbolo.getLinha());
  }

  private void adicionarSimbolos(List<Simbolo> novosSimbolos) {
    for(Simbolo s: novosSimbolos) {
      adicionarSimbolo(s);
    }
  }

  private void adicionarSimbolo(Simbolo simbolo) {
    if ( !pilhaContemSimbolo(simbolo.getNome() )) {
      pilha.peek().put(simbolo.getNome(), simbolo);
    } else {
      Lac.errorBuffer.println(Mensagens.erroIdentificadorJaDeclarado( simbolo.getLinha(), simbolo.getNome() ));
      Simbolo sim = pegarSimbolo(simbolo.getNome());
      //Lac.errorBuffer.println(Integer.toString(sim.getLinha()));
    }
  }

  private Simbolo pegarSimbolo(Token id) {
    if ( checarSePilhaContemSimbolo(id) ) {
      int i;
      for( i = pilha.size() -1; i >= 0; i--) {
        HashMap<String, Simbolo> t = pilha.get(i);
        if ( t.containsKey(id.getText()) ) {
          return t.get(id.getText());
        }
      }
    }
    return new Simbolo("ERRO", "ERRO", -1, LacClass.PARSER);
  }

  private Simbolo pegarSimbolo(String id) {
    if ( pilhaContemSimbolo(id) ) {
      int i;
      for( i = pilha.size() -1; i >= 0; i--) {
        HashMap<String, Simbolo> t = pilha.get(i);
        if ( t.containsKey(id) ) {
          return t.get(id);
        }
      }
    }

    return new Simbolo("ERRO", "ERRO", -1, LacClass.PARSER);
  }

  @Override
  public Void visitPrograma(LaParser.ProgramaContext ctx) {
    tipos.put(LITERAL, new Tipo(LITERAL));
    tipos.put(INTEIRO, new Tipo(INTEIRO));
    tipos.put(REAL, new Tipo(REAL));
    tipos.put(LOGICO, new Tipo(LOGICO));

    // Adiciona tabela de escopo global na pilha de tabelas
    pilha.push( new HashMap<String, Simbolo>() );

    visitChildren(ctx);
    return null;
  }

  @Override
  public Void visitDeclareConstante(LaParser.DeclareConstanteContext ctx) {
    String simboloId = ctx.Ident().getText();
    int linha = ctx.Ident().getSymbol().getLine();
    String tipo = ctx.tipo_basico().getText();

    adicionarSimbolo(new Simbolo(simboloId, tipo, linha, LacClass.CONSTANTE));
    visitChildren(ctx);
    return null;
  }
  @Override
  public Void visitDeclareVariavel(LaParser.DeclareVariavelContext ctx) {
    ArrayList<Simbolo> simbolos = (ArrayList<Simbolo>) visit(ctx.lista_variavel());

    for( Simbolo s: simbolos ) {
      adicionarSimbolo(s);
    }

    return null;
  }

  @Override
  public Void visitDeclareTipo(LaParser.DeclareTipoContext ctx) {
    Token tipoTk = ctx.Ident().getSymbol();
    String tipoId = tipoTk.getText();
    int linha = tipoTk.getLine();
    Object tipoVisit = visit(ctx.tipo());

    if ( tipoVisit instanceof String ) {
      String tipoVisitId = (String) tipoVisit;
      String tipoSemPonteiro = tipoVisitId.replace("^", "");
      if( tipos.containsKey ( tipoSemPonteiro ) ) {
        Tipo novoTipo = new Tipo(tipoId, tipoVisitId);
        tipos.put(tipoId, novoTipo);

        // Adiciona na tabela de simbolos o novo tipo
        adicionarSimbolo(new Simbolo(tipoId, tipoVisitId, linha, LacClass.TIPO));
      } else {
        Lac.errorBuffer.println(Mensagens.erroTipoNaoDeclarado( linha, tipoId ));
      }
    } else if ( tipoVisit instanceof List ) {
      Tipo novoTipo = new Tipo(tipoId);
      tipos.put(tipoId, novoTipo);

      // Adiciona na tabela de simbolos o novo tipo
      adicionarSimbolo(new Simbolo(tipoId, tipoId, linha, LacClass.TIPO));

      List<Simbolo> simbolosTipo = (List<Simbolo>) tipoVisit;
      for( Simbolo simbolo : simbolosTipo) {
        novoTipo.addSimbolo( simbolo );
      }
    }

    return null;
  }

  @Override
  public Object visitTipoRegistro(LaParser.TipoRegistroContext ctx) {
    return visit(ctx.registro());
  }

  @Override
  public Object visitRegistro(LaParser.RegistroContext ctx) {
    ArrayList<Simbolo> registroSimbolos = new ArrayList<Simbolo>();

    for (LaParser.Lista_variavelContext variavel : ctx.lista_variavel()) {
      List<Simbolo> simbolos = (List<Simbolo>) visit(variavel);
      registroSimbolos.addAll(simbolos);
    }

    return registroSimbolos;
  }

  @Override
  public Object visitTipoReferencia(LaParser.TipoReferenciaContext ctx) {
    return ctx.tipo_estendido().getText();
  }

  @Override
  public Simbolo visitVariavel_unica(LaParser.Variavel_unicaContext ctx) {
    Token ident = ctx.Ident().getSymbol();
    Simbolo simbolo = new Simbolo( ident.getText(), "undefined", ident.getLine(), LacClass.PARSER );
    simbolo.setDimensoes( (ArrayList<Integer>) visit(ctx.lista_dimensao() ));
    return simbolo;
  }

  @Override
  public Integer visitDimensao(LaParser.DimensaoContext ctx) {
    return 0;
    //TODO return visit(ctx.exp_aritmetica());
  }

  @Override
  public ArrayList<Integer> visitLista_dimensao(LaParser.Lista_dimensaoContext ctx) {
    ArrayList<Integer> listaDeDimensoes = new ArrayList<Integer>();
    for (LaParser.DimensaoContext dimensao : ctx.dimensao()) {
      listaDeDimensoes.add((Integer) visit(dimensao));
    }
    return listaDeDimensoes;
  }

  @Override
  public Object visitLista_variavel(LaParser.Lista_variavelContext ctx) {
    ArrayList<Simbolo> simbolos = new ArrayList<Simbolo>();
    // Get name list
    ArrayList<Simbolo>  parserSimbolos = new ArrayList<Simbolo>();
    parserSimbolos.add( (Simbolo) visit(ctx.variavel_unica() ));

    for (LaParser.Mais_variaveisContext mais_var : ctx.mais_variaveis()) {
      Simbolo simbolo = (Simbolo) visit(mais_var);
      parserSimbolos.add(simbolo);
    }

    // Get Type
    Object visitTipo = visit(ctx.tipo());
    String tipoId = "undefined";

    if ( visitTipo instanceof String ) {
      tipoId = (String) visitTipo;
      // Remove ponteiros da verificação do tipo
      tipoId = tipoId.replace("^", "");
      if( !tipos.containsKey ( tipoId ) ) {
        // else warn error
        int linha = ctx.getStart().getLine();
        Lac.errorBuffer.println(Mensagens.erroTipoNaoDeclarado( linha, tipoId ));
      }
    } else if ( visitTipo instanceof List ) {
      tipoId = AUTOGENERATED_FLAG + autoGeneratedTypes;
      autoGeneratedTypes++;

      Tipo novoTipo = new Tipo(tipoId);
      tipos.put(tipoId, novoTipo);

      List<Simbolo> simbolosTipo = (List<Simbolo>) visitTipo;
      for( Simbolo simbolo : simbolosTipo) {
        novoTipo.addSimbolo( simbolo );
      }
    }

    // if type is registered add symbols
    for( Simbolo ps : parserSimbolos ) {
      Simbolo simbolo = new Simbolo(ps.getNome(), tipoId, ps.getLinha());
      simbolo.setDimensoes(ps.getDimensoes());
      simbolos.add(simbolo);
    }

    return simbolos;
  }

  @Override
  public Simbolo visitMais_variaveis(LaParser.Mais_variaveisContext ctx) {
    return (Simbolo) visit(ctx.variavel_unica());
  }

  @Override
  public Void visitCmdAssign(LaParser.CmdAssignContext ctx) {
    Object pointer = ctx.Pointer();
    Token ident = ctx.Ident().getSymbol();
    String text = (pointer != null ? "^" : "") + ident.getText();

    // Lac.errorBuffer.println(ident.getText());

    if ( pilhaContemSimbolo( ident.getText() ) ) {
      ArrayList<Simbolo> subIdents = new ArrayList<Simbolo>();
      Simbolo sim = pegarSimbolo(ident.getText());
      Tipo simType = tipos.get(sim.getTipo());
      Tipo type = getType(ctx.expressao());

      for( LaParser.Sub_identificadorContext sub : ctx.sub_identificador()) {
        Simbolo subSim = (Simbolo) visit(sub);
        text += "." + subSim.getNome();
        if ( sim.temSimbolo(subSim)) {
        }
      }

      ArrayList<Integer> dims = sim.getDimensoes();
      if( dims != null ) {
        for( Integer i : dims ) {
          text+="[" + i.toString() + "]";
        }
      }

      if( !simType.getNome().equals(type.getNome())) {
        if ( !(simType.getNome().equals(REAL) && type.getNome().equals(INTEIRO))) {
          Lac.errorBuffer.println(Mensagens.erroAtribuicaoIncompativel( ident.getLine(), text));
        }
      }
    } else {
      //TODO
      // String sub = null ;
      // if ( sub != null ) {
      //   Mensagens.erroIdentificadorNaoDeclarado( ident.getLine(), ident.getText()+sub );
      // } else {
      // }
      Lac.errorBuffer.println(Mensagens.erroIdentificadorNaoDeclarado( ident.getLine(), ident.getText() ));
    }

    visitChildren(ctx);
    return null;
  }

  @Override
  public Void visitCmdCall(LaParser.CmdCallContext ctx) {
    visitChildren(ctx);
    return null;
  }

  @Override
  public Void visitCmdRead(LaParser.CmdReadContext ctx) {
    ArrayList<Simbolo> listaIdentificadores = (ArrayList<Simbolo>) visit(ctx.lista_identificador());

    for( Simbolo s : listaIdentificadores ) {
      checarSePilhaContemSimbolo(s);
    }

    return null;
  }

  @Override
  public Void visitCmdReturn(LaParser.CmdReturnContext ctx) {
    if( !scopeCanReturn ) {
      int linha = ctx.getStart().getLine();
      Lac.errorBuffer.println( Mensagens.erroRetorneEmEscopoIncorreto( linha ) );
    }

    visitChildren(ctx);
    return null;
  }

  @Override
  public Void visitDeclareProcedure(LaParser.DeclareProcedureContext ctx) {
    scopeCanReturn = false;
    Token procedimentoTk = ctx.Ident().getSymbol();
    String simboloId = procedimentoTk.getText();
    int linha = procedimentoTk.getLine();
    Simbolo procedimento = new Simbolo(simboloId, "", linha, LacClass.PROCEDIMENTO);

    adicionarSimbolo(procedimento);
    pilha.push( new HashMap<String, Simbolo>() );
    ArrayList<Simbolo> parametros = (ArrayList<Simbolo>) visit(ctx.lista_parametros());
    procedimento.addSimbolos(parametros);

    for( LaParser.Declaracao_localContext local : ctx.declaracao_local()) {
      visit(local);
    }
    for( LaParser.ComandoContext comando : ctx.comando()) {
      visit(comando);
    }
    pilha.pop();

    return null;
  }

	@Override
  public Void visitDeclareFunction(LaParser.DeclareFunctionContext ctx) {
    scopeCanReturn = true;
    Token funcaoTk = ctx.Ident().getSymbol();
    String simboloId = funcaoTk.getText();
    int linha = funcaoTk.getLine();
    String tipoFuncao = ctx.tipo_estendido().getText();
    Simbolo funcao = new Simbolo(simboloId, tipoFuncao, linha, LacClass.FUNCAO);

    adicionarSimbolo(funcao);
    pilha.push( new HashMap<String, Simbolo>() );
    ArrayList<Simbolo> parametros = (ArrayList<Simbolo>) visit(ctx.lista_parametros());
    funcao.addSimbolos(parametros);

    for( LaParser.Declaracao_localContext local : ctx.declaracao_local()) {
      visit(local);
    }
    for( LaParser.ComandoContext comando : ctx.comando()) {
      visit(comando);
    }
    pilha.pop();

    return null;
  }

  @Override
  public ArrayList<Simbolo> visitParametro(LaParser.ParametroContext ctx) {
    ArrayList<Simbolo> parametros = new ArrayList<Simbolo>();
    String tipo = ctx.tipo_estendido().getText();
    int linha = ctx.getStart().getLine();
    String tipoSemPonteiro = tipo.replace("^", "");

    if( !tipos.containsKey ( tipoSemPonteiro ) ) {
      Lac.errorBuffer.println(Mensagens.erroTipoNaoDeclarado( linha, tipo ));
    }

    ArrayList<Simbolo> identificadores = new ArrayList<Simbolo>();

    for( Simbolo s: (ArrayList<Simbolo>) visit(ctx.lista_identificador()) ) {
      Simbolo simbolo = new Simbolo(s.getNome(), tipo, s.getLinha(), LacClass.PARAMETRO);
      simbolo.setDimensoes(s.getDimensoes());

      parametros.add(simbolo);
    }

    visitChildren(ctx);
    return parametros;
  }
  @Override
  public ArrayList<Simbolo> visitLista_parametros(LaParser.Lista_parametrosContext ctx) {
    ArrayList<Simbolo> parametros = (ArrayList<Simbolo>) visit(ctx.parametro());

    for( LaParser.Mais_parametroContext mais_parametro : ctx.mais_parametro()) {
      ArrayList<Simbolo> mais_parametros = (ArrayList<Simbolo>) visit(mais_parametro);

      parametros.addAll(mais_parametros);
    }

    adicionarSimbolos(parametros);
    return parametros;
  }

  @Override
  public Void visitCorpo(LaParser.CorpoContext ctx) {
    // Lac.errorBuffer.println(Integer.toString(pilha.size()));
    scopeCanReturn = false;
    visitChildren(ctx);
    return null;
  }

  @Override
  public Simbolo visitIdentificador(LaParser.IdentificadorContext ctx) {
    Token ident = ctx.Ident().getSymbol();
    ArrayList<Integer> dimensoes = (ArrayList<Integer>) visit(ctx.lista_dimensao());

    Simbolo simbolo = new Simbolo( ident.getText(), "undefined", ident.getLine(), LacClass.PARSER );
    simbolo.setDimensoes(dimensoes);

    List<LaParser.Sub_identificadorContext> subs = ctx.sub_identificador();
    if( subs != null && subs.size() > 0 ) {
      for( LaParser.Sub_identificadorContext sub : subs ) {
        simbolo.addSimbolo( (Simbolo) visit(sub) );
      }
    }

    return simbolo;
  }

  @Override
  public ArrayList<Simbolo> visitLista_identificador(LaParser.Lista_identificadorContext ctx) {
    ArrayList<Simbolo> simbolos = new ArrayList<Simbolo>();
    simbolos.add( (Simbolo) visit(ctx.identificador() ));

    for( LaParser.Mais_identificadorContext identificador : ctx.mais_identificador() ) {
      Simbolo simbolo = (Simbolo) visit(identificador);
      simbolos.add(simbolo);
    }

    visitChildren(ctx);
    return simbolos;
  }

  @Override
  public Void visitParcelaUnarioChamadaFuncao(LaParser.ParcelaUnarioChamadaFuncaoContext ctx) {
    visitChildren(ctx);
    return null;
  }

  @Override
  public Simbolo visitSub_identificador(LaParser.Sub_identificadorContext ctx) {
    return (Simbolo) visit(ctx.identificador());
  }

  @Override
  public Void visitParcelaUnarioVariavel(LaParser.ParcelaUnarioVariavelContext ctx) {
    Token ident = ctx.Ident().getSymbol();
    checarSePilhaContemSimbolo(ident);
    visitChildren(ctx);
    return null;
  }

  @Override
  public Tipo visitExpressao(LaParser.ExpressaoContext ctx) {
    //Lac.errorBuffer.println(ctx.getText() + "/" + getType(ctx));
    Tipo type = getType(ctx);

    return type;
  }

/*
Expressao type getter
*/

  public ArrayList<Tipo> getType(LaParser.Lista_expressaoContext ctx) {
    ArrayList<Tipo> listaTipos = new ArrayList<Tipo>();

    listaTipos.add( (Tipo) visit(ctx.expressao()));

    for( LaParser.Mais_expressaoContext mais_expressao : ctx.mais_expressao()) {
      listaTipos.add( (Tipo) visit(mais_expressao.expressao()));
    }

    return listaTipos;
  }

  public Tipo getType(LaParser.ExpressaoContext ctx) {
    Tipo ret = getType(ctx.termo_logico());
    List<LaParser.Outros_termos_logicosContext> outros_termos_logicos = ctx.outros_termos_logicos();
    if( outros_termos_logicos != null && outros_termos_logicos.size() > 0 ) {
      ret = tipos.get(LOGICO);
      for (LaParser.Outros_termos_logicosContext outro : outros_termos_logicos) {
        getType(outro.termo_logico());
      }
    }

    return ret;
  }

  public Tipo getType(LaParser.Termo_logicoContext ctx) {
    Tipo ret = getType(ctx.fator_logico());
    List<LaParser.Outros_fatores_logicosContext> outros_fatores_logicos = ctx.outros_fatores_logicos();
    if ( outros_fatores_logicos != null && outros_fatores_logicos.size() > 0 ) {
      ret = tipos.get(LOGICO);
      for ( LaParser.Outros_fatores_logicosContext outro : outros_fatores_logicos ) {
        getType(outro.fator_logico());
      }
    }

    return ret;
  }

  public Tipo getType(LaParser.Fator_logicoContext ctx) {
    return getType(ctx.parcela_logica());
  }

  public Tipo getType(LaParser.Parcela_logicaContext ctx) {
    if ( ctx instanceof LaParser.ParcelaLogicaTrueContext ) {
      return getType((LaParser.ParcelaLogicaTrueContext) ctx);
    } else if ( ctx instanceof LaParser.ParcelaLogicaFalseContext) {
      return getType((LaParser.ParcelaLogicaFalseContext) ctx);
    } else {
      return getType((LaParser.ParcelaLogicaExpRelacionalContext) ctx);
    }
  }

  public Tipo getType(LaParser.ParcelaLogicaTrueContext ctx) {
    return tipos.get(LOGICO);
  }

  public Tipo getType(LaParser.ParcelaLogicaFalseContext ctx) {
    return tipos.get(LOGICO);
  }

  public Tipo getType(LaParser.ParcelaLogicaExpRelacionalContext ctx) {
    return getType(ctx.exp_relacional());
  }

  public Tipo getType(LaParser.Exp_relacionalContext ctx) {
    Tipo ret = getType(ctx.exp_aritmetica());
    if ( ctx.op_opcional() != null ) {
      ret = tipos.get(LOGICO);
      getType(ctx.op_opcional().exp_aritmetica());
    }
    return ret;
  }

  public Tipo getType(LaParser.Exp_aritmeticaContext ctx) {
    Tipo termo = getType(ctx.termo());
    Tipo ret = termo;
    List<LaParser.Outros_termosContext> outros_termos = ctx.outros_termos();
    if( outros_termos != null && outros_termos.size() > 0 ) {
      boolean isInteiro = true;

      ArrayList<Tipo> expTipos = new ArrayList<Tipo>();

      for( LaParser.Outros_termosContext outro : outros_termos) {
        Tipo t = getType(outro.termo());
        expTipos.add(t);
      }

      // se termo = literal -> literal
      // se termo = real -> real
      // se termo = logico -> ERRO
      // se termo = inteiro -> inteiro | real
      if ( termo.getNome() == LOGICO ) {
        Lac.errorBuffer.println("TENTANDO SOMAR VALORES LOGICOS");
      } else if ( termo.getNome() == LITERAL ) {
        for( Tipo t : expTipos) {
          if ( t.getNome() != LITERAL ) {
            // Lac.errorBuffer.println("TENTANDO SOMAR VALOR DIFERENTE DE LITERAL EM LITERAL");
          }
        }
      } else if ( termo.getNome() == INTEIRO || termo.getNome() == REAL ) {
        for( Tipo t : expTipos) {
          if ( t != null && t.getNome() == REAL ) {
            ret = tipos.get(REAL);
          }
        }
      } else {
        Lac.errorBuffer.println("NÂO SEI SOMAR ESSE TIPO:" + termo.getNome());
      }

    }
    return ret;
  }

  public Tipo getType(LaParser.TermoContext ctx ) {
    Tipo ret = getType(ctx.fator());
    List<LaParser.Outros_fatoresContext> outros_fatores = ctx.outros_fatores();
    if ( outros_fatores != null && outros_fatores.size() > 0 ) {
      boolean isInteiro = true;

      ArrayList<Tipo> expTipos = new ArrayList<Tipo>();
      for( LaParser.Outros_fatoresContext outro : outros_fatores) {
        Tipo t = getType(outro.fator());
        expTipos.add(t);
        if(t != null && t.getNome() != INTEIRO) {
          isInteiro = false;
        }
      }

      if ( isInteiro ) {
        return tipos.get(INTEIRO);
      } else {
        return tipos.get(REAL);
      }
    }
    return ret;
  }

  public Tipo getType(LaParser.FatorContext ctx) {
    Tipo ret = getType(ctx.parcela());
    List<LaParser.Outras_parcelasContext> outras_parcelas = ctx.outras_parcelas();
    if ( outras_parcelas != null && outras_parcelas.size() > 0 ) {
      ret = tipos.get(INTEIRO);
      for( LaParser.Outras_parcelasContext outra : outras_parcelas ) {
        getType(outra.parcela());
      }
    }
    return ret;
  }

  public Tipo getType(LaParser.ParcelaContext ctx) {
    if( ctx instanceof LaParser.ParcelaParcelaUnarioContext ) {
      return getType((LaParser.ParcelaParcelaUnarioContext) ctx);
    } else {
      return getType((LaParser.ParcelaParcelaNaoUnarioContext) ctx);
    }
  }

  public Tipo getType(LaParser.ParcelaParcelaUnarioContext ctx) {
    return getType(ctx.parcela_unario());
  }

  public Tipo getType(LaParser.Parcela_unarioContext ctx) {
    if( ctx instanceof LaParser.ParcelaUnarioVariavelContext ) {
      return getType((LaParser.ParcelaUnarioVariavelContext) ctx);
    } else if ( ctx instanceof LaParser.ParcelaUnarioChamadaFuncaoContext ) {
      return getType((LaParser.ParcelaUnarioChamadaFuncaoContext) ctx);
    } else if ( ctx instanceof LaParser.ParcelaUnarioInteiroContext ) {
      return getType((LaParser.ParcelaUnarioInteiroContext) ctx);
    } else if ( ctx instanceof LaParser.ParcelaUnarioRealContext ) {
      return getType((LaParser.ParcelaUnarioRealContext) ctx);
    } else {
      return getType((LaParser.ParcelaUnarioParentesesContext) ctx);
    }
  }

  public Tipo getType(LaParser.ParcelaParcelaNaoUnarioContext ctx) {
    return getType(ctx.parcela_nao_unario());
  }

  public Tipo getType(LaParser.Parcela_nao_unarioContext ctx) {
    if( ctx instanceof LaParser.ParcelaNaoUnarioVetorContext ) {
      return getType((LaParser.ParcelaNaoUnarioVetorContext) ctx);
    } else {
      return getType((LaParser.ParcelaNaoUnarioStringContext) ctx);
    }
  }

  public Tipo getType(LaParser.ParcelaUnarioVariavelContext ctx) {
    Token tk = ctx.Ident().getSymbol();
    String id = tk.getText();
    Simbolo sim = pegarSimbolo(tk);
    Tipo ret = tipos.get(sim.getTipo());
    if ( ret != null && !ret.isSimple() ) {
      for( LaParser.Sub_identificadorContext sub : ctx.sub_identificador()) {
        Simbolo subSim = (Simbolo) visit(sub);
        ret = tipos.get(subSim.getTipo());
      }
    }
    return ret;
  }

  public Tipo getType(LaParser.ParcelaUnarioChamadaFuncaoContext ctx) {
    Token ident = ctx.Ident().getSymbol();

    if ( checarSePilhaContemSimbolo( ident ) ) {
      ArrayList<Tipo> argumentos = (ArrayList<Tipo>) getType(ctx.lista_expressao());
      Simbolo sim = pegarSimbolo(ident.getText());

      ArrayList<Simbolo> assinatura = sim.getSimbolos();

      if ( argumentos == null && assinatura != null || assinatura == null && argumentos != null ) {

        Lac.errorBuffer.println(Mensagens.erroIncompatibilidadeDeParametros(ident.getLine(), ident.getText()));
      } else if ( argumentos == null && assinatura == null ) {

      } else if( argumentos.size() != assinatura.size() ) {
        Lac.errorBuffer.println(Mensagens.erroIncompatibilidadeDeParametros(ident.getLine(), ident.getText()));
      } else {
        for( int i = 0; i < argumentos.size(); i++ ) {
          Tipo t1 = argumentos.get(i);
          Tipo t2 = tipos.get(assinatura.get(i).getTipo());
          // Lac.errorBuffer.println( t1.toString() );

          if( !t1.getNome().equals(t2.getNome()) ) {
            Lac.errorBuffer.println(Mensagens.erroIncompatibilidadeDeParametros(ident.getLine(), ident.getText()));
            break;
          }
        }
      }
    }

    return tipos.get(pegarSimbolo(ctx.Ident().getSymbol()).getTipo());
  }

  public Tipo getType(LaParser.ParcelaUnarioInteiroContext ctx) {
    return tipos.get(INTEIRO);
  }

  public Tipo getType(LaParser.ParcelaUnarioRealContext ctx) {
    return tipos.get(REAL);
  }

  public Tipo getType(LaParser.ParcelaUnarioParentesesContext ctx) {
    return getType(ctx.expressao());
  }

  public Tipo getType(LaParser.ParcelaNaoUnarioVetorContext ctx) {
    return tipos.get(pegarSimbolo(ctx.Ident().getSymbol()).getTipo());
  }

  public Tipo getType(LaParser.ParcelaNaoUnarioStringContext ctx) {
    return tipos.get(LITERAL);
  }
}
