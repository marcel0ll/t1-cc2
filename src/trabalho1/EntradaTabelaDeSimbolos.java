/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalho1;

/**
 *
 * @author daniel
 */
public class EntradaTabelaDeSimbolos {
  private String nome, tipo;
  private Boolean isParameter;

  public EntradaTabelaDeSimbolos(String nome, String tipo, Boolean isParameter) {
    this.nome = nome;
    this.tipo = tipo;
    this.isParameter = isParameter;
  }

  public String getNome() {
    return nome;
  }

  public String getTipo() {
    return tipo;
  }

  public Boolean isParameter() {
    return isParameter;
  }

  @Override
  public String toString() {
    return nome+":"+ tipo + " Parametro:" + isParameter;
  }
}
