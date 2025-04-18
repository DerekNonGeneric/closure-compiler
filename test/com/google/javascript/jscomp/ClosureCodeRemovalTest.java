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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ClosureCodeRemoval} */
@RunWith(JUnit4.class)
public final class ClosureCodeRemovalTest extends CompilerTestCase {

  private static final String EXTERNS = "var window;";

  private static final String ASSERTIONS =
      """
      const asserts = {};
      /** @closurePrimitive {asserts.truthy} */
      asserts.assert = function(...args) {};

      /** @closurePrimitive {asserts.fail} */
      asserts.fail = function(...args) {};
      """;

  public ClosureCodeRemovalTest() {
    super(EXTERNS);
  }

  @Test
  public void testRemoveAbstract() {
    test(
        "function Foo() {}; Foo.prototype.doSomething = goog.abstractMethod;",
        "function Foo() {};");
  }

  @Test
  public void testRemoveMultiplySetAbstract() {
    test(
        """
        function Foo() {}; Foo.prototype.doSomething =
        Foo.prototype.doSomethingElse = Foo.prototype.oneMore =
        goog.abstractMethod;
        """,
        "function Foo() {};");
  }

  @Test
  public void testDoNotRemoveNormal() {
    testSame("function Foo() {}; Foo.prototype.doSomething = function() {};");
  }

  @Test
  public void testDoNotRemoveOverride() {
    test(
        """
        function Foo() {}; Foo.prototype.doSomething = goog.abstractMethod;
        function Bar() {}; goog.inherits(Bar, Foo);
        Bar.prototype.doSomething = function() {}
        """,
        """
        function Foo() {}; function Bar() {}; goog.inherits(Bar, Foo);
        Bar.prototype.doSomething = function() {}
        """);
  }

  @Test
  public void testDoNotRemoveNonQualifiedName() {
    testSame("document.getElementById('x').y = goog.abstractMethod;");
  }

  @Test
  public void testStopRemovalAtNonQualifiedName() {
    test(
        """
        function Foo() {}; function Bar() {};
        Foo.prototype.x = document.getElementById('x').y = Bar.prototype.x
         = goog.abstractMethod;
        """,
        """
        function Foo() {}; function Bar() {};
        Foo.prototype.x = document.getElementById('x').y =
        goog.abstractMethod;
        """);
  }

  @Test
  public void testRemoveAbstractAssignmentOfEmptyFunction() {
    test(
        """
        function Foo() {};
        /** @abstract */
        Foo.prototype.doSomething = function() {};
        """,
        "function Foo() {};");
  }

  @Test
  public void testDoNotRemoveAbstractAssignmentOfFunctionCall() {
    testSame(
        """
        function Foo() {};
        /** @abstract */
        Foo.prototype.doSomething = (function() { /* return something fancy */ })();
        """);
  }

  @Test
  public void testRemoveAbstract_annotation_es6() {
    test(
        """
        /** @abstract */
        class Foo {
          /** @abstract */
          doSomething() {}
        }
        """,
        "/** @abstract */ class Foo {}");
  }

  @Test
  public void testDoNotRemoveNormal_es6() {
    testSame(
        """
        /** @abstract */
        class Foo {
          doSomething() {}
        }
        """);
  }

  @Test
  public void testAssertionRemoval1() {
    test("var x = goog.asserts.assert(y(), 'message');", "var x = y();");
  }

  @Test
  public void testAssertionRemoval2() {
    test("goog.asserts.assert(y(), 'message');", "");
  }

  @Test
  public void testAssertionRemoval3() {
    test("goog.asserts.assert();", "");
  }

  @Test
  public void testAssertionRemoval4() {
    test("var x = goog.asserts.assert();", "var x = void 0;");
  }

  @Test
  public void testClosurePrimitiveAssertionRemoval1() {
    enableTypeCheck();
    test(ASSERTIONS + "var x = asserts.assert(y(), 'message');", ASSERTIONS + "var x = y();");
  }

  @Test
  public void testClosurePrimitiveAssertionRemoval2() {
    enableTypeCheck();
    test(ASSERTIONS + "asserts.assert(y(), 'message');", ASSERTIONS);
  }

  @Test
  public void testClosurePrimitiveAssertionRemoval3() {
    enableTypeCheck();
    test(ASSERTIONS + "asserts.assert();", ASSERTIONS);
  }

  @Test
  public void testClosurePrimitiveAssertionRemoval4() {
    enableTypeCheck();
    test(ASSERTIONS + "var x = asserts.assert();", ASSERTIONS + "var x = void 0;");
  }

  @Test
  public void testClosurePrimitiveAssertionRemoval_keepsAssertsFail() {
    enableTypeCheck();
    testSame(ASSERTIONS + "var x = asserts.fail();");
  }

  @Test
  public void testClosurePrimitiveAssertionRemoval_worksWithColors() {
    enableTypeCheck();
    replaceTypesWithColors();
    disableCompareJsDoc();

    test(ASSERTIONS + "var x = asserts.assert(y(), 'message');", ASSERTIONS + "var x = y();");
  }

  @Test
  public void testClosurePrimitiveAssertionRemoval_keepsAssertsFailWithColors() {
    enableTypeCheck();
    replaceTypesWithColors();
    disableCompareJsDoc();

    testSame(ASSERTIONS + "var x = asserts.fail();");
  }

  @Test
  public void testDoNotRemoveAbstractClass() {
    testSame(
        """
        var ns = {};
        /** @abstract */
        ns.A = class {};
        ns.B = class extends ns.A {}
        """);
  }

  @Override
  protected ClosureCodeRemoval getProcessor(Compiler compiler) {
    return new ClosureCodeRemoval(compiler, true, true);
  }
}
