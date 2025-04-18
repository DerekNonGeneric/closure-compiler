/*
 * Copyright 2008 The Closure Compiler Authors.
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

import com.google.javascript.jscomp.testing.JSChunkGraphBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link CrossChunkMethodMotion}. */
@RunWith(JUnit4.class)
public final class CrossChunkMethodMotionTest extends CompilerTestCase {

  private boolean canMoveExterns = false;
  private boolean noStubs = false;
  private static final String STUB_DECLARATIONS = CrossChunkMethodMotion.STUB_DECLARATIONS;

  @Override
  protected CompilerPass getProcessor(Compiler compiler) {
    return new CrossChunkMethodMotion(compiler, new IdGenerator(), canMoveExterns, noStubs);
  }

  @Override
  protected CompilerOptions getOptions() {
    CompilerOptions options = super.getOptions();
    // pretty printing makes it much easier to read the failure messages.
    options.setPrettyPrint(true);
    return options;
  }

  @Before
  public void customSetUp() throws Exception {
    canMoveExterns = false;
    noStubs = false;
    enableNormalize();
  }

  @Test
  public void moveMethodAssignedToPrototype() {
    testSame(
        // bar property is defined in externs, so it cannot be moved
        externs("IFoo.prototype.bar;"),
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.bar = function() {};
                    """)
                .addChunk("(new Foo).bar()")
                .build()));

    canMoveExterns = true;
    test(
        externs("IFoo.prototype.bar;"),
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.bar = function() {};
                    """)
                .addChunk("(new Foo).bar()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype.bar = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.bar = JSCompiler_unstubMethod(0, function() {});
            (new Foo).bar()
            """));
  }

  @Test
  public void moveMethodDefinedInPrototypeLiteralWithStubs() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype = { method: function() {} };
                    """)
                .addChunk("(new Foo).method()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype = { method: JSCompiler_stubMethod(0) };
                """,
            // Chunk 2
            """
            Foo.prototype.method =
                JSCompiler_unstubMethod(0, function() {});
            (new Foo).method()
            """));
  }

  @Test
  public void moveMethodDefinedInPrototypeLiteralWithoutStubs() {
    noStubs = true;
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype = { method: function() {} };
                    """)
                .addChunk("(new Foo).method()")
                .build()),
        expected(
            """
            function Foo() {}
            Foo.prototype = {};
            """,
            // Chunk 2
            """
            Foo.prototype.method = function() {};
            (new Foo).method()
            """));
  }

  @Test
  public void moveMethodDefinedInPrototypeLiteralUsingShorthandSyntaxWithStub() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype = { method() {} };
                    """)
                .addChunk("(new Foo).method()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype = { method: JSCompiler_stubMethod(0) };
                """,
            // Chunk 2
            """
            Foo.prototype.method =
                JSCompiler_unstubMethod(0, function() {});
            (new Foo).method()
            """));
  }

  @Test
  public void moveMethodDefinedInPrototypeLiteralUsingShorthandSyntaxWithoutStub() {
    noStubs = true;
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype = { method() {} };
                    """)
                .addChunk("(new Foo).method()")
                .build()),
        expected(
            """
            function Foo() {}
            Foo.prototype = {};
            """,
            // Chunk 2
            """
            Foo.prototype.method = function() {};
            (new Foo).method()
            """));
  }

  @Test
  public void doNotMoveMethodDefinedInPrototypeLiteralContainingSuper() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype = { method() { return super.toString(); } };
                    """)
                .addChunk("(new Foo).method()")
                .build()));
  }

  @Test
  public void doNotMoveMethodDefinedInPrototypeLiteralAsComputedProp() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype = { [1]:  {} };
                    """)
                .addChunk("(new Foo)[1]()")
                .build()));
  }

  @Test
  public void moveClassMethod() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk("class Foo { method() {} }")
                .addChunk("(new Foo).method()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.method = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.method = JSCompiler_unstubMethod(0, function() {});
            (new Foo).method();
            """));

    // Same as above, but reference to the method is via an optional chain
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk("class Foo { method() {} }")
                .addChunk("(new Foo)?.method()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.method = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.method = JSCompiler_unstubMethod(0, function() {});
            (new Foo)?.method();
            """));
  }

  @Test
  public void doNotMoveClassMethodContainingSuper() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Bar { method() {} }
                    class Foo extends Bar { method2() { super.method(); } }
                    """)
                .addChunk("(new Foo).method2()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Bar {}
                Bar.prototype.method = JSCompiler_stubMethod(0);
                class Foo extends Bar { method2() { super.method(); } }

                """,
            """
            Bar.prototype.method = JSCompiler_unstubMethod(0, function() {});
            (new Foo).method2();
            """));
  }

  @Test
  public void doNotMoveClassMethodContainingSuperInAnArrow() {
    // TODO(bradfordcsmith): Stop normalizing the expected output or document why it is necessary.
    enableNormalizeExpectedOutput();
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Bar { method() {} }
                    class Foo extends Bar { method2() { return () => super.method(); } }
                    """)
                .addChunk("(new Foo).method2()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Bar {}
                Bar.prototype.method = JSCompiler_stubMethod(0);
                class Foo extends Bar { method2() { return () => super.method(); } }

                """,
            """
            Bar.prototype.method = JSCompiler_unstubMethod(0, function() {});
            (new Foo).method2();
            """));
  }

  @Test
  public void moveClassMethodContainingObjLitContainingSuper() {
    // Don't be fooled by `super` that isn't referring to the method's `super`.
    // This `super` isn't really a reference within `method()`
    // It refers to Object.prototype.toString.
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Foo {
                      method() {
                        return {
                          objLitMethod() {
                    // This `super` isn't really a reference within `method()`
                    // It refers to Object.prototype.toString.
                            super.toString;
                          }
                        };
                      }
                    }
                    """)
                .addChunk("(new Foo).method();")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {
                }
                Foo.prototype.method = JSCompiler_stubMethod(0);
                """,
            """
            Foo.prototype.method = JSCompiler_unstubMethod(0, function() {
              return {
                objLitMethod() {
                  super.toString;
                }
              };
            });
            (new Foo).method();
            """));
  }

  @Test
  public void doNotMoveClassMethodContainingSuperDefaultParam() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Bar { defaultValue() { return 1; } }
                    class Foo extends Bar { method(x = super.defaultValue()) { return x; } }
                    """)
                .addChunk("(new Foo).method2()")
                .build()));
  }

  @Test
  public void doNotMoveClassConstructor() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Foo { constructor() { } }
                    """)
                .addChunk("(new Foo).constructor")
                .build()));
  }

  @Test
  public void doNotMoveClassComputedPropertyMethod() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk("const methodName = 'method';")
                .addChunk("class Foo { [methodName]() {} }")
                .addChunk("(new Foo)[methodName]()")
                .build()));
  }

  @Test
  public void moveClassMethodForConstDefinition() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk("const Foo = class FooInternal { method() {} }")
                .addChunk("(new Foo).method()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                const Foo = class FooInternal {}
                Foo.prototype.method = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.method = JSCompiler_unstubMethod(0, function() {});
            (new Foo).method();
            """));
  }

  @Test
  public void doNotMoveFunctionCall_thatIsSideEffected() {
    // TODO(bradfordcsmith): Stop normalizing the expected output or document why it is necessary.
    enableNormalizeExpectedOutput();
    JSChunk[] chunks =
        JSChunkGraphBuilder.forChain()
            // m1
            .addChunk(
                """
                var a = 0;
                function f1(a) { return a + 1 }
                var b = f1(1);
                a += 1;
                """)
            // m2
            .addChunk("var c = b")
            .build();

    testSame(srcs(chunks));
  }

  @Test
  public void doNotMoveClassMethodWithLocalClassNameReference() {
    // We could probably rewrite the internal reference, but it is unlikely that the added
    // complexity of doing so would be worthwhile.
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk("const Foo = class FooInternal { method() { FooInternal; } }")
                .addChunk("(new Foo).method()")
                .build()));
  }

  @Test
  public void doNotMoveGetterDefinedInPrototypeLiteral() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype = { get method() {} };
                    """)
                .addChunk("(new Foo).method()")
                .build()));
  }

  @Test
  public void doNotMoveClassGetter() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk("class Foo { get method() {} }")
                .addChunk("(new Foo).method()")
                .build()));
  }

  @Test
  public void movePrototypeMethodWithoutStub() {
    testSame(
        externs("IFoo.prototype.bar;"),
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.bar = function() {};
                    """)
                .addChunk("(new Foo).bar()")
                .build()));

    canMoveExterns = true;
    noStubs = true;
    test(
        externs("IFoo.prototype.bar;"),
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.bar = function() {};
                    """)
                .addChunk("(new Foo).bar()")
                .build()),
        expected(
            "function Foo() {}",
            // Chunk 2
            """
            Foo.prototype.bar = function() {};
            (new Foo).bar()
            """));
  }

  @Test
  public void movePrototypeMethodImplementingInterfaceWithoutStub() {
    disableCompareJsDoc(); // multistage compilation erases the @implements
    testSame(
        externs(
            """
            /** @interface */
            class IFoo {
              ifooMethod() {}
            }
            """),
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    /**
                     * @constructor
                     * @implements {IFoo}
                     */
                    function Foo() {}
                    Foo.prototype.ifooMethod = function() {};
                    """)
                .addChunk("(new Foo).ifooMethod()")
                .build()));

    canMoveExterns = true;
    noStubs = true;
    test(
        externs(
            """
            /** @interface */
            class IFoo {
              ifooMethod() {}
            }
            """),
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    /**
                     * @constructor
                     * @implements {IFoo}
                     */
                    function Foo() {}
                    Foo.prototype.ifooMethod = function() {};
                    """)
                .addChunk("(new Foo).ifooMethod()")
                .build()),
        expected(
            """
            /**
             * @constructor
             */
            function Foo() {}
            """,
            // Chunk 2
            """
            Foo.prototype.ifooMethod = function() {};
            (new Foo).ifooMethod()
            """));
  }

  @Test
  public void moveClassMethodWithoutStub() {
    testSame(
        externs("IFoo.prototype.bar;"),
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk("class Foo { bar() {} }")
                .addChunk("(new Foo).bar()")
                .build()));

    canMoveExterns = true;
    noStubs = true;
    test(
        externs("IFoo.prototype.bar;"),
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk("class Foo { bar() {} }")
                .addChunk("(new Foo).bar()")
                .build()),
        expected(
            "class Foo {}",
            // Chunk 2
            """
            Foo.prototype.bar = function() {};
            (new Foo).bar()
            """));
  }

  @Test
  public void moveClassMethodImplementingExternsInterfaceWithoutStub() {
    disableCompareJsDoc(); // multistage compilation deletes the @implements
    testSame(
        externs(
            """
            /** @interface */
            class IFoo {
              ifooMethod() {}
            }
            """),
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    /** @implements {IFoo} */
                    class Foo { ifooMethod() {} }
                    """)
                .addChunk("(new Foo).ifooMethod()")
                .build()));

    canMoveExterns = true;
    noStubs = true;
    test(
        externs(
            """
            /** @interface */
            class IFoo {
              ifooMethod() {}
            }
            """),
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    /** @implements {IFoo} */
                    class Foo { ifooMethod() {} }
                    """)
                .addChunk("(new Foo).ifooMethod()")
                .build()),
        expected(
            "class Foo {}",
            // Chunk 2
            """
            Foo.prototype.ifooMethod = function() {};
            (new Foo).ifooMethod()
            """));
  }

  @Test
  public void doNotMovePrototypeMethodIfAliasedAndNoStubs() {
    // don't move if noStubs enabled and there's a reference to the method to be moved
    noStubs = true;
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.m = function() {};
                    Foo.prototype.m2 = Foo.prototype.m;
                    """)
                .addChunk("(new Foo).m()")
                .build()));

    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.m = function() {};
                    Foo.prototype.m2 = Foo.prototype.m;
                    """)
                .addChunk("(new Foo).m(), (new Foo).m2()")
                .build()));

    noStubs = false;

    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.m = function() {};
                    Foo.prototype.m2 = Foo.prototype.m;
                    """)
                .addChunk("(new Foo).m()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype.m = JSCompiler_stubMethod(0);
                Foo.prototype.m2 = Foo.prototype.m;
                """,
            // Chunk 2
            """
            Foo.prototype.m = JSCompiler_unstubMethod(0, function() {});
            (new Foo).m()
            """));

    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.m = function() {};
                    Foo.prototype.m2 = Foo.prototype.m;
                    """)
                .addChunk("(new Foo).m(), (new Foo).m2()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype.m = JSCompiler_stubMethod(0);
                Foo.prototype.m2 = Foo.prototype.m;
                """,
            // Chunk 2
            """
            Foo.prototype.m = JSCompiler_unstubMethod(0, function() {});
            (new Foo).m(), (new Foo).m2()
            """));
  }

  @Test
  public void doNotMoveClassMethodIfAliasedAndNoStubs() {
    // don't move if noStubs enabled and there's a reference to the method to be moved
    noStubs = true;
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Foo { m() {} }
                    Foo.prototype.m2 = Foo.prototype.m;
                    """)
                .addChunk("(new Foo).m()")
                .build()));

    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Foo { m() {} }
                    Foo.prototype.m2 = Foo.prototype.m;
                    """)
                .addChunk("(new Foo).m(), (new Foo).m2()")
                .build()));

    noStubs = false;

    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Foo { m() {} }
                    Foo.prototype.m2 = Foo.prototype.m;
                    """)
                .addChunk("(new Foo).m()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.m = JSCompiler_stubMethod(0);
                Foo.prototype.m2 = Foo.prototype.m;
                """,
            // Chunk 2
            """
            Foo.prototype.m = JSCompiler_unstubMethod(0, function() {});
            (new Foo).m()
            """));

    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Foo { m() {} }
                    Foo.prototype.m2 = Foo.prototype.m;
                    """)
                .addChunk("(new Foo).m(), (new Foo).m2()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.m = JSCompiler_stubMethod(0);
                Foo.prototype.m2 = Foo.prototype.m;
                """,
            // Chunk 2
            """
            Foo.prototype.m = JSCompiler_unstubMethod(0, function() {});
            (new Foo).m(), (new Foo).m2()
            """));
  }

  @Test
  public void doNotMovePrototypeMethodRedeclaredInSiblingChunk() {
    // don't move if it can be overwritten when a sibling of the first referencing chunk is loaded.
    testSame(
        srcs(
            JSChunkGraphBuilder.forStar()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.method = function() {};
                    """)
                .addChunk("Foo.prototype.method = function() {};")
                .addChunk("(new Foo).method()")
                .build()));
  }

  @Test
  public void doNotMoveClassMethodRedeclaredInSiblingChunk() {
    // don't move if it can be overwritten when a sibling of the first referencing chunk is loaded.
    testSame(
        srcs(
            JSChunkGraphBuilder.forStar()
                .addChunk("class Foo { method() {} }")
                .addChunk("Foo.prototype.method = function() {};")
                .addChunk("(new Foo).method()")
                .build()));
  }

  @Test
  public void doNotMovePrototypeMethodRedeclaredInDependentChunk() {
    // don't move if it can be overwritten by a chunk depending on the first referencing chunk.
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.method = function() {};
                    """)
                .addChunk("(new Foo).method()")
                .addChunk("Foo.prototype.method = function() {};")
                .build()));
  }

  @Test
  public void doNotMoveClassMethodRedeclaredInDependentChunk() {
    // don't move if it can be overwritten by a chunk depending on the first referencing chunk.
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk("class Foo { method() {} }")
                .addChunk("(new Foo).method()")
                .addChunk("Foo.prototype.method = function() {};")
                .build()));
  }

  @Test
  public void doNotMovePrototypeMethodRedeclaredBeforeFirstReferencingChunk() {
    // Note: it is reasonable to move the method in this case,
    // but it is difficult enough to prove that we don't.
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.method = function() {};
                    """)
                .addChunk("Foo.prototype.method = function() {};")
                .addChunk("(new Foo).method()")
                .build()));
  }

  @Test
  public void doNotMoveClassMethodRedeclaredBeforeFirstReferencingChunk() {
    // Note: it is reasonable to move the method in this case,
    // but it is difficult enough to prove that we don't.
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk("class Foo { method() {} }")
                .addChunk("Foo.prototype.method = function() {};")
                .addChunk("(new Foo).method()")
                .build()));
  }

  @Test
  public void movePrototypeRecursiveMethod() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.baz = function() { this.baz(); };
                    """)
                .addChunk("(new Foo).baz()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.baz = JSCompiler_unstubMethod(0, function() { this.baz(); });
            (new Foo).baz()
            """));
  }

  @Test
  public void moveInstanceRecursiveMethod() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk("class Foo { baz() { this.baz(); } }")
                .addChunk("(new Foo).baz()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.baz = JSCompiler_unstubMethod(0, function() { this.baz(); });
            (new Foo).baz()
            """));
  }

  @Test
  public void doNotMoveNonLiteralFunction() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.baz = shared;
                    """)
                .addChunk("(new Foo).baz()")
                .build()));

    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Foo {}
                    Foo.prototype.baz = shared;
                    """)
                .addChunk("(new Foo).baz()")
                .build()));
  }

  @Test
  public void movePrototypeDeclarationsInTheRightOrder() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.baz = function() { return 1; };
                    Foo.prototype.baz = function() { return 2; };
                    """)
                .addChunk("(new Foo).baz()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype.baz = JSCompiler_stubMethod(1);
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.baz = JSCompiler_unstubMethod(1, function() { return 1; });
            Foo.prototype.baz = JSCompiler_unstubMethod(0, function() { return 2; });
            (new Foo).baz()
            """));
  }

  @Test
  public void moveClassMethodAndReclarationInTheRightOrder() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Foo { baz() { return 1; } }
                    Foo.prototype.baz = function() { return 2; };
                    """)
                .addChunk("(new Foo).baz()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.baz = JSCompiler_stubMethod(1);
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.baz =
            JSCompiler_unstubMethod(1, function() { return 1; });
            Foo.prototype.baz =
            JSCompiler_unstubMethod(0, function() { return 2; });
            (new Foo).baz()
            """));
  }

  @Test
  public void movePrototypeMethodsForDifferentClassesInTheRightOrder() {
    JSChunk[] m =
        JSChunkGraphBuilder.forUnordered()
            .addChunk(
                """
                function Foo() {}
                Foo.prototype.baz = function() { return 1; };
                function Goo() {}
                Goo.prototype.baz = function() { return 2; };
                """)

            // Chunk 2, depends on 1
            .addChunk("")
            // Chunk 3, depends on 2
            .addChunk("(new Foo).baz()")
            // Chunk 4, depends on 3
            .addChunk("")
            // Chunk 5, depends on 3
            .addChunk("(new Goo).baz()")
            .build();

    m[1].addDependency(m[0]);
    m[2].addDependency(m[1]);
    m[3].addDependency(m[2]);
    m[4].addDependency(m[2]);

    test(
        srcs(m),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype.baz = JSCompiler_stubMethod(1);
                function Goo() {}
                Goo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            "",
            // Chunk 3
            """
            Foo.prototype.baz = JSCompiler_unstubMethod(1, function() { return 1; });
            Goo.prototype.baz = JSCompiler_unstubMethod(0, function() { return 2; });
            (new Foo).baz()
            """,
            // Chunk 4
            "",
            // Chunk 5
            "(new Goo).baz()"));
  }

  @Test
  public void moveClassMethodsForDifferentClassesInTheRightOrder() {
    JSChunk[] m =
        JSChunkGraphBuilder.forUnordered()
            .addChunk(
                """
                class Foo { baz() { return 1; } }
                class Goo { baz() { return 2; } }
                """)
            // Chunk 2, depends on 1
            .addChunk("")
            // Chunk 3, depends on 2
            .addChunk("(new Foo).baz()")
            // Chunk 4, depends on 3
            .addChunk("")
            // Chunk 5, depends on 3
            .addChunk("(new Goo).baz()")
            .build();

    m[1].addDependency(m[0]);
    m[2].addDependency(m[1]);
    m[3].addDependency(m[2]);
    m[4].addDependency(m[2]);

    test(
        srcs(m),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.baz = JSCompiler_stubMethod(1);
                class Goo {}
                Goo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            "",
            // Chunk 3
            """
            Foo.prototype.baz = JSCompiler_unstubMethod(1, function() { return 1; });
            Goo.prototype.baz = JSCompiler_unstubMethod(0, function() { return 2; });
            (new Foo).baz()
            """,
            // Chunk 4
            "",
            // Chunk 5
            "(new Goo).baz()"));
  }

  @Test
  public void doNotMovePrototypeMethodUsedInMultiplepDependentChunks() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forStar()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.baz = function() {};
                    """)
                .addChunk("(new Foo).baz()")
                .addChunk("(new Foo).baz()")
                .build()));
  }

  @Test
  public void doNotMoveClassMethodUsedInMultiplepDependentChunks() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forStar()
                .addChunk("class Foo { baz() {} }")
                .addChunk("(new Foo).baz()")
                .addChunk("(new Foo).baz()")
                .build()));
  }

  @Test
  public void movePrototypeMethodToDeepestCommonDependencyOfReferencingChunks() {
    JSChunk[] chunks =
        JSChunkGraphBuilder.forUnordered()
            .addChunk(
                """
                function Foo() {}
                Foo.prototype.baz = function() {};
                """)
            // Chunk 2
            // a blank chunk in the middle
            .addChunk("")
            // Chunk 3
            .addChunk("(new Foo).baz() , 1")
            // Chunk 4
            .addChunk("(new Foo).baz() , 2")
            .build();

    chunks[1].addDependency(chunks[0]);
    chunks[2].addDependency(chunks[1]);
    chunks[3].addDependency(chunks[1]);
    test(
        srcs(chunks),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            "Foo.prototype.baz = JSCompiler_unstubMethod(0, function() {});",
            // Chunk 3
            "(new Foo).baz() , 1",
            // Chunk 4
            "(new Foo).baz() , 2"));
  }

  @Test
  public void moveClassMethodToDeepestCommonDependencyOfReferencingChunks() {
    JSChunk[] chunks =
        JSChunkGraphBuilder.forUnordered()
            .addChunk("class Foo { baz() {} }")
            // Chunk 2
            // a blank chunk in the middle
            .addChunk("")
            // Chunk 3
            .addChunk("(new Foo).baz() , 1")
            // Chunk 4
            .addChunk("(new Foo).baz() , 2")
            .build();

    chunks[1].addDependency(chunks[0]);
    chunks[2].addDependency(chunks[1]);
    chunks[3].addDependency(chunks[1]);

    test(
        srcs(chunks),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            "Foo.prototype.baz = JSCompiler_unstubMethod(0, function() {});",
            // Chunk 3
            "(new Foo).baz() , 1",
            // Chunk 4
            "(new Foo).baz() , 2"));
  }

  @Test
  public void movePrototypeMethodThatRefersToAnotherOnTheSameClass() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.baz = function() {};
                    """)
                // Chunk 2
                .addChunk("Foo.prototype.callBaz = function() { this.baz(); }")
                // Chunk 3
                .addChunk("(new Foo).callBaz()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            "Foo.prototype.callBaz = JSCompiler_stubMethod(1);",
            // Chunk 3
            """
            Foo.prototype.callBaz =
              JSCompiler_unstubMethod(1, function() { this.baz(); });
            Foo.prototype.baz = JSCompiler_unstubMethod(0, function() {});
            (new Foo).callBaz()
            """));
  }

  @Test
  public void movePrototypeMethodThatRefersToAnClassMethodOnTheSameClass() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk("class Foo { baz() {} }")
                // Chunk 2
                .addChunk("Foo.prototype.callBaz = function() { this.baz(); }")
                // Chunk 3
                .addChunk("(new Foo).callBaz()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            "Foo.prototype.callBaz = JSCompiler_stubMethod(1);",
            // Chunk 3
            """
            Foo.prototype.callBaz = JSCompiler_unstubMethod(1, function() { this.baz(); });
            Foo.prototype.baz = JSCompiler_unstubMethod(0, function() {});
            (new Foo).callBaz()
            """));
  }

  @Test
  public void doNotMovePrototypeMethodDefinitionThatFollowsFirstUse() {
    // if the programmer screws up the module order, we don't try to correct
    // the mistake.
    // call before definition
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.baz = function() {};
                    """)
                // Chunk 2
                // call before definition
                .addChunk("(new Foo).callBaz()")
                // Chunk 3
                .addChunk("Foo.prototype.callBaz = function() { this.baz(); }")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            "(new Foo).callBaz()",
            // Chunk 3
            """
            Foo.prototype.baz = JSCompiler_unstubMethod(0, function() {});
            Foo.prototype.callBaz = function() { this.baz(); };
            """));
  }

  @Test
  public void movePrototypeMethodPastUsageInAGlobalFunction() {
    // usage here doesn't really happen until x() is called, so
    // it's OK to move the definition of baz().
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.baz = function() {};
                    // usage here doesn't really happen until x() is called, so
                    // it's OK to move the definition of baz().
                    function x() { return (new Foo).baz(); }
                    """)
                // Chunk 2
                .addChunk("x();")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                function x() { return (new Foo).baz(); }
                """,
            // Chunk 2
            """
            Foo.prototype.baz = JSCompiler_unstubMethod(0, function() {});
            x();
            """));
  }

  @Test
  public void moveClassMethodPastUsageInAGlobalFunction() {
    // usage here doesn't really happen until x() is called, so
    // it's OK to move the definition of baz().
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Foo {
                      baz() {}
                    }
                    // usage here doesn't really happen until x() is called, so
                    // it's OK to move the definition of baz().
                    function x() { return (new Foo).baz(); }
                    """)
                // Chunk 2
                .addChunk("x();")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                function x() { return (new Foo).baz(); }
                """,
            // Chunk 2
            """
            Foo.prototype.baz = JSCompiler_unstubMethod(0, function() {});
            x();
            """));
  }

  // Read of closure variable disables method motions.
  @Test
  public void doNotMovePrototypeMethodThatUsesLocalClosureVariable() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    (function() {
                      var x = 'x';
                      Foo.prototype.baz = function() {x};
                    })();
                    """)
                .addChunk("var y = new Foo(); y.baz();")
                .build()));
  }

  @Test
  public void doNotMoveClassMethodThatUsesLocalClosureVariable() {
    // TODO(bradfordcsmith): Stop normalizing the expected output or document why it is necessary.
    enableNormalizeExpectedOutput();
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    const Foo = (function() {
                      var x = 'x';
                      return class Foo { baz() { return x; } };
                    })();
                    """)
                .addChunk("var y = new Foo(); y.baz();")
                .build()));
  }

  @Test
  public void movePrototypeMethodThatDefinesOtherMethodsOnSameGlobalClass() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.b1 = function() {
                      var x = 1;
                      Foo.prototype.b2 = function() {
                        Foo.prototype.b3 = function() {
                          x;
                        }
                      }
                    };
                    """)
                // Chunk 2
                .addChunk("var y = new Foo(); y.b1();")
                // Chunk 3
                .addChunk("y = new Foo(); z.b2();")
                // Chunk 4
                .addChunk("y = new Foo(); z.b3();")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype.b1 = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.b1 = JSCompiler_unstubMethod(0, function() {
              var x = 1;
              Foo.prototype.b2 = function() {
                Foo.prototype.b3 = function() {
                  x;
                }
              }
            });
            var y = new Foo(); y.b1();
            """,
            // Chunk 3
            "y = new Foo(); z.b2();",
            // Chunk 4
            "y = new Foo(); z.b3();"));
  }

  @Test
  public void moveClassMethodThatDefinesOtherMethodsOnSameGlobalClass() {
    // b2 cannot be extracted, because it contains a reference to x
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Foo {
                      b1() {
                        var x = 1;
                    // b2 cannot be extracted, because it contains a reference to x
                        Foo.prototype.b2 = function() {
                          Foo.prototype.b3 = function() {
                            x;
                          }
                        }
                      };
                    }
                    """)
                .addChunk("var y = new Foo(); y.b1();")
                .addChunk("y = new Foo(); z.b2();")
                .addChunk("y = new Foo(); z.b3();")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.b1 = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.b1 = JSCompiler_unstubMethod(0, function() {
              var x = 1;
              Foo.prototype.b2 = function() {
                Foo.prototype.b3 = function() {
                  x;
                }
              }
            });
            var y = new Foo(); y.b1();
            """,
            // Chunk 3
            "y = new Foo(); z.b2();",
            // Chunk 4
            "y = new Foo(); z.b3();"));
  }

  @Test
  public void extractPrototypeMethodDefinedInAnotherMethodWhenNoClosureReferencePreventsIt() {
    // definition of b2 can be extracted, because it doesn't refer to any
    // variables
    // defined by b1.
    // definition of b3 cannot be extracted, because it refers to x
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.b1 = function() {
                    // definition of b2 can be extracted, because it doesn't refer to any
                    // variables
                    // defined by b1.
                      Foo.prototype.b2 = function() {
                        var x = 1;
                    // definition of b3 cannot be extracted, because it refers to x
                        Foo.prototype.b3 = function() {
                          x;
                        }
                      }
                    };
                    """)
                .addChunk("var y = new Foo(); y.b1();")
                .addChunk("y = new Foo(); z.b2();")
                .addChunk("y = new Foo(); z.b3();")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype.b1 = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.b1 = JSCompiler_unstubMethod(0, function() {
              Foo.prototype.b2 = JSCompiler_stubMethod(1);
            });
            var y = new Foo(); y.b1();
            """,
            // Chunk 3
            """
            Foo.prototype.b2 = JSCompiler_unstubMethod(1, function() {
              var x = 1;
              Foo.prototype.b3 = function() {
                x;
              }
            });
            y = new Foo(); z.b2();
            """,
            // Chunk 4
            "y = new Foo(); z.b3();"));
  }

  @Test
  public void extractClassMethodDefinedInAnotherMethodWhenNoClosureReferencePreventsIt() {
    // definition of b2 can be extracted, because it doesn't refer to any
    // variables
    // defined by b1.
    // definition of b3 cannot be extracted, because it refers to x
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Foo {
                      b1() {
                    // definition of b2 can be extracted, because it doesn't refer to any
                    // variables
                    // defined by b1.
                        Foo.prototype.b2 = function() {
                          var x = 1;
                    // definition of b3 cannot be extracted, because it refers to x
                          Foo.prototype.b3 = function() {
                            x;
                          }
                        }
                      }
                    }
                    """)
                // Chunk 2
                .addChunk("var y = new Foo(); y.b1();")
                // Chunk 3
                .addChunk("y = new Foo(); z.b2();")
                // Chunk 4
                .addChunk("y = new Foo(); z.b3();")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.b1 = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.b1 =
                JSCompiler_unstubMethod(
                    0,
                    function() {
                      Foo.prototype.b2 = JSCompiler_stubMethod(1);
                    });


            var y = new Foo(); y.b1();
            """,
            // Chunk 3
            """
            Foo.prototype.b2 = JSCompiler_unstubMethod(1, function() {
              var x = 1;
              Foo.prototype.b3 = function() {
                x;
              }
            });
            y = new Foo(); z.b2();
            """,
            // Chunk 4
            "y = new Foo(); z.b3();"));
  }

  // Read of global variable is fine.
  @Test
  public void movePrototypeMethodThatReadsGlobalVar() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    var x = 'x';
                    Foo.prototype.baz = function(){x};
                    """)
                .addChunk("var y = new Foo(); y.baz();")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                var x = 'x';
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.baz = JSCompiler_unstubMethod(0, function(){x});
            var y = new Foo(); y.baz();
            """));
  }

  // Read of global variable is fine.
  @Test
  public void moveClassMethodThatReadsGlobalVar() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Foo {
                      baz() { x; }
                    }
                    var x = 'x';
                    """)
                .addChunk("var y = new Foo(); y.baz();")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                var x = 'x';

                """,
            // Chunk 2
            """
            Foo.prototype.baz = JSCompiler_unstubMethod(0, function(){x});
            var y = new Foo(); y.baz();
            """));
  }

  // Read of a local is fine.
  @Test
  public void movePrototypeMethodThatReferencesOnlyLocalVariables() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.baz = function(){var x = 1;x};
                    """)
                .addChunk("var y = new Foo(); y.baz();")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.baz = JSCompiler_unstubMethod(
                0, function(){var x = 1; x});
            var y = new Foo(); y.baz();
            """));
  }

  // Read of a local is fine.
  @Test
  public void moveClassMethodThatReferencesOnlyLocalVariables() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk("class Foo { baz() {var x = 1; x; } }")
                .addChunk("var y = new Foo(); y.baz();")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.baz = JSCompiler_unstubMethod(
                0, function(){var x = 1; x});
            var y = new Foo(); y.baz();
            """));
  }

  // An anonymous inner function reading a closure variable is fine.
  @Test
  public void movePrototypeMethodContainingClosureOverLocalVariable() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    function Foo() {}
                    Foo.prototype.baz = function() {
                      var x = 1;
                      return function(){x}
                    };
                    """)
                .addChunk("var y = new Foo(); y.baz();")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                function Foo() {}
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.baz = JSCompiler_unstubMethod(
                0, function(){var x = 1; return function(){x}});
            var y = new Foo(); y.baz();
            """));
  }

  @Test
  public void moveClassMethodContainingClosureOverLocalVariable() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Foo {
                      baz() {
                        var x = 1;
                        return function(){x}
                      }
                    }
                    """)
                .addChunk("var y = new Foo(); y.baz();")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Foo {}
                Foo.prototype.baz = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Foo.prototype.baz = JSCompiler_unstubMethod(
                0, function(){var x = 1; return function(){x}});
            var y = new Foo(); y.baz();
            """));
  }

  @Test
  public void staticBlockWithoutMethodReference() {
    // TODO(bradfordcsmith): Stop normalizing the expected output or document why it is necessary.
    enableNormalizeExpectedOutput();
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Bar {
                      method() {
                      }
                      static {
                      }
                    }
                    class Foo extends Bar {
                      method2() {
                        return () => super.method();
                      }
                    }
                    """)
                .addChunk("(new Foo).method2()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Bar {
                  static {
                  }
                }
                Bar.prototype.method = JSCompiler_stubMethod(0);
                class Foo extends Bar {
                  method2() {
                    return () => super.method();
                  }
                }
                """,
            // Chunk 2
            """
            Bar.prototype.method = JSCompiler_unstubMethod(0, function() {});
            (new Foo).method2()
            """));
  }

  @Test
  public void referenceToMethodInOwnStaticBlock() {
    // TODO(bradfordcsmith): Stop normalizing the expected output or document why it is necessary.
    enableNormalizeExpectedOutput();
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Bar {
                      method() {
                      }
                      static {
                        this.prototype.method;
                      }
                    }
                    class Foo extends Bar {
                      method2() {
                        return () => super.method();
                      }
                    }
                    """)
                .addChunk("(new Foo).method2()")
                .build()));
  }

  @Test
  public void staticBlockReferenceToMethodInDifferentClassNoMovement() {
    // TODO(bradfordcsmith): Stop normalizing the expected output or document why it is necessary.
    enableNormalizeExpectedOutput();
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Bar {
                      method() {}
                    }
                    class Foo extends Bar {
                      static {
                        (new Bar).method();
                      }
                      method2() {
                        return () => super.method();
                      }
                    }
                    """)
                .addChunk("(new Foo).method2()")
                .build()));
  }

  @Test
  public void staticBlockReferenceToMethodInDifferentClassWithMovement() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    class Bar {
                      method() {}
                    }
                    """)
                .addChunk(
                    """
                    class Foo extends Bar {
                      static {
                        (new Bar()).method();
                      }
                      method2() {
                        return () => { return super.method(); };
                      }
                    }
                    """)
                .addChunk("(new Foo).method2()")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                class Bar {
                }
                Bar.prototype.method = JSCompiler_stubMethod(0);
                """,
            // Chunk 2
            """
            Bar.prototype.method = JSCompiler_unstubMethod(0, function() {});
            class Foo extends Bar {
              static {
                (new Bar()).method();
              }
              method2() {
                return () => { return super.method(); };
              }
            }
            """,
            // Chunk 3
            "(new Foo()).method2();"));
  }

  @Test
  public void testIssue600() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    var jQuery1 = (function() {
                      var jQuery2 = function() {};
                      var theLoneliestNumber = 1;
                      jQuery2.prototype = {
                        size: function() {
                          return theLoneliestNumber;
                        }
                      };
                      return jQuery2;
                    })();
                    """)
                .addChunk(
                    """
                    (function() {
                      var div = jQuery1('div');
                      div.size();
                    })();
                    """)
                .build()));
  }

  @Test
  public void testIssue600b() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    var jQuery1 = (function() {
                      var jQuery2 = function() {};
                      jQuery2.prototype = {
                        size: function() {
                          return 1;
                        }
                      };
                      return jQuery2;
                    })();
                    """)
                .addChunk(
                    """
                    (function() {
                      var div = jQuery1('div');
                      div.size();
                    })();
                    """)
                .build()));
  }

  @Test
  public void testIssue600c() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    var jQuery2 = function() {};
                    jQuery2.prototype = {
                      size: function() {
                        return 1;
                      }
                    };
                    """)
                .addChunk(
                    """
                    (function() {
                      var div = jQuery2('div');
                      div.size();
                    })();
                    """)
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                var jQuery2 = function() {};
                jQuery2.prototype = {
                  size: JSCompiler_stubMethod(0)
                };
                """,
            // Chunk 2
            """
            jQuery2.prototype.size=
                JSCompiler_unstubMethod(0,function(){return 1});
            (function() {
              var div = jQuery2('div');
              div.size();
            })();
            """));
  }

  @Test
  public void testIssue600d() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    var jQuery2 = function() {};
                    (function() {
                      jQuery2.prototype = {
                        size: function() {
                          return 1;
                        }
                      };
                    })();
                    """)
                .addChunk(
                    """
                    (function() {
                      var div = jQuery2('div');
                      div.size();
                    })();
                    """)
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                var jQuery2 = function() {};
                (function() {
                  jQuery2.prototype = {
                    size: JSCompiler_stubMethod(0)
                  };
                })();
                """,
            """
            jQuery2.prototype.size=
                JSCompiler_unstubMethod(0,function(){return 1});
            (function() {
              var div = jQuery2('div');
              div.size();
            })();
            """));
  }

  @Test
  public void testIssue600e() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    var jQuery2 = function() {};
                    (function() {
                      var theLoneliestNumber = 1;
                      jQuery2.prototype = {
                        size: function() {
                          return theLoneliestNumber;
                        }
                      };
                    })();
                    """)
                .addChunk(
                    """
                    (function() {
                      var div = jQuery2('div');
                      div.size();
                    })();
                    """)
                .build()));
  }

  @Test
  public void testPrototypeOfThisAssign() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    /** @constructor */
                    function F() {}
                    """)
                .addChunk("this.prototype.foo = function() {};")
                .addChunk("(new F()).foo();")
                .build()));
  }

  @Test
  public void testDestructuring() {
    test(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    /** @constructor */
                    function F() {}
                    F.prototype.foo = function() {};
                    """)
                .addChunk("const {foo} = new F();")
                .build()),
        expected(
            STUB_DECLARATIONS
                + """
                /** @constructor */
                function F() {}
                F.prototype.foo = JSCompiler_stubMethod(0);
                """,
            """
            F.prototype.foo = JSCompiler_unstubMethod(0, function(){});
            const {foo} = new F();
            """));
  }

  @Test
  public void testDestructuringWithQuotedProp() {
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    /** @constructor */
                    function F() {}
                    F.prototype.foo = function() {};
                    """)
                .addChunk("const {'foo': foo} = new F();")
                .build()));
  }

  @Test
  public void testDestructuringWithComputedProp() {
    // See https://github.com/google/closure-compiler/issues/3145
    testSame(
        srcs(
            JSChunkGraphBuilder.forChain()
                .addChunk(
                    """
                    /** @constructor */
                    function F() {}
                    F.prototype['foo'] = function() {};
                    """)
                .addChunk("const {['foo']: foo} = new F();")
                .build()));
  }
}
