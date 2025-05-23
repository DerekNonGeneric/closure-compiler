/*
 * Copyright 2007 The Closure Compiler Authors.
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

import com.google.javascript.rhino.Node;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link ConstCheck}. */
@RunWith(JUnit4.class)
public final class ConstCheckTest extends CompilerTestCase {

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    enableCreateModuleMap();
  }

  @Override
  protected CompilerPass getProcessor(Compiler compiler) {
    return (Node externs, Node root) -> {
      new InferConsts(compiler).process(externs, root);
      new ConstCheck(compiler).process(externs, root);
    };
  }

  private void testWarning(String js) {
    testWarning(js, ConstCheck.CONST_REASSIGNED_VALUE_ERROR);
  }

  @Test
  public void testConstantDefinition1() {
    testSame("/** @const */ var XYZ = 1;");
  }

  @Test
  public void testConstantDefinition2() {
    testSame("/** @const */ var a$b$XYZ = 1;");
  }

  @Test
  public void testConstantDefinition3() {
    testNoWarning("const xyz=1;");
  }

  @Test
  public void testConstantDefinition4() {
    System.out.println("HELLO");
    testNoWarning("const a$b$xyz = 1;");
  }

  @Test
  public void testConstantInitializedInAnonymousNamespace1() {
    testSame("/** @const */ var XYZ; (function(){ XYZ = 1; })();");
  }

  @Test
  public void testConstantInitializedInAnonymousNamespace2() {
    testSame("/** @const */ var a$b$XYZ; (function(){ a$b$XYZ = 1; })();");
  }

  @Test
  public void testObjectModified() {
    testSame("/** @const */ var IE = true, XYZ = {a:1,b:1}; if (IE) XYZ['c'] = 1;");
  }

  @Test
  public void testObjectPropertyInitializedLate() {
    testSame("/** @const */ var XYZ = {}; for (var i = 0; i < 10; i++) { XYZ[i] = i; }");
  }

  @Test
  public void testObjectRedefined1() {
    testError("/** @const */ var XYZ = {}; XYZ = 2;");
  }

  @Test
  public void testObjectRedefined2() {
    testError("const xyz = {}; xyz = 2;");
  }

  @Test
  public void testConstantRedefined1() {
    testError("/** @const */ var XYZ = 1; XYZ = 2;");
  }

  @Test
  public void testConstantRedefined2() {
    testError("/** @const */ var a$b$XYZ = 1; a$b$XYZ = 2;");
  }

  // test will be caught be earlier pass, but demonstrates that it returns error upon const
  // reassigning
  @Test
  public void testConstantRedefined3() {
    testWarning("const xyz = 1; xyz = 2;");
  }

  @Test
  public void testConstantRedefined4() {
    testError("/** @const */ let XYZ = 1; XYZ = 2;");
  }

  @Test
  public void testConstantRedefinedInLocalScope1() {
    testError("/** @const */ var XYZ = 1; (function(){ XYZ = 2; })();");
  }

  @Test
  public void testConstantRedefinedInLocalScope2() {
    testError("/** @const */ var a$b$XYZ = 1; (function(){ a$b$XYZ = 2; })();");
  }

  @Test
  public void testConstantRedefinedInLocalScopeOutOfOrder() {
    testError("function f() { XYZ = 2; } /** @const */ var XYZ = 1;");
  }

  @Test
  public void testConstantPostIncremented1() {
    testError("/** @const */ var XYZ = 1; XYZ++;");
  }

  @Test
  public void testConstantPostIncremented2() {
    testError("/** @const */ var a$b$XYZ = 1; a$b$XYZ++;");
  }

  @Test
  public void testConstantPreIncremented1() {
    testError("/** @const */ var XYZ = 1; ++XYZ;");
  }

  @Test
  public void testConstantPreIncremented2() {
    testError("/** @const */ var a$b$XYZ = 1; ++a$b$XYZ;");
  }

  @Test
  public void testConstantPostDecremented1() {
    testError("/** @const */ var XYZ = 1; XYZ--;");
  }

  @Test
  public void testConstantPostDecremented2() {
    testError("/** @const */ var a$b$XYZ = 1; a$b$XYZ--;");
  }

  @Test
  public void testConstantPreDecremented1() {
    testError("/** @const */ var XYZ = 1; --XYZ;");
  }

  @Test
  public void testConstantPreDecremented2() {
    testError("/** @const */ var a$b$XYZ = 1; --a$b$XYZ;");
  }

  @Test
  public void testConstantPreDecremented3() {
    testWarning("const xyz = 1; --xyz;");
  }

  @Test
  public void testAbbreviatedArithmeticAssignment1() {
    testError("/** @const */ var XYZ = 1; XYZ += 2;");
  }

  @Test
  public void testAbbreviatedArithmeticAssignment2() {
    testError("/** @const */ var a$b$XYZ = 1; a$b$XYZ %= 2;");
  }

  @Test
  public void testAbbreviatedArithmeticAssignment3() {
    testError("/** @const */ var a$b$XYZ = 1; a$b$XYZ **= 2;");
  }

  @Test
  public void testAbbreviatedBitAssignment1() {
    testError("/** @const */ var XYZ = 1; XYZ |= 2;");
  }

  @Test
  public void testAbbreviatedBitAssignment2() {
    testError("/** @const */ var a$b$XYZ = 1; a$b$XYZ &= 2;");
  }

  @Test
  public void testAbbreviatedShiftAssignment1() {
    testError("/** @const */ var XYZ = 1; XYZ >>= 2;");
  }

  @Test
  public void testAbbreviatedShiftAssignment2() {
    testError("/** @const */ var a$b$XYZ = 1; a$b$XYZ <<= 2;");
  }

  @Test
  public void testConstAnnotation1() {
    testWarning("/** @const */ var XYZ = 1; XYZ = 2;");
  }

  @Test
  public void testConstAnnotation2() {
    testWarning("/** @const */ let x = 1; x = 2;");
  }

  @Test
  public void testConstAnnotation3() {
    testWarning("/** @const */ const xyz = 1; xyz = 2;");
  }

  @Test
  public void testConstSuppressionInFileJsDoc() {
    testSame(
        """
        /**
         * @fileoverview
         * @suppress {const}
         */
        /** @const */ var xyz = 1; xyz = 3;
        """);
  }

  @Test
  public void testConstSuppressionOnAssignment() {
    testSame("/** @const */ var xyz = 1; /** @suppress {const} */ xyz = 3;");
  }

  @Test
  public void testConstSuppressionOnAddAssign() {
    testSame("/** @const */ var xyz = 1; /** @suppress {const} */ xyz += 1;");
  }

  @Test
  public void testConstSuppressionOnVar() {
    testSame("/** @const */ var xyz = 1;\n/** @suppress {const} */ var xyz = 3;");
  }

  // If there are two 'var' statements for the same variable, one in externs and
  // one in the JS, there is no normalization, and the suppression remains on the
  // statement in the JS.
  @Test
  public void testConstSuppressionOnVarFromExterns() {
    String externs = "/** @const */ var xyz;";
    String js = "/** @suppress {const} */ var xyz = 3;";
    testSame(externs(externs), srcs(js));
  }

  @Test
  public void testConstSuppressionOnInc() {
    testSame("/** @const */ var xyz = 1; /** @suppress {const} */ xyz++;");
  }

  @Test
  public void testConstNameInExterns() {
    String externs = "/** @const */ var FOO;";
    String js = "FOO = 1;";
    testWarning(externs(externs), srcs(js), warning(ConstCheck.CONST_REASSIGNED_VALUE_ERROR));
  }

  @Test
  public void testGoogModuleExportsShadowingGlobal() {
    testSame(srcs("/** @const */ var exports = {};", "goog.module('m'); exports = class {};"));
  }

  @Test
  public void testGoogModuleExportsReassigned() {
    testWarning("goog.module('m'); exports = class {}; exports = class {};");
  }

  @Test
  public void testGoogProvide_rootNamespaceExplicitlyDeclared() {
    testSame("goog.provide('a'); var a = {};");
    testSame("goog.provide('a'); var a = {}; a = 1;");
    testWarning(
        "goog.provide('a'); /** @const */ var a = {}; a = 1;",
        ConstCheck.CONST_REASSIGNED_VALUE_ERROR);
  }

  @Test
  public void testGoogProvide_rootNamespaceImplicitlyDeclared() {
    testSame("goog.provide('a'); a = {};");
    testSame("goog.provide('a'); a = {}; a = 1;");
    testSame("goog.provide('a'); a.B = class {};");
    testSame("goog.provide('a.b.c'); a = class {};");
    testSame(srcs("goog.provide('a.b');", "goog.provide('a.c');"));
    testSame("goog.provide('a'); a = 0; a++;");
  }

  private void testError(String js) {
    testWarning(js, ConstCheck.CONST_REASSIGNED_VALUE_ERROR);
  }
}
