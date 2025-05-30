/*
 * Copyright 2016 The Closure Compiler Authors.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.javascript.jscomp.AstFactory.type;

import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.ExpressionDecomposer.DecompositionType;
import com.google.javascript.jscomp.colors.StandardColors;
import com.google.javascript.jscomp.deps.ModuleNames;
import com.google.javascript.jscomp.parsing.parser.FeatureSet.Feature;
import com.google.javascript.rhino.IR;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.Node;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Extracts ES6 classes defined in function calls to local constants.
 *
 * <p>Example: Before:
 *
 * <pre>
 * <code>
 *   foo(class { constructor() {} });
 * </code>
 * </pre>
 *
 * After:
 *
 * <pre>
 * <code>
 *   const $jscomp$classdecl$var0 = class { constructor() {} };
 *   foo($jscomp$classdecl$var0);
 * </code>
 * </pre>
 *
 * <p>This must be done before {@link Es6RewriteClass}, because that pass only handles classes that
 * are declarations or simple assignments.
 *
 * @see Es6RewriteClass#visitClass(NodeTraversal, Node, Node)
 */
public final class Es6ExtractClasses extends NodeTraversal.AbstractPostOrderCallback
    implements CompilerPass {

  static final String CLASS_DECL_VAR = "$classdecl$var";

  private final AbstractCompiler compiler;
  private final AstFactory astFactory;
  private final ExpressionDecomposer expressionDecomposer;
  private int classDeclVarCounter = 0;

  Es6ExtractClasses(AbstractCompiler compiler) {
    this.compiler = compiler;
    this.astFactory = compiler.createAstFactory();
    this.expressionDecomposer = compiler.createDefaultExpressionDecomposer();
  }

  @Override
  public void process(Node externs, Node root) {
    NodeTraversal.traverseRoots(compiler, this, externs, root);
    NodeTraversal.traverseRoots(compiler, new SelfReferenceRewriter(), externs, root);
  }

  @Override
  public void visit(NodeTraversal t, Node n, Node parent) {
    if (n.isClass() && shouldExtractClass(n)) {
      extractClass(t, n);
    }
  }

  private class SelfReferenceRewriter implements NodeTraversal.Callback {
    private static class ClassDescription {
      final Node nameNode;
      final String outerName;

      ClassDescription(Node nameNode, String outerName) {
        this.nameNode = nameNode;
        this.outerName = outerName;
      }
    }

    private final Deque<ClassDescription> classStack = new ArrayDeque<>();

    private boolean needsInnerNameRewriting(Node classNode, Node parent) {
      checkArgument(classNode.isClass());
      return classNode.getFirstChild().isName() && parent.isName();
    }

    @Override
    public boolean shouldTraverse(NodeTraversal t, Node n, Node parent) {
      if (n.isClass() && needsInnerNameRewriting(n, parent)) {
        classStack.addFirst(new ClassDescription(n.getFirstChild(), parent.getString()));
      }
      return true;
    }

    @Override
    public void visit(NodeTraversal t, Node n, Node parent) {
      switch (n.getToken()) {
        case CLASS:
          if (needsInnerNameRewriting(n, parent)) {
            classStack.removeFirst();
            n.getFirstChild().replaceWith(IR.empty().srcref(n.getFirstChild()));
            compiler.reportChangeToEnclosingScope(n);
          }
          break;
        case NAME:
          maybeUpdateClassSelfRef(t, n);
          break;
        default:
          break;
      }
    }

    private void maybeUpdateClassSelfRef(NodeTraversal t, Node nameNode) {
      for (ClassDescription klass : classStack) {
        if (nameNode != klass.nameNode && nameNode.matchesQualifiedName(klass.nameNode)) {
          Var var = t.getScope().getVar(nameNode.getString());
          if (var != null && var.getNameNode() == klass.nameNode) {
            Node newNameNode =
                astFactory.createName(klass.outerName, type(nameNode)).srcref(nameNode);
            checkState(klass.outerName.contains(CLASS_DECL_VAR), klass.outerName);
            // Explicitly mark the usage node as constant as the declaration is marked constant
            // when this pass runs post normalization because of b/322009741
            newNameNode.putBooleanProp(Node.IS_CONSTANT_NAME, true);
            nameNode.replaceWith(newNameNode);
            compiler.reportChangeToEnclosingScope(newNameNode);
            return;
          }
        }
      }
    }
  }

  private boolean shouldExtractClass(Node classNode) {
    Node parent = classNode.getParent();
    boolean isAnonymous = classNode.getFirstChild().isEmpty();
    if (NodeUtil.isClassDeclaration(classNode)
        || (isAnonymous && parent.isName())
        || (isAnonymous
            && parent.isAssign()
            && parent.getFirstChild().isQualifiedName()
            && parent.getParent().isExprResult())) {
      // No need to extract. Handled directly by Es6ToEs3Converter.ClassDeclarationMetadata#create.
      return false;
    }

    return true;
  }

  /**
   * Wraps any class definition into an IIFE.
   *
   * <p>Example:
   *
   * <pre>{@code
   * function foo(x = class A {}) {}
   *
   * }</pre>
   *
   * turns into:
   *
   * <pre>{@code
   * function foo(x = (() => { return class A {};})()) {}
   *
   * }</pre>
   */
  private void wrapClassDefInsideIife(Node n) {
    Preconditions.checkState(n.isClass());

    final Node parent = n.getParent();
    // We track the previous sibling of the class node to determine where to insert the IIFE.
    final Node previous = n.getPrevious();

    Node returnBlock = astFactory.createBlock(astFactory.createReturn(n.detach())).srcref(n);
    Node arrowFn = IR.arrowFunction(IR.name(""), IR.paramList(), returnBlock).srcref(n);
    arrowFn.setColor(StandardColors.UNKNOWN);
    Node iife = astFactory.createCallWithUnknownType(arrowFn).srcrefTreeIfMissing(n);
    if (previous != null) {
      iife.insertAfter(previous);
    } else {
      // `n` was either the first or only child of its parent. Insert at the front.
      parent.addChildToFront(iife);
    }
    NodeUtil.addFeatureToScript(NodeUtil.getEnclosingScript(n), Feature.ARROW_FUNCTIONS, compiler);
    compiler.reportChangeToEnclosingScope(iife);
  }

  private void extractClass(NodeTraversal t, Node classNode) {
    if (expressionDecomposer.canExposeExpression(classNode) != DecompositionType.MOVABLE) {
      // When class is not movable, we wrap it inside an IIFE. We have observed unsafe circumstances
      // where decomposing causes issues so this is safer. See b/417772606.
      wrapClassDefInsideIife(classNode);
    }
    Node parent = classNode.getParent();

    String name = ModuleNames.fileToJsIdentifier(classNode.getStaticSourceFile().getName())
        + CLASS_DECL_VAR
        + (classDeclVarCounter++);
    JSDocInfo info = NodeUtil.getBestJSDocInfo(classNode);

    Node statement = NodeUtil.getEnclosingStatement(parent);
    // class name node used as LHS in newly created assignment
    Node classNameLhs = astFactory.createConstantName(name, type(classNode));
    // class name node that replaces the class literal in the original statement
    Node classNameRhs = classNameLhs.cloneTree();
    classNode.replaceWith(classNameRhs);
    Node classDeclaration = IR.constNode(classNameLhs, classNode).srcrefTreeIfMissing(classNode);
    NodeUtil.addFeatureToScript(t.getCurrentScript(), Feature.CONST_DECLARATIONS, compiler);
    classDeclaration.setJSDocInfo(JSDocInfo.Builder.maybeCopyFrom(info).build());
    classDeclaration.insertBefore(statement);

    // If the original statement was a variable declaration or qualified name assignment like
    // like these:
    // var ClassName = class {...
    // OR
    // some.qname.ClassName = class {...
    //
    // We will have changed the original statement to
    //
    // var ClassName = generatedName;
    // OR
    // some.qname.ClassName = generatedName;
    //
    // This is creating a type alias for a class, but since there's no literal class on the RHS,
    // it doesn't look like one. Add at-constructor JSDoc to make it clear that this is happening.
    //
    // This was added to fix a specific problem where the original definition was for an abstract
    // class, so its JSDoc included at-abstract.
    // This caused ClosureCodeRemoval to think this rewritten assignment was a removable abstract
    // method definition instead of the definition of an abstract class.
    //
    // TODO(b/117292942): Make ClosureCodeRemoval smarter so this hack isn't necessary to
    // prevent incorrect removal of assignments.
    if (NodeUtil.isNameDeclaration(statement)
        && statement.hasOneChild()
        && statement.getOnlyChild() == parent) {
      // var ClassName = generatedName;
      addAtConstructor(statement);
    } else if (statement.isExprResult()) {
      Node expr = statement.getOnlyChild();
      if (expr.isAssign()
          && expr.getFirstChild().isQualifiedName()
          && expr.getSecondChild() == classNameRhs) {
        // some.qname.ClassName = generatedName;
        addAtConstructor(expr);
      }
    }
    compiler.reportChangeToEnclosingScope(classDeclaration);
  }

  /** Add at-constructor to the JSDoc of the given node. */
  private void addAtConstructor(Node node) {
    JSDocInfo.Builder builder = JSDocInfo.Builder.maybeCopyFrom(node.getJSDocInfo());
    builder.recordConstructor();
    node.setJSDocInfo(builder.build());
  }
}
