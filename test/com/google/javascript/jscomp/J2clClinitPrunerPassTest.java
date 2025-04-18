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

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class J2clClinitPrunerPassTest extends CompilerTestCase {

  @Override
  protected CompilerPass getProcessor(Compiler compiler) {
    return new J2clClinitPrunerPass(
        compiler, compiler.getChangedScopeNodesForPass("J2clClinitPrunerPass"));
  }

  @Override
  protected Compiler createCompiler() {
    Compiler compiler = super.createCompiler();
    J2clSourceFileChecker.markToRunJ2clPasses(compiler);
    return compiler;
  }

  @Override
  protected int getNumRepetitions() {
    // A single run should be sufficient.
    return 1;
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    enableNormalize();
    // TODO(bradfordcsmith): Stop normalizing the expected output or document why it is necessary.
    enableNormalizeExpectedOutput();
    enableComputeSideEffects();
  }

  @Test
  public void testRemoveDuplicates() {
    test(
        """
        var someClass = {};
        someClass.$clinit = function() {}
        someClass.someOtherFunction = function() {
          someClass.$clinit();
          someClass.$clinit();
        };
        """,
        """
        var someClass = {};
        someClass.$clinit = function() {}
        someClass.someOtherFunction = function() {
          someClass.$clinit();
        };
        """);
  }

  @Test
  public void testRemoveDuplicates_commaExpressions() {
    test(
        """
        var someClass = {};
        someClass.$clinit = function() {}
        someClass.someOtherFunction = function() {
          (someClass.$clinit(), true);
          (someClass.$clinit(), true);
        };
        """,
        """
        var someClass = {};
        someClass.$clinit = function() {}
        someClass.someOtherFunction = function() {
          (someClass.$clinit(), true);
          (void 0, true);
        };
        """);
  }

  @Test
  public void testRemoveDuplicates_controlBlocks() {
    test(
        """
        var someClass = {};
        someClass.$clinit = function() {}
        someClass.someOtherFunction = function() {
          someClass.$clinit();
          if (true) {
            someClass.$clinit();
            while(true) {
              someClass.$clinit();
            }
          } else {
            someClass.$clinit();
          }
          var a = (someClass.$clinit(), true) ? (someClass.$clinit(), void 0) : void 0;
          var b = function() { someClass.$clinit(); };
          var c = function c() { someClass.$clinit(); };
          var d = function*() { someClass.$clinit(); };
          var e = async function() { someClass.$clinit(); };
          var f = () => { someClass.$clinit(); };
          [].forEach(function() { someClass.$clinit(); });
          someClass.$clinit();
        };
        """,
        """
        var someClass = {};
        someClass.$clinit = function() {}
        someClass.someOtherFunction = function() {
          someClass.$clinit();
          if (true) {
            while(true) {
            }
          } else {
          }
          var a = (void 0, true) ? (void 0, void 0) : void 0;
          var b = function() {};
          var c = function c() {};
          var d = function*() {};
          var e = async function() {};
          var f = () => {};
          [].forEach(function() {});
        };
        """);
  }

  @Test
  public void testRemoveDuplicates_selfRemoval() {
    test(
        """
        var someClass = {};
        someClass.$clinit = function() {
          someClass.$clinit();
        };
        """,
        """
        var someClass = {};
        someClass.$clinit = function() {}
        """);

    test(
        """
        function someClass$$0clinit() {
          someClass$$0clinit();
        }
        """,
        "function someClass$$0clinit() {}");
  }

  @Test
  public void testRemoveDuplicates_jumpFunctionDeclarations() {
    test(
        """
        var someClass = {};
        someClass.$clinit = function() {}
        someClass.someOtherFunction = function() {
          myFunc();
          someClass.$clinit();
          function myFunc() {
            someClass.$clinit();
            someClass.$clinit();
          }
        };
        """,
        """
        var someClass = {};
        someClass.$clinit = function() {}
        someClass.someOtherFunction = function() {
          myFunc();
        // Control flow analysis doesn't understand that this was already called in `myFunc`.
          someClass.$clinit();
          function myFunc() {
            someClass.$clinit();
          }
        };
        """);
  }

  @Test
  public void testRemoveDuplicates_jumpDefaults() {
    test(
        """
        var someClass = {};
        someClass.$clinit = function() {}
        someClass.someOtherFunction = function(p=someClass.$clinit()) {
          var { x = someClass.$clinit() } = someClass.$clinit();
          someClass.$clinit();
        };
        """,
        """
        var someClass = {};
        someClass.$clinit = function() {}
        someClass.someOtherFunction = function(p=someClass.$clinit()) {
          var { x = someClass.$clinit() } = someClass.$clinit();
        };
        """);
  }

  @Test
  public void testRemoveDuplicates_avoidControlBlocks() {
    testSame(
        """
        var someClass = {};
        someClass.$clinit = function() {}
        someClass.anotherMethod = function() {
          (false && someClass.$clinit());
          (true || someClass.$clinit());
          if (true) {
            someClass.$clinit();
          } else {
            someClass.$clinit();
          }
          while(false) {
            someClass.$clinit();
          }
          for(;false;) {
            someClass.$clinit();
          }
          try {
            someClass.$clinit();
          } catch(e) {
            someClass.$clinit();
          }
          switch(2) {
            case 1: someClass.$clinit(); break;
            case 2: break;
            default: someClass.$clinit();
          }
          var a = true ? (someClass.$clinit(), void 0) : void 0;
          var b = function() { someClass.$clinit(); }
          var c = function*() { someClass.$clinit(); }
          var d = async function() { someClass.$clinit(); }
          var e = () => { someClass.$clinit(); }
          someClass.$clinit();
        };
        """);
  }

  @Test
  public void testRemoveDuplicates_avoidRemovalAcrossScriptRoots() {
    testSame(
        new FlatSources(
            ImmutableList.of(
                SourceFile.fromCode(
                    "file1",
                    """
                    var someClass = {};
                    someClass.$clinit = function() {}
                    someClass.$clinit();
                    """),
                SourceFile.fromCode("file2", "someClass.$clinit();"))));
  }

  @Test
  public void testRedundantClinit_returnCtor() {
    test(
        """
        var Foo = function() {
          Foo.$clinit();
        };
        Foo.ctor = function() {
          Foo.$clinit();
          return new Foo();
        };
        """,
        """
        var Foo = function() {
          Foo.$clinit();
        };
        Foo.ctor = function() {
          return new Foo();
        };
        """);
  }

  @Ignore("Improve look ahead pruner to handle ES6 classes")
  @Test
  public void testRedundantClinit_returnCtor_es6() {
    test(
        """
        class Foo {
          constructor(){
            Foo.$clinit();
          }
          static ctor() {
            Foo.$clinit();
            return new Foo();
          }
        };
        """,
        """
        class Foo {
          constructor(){
            Foo.$clinit();
          }
          static ctor() {
            return new Foo();
          }
        };
        """);
  }

  @Test
  public void testRedundantClinit_returnCall() {
    test(
        """
        var foo = function() {
          Foo.$clinit();
        };
        Foo.ctor = function() {
          Foo.$clinit();
          return foo();
        };
        """,
        """
        var foo = function() {
          Foo.$clinit();
        };
        Foo.ctor = function() {
          return foo();
        };
        """);
  }

  @Ignore("Improve look ahead pruner to handle ES6 classes")
  @Test
  public void testRedundantClinit_returnCall_es6() {
    test(
        """
        class Foo {
          static bar(){
            Foo.$clinit();
          }
          static zoo() {
            Foo.$clinit();
            return Foo.bar();
          }
        };
        """,
        """
        class Foo {
          static bar(){
            Foo.$clinit();
          }
          static zoo() {
            return bar();
          }
        };
        """);
  }

  @Test
  public void testRedundantClinit_exprResult() {
    test(
        """
        var foo = function() {
          Foo.$clinit();
        };
        Foo.ctor = function() {
          Foo.$clinit();
          foo();
        };
        """,
        """
        var foo = function() {
          Foo.$clinit();
        };
        Foo.ctor = function() {
          foo();
        };
        """);
  }

  @Test
  public void testRedundantClinit_var() {
    test(
        """
        var foo = function() {
          Foo.$clinit();
        };
        Foo.ctor = function() {
          Foo.$clinit();
          var x = foo();
        };
        """,
        """
        var foo = function() {
          Foo.$clinit();
        };
        Foo.ctor = function() {
          var x = foo();
        };
        """);
  }

  @Test
  public void testRedundantClinit_let() {
    test(
        """
        var foo = function() {
          Foo.$clinit();
        };
        Foo.ctor = function() {
          Foo.$clinit();
          let x = foo();
        };
        """,
        """
        var foo = function() {
          Foo.$clinit();
        };
        Foo.ctor = function() {
          let x = foo();
        };
        """);
  }

  @Test
  public void testRedundantClinit_const() {
    test(
        """
        var foo = function() {
          Foo.$clinit();
        };
        Foo.ctor = function() {
          Foo.$clinit();
          const x = foo();
        };
        """,
        """
        var foo = function() {
          Foo.$clinit();
        };
        Foo.ctor = function() {
          const x = foo();
        };
        """);
  }

  @Test
  public void testRedundantClinit_literalArgs() {
    test(
        """
        var Foo = function(a) {
          Foo.$clinit();
        };
        Foo.ctor = function() {
          Foo.$clinit();
          return new Foo(1);
        };
        """,
        """
        var Foo = function(a) {
          Foo.$clinit();
        };
        Foo.ctor = function() {
          return new Foo(1);
        };
        """);
  }

  @Test
  public void testRedundantClinit_paramArgs() {
    test(
        """
        var Foo = function(a, b) {
          Foo.$clinit();
        };
        Foo.ctor = function(a) {
          Foo.$clinit();
          return new Foo(a, 1);
        };
        """,
        """
        var Foo = function(a, b) {
          Foo.$clinit();
        };
        Foo.ctor = function(a) {
          return new Foo(a, 1);
        };
        """);
  }

  @Test
  public void testRedundantClinit_unsafeArgs() {
    testSame(
        """
        var Foo = function(a) {
          Foo.$clinit();
        };
        Foo.STATIC_VAR = null;
        Foo.ctor = function() {
          Foo.$clinit();
          return new Foo(Foo.STATIC_VAR);
        };
        """);
  }

  @Test
  public void testRedundantClinit_otherClinit() {
    testSame(
        """
        var Foo = function() {
          Foo1.$clinit();
        };
        Foo.ctor = function() {
          Foo.$clinit();
          return new Foo();
        };
        """);
  }

  @Test
  public void testRedundantClinit_clinitNotFirstStatement() {
    testSame(
        """
        var Foo = function() {
          var x = 1;
          Foo.$clinit();
        };
        Foo.ctor = function() {
          Foo.$clinit();
          return new Foo();
        };
        """);
  }

  @Test
  public void testRedundantClinit_recursiveCall() {
    testSame(
        """
        var foo = function() {
          Foo1.$clinit();
          foo();
        };
        """);
  }

  @Test
  public void testFoldClinit_es5() {
    test(
        """
        var someClass = {};
        someClass.$clinit = function() {
          someClass.$clinit = function() {};
        };
        """,
        """
        var someClass = {};
        someClass.$clinit = function() {};
        """);
  }

  @Test
  public void testFoldClinit_es6() {
    test(
        """
        class someClass {
          static $clinit() {
            someClass.$clinit = function() {};
          }
        }
        """,
        """
        class someClass {
          static $clinit() {}
        }
        """);
  }

  @Test
  public void testFoldClinit_classHierarchy_es5() {
    test(
        """
        var someClass = {};
        someClass.$clinit = function() {
          someClass.$clinit = function() {};
        };
        var someChildClass = {};
        someChildClass.$clinit = function() {
          someChildClass.$clinit = function() {};
          someClass.$clinit();
        };
        someChildClass.someFunction = function() {
          someChildClass.$clinit();
          someClass.$clinit();
        };
        """,
        """
        var someClass = {};
        someClass.$clinit = function() {};
        var someChildClass = {};
        someChildClass.$clinit = function() {};
        someChildClass.someFunction = function() {};
        """);
  }

  @Test
  public void testFoldClinit_classHierarchy_es6() {
    test(
        """
        class someClass {
          static $clinit() {
            someClass.$clinit = function() {};
          }
        }
        class someChildClass {
          static $clinit() {
            someChildClass.$clinit = function() {};
            someClass.$clinit();
          }

          static someFunction() {
            someChildClass.$clinit();
            someClass.$clinit();
          }
        }
        """,
        """
        class someClass {
          static $clinit() {}
        }
        class someChildClass {
          static $clinit() {}

          static someFunction() {}
        }
        """);
  }

  @Test
  public void testFoldClinit_classHierarchyNonEmpty_es5() {
    test(
        """
        var someClass = {};
        someClass.$clinit = function() {
          someClass.$clinit = function() {};
          somefn();
        };
        var someChildClass = {};
        someChildClass.$clinit = function() {
          someChildClass.$clinit = function() {};
          someClass.$clinit();
        };
        someChildClass.someFunction = function() {
          someChildClass.$clinit();
        };
        """,
        """
        var someClass = {};
        someClass.$clinit = function() {
          someClass.$clinit = function() {};
          somefn();
        };
        var someChildClass = {};
        someChildClass.$clinit = function() {
          someClass.$clinit();
        };
        someChildClass.someFunction = function() {
          someClass.$clinit();
        };
        """);
  }

  @Test
  public void testFoldClinit_classHierarchyNonEmpty_es6() {
    test(
        """
        class someClass {
          static $clinit() {
            someClass.$clinit = function() {};
            somefn();
          }
        }
        class someChildClass {
          static $clinit() {
            someChildClass.$clinit = function() {};
            someClass.$clinit();
          }

          static someFunction() {
            someChildClass.$clinit();
          }
        }
        """,
        """
        class someClass {
          static $clinit() {
            someClass.$clinit = function() {};
            somefn();
          }
        }
        class someChildClass {
          static $clinit() {
            someClass.$clinit();
          }

          static someFunction() {
            someClass.$clinit();
          }
        }
        """);
  }

  @Test
  public void testFoldClinit_invalidCandidates_es5() {
    testSame(
        """
        var someClass = /** @constructor */ function() {};
        someClass.foo = function() {};
        someClass.$clinit = function() {
          someClass.$clinit = function() {};
          someClass.foo();
        };
        """);
    testSame(
        """
        var someClass = {}, otherClass = {};
        someClass.$clinit = function() {
          otherClass.$clinit = function() {};
        };
        """);
    testSame(
        """
        var someClass = {};
        someClass.$notClinit = function() {
          someClass.$notClinit = function() {};
        };
        """);
  }

  @Test
  public void testFoldClinit_invalidCandidates_es6() {
    testSame(
        """
        class someClass {
          static foo() {}
          static $clinit() {
            someClass.$clinit = function() {};
            someClass.foo();
          }
        }
        """);
    testSame(
        """
        class someClass {
          static $clinit() {
            otherClass.$clinit = function() {};
          }
        }
        class otherClass {}
        """);
    testSame(
        """
        class someClass {
          static $notClinit() {
            someClass.$notClinit = function() {};
          }
        }
        """);
  }
}
