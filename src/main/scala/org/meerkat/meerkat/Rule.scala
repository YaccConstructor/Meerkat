/*
 * Copyright (c) 2014 CWI. All rights reserved.
 * 
 * Authors:
 *     Anastasia Izmaylova  <anastasia.izmaylova@cwi.nl>
 *     Ali Afroozeh         <ali.afroozeh@cwi.nl>
 */
package org.meerkat.meerkat

import org.meerkat.sppf.NonPackedNode
import Result.memo
import org.meerkat.util.Input
import org.meerkat.sppf.SPPFLookup
import org.meerkat.tree.RuleType
import org.meerkat.tree.Nonterminal

class Head(head: String) {
  def ::= (parser: => MeerkatParser): Rule = new Rule(head, parser.rule(Nonterminal(head)))
}

class Rule(head: String, parser: => MeerkatParser) {
  var p: MeerkatParser = null
  var eval = 0
    
  def getParser: MeerkatParser = {
    if(eval == 0) {
      eval = 1
      p = parser
    }
    p
  }
  
  def getHead: String = this.head
}

object Rule {
  
  implicit def memoize(r: Rule): MeerkatParser = {
    var table: Array[Result[NonPackedNode]] = null
    val p = new MeerkatParser {
              def apply(input: Input, sppf: SPPFLookup, i: Int): Result[NonPackedNode]= {
                if(table == null) {
                  table = new Array(input.length + 1)
                }
                val result = table(i)
                if(result == null) {
                  table(i) = memo(r.getParser(input, sppf, i))
                  table(i)
                } else {
                  result
                }
                
              }
              
              override def symbol = org.meerkat.tree.Nonterminal(this.name.value)
            }
    p.nameAs(r.getHead)
    p.resetWith(if(table != null) { table = null; r.getParser.reset() }) 
    p
  }
  
  def regular(sym: Nonterminal, parser: => MeerkatParser): MeerkatParser = {
    lazy val q = parser.rule(sym)
    var table: Array[Result[NonPackedNode]] = null
    val p = new MeerkatParser {
              def apply(input: Input, sppf: SPPFLookup, i: Int): Result[NonPackedNode]= {
                if(table == null) {
                  table = new Array(input.length + 1)
                }
                val result = table(i)
                if(result == null) {
                  table(i) = memo(q(input, sppf, i))
                  table(i)
                } else {
                  result
                }
                
              }
              
              override def symbol = sym
            }
    p.nameAs(sym.toString())
    p.resetWith(if(table != null) { table = null; parser.reset() }) 
    p
  }
}
