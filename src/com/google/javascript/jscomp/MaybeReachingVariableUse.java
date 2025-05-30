/*
 * Copyright 2009 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.javascript.jscomp;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.javascript.jscomp.ControlFlowGraph.Branch;
import com.google.javascript.jscomp.MaybeReachingVariableUse.ReachingUses;
import com.google.javascript.jscomp.graph.DiGraph.DiGraphEdge;
import com.google.javascript.jscomp.graph.GraphNode;
import com.google.javascript.jscomp.graph.LatticeElement;
import com.google.javascript.rhino.HamtPMap;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.PMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes "may be" reaching use for all definitions of each variable.
 *
 * <p>A use of {@code A} in {@code alert(A)} is a "may be" reaching use of the definition of {@code
 * A} at {@code A = foo()} if at least one path from the definition node to the end node reaches
 * that use and it is the last definition before the use on that path.
 *
 * <p>Example:
 *
 * <p><code>
 * D1: var A = foo();
 * U1: alert(A);
 *     if(....) {
 *        D2: A = bar();
 *        U2: alert(A);
 *     }
 * U3: alert(A);
 * </code>
 *
 * <p>Here, MaybeReachingUses[D1] = {U1, U3} and MaybeReachingUses[D2]={U2, U3}. The use U3 is not
 * guaranteed to use def D1: this is a "may-be" analysis.
 *
 * <p>This pass is a backwards-analysis pass, i.e. it traverses the CFG nodes bottom-up, `MAX_STEPS`
 * times or till a fixed point solution is reached. At each `cfgNode`, it:
 *
 * <ol>
 *   <li>1. Creates a new output lattice element to store the set of upward exposed variable uses at
 *       `cfgNode`.
 *   <li>2. Propagates the set of existing upwards exposed variable uses at `cfgNode` from the input
 *       lattice to output lattice
 *   <li>3. Adds new exposed uses of a variable to the upward exposed set in the output lattice
 *   <li>4. Removes killed(unconditionally redefined) variables from upward exposed set in the
 *       output lattice
 * </ol>
 */
class MaybeReachingVariableUse extends DataFlowAnalysis<Node, ReachingUses> {
  // The scope of the function that we are analyzing.
  private final Set<Var> escaped;
  private final Map<String, Var> allVarsInFn;

  MaybeReachingVariableUse(
      ControlFlowGraph<Node> cfg, Set<Var> escaped, Map<String, Var> allVarsInFn) {
    super(cfg);
    this.escaped = escaped;
    this.allVarsInFn = allVarsInFn;
  }

  /**
   * May use definition lattice representation. It captures a product lattice for each local
   * (non-escaped) variable. The sub-lattice is a n + 2 power set element lattice with all the Nodes
   * in the program, TOP and BOTTOM. This is better explained with an example:
   *
   * <p>Consider: A sub-lattice element representing the variable A represented by { N_4, N_5} where
   * N_x is a Node in the program. This implies at that particular point in the program the content
   * of A is "upward exposed" at point N_4 and N_5.
   *
   * <p>Example:
   *
   * <p><code>
   *
   * A = 1;
   * ...
   * N_3:
   * N_4: print(A);
   * N_5: y = A;
   * N_6: A = 1;
   * N_7: print(A);
   * </code>
   *
   * <p>At N_3, reads of A in {N_4, N_5} are said to be upward exposed.
   */
  static final class ReachingUses implements LatticeElement {
    // Maps variables to all their uses that are upward exposed at the current cfgNode.
    private PMap<Var, PMap<Node, Node>> mayUsePMap = HamtPMap.empty();

    public ReachingUses() {}

    /**
     * Copy constructor.
     *
     * @param other The constructed object is a replicated copy of this element.
     */
    public ReachingUses(ReachingUses other) {
      mayUsePMap = other.mayUsePMap;
    }

    public Iterable<Node> get(Var v) {
      PMap<Node, Node> uses = this.mayUsePMap.get(v);
      return uses != null ? uses.keys() : ImmutableList.of();
    }

    public void removeAll(Var v) {
      mayUsePMap = mayUsePMap.minus(v);
    }

    public void put(Var v, Node n) {
      var values = mayUsePMap.get(v);
      if (values == null) {
        values = HamtPMap.empty();
      }
      var newValues = values.plus(n, n);
      if (newValues != values) {
        mayUsePMap = mayUsePMap.plus(v, newValues);
      }
    }

    public void join(ReachingUses other) {
      mayUsePMap =
          mayUsePMap.reconcile(
              other.mayUsePMap,
              (Var var, PMap<Node, Node> thisVal, PMap<Node, Node> thatVal) -> {
                if (thisVal == null) {
                  return thatVal;
                }
                if (thatVal == null) {
                  return thisVal;
                }
                // The PMap as a set with the key and value pair have the same value.
                return thisVal.reconcile(thatVal, (node, unused1, unused2) -> node);
              });
    }

    @Override
    public boolean equals(Object other) {
      return (other instanceof ReachingUses reachingUses)
          && reachingUses.mayUsePMap.equivalent(this.mayUsePMap, ReachingUses::equalMaps);
    }

    private static boolean equalMaps(PMap<Node, Node> map1, PMap<Node, Node> map2) {
      return map1 == map2 || map1.equivalent(map2, ReachingUses::equalNodes);
    }

    private static boolean equalNodes(Node n1, Node n2) {
      return n1 == n2;
    }

    @Override
    public int hashCode() {
      throw new UnsupportedOperationException("the hashcode of this object is not stable");
    }
  }

  /**
   * The join is a simple union because of the "may be" nature of the analysis.
   *
   * <p>Consider: A = 1; if (x) { A = 2 }; alert(A);
   *
   * <p>The read of A "may be" exposed to A = 1 in the beginning.
   */
  private static class ReachingUsesJoinOp implements FlowJoiner<ReachingUses> {
    final ReachingUses result = new ReachingUses();

    @Override
    public void joinFlow(ReachingUses uses) {
      this.result.join(uses);
    }

    @Override
    public ReachingUses finish() {
      return result;
    }
  }

  @Override
  boolean isForward() {
    return false;
  }

  @Override
  ReachingUses createEntryLattice() {
    return new ReachingUses();
  }

  @Override
  ReachingUses createInitialEstimateLattice() {
    return new ReachingUses();
  }

  @Override
  FlowJoiner<ReachingUses> createFlowJoiner() {
    return new ReachingUsesJoinOp();
  }

  /**
   * Computes the new LatticeElement for a given node given its LatticeElement from previous
   * iteration.
   *
   * @param n node
   * @param input - Backward dataflow analyses compute their LatticeElement bottom-up (i.e.
   *     LinearFlowState.out to LinearFlowState.in). See {@link DataFlowAnalysis#flow(DiGraphNode)}.
   *     Here param `input` is the readonly input LinearFlowState.out that was constructed as
   *     `LinearFlowState.in` in the previous iteration, or the initial lattice element if this is
   *     the first iteration.
   */
  @Override
  ReachingUses flowThrough(Node n, ReachingUses input) {
    ReachingUses output = new ReachingUses(input);

    // If there's an ON_EX edge, this cfgNode may or may not get executed.
    // We can express this concisely by just pretending this happens in
    // a conditional.
    boolean conditional = hasExceptionHandler(n);
    computeMayUse(n, n, output, conditional);

    return output;
  }

  private boolean hasExceptionHandler(Node cfgNode) {
    List<? extends DiGraphEdge<Node, Branch>> branchEdges = getCfg().getOutEdges(cfgNode);
    for (DiGraphEdge<Node, Branch> edge : branchEdges) {
      if (edge.getValue() == Branch.ON_EX) {
        return true;
      }
    }
    return false;
  }

  /**
   * Given a cfgNode, updates the output LatticeElement at that node by finding and storing all
   * variables and their uses that are upward exposed at the cfgNode.
   *
   * @param n The explorer node which searches for variables
   * @param cfgNode The CFG node for which the upward exposed variables are being searched.
   * @param conditional Whether {@code n} is only conditionally evaluated given that {@code cfgNode}
   *     is evaluated. Do not remove conditionally redefined variables from the reaching uses set.
   */
  private void computeMayUse(Node n, Node cfgNode, ReachingUses output, boolean conditional) {
    switch (n.getToken()) {
      case BLOCK:
      case ROOT:
      case FUNCTION:
        return;

      case NAME:
        if (NodeUtil.isLhsByDestructuring(n)) {
          if (!conditional) {
            removeFromUseIfLocal(n.getString(), output);
          }
        } else {
          addToUseIfLocal(n.getString(), cfgNode, output);
        }
        return;

      case WHILE:
      case DO:
      case IF:
      case FOR:
        Node condExpr = NodeUtil.getConditionExpression(n);
        computeMayUse(condExpr, cfgNode, output, conditional);
        return;

      case FOR_IN:
      case FOR_OF:
      case FOR_AWAIT_OF:
        // for(x in y) {...}
        Node lhs = n.getFirstChild();
        Node rhs = lhs.getNext();
        if (NodeUtil.isNameDeclaration(lhs)) {
          lhs = lhs.getLastChild(); // for(var x in y) {...}
          if (lhs.isDestructuringLhs()) {
            lhs = lhs.getFirstChild(); // for (let [x] of obj) {...}
          }
        }
        if (lhs.isName() && !conditional) {
          removeFromUseIfLocal(lhs.getString(), output);
        } else if (lhs.isDestructuringPattern()) {
          computeMayUse(lhs, cfgNode, output, true);
        }
        computeMayUse(rhs, cfgNode, output, conditional);
        return;

      case AND:
      case OR:
      case COALESCE:
      case OPTCHAIN_GETPROP:
      case OPTCHAIN_GETELEM:
        computeMayUse(n.getLastChild(), cfgNode, output, /* conditional= */ true);
        computeMayUse(n.getFirstChild(), cfgNode, output, conditional);
        return;

      case OPTCHAIN_CALL:
        // As args are evaluated in AST order, we traverse in reverse AST order for backward
        // dataflow analysis.
        for (Node c = n.getLastChild(); c != n.getFirstChild(); c = c.getPrevious()) {
          computeMayUse(c, cfgNode, output, /* conditional= */ true);
        }
        computeMayUse(n.getFirstChild(), cfgNode, output, conditional);
        return;

      case HOOK:
        computeMayUse(n.getLastChild(), cfgNode, output, /* conditional= */ true);
        computeMayUse(n.getSecondChild(), cfgNode, output, /* conditional= */ true);
        computeMayUse(n.getFirstChild(), cfgNode, output, conditional);
        return;

      case VAR:
      case LET:
      case CONST:
        Node varName = n.getFirstChild();
        checkState(n.hasChildren(), "AST should be normalized (%s)", n);

        if (varName.isDestructuringLhs()) {
          // Note: since destructuring is evaluated in reverse AST order, we traverse the first
          // child before the second in order to do our backwards data flow analysis.
          computeMayUse(varName.getFirstChild(), cfgNode, output, conditional);
          computeMayUse(varName.getSecondChild(), cfgNode, output, conditional);
        } else if (varName.hasChildren()) {
          computeMayUse(varName.getFirstChild(), cfgNode, output, conditional);
          if (!conditional) {
            removeFromUseIfLocal(varName.getString(), output);
          }
        } // else var name declaration with no initial value
        return;

      case DEFAULT_VALUE:
        if (n.getFirstChild().isDestructuringPattern()) {
          computeMayUse(n.getFirstChild(), cfgNode, output, conditional);
          computeMayUse(n.getSecondChild(), cfgNode, output, true);
        } else if (n.getFirstChild().isName()) {
          // assigning to the name occurs after evaluating the default value
          if (!conditional) {
            removeFromUseIfLocal(n.getFirstChild().getString(), output);
          }
          computeMayUse(n.getSecondChild(), cfgNode, output, true);
        } else {
          computeMayUse(n.getSecondChild(), cfgNode, output, true);
          computeMayUse(n.getFirstChild(), cfgNode, output, conditional);
        }
        break;

      default:
        if (NodeUtil.isAssignmentOp(n) && n.getFirstChild().isName()) {
          checkState(!NodeUtil.isLogicalAssignmentOp(n));
          Node name = n.getFirstChild();
          if (!conditional) {
            removeFromUseIfLocal(name.getString(), output);
          }

          // In case of a += "Hello". There is a read of a.
          if (!n.isAssign()) {
            addToUseIfLocal(name.getString(), cfgNode, output);
          }

          computeMayUse(name.getNext(), cfgNode, output, conditional);
        } else if (n.isAssign() && n.getFirstChild().isDestructuringPattern()) {
          // Note: the rhs of destructuring is evaluated before the lhs
          computeMayUse(n.getFirstChild(), cfgNode, output, conditional);
          computeMayUse(n.getSecondChild(), cfgNode, output, conditional);
        } else {
          /*
           * We want to traverse in reverse order because we want the LAST
           * definition in the sub-tree.
           */
          for (Node c = n.getLastChild(); c != null; c = c.getPrevious()) {
            computeMayUse(c, cfgNode, output, conditional);
          }
        }
    }
  }

  /**
   * Sets the variable for the given name to the node value in the upward exposed lattice. Do
   * nothing if the variable name is one of the escaped variable.
   */
  private void addToUseIfLocal(String name, Node node, ReachingUses use) {
    Var var = allVarsInFn.get(name);
    if (var == null) {
      return;
    }
    if (!escaped.contains(var)) {
      use.put(var, node);
    }
  }

  /**
   * Removes the variable for the given name from the node value in the upward exposed lattice. Do
   * nothing if the variable name is one of the escaped variable.
   */
  private void removeFromUseIfLocal(String name, ReachingUses use) {
    Var var = allVarsInFn.get(name);
    if (var == null) {
      return;
    }
    if (!escaped.contains(var)) {
      use.removeAll(var);
    }
  }

  /**
   * Gets a list of nodes that may be using the value assigned to {@code name} in {@code defNode}.
   * {@code defNode} must be one of the control flow graph nodes.
   *
   * @param name name of the variable. It can only be names of local variable that are not function
   *     parameters, escaped variables or variables declared in catch.
   * @param defNode the control flow graph node that may assign a value to {@code name}
   * @return the list of upward exposed uses of the variable {@code name} at defNode.
   */
  Iterable<Node> getUses(String name, Node defNode) {
    GraphNode<Node, Branch> n = getCfg().getNode(defNode);
    checkNotNull(n);
    LinearFlowState<ReachingUses> state = n.getAnnotation();
    return state.getOut().get(allVarsInFn.get(name));
  }
}
