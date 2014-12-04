/*
 * Copyright (c) 2011-2014, Andreas J. Kuebler & Christoph Zengler & Rouven Walter
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

package org.warthog.pl.decisionprocedures.satsolver.impl.picosat

import scala.collection.mutable.Map

import org.warthog.pl.decisionprocedures.satsolver.{Model, Solver}
import org.warthog.pl.io.CNFUtil
import org.warthog.pl.formulas.{PLAtom, PL}
import org.warthog.generic.formulas._

/**
 * Solver Wrapper for Picosat
 */
class Picosat extends Solver {
  private val jPicosatInstance = new JPicosat()
  private val varToID = Map[PLAtom, Int]()
  private val idToVar = Map[Int, PLAtom]()
  private var clauses: List[Set[Int]] = Nil
  private var marks: List[Int] = Nil
  private var lastState = Solver.UNKNOWN

  jPicosatInstance.picosat_init()

  override def reset() {
    jPicosatInstance.picosat_reset()
    jPicosatInstance.picosat_init()
    varToID.clear()
    idToVar.clear()
    clauses = Nil
    marks = Nil
    lastState = Solver.UNKNOWN
  }

  override def add(fm: Formula[PL]) {
    /*
     * Convert clause list to List of Set of Ints, update Int->Formula
     * and Formula->Int mapping if necessary
     */
    val lcls = CNFUtil.toCNF(fm) match {
      case Nil => Nil
      case clauses => clauses.map(clause => clause.literals.map(literal => {
        val (at, mul) = (literal.variable, if (literal.phase) 1 else -1)
        varToID.getOrElseUpdate(at, {
          val lit = varToID.size + 1
          idToVar += (lit -> at)
          lit
        }) * mul
      }).toSet)
    }
    /* add clauses to solver */
    lcls.foreach(addClauses)

    /* add clauses to solver stack */
    clauses = lcls ++ clauses

    /* an unsatisfiable formula doesn't get satisfiable by adding clauses */
    if (lastState != Solver.UNSAT)
      lastState = Solver.UNKNOWN
  }

  private def addClauses(cs: Set[Int]): Int = {
    cs.foreach(jPicosatInstance.picosat_add(_))
    jPicosatInstance.picosat_add(0)
  }

  override def mark() {
    marks = clauses.length :: marks
  }

  override def undo() {
    marks match {
      case h :: t => {
        marks = t
        jPicosatInstance.picosat_reset()
        jPicosatInstance.picosat_init()
        clauses = clauses.drop(clauses.length - h)
        clauses.foreach(addClauses)
        lastState = Solver.UNKNOWN
      }
      case _ =>
    }
  }

  override def sat(): Int = {
    if (lastState == Solver.UNKNOWN)
    /* call sat only if solver is in unknown state */
      lastState = Picosat.jPicoSatStateToSolverState(
        jPicosatInstance.picosat_sat(JPicosat.INFINITY_DECISION_LEVELS))
    lastState
  }

  override def getModel(): Option[Model] = {
    require(lastState == Solver.SAT || lastState == Solver.UNSAT, "getModel(): Solver needs to be in SAT or UNSAT state!")

    lastState match {
      case Solver.UNSAT => None
      case Solver.SAT => {
        val picosatLiterals = (for {
          i <- 1 to jPicosatInstance.picosat_variables()
          j = i * jPicosatInstance.picosat_deref(i)
          if j != 0 /* filter out unassigned variables */
        } yield j)
        val positiveVariables = picosatLiterals.filter(picosatLit => picosatLit > 0)
          .filter(picosatLit => idToVar.contains(picosatLit))
          .map(picosatLit => idToVar(picosatLit)).toList
        val negativeVariables = picosatLiterals.filter(picosatLit => picosatLit < 0)
          .filter(picosatLit => idToVar.contains(picosatLit))
          .map(picosatLit => idToVar(picosatLit)).toList
        Some(Model(positiveVariables, negativeVariables))
      }
    }
  }
}

object Picosat {
  private def jPicoSatStateToSolverState(jPicoSatState: Int) = jPicoSatState match {
    case JPicosat.UNSAT => Solver.UNSAT
    case JPicosat.SAT => Solver.SAT
    case JPicosat.UNKNOWN => Solver.UNKNOWN
  }
}