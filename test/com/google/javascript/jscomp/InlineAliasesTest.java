/*
 * Copyright 2015 The Closure Compiler Authors.
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

import static com.google.javascript.jscomp.InlineAndCollapseProperties.ALIAS_CYCLE;

import com.google.javascript.jscomp.CompilerOptions.PropertyCollapseLevel;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link InlineAndCollapseProperties.InlineAliases}. */
@RunWith(JUnit4.class)
public class InlineAliasesTest extends CompilerTestCase {

  private static PassFactory makePassFactory(
      String name, Function<AbstractCompiler, CompilerPass> pass) {
    return PassFactory.builder().setName(name).setInternalFactory(pass).build();
  }

  @Override
  protected CompilerPass getProcessor(final Compiler compiler) {
    PhaseOptimizer optimizer = new PhaseOptimizer(compiler, null);
    optimizer.addOneTimePass(makePassFactory("es6NormalizeClasses", Es6NormalizeClasses::new));
    optimizer.addOneTimePass(
        makePassFactory(
            "inlineAndCollapseProperties",
            (comp) ->
                InlineAndCollapseProperties.builder(compiler)
                    .setPropertyCollapseLevel(PropertyCollapseLevel.NONE)
                    .build()));
    return optimizer;
  }

  @Before
  public void customSetUp() throws Exception {
    setGenericNameReplacements(Es6NormalizeClasses.GENERIC_NAME_REPLACEMENTS);
  }

  @Override
  protected CompilerOptions getOptions() {
    CompilerOptions options = super.getOptions();
    enableTypeInfoValidation();
    enableTypeCheck();
    return options;
  }

  /**
   * Returns the number of times the pass should be run before results are verified.
   *
   * <p>This pass is not idempotent so we only run it once.
   */
  @Test
  public void testSimpleAliasInJSDoc_isUnchanged() {
    testSame("/** @constructor */ function Foo(){} const alias = Foo; /** @type {alias} */ var x;");

    testSame(
        """
        var ns={};
        /** @constructor */ function Foo(){};
        /** @const */ ns.alias = Foo;
        /** @type {ns.alias} */ var x;
        """);

    testSame(
        """
        /** @const */
        var ns={};
        /** @constructor */ function Foo(){};
        Foo.Subfoo = class {};
        /** @const */ ns.alias = Foo;
        /** @type {ns.alias.Subfoo} */ var x;
        """);
  }

  @Test
  public void testSimpleAliasInCode() {
    test(
        """
        /** @constructor */ function Foo(){};
        Foo.Subfoo = class {};
        var /** @const */ alias = Foo; var x = new alias;
        """,
        """
        /** @constructor */ function Foo(){};
        Foo.Subfoo = class {};var /** @const */ alias = Foo; var x = new Foo;
        """);

    test(
        """
        var ns={}; /** @constructor */ function Foo(){};
        Foo.Subfoo = class {};
        /** @const */ ns.alias = Foo; var x = new ns.alias;
        """,
        """
        var ns={}; /** @constructor */ function Foo(){};
        Foo.Subfoo = class {};
        /** @const */ ns.alias = Foo; var x = new Foo;
        """);

    test(
        """
        var ns={}; /** @constructor */ function Foo(){};
        Foo.Subfoo = class {};
        /** @const */ ns.alias = Foo; var x = new ns.alias.Subfoo;
        """,
        """
        var ns={}; /** @constructor */ function Foo(){};
        Foo.Subfoo = class {};
        /** @const */ ns.alias = Foo; var x = new Foo.Subfoo;
        """);
  }

  @Test
  public void testAliasQualifiedName() {
    testSame(
        """
        /** @const */
        var ns = {};
        ns.Foo = function(){};
        ns.Foo.Subfoo = class {};
        /** @const */ ns.alias = ns.Foo;
        /** @type {ns.alias.Subfoo} */ var x;
        """);

    test(
        """
        var ns = {};
        ns.Foo = function(){};
        ns.Foo.Subfoo = class {};
        /** @const */ ns.alias = ns.Foo;
        var x = new ns.alias.Subfoo;
        """,
        """
        var ns = {};
        ns.Foo = function(){};
        ns.Foo.Subfoo = class {};
        /** @const */ ns.alias = ns.Foo;
        var x = new ns.Foo.Subfoo;
        """);
  }

  @Test
  public void testHoistedAliasesInCode() {
    // Unqualified
    test(
        """
        function Foo(){};
        function Bar(){ var x = alias; };
        var /** @const */ alias = Foo;
        """,
        """
        function Foo(){};
        function Bar(){ var x = Foo; };
        var /** @const */ alias = Foo;
        """);

    // Qualified
    test(
        """
        var ns = {};
        ns.Foo = function(){};
        function Bar(){ var x = ns.alias; };
        /** @const */ ns.alias = ns.Foo;
        """,
        """
        var ns = {};
        ns.Foo = function(){};
        function Bar(){ var x = ns.Foo; };
        /** @const */ ns.alias = ns.Foo;
        """);
  }

  @Test
  public void testAliasCycleError() {
    testError(
        """
        /** @const */ var x = y;
        /** @const */ var y = x;
        """,
        ALIAS_CYCLE);
  }

  @Test
  public void testTransitiveAliases() {
    test(
        """
        /** @const */ var ns = {};
        /** @constructor */ ns.Foo = function() {};
        /** @constructor */ ns.Foo.Bar = function() {};
        var /** @const */ alias = ns.Foo;
        var /** @const */ alias2 = alias.Bar;
        var x = new alias2
        """,
        """
        /** @const */ var ns = {};
        /** @constructor */ ns.Foo = function() {};
        /** @constructor */ ns.Foo.Bar = function() {};
        var /** @const */ alias = ns.Foo;
        var /** @const */ alias2 = ns.Foo.Bar;
        // Note: in order to replace "alias2" with "ns.Foo.Bar", we would either have to do
        // multiple traversals of the AST in InlineAliases, or mark alias2 as an alias of
        // ns.Foo.Bar in the GlobalNamespace after replacing "alias2 = alias.Bar" with
        // "alias2 = ns.Foo.Bar"
        var x = new alias2;
        """);
  }

  @Test
  public void testAliasChains() {
    // Unqualified
    test(
        """
        /** @constructor */ var Foo = function() {};
        var /** @const */ alias1 = Foo;
        var /** @const */ alias2 = alias1;
        var x = new alias2
        """,
        """
        /** @constructor */ var Foo = function() {};
        var /** @const */ alias1 = Foo;
        var /** @const */ alias2 = Foo;
        var x = new Foo;
        """);

    // Qualified
    test(
        """
        /** @const */ var ns = {};
        /** @constructor */ ns.Foo = function() {};
        var /** @const */ alias1 = ns.Foo;
        var /** @const */ alias2 = alias1;
        var x = new alias2
        """,
        """
        /** @const */ var ns = {};
        /** @constructor */ ns.Foo = function() {};
        var /** @const */ alias1 = ns.Foo;
        var /** @const */ alias2 = ns.Foo;
        var x = new ns.Foo;
        """);
  }

  @Test
  public void testAliasedEnums() {
    test(
        "/** @enum {number} */ var E = { A : 1 }; var /** @const */ alias = E.A; alias;",
        "/** @enum {number} */ var E = { A : 1 }; var /** @const */ alias = E.A; E.A;");
  }

  @Test
  public void testIncorrectConstAnnotationDoesntCrash() {
    testSame("var x = 0; var /** @const */ alias = x; alias = 5; use(alias);");
    testSame("var x = 0; var ns={}; /** @const */ ns.alias = x; ns.alias = 5; use(ns.alias);");
  }

  @Test
  public void testRedefinedAliasesNotRenamed() {
    testSame("var x = 0; var /** @const */ alias = x; x = 5; use(alias);");
  }

  @Test
  public void testDefinesAreNotInlined() {
    testSame("var ns = {Foo: 0}; var /** @define {number} */ alias = ns.Foo; use(alias);");
  }

  @Test
  public void testConstWithTypesAreNotInlined() {
    testSame(
        """
        var /** @type {number} */ n = 5
        var /** @const {number} */ alias = n;
        var x = use(alias)
        """);
  }

  @Test
  public void testShadowedAliasesNotRenamed() {
    testSame(
        """
        var ns = {};
        ns.Foo = function(){};
        var /** @const */ alias = ns.Foo;
        function f(alias) {
          var x = alias
        }
        """);

    testSame(
        """
        var ns = {};
        ns.Foo = function(){};
        var /** @const */ alias = ns.Foo;
        function f() {
          var /** @const */ alias = 5;
          var x = alias
        }
        """);

    testSame(
        """
        /** @const */
        var x = y;
        function f() {
          var x = 123;
          function g() {
            return x;
          }
        }
        """);
  }

  @Test
  public void testShadowedAliasesNotRenamed_withBlockScope() {
    testSame(
        """
        var ns = {};
        ns.Foo = function(){};
        var /** @const */ alias = ns.Foo;
        if (true) {
          const alias = 5;
          var x = alias
        }
        """);

    testSame(
        """
        /** @const */
        var x = y;
        if (true) {
          const x = 123;
          function g() {
            return x;
          }
        }
        """);
  }

  @Test
  public void testES6VarAliasClassDeclarationWithNew() {
    test(
        "class Foo{}; var /** @const */ alias = Foo; var x = new alias;",
        "class Foo{}; var /** @const */ alias = Foo; var x = new Foo;");
  }

  @Test
  public void testES6VarAliasClassDeclarationWithoutNew() {
    test(
        "class Foo{}; var /** @const */ alias = Foo; var x = alias;",
        "class Foo{}; var /** @const */ alias = Foo; var x = Foo;");
  }

  @Test
  public void testNoInlineAliasesInsideClassConstructor() {
    testSame(
        """
        class Foo {
         constructor(x) {
             /** @const */
             this.x = class {};
             var /** @const */ alias1 = this.x;
             var /** @const */ alias2 = alias1;
             var z = new alias2;
         }
        }
        """);
  }

  @Test
  public void testArrayDestructuringVarAssign() {
    test(
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        var a = [5, A];
        var [one, two] = a;
        """,
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        var a = [5, Foo];
        var [one, two] = a;
        """);
  }

  @Test
  public void testArrayDestructuringFromFunction() {
    test(
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        function f() {
          return [A, 3];
        }
        var a, b;
        [a, b] = f();
        """,
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        function f() {
          return [Foo, 3];
        }
        var a, b;
        [a, b] = f();
        """);
  }

  @Test
  public void testArrayDestructuringSwapIsNotInlined() {
    testSame(
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        var temp = 3;
        [A, temp] = [temp, A];
        """);
  }

  @Test
  public void testArrayDestructuringSwapIsNotInlinedWithClassDeclaration() {
    testSame(
        """
        class Foo {};
        var /** @const */ A = Foo;
        var temp = 3;
        [A, temp] = [temp, A];
        """);
  }

  @Test
  public void testArrayDestructuringAndRedefinedAliasesNotRenamed() {
    testSame("var x = 0; var /** @const */ alias = x; [x] = [5]; use(alias);");
  }

  @Test
  public void testArrayDestructuringTwoVarsAndRedefinedAliasesNotRenamed() {
    testSame(
        """
        var x = 0;
        var /** @const */ alias = x;
        var y = 5;
        [x] = [y];
        use(alias);
        """);
  }

  @Test
  public void testObjectDestructuringBasicAssign() {
    test(
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        var o = {p: A, q: 5};
        var {p, q} = o;
        """,
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        var o = {p: Foo, q: 5};
        var {p, q} = o;
        """);
  }

  @Test
  public void testObjectDestructuringAssignWithoutDeclaration() {
    test(
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        ({a, b} = {a: A, b: A});
        """,
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        ({a, b} = {a: Foo, b: Foo});
        """);
  }

  @Test
  public void testObjectDestructuringAssignNewVarNames() {
    test(
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        var o = {p: A, q: true};
        var {p: newName1, q: newName2} = o;
        """,
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        var o = {p: Foo, q: true};
        var {p: newName1, q: newName2} = o;
        """);
  }

  @Test
  public void testObjectDestructuringDefaultVals() {
    ignoreWarnings(DiagnosticGroups.MISSING_PROPERTIES);
    test(
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        var {a = A, b = A} = {a: 13};
        """,
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        var {a = Foo, b = Foo} = {a: 13};
        """);
  }

  @Test
  public void testArrayDestructuringWithParameter() {
    test(
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        function f([name, val]) {
           alert(name, val);
        }
        f([A, A]);
        """,
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        function f([name, val]) {
           alert(name, val);
        }
        f([Foo, Foo]);
        """);
  }

  @Test
  public void testObjectDestructuringWithParameters() {
    test(
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        function g({
           name: n,
           val: v
        }) {
           alert(n, v);
        }
        g({
           name: A,
           val: A
        });
        """,
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        function g({
           name: n,
           val: v
        }) {
           alert(n, v);
        }
        g({
           name: Foo,
           val: Foo
        });
        """);
  }

  @Test
  public void testObjectDestructuringWithParametersAndStyleShortcut() {
    test(
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        function h({
           name,
           val
        }) {
           alert(name, val);
        }
        h({name: A, val: A});
        """,
        """
        var Foo = class {};
        var /** @const */ A = Foo;
        function h({
           name,
           val
        }) {
           alert(name, val);
        }
        h({name: Foo, val: Foo});
        """);
  }

  @Test
  public void testSimpleConstAliasInCode() {
    test(
        "/** @constructor */ function Foo(){}; const alias = Foo; var x = new alias;",
        "/** @constructor */ function Foo(){}; const alias = Foo; var x = new Foo;");
  }

  @Test
  public void testSimpleLetAliasInCode() {
    test(
        "/** @constructor */ function Foo(){}; let /** @const */ alias = Foo; var x = new alias;",
        "/** @constructor */ function Foo(){}; let /** @const */ alias = Foo; var x = new Foo;");
  }

  @Test
  public void testClassExtendsAlias1() {
    test(
        "class Foo{} const alias = Foo; class Bar extends alias {}",
        "class Foo {} const alias = Foo; class Bar extends Foo {}");
  }

  @Test
  public void testClassExtendsAlias2() {
    test(
        "var ns = {}; ns.Foo = class {}; const alias = ns.Foo; class Bar extends alias {}",
        "var ns = {}; ns.Foo = class {}; const alias = ns.Foo; class Bar extends ns.Foo {}");
  }

  @Test
  public void testBlockScopedAlias() {
    testSame("function Foo() {} if (true) { const alias = Foo; alias; }");
  }

  @Test
  public void testVarAliasDeclaredInBlockScope() {
    testSame("function Foo() {} { var /** @const */ alias = Foo; alias; }");
  }

  @Test
  public void testDontInlineEscapedQnameProperty() {
    testSame(
        externs("function use(obj) {}"),
        srcs(
            """
            /** @const */
            var ns = {};
            ns.foo = 3;
            const alias = ns.foo;
            use(ns);
            // "ns" escapes and we don't know if the value of "ns.foo" has also changed, so
            // we cannot replace "alias" with "ns.foo".
            alert(alias);
            """));
  }

  @Test
  public void testDoInlineEscapedConstructorProperty() {
    // TODO(b/80429954): this is unsafe. The call to use(Foobar) could have changed the value of
    // Foobar.foo
    test(
        externs("function use(obj) {}"),
        srcs(
            """
            /** @constructor */
            function Foobar() {}
            Foobar.foo = 3;
            const alias = Foobar.foo;
            use(Foobar);
            alert(alias);
            """),
        expected(
            """
            /** @constructor */
            function Foobar() {}
            Foobar.foo = 3;
            const alias = Foobar.foo;
            use(Foobar);
            alert(Foobar.foo);
            """));
  }

  @Test
  public void testForwardedExport() {
    testSame(
        """
        const proto = {};
        /** @const */
        proto.google = {};
        /** @const */
        proto.google.type = {};
        proto.google.type.Date = class {};
        const alias = proto;
        function f() {
          const d = new alias.google.type.Date();
          const proto = 0;
        }
        """);
  }

  @Test
  public void testForwardedExportNested() {
    testSame(
        """
        const proto = {};
        /** @const */
        proto.google = {};
        /** @const */
        proto.google.type = {};
        proto.google.type.Date = class {};
        const alias = proto.google;
        function f() {
          const d = new alias.type.Date();
          const proto = 0;
        }
        """);
  }

  @Test
  public void testQualifiedNameSetViaUnaryDecrementNotInlined() {
    testSame(
        """
        const a = {b: 0, c: 0};
        const v1 = a.b;
        a.b--;
        const v2 = a.b;
        a.b--;
        use(v1 + v2);
        """);
  }

  @Test
  public void testQualifiedNameSetViaUnaryIncrementNotInlined() {
    testSame(
        """
        const a = {b: 0};
        const v1 = a.b;
        a.b++;
        const v2 = a.b;
        a.b++;
        use(v1 + v2);
        """);
  }

  @Test
  public void testAliasOfStubDeclaration() {
    test(
        """
        const a = {};
        var stubDeclaration;
        /** @const */
        a.b = stubDeclaration;
        alert(a.b);
        """,
        """
        const a = {};
        var stubDeclaration;
        /** @const */
        a.b = stubDeclaration;
        alert(stubDeclaration);
        """);
  }
}
