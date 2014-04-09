/*
 * Copyright (c) 2011-2014, Andreas J. Kuebler & Christoph Zengler
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.warthog.fol.formulas

import org.warthog.generic.formulas.Formula

/**
  * FOL function application
  * @param symbol the function symbol
  * @param args the applied terms
  */
case class FOLFunction(symbol: FunctionSymbol, args: FOLTerm*) extends FOLTerm {

  override def toString = if (args.size == 0) symbol.toString else symbol + "(" + (args.mkString(",")) + ")"

  def vars = if (args.size > 0) args.map(_.vars).reduce(_ union _).distinct else List()

  def functions = args.foldLeft(List(symbol))((set, elem) => set union elem.functions).distinct

  def numOfNodes = args.foldLeft(1)((s, e) => s + e.numOfNodes)

  def tsubst(s: Map[FOLVariable, FOLTerm]) = FOLFunction(symbol, args.map(_.tsubst(s)): _*)
}

object FOLFunction {
  def apply(name: String, args: FOLTerm*): FOLFunction =
    new FOLFunction(new FunctionSymbol(name, args.length), args: _*)
}
