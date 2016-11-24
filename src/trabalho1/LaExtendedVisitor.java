package trabalho1;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import org.antlr.v4.runtime.Token;

public class LaExtendedVisitor extends LaBaseVisitor<Object> {

  // Uso de caracter não reconhecido pela linguagem como flag de autoGerado
  private static final String AUTOGENERATED_FLAG = "@";

  private HashMap<String, Tipo> tipos;
  private LinkedList<HashMap<String, Simbolo>> pilha;

  private int autoGeneratedTypes;
  private boolean scopeCanReturn;

  public LaExtendedVisitor() {
    super();
    tipos = new HashMap<String, Tipo>();
    pilha = new LinkedList<HashMap<String, Simbolo>>();

    autoGeneratedTypes = 0;
    scopeCanReturn = false;
  }

  private boolean pilhaContemSimbolo(String simboloId) {
    for( HashMap<String, Simbolo> tabela : pilha ) {
      if ( tabela.containsKey(simboloId) ) {
        return true;
      }
    }
    return false;
  }

  private void checarSePilhaContemSimbolo(Token ident) {
    if( !pilhaContemSimbolo(ident.getText()) ) {
      Lac.errorBuffer.println(Mensagens.erroIdentificadorNaoDeclarado(ident.getLine(), ident.getText()));
    }
  }

  private void adicionarSimbolo(Simbolo simbolo) {
    if ( !pilhaContemSimbolo(simbolo.getNome() )) {
      pilha.peek().put(simbolo.getNome(), simbolo);
    } else {
      Lac.errorBuffer.println(Mensagens.erroIdentificadorJaDeclarado( simbolo.getLinha(), simbolo.getNome() ));
    }
  }

  @Override
  public Void visitPrograma(LaParser.ProgramaContext ctx) {
    tipos.put("literal", new Tipo("literal"));
    tipos.put("inteiro", new Tipo("inteiro"));
    tipos.put("real", new Tipo("real"));
    tipos.put("logico", new Tipo("logico"));

    // Adiciona tabela de escopo global na pilha de tabelas
    pilha.push( new HashMap<String, Simbolo>() );

    visitChildren(ctx);
    return null;
  }

  @Override
  public Void visitDeclareConstante(LaParser.DeclareConstanteContext ctx) {
    String simboloId = ctx.IDENT().getText();
    int linha = ctx.IDENT().getSymbol().getLine();
    String tipo = ctx.tipo_basico().getText();

    adicionarSimbolo(new Simbolo(simboloId, tipo, linha, LacClass.CONSTANTE));
    visitChildren(ctx);
    return null;
  }
  @Override
  public Void visitDeclareVariavel(LaParser.DeclareVariavelContext ctx) {
    ArrayList<Simbolo> simbolos = (ArrayList<Simbolo>) visit(ctx.variavel());

    for( Simbolo s: simbolos ) {
      adicionarSimbolo(s);
    }

    return null;
  }

  @Override
  public Void visitDeclareTipo(LaParser.DeclareTipoContext ctx) {
    Token tipoTk = ctx.IDENT().getSymbol();
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

    for (LaParser.VariavelContext variavel : ctx.variavel()) {
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
  public Object visitVariavel(LaParser.VariavelContext ctx) {
    ArrayList<Simbolo> simbolos = new ArrayList<Simbolo>();
    // Get name list
    ArrayList<Token> tks = new ArrayList<Token>();
    tks.add(ctx.IDENT().getSymbol());

    for (LaParser.Mais_varContext mais_var : ctx.mais_var()) {
      Token tk = (Token) visit(mais_var);
      tks.add(tk);
    }

    // Get Type
    Object visitTipo = visit(ctx.tipo());

    if ( visitTipo instanceof String ) {
      String tipoId = (String) visitTipo;
      // Remove ponteiros da verificação do tipo
      tipoId = tipoId.replace("^", "");
      if( tipos.containsKey ( tipoId ) ) {
        // if type is registered add symbols
        for( Token tk : tks ) {
          simbolos.add(new Simbolo( tk.getText(), tipoId, tk.getLine()));
        }
      } else {
        // else warn error
        int linha = ctx.getStart().getLine();
        Lac.errorBuffer.println(Mensagens.erroTipoNaoDeclarado( linha, tipoId ));
      }
    } else if ( visitTipo instanceof List ) {
      String tipoId = AUTOGENERATED_FLAG + autoGeneratedTypes;
      autoGeneratedTypes++;

      Tipo novoTipo = new Tipo(tipoId);
      tipos.put(tipoId, novoTipo);

      List<Simbolo> simbolosTipo = (List<Simbolo>) visitTipo;
      for( Simbolo simbolo : simbolosTipo) {
        novoTipo.addSimbolo( simbolo );
      }

      for( Token tk : tks ) {
        simbolos.add(new Simbolo( tk.getText(), tipoId, tk.getLine()));
      }
    }

    return simbolos;
  }

  @Override
  public Object visitMais_var(LaParser.Mais_varContext ctx) {
    return ctx.IDENT().getSymbol();
  }

  @Override
  public Void visitCmdRetorne(LaParser.CmdRetorneContext ctx) {
    if( !scopeCanReturn ) {
      int linha = ctx.getStart().getLine();
      Lac.errorBuffer.println( Mensagens.erroRetorneEmEscopoIncorreto( linha ) );
    }
    visitChildren(ctx);
    return null;
  }

  @Override
  public Void visitDeclareProcedimento(LaParser.DeclareProcedimentoContext ctx) {
    scopeCanReturn = false;
    Token procedimentoTk = ctx.IDENT().getSymbol();
    String simboloId = procedimentoTk.getText();
    int linha = procedimentoTk.getLine();
    Simbolo procedimento = new Simbolo(simboloId, "", linha, LacClass.PROCEDIMENTO);
    adicionarSimbolo(procedimento);

    visitChildren(ctx);
    return null;
  }

	@Override
  public Void visitDeclareFuncao(LaParser.DeclareFuncaoContext ctx) {
    scopeCanReturn = true;
    Token funcaoTk = ctx.IDENT().getSymbol();
    String simboloId = funcaoTk.getText();
    int linha = funcaoTk.getLine();
    Simbolo funcao = new Simbolo(simboloId, "", linha, LacClass.FUNCAO);
    adicionarSimbolo(funcao);

    visitChildren(ctx);
    return null;
  }

  @Override
  public Void visitCorpo(LaParser.CorpoContext ctx) {
    scopeCanReturn = false;
    visitChildren(ctx);
    return null;
  }

  @Override
  public Void visitIdentificador(LaParser.IdentificadorContext ctx) {
    Token ident = ctx.IDENT().getSymbol();
    checarSePilhaContemSimbolo(ident);
    visitChildren(ctx);
    return null;
  }

  @Override
  public Void visitParcelaUnarioChamadaFuncao(LaParser.ParcelaUnarioChamadaFuncaoContext ctx) {
    Token ident = ctx.IDENT().getSymbol();
    checarSePilhaContemSimbolo(ident);
    visitChildren(ctx);
    return null;
  }

  @Override
  public Token visitOutros_ident(LaParser.Outros_identContext ctx) {
    return ctx.IDENT().getSymbol();
  }

  @Override
  public Void visitParcelaUnarioVariavel(LaParser.ParcelaUnarioVariavelContext ctx) {
    Token ident = ctx.IDENT().getSymbol();
    checarSePilhaContemSimbolo(ident);
    visitChildren(ctx);
    return null;
  }

 }
