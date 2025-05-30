/*
 * Copyright 2014 The Closure Compiler Authors.
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

package com.google.javascript.jscomp.parsing;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.javascript.jscomp.parsing.Config.StrictMode.SLOPPY;
import static com.google.javascript.jscomp.parsing.Config.StrictMode.STRICT;
import static com.google.javascript.jscomp.parsing.JsDocInfoParser.BAD_TYPE_WIKI_LINK;
import static com.google.javascript.jscomp.parsing.parser.testing.FeatureSetSubject.assertFS;
import static com.google.javascript.rhino.testing.NodeSubject.assertNode;

import com.google.common.collect.ImmutableList;
import com.google.javascript.jscomp.NodeUtil;
import com.google.javascript.jscomp.parsing.Config.JsDocParsing;
import com.google.javascript.jscomp.parsing.Config.LanguageMode;
import com.google.javascript.jscomp.parsing.ParserRunner.ParseResult;
import com.google.javascript.jscomp.parsing.parser.FeatureSet;
import com.google.javascript.jscomp.parsing.parser.FeatureSet.Feature;
import com.google.javascript.jscomp.parsing.parser.trees.Comment;
import com.google.javascript.rhino.IR;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSTypeExpression;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.NonJSDocComment;
import com.google.javascript.rhino.SimpleSourceFile;
import com.google.javascript.rhino.StaticSourceFile;
import com.google.javascript.rhino.StaticSourceFile.SourceKind;
import com.google.javascript.rhino.Token;
import com.google.javascript.rhino.jstype.JSType;
import com.google.javascript.rhino.testing.BaseJSTypeTestCase;
import com.google.javascript.rhino.testing.TestErrorReporter;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class ParserTest extends BaseJSTypeTestCase {
  private static final String TRAILING_COMMA_MESSAGE =
      "Trailing comma is not legal in an ECMA-262 object initializer";

  private static final String MISSING_GT_MESSAGE =
      "Bad type annotation. missing closing >" + BAD_TYPE_WIKI_LINK;

  private static final String UNNECESSARY_BRACES_MESSAGE =
      "Bad type annotation. braces are not required here" + BAD_TYPE_WIKI_LINK;

  private static final String NAME_NOT_RECOGNIZED_MESSAGE =
      "name not recognized due to syntax error.";

  private static final String UNLABELED_BREAK = "unlabelled break must be inside loop or switch";

  private static final String UNEXPECTED_CONTINUE = "continue must be inside loop";

  private static final String UNEXPECTED_RETURN = "return must be inside function";

  private static final String UNEXPECTED_LABELLED_CONTINUE =
      "continue can only use labels of iteration statements";

  private static final String UNEXPECTED_YIELD = "yield must be inside generator function";

  private static final String UNEXPECTED_AWAIT = "await must be inside asynchronous function";

  private static final String UNDEFINED_LABEL = "undefined label";

  private static final String HTML_COMMENT_WARNING =
      """
      In some cases, '<!--' and '-->' are treated as a '//' for legacy reasons. \
      Removing this from your code is safe for all browsers currently in use.\
      """;
  private static final String INVALID_ASSIGNMENT_TARGET = "invalid assignment target";

  private static final String SEMICOLON_EXPECTED = "Semi-colon expected";

  private static final String INVALID_PRIVATE_ID =
      "Private identifiers may not be used in this context";
  private static final String PRIVATE_FIELD_NOT_DEFINED =
      "Private fields must be declared in an enclosing class";
  private static final String PRIVATE_METHOD_NOT_DEFINED =
      "Private methods must be declared in an enclosing class";
  private static final String PRIVATE_FIELD_DELETED = "Private fields cannot be deleted";

  private LanguageMode mode;
  private JsDocParsing parsingMode;
  private Config.StrictMode strictMode;
  private boolean isIdeMode = false;
  private FeatureSet expectedFeatures;

  @Before
  public void setUp() throws Exception {
    mode = LanguageMode.UNSUPPORTED;
    parsingMode = JsDocParsing.INCLUDE_DESCRIPTIONS_NO_WHITESPACE;
    strictMode = STRICT;
    isIdeMode = false;
    expectedFeatures = FeatureSet.BARE_MINIMUM;
  }

  @Test
  public void testParseUnescapedLineSep() {
    parse("`\u2028`;");

    expectFeatures(Feature.UNESCAPED_UNICODE_LINE_OR_PARAGRAPH_SEP);
    parse("\"\u2028\";");
    parse("'\u2028';");
  }

  @Test
  public void testParseUnescapedParagraphSep() {
    parse("`\u2029`;");

    expectFeatures(Feature.UNESCAPED_UNICODE_LINE_OR_PARAGRAPH_SEP);
    parse("\"\u2029\";");
    parse("'\u2029';");
  }

  @Test
  public void testOptionalCatchBinding() {
    expectFeatures(Feature.OPTIONAL_CATCH_BINDING);

    parse("try {} catch {}");
    parse("try {} catch {} finally {}");
  }

  @Test
  public void testOptionalCatchBindingSourceInfo() {
    expectFeatures(Feature.OPTIONAL_CATCH_BINDING);

    Node result = parse("try {} catch     {}");
    Node catchNode = result.getFirstFirstChild().getNext().getFirstChild();
    assertNode(catchNode).hasToken(Token.CATCH);
    Node emptyNode = catchNode.getFirstChild();
    assertNode(emptyNode).hasToken(Token.EMPTY).hasLength(5); // The length matches the whitespace.
  }

  @Test
  public void testExponentOperator() {
    parseError("-x**y", "Unary operator '-' requires parentheses before '**'");

    expectFeatures(Feature.EXPONENT_OP);

    parse("x**y");
    // Parentheses are required for disambiguation when a unary expression is desired as
    // the left operand.
    parse("-(x**y)");
    parse("(-x)**y");
    // Parens are not required for unary operator on the right operand
    parse("x**-y");
    parse("x/y**z");

    parse("2 ** 3 > 3");

    mode = LanguageMode.ECMASCRIPT_2015;
    strictMode = SLOPPY;
    parseWarning("x**y", requiresLanguageModeMessage(Feature.EXPONENT_OP));
  }

  @Test
  public void testExponentAssignmentOperator() {
    expectFeatures(Feature.EXPONENT_OP);
    parse("x**=y;");

    mode = LanguageMode.ECMASCRIPT_2015;
    strictMode = SLOPPY;
    parseWarning("x**=y;", requiresLanguageModeMessage(Feature.EXPONENT_OP));
  }

  @Test
  public void testFunction() {
    parse("var f = function(x,y,z) { return 0; }");
    parse("function f(x,y,z) { return 0; }");

    isIdeMode = true;
    parseError("function f(x y z) {}", "',' expected");
  }

  @Test
  public void testFunctionTrailingComma() {
    parse("var f = function(x,y,z,) {}");
    parse("function f(x,y,z,) {}");
  }

  @Test
  public void testFunctionTrailingCommaPreES8() {
    mode = LanguageMode.ECMASCRIPT_2016;

    parseError("var f = function(x,y,z,) {}", "Invalid trailing comma in formal parameter list");
    parseError("function f(x,y,z,) {}", "Invalid trailing comma in formal parameter list");
  }

  @Test
  public void testFunctionExtraTrailingComma() {
    parseError("var f = function(x,y,z,,) {}", "')' expected");
    parseError("function f(x,y,z,,) {}", "')' expected");
  }

  @Test
  public void testCallTrailingComma() {
    parse("f(x,y,z,);");
  }

  @Test
  public void testCallTrailingCommaPreES8() {
    mode = LanguageMode.ECMASCRIPT_2016;

    parseError("f(x,y,z,);", "Invalid trailing comma in arguments list");
  }

  @Test
  public void testCallExtraTrailingComma() {
    parseError("f(x,y,z,,);", "')' expected");
  }

  @Test
  public void testTrailingCommaArray() {
    Node arrayLit =
        parse("[1, 2, 3,]") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild(); // ARRAYLIT
    assertNode(arrayLit).hasType(Token.ARRAYLIT);
    assertNode(arrayLit).hasTrailingComma();
  }

  @Test
  public void testTrailingCommaObject() {
    Node objectlit =
        parse("var obj = {a:1,b:2,};") // SCRIPT
            .getOnlyChild() // VAR
            .getOnlyChild() // NAME
            .getOnlyChild(); // OBJECTLIT
    assertNode(objectlit).hasType(Token.OBJECTLIT);
    assertNode(objectlit).hasTrailingComma();
  }

  @Test
  public void testTrailingCommaParamList() {
    Node paramList =
        parse("function f(a, b,) {}") // SCRIPT
            .getOnlyChild() // FUNCTION
            .getSecondChild(); // PARAM_LIST
    assertNode(paramList).hasType(Token.PARAM_LIST);
    assertNode(paramList).hasTrailingComma();
  }

  @Test
  public void testTrailingCommaParamListArrow() {
    Node paramList =
        parse("x = (a, b,) => {};") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild() // ASSIGN
            .getSecondChild() // FUNCTION
            .getSecondChild(); // PARAM_LIST
    assertNode(paramList).hasType(Token.PARAM_LIST);
    assertNode(paramList).hasTrailingComma();

    paramList =
        parse("x = (a,) => {};") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild() // ASSIGN
            .getSecondChild() // FUNCTION
            .getSecondChild(); // PARAM_LIST
    assertNode(paramList).hasType(Token.PARAM_LIST);
    assertNode(paramList).hasTrailingComma();

    paramList =
        parse("x = async (a, b,) => {};") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild() // ASSIGN
            .getSecondChild() // FUNCTION
            .getSecondChild(); // PARAM_LIST
    assertNode(paramList).hasType(Token.PARAM_LIST);
    assertNode(paramList).hasTrailingComma();
  }

  @Test
  public void testTrailingCommaCallNode() {
    Node call =
        parse("f(a, b,);") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild(); // CALL
    assertNode(call).hasType(Token.CALL);
    assertNode(call).hasTrailingComma();
  }

  @Test
  public void testTrailingCommaNewNode() {
    Node call =
        parse("new f(a, b,);") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild(); // NEW
    assertNode(call).hasType(Token.NEW);
    assertNode(call).hasTrailingComma();
  }

  @Test
  public void testTrailingCommaOptChainCallNode() {
    Node call =
        parse("f?.(a, b,);") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild(); // OPTCHAIN_CALL
    assertNode(call).hasType(Token.OPTCHAIN_CALL);
    assertNode(call).hasTrailingComma();
  }

  @Test
  public void testArrayWithElisions() {
    Node arrayLit =
        parse("[  , 1,   ,]") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild(); // ARRAYLIT

    assertNode(arrayLit).hasType(Token.ARRAYLIT);
    assertNode(arrayLit).hasXChildren(3);

    assertNode(arrayLit.getFirstChild()).hasType(Token.EMPTY);
    assertNode(arrayLit.getFirstChild()).hasCharno(3);
    assertNode(arrayLit.getFirstChild()).hasLength(0);

    assertNode(arrayLit.getSecondChild()).hasToken(Token.NUMBER);

    assertNode(arrayLit.getChildAtIndex(2)).hasType(Token.EMPTY);
    assertNode(arrayLit.getChildAtIndex(2)).hasLength(0);
    assertNode(arrayLit.getChildAtIndex(2)).hasCharno(10);

    assertNode(arrayLit).hasTrailingComma();
  }

  @Test
  public void testWhile() {
    parse("while(1) { break; }");
  }

  @Test
  public void testNestedWhile() {
    parse("while(1) { while(1) { break; } }");
  }

  @Test
  public void testBreak() {
    parseError("break;", UNLABELED_BREAK);
  }

  @Test
  public void testContinue() {
    parseError("continue;", UNEXPECTED_CONTINUE);
  }

  @Test
  public void testBreakCrossFunction() {
    parseError("while(1) { var f = function() { break; } }", UNLABELED_BREAK);
  }

  @Test
  public void testBreakCrossFunctionInFor() {
    parseError("while(1) {for(var f = function () { break; };;) {}}", UNLABELED_BREAK);
  }

  @Test
  public void testBreakInForOf() {
    strictMode = SLOPPY;
    expectFeatures(Feature.FOR_OF);
    parse(
        """
        for (var x of [1, 2, 3]) {
          if (x == 2) break;
        }
        """);
  }

  @Test
  public void testContinueToSwitch() {
    parseError("switch(1) {case(1): continue; }", UNEXPECTED_CONTINUE);
  }

  @Test
  public void testContinueToSwitchWithNoCases() {
    parse("switch(1){}");
  }

  @Test
  public void testContinueToSwitchWithTwoCases() {
    parseError("switch(1){case(1):break;case(2):continue;}", UNEXPECTED_CONTINUE);
  }

  @Test
  public void testContinueToSwitchWithDefault() {
    parseError("switch(1){case(1):break;case(2):default:continue;}", UNEXPECTED_CONTINUE);
  }

  @Test
  public void testContinueToLabelSwitch() {
    parseError("while(1) {a: switch(1) {case(1): continue a; }}", UNEXPECTED_LABELLED_CONTINUE);
  }

  @Test
  public void testContinueOutsideSwitch() {
    parse("b: while(1) { a: switch(1) { case(1): continue b; } }");
  }

  @Test
  public void testContinueNotCrossFunction1() {
    parse("a:switch(1){case(1):var f = function(){a:while(1){continue a;}}}");
  }

  @Test
  public void testContinueNotCrossFunction2() {
    parseError(
        "a:switch(1){case(1):var f = function(){while(1){continue a;}}}",
        UNDEFINED_LABEL + " \"a\"");
  }

  @Test
  public void testContinueInForOf() {
    strictMode = SLOPPY;
    expectFeatures(Feature.FOR_OF);
    parse(
        """
        for (var x of [1, 2, 3]) {
          if (x == 2) continue;
        }
        """);
  }

  /**
   * @bug 19100575
   */
  @Test
  public void testVarSourceLocations() {
    isIdeMode = true;

    Node n = parse("var x, y = 1;");
    Node var = n.getFirstChild();
    assertNode(var).hasType(Token.VAR);

    Node x = var.getFirstChild();
    assertNode(x).hasType(Token.NAME);
    assertNode(x).hasCharno("var ".length());

    Node y = x.getNext();
    assertNode(y).hasType(Token.NAME);
    assertNode(y).hasCharno("var x, ".length());
    assertNode(y).hasLength("y = 1".length());
  }

  @Test
  public void testSourceLocationsNonAscii() {
    Node n = parse("'안녕세계!'");
    Node exprResult = n.getFirstChild();
    Node string = exprResult.getFirstChild();
    assertNode(string).hasType(Token.STRINGLIT);
    assertNode(string).hasLength(7); // 2 quotes, plus 5 characters
  }

  @Test
  public void testNumericSeparatorOnDecimal() {
    Node result = parse("1_000_000;");
    expectFeatures(Feature.NUMERIC_SEPARATOR);
    assertNode(result).hasType(Token.SCRIPT);
    Node exprResult = result.getOnlyChild();
    assertNode(exprResult).hasType(Token.EXPR_RESULT);
    Node numberNode = exprResult.getOnlyChild();
    assertNode(numberNode).isNumber(1000000);
  }

  @Test
  public void testNumericSeparatorOnBinary() {
    Node result = parse("0b1_0000;");
    expectFeatures(Feature.NUMERIC_SEPARATOR);
    assertNode(result).hasType(Token.SCRIPT);
    Node exprResult = result.getOnlyChild();
    assertNode(exprResult).hasType(Token.EXPR_RESULT);
    Node numberNode = exprResult.getOnlyChild();
    assertNode(numberNode).isNumber(16);
  }

  @Test
  public void testNumericSeparatorOnOctal() {
    Node result = parse("0o01_00;");
    expectFeatures(Feature.NUMERIC_SEPARATOR);
    assertNode(result).hasType(Token.SCRIPT);
    Node exprResult = result.getOnlyChild();
    assertNode(exprResult).hasType(Token.EXPR_RESULT);
    Node numberNode = exprResult.getOnlyChild();
    assertNode(numberNode).isNumber(64);
  }

  @Test
  public void testNumericSeparatorOnHex() {
    Node result = parse("0x01_01");
    expectFeatures(Feature.NUMERIC_SEPARATOR);
    assertNode(result).hasType(Token.SCRIPT);
    Node exprResult = result.getOnlyChild();
    assertNode(exprResult).hasType(Token.EXPR_RESULT);
    Node numberNode = exprResult.getOnlyChild();
    assertNode(numberNode).isNumber(257);
  }

  @Test
  public void testNumericSeparatorOnBigInt() {
    Node bigint =
        parse("1_000n") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild(); // BIGINT
    assertNode(bigint).hasType(Token.BIGINT);
    assertNode(bigint).isBigInt(new BigInteger("1000"));
  }

  @Test
  public void testTrailingNumericSeparator() {
    parseError("1000_", "Trailing numeric separator");
    parseError("0b0001_", "Trailing numeric separator");
    parseError("0o100_", "Trailing numeric separator");
    parseError("0x0F_", "Trailing numeric separator");
    parseError("1000_n", "Trailing numeric separator");
    parseError("0b0001_n", "Trailing numeric separator");
    parseError("0o100_n", "Trailing numeric separator");
    parseError("0x0F_n", "Trailing numeric separator");
  }

  @Test
  public void testNumericSeparatorWarning() {
    mode = LanguageMode.ECMASCRIPT_2020;
    parseWarning("1_000", requiresLanguageModeMessage(Feature.NUMERIC_SEPARATOR));
  }

  @Test
  public void testReturn() {
    parse("function foo() { return 1; }");
    parseError("return;", UNEXPECTED_RETURN);
    parseError("return 1;", UNEXPECTED_RETURN);
  }

  @Test
  public void testThrow() {
    parse("throw Error();");
    parse("throw new Error();");
    parse("throw '';");
    parseError("throw;", "semicolon/newline not allowed after 'throw'");
    parseError("throw\nError();", "semicolon/newline not allowed after 'throw'");
  }

  @Test
  public void testLabel1() {
    parse("foo:bar");
  }

  @Test
  public void testLabel2() {
    parse("{foo:bar}");
  }

  @Test
  public void testLabel3() {
    parse("foo:bar:baz");
  }

  @Test
  public void testDuplicateLabelWithoutBraces() {
    parseError("foo:foo:bar", "Duplicate label \"foo\"");
  }

  @Test
  public void testDuplicateLabelWithBraces() {
    parseError("foo:{bar;foo:baz}", "Duplicate label \"foo\"");
  }

  @Test
  public void testDuplicateLabelWithFor() {
    parseError("foo:for(;;){foo:bar}", "Duplicate label \"foo\"");
  }

  @Test
  public void testNonDuplicateLabelSiblings() {
    parse("foo:1;foo:2");
  }

  @Test
  public void testNonDuplicateLabelCrossFunction() {
    parse("foo:(function(){foo:2})");
  }

  @Test
  public void testLabeledFunctionDeclaration() {
    parseError(
        "foo:function f() {}",
        "Lexical declarations are only allowed at top level or inside a block.");
  }

  @Test
  public void testLabeledClassDeclaration() {
    parseError(
        "foo:class Foo {}",
        "Lexical declarations are only allowed at top level or inside a block.");
  }

  @Test
  public void testLabeledLetDeclaration() {
    parseError(
        "foo: let x = 0;", "Lexical declarations are only allowed at top level or inside a block.");
  }

  @Test
  public void testLabeledConstDeclaration() {
    parseError(
        "foo: const x = 0;",
        "Lexical declarations are only allowed at top level or inside a block.");
  }

  @Test
  public void testMethodNamedStatic() {
    parse("class C { static(a, b) {} }");
  }

  @Test
  public void testLinenoCharnoAssign1() {
    Node assign = parse("a = b").getFirstFirstChild();

    assertNode(assign).hasType(Token.ASSIGN);
    assertNode(assign).hasLineno(1);
    assertNode(assign).hasCharno(0);
  }

  @Test
  public void testLinenoCharnoAssign2() {
    Node assign = parse("\n a.g.h.k    =  45").getFirstFirstChild();

    assertNode(assign).hasType(Token.ASSIGN);
    assertNode(assign).hasLineno(2);
    assertNode(assign).hasCharno(1);
  }

  @Test
  public void testLinenoCharnoCall() {
    Node call = parse("\n foo(123);").getFirstFirstChild();

    assertNode(call).hasType(Token.CALL);
    assertNode(call).hasLineno(2);
    assertNode(call).hasCharno(1);
  }

  @Test
  public void testLinenoCharnoGetProp1() {
    Node getprop = parse("\n foo.bar").getFirstFirstChild();

    assertNode(getprop).hasType(Token.GETPROP);
    assertNode(getprop).hasLineno(2);
    assertNode(getprop).hasCharno(5);
    assertNode(getprop).hasStringThat().isEqualTo("bar");
  }

  @Test
  public void testLinenoCharnoGetProp2() {
    Node getprop = parse("\n foo.\nbar").getFirstFirstChild();

    assertNode(getprop).hasType(Token.GETPROP);
    assertNode(getprop).hasLineno(3);
    assertNode(getprop).hasCharno(0);
    assertNode(getprop).hasStringThat().isEqualTo("bar");
  }

  @Test
  public void testLinenoCharnoGetelem1() {
    Node call = parse("\n foo[123]").getFirstFirstChild();

    assertNode(call).hasType(Token.GETELEM);
    assertNode(call).hasLineno(2);
    assertNode(call).hasCharno(1);
  }

  @Test
  public void testLinenoCharnoGetelem2() {
    Node call = parse("\n   \n foo()[123]").getFirstFirstChild();

    assertNode(call).hasType(Token.GETELEM);
    assertNode(call).hasLineno(3);
    assertNode(call).hasCharno(1);
  }

  @Test
  public void testLinenoCharnoGetelem3() {
    Node call = parse("\n   \n (8 + kl)[123]").getFirstFirstChild();

    assertNode(call).hasType(Token.GETELEM);
    assertNode(call).hasLineno(3);
    assertNode(call).hasCharno(1);
  }

  @Test
  public void testLinenoCharnoForComparison() {
    Node lt = parse("for (; i < j;){}").getFirstChild().getSecondChild();

    assertNode(lt).hasType(Token.LT);
    assertNode(lt).hasLineno(1);
    assertNode(lt).hasCharno(7);
  }

  @Test
  public void testLinenoCharnoHook() {
    Node n = parse("\n a ? 9 : 0").getFirstFirstChild();

    assertNode(n).hasType(Token.HOOK);
    assertNode(n).hasLineno(2);
    assertNode(n).hasCharno(1);
  }

  @Test
  public void testLinenoCharnoArrayLiteral() {
    Node n = parse("\n  [8, 9]").getFirstFirstChild();

    assertNode(n).hasType(Token.ARRAYLIT);
    assertNode(n).hasLineno(2);
    assertNode(n).hasCharno(2);

    n = n.getFirstChild();

    assertNode(n).hasType(Token.NUMBER);
    assertNode(n).hasLineno(2);
    assertNode(n).hasCharno(3);

    n = n.getNext();

    assertNode(n).hasType(Token.NUMBER);
    assertNode(n).hasLineno(2);
    assertNode(n).hasCharno(6);
  }

  @Test
  public void testLinenoCharnoObjectLiteral() {
    Node n = parse("\n\n var a = {a:0\n,b :1};").getFirstFirstChild().getFirstChild();

    assertNode(n).hasType(Token.OBJECTLIT);
    assertNode(n).hasLineno(3);
    assertNode(n).hasCharno(9);

    Node key = n.getFirstChild();

    assertNode(key).hasType(Token.STRING_KEY);
    assertNode(key).hasLineno(3);
    assertNode(key).hasCharno(10);

    Node value = key.getFirstChild();

    assertNode(value).hasType(Token.NUMBER);
    assertNode(value).hasLineno(3);
    assertNode(value).hasCharno(12);

    key = key.getNext();

    assertNode(key).hasType(Token.STRING_KEY);
    assertNode(key).hasLineno(4);
    assertNode(key).hasCharno(1);

    value = key.getFirstChild();

    assertNode(value).hasType(Token.NUMBER);
    assertNode(value).hasLineno(4);
    assertNode(value).hasCharno(4);
  }

  @Test
  public void testLinenoCharnoObjectLiteralMemberFunction() {
    Node n = parse("var a = {\n fn() {} };").getFirstFirstChild().getFirstChild();

    assertNode(n).hasType(Token.OBJECTLIT);
    assertNode(n).hasLineno(1);
    assertNode(n).hasCharno(8);

    // fn() {}
    Node key = n.getFirstChild();

    assertNode(key).hasType(Token.MEMBER_FUNCTION_DEF);
    assertNode(key).hasLineno(2);
    assertNode(key).hasCharno(1);
    assertNode(key).hasLength(2); // "fn"

    Node value = key.getFirstChild();

    assertNode(value).hasType(Token.FUNCTION);
    assertNode(value).hasLineno(2);
    assertNode(value).hasCharno(1);
    assertNode(value).hasLength(7); // "fn() {}"
  }

  @Test
  public void testLinenoCharnoEs6Class() {
    Node n = parse("class C {\n  fn1() {}\n  static fn2() {}\n };").getFirstChild();

    assertNode(n).hasType(Token.CLASS);
    assertNode(n).hasLineno(1);
    assertNode(n).hasCharno(0);

    Node members = NodeUtil.getClassMembers(n);
    assertNode(members).hasType(Token.CLASS_MEMBERS);

    // fn1 () {}
    Node memberFn = members.getFirstChild();

    assertNode(memberFn).hasType(Token.MEMBER_FUNCTION_DEF);
    assertNode(memberFn).hasLineno(2);
    assertNode(memberFn).hasCharno(2);
    assertNode(memberFn).hasLength(3); // "fn"

    Node fn = memberFn.getFirstChild();
    assertNode(fn).hasType((Token.FUNCTION));
    assertNode(fn).hasLineno(2);
    assertNode(fn).hasCharno(2);
    assertNode(fn).hasLength(8); // "fn1() {}"

    // static fn2() {}
    memberFn = memberFn.getNext();

    assertNode(memberFn).hasType(Token.MEMBER_FUNCTION_DEF);
    assertNode(memberFn).hasLineno(3);
    assertNode(memberFn).hasCharno(9);
    assertNode(memberFn).hasLength(3); // "fn2"
    assertNode(memberFn).isStatic();

    fn = memberFn.getFirstChild();
    assertNode(fn).hasType((Token.FUNCTION));
    assertNode(fn).hasLineno(3);
    assertNode(fn).hasCharno(2);
    assertNode(fn).hasLength(15); // "static fn2() {}"
  }

  @Test
  public void testLinenoCharnoAdd() {
    testLinenoCharnoBinop("+");
  }

  @Test
  public void testLinenoCharnoSub() {
    testLinenoCharnoBinop("-");
  }

  @Test
  public void testLinenoCharnoMul() {
    testLinenoCharnoBinop("*");
  }

  @Test
  public void testLinenoCharnoDiv() {
    testLinenoCharnoBinop("/");
  }

  @Test
  public void testLinenoCharnoMod() {
    testLinenoCharnoBinop("%");
  }

  @Test
  public void testLinenoCharnoShift() {
    testLinenoCharnoBinop("<<");
  }

  @Test
  public void testLinenoCharnoBinaryAnd() {
    testLinenoCharnoBinop("&");
  }

  @Test
  public void testLinenoCharnoAnd() {
    testLinenoCharnoBinop("&&");
  }

  @Test
  public void testLinenoCharnoBinaryOr() {
    testLinenoCharnoBinop("|");
  }

  @Test
  public void testLinenoCharnoOr() {
    testLinenoCharnoBinop("||");
  }

  @Test
  public void testLinenoCharnoLt() {
    testLinenoCharnoBinop("<");
  }

  @Test
  public void testLinenoCharnoLe() {
    testLinenoCharnoBinop("<=");
  }

  @Test
  public void testLinenoCharnoGt() {
    testLinenoCharnoBinop(">");
  }

  @Test
  public void testLinenoCharnoGe() {
    testLinenoCharnoBinop(">=");
  }

  private void testLinenoCharnoBinop(String binop) {
    Node op = parse("var a = 89 " + binop + " 76;").getFirstChild().getFirstFirstChild();

    assertNode(op).hasLineno(1);
    assertNode(op).hasCharno(8);
  }

  @Test
  public void testLinenoCharnoNegativeNumber() {
    Node n = parse("var x = -1000").getFirstChild().getFirstFirstChild();

    assertNode(n).hasLineno(1);
    assertNode(n).hasCharno(8);
    assertNode(n).hasLength(5);
  }

  @Test
  public void testJSDocAttachment1() {
    Node varNode = parse("/** @type {number} */var a;").getFirstChild();

    // VAR
    assertNode(varNode).hasType(Token.VAR);
    assertNodeHasJSDocInfoWithJSType(varNode, NUMBER_TYPE);

    // VAR NAME
    Node varNameNode = varNode.getFirstChild();
    assertNode(varNameNode).hasType(Token.NAME);
    assertThat(varNameNode.getJSDocInfo()).isNull();

    strictMode = SLOPPY;
    expectFeatures(Feature.LET_DECLARATIONS);

    Node letNode = parse("/** @type {number} */let a;").getFirstChild();

    // LET
    assertNode(letNode).hasType(Token.LET);
    assertNodeHasJSDocInfoWithJSType(letNode, NUMBER_TYPE);

    // LET NAME
    Node letNameNode = letNode.getFirstChild();
    assertNode(letNameNode).hasType(Token.NAME);
    assertThat(letNameNode.getJSDocInfo()).isNull();

    expectFeatures(Feature.CONST_DECLARATIONS);
    Node constNode = parse("/** @type {number} */const a = 0;").getFirstChild();

    // CONST
    assertNode(constNode).hasType(Token.CONST);
    assertNodeHasJSDocInfoWithJSType(constNode, NUMBER_TYPE);

    // CONST NAME
    Node constNameNode = constNode.getFirstChild();
    assertNode(constNameNode).hasType(Token.NAME);
    assertThat(constNameNode.getJSDocInfo()).isNull();
  }

  @Test
  public void testJSDocAttachment2() {
    Node varNode = parse("/** @type {number} */var a,b;").getFirstChild();

    // VAR
    assertNode(varNode).hasType(Token.VAR);
    assertNodeHasJSDocInfoWithJSType(varNode, NUMBER_TYPE);

    // First NAME
    Node nameNode1 = varNode.getFirstChild();
    assertNode(nameNode1).hasType(Token.NAME);
    assertThat(nameNode1.getJSDocInfo()).isNull();

    // Second NAME
    Node nameNode2 = nameNode1.getNext();
    assertNode(nameNode2).hasType(Token.NAME);
    assertThat(nameNode2.getJSDocInfo()).isNull();
  }

  @Test
  public void testJSDocAttachment3() {
    Node assignNode = parse("/** @type {number} */goog.FOO = 5;").getFirstFirstChild();
    assertNode(assignNode).hasType(Token.ASSIGN);
    assertNodeHasJSDocInfoWithJSType(assignNode, NUMBER_TYPE);
  }

  @Test
  public void testJSDocAttachment4() {
    Node varNode = parse("var a, /** @define {number} */ b = 5;").getFirstChild();

    // ASSIGN
    assertNode(varNode).hasType(Token.VAR);
    assertThat(varNode.getJSDocInfo()).isNull();

    // a
    Node a = varNode.getFirstChild();
    assertThat(a.getJSDocInfo()).isNull();

    // b
    Node b = a.getNext();
    JSDocInfo info = b.getJSDocInfo();
    assertThat(info).isNotNull();
    assertThat(info.isDefine()).isTrue();
    assertTypeEquals(NUMBER_TYPE, info.getType());
  }

  @Test
  public void testJSDocAttachment5() {
    Node varNode =
        parse("var /** @type {number} */a, /** @define {number} */b = 5;").getFirstChild();

    // ASSIGN
    assertNode(varNode).hasType(Token.VAR);
    assertThat(varNode.getJSDocInfo()).isNull();

    // a
    Node a = varNode.getFirstChild();
    assertThat(a.getJSDocInfo()).isNotNull();
    JSDocInfo info = a.getJSDocInfo();
    assertThat(info).isNotNull();
    assertThat(info.isDefine()).isFalse();
    assertTypeEquals(NUMBER_TYPE, info.getType());

    // b
    Node b = a.getNext();
    info = b.getJSDocInfo();
    assertThat(info).isNotNull();
    assertThat(info.isDefine()).isTrue();
    assertTypeEquals(NUMBER_TYPE, info.getType());
  }

  /**
   * Tests that a JSDoc comment in an unexpected place of the code does not propagate to following
   * code due to {@link JSDocInfo} aggregation.
   */
  @Test
  public void testJSDocAttachment6() {
    Node functionNode =
        parse(
                """
                var a = /** @param {number} index */5;
                /** @return {boolean} */function f(index){}
                """)
            .getSecondChild();

    assertNode(functionNode).hasType(Token.FUNCTION);
    JSDocInfo info = functionNode.getJSDocInfo();
    assertThat(info).isNotNull();
    assertThat(info.hasParameter("index")).isFalse();
    assertThat(info.hasReturnType()).isTrue();
    assertTypeEquals(BOOLEAN_TYPE, info.getReturnType());
  }

  @Test
  public void testJSDocAttachment7() {
    Node varNode = parse("/** */var a;").getFirstChild();

    // VAR
    assertNode(varNode).hasType(Token.VAR);

    // NAME
    Node nameNode = varNode.getFirstChild();
    assertNode(nameNode).hasType(Token.NAME);
    assertThat(nameNode.getJSDocInfo()).isNull();
  }

  @Test
  public void testJSDocAttachment8() {
    Node varNode = parse("/** x */var a;").getFirstChild();

    // VAR
    assertNode(varNode).hasType(Token.VAR);

    // NAME
    Node nameNode = varNode.getFirstChild();
    assertNode(nameNode).hasType(Token.NAME);
    assertThat(nameNode.getJSDocInfo()).isNull();
  }

  @Test
  public void testJSDocAttachment9() {
    Node varNode = parse("/** \n x */var a;").getFirstChild();

    // VAR
    assertNode(varNode).hasType(Token.VAR);

    // NAME
    Node nameNode = varNode.getFirstChild();
    assertNode(nameNode).hasType(Token.NAME);
    assertThat(nameNode.getJSDocInfo()).isNull();
  }

  @Test
  public void testJSDocAttachment10() {
    Node varNode = parse("/** x\n */var a;").getFirstChild();

    // VAR
    assertNode(varNode).hasType(Token.VAR);

    // NAME
    Node nameNode = varNode.getFirstChild();
    assertNode(nameNode).hasType(Token.NAME);
    assertThat(nameNode.getJSDocInfo()).isNull();
  }

  @Test
  public void testJSDocAttachment11() {
    Node varNode = parse("/** @type {{x : number, 'y' : string, z}} */var a;").getFirstChild();

    // VAR
    assertNode(varNode).hasType(Token.VAR);
    assertNodeHasJSDocInfoWithJSType(
        varNode,
        createRecordTypeBuilder()
            .addProperty("x", NUMBER_TYPE, null)
            .addProperty("y", STRING_TYPE, null)
            .addProperty("z", UNKNOWN_TYPE, null)
            .build());

    // NAME
    Node nameNode = varNode.getFirstChild();
    assertNode(nameNode).hasType(Token.NAME);
    assertThat(nameNode.getJSDocInfo()).isNull();
  }

  @Test
  public void testJSDocAttachment12() {
    Node varNode = parse("var a = {/** @type {Object} */ b: c};").getFirstChild();
    Node objectLitNode = varNode.getFirstFirstChild();
    assertNode(objectLitNode).hasType(Token.OBJECTLIT);
    assertThat(objectLitNode.getFirstChild().getJSDocInfo()).isNotNull();
  }

  @Test
  public void testJSDocAttachment13() {
    Node varNode = parse("/** foo */ var a;").getFirstChild();
    assertThat(varNode.getJSDocInfo()).isNotNull();
  }

  @Test
  public void testJSDocAttachment14() {
    Node varNode = parse("/** */ var a;").getFirstChild();
    assertThat(varNode.getJSDocInfo()).isNull();
  }

  @Test
  public void testJSDocAttachment15() {
    Node varNode = parse("/** \n * \n */ var a;").getFirstChild();
    assertThat(varNode.getJSDocInfo()).isNull();
  }

  @Test
  public void testJSDocAttachment16() {
    Node exprCall = parse("/** @private */ x(); function f() {};").getFirstChild();
    assertNode(exprCall).hasType(Token.EXPR_RESULT);
    assertThat(exprCall.getNext().getJSDocInfo()).isNull();
    assertThat(exprCall.getFirstChild().getJSDocInfo()).isNotNull();
  }

  @Test
  public void testJSDocAttachmentForCastFnCall() {
    Node fn =
        parse(
                """
                function f() {
                  return /** @type {string} */ (g(1 /** @desc x */));
                };
                """)
            .getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);
    Node cast = fn.getLastChild().getFirstFirstChild();
    assertNode(cast).hasType(Token.CAST);
  }

  @Test
  public void testJSDocAttachmentForCastName() {
    Node fn =
        parse(
                """
                function f() {
                  var x = /** @type {string} */ (y);
                };
                """)
            .getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);
    Node cast = fn.getLastChild().getFirstFirstChild().getFirstChild();
    assertNode(cast).hasType(Token.CAST);
  }

  @Test
  public void testJSDocAttachmentForCastLhs() {
    Node expr = parse("/** some jsdoc */ (/** @type {?} */ (a)).b = 0;").getOnlyChild();
    Node lhs = expr.getFirstFirstChild(); // child is ASSIGN, grandchild is the GETPROP
    Node cast = lhs.getFirstChild();
    assertNode(cast).hasToken(Token.CAST);
    assertThat(cast.getJSDocInfo()).isNotNull();
    // TODO(b/123955687): this should be true
    assertThat(cast.getJSDocInfo().hasType()).isFalse();
  }

  @Test
  public void testJSDocAttachment19() {
    Node fn =
        parse(
                """
                function f() {
                  /** @type {string} */
                  return;
                };
                """)
            .getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node ret = fn.getLastChild().getFirstChild();
    assertNode(ret).hasType(Token.RETURN);
    assertThat(ret.getJSDocInfo()).isNotNull();
  }

  @Test
  public void testJSDocAttachment20() {
    Node fn =
        parse(
                """
                function f() {
                  /** @type {string} */
                  if (true) return;
                };
                """)
            .getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node ret = fn.getLastChild().getFirstChild();
    assertNode(ret).hasType(Token.IF);
    assertThat(ret.getJSDocInfo()).isNotNull();
  }

  @Test
  public void testJSDocAttachment21() {
    strictMode = SLOPPY;
    expectFeatures(Feature.CONST_DECLARATIONS);
    parse("/** @param {string} x */ const f = function() {};");

    expectFeatures(Feature.LET_DECLARATIONS);
    parse("/** @param {string} x */ let f = function() {};");
  }

  // Tests that JSDoc gets attached to the children of export nodes, and there are no warnings.
  // See https://github.com/google/closure-compiler/issues/781
  @Test
  public void testJSDocAttachment22() {
    strictMode = SLOPPY;
    expectFeatures(Feature.MODULES);

    Node n = parse("/** @param {string} x */ export function f(x) {};");
    Node export = n.getFirstFirstChild();

    assertNode(export).hasType(Token.EXPORT);
    assertThat(export.getJSDocInfo()).isNull();
    assertThat(export.getFirstChild().getJSDocInfo()).isNotNull();
    assertThat(export.getFirstChild().getJSDocInfo().hasParameter("x")).isTrue();
  }

  @Test
  public void testInlineJSDocAttachmentToVar() {
    Node letNode = parse("let /** string */ x = 'a';").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    JSDocInfo info = letNode.getFirstChild().getJSDocInfo();
    assertThat(info).isNotNull();
    assertTypeEquals(STRING_TYPE, info.getType());
  }

  @Test
  public void testNodeInsideParens_simpleString() {
    Node exprRes = parse("('a')").getFirstChild();
    Node str = exprRes.getFirstChild();

    assertNode(exprRes).hasType(Token.EXPR_RESULT);
    assertNode(str).hasType(Token.STRINGLIT);
    assertThat(str.getIsParenthesized()).isTrue();
  }

  @Test
  public void testNodesInsideParens() {
    Node andExprRes = parse("((x&&y) && z)").getFirstChild();
    assertNode(andExprRes).hasType(Token.EXPR_RESULT);

    //  The expression `((x&&y) && z)` is marked as parenthesized
    Node andNode = andExprRes.getFirstChild();
    assertNode(andNode).hasType(Token.AND);
    assertThat(andNode.getIsParenthesized()).isTrue();

    // The expression `(x&&y)` is marked as parenthesized
    // These parens are still recorded despite not affecting the structure of the parent expression.
    Node xAndy = andNode.getFirstChild();
    assertNode(xAndy).hasType(Token.AND);
    assertThat(xAndy.getIsParenthesized()).isTrue();

    // inner nodes `x` and `y` of a `(x&&y)` aren't marked as parenthesized
    Node x = xAndy.getFirstChild();
    Node y = xAndy.getSecondChild();
    assertNode(x).hasType(Token.NAME);
    assertNode(y).hasType(Token.NAME);
    assertThat(x.getIsParenthesized()).isFalse();
    assertThat(y.getIsParenthesized()).isFalse();

    Node z = andNode.getSecondChild();
    assertNode(z).hasType(Token.NAME);
    assertThat(z.getIsParenthesized()).isFalse();
  }

  @Test
  public void testInlineNonJSDocCommentAttachmentToVar() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node letNode = parse("let /* blah */ x = 'a';").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    NonJSDocComment nonJSDocComment = letNode.getFirstChild().getNonJSDocComment();
    assertThat(nonJSDocComment).isNotNull();
    assertThat(nonJSDocComment.isInline()).isTrue();
    assertThat(nonJSDocComment.isEndingAsLineComment()).isFalse();
    assertThat(nonJSDocComment.getCommentString()).contains("/* blah */");
  }

  @Test
  public void testNoInlineNonJSDocCommentAttachmentWithoutParsingMode() {
    Node letNode = parse("let /* blah */ x = 'a';").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    NonJSDocComment nonJSDocComment = letNode.getFirstChild().getNonJSDocComment();
    assertThat(nonJSDocComment).isNull();
  }

  @Test
  public void testInlineJSDocAttachmentToObjPatNormalProp() {
    Node letNode =
        parse("let { normalProp: /** string */ normalPropTarget } = {};").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node objectPattern = destructuringLhs.getFirstChild();

    Node normalProp = objectPattern.getFirstChild();
    assertNode(normalProp).hasType(Token.STRING_KEY);
    Node normalPropTarget = normalProp.getOnlyChild();
    assertNodeHasJSDocInfoWithJSType(normalPropTarget, STRING_TYPE);
  }

  @Test
  public void testInlineNonJSDocCommentAttachmentToObjPatNormalProp() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node letNode = parse("let { normalProp: /* blah */ normalPropTarget } = {};").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node objectPattern = destructuringLhs.getFirstChild();

    Node normalProp = objectPattern.getFirstChild();
    assertNode(normalProp).hasType(Token.STRING_KEY);
    Node normalPropTarget = normalProp.getOnlyChild();
    assertThat(normalPropTarget.getNonJSDocCommentString()).contains("/* blah */");
  }

  @Test
  public void testNoInlineNonJSDocCommentAttachmentToObjWithoutParsingMode() {
    Node letNode = parse("let { normalProp: /* blah */ normalPropTarget } = {};").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node objectPattern = destructuringLhs.getFirstChild();

    Node normalProp = objectPattern.getFirstChild();
    assertNode(normalProp).hasType(Token.STRING_KEY);
    Node normalPropTarget = normalProp.getOnlyChild();
    assertThat(normalPropTarget.getNonJSDocComment()).isNull();
  }

  @Test
  public void testInlineJSDocAttachmentToObjPatNormalPropKey() {
    Node letNode = parse("let { /** string */ normalProp: normalProp } = {};").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node objectPattern = destructuringLhs.getFirstChild();

    Node normalProp = objectPattern.getFirstChild();
    assertNode(normalProp).hasType(Token.STRING_KEY);
    // TODO(bradfordcsmith): Putting the inline jsdoc on the key should be an error,
    //     because it isn't clear what that should mean.
    assertNodeHasNoJSDocInfo(normalProp);
  }

  @Test
  public void testInlineNonJSDocCommentAttachmentToObjPatNormalPropKey() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node letNode = parse("let { /* blah */ normalProp: normalProp } = {};").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node objectPattern = destructuringLhs.getFirstChild();

    Node normalPropKey = objectPattern.getFirstChild();
    assertNode(normalPropKey).hasType(Token.STRING_KEY);
    assertThat(normalPropKey.getNonJSDocCommentString()).contains("/* blah */");
  }

  @Test
  public void testInlineNonJSDocCommentAttachment_numberAsKey() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node script = parse("var { /*comment */ 3: x} = foo();");
    Node var = script.getFirstChild();
    Node destructuringLHS = var.getFirstChild();
    Node objLit = destructuringLHS.getFirstChild();
    Node three = objLit.getFirstChild();
    Node x = three.getFirstChild();
    assertThat(three.getNonJSDocComment()).isNotNull();
    assertThat(three.getNonJSDocComment().getCommentString()).isEqualTo("/*comment */");
    assertThat(three.getNonJSDocComment().isInline()).isTrue();
    assertThat(x.getNonJSDocComment()).isNull();
  }

  @Test
  public void testInlineNonJSDocCommentAttachment_quotedKey() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node script = parse("var { /*comment */ 'a': x} = foo();");
    Node var = script.getFirstChild();
    Node destructuringLHS = var.getFirstChild();
    Node objLit = destructuringLHS.getFirstChild();
    Node a = objLit.getFirstChild();
    Node x = a.getFirstChild();
    assertThat(a.getNonJSDocComment()).isNotNull();
    assertThat(a.getNonJSDocComment().getCommentString()).isEqualTo("/*comment */");
    assertThat(a.getNonJSDocComment().isInline()).isTrue();
    assertThat(x.getNonJSDocComment()).isNull();
  }

  @Test
  public void testInlineJSDocAttachmentToObjPatShorthandProp() {
    Node letNode = parse("let { /** string */ shorthandProp } = {};").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node objectPattern = destructuringLhs.getFirstChild();

    Node shorthandProp = objectPattern.getFirstChild();
    assertNode(shorthandProp).hasType(Token.STRING_KEY);
    Node shorthandPropTarget = shorthandProp.getOnlyChild();
    assertNodeHasJSDocInfoWithJSType(shorthandPropTarget, STRING_TYPE);
  }

  @Test
  public void testInlineNonJSDocAttachmentToObjPatShorthandProp() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node letNode = parse("let { /* blah */ shorthandProp } = {};").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node objectPattern = destructuringLhs.getFirstChild();

    Node shorthandProp = objectPattern.getFirstChild();
    assertNode(shorthandProp).hasType(Token.STRING_KEY);
    assertThat(shorthandProp.getNonJSDocCommentString()).contains("/* blah */");
  }

  @Test
  public void testInlineJSDocAttachmentToObjPatNormalPropWithDefault() {
    Node letNode =
        parse("let { normalPropWithDefault: /** string */ normalPropWithDefault = 'hi' } = {};")
            .getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node objectPattern = destructuringLhs.getFirstChild();

    Node normalPropWithDefault = objectPattern.getFirstChild();
    assertNode(normalPropWithDefault).hasType(Token.STRING_KEY);
    Node normalPropDefaultValue = normalPropWithDefault.getOnlyChild();
    assertNode(normalPropDefaultValue).hasType(Token.DEFAULT_VALUE);
    Node normalPropWithDefaultTarget = normalPropDefaultValue.getFirstChild();
    assertNodeHasJSDocInfoWithJSType(normalPropWithDefaultTarget, STRING_TYPE);
  }

  @Test
  public void testInlineNonJSDocCommentAttachmentToObjPatNormalPropWithDefault() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node letNode =
        parse("let { normalPropWithDefault: /* blah */ normalPropWithDefault = 'hi' } = {};")
            .getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node objectPattern = destructuringLhs.getFirstChild();

    Node normalPropWithDefault = objectPattern.getFirstChild();
    assertNode(normalPropWithDefault).hasType(Token.STRING_KEY);
    Node normalPropDefaultValue = normalPropWithDefault.getOnlyChild();
    assertNode(normalPropDefaultValue).hasType(Token.DEFAULT_VALUE);
    Node normalPropWithDefaultTarget = normalPropDefaultValue.getFirstChild();
    assertThat(normalPropWithDefaultTarget.getNonJSDocCommentString()).contains("/* blah */");
  }

  @Test
  public void testInlineJSDocAttachmentToObjPatShorthandWithDefault() {
    Node letNode =
        parse("let { /** string */ shorthandPropWithDefault = 'lo' } = {};").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node objectPattern = destructuringLhs.getFirstChild();

    Node shorthandPropWithDefault = objectPattern.getFirstChild();
    assertNode(shorthandPropWithDefault).hasType(Token.STRING_KEY);
    Node shorthandPropDefaultValue = shorthandPropWithDefault.getOnlyChild();
    assertNode(shorthandPropDefaultValue).hasType(Token.DEFAULT_VALUE);
    Node shorthandPropWithDefaultTarget = shorthandPropDefaultValue.getFirstChild();
    assertNodeHasJSDocInfoWithJSType(shorthandPropWithDefaultTarget, STRING_TYPE);
  }

  @Test
  public void testInlineNonJSDocCommentAttachmentToObjPatShorthandWithDefault() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node letNode =
        parse("let { /* blah */ shorthandPropWithDefault = 'lo' } = {};").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node objectPattern = destructuringLhs.getFirstChild();

    Node shorthandPropWithDefault = objectPattern.getFirstChild();
    assertNode(shorthandPropWithDefault).hasType(Token.STRING_KEY);
    Node shorthandPropDefaultValue = shorthandPropWithDefault.getOnlyChild();
    assertNode(shorthandPropDefaultValue).hasType(Token.DEFAULT_VALUE);
    Node shorthandPropWithDefaultTarget = shorthandPropDefaultValue.getFirstChild();
    assertThat(shorthandPropWithDefaultTarget.getNonJSDocCommentString()).contains("/* blah */");
  }

  @Test
  public void testObjLitKeyNonJSDocComment() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node exprResult = parse("a = {\n// blah\n1n: 3};").getFirstChild();
    Node assignNode = exprResult.getFirstChild();
    Node objectLit = assignNode.getSecondChild();
    Node stringKey = objectLit.getFirstChild();
    assertThat(stringKey.getNonJSDocCommentString()).contains("// blah");
  }

  @Test
  public void testLabelNonJSDocComment() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node label = parse("\n// blah\nlabel:\nfor(;;){ continue label; }").getFirstChild();
    assertNode(label).hasType(Token.LABEL);
    assertThat(label.getNonJSDocCommentString()).contains("// blah");
  }

  @Test
  public void testFieldNonJSDocComment() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node field =
        parse("class C{\n// blah\n field }").getFirstChild().getLastChild().getFirstChild();
    assertNode(field).hasType(Token.MEMBER_FIELD_DEF);
    assertThat(field.getNonJSDocCommentString()).contains("// blah");
  }

  @Test
  public void testPropertyNameAssignmentNonJSDocComment() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node varName =
        parse("let {key: \n// blah\nvarName} = someObject")
            .getFirstChild()
            .getFirstChild()
            .getFirstChild()
            .getFirstChild()
            .getFirstChild();
    assertNode(varName).hasType(Token.NAME);
    assertThat(varName.getNonJSDocCommentString()).contains("// blah");
  }

  @Test
  public void testGetPropCallNonJSDocComment() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node exprResult = parse("foo.\n// blah\nbaz(3);").getFirstChild();
    Node call = exprResult.getFirstChild();
    assertNode(call).hasType(Token.CALL);

    Node bazAccess = call.getFirstChild();
    assertThat(bazAccess.getNonJSDocCommentString()).contains("// blah");
  }

  @Test
  public void testGetPropOptionalCallNonJSDocComment() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node exprResult = parse("foo?.\n// blah\nbaz(3);").getFirstChild();
    Node callNode = exprResult.getFirstChild();
    assertNode(callNode).hasType(Token.OPTCHAIN_CALL);

    Node bazAccess = callNode.getFirstChild();
    assertThat(bazAccess.getNonJSDocCommentString()).contains("// blah");
  }

  @Test
  public void testGetPropAssignmentNonJSDocComment() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node exprResult = parse("foo.\n// blah\nbaz = 5;").getFirstChild();
    Node assign = exprResult.getFirstChild();
    assertNode(assign).hasType(Token.ASSIGN);

    Node bazAccess = assign.getFirstChild();
    assertThat(bazAccess.getNonJSDocCommentString()).contains("// blah");
  }

  @Test
  public void testNonJSDocCommentsOnAdjacentNodes() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node root =
        parse(
            """
            // comment before GETPROP
            a(
            // comment on GETPROP
            )
            .b();
            // comment after GETPROP
            c();
            """);
    Node exprResultA = root.getFirstChild();
    assertNode(exprResultA).hasType(Token.EXPR_RESULT);
    assertThat(exprResultA.getNonJSDocCommentString()).isEqualTo("// comment before GETPROP");

    Node nodeB = exprResultA.getFirstFirstChild();
    assertNode(nodeB).hasType(Token.GETPROP);
    assertThat(nodeB.getNonJSDocCommentString()).isEqualTo("// comment on GETPROP");

    Node exprResultC = root.getLastChild();
    assertNode(exprResultC).hasType(Token.EXPR_RESULT);
    assertThat(exprResultC.getNonJSDocCommentString()).isEqualTo("// comment after GETPROP");
  }

  @Test
  public void testInlineJSDocAttachmentToObjPatComputedPropKey() {
    Node letNode =
        parse("let { /** string */ ['computedProp']: computedProp } = {};").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node objectPattern = destructuringLhs.getFirstChild();

    Node computedProp = objectPattern.getFirstChild();
    assertNode(computedProp).hasType(Token.COMPUTED_PROP);
    // TODO(bradfordcsmith): Putting inline JSDoc on the computed property key should be an error,
    //     since it's not clear what it should mean.
    assertNodeHasNoJSDocInfo(computedProp);
  }

  @Test
  public void testInlineJSDocAttachmentToObjPatComputedProp() {
    Node letNode =
        parse("let { ['computedProp']: /** string */ computedProp } = {};").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node objectPattern = destructuringLhs.getFirstChild();

    Node computedProp = objectPattern.getFirstChild();
    assertNode(computedProp).hasType(Token.COMPUTED_PROP);
    Node computedPropTarget = computedProp.getSecondChild();
    assertNodeHasJSDocInfoWithJSType(computedPropTarget, STRING_TYPE);
  }

  @Test
  public void testInlineJSDocAttachmentToObjPatComputedPropWithDefault() {
    Node letNode =
        parse("let { ['computedPropWithDefault']: /** string */ computedProp = 'go' } = {};")
            .getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node objectPattern = destructuringLhs.getFirstChild();

    Node computedPropWithDefault = objectPattern.getFirstChild();
    assertNode(computedPropWithDefault).hasType(Token.COMPUTED_PROP);
    Node computedPropDefaultValue = computedPropWithDefault.getSecondChild();
    assertNode(computedPropDefaultValue).hasType(Token.DEFAULT_VALUE);
    Node computedPropWithDefaultTarget = computedPropDefaultValue.getFirstChild();
    assertNodeHasJSDocInfoWithJSType(computedPropWithDefaultTarget, STRING_TYPE);
  }

  @Test
  public void testInlineJSDocAttachmentToObjPatNormalPropWithQualifiedName() {
    Node exprResult =
        parse("({ normalProp: /** string */ ns.normalPropTarget } = {});").getFirstChild();
    Node assignNode = exprResult.getFirstChild();
    assertNode(assignNode).hasType(Token.ASSIGN);

    Node objectPattern = assignNode.getFirstChild();

    Node normalProp = objectPattern.getFirstChild();
    assertNode(normalProp).hasType(Token.STRING_KEY);
    Node nsNormalPropTarget = normalProp.getOnlyChild();
    assertNodeHasJSDocInfoWithJSType(nsNormalPropTarget, STRING_TYPE);
  }

  @Test
  public void testInlineJSDocAttachmentToObjPatNormalPropWithQualifiedNameWithDefault() {
    Node exprResult =
        parse("({ normalProp: /** string */ ns.normalPropTarget = 'foo' } = {});").getFirstChild();
    Node assignNode = exprResult.getFirstChild();
    assertNode(assignNode).hasType(Token.ASSIGN);

    Node objectPattern = assignNode.getFirstChild();

    Node normalProp = objectPattern.getFirstChild();
    assertNode(normalProp).hasType(Token.STRING_KEY);
    Node defaultValue = normalProp.getFirstChild();
    assertNode(defaultValue).hasType(Token.DEFAULT_VALUE);
    Node nsNormalPropTarget = defaultValue.getFirstChild();
    assertNodeHasJSDocInfoWithJSType(nsNormalPropTarget, STRING_TYPE);
  }

  @Test
  public void testInlineJSDocAttachmentToArrayPatElement() {
    Node letNode = parse("let [/** string */ x] = [];").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node arrayPattern = destructuringLhs.getFirstChild();
    Node xVarName = arrayPattern.getFirstChild();
    assertNodeHasJSDocInfoWithJSType(xVarName, STRING_TYPE);
  }

  @Test
  public void testInlineJSDocAttachmentToArrayPatElementWithDefault() {
    Node letNode = parse("let [/** string */ x = 'hi'] = [];").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node arrayPattern = destructuringLhs.getFirstChild();
    Node defaultValue = arrayPattern.getFirstChild();
    assertNode(defaultValue).hasType(Token.DEFAULT_VALUE);

    Node xVarName = defaultValue.getFirstChild();
    assertNodeHasJSDocInfoWithJSType(xVarName, STRING_TYPE);
  }

  @Test
  public void testInlineJSDocAttachmentToArrayPatElementQualifiedName() {
    Node exprResult = parse("[/** string */ x.y.z] = [];").getFirstChild();
    Node assignNode = exprResult.getFirstChild();
    assertNode(assignNode).hasType(Token.ASSIGN);

    Node arrayPattern = assignNode.getFirstChild();
    assertNode(arrayPattern).hasType(Token.ARRAY_PATTERN);
    Node xYZName = arrayPattern.getFirstChild();
    assertNodeHasJSDocInfoWithJSType(xYZName, STRING_TYPE);
  }

  @Test
  public void testInlineJSDocAttachmentToArrayPatElementQualifiedNameWithDefault() {
    Node exprResult = parse("[/** string */ x.y.z = 'foo'] = [];").getFirstChild();
    Node assignNode = exprResult.getFirstChild();
    assertNode(assignNode).hasType(Token.ASSIGN);

    Node arrayPattern = assignNode.getFirstChild();
    assertNode(arrayPattern).hasType(Token.ARRAY_PATTERN);
    Node defaultValue = arrayPattern.getOnlyChild();
    assertNode(defaultValue).hasType(Token.DEFAULT_VALUE);
    Node xYZName = defaultValue.getFirstChild();
    assertNodeHasJSDocInfoWithJSType(xYZName, STRING_TYPE);
  }

  @Test
  public void testInlineJSDocAttachmentToArrayPatElementAfterElision() {
    Node letNode = parse("let [, /** string */ x] = [];").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node destructuringLhs = letNode.getFirstChild();
    Node arrayPattern = destructuringLhs.getFirstChild();
    Node empty = arrayPattern.getFirstChild();
    assertNode(empty).hasToken(Token.EMPTY);
    assertNode(empty).hasCharno(5);
    assertNode(empty).hasLength(1);
    Node xVarName = arrayPattern.getSecondChild();
    assertNodeHasJSDocInfoWithJSType(xVarName, STRING_TYPE);
  }

  @Test
  public void testInlineJSDocAttachmentToObjLitNormalProp() {
    Node letNode = parse("let x = { normalProp: /** string */ normalPropTarget };").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node xNode = letNode.getFirstChild();
    Node objectLit = xNode.getFirstChild();

    Node normalProp = objectLit.getFirstChild();
    assertNode(normalProp).hasType(Token.STRING_KEY);
    Node normalPropTarget = normalProp.getOnlyChild();
    // TODO(bradfordcsmith): Make sure CheckJsDoc considers this an error, because it doesn't
    //     make sense to have inline JSDoc on the value.
    assertNodeHasJSDocInfoWithNoJSType(normalPropTarget);
  }

  @Test
  public void testInlineJSDocAttachmentToObjLitNormalPropKey() {
    Node letNode = parse("let x = { /** string */ normalProp: normalProp };").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node xNode = letNode.getFirstChild();
    Node objectLit = xNode.getFirstChild();

    Node normalProp = objectLit.getFirstChild();
    assertNode(normalProp).hasType(Token.STRING_KEY);
    // TODO(bradfordcsmith): We should either disallow inline JSDoc here or correctly pull the type
    //     out of it.
    assertNodeHasJSDocInfoWithNoJSType(normalProp);
  }

  @Test
  public void testJSDocAttachmentToObjLitNormalPropKey() {
    Node letNode =
        parse("let x = { /** @type {string} */ normalProp: normalProp };").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node xNode = letNode.getFirstChild();
    Node objectLit = xNode.getFirstChild();

    Node normalProp = objectLit.getFirstChild();
    assertNode(normalProp).hasType(Token.STRING_KEY);
    assertNodeHasJSDocInfoWithJSType(normalProp, STRING_TYPE);
  }

  @Test
  public void testInlineJSDocAttachmentToObjLitShorthandProp() {
    Node letNode = parse("let x = { /** string */ shorthandProp };").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node xNode = letNode.getFirstChild();
    Node objectLit = xNode.getFirstChild();

    Node shorthandPropKey = objectLit.getFirstChild();
    assertNode(shorthandPropKey).hasType(Token.STRING_KEY);
    // TODO(bradfordcsmith): We should either disallow inline JSDoc here or correctly pull the type
    //     out of it.
    assertNodeHasJSDocInfoWithNoJSType(shorthandPropKey);
    Node shorthandPropTarget = shorthandPropKey.getOnlyChild();
    assertNodeHasNoJSDocInfo(shorthandPropTarget);
  }

  @Test
  public void testJSDocAttachmentToObjLitShorthandProp() {
    Node letNode = parse("let x = { /** @type {string} */ shorthandProp };").getFirstChild();
    assertNode(letNode).hasType(Token.LET);

    Node xNode = letNode.getFirstChild();
    Node objectLit = xNode.getFirstChild();

    Node shorthandPropKey = objectLit.getFirstChild();
    assertNode(shorthandPropKey).hasType(Token.STRING_KEY);
    assertNodeHasJSDocInfoWithJSType(shorthandPropKey, STRING_TYPE);
    Node shorthandPropTarget = shorthandPropKey.getOnlyChild();
    assertNodeHasNoJSDocInfo(shorthandPropTarget);
  }

  @Test
  public void testInlineJSDocAttachment1() {
    Node fn = parse("function f(/** string */ x) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    JSDocInfo info = fn.getSecondChild().getFirstChild().getJSDocInfo();
    assertThat(info).isNotNull();
    assertTypeEquals(STRING_TYPE, info.getType());
  }

  @Test
  public void testInline_BlockCommentAttachment() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn = parse("function f(/* blah */ x) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);
    Node xNode = fn.getSecondChild().getFirstChild();
    assertThat(xNode.getNonJSDocCommentString()).contains("/* blah */");
  }

  @Test
  public void testInline_LineCommentAttachment() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn = parse("function f( // blah\n x) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);
    Node xNode = fn.getSecondChild().getFirstChild();
    assertThat(xNode.getNonJSDocCommentString()).contains("// blah");
  }

  @Test
  public void testInlineNonJSDocComments_TrailingAndNonTrailing_ParamList() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn = parse("function f(x /* first */ , /* second */ y ) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node paramListNode = fn.getSecondChild();
    Node xNode = paramListNode.getFirstChild();
    Node yNode = paramListNode.getSecondChild();

    assertThat(xNode.getTrailingNonJSDocCommentString()).contains("/* first */");
    assertThat(yNode.getNonJSDocCommentString()).isEqualTo("/* second */");
  }

  @Test
  public void testInlineNonJSDocTrailingComments_ParamList() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn = parse("function f(x /* first */ , y ) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node paramListNode = fn.getSecondChild();
    Node xNode = paramListNode.getFirstChild();
    Node yNode = xNode.getNext();

    assertThat(xNode.getTrailingNonJSDocCommentString()).contains("/* first */");
    ;
    assertThat(yNode.getNonJSDocCommentString()).isEmpty();
  }

  @Test
  public void testInlineNonJSDocTrailingComments_formalParamList_SingleParam() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn = parse("function f(x /* first */) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node paramListNode = fn.getSecondChild();
    Node xNode = paramListNode.getFirstChild();
    assertNode(xNode).hasType(Token.NAME);
    assertThat(xNode.getTrailingNonJSDocCommentString()).contains("/* first */");
  }

  // Tests that same-line trailing comments attach to the same line param
  // function f(x, // first
  //            y // second
  //            ) {}
  @Test
  public void testInlineNonJSDocTrailingComments_ParamList_MultiLine() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn =
        parse(
                """
                function f(
                  x,// first
                  y // second
                ){}
                """)
            .getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node paramListNode = fn.getSecondChild();
    Node xNode = paramListNode.getFirstChild();
    Node yNode = paramListNode.getSecondChild();

    assertThat(xNode.getTrailingNonJSDocCommentString()).isEqualTo("// first");

    assertThat(yNode.getTrailingNonJSDocCommentString()).isEqualTo("// second");
  }

  // Tests that same-line trailing comments attach to the same line param
  // function f(x, /* first */
  //            y /* second */
  //            ) {}
  @Test
  public void testInlineNonJSDocTrailingComments_ParamList_MultiLine_BlockComments() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn =
        parse(
                """
                function f(
                  x, /* first */
                  y /* second */
                ) {}
                """)
            .getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node paramListNode = fn.getSecondChild();
    Node xNode = paramListNode.getFirstChild();
    Node yNode = paramListNode.getSecondChild();

    assertThat(xNode.getTrailingNonJSDocCommentString()).isEqualTo("/* first */");

    assertThat(yNode.getTrailingNonJSDocCommentString()).isEqualTo("/* second */");
  }

  // Tests that same-line trailing comments attach to the same line param
  // function f(x, /* first */
  //            y
  //            ) {}
  @Test
  public void testInlineNonJSDocTrailingComments_ParamList_MultiLine_SingleBlockComments() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn =
        parse(
                """
                function f(x, /* first */
                y
                ) {}
                """)
            .getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node paramListNode = fn.getSecondChild();
    Node xNode = paramListNode.getFirstChild();
    Node yNode = paramListNode.getSecondChild();

    assertThat(xNode.getTrailingNonJSDocCommentString()).isEqualTo("/* first */");

    assertThat(yNode.getNonJSDocComment()).isNull();
  }

  @Test
  public void testNonJSDocTrailingCommentOnConstant() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node cnst = parse("const A = 1; // comment").getFirstChild();
    assertNode(cnst).hasType(Token.CONST);

    assertThat(cnst.getTrailingNonJSDocCommentString()).isEqualTo("// comment");
  }

  @Test
  public void testNonJSDocTrailingCommentOnConstantNoWhitespace() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node cnst = parse("const A = 1;// comment").getFirstChild();
    assertNode(cnst).hasType(Token.CONST);

    assertThat(cnst.getTrailingNonJSDocCommentString()).isEqualTo("// comment");
  }

  @Test
  public void testNonJSDocTrailingCommentOnConstantWithMoreWhitespace() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node cnst = parse("const A = 1;   // comment").getFirstChild();
    assertNode(cnst).hasType(Token.CONST);

    assertThat(cnst.getTrailingNonJSDocCommentString()).isEqualTo("// comment");
  }

  @Test
  public void testNonJSDocTrailingCommentOnConstantFollowedByConstant() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node cnst =
        parse(
                """
                const A = 1; // comment

                const B = 2;
                """)
            .getFirstChild();
    assertNode(cnst).hasType(Token.CONST);

    assertThat(cnst.getTrailingNonJSDocCommentString()).isEqualTo("// comment");
  }

  @Test
  public void testMultipleNonJSDocTrailingComments() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node n =
        parse(
            """
            const A = 1; /* A1 */ /* A2 */ // A3

            const B = 2;
            """);
    Node a = n.getFirstChild();
    assertNode(a).hasType(Token.CONST);
    Node b = n.getLastChild();
    assertNode(a).hasType(Token.CONST);
    assertThat(b.getNonJSDocCommentString()).isEqualTo("/* A2 */// A3");
  }

  @Test
  public void testNonJSDocTrailingCommentAfterFunction() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node n = parse("function foo(){} // comment").getFirstChild();
    assertNode(n).hasType(Token.FUNCTION);

    assertThat(n.getTrailingNonJSDocCommentString()).isEqualTo("// comment");
  }

  @Test
  public void testNonJSDocTrailingCommentAfterFunctionCall() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node n =
        parse(
            """
            function g(){ f(); // comment
            f();}
            """);
    Node exprRes = n.getFirstChild().getLastChild().getFirstChild();
    assertNode(exprRes).hasType(Token.EXPR_RESULT);

    assertThat(exprRes.getTrailingNonJSDocCommentString()).isEqualTo("// comment");
  }

  @Test
  public void testNonJSDocTrailingCommentAfterFunctionCallInBlock() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node n =
        parse(
            """
            if (true) {
              f1(); // comment1 on f1()
              // comment2
              // comment3
            }
            """);
    Node exprRes = n.getFirstChild().getLastChild().getFirstChild();
    assertNode(exprRes).hasType(Token.EXPR_RESULT);

    assertThat(exprRes.getTrailingNonJSDocCommentString())
        .isEqualTo("// comment1 on f1()\n// comment2\n// comment3");
  }

  @Test
  public void testLastNonJSDocCommentInBlock() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node n =
        parse(
            """
            if (true) {
              f();
              /* comment */
            }
            """);
    Node exprRes = n.getFirstChild().getLastChild().getFirstChild();
    assertNode(exprRes).hasType(Token.EXPR_RESULT);

    assertThat(exprRes.getTrailingNonJSDocCommentString()).isEqualTo("\n/* comment */");
  }

  @Test
  public void testLastNonJSDocCommentInBlockWithBlankLines() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node n =
        parse(
            """
            if (true) {
              f();


              /* comment */
            }
            """);
    Node exprRes = n.getFirstChild().getLastChild().getFirstChild();
    assertNode(exprRes).hasType(Token.EXPR_RESULT);

    // TODO(b/242294987): This should keep the blank lines.
    assertThat(exprRes.getTrailingNonJSDocCommentString()).isEqualTo("\n/* comment */");
  }

  @Test
  public void testInlineCommentInFunctionCallInBlock() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node n =
        parse(
            """
            if (true) {
              f(0, 1 /* comment */);}
            """);
    Node exprRes = n.getFirstChild().getLastChild().getFirstChild();
    assertNode(exprRes).hasType(Token.EXPR_RESULT);
    // TODO(b/242294987): This should not be an "end of block" comment (which we treat as trailing
    // comment on the last child), but a comment on the argument.
    assertThat(exprRes.getTrailingNonJSDocCommentString()).isEqualTo("\n/* comment */");
  }

  @Test
  public void testNonJSDocBigCommentInbetween() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node n =
        parse(
            """
            let x = 0; // comment on x
            //
            // more comment
            let y = 1;
            """);

    Node fstLetDecl = n.getFirstChild();
    Node sndLetDecl = n.getLastChild();

    assertNode(fstLetDecl).hasType(Token.LET);
    assertThat(fstLetDecl.getTrailingNonJSDocCommentString()).isEqualTo("// comment on x");
    assertNode(sndLetDecl).hasType(Token.LET);
    assertThat(sndLetDecl.getNonJSDocCommentString()).isEqualTo("//\n// more comment");
  }

  @Test
  public void testInlineNonJSDocCommentsOnSeparateLetDeclarations() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;

    Node n = parse("let a /* leading a */ = {c} /* trailing */; let /* leading b */ b  = {d};");
    Node letADecl = n.getFirstChild();
    Node letBDecl = n.getLastChild();

    assertNode(letADecl).hasType(Token.LET);
    assertNode(letBDecl).hasType(Token.LET);
    assertThat(letADecl.getFirstFirstChild().getNonJSDocCommentString())
        .contains("/* leading a */");
    assertThat(letADecl.getTrailingNonJSDocCommentString()).contains("/* trailing */");
    assertThat(letBDecl.getFirstChild().getNonJSDocCommentString()).contains("/* leading b */");
  }

  // function f( // blah1
  //              x,
  //             // blah2
  //              y) {}
  @Test
  public void testMultipleInline_LineCommentsAttachment() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn =
        parse(
                """
                function f(
                  // blah1
                  x,
                  // blah2
                  y) {}
                """)
            .getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node xNode = fn.getSecondChild().getFirstChild();
    assertThat(xNode.getNonJSDocCommentString()).contains("// blah1");

    Node yNode = fn.getSecondChild().getSecondChild();
    assertThat(yNode.getNonJSDocCommentString()).isEqualTo("// blah2");
  }

  // function f( /* blah1 */ x,
  //            // blah2
  //            y) {}
  @Test
  public void testMultipleInline_MixedCommentsAttachment() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn =
        parse(
                """
                function f(
                  /* blah1 */ x,
                  // blah2
                  y
                ) {}
                """)
            .getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node xNode = fn.getSecondChild().getFirstChild();
    assertThat(xNode.getNonJSDocCommentString()).contains("/* blah1 */");

    Node yNode = fn.getSecondChild().getSecondChild();
    assertThat(yNode.getNonJSDocCommentString()).isEqualTo("// blah2");
  }

  @Test
  public void testMultipleInline_NonJSDocCommentsGetAttachedToSameNode() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn = parse("function f(/* blah1 */\n// blah\n x) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node xNode = fn.getSecondChild().getFirstChild();
    assertThat(xNode.getNonJSDocCommentString()).contains("/* blah1 */\n// blah");
  }

  @Test
  public void testBoth_TrailingAndNonTrailing_NonJSDocCommentsGetAttachedToSameNode_MultiLine() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn =
        parse(
                """
                function f(
                     /* blah1 */
                     x // blah
                  ) {}
                """)
            .getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node xNode = fn.getSecondChild().getFirstChild();
    assertThat(xNode.getNonJSDocCommentString()).contains("/* blah1 */");
    assertThat(xNode.getTrailingNonJSDocCommentString()).contains("// blah");
  }

  @Test
  public void testEndOfFileNonJSDocComments_lineComment() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node scriptNode =
        parse(
            """
            function f1() {}
            // first
            f1();
            // second
            """);

    assertNode(scriptNode).hasType(Token.SCRIPT);

    Node exprNode = scriptNode.getLastChild();
    assertNode(exprNode).hasType(Token.EXPR_RESULT);

    assertThat(scriptNode.getTrailingNonJSDocCommentString()).isEqualTo("// second");
  }

  @Test
  public void testEndOfFileNonJSDocComments_blockComment() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node scriptNode =
        parse(
            """
            function f1() {}
            // first
            f1();
            /* second */
            """);

    assertNode(scriptNode).hasType(Token.SCRIPT);

    Node exprNode = scriptNode.getLastChild();
    assertNode(exprNode).hasType(Token.EXPR_RESULT);

    assertThat(scriptNode.getTrailingNonJSDocCommentString()).isEqualTo("/* second */");
  }

  @Test
  public void testEndOfFileNonJSDocComments_manyComments() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node scriptNode =
        parse(
            """
            function f1() {}
            // first
            f1();
            // second
            /* third */
            // fourth
            """);

    assertNode(scriptNode).hasType(Token.SCRIPT);

    Node exprNode = scriptNode.getLastChild();
    assertNode(exprNode).hasType(Token.EXPR_RESULT);

    assertThat(scriptNode.getTrailingNonJSDocCommentString())
        .isEqualTo("// second\n/* third */\n// fourth");
  }

  @Test
  public void testEndOfFileNonJSDocComments() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node scriptNode =
        parse(
            """
            function f1() {}
            if (true) {
            // first
            f1(); // second
            }
            // third
            """);

    assertNode(scriptNode).hasType(Token.SCRIPT);

    Node exprNode = scriptNode.getLastChild().getLastChild().getFirstChild();
    assertNode(exprNode).hasType(Token.EXPR_RESULT);
    assertThat(exprNode.getNonJSDocCommentString()).isEqualTo("// first");

    Node callNode = scriptNode.getLastChild().getLastChild().getLastChild();
    assertThat(callNode.getTrailingNonJSDocCommentString()).isEqualTo("// second");

    assertThat(scriptNode.getTrailingNonJSDocCommentString()).isEqualTo("// third");
  }

  @Test
  public void testBoth_TrailingAndNonTrailing_NonJSDocCommentsGetAttachedToSameNode_SingleLine() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn =
        parse(
                """
                function f(/* blah1 */ x // blah
                ) {}
                """)
            .getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node xNode = fn.getSecondChild().getFirstChild();
    assertThat(xNode.getNonJSDocCommentString()).contains("/* blah1 */");
    assertThat(xNode.getTrailingNonJSDocCommentString()).contains("/ blah");
  }

  @Test
  public void testBothJSDocAndNonJSDocCommentsGetAttached() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn = parse("function f(/** string */ // nonJSDoc\n x) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node xNode = fn.getSecondChild().getFirstChild();
    JSDocInfo info = xNode.getJSDocInfo();
    assertThat(info).isNotNull();
    assertNodeHasJSDocInfoWithJSType(xNode, STRING_TYPE);
    assertThat(xNode.getNonJSDocCommentString()).contains("// nonJSDoc");
  }

  @Test
  public void testBothNonJSDocAndJSDocCommentsGetAttached() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn = parse("function f(// nonJSDoc\n /** string */ x) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node xNode = fn.getSecondChild().getFirstChild();
    JSDocInfo info = xNode.getJSDocInfo();
    assertThat(info).isNotNull();
    assertNodeHasJSDocInfoWithJSType(xNode, STRING_TYPE);
    assertThat(xNode.getNonJSDocCommentString()).contains("// nonJSDoc");
  }

  // Tests inline trailing comment of a parameter does not get attached to function body code when
  // there are no more parameters
  @Test
  public void testInlineTrailingNonJSDocComments_FunctionArgsAndBody() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node fn = parse("function f(x /* first */ ) { /* second */ let y;}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    Node xNode = fn.getSecondChild().getOnlyChild();
    Node yNode = fn.getLastChild().getFirstChild();

    assertThat(xNode.getTrailingNonJSDocCommentString()).contains("/* first */");
    assertThat(yNode.getNonJSDocCommentString()).contains("/* second */");
  }

  // Tests inline (non-trailing) comment preserved for single argument
  @Test
  public void testInlineNonJSDocComments_FunctionCall() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;

    Node exprRes = parse("function f(x) {let y;}; f( /* first */  1)").getLastChild();
    assertNode(exprRes).hasType(Token.EXPR_RESULT);

    Node call = exprRes.getFirstChild();
    Node oneArgNode = call.getSecondChild();

    assertThat(oneArgNode.getNonJSDocCommentString()).contains("/* first */");
  }

  // Tests inline trailing comment does not get attached to the next argument
  @Test
  public void testInlineTrailingNonJSDocComments_MultipleArgs() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;

    Node exprRes = parse("function f(x, y) {}; f( 1 /* first */, 2 );").getLastChild();
    assertNode(exprRes).hasType(Token.EXPR_RESULT);

    Node call = exprRes.getFirstChild();
    Node oneArgNode = call.getSecondChild();
    Node twoArgNode = oneArgNode.getNext();

    assertThat(oneArgNode.getTrailingNonJSDocCommentString()).contains("/* first */");
    assertThat(twoArgNode.getNonJSDocCommentString()).isEmpty();
  }

  // Tests inline trailing comment does not get lost when there is no next argument
  @Test
  public void testInlineTrailingNonJSDocComments_SingleArgument() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;

    Node exprRes = parse("function f(x, y) {}; f( 1 /* first */);").getLastChild();
    assertNode(exprRes).hasType(Token.EXPR_RESULT);

    Node call = exprRes.getFirstChild();
    Node oneArgNode = call.getSecondChild();

    assertThat(oneArgNode.getTrailingNonJSDocCommentString()).contains("/* first */");
  }

  @Test
  public void testInlineJSDocAttachment2() {
    Node fn = parse("function f(/** ? */ x) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    JSDocInfo info = fn.getSecondChild().getFirstChild().getJSDocInfo();
    assertThat(info).isNotNull();
    assertTypeEquals(UNKNOWN_TYPE, info.getType());
  }

  @Test
  public void testInlineJSDocAttachment3() {
    parse("function f(/** @type {string} */ x) {}");
  }

  @Test
  public void testInlineJSDocAttachment4() {
    parse(
        """
        function f(/**
         * @type {string}
         */ x) {}
        """);
  }

  @Test
  public void testInlineJSDocAttachment5() {
    Node vardecl = parse("var /** string */ x = 'asdf';").getFirstChild();
    JSDocInfo info = vardecl.getFirstChild().getJSDocInfo();
    assertThat(info).isNotNull();
    assertThat(info.hasType()).isTrue();
    assertTypeEquals(STRING_TYPE, info.getType());
  }

  @Test
  public void testInlineJSDocAttachment6() {
    Node fn = parse("function f(/** {attr: number} */ x) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    JSDocInfo info = fn.getSecondChild().getFirstChild().getJSDocInfo();
    assertThat(info).isNotNull();
    assertTypeEquals(
        createRecordTypeBuilder().addProperty("attr", NUMBER_TYPE, null).build(), info.getType());
  }

  @Test
  public void testInlineJSDocWithOptionalType() {
    Node fn = parse("function f(/** string= */ x) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    JSDocInfo info = fn.getSecondChild().getFirstChild().getJSDocInfo();
    assertThat(info.getType().isOptionalArg()).isTrue();
  }

  @Test
  public void testInlineJSDocWithVarArgs() {
    Node fn = parse("function f(/** ...string */ x) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    JSDocInfo info = fn.getSecondChild().getFirstChild().getJSDocInfo();
    assertThat(info.getType().isVarArgs()).isTrue();
  }

  @Test
  public void testInlineJSDocReturnType() {
    Node fn = parse("function /** string */ f(x) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    JSDocInfo info = fn.getFirstChild().getJSDocInfo();
    assertThat(info.hasType()).isTrue();
    assertTypeEquals(STRING_TYPE, info.getType());
  }

  @Test
  public void testInlineJSDocReturnType_generator1() {
    Node fn = parse("function * /** string */ f(x) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    JSDocInfo info = fn.getFirstChild().getJSDocInfo();
    assertThat(info.hasType()).isTrue();
    assertTypeEquals(STRING_TYPE, info.getType());
  }

  @Test
  public void testInlineJSDocReturnType_generator2() {
    Node fn = parse("function /** string */ *f(x) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    JSDocInfo info = fn.getFirstChild().getJSDocInfo();
    assertThat(info.hasType()).isTrue();
    assertTypeEquals(STRING_TYPE, info.getType());
  }

  @Test
  public void testInlineJSDocReturnType_async() {
    Node fn = parse("async function /** string */ f(x) {}").getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);

    JSDocInfo info = fn.getFirstChild().getJSDocInfo();
    assertThat(info.hasType()).isTrue();
    assertTypeEquals(STRING_TYPE, info.getType());
  }

  @Test
  public void testIncorrectJSDocDoesNotAlterJSParsing1() {
    assertNodeEquality(
        parse("var a = [1,2]"),
        parseWarning("/** @type {Array<number} */var a = [1,2]", MISSING_GT_MESSAGE));
  }

  @Test
  public void testIncorrectJSDocDoesNotAlterJSParsing2() {
    assertNodeEquality(
        parse("var a = [1,2]"),
        parseWarning("/** @type {Array.<number}*/var a = [1,2]", MISSING_GT_MESSAGE));
  }

  @Test
  public void testIncorrectJSDocDoesNotAlterJSParsing3() {
    assertNodeEquality(
        parse("C.prototype.say=function(nums) {alert(nums.join(','));};"),
        parseWarning(
            """
            /** @param {Array.<number} nums */
            C.prototype.say=function(nums) {alert(nums.join(','));};
            """,
            MISSING_GT_MESSAGE));
  }

  @Test
  public void testIncorrectJSDocDoesNotAlterJSParsing4() {
    assertNodeEquality(
        parse("C.prototype.say=function(nums) {alert(nums.join(','));};"),
        parse(
            """
            /** @return {boolean} */
            C.prototype.say=function(nums) {alert(nums.join(','));};
            """));
  }

  @Test
  public void testIncorrectJSDocDoesNotAlterJSParsing5() {
    assertNodeEquality(
        parse("C.prototype.say=function(nums) {alert(nums.join(','));};"),
        parse(
            """
            /** @param {boolean} this is some string*/
            C.prototype.say=function(nums) {alert(nums.join(','));};
            """));
  }

  @Test
  public void testIncorrectJSDocDoesNotAlterJSParsing6() {
    assertNodeEquality(
        parse("C.prototype.say=function(nums) {alert(nums.join(','));};"),
        parseWarning(
            """
            /** @param {bool!*%E$} */
            C.prototype.say=function(nums) {alert(nums.join(','));};
            """,
            "Bad type annotation. expected closing }" + BAD_TYPE_WIKI_LINK,
            "Bad type annotation. expecting a variable name in a @param tag."
                + BAD_TYPE_WIKI_LINK));
  }

  @Test
  public void testIncorrectJSDocDoesNotAlterJSParsing7() {
    isIdeMode = true;

    assertNodeEquality(
        parse("C.prototype.say=function(nums) {alert(nums.join(','));};"),
        parseWarning(
            """
            /** @see */
            C.prototype.say=function(nums) {alert(nums.join(','));};
            """,
            "@see tag missing description"));
  }

  @Test
  public void testIncorrectJSDocDoesNotAlterJSParsing8() {
    isIdeMode = true;

    assertNodeEquality(
        parse("C.prototype.say=function(nums) {alert(nums.join(','));};"),
        parseWarning(
            """
            /** @author */
            C.prototype.say=function(nums) {alert(nums.join(','));};
            """,
            "@author tag missing author"));
  }

  @Test
  public void testIncorrectJSDocDoesNotAlterJSParsing9() {
    assertNodeEquality(
        parse("C.prototype.say=function(nums) {alert(nums.join(','));};"),
        parseWarning(
            """
            /** @someillegaltag */
            C.prototype.say=function(nums) {alert(nums.join(','));};
            """,
            "illegal use of unknown JSDoc tag \"someillegaltag\"; ignoring it. Place another"
                + " character before the @ to stop JSCompiler from parsing it as an annotation."));
  }

  @Test
  public void testMisplacedDescAnnotation_noWarning() {
    parse("/** @desc Foo. */ var MSG_BAR = goog.getMsg('hello');");
    parse("/** @desc Foo. */ x.y.z.MSG_BAR = goog.getMsg('hello');");
    parse("/** @desc Foo. */ MSG_BAR = goog.getMsg('hello');");
    parse("var msgs = {/** @desc x */ MSG_X: goog.getMsg('x')}");
  }

  @Test
  public void testUnescapedSlashInRegexpCharClass() {
    parse("var foo = /[/]/;");
    parse("var foo = /[hi there/]/;");
    parse("var foo = /[/yo dude]/;");
    parse("var foo = /\\/[@#$/watashi/wa/suteevu/desu]/;");
  }

  /** Test for https://github.com/google/closure-compiler/issues/389. */
  @Test
  public void testMalformedRegexp() {
    // Simple repro case
    String js = "var x = com\\";
    parseError(js, "Invalid escape sequence");

    // The original repro case as reported.
    js =
        """
        (function() {
          var url="";
          switch(true)
          {
            case /a.com\\/g|l.i/N/.test(url):
              return "";
            case /b.com\\/T/.test(url):
              return "";
          }
        }
        )();
        """;
    parseError(js, "primary expression expected");
  }

  private static void assertNodeEquality(Node expected, Node found) {
    assertNode(found).isEqualTo(expected);
  }

  @Test
  public void testParse() {
    strictMode = SLOPPY;
    Node a = Node.newString(Token.NAME, "a");
    a.addChildToFront(Node.newString(Token.NAME, "b"));
    ImmutableList<ParserResult> testCases =
        ImmutableList.of(
            new ParserResult("3;", createScript(new Node(Token.EXPR_RESULT, Node.newNumber(3.0)))),
            new ParserResult("var a = b;", createScript(new Node(Token.VAR, a))));

    for (ParserResult testCase : testCases) {
      assertNodeEquality(testCase.node, parse(testCase.code));
    }
  }

  @Test
  public void testPostfixExpression() {
    parse("a++");
    parse("a.b--");
    parse("a[0]++");
    parse("/** @type {number} */ (a)++;");

    parseError("a()++", "Invalid postfix increment operand.");
    parseError("(new C)--", "Invalid postfix decrement operand.");
    parseError("this++", "Invalid postfix increment operand.");
    parseError("(a--)++", "Invalid postfix increment operand.");
    parseError("(+a)++", "Invalid postfix increment operand.");
    parseError("[1,2]++", "Invalid postfix increment operand.");
    parseError("'literal'++", "Invalid postfix increment operand.");
    parseError("/** @type {number} */ (a())++;", "Invalid postfix increment operand.");
  }

  @Test
  public void testUnaryExpression() {
    strictMode = SLOPPY;
    parse("delete a.b");
    parse("delete a[0]");
    parse("void f()");
    parse("typeof new C");
    parse("++a[0]");
    parse("--a.b");
    parse("+{a: 1}");
    parse("-[1,2]");
    parse("~'42'");
    expectFeatures(Feature.SUPER);
    parse("!super.a");
    expectFeatures();

    parseError("delete f()", "Invalid delete operand. Only properties can be deleted.");
    parseError("++a++", "Invalid prefix increment operand.");
    parseError("--{a: 1}", "Invalid prefix decrement operand.");
    parseError("++this", "Invalid prefix increment operand.");
    parseError("++(-a)", "Invalid prefix increment operand.");
    parseError("++{a: 1}", "Invalid prefix increment operand.");
    parseError("++'literal'", "Invalid prefix increment operand.");
    parseError("++delete a.b", "Invalid prefix increment operand.");
  }

  @Test
  public void testUnaryExpressionWithBigInt() {
    parseError("+1n", "Cannot convert a BigInt value to a number");
    parseError("delete 6n", "Invalid delete operand. Only properties can be deleted.");
  }

  // Automatic Semicolon Insertion
  // http://www.ecma-international.org/ecma-262/10.0/index.html#sec-rules-of-automatic-semicolon-insertion

  @Test
  public void testAutomaticSemicolonInsertion() {
    // var statements
    assertNodeEquality(parse("var x = 1\nvar y = 2"), parse("var x = 1; var y = 2;"));
    assertNodeEquality(parse("var x = 1\n, y = 2"), parse("var x = 1, y = 2;"));

    // assign statements
    assertNodeEquality(parse("x = 1\ny = 2"), parse("x = 1; y = 2;"));

    assertNodeEquality(
        parse("x = 1\n;y = 2"), //
        parse("x = 1; y = 2;"));
    assertNodeEquality(
        parse("if (true) 1\n; else {}"), //
        parse("if (true) 1; else {}"));

    // if/else statements
    assertNodeEquality(parse("if (x)\n;else{}"), parse("if (x) {} else {}"));
  }

  @Test
  public void testAutomaticSemicolonInsertion_curly() {
    assertNodeEquality(
        parse("while (true) { 1 }"), //
        parse("while (true) { 1; }"));
  }

  @Test
  public void testAutomaticSemicolonInsertion_doWhile() {
    assertNodeEquality(
        parse("do {} while (true) 1;"), //
        parse("do {} while (true); 1;"));
  }

  /** Test all the ASI examples from http://www.ecma-international.org/ecma-262/5.1/#sec-7.9.2 */
  @Test
  public void testAutomaticSemicolonInsertion_examplesFromSpec() {
    parseError("{ 1 2 } 3", SEMICOLON_EXPECTED);

    assertNodeEquality(parse("{ 1\n2 } 3"), parse("{ 1; 2; } 3;"));

    parseError("for (a; b\n)", "';' expected");

    assertNodeEquality(
        parse("function f() { return\na + b }"), parse("function f() { return; a + b; }"));

    assertNodeEquality(parse("a = b\n++c"), parse("a = b; ++c;"));

    parseError("if (a > b)\nelse c = d", "primary expression expected");

    assertNodeEquality(parse("a = b + c\n(d + e).print()"), parse("a = b + c(d + e).print()"));
  }

  @Test
  public void testAutomaticSemicolonInsertion_restrictedRules() {
    parseError("x\n++;", "primary expression expected");
    assertNodeEquality(
        parse("function f() { return\n1; }"), //
        parse("function f() { return;1; }"));
    assertNodeEquality(
        parse("while (true) { continue\nlabel; }"), //
        parse("while (true) { continue; label; }"));
    assertNodeEquality(
        parse("while (true) { break\nlabel; }"), //
        parse("while (true) { break; label; }"));
    parseError("throw\n1;", "semicolon/newline not allowed after 'throw'");
    parseError("yield\nvalue;", "primary expression expected");
    parseError("()\n=> 1;", "No newline allowed before '=>'");
  }

  private static Node createScript(Node n) {
    Node script = new Node(Token.SCRIPT);
    script.addChildToBack(n);
    return script;
  }

  @Test
  public void testMethodInObjectLiteral() {
    expectFeatures(Feature.MEMBER_DECLARATIONS);
    testMethodInObjectLiteral("var a = {b() {}};");
    testMethodInObjectLiteral("var a = {b() { alert('b'); }};");

    // Static methods not allowed in object literals.
    expectFeatures();
    parseError(
        "var a = {static b() { alert('b'); }};", "Cannot use keyword in short object literal");
  }

  @Test
  public void testBigIntAsObjectLiteralPropertyName() {
    Node objectLit =
        parse("({1n() {}, 1n: 0})") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild(); // OBJECTLIT
    assertNode(objectLit).hasType(Token.OBJECTLIT);
    Node computedProp = objectLit.getFirstChild();
    assertNode(computedProp).hasType(Token.COMPUTED_PROP);
    Node bigint = computedProp.getFirstChild();
    assertNode(bigint).hasType(Token.BIGINT);
    Node stringKey = objectLit.getLastChild();
    assertNode(stringKey).hasType(Token.STRING_KEY);
  }

  private void testMethodInObjectLiteral(String js) {
    mode = LanguageMode.ECMASCRIPT_2015;
    strictMode = SLOPPY;
    parse(js);

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning(js, requiresLanguageModeMessage(Feature.MEMBER_DECLARATIONS));
  }

  @Test
  public void testExtendedObjectLiteral() {
    expectFeatures(Feature.EXTENDED_OBJECT_LITERALS);
    testExtendedObjectLiteral("var a = {b};");
    testExtendedObjectLiteral("var a = {b, c};");
    testExtendedObjectLiteral("var a = {b, c: d, e};");
    testExtendedObjectLiteral("var a = {type};");
    testExtendedObjectLiteral("var a = {declare};");
    testExtendedObjectLiteral("var a = {namespace};");
    testExtendedObjectLiteral("var a = {module};");

    expectFeatures();
    parseError("var a = { '!@#$%' };", "':' expected");
    parseError("var a = { 123 };", "':' expected");
    parseError("var a = { let };", "Cannot use keyword in short object literal");
    parseError("var a = { else };", "Cannot use keyword in short object literal");
  }

  private void testExtendedObjectLiteral(String js) {
    mode = LanguageMode.ECMASCRIPT_2015;
    strictMode = SLOPPY;
    parse(js);

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning(js, requiresLanguageModeMessage(Feature.EXTENDED_OBJECT_LITERALS));
  }

  @Test
  public void testComputedPropertiesObjLit() {
    expectFeatures(Feature.COMPUTED_PROPERTIES);

    // Method
    testComputedProperty("var x = {  [prop + '_']() {} }");
    // NOTE: we treat string and number keys as if they were computed properties for method
    // shorthand, but not getters and setters.
    testComputedProperty("var x = {  'abc'() {} }");
    testComputedProperty("var x = {  123() {} }");

    // Getter
    testComputedProperty("var x = {  get [prop + '_']() {} }");

    // Setter
    testComputedProperty("var x = { set [prop + '_'](val) {} }");

    // Generator method
    mode = LanguageMode.ECMASCRIPT_2015;
    strictMode = SLOPPY;
    parse("var x = { *[prop + '_']() {} }");
    parse("var x = { *'abc'() {} }");
    parse("var x = { *123() {} }");

    mode = LanguageMode.ECMASCRIPT_2017;
    parse("var x = { async [prop + '_']() {} }");
    parse("var x = { async 'abc'() {} }");
    parse("var x = { async 123() {} }");
  }

  @Test
  public void testComputedMethodClass() {
    strictMode = SLOPPY;
    expectFeatures(Feature.CLASSES, Feature.COMPUTED_PROPERTIES);
    parse("class X { [prop + '_']() {} }");
    // Note that we pretend string and number keys are computed property names, because
    // this makes it easier to treat class and object-literal cases consistently.
    parse("class X { 'abc'() {} }");
    parse("class X { 123() {} }");

    parse("class X { static [prop + '_']() {} }");
    parse("class X { static 'abc'() {} }");
    parse("class X { static 123() {} }");

    parse("class X { *[prop + '_']() {} }");
    parse("class X { *'abc'() {} }");
    parse("class X { *123() {} }");

    parse("class X { async [prop + '_']() {} }");
    parse("class X { async 'abc'() {} }");
    parse("class X { async 123() {} }");
  }

  @Test
  public void testBigIntComputedMethodClass() {
    expectFeatures(Feature.CLASSES, Feature.COMPUTED_PROPERTIES);
    Node classMembers;
    Node computedProp;
    Node bigint;
    BigInteger bigintValue = new BigInteger("123");
    classMembers =
        parse("class X { 123n() {} }") // SCRIPT
            .getOnlyChild() // CLASS
            .getLastChild(); // CLASS_MEMBERS
    assertNode(classMembers).hasType(Token.CLASS_MEMBERS);
    computedProp = classMembers.getOnlyChild();
    assertNode(computedProp).hasType(Token.COMPUTED_PROP);
    bigint = computedProp.getFirstChild();
    assertNode(bigint).hasType(Token.BIGINT);
    assertNode(bigint).isBigInt(bigintValue);
    classMembers =
        parse("class X { static 123n() {} }") // SCRIPT
            .getOnlyChild() // CLASS
            .getLastChild(); // CLASS_MEMBERS
    assertNode(classMembers).hasType(Token.CLASS_MEMBERS);
    computedProp = classMembers.getOnlyChild();
    assertNode(computedProp).hasType(Token.COMPUTED_PROP);
    assertNode(computedProp).isStatic();
    bigint = computedProp.getFirstChild();
    assertNode(bigint).hasType(Token.BIGINT);
    assertNode(bigint).isBigInt(bigintValue);
    classMembers =
        parse("class X { *123n() {} }") // SCRIPT
            .getOnlyChild() // CLASS
            .getLastChild(); // CLASS_MEMBERS
    assertNode(classMembers).hasType(Token.CLASS_MEMBERS);
    computedProp = classMembers.getOnlyChild();
    assertNode(computedProp).hasType(Token.COMPUTED_PROP);
    bigint = computedProp.getFirstChild();
    assertNode(bigint).hasType(Token.BIGINT);
    assertNode(bigint).isBigInt(bigintValue);
    classMembers =
        parse("class X { async 123n() {} }") // SCRIPT
            .getOnlyChild() // CLASS
            .getLastChild(); // CLASS_MEMBERS
    assertNode(classMembers).hasType(Token.CLASS_MEMBERS);
    computedProp = classMembers.getOnlyChild();
    assertNode(computedProp).hasType(Token.COMPUTED_PROP);
    bigint = computedProp.getFirstChild();
    assertNode(bigint).hasType(Token.BIGINT);
    assertNode(bigint).isBigInt(bigintValue);
  }

  @Test
  public void testComputedProperty() {
    expectFeatures(Feature.COMPUTED_PROPERTIES);

    testComputedProperty(
        """
        var prop = 'some complex expression';

        var x = {
          [prop]: 'foo'
        }
        """);

    testComputedProperty(
        """
        var prop = 'some complex expression';

        var x = {
          [prop + '!']: 'foo'
        }
        """);

    testComputedProperty(
        """
        var prop;

        var x = {
          [prop = 'some expr']: 'foo'
        }
        """);

    testComputedProperty(
        """
        var x = {
          [1 << 8]: 'foo'
        }
        """);

    String js =
        """
        var x = {
          [1 << 8]: 'foo',
          [1 << 7]: 'bar'
        }
        """;
    mode = LanguageMode.ECMASCRIPT_2015;
    strictMode = SLOPPY;
    parse(js);
    mode = LanguageMode.ECMASCRIPT5;
    String warning = requiresLanguageModeMessage(Feature.COMPUTED_PROPERTIES);
    parseWarning(js, warning, warning);
  }

  private void testComputedProperty(String js) {
    mode = LanguageMode.ECMASCRIPT_2015;
    strictMode = SLOPPY;
    parse(js);

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning(js, requiresLanguageModeMessage(Feature.COMPUTED_PROPERTIES));
  }

  @Test
  public void testTrailingCommaWarning1() {
    parse("var a = ['foo', 'bar'];");
  }

  @Test
  public void testTrailingCommaWarning2() {
    parse("var a = ['foo',,'bar'];");
  }

  @Test
  public void testTrailingCommaWarning3() {
    mode = LanguageMode.ECMASCRIPT3;
    expectFeatures(Feature.TRAILING_COMMA);
    parseWarning("var a = ['foo', 'bar',];", TRAILING_COMMA_MESSAGE);
    mode = LanguageMode.ECMASCRIPT5;
    strictMode = SLOPPY;
    parse("var a = ['foo', 'bar',];");
  }

  @Test
  public void testTrailingCommaWarning4() {
    mode = LanguageMode.ECMASCRIPT3;
    expectFeatures(Feature.TRAILING_COMMA);
    parseWarning("var a = [,];", TRAILING_COMMA_MESSAGE);
    mode = LanguageMode.ECMASCRIPT5;
    strictMode = SLOPPY;
    parse("var a = [,];");
  }

  @Test
  public void testTrailingCommaWarning5() {
    parse("var a = {'foo': 'bar'};");
  }

  @Test
  public void testTrailingCommaWarning6() {
    mode = LanguageMode.ECMASCRIPT3;
    expectFeatures(Feature.TRAILING_COMMA);
    parseWarning("var a = {'foo': 'bar',};", TRAILING_COMMA_MESSAGE);
    mode = LanguageMode.ECMASCRIPT5;
    strictMode = SLOPPY;
    parse("var a = {'foo': 'bar',};");
  }

  @Test
  public void testTrailingCommaWarning7() {
    parseError("var a = {,};", "'}' expected");
  }

  @Test
  public void testCatchClauseForbidden() {
    parseError("try { } catch (e if true) {}", "')' expected");
  }

  @Test
  public void testConstForbidden() {
    mode = LanguageMode.ECMASCRIPT5;
    expectFeatures(Feature.CONST_DECLARATIONS);
    parseWarning("const x = 3;", requiresLanguageModeMessage(Feature.CONST_DECLARATIONS));
  }

  @Test
  public void testAnonymousFunctionExpression() {
    mode = LanguageMode.ECMASCRIPT5;
    strictMode = SLOPPY;
    parseError("function () {}", "'identifier' expected");

    mode = LanguageMode.ECMASCRIPT_2015;
    parseError("function () {}", "'identifier' expected");

    isIdeMode = true;
    parseError("function () {}", "'identifier' expected", "unnamed function statement");
  }

  @Test
  public void testAnonymousFunctionExpressionInClosureUnawareCode() {
    mode = LanguageMode.ECMASCRIPT_2015;
    parseError(
        "/** @closureUnaware */ (function() { function () {} }).call(globalThis)",
        "'identifier' expected");
  }

  @Test
  public void testArrayDestructuringVar() {
    mode = LanguageMode.ECMASCRIPT5;
    strictMode = SLOPPY;
    expectFeatures(Feature.ARRAY_DESTRUCTURING);
    parseWarning("var [x,y] = foo();", requiresLanguageModeMessage(Feature.ARRAY_DESTRUCTURING));

    mode = LanguageMode.ECMASCRIPT_2015;
    parse("var [x,y] = foo();");
  }

  @Test
  public void testLHSOfNonVanillaEqualsOperator() {
    strictMode = SLOPPY;
    mode = LanguageMode.ECMASCRIPT_2015;

    // object pattern or array pattern on the lhs of vanilla equals passes
    parse("for (let [i,j] = [2,0]; j < 2; [i,j]  =  [j, j+1]) {}");
    parse("for (let [i,j] = [2,0]; j < 2; {i,j}  =  [j, j+1]) {}");

    // error with object pattern or array pattern on the lhs of +=
    parseError(
        "for (let [i,j] = [2,0]; j < 2; [i,j]  +=  [j, j+1]) {}", "invalid assignment target");
    parseError(
        "for (let [i,j] = [2,0]; j < 2; {i,j}  +=  [j, j+1]) {}", "invalid assignment target");

    parse("let [i,j]  =  [2, 2];");
    parseError("let [i,j]  +=  [2, 2];", "destructuring must have an initializer");
  }

  @Test
  public void testArrayDestructuringVarInvalid() {
    // arbitrary LHS assignment target not allowed
    parseError(
        "var [x,y[15]] = foo();", "Only an identifier or destructuring pattern is allowed here.");
  }

  @Test
  public void testArrayDestructuringAssign() {
    mode = LanguageMode.ECMASCRIPT5;
    strictMode = SLOPPY;
    expectFeatures(Feature.ARRAY_DESTRUCTURING);
    parseWarning("[x,y] = foo();", requiresLanguageModeMessage(Feature.ARRAY_DESTRUCTURING));

    mode = LanguageMode.ECMASCRIPT_2015;
    parse("[x,y] = foo();");
    // arbitrary LHS assignment target is allowed
    parse("[x,y[15]] = foo();");
  }

  @Test
  public void testArrayDestructuringInitializer() {
    strictMode = SLOPPY;
    expectFeatures(Feature.ARRAY_DESTRUCTURING);
    parse("var [x=1,y] = foo();");
    parse("[x=1,y] = foo();");
    parse("var [x,y=2] = foo();");
    parse("[x,y=2] = foo();");

    parse("var [[a] = ['b']] = [];");
    parse("[[a] = ['b']] = [];");
    // arbitrary LHS target allowed in assignment, but not declaration
    parse("[[a.x] = ['b']] = [];");
  }

  @Test
  public void testArrayDestructuringInitializerInvalid() {
    parseError(
        "var [[a.x] = ['b']] = [];",
        "Only an identifier or destructuring pattern is allowed here.");
  }

  @Test
  public void testArrayDestructuringDeclarationRest() {
    strictMode = SLOPPY;

    expectFeatures(Feature.ARRAY_DESTRUCTURING, Feature.ARRAY_PATTERN_REST);
    parse("var [first, ...rest] = foo();");
    parse("let [first, ...rest] = foo();");
    parse("const [first, ...rest] = foo();");

    // nested destructuring in regular parameters and rest parameters
    parse("var [first, {a, b}, ...[re, st, ...{length}]] = foo();");

    expectFeatures();
    parseError(
        "var [first, ...more = 'default'] = foo();",
        "A default value cannot be specified after '...'");
    parseError("var [first, ...more, last] = foo();", "']' expected");

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning(
        "var [first, ...rest] = foo();",
        requiresLanguageModeMessage(Feature.ARRAY_DESTRUCTURING),
        requiresLanguageModeMessage(Feature.ARRAY_PATTERN_REST));
  }

  @Test
  public void testObjectDestructuringDeclarationRest() {
    strictMode = SLOPPY;

    expectFeatures(Feature.OBJECT_DESTRUCTURING, Feature.OBJECT_PATTERN_REST);
    parse("var {first, ...rest} = foo();");
    parse("let {first, ...rest} = foo();");
    parse("const {first, ...rest} = foo();");

    expectFeatures();
    parseError(
        "var {first, ...more = 'default'} = foo();",
        "A default value cannot be specified after '...'");
    parseError("var {first, ...more, last} = foo();", "'}' expected");

    mode = LanguageMode.ECMASCRIPT_2015;
    parseWarning(
        "var {first, ...rest} = foo();", requiresLanguageModeMessage(Feature.OBJECT_PATTERN_REST));
  }

  @Test
  public void testArrayLiteralDeclarationSpread() {
    strictMode = SLOPPY;

    expectFeatures(Feature.SPREAD_EXPRESSIONS);
    parse("var o = [first, ...spread];");

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning(
        "var o = [first, ...spread];", requiresLanguageModeMessage(Feature.SPREAD_EXPRESSIONS));
  }

  @Test
  public void testObjectLiteralDeclarationSpread() {
    strictMode = SLOPPY;

    expectFeatures(Feature.OBJECT_LITERALS_WITH_SPREAD);
    parse("var o = {first: 1, ...spread};");

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning(
        "var o = {first: 1, ...spread};",
        requiresLanguageModeMessage(Feature.OBJECT_LITERALS_WITH_SPREAD));

    mode = LanguageMode.ECMASCRIPT_2015;
    parseWarning(
        "var o = {first: 1, ...spread};",
        requiresLanguageModeMessage(Feature.OBJECT_LITERALS_WITH_SPREAD));
  }

  @Test
  public void testArrayDestructuringAssignRest() {
    strictMode = SLOPPY;
    expectFeatures(Feature.ARRAY_DESTRUCTURING, Feature.ARRAY_PATTERN_REST);
    parse("[first, ...rest] = foo();");
    // nested destructuring in regular parameters and rest parameters
    parse("[first, {a, b}, ...[re, st, ...{length}]] = foo();");
    // arbitrary LHS assignment target is allowed
    parse("[x, ...y[15]] = foo();");

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning(
        "var [first, ...rest] = foo();",
        requiresLanguageModeMessage(Feature.ARRAY_DESTRUCTURING),
        requiresLanguageModeMessage(Feature.ARRAY_PATTERN_REST));
  }

  @Test
  public void testObjectDestructuringAssignRest() {
    strictMode = SLOPPY;
    expectFeatures(Feature.OBJECT_DESTRUCTURING, Feature.OBJECT_PATTERN_REST);
    parse("const {first, ...rest} = foo();");

    mode = LanguageMode.ECMASCRIPT_2015;
    parseWarning(
        "var {first, ...rest} = foo();", requiresLanguageModeMessage(Feature.OBJECT_PATTERN_REST));
  }

  @Test
  public void testArrayDestructuringAssignRestInvalid() {
    // arbitrary LHS assignment target not allowed
    parseError(
        "var [x, ...y[15]] = foo();",
        "Only an identifier or destructuring pattern is allowed here.");

    parseError(
        "[first, ...more = 'default'] = foo();", "A default value cannot be specified after '...'");
    parseError("var [first, ...more, last] = foo();", "']' expected");
  }

  @Test
  public void testArrayDestructuringFnDeclaration() {
    strictMode = SLOPPY;
    expectFeatures(Feature.ARRAY_DESTRUCTURING);
    parse("function f([x, y]) { use(x); use(y); }");
    parse("function f([x, [y, z]]) {}");
    parse("function f([x, {y, foo: z}]) {}");
    parse("function f([x, y] = [1, 2]) { use(x); use(y); }");
    parse("function f([x1, x2]) {}");
  }

  @Test
  public void testArrayDestructuringFnDeclarationInvalid() {
    // arbitrary LHS expression not allowed as a formal parameter
    parseError(
        "function f([a[0], x]) {}", "Only an identifier or destructuring pattern is allowed here.");
    // restriction applies to sub-patterns
    parseError(
        "function f([a, [x.foo]]) {}",
        "Only an identifier or destructuring pattern is allowed here.");
    parseError(
        "function f([a, {foo: x.foo}]) {}",
        "Only an identifier or destructuring pattern is allowed here.");
  }

  @Test
  public void testObjectDestructuringVar() {
    strictMode = SLOPPY;
    expectFeatures(Feature.OBJECT_DESTRUCTURING);
    parse("var {x, y} = foo();");
    parse("var {x: x, y: y} = foo();");
    parse("var {x: {y, z}} = foo();");
    parse("var {x: {y: {z}}} = foo();");

    // Useless, but legal.
    parse("var {} = foo();");
  }

  @Test
  public void testObjectDestructuringVarInvalid() {
    // Arbitrary LHS target not allowed in declaration
    parseError("var {x.a, y} = foo();", "'}' expected");
    parseError(
        "var {a: x.a, y} = foo();", "Only an identifier or destructuring pattern is allowed here.");
  }

  @Test
  public void testObjectDestructuringVarWithInitializer() {
    strictMode = SLOPPY;
    expectFeatures(Feature.OBJECT_DESTRUCTURING, Feature.DEFAULT_PARAMETERS);
    parse("var {x = 1} = foo();");
    parse("var {x: {y = 1}} = foo();");
    parse("var {x: y = 1} = foo();");
    parse("var {x: v1 = 5, y: v2 = 'str'} = foo();");
    parse("var {k1: {k2 : x} = bar(), k3: y} = foo();");
  }

  @Test
  public void testObjectDestructuringAssign() {
    strictMode = SLOPPY;
    parseError("({x, y}) = foo();", "invalid assignment target");
    expectFeatures(Feature.OBJECT_DESTRUCTURING);
    parse("({x, y} = foo());");
    parse("({x: x, y: y} = foo());");
    parse("({x: {y, z}} = foo());");
    parse("({k1: {k2 : x} = bar(), k3: y} = foo());");

    // Useless, but legal.
    parse("({} = foo());");
  }

  @Test
  public void testObjectDestructuringAssignWithInitializer() {
    strictMode = SLOPPY;
    parseError("({x = 1}) = foo();", "invalid assignment target");
    expectFeatures(Feature.OBJECT_DESTRUCTURING);
    parse("({x = 1} = foo());");
    parse("({x: {y = 1}} = foo());");
    parse("({x: y = 1} = foo());");
    parse("({x: v1 = 5, y: v2 = 'str'} = foo());");
    parse("({k1: {k2 : x} = bar(), k3: y} = foo());");
  }

  @Test
  public void testObjectDestructuringWithInitializerInvalid() {
    parseError("var {{x}} = foo();", "'}' expected");
    parseError("({{x}}) = foo();", "'}' expected");
    parseError("({{a} = {a: 'b'}}) = foo();", "'}' expected");
    parseError("({{a : b} = {a: 'b'}}) = foo();", "'}' expected");
  }

  @Test
  public void testObjectDestructuringFnDeclaration() {
    strictMode = SLOPPY;
    expectFeatures(Feature.OBJECT_DESTRUCTURING);
    parse("function f({x, y}) { use(x); use(y); }");
    parse("function f({w, x: {y, z}}) {}");
    parse("function f({x, y} = {x:1, y:2}) {}");
    parse("function f({x1, x2}) {}");
  }

  @Test
  public void testObjectDestructuringFnDeclarationInvalid() {
    // arbitrary LHS expression not allowed as a formal parameter
    parseError("function f({a[0], x}) {}", "'}' expected");
    parseError(
        "function f({foo: a[0], x}) {}",
        "Only an identifier or destructuring pattern is allowed here.");
    // restriction applies to sub-patterns
    parseError(
        "function f({a, foo: [x.foo]}) {}",
        "Only an identifier or destructuring pattern is allowed here.");
    parseError(
        "function f({a, x: {foo: x.foo}}) {}",
        "Only an identifier or destructuring pattern is allowed here.");
  }

  @Test
  public void testObjectDestructuringComputedProp() {
    strictMode = SLOPPY;

    parseError("var {[x]} = z;", "':' expected");

    expectFeatures(Feature.OBJECT_DESTRUCTURING);
    parse("var {[x]: y} = z;");
    parse("var { [foo()] : [x,y,z] = bar() } = baz();");
  }

  @Test
  public void testObjectDestructuringStringAndNumberKeys() {
    strictMode = SLOPPY;

    parseError("var { 'hello world' } = foo();", "':' expected");
    parseError("var { 4 } = foo();", "':' expected");
    parseError("var { 'hello' = 'world' } = foo();", "':' expected");
    parseError("var { 2 = 5 } = foo();", "':' expected");

    expectFeatures(Feature.OBJECT_DESTRUCTURING);
    parse("var {'s': x} = foo();");
    parse("var {3: x} = foo();");
  }

  /** See https://github.com/google/closure-compiler/issues/1262 */
  @Test
  public void testObjectNumberKeysSpecial() {
    Node n = parse("var a = {12345678901234567890: 2}");

    Node objectLit = n.getFirstChild().getFirstFirstChild();
    assertThat(objectLit.getToken()).isEqualTo(Token.OBJECTLIT);

    Node number = objectLit.getFirstChild();
    assertThat(number.getToken()).isEqualTo(Token.STRING_KEY);
    assertThat(number.getString()).isEqualTo("12345678901234567000");
  }

  @Test
  public void testObjectDestructuringKeywordKeys() {
    strictMode = SLOPPY;
    expectFeatures(Feature.OBJECT_DESTRUCTURING);
    parse("var {if: x, else: y} = foo();");
    parse("var {while: x=1, for: y} = foo();");
    parse("var {type} = foo();");
    parse("var {declare} = foo();");
    parse("var {module} = foo();");
    parse("var {namespace} = foo();");
  }

  @Test
  public void testObjectDestructuringKeywordKeysInvalid() {
    parseError("var {while} = foo();", "cannot use keyword 'while' here.");
    parseError("var {implements} = foo();", "cannot use keyword 'implements' here.");
  }

  @Test
  public void testObjectDestructuringComplexTarget() {
    strictMode = SLOPPY;
    parseError(
        "var {foo: bar.x} = baz();",
        "Only an identifier or destructuring pattern is allowed here.");

    parseError(
        "var {foo: bar[x]} = baz();",
        "Only an identifier or destructuring pattern is allowed here.");

    expectFeatures(Feature.OBJECT_DESTRUCTURING);
    parse("({foo: bar.x} = baz());");
    parse("for ({foo: bar.x} in baz());");

    parse("({foo: bar[x]} = baz());");
    parse("for ({foo: bar[x]} in baz());");
  }

  @Test
  public void testObjectDestructuringExtraParens() {
    strictMode = SLOPPY;
    expectFeatures(Feature.OBJECT_DESTRUCTURING);
    parse("({x: y} = z);");
    parse("({x: (y)} = z);");
    parse("({x: ((y))} = z);");

    expectFeatures(Feature.ARRAY_DESTRUCTURING);
    parse("([x] = y);");
    parse("[(x), y] = z;");
    parse("[x, (y)] = z;");
  }

  @Test
  public void testObjectDestructuringExtraParensInvalid() {
    parseError("[x, ([y])] = z;", INVALID_ASSIGNMENT_TARGET);
    parseError("[x, (([y]))] = z;", INVALID_ASSIGNMENT_TARGET);
  }

  @Test
  public void testObjectLiteralCannotUseDestructuring() {
    strictMode = SLOPPY;
    parseError("var o = {x = 5}", "Default value cannot appear at top level of an object literal.");
  }

  @Test
  public void testMixedDestructuring() {
    strictMode = SLOPPY;
    expectFeatures(Feature.ARRAY_DESTRUCTURING, Feature.OBJECT_DESTRUCTURING);
    parse("var {x: [y, z]} = foo();");
    parse("var [x, {y, z}] = foo();");

    parse("({x: [y, z]} = foo());");
    parse("[x, {y, z}] = foo();");

    parse("function f({x: [y, z]}) {}");
    parse("function f([x, {y, z}]) {}");
  }

  @Test
  public void testMixedDestructuringWithInitializer() {
    strictMode = SLOPPY;
    expectFeatures(Feature.ARRAY_DESTRUCTURING, Feature.OBJECT_DESTRUCTURING);
    parse("var {x: [y, z] = [1, 2]} = foo();");
    parse("var [x, {y, z} = {y: 3, z: 4}] = foo();");

    parse("({x: [y, z] = [1, 2]} = foo());");
    parse("[x, {y, z} = {y: 3, z: 4}] = foo();");

    parse("function f({x: [y, z] = [1, 2]}) {}");
    parse("function f([x, {y, z} = {y: 3, z: 4}]) {}");
  }

  @Test
  public void testDestructuringNoRHS() {
    strictMode = SLOPPY;

    parseError("var {x: y};", "destructuring must have an initializer");
    parseError("let {x: y};", "destructuring must have an initializer");
    parseError("const {x: y};", "const variables must have an initializer");
    parseError("var {x};", "destructuring must have an initializer");
    parseError("let {x};", "destructuring must have an initializer");
    parseError("const {x};", "const variables must have an initializer");
    parseError("var [x, y];", "destructuring must have an initializer");
    parseError("let [x, y];", "destructuring must have an initializer");
    parseError("const [x, y];", "const variables must have an initializer");
  }

  @Test
  public void testComprehensions() {
    strictMode = SLOPPY;
    String error = "unsupported language feature:" + " array/generator comprehensions";

    // array comprehensions
    parseError("[for (x of y) z];", error);
    parseError("[for ({x,y} of z) x+y];", error);
    parseError("[for (x of y) if (x<10) z];", error);
    parseError("[for (a = 5 of v) a];", "'identifier' expected");

    // generator comprehensions
    parseError("(for (x of y) z);", error);
    parseError("(for ({x,y} of z) x+y);", error);
    parseError("(for (x of y) if (x<10) z);", error);
    parseError("(for (a = 5 of v) a);", "'identifier' expected");
  }

  @Test
  public void testLetForbidden1() {
    mode = LanguageMode.ECMASCRIPT5;
    expectFeatures(Feature.LET_DECLARATIONS);
    parseWarning("let x = 3;", requiresLanguageModeMessage(Feature.LET_DECLARATIONS));
  }

  @Test
  public void testLetForbidden2() {
    mode = LanguageMode.ECMASCRIPT5;
    expectFeatures(Feature.LET_DECLARATIONS);
    parseWarning(
        "function f() { let x = 3; };", requiresLanguageModeMessage(Feature.LET_DECLARATIONS));
  }

  @Test
  public void testBlockScopedFunctionDeclaration() {
    expectFeatures(Feature.BLOCK_SCOPED_FUNCTION_DECLARATION);
    parse("{ function foo() {} }");
    parse("if (true) { function foo() {} }");
    parse("{ function* gen() {} }");
    parse("if (true) function foo() {}");
    parse("if (true) function foo() {} else {}");
    parse("if (true) {} else function foo() {}");
    parse("if (true) function foo() {} else function foo() {}");

    mode = LanguageMode.ECMASCRIPT5;
    expectFeatures();
    // Function expressions and functions directly inside other functions do not trigger this
    parse("function foo() {}");
    parse("(function foo() {})");
    parse("function foo() { function bar() {} }");
    parse("{ var foo = function() {}; }");
    parse("{ var foo = function bar() {}; }");
    parse("{ (function() {})(); }");
    parse("{ (function foo() {})(); }");

    parseWarning(
        "{ function f() {} }",
        requiresLanguageModeMessage(Feature.BLOCK_SCOPED_FUNCTION_DECLARATION));
  }

  @Test
  public void testLetForbidden3() {
    mode = LanguageMode.ECMASCRIPT5;
    parseError("function f() { var let = 3; }", "'identifier' expected");

    mode = LanguageMode.ECMASCRIPT_2015;
    parseError("function f() { var let = 3; }", "'identifier' expected");
  }

  @Test
  public void testYieldForbidden() {
    parseError("function f() { yield 3; }", "primary expression expected");
    parseError("function f(x = yield 3) { return x }", "primary expression expected");
    parseError(
        "function *f(x = yield 3) { return x }", "`yield` is illegal in parameter default value.");
    parseError(
        "function *f() { return function*(x = yield 1) { return x; } }",
        "`yield` is illegal in parameter default value.");
  }

  @Test
  public void testGenerator() {
    expectFeatures(Feature.GENERATORS);
    parse("var obj = { *f() { yield 3; } };");
    parse("function* f() { yield 3; }");
    parse("function f() { return function* g() {} }");

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning("function* f() { yield 3; }", requiresLanguageModeMessage(Feature.GENERATORS));
    parseWarning(
        "var obj = { * f() { yield 3; } };",
        requiresLanguageModeMessage(Feature.GENERATORS),
        requiresLanguageModeMessage(Feature.MEMBER_DECLARATIONS));
  }

  @Test
  public void testBracelessFunctionForbidden() {
    parseError("var sq = function(x) x * x;", "'{' expected");
  }

  @Test
  public void testGeneratorsForbidden() {
    parseError("var i = (x for (x in obj));", "')' expected");
  }

  @Test
  public void testGettersForbidden1() {
    mode = LanguageMode.ECMASCRIPT3;
    expectFeatures(Feature.GETTER);
    parseError("var x = {get foo() { return 3; }};", IRFactory.GETTER_ERROR_MESSAGE);
  }

  @Test
  public void testGettersForbidden2() {
    mode = LanguageMode.ECMASCRIPT3;
    parseError("var x = {get foo bar() { return 3; }};", "'(' expected");
  }

  @Test
  public void testGettersForbidden3() {
    mode = LanguageMode.ECMASCRIPT3;
    parseError("var x = {a getter:function b() { return 3; }};", "'}' expected");
  }

  @Test
  public void testGettersForbidden4() {
    mode = LanguageMode.ECMASCRIPT3;
    parseError("var x = {\"a\" getter:function b() { return 3; }};", "':' expected");
  }

  @Test
  public void testGettersForbidden5() {
    mode = LanguageMode.ECMASCRIPT3;
    expectFeatures(Feature.GETTER);
    parseError("var x = {a: 2, get foo() { return 3; }};", IRFactory.GETTER_ERROR_MESSAGE);
  }

  @Test
  public void testGettersForbidden6() {
    mode = LanguageMode.ECMASCRIPT3;
    expectFeatures(Feature.GETTER);
    parseError("var x = {get 'foo'() { return 3; }};", IRFactory.GETTER_ERROR_MESSAGE);
  }

  @Test
  public void testSettersForbidden() {
    mode = LanguageMode.ECMASCRIPT3;
    expectFeatures(Feature.SETTER);
    parseError("var x = {set foo(a) { y = 3; }};", IRFactory.SETTER_ERROR_MESSAGE);
  }

  @Test
  public void testSettersForbidden2() {
    mode = LanguageMode.ECMASCRIPT3;
    // TODO(johnlenz): maybe just report the first error, when not in IDE mode?
    parseError("var x = {a setter:function b() { return 3; }};", "'}' expected");
  }

  @Test
  public void testFileOverviewJSDoc1() {
    isIdeMode = true;

    Node n = parse("/** @fileoverview Hi mom! */ function Foo() {}");
    assertNode(n.getFirstChild()).hasType(Token.FUNCTION);
    assertThat(n.getJSDocInfo()).isNotNull();
    assertThat(n.getFirstChild().getJSDocInfo()).isNull();
    assertThat(n.getJSDocInfo().getFileOverview()).isEqualTo("Hi mom!");
  }

  @Test
  public void testFileOverviewJSDoc_notOnTopOfFile() {
    isIdeMode = true;

    Node n = parse("// some comment \n let x; /** @fileoverview Hi mom! */ class Foo {}");
    assertNode(n).hasType(Token.SCRIPT);
    assertThat(n.getJSDocInfo()).isNotNull();
    assertThat(n.getJSDocInfo().getFileOverview()).isEqualTo("Hi mom!");

    Node letNode = n.getFirstChild();
    assertNode(letNode).hasType(Token.LET);
    assertThat(letNode.getJSDocInfo()).isNull();

    Node classNode = n.getSecondChild();
    assertNode(classNode).hasType(Token.CLASS);
    assertThat(classNode.getJSDocInfo()).isNull();
  }

  @Test
  public void testFileOverviewJSDocDoesNotHoseParsing() {
    assertNode(parse("/** @fileoverview Hi mom! \n */ function Foo() {}").getFirstChild())
        .hasType(Token.FUNCTION);
    assertNode(parse("/** @fileoverview Hi mom! \n * * * */ function Foo() {}").getFirstChild())
        .hasType(Token.FUNCTION);
    assertNode(parse("/** @fileoverview \n * x */ function Foo() {}").getFirstChild())
        .hasType(Token.FUNCTION);
    assertNode(parse("/** @fileoverview \n * x \n */ function Foo() {}").getFirstChild())
        .hasType(Token.FUNCTION);
  }

  @Test
  public void testFileOverviewJSDoc2() {
    isIdeMode = true;

    Node n =
        parse(
            """
            /** @fileoverview Hi mom! */
             /** @constructor */ function Foo() {}
            """);
    assertThat(n.getJSDocInfo()).isNotNull();
    assertThat(n.getJSDocInfo().getFileOverview()).isEqualTo("Hi mom!");
    assertThat(n.getFirstChild().getJSDocInfo()).isNotNull();
    assertThat(n.getFirstChild().getJSDocInfo().hasFileOverview()).isFalse();
    assertThat(n.getFirstChild().getJSDocInfo().isConstructor()).isTrue();
  }

  @Test
  public void testFileoverview_firstOneWins() {
    isIdeMode = true;

    Node n =
        parse(
            """
            /** @fileoverview First */
            /** @fileoverview Second */
            """);

    assertThat(n.getJSDocInfo()).isNotNull();
    assertThat(n.getJSDocInfo().getFileOverview()).isEqualTo("First");
  }

  @Test
  public void testFileoverview_firstOneWins_implicitFileoverview() {
    isIdeMode = true;

    Node n =
        parse(
            """
            /** @typeSummary */
            /** @fileoverview Second */
            """);

    assertThat(n.getJSDocInfo()).isNotNull();
    assertThat(n.getJSDocInfo().isTypeSummary()).isTrue();
    assertThat(n.getJSDocInfo().getFileOverview()).isNull();
  }

  @Test
  public void testFileoverview_firstOneWins_suppressionsAccumulate() {
    isIdeMode = true;

    Node n =
        parse(
            """
            /** @fileoverview @suppress {const} */
            /** @fileoverview @suppress {checkTypes} */
            """);

    assertThat(n.getJSDocInfo()).isNotNull();
    assertThat(n.getJSDocInfo().getSuppressions()).containsExactly("const", "checkTypes");
  }

  @Test
  public void testFileoverview_firstOneWins_externsAccumulate() {
    isIdeMode = true;

    Node n =
        parse(
            """
            /** @fileoverview First */
            /** @externs */
            """);

    assertThat(n.getJSDocInfo()).isNotNull();
    assertThat(n.getJSDocInfo().getFileOverview()).isEqualTo("First");
    assertThat(n.getJSDocInfo().isExterns()).isTrue();
  }

  @Test
  public void testImportantComment() {
    isIdeMode = true;

    Node n = parse("/*! Hi mom! */ function Foo() {}");
    assertNode(n.getFirstChild()).hasType(Token.FUNCTION);
    assertThat(n.getJSDocInfo()).isNotNull();
    assertThat(n.getFirstChild().getJSDocInfo()).isNull();
    assertThat(n.getJSDocInfo().getLicense()).isEqualTo(" Hi mom! ");
  }

  @Test
  public void testBlockCommentParsed() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node n = parse("/* Hi mom! */ \n function Foo() {}");
    assertNode(n.getFirstChild()).hasType(Token.FUNCTION);
    assertThat(n.getFirstChild().getNonJSDocCommentString()).isEqualTo("/* Hi mom! */");
    assertThat(n.getFirstChild().getNonJSDocComment().isInline()).isFalse();
  }

  @Test
  public void testBlockCommentNotParsedWithoutParsingMode() {
    Node n = parse("/* Hi mom! */ \n function Foo() {}");
    assertNode(n.getFirstChild()).hasType(Token.FUNCTION);
    assertThat(n.getFirstChild().getNonJSDocComment()).isNull();
  }

  @Test
  public void testLineCommentParsed() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node n = parse("// Hi mom! \n function Foo() {}");
    assertNode(n.getFirstChild()).hasType(Token.FUNCTION);
    assertThat(n.getFirstChild().getNonJSDocCommentString()).isEqualTo("// Hi mom!");
  }

  @Test
  public void testManyIndividualCommentsGetAttached() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node n = parse("// Hi Mom! \n function Foo() {} \n // Hi Dad! \n  function Bar() {}");
    assertNode(n.getFirstChild()).hasType(Token.FUNCTION);
    assertThat(n.getFirstChild().getNonJSDocCommentString()).isEqualTo("// Hi Mom!");
    assertNode(n.getSecondChild()).hasType(Token.FUNCTION);
    assertThat(n.getSecondChild().getNonJSDocCommentString()).isEqualTo("// Hi Dad!");
  }

  @Test
  public void testIndividualCommentsAroundClasses() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node n = parse("// comment A \n class A{} // trailing a \n // comment B \n  class Bar{}");
    Node classA = n.getFirstChild();
    Node classB = n.getSecondChild();
    assertNode(classA).hasType(Token.CLASS);
    assertThat(classA.getNonJSDocCommentString()).isEqualTo("// comment A");
    assertThat(classA.getTrailingNonJSDocCommentString()).isEqualTo("// trailing a");
    assertNode(classB).hasType(Token.CLASS);
    assertThat(classB.getNonJSDocCommentString()).isEqualTo("// comment B");
  }

  @Test
  public void testMultipleLinedCommentsAttachedToSameNode() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node n = parse("// Hi Mom! \n // And Dad! \n  function Foo() {} ");
    assertNode(n.getFirstChild()).hasType(Token.FUNCTION);
    assertThat(n.getFirstChild().getNonJSDocCommentString()).isEqualTo("// Hi Mom!\n// And Dad!");
  }

  @Test
  public void testEmptyLinesBetweenCommentsIsPreserved() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    Node n = parse("/* Hi Mom!*/ \n\n\n // And Dad! \n  function Foo() {} ");
    assertNode(n.getFirstChild()).hasType(Token.FUNCTION);
    assertThat(n.getFirstChild().getNonJSDocCommentString())
        .isEqualTo("/* Hi Mom!*/\n\n\n// And Dad!");
  }

  @Test
  public void testObjectLiteralDoc1() {
    Node n = parse("var x = {/** @type {number} */ 1: 2};");

    Node objectLit = n.getFirstFirstChild().getFirstChild();
    assertNode(objectLit).hasType(Token.OBJECTLIT);

    Node number = objectLit.getFirstChild();
    assertNode(number).hasType(Token.STRING_KEY);
    assertThat(number.getJSDocInfo()).isNotNull();
  }

  @Test
  public void testDuplicatedParam() {
    parseWarning("function foo(x, x) {}", "Duplicate parameter name \"x\"");
    parseWarning("function foo(x, n, x) {}", "Duplicate parameter name \"x\"");
    parseWarning("function foo(n, x, x) {}", "Duplicate parameter name \"x\"");
    parseWarning("function foo(x, {x}) {}", "Duplicate parameter name \"x\"");
    parseWarning("function foo(x, {n: {x}}) {}", "Duplicate parameter name \"x\"");
    parseWarning("function foo(x, [x]) {}", "Duplicate parameter name \"x\"");
    parseWarning("function foo(x, ...x) {}", "Duplicate parameter name \"x\"");
    parseWarning("function foo(...[x, x]) {}", "Duplicate parameter name \"x\"");
    parseWarning("function foo(...[x,,x]) {}", "Duplicate parameter name \"x\"");
    parseWarning("function foo(x, x = 1) {}", "Duplicate parameter name \"x\"");
    parseWarning("function foo([x, x] = [1, 2]) {}", "Duplicate parameter name \"x\"");
    parseWarning("function foo({x, x} = {x: 1}) {}", "Duplicate parameter name \"x\"");
    parseWarning("function foo(x, {[n()]: x}) {}", "Duplicate parameter name \"x\"");
    parseWarning("function foo(x = x) {}");
    parseWarning("function foo({x: x}) {}");
    parseWarning("function foo(foo) {}");
  }

  @Test
  public void testLetAsIdentifier() {
    mode = LanguageMode.ECMASCRIPT3;
    strictMode = SLOPPY;
    parse("var let");

    mode = LanguageMode.ECMASCRIPT5;
    parse("var let");

    mode = LanguageMode.ECMASCRIPT5;
    strictMode = STRICT;
    parseError("var let", "'identifier' expected");

    mode = LanguageMode.ECMASCRIPT_2015;
    strictMode = SLOPPY;
    parse("var let");

    mode = LanguageMode.ECMASCRIPT_2015;
    strictMode = STRICT;
    parseError("var let", "'identifier' expected");
  }

  @Test
  public void testLet() {
    strictMode = SLOPPY;
    expectFeatures(Feature.LET_DECLARATIONS);

    parse("let x;");
    parse("let x = 1;");
    parse("let x, y = 2;");
    parse("let x = 1, y = 2;");
  }

  @Test
  public void testConst() {
    strictMode = SLOPPY;

    parseError("const x;", "const variables must have an initializer");
    parseError("const x, y = 2;", "const variables must have an initializer");

    expectFeatures(Feature.CONST_DECLARATIONS);

    parse("const x = 1;");
    parse("const x = 1, y = 2;");
  }

  @Test
  public void testYield1() {
    mode = LanguageMode.ECMASCRIPT3;
    strictMode = SLOPPY;
    parse("var yield");

    mode = LanguageMode.ECMASCRIPT5;
    parse("var yield");

    mode = LanguageMode.ECMASCRIPT5;
    strictMode = STRICT;
    parseError("var yield", "'identifier' expected");

    mode = LanguageMode.ECMASCRIPT_2015;
    strictMode = SLOPPY;
    parse("var yield");

    mode = LanguageMode.ECMASCRIPT_2015;
    strictMode = STRICT;
    parseError("var yield", "'identifier' expected");
  }

  @Test
  public void testYield2() {
    expectFeatures(Feature.GENERATORS);
    parse("function * f() { yield; }");
    parse("function * f() { yield /a/i; }");

    expectFeatures();
    parseError("function * f() { 1 + yield; }", "primary expression expected");
    parseError("function * f() { 1 + yield 2; }", "primary expression expected");
    parseError("function * f() { yield 1 + yield 2; }", "primary expression expected");
    parseError("function * f() { yield(1) + yield(2); }", "primary expression expected");
    expectFeatures(Feature.GENERATORS);
    parse("function * f() { (yield 1) + (yield 2); }"); // OK
    parse("function * f() { yield * yield; }"); // OK  (yield * (yield))
    expectFeatures();
    parseError("function * f() { yield + yield; }", "primary expression expected");
    expectFeatures(Feature.GENERATORS);
    parse("function * f() { (yield) + (yield); }"); // OK
    parse("function * f() { return yield; }"); // OK
    parse("function * f() { return yield 1; }"); // OK
    parse(
        """
        function * f() {
          yield * // line break allowed here
              [1, 2, 3];
        }
        """);
    expectFeatures();
    parseError(
        """
        function * f() {
          yield // line break not allowed here
              *[1, 2, 3];
        }
        """,
        "'}' expected");
    parseError("function * f() { yield *; }", "yield* requires an expression");
  }

  @Test
  public void testYield3() {
    expectFeatures(Feature.GENERATORS);
    // TODO(johnlenz): validate "yield" parsing. Firefox rejects this
    // use of "yield".
    parseError("function * f() { yield , yield; }");
  }

  private static final String STRING_CONTINUATIONS_WARNING =
      """
      String continuations are not recommended. \
      See https://google.github.io/styleguide/jsguide.html#features-strings-no-line-continuations
      """
          .stripTrailing();

  @Test
  public void testStringLineContinuationWarningsByMode() {
    expectFeatures(Feature.STRING_CONTINUATION);
    strictMode = SLOPPY;

    mode = LanguageMode.ECMASCRIPT3;
    parseWarning(
        "'one\\\ntwo';",
        requiresLanguageModeMessage(Feature.STRING_CONTINUATION),
        STRING_CONTINUATIONS_WARNING);

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning("'one\\\ntwo';", STRING_CONTINUATIONS_WARNING);

    mode = LanguageMode.ECMASCRIPT_2015;
    parseWarning("'one\\\ntwo';", STRING_CONTINUATIONS_WARNING);
  }

  @Test
  public void testStringLineContinuationNormalization() {
    expectFeatures(Feature.STRING_CONTINUATION);
    strictMode = SLOPPY;

    Node n = parseWarning("'one\\\ntwo';", STRING_CONTINUATIONS_WARNING);
    assertThat(n.getFirstFirstChild().getString()).isEqualTo("onetwo");

    n = parseWarning("'one\\\rtwo';", STRING_CONTINUATIONS_WARNING);
    assertThat(n.getFirstFirstChild().getString()).isEqualTo("onetwo");

    n = parseWarning("'one\\\r\ntwo';", STRING_CONTINUATIONS_WARNING);
    assertThat(n.getFirstFirstChild().getString()).isEqualTo("onetwo");

    n = parseWarning("'one \\\ntwo';", STRING_CONTINUATIONS_WARNING);
    assertThat(n.getFirstFirstChild().getString()).isEqualTo("one two");

    n = parseWarning("'one\\\n two';", STRING_CONTINUATIONS_WARNING);
    assertThat(n.getFirstFirstChild().getString()).isEqualTo("one two");
  }

  /** See https://github.com/google/closure-compiler/issues/3492 */
  @Test
  public void testStringContinuationIssue3492() {
    expectFeatures(Feature.STRING_CONTINUATION);
    strictMode = SLOPPY;

    // This test runs on this input code, which technically has STRING_CONTINUATIONS_WARNINGs at 2
    // places:
    // ```
    // function x() {
    //    a = "\ <--- STRING_CONTINUATIONS_WARNING
    //    \ \ <--- STRING_CONTINUATIONS_WARNING
    //    ";
    // };
    // ```
    // We deduplicate based on the key `sourceName:line_no:col_no:warning` being the same. Here,
    // even though the `STRING_CONTINUATIONS_WARNING` happens at 2 places, JSCompiler reports the
    // warnings within a string with a location pointing to the start of the string token. This
    // happens here `charno(token.location.start)`:
    // google3/third_party/java_src/jscomp/java/com/google/javascript/jscomp/parsing/IRFactory.java?rcl=719034735&l=3482
    // Hence, after deduplication, we only get one error.
    parseWarning(
        """
        function x() {
                a = "\\
                \\ \\
                ";
        };
        """,
        "Unnecessary escape: '\\ ' is equivalent to just ' '",
        STRING_CONTINUATIONS_WARNING);
  }

  @Test
  public void testStringLiteral() {
    Node n = parse("'foo'");
    Node stringNode = n.getFirstFirstChild();
    assertNode(stringNode).hasType(Token.STRINGLIT);
    assertThat(stringNode.getString()).isEqualTo("foo");
  }

  private Node testTemplateLiteral(String s) {
    mode = LanguageMode.ECMASCRIPT5;
    strictMode = SLOPPY;
    parseWarning(s, requiresLanguageModeMessage(Feature.TEMPLATE_LITERALS));

    mode = LanguageMode.ECMASCRIPT_2015;
    return parse(s);
  }

  private void assertSimpleTemplateLiteral(String expectedContents, String literal) {
    Node node = testTemplateLiteral(literal).getFirstFirstChild();
    assertNode(node).hasType(Token.TEMPLATELIT);
    assertThat(node.getChildCount()).isEqualTo(1);
    assertNode(node.getFirstChild()).hasType(Token.TEMPLATELIT_STRING);
    assertThat(node.getFirstChild().getCookedString()).isEqualTo(expectedContents);
  }

  @Test
  public void testUseTemplateLiteral() {
    expectFeatures(Feature.TEMPLATE_LITERALS);
    testTemplateLiteral("f`hello world`;");
    testTemplateLiteral("`hello ${name} ${world}`.length;");
  }

  @Test
  public void testTemplateLiterals() {
    expectFeatures(Feature.TEMPLATE_LITERALS);
    testTemplateLiteral("``");
    testTemplateLiteral("`\"`");
    testTemplateLiteral("`\\``");
    testTemplateLiteral("`hello world`;");
    testTemplateLiteral("`hello\nworld`;");
    testTemplateLiteral("`string containing \\`escaped\\` backticks`;");
    testTemplateLiteral("{ `in block` }");
    testTemplateLiteral("{ `in ${block}` }");
  }

  @Test
  public void testEscapedTemplateLiteral() {
    expectFeatures(Feature.TEMPLATE_LITERALS);
    assertSimpleTemplateLiteral("${escaped}", "`\\${escaped}`");
  }

  @Test
  public void testTemplateLiteralWithNulChar() {
    expectFeatures(Feature.TEMPLATE_LITERALS);
    strictMode = SLOPPY;
    parse("var test = `\nhello\\0`");
  }

  @Test
  public void testTemplateLiteralWithNewline() {
    expectFeatures(Feature.TEMPLATE_LITERALS);
    assertSimpleTemplateLiteral("hello\nworld", "`hello\nworld`");
    assertSimpleTemplateLiteral("\n", "`\r`");
    assertSimpleTemplateLiteral("\n", "`\r\n`");
    assertSimpleTemplateLiteral("\\\n", "`\\\\\n`");
    assertSimpleTemplateLiteral("\\\n", "`\\\\\r\n`");
    assertSimpleTemplateLiteral("\r\n", "`\\r\\n`"); // template literals support explicit escapes
    assertSimpleTemplateLiteral("\\r\\n", "`\\\\r\\\\n`"); // note: no actual newlines here
  }

  @Test
  public void testTemplateLiteralWithLineContinuation() {
    strictMode = SLOPPY;
    expectFeatures(Feature.TEMPLATE_LITERALS);
    Node n = parseWarning("`string \\\ncontinuation`", STRING_CONTINUATIONS_WARNING);
    Node templateLiteral = n.getFirstFirstChild();
    Node stringNode = templateLiteral.getFirstChild();
    assertNode(stringNode).hasType(Token.TEMPLATELIT_STRING);
    assertThat(stringNode.getCookedString()).isEqualTo("string continuation");
  }

  @Test
  public void testTemplateLiteralSubstitution() {
    strictMode = SLOPPY;
    expectFeatures(Feature.TEMPLATE_LITERALS);
    parse("`hello ${name}`;");
    parse("`hello ${name} ${world}`;");
    parse("`hello ${name }`");

    expectFeatures();
    parseError("`hello ${name", "Expected '}' after expression in template literal");
    parseError("`hello ${name tail}", "Expected '}' after expression in template literal");
  }

  @Test
  public void testUnterminatedTemplateLiteral() {
    strictMode = SLOPPY;
    parseError("`hello", "Unterminated template literal");
    parseError("`hello\\`", "Unterminated template literal");
  }

  @Test
  public void testTemplateLiteralOctalEscapes() {
    assertSimpleTemplateLiteral("\0", "`\\0`");
    assertSimpleTemplateLiteral("aaa\0aaa", "`aaa\\0aaa`");
  }

  @Test
  public void testIncorrectEscapeSequenceInTemplateLiteral() {
    parseError("`hello\\x`", "Hex digit expected");

    parseError("`hello\\1`", "Invalid escape sequence");
    parseError("`hello\\2`", "Invalid escape sequence");
    parseError("`hello\\3`", "Invalid escape sequence");
    parseError("`hello\\4`", "Invalid escape sequence");
    parseError("`hello\\5`", "Invalid escape sequence");
    parseError("`hello\\6`", "Invalid escape sequence");
    parseError("`hello\\7`", "Invalid escape sequence");
    parseError("`hello\\8`", "Invalid escape sequence");
    parseError("`hello\\9`", "Invalid escape sequence");
    parseError("`hello\\00`", "Invalid escape sequence");
    parseError("`hello\\01`", "Invalid escape sequence");
    parseError("`hello\\02`", "Invalid escape sequence");
    parseError("`hello\\03`", "Invalid escape sequence");
    parseError("`hello\\04`", "Invalid escape sequence");
    parseError("`hello\\05`", "Invalid escape sequence");
    parseError("`hello\\06`", "Invalid escape sequence");
    parseError("`hello\\07`", "Invalid escape sequence");
    parseError("`hello\\08`", "Invalid escape sequence");
    parseError("`hello\\09`", "Invalid escape sequence");

    // newline before invalid escape sequence
    parseError("`\n\\1`", "Invalid escape sequence");
    parseError("`\n\\1 ${0}`", "Invalid escape sequence");
  }

  @Test
  public void testTemplateLiteralSubstitutionWithCast() {
    Node root = parse("`${ /** @type {?} */ (3)}`");
    Node exprResult = root.getFirstChild();
    Node templateLiteral = exprResult.getFirstChild();
    assertNode(templateLiteral).hasType(Token.TEMPLATELIT);

    Node substitution = templateLiteral.getSecondChild();
    assertNode(substitution).hasType(Token.TEMPLATELIT_SUB);

    Node cast = substitution.getFirstChild();
    assertNode(cast).hasType(Token.CAST);

    Node number = cast.getFirstChild();
    assertNode(number).hasType(Token.NUMBER);
  }

  @Test
  public void testExponentialLiterals() {
    parse("0e0");
    parse("0E0");
    parse("0E1");
    parse("1E0");
    parse("1E-0");
    parse("10E10");
    parse("10E-10");
    parse("1.0E1");
    parseError("01E0", SEMICOLON_EXPECTED);
    parseError("0E", "Exponent part must contain at least one digit");
    parseError("1E-", "Exponent part must contain at least one digit");
    parseError("1E1.1", SEMICOLON_EXPECTED);
  }

  @Test
  public void testBigIntLiteralZero() {
    Node bigint =
        parse("0n;") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild(); // BIGINT
    assertNode(bigint).hasType(Token.BIGINT);
    assertNode(bigint).hasLineno(1);
    assertNode(bigint).hasCharno(0);
    assertNode(bigint).hasLength(2);
    assertNode(bigint).isBigInt(BigInteger.ZERO);
  }

  @Test
  public void testBigIntLiteralPositive() {
    Node bigint =
        parse("1n;") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild(); // BIGINT
    assertNode(bigint).hasType(Token.BIGINT);
    assertNode(bigint).hasLineno(1);
    assertNode(bigint).hasCharno(0);
    assertNode(bigint).hasLength(2);
    assertNode(bigint).isBigInt(BigInteger.ONE);
  }

  @Test
  public void testBigIntLiteralNegative() {
    Node neg =
        parse("-1n;") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild(); // NEG

    assertNode(neg).hasType(Token.NEG);
    assertNode(neg).hasLineno(1);
    assertNode(neg).hasCharno(0);
    assertNode(neg).hasLength(3);

    Node bigint = neg.getOnlyChild();
    assertNode(bigint).hasType(Token.BIGINT);
    assertNode(bigint).hasLineno(1);
    assertNode(bigint).hasCharno(1);
    assertNode(bigint).hasLength(2);
    assertNode(bigint).isBigInt(BigInteger.ONE);
  }

  @Test
  public void testBigIntLiteralBinary() {
    Node bigint =
        parse("0b10000n;") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild(); // BIGINT
    assertNode(bigint).hasType(Token.BIGINT);
    assertNode(bigint).hasLineno(1);
    assertNode(bigint).hasCharno(0);
    assertNode(bigint).hasLength(8);
    assertNode(bigint).isBigInt(new BigInteger("16"));
  }

  @Test
  public void testBigIntLiteralOctal() {
    Node bigint =
        parse("0o100n;") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild(); // BIGINT
    assertNode(bigint).hasType(Token.BIGINT);
    assertNode(bigint).hasLineno(1);
    assertNode(bigint).hasCharno(0);
    assertNode(bigint).hasLength(6);
    assertNode(bigint).isBigInt(new BigInteger("64"));
  }

  @Test
  public void testBigIntLiteralHex() {
    Node bigint =
        parse("0xFn;") // SCRIPT
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild(); // BIGINT
    assertNode(bigint).hasType(Token.BIGINT);
    assertNode(bigint).hasLineno(1);
    assertNode(bigint).hasCharno(0);
    assertNode(bigint).hasLength(4);
    assertNode(bigint).isBigInt(new BigInteger("15"));
  }

  @Test
  public void testBigIntInFunctionStatement() {
    Node add =
        parse("function f(/** @type {bigint} */ x) { 0n + x }") // SCRIPT
            .getOnlyChild() // FUNCTION
            .getLastChild() // BLOCK
            .getOnlyChild() // EXPR_RESULT
            .getOnlyChild(); // ADD
    assertNode(add).hasToken(Token.ADD);
    assertNode(add.getFirstChild()).isBigInt(BigInteger.ZERO);
    assertNode(add.getLastChild()).isName("x");
  }

  @Test
  public void testBigIntLiteralErrors() {
    parseError("01n;", "SyntaxError: nonzero BigInt can't have leading zero");
    parseError(".1n", "Semi-colon expected");
    parseError("0.1n", "Semi-colon expected");
    parseError("1e1n", "Semi-colon expected");
  }

  @Test
  public void testBigIntLiteralWarning() {
    mode = LanguageMode.ECMASCRIPT_2019;
    parseWarning(
        "1n;",
        "This language feature is only supported for ECMASCRIPT_2020 mode or better: bigint");
  }

  @Test
  public void testBigIntFeatureRecorded() {
    parse("1n;");
    expectFeatures(Feature.BIGINT);
  }

  @Test
  public void testBigIntLiteralInCall() {
    parse("alert(1n)");
  }

  @Test
  public void testBinaryLiterals() {
    expectFeatures(Feature.BINARY_LITERALS);
    mode = LanguageMode.ECMASCRIPT3;
    strictMode = SLOPPY;
    parseWarning("0b0001;", requiresLanguageModeMessage(Feature.BINARY_LITERALS));
    mode = LanguageMode.ECMASCRIPT5;
    parseWarning("0b0001;", requiresLanguageModeMessage(Feature.BINARY_LITERALS));
    mode = LanguageMode.ECMASCRIPT_2015;
    parse("0b0001;");
  }

  @Test
  public void testOctalLiterals() {
    expectFeatures(Feature.OCTAL_LITERALS);
    mode = LanguageMode.ECMASCRIPT3;
    strictMode = SLOPPY;
    parseWarning("0o0001;", requiresLanguageModeMessage(Feature.OCTAL_LITERALS));
    mode = LanguageMode.ECMASCRIPT5;
    parseWarning("0o0001;", requiresLanguageModeMessage(Feature.OCTAL_LITERALS));
    mode = LanguageMode.ECMASCRIPT_2015;
    parse("0o0001;");
  }

  @Test
  public void testOctalEscapes() {
    mode = LanguageMode.ECMASCRIPT3;
    strictMode = SLOPPY;
    Node n = parse("var x = 'This is a \251 copyright symbol.'"); // 251 is the octal value for "©"
    assertNode(n.getFirstFirstChild()).isName("x");
    assertNode(n.getFirstFirstChild().getFirstChild()).isString("This is a © copyright symbol.");
  }

  @Test
  public void testOldStyleOctalLiterals() {
    mode = LanguageMode.ECMASCRIPT3;
    strictMode = SLOPPY;
    parseWarning("0001;", "Octal integer literals are not supported in strict mode.");

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning("0001;", "Octal integer literals are not supported in strict mode.");

    mode = LanguageMode.ECMASCRIPT_2015;
    parseWarning("0001;", "Octal integer literals are not supported in strict mode.");
  }

  @Test
  public void testOldStyleOctalLiterals_strictMode() {
    strictMode = STRICT;

    mode = LanguageMode.ECMASCRIPT5;
    parseError("0001;", "Octal integer literals are not supported in strict mode.");

    mode = LanguageMode.ECMASCRIPT_2015;
    parseError("0001;", "Octal integer literals are not supported in strict mode.");
  }

  @Test
  public void testInvalidOctalLiterals() {
    mode = LanguageMode.ECMASCRIPT3;
    strictMode = SLOPPY;
    parseError("0o08;", "Invalid octal digit in octal literal.");

    mode = LanguageMode.ECMASCRIPT5;
    parseError("0o08;", "Invalid octal digit in octal literal.");

    mode = LanguageMode.ECMASCRIPT_2015;
    parseError("0o08;", "Invalid octal digit in octal literal.");
  }

  @Test
  public void testInvalidOldStyleOctalLiterals() {
    mode = LanguageMode.ECMASCRIPT3;
    strictMode = SLOPPY;
    parseError("08;", "Invalid octal digit in octal literal.");
    parseError("01238;", "Invalid octal digit in octal literal.");

    mode = LanguageMode.ECMASCRIPT5;
    parseError("08;", "Invalid octal digit in octal literal.");
    parseError("01238;", "Invalid octal digit in octal literal.");

    mode = LanguageMode.ECMASCRIPT_2015;
    parseError("08;", "Invalid octal digit in octal literal.");
    parseError("01238;", "Invalid octal digit in octal literal.");
  }

  @Test
  public void testGetter_ObjectLiteral_Es3() {
    expectFeatures(Feature.GETTER);
    mode = LanguageMode.ECMASCRIPT3;
    strictMode = SLOPPY;

    parseError("var x = {get 1(){}};", IRFactory.GETTER_ERROR_MESSAGE);
    parseError("var x = {get 'a'(){}};", IRFactory.GETTER_ERROR_MESSAGE);
    parseError("var x = {get a(){}};", IRFactory.GETTER_ERROR_MESSAGE);
    mode = LanguageMode.ECMASCRIPT5;
    parse("var x = {get 1(){}};");
    parse("var x = {get 'a'(){}};");
    parse("var x = {get a(){}};");
  }

  @Test
  public void testGetter_ObjectLiteral_Es5() {
    expectFeatures(Feature.GETTER);
    mode = LanguageMode.ECMASCRIPT5;
    strictMode = SLOPPY;

    parse("var x = {get 1(){}};");
    parse("var x = {get 'a'(){}};");
    parse("var x = {get a(){}};");
  }

  @Test
  public void testGetterInvalid_ObjectLiteral_EsNext() {
    expectFeatures();
    strictMode = SLOPPY;

    parseError("var x = {get a(b){}};", "')' expected");
  }

  @Test
  public void testGetter_Computed_ObjectLiteral_Es6() {
    expectFeatures(Feature.GETTER, Feature.COMPUTED_PROPERTIES);
    strictMode = SLOPPY;

    parse("var x = {get [1](){}};");
    parse("var x = {get ['a'](){}};");
    parse("var x = {get [a](){}};");
  }

  @Test
  public void testGetterInvalid_Computed_ObjectLiteral_EsNext() {
    expectFeatures();
    strictMode = SLOPPY;

    parseError("var x = {get [a](b){}};", "')' expected");
  }

  @Test
  public void testGetter_ClassSyntax() {
    expectFeatures(Feature.CLASSES, Feature.GETTER);
    strictMode = SLOPPY;

    parse("class Foo { get 1() {} };");
    parse("class Foo { get 'a'() {} };");
    parse("class Foo { get a() {} };");
  }

  @Test
  public void testGetterInvalid_ClassSyntax_EsNext() {
    expectFeatures();
    strictMode = SLOPPY;

    parseError("class Foo { get a(b) {} };", "')' expected");
  }

  @Test
  public void testGetter_Computed_ClassSyntax() {
    expectFeatures(Feature.CLASSES, Feature.GETTER, Feature.COMPUTED_PROPERTIES);
    strictMode = SLOPPY;

    parse("class Foo { get [1]() {} };");
    parse("class Foo { get ['a']() {} };");
    parse("class Foo { get [a]() {} };");
  }

  @Test
  public void testGetterInvalid_Computed_ClassSyntax_EsNext() {
    expectFeatures();
    strictMode = SLOPPY;

    parseError("class Foo { get [a](b) {} };", "')' expected");
  }

  @Test
  public void testSetter_ObjectLiteral_Es3() {
    expectFeatures(Feature.SETTER);
    mode = LanguageMode.ECMASCRIPT3;
    strictMode = SLOPPY;

    parseError("var x = {set 1(x){}};", IRFactory.SETTER_ERROR_MESSAGE);
    parseError("var x = {set 'a'(x){}};", IRFactory.SETTER_ERROR_MESSAGE);
    parseError("var x = {set a(x){}};", IRFactory.SETTER_ERROR_MESSAGE);
  }

  @Test
  public void testSetter_ObjectLiteral_Es5() {
    expectFeatures(Feature.SETTER);
    mode = LanguageMode.ECMASCRIPT5;
    strictMode = SLOPPY;

    parse("var x = {set 1(x){}};");
    parse("var x = {set 'a'(x){}};");
    parse("var x = {set a(x){}};");
  }

  // We only cover some of the common permutations though.
  @Test
  public void testSetter_ObjectLiteral_Es6() {
    expectFeatures(Feature.SETTER);
    mode = LanguageMode.ECMASCRIPT_2015;
    strictMode = SLOPPY;

    parse("var x = {set 1(x){}};");
    parse("var x = {set 'a'(x){}};");
    parse("var x = {set a(x){}};");

    parse("var x = {set setter(x = 5) {}};");
    parse("var x = {set setter(x = a) {}};");
    parse("var x = {set setter(x = a + 5) {}};");

    parse("var x = {set setter([x, y, z]) {}};");
    parse("var x = {set setter([x, y, ...z]) {}};");
    parse("var x = {set setter([x, y, z] = [1, 2, 3]) {}};");
    parse("var x = {set setter([x = 1, y = 2, z = 3]) {}};");

    parse("var x = {set setter({x, y, z}) {}};");
    parse("var x = {set setter({x, y, z} = {x: 1, y: 2, z: 3}) {}};");
    parse("var x = {set setter({x = 1, y = 2, z = 3}) {}};");
  }

  @Test
  public void testSetterInvalid_ObjectLiteral_EsNext() {
    expectFeatures();
    strictMode = SLOPPY;

    parseError("var x = {set a() {}};", "Setter must have exactly 1 parameter, found 0");
    parseError("var x = {set a(x, y) {}};", "Setter must have exactly 1 parameter, found 2");
    parseError("var x = {set a(...x, y) {}};", "Setter must have exactly 1 parameter, found 2");
    parseError("var x = {set a(...x) {}};", "Setter must not have a rest parameter");
  }

  // We only cover some of the common permutations though.
  @Test
  public void testSetter_Computed_ObjectLiteral_Es6() {
    expectFeatures(Feature.SETTER, Feature.COMPUTED_PROPERTIES);
    mode = LanguageMode.ECMASCRIPT_2015;
    strictMode = SLOPPY;

    parse("var x = {set [setter](x = 5) {}};");
    parse("var x = {set [setter](x = a) {}};");
    parse("var x = {set [setter](x = a + 5) {}};");

    parse("var x = {set [setter]([x, y, z]) {}};");
    parse("var x = {set [setter]([x, y, ...z]) {}};");
    parse("var x = {set [setter]([x, y, z] = [1, 2, 3]) {}};");
    parse("var x = {set [setter]([x = 1, y = 2, z = 3]) {}};");

    parse("var x = {set [setter]({x, y, z}) {}};");
    parse("var x = {set [setter]({x, y, z} = {x: 1, y: 2, z: 3}) {}};");
    parse("var x = {set [setter]({x = 1, y = 2, z = 3}) {}};");
  }

  // We only cover some of the common permutations though.
  @Test
  public void testSetterInvalid_Computed_ObjectLiteral_EsNext() {
    expectFeatures();
    strictMode = SLOPPY;

    parseError("var x = {set [setter]() {}};", "Setter must have exactly 1 parameter, found 0");
    parseError("var x = {set [setter](x, y) {}};", "Setter must have exactly 1 parameter, found 2");
    parseError(
        "var x = {set [setter](...x, y) {}};", "Setter must have exactly 1 parameter, found 2");
    parseError("var x = {set [setter](...x) {}};", "Setter must not have a rest parameter");
  }

  @Test
  public void testSetter_ClassSyntax() {
    expectFeatures(Feature.CLASSES, Feature.SETTER);

    parse("class Foo { set setter(x = 5) {} };");
    parse("class Foo { set setter(x = a) {} };");
    parse("class Foo { set setter(x = a + 5) {} };");

    parse("class Foo { set setter([x, y, z]) {} };");
    parse("class Foo { set setter([x, y, ...z]) {}};");
    parse("class Foo { set setter([x, y, z] = [1, 2, 3]) {} };");
    parse("class Foo { set setter([x = 1, y = 2, z = 3]) {} };");

    parse("class Foo { set setter({x, y, z}) {}};");
    parse("class Foo { set setter({x, y, z} = {x: 1, y: 2, z: 3}) {} };");
    parse("class Foo { set setter({x = 1, y = 2, z = 3}) {} };");
  }

  @Test
  public void testSetterInvalid_ClassSyntax_EsNext() {
    expectFeatures();

    parseError("class Foo { set setter() {} };", "Setter must have exactly 1 parameter, found 0");
    parseError(
        "class Foo { set setter(x, y) {} };", "Setter must have exactly 1 parameter, found 2");
    parseError(
        "class Foo { set setter(...x, y) {} };", "Setter must have exactly 1 parameter, found 2");
    parseError("class Foo { set setter(...x) {} };", "Setter must not have a rest parameter");
  }

  // We only cover some of the common permutations though.
  @Test
  public void testSetter_Computed_ClassSyntax() {
    expectFeatures(Feature.CLASSES, Feature.SETTER, Feature.COMPUTED_PROPERTIES);
    mode = LanguageMode.ECMASCRIPT_2015;

    parse("class Foo { set [setter](x = 5) {} };");
    parse("class Foo { set [setter](x = a) {} };");
    parse("class Foo { set [setter](x = a + 5) {} };");

    parse("class Foo { set [setter]([x, y, z]) {} };");
    parse("class Foo { set [setter]([x, y, ...z]) {}};");
    parse("class Foo { set [setter]([x, y, z] = [1, 2, 3]) {} };");
    parse("class Foo { set [setter]([x = 1, y = 2, z = 3]) {} };");

    parse("class Foo { set [setter]({x, y, z}) {}};");
    parse("class Foo { set [setter]({x, y, z} = {x: 1, y: 2, z: 3}) {} };");
    parse("class Foo { set [setter]({x = 1, y = 2, z = 3}) {} };");
  }

  @Test
  public void testSetterInvalid_Computed_ClassSyntax_EsNext() {
    expectFeatures();

    parseError("class Foo { set [setter]() {} };", "Setter must have exactly 1 parameter, found 0");
    parseError(
        "class Foo { set [setter](x, y) {} };", "Setter must have exactly 1 parameter, found 2");
    parseError(
        "class Foo { set [setter](...x, y) {} };", "Setter must have exactly 1 parameter, found 2");
    parseError("class Foo { set [setter](...x) {} };", "Setter must not have a rest parameter");
  }

  @Test
  public void testLamestWarningEver() {
    // This used to be a warning.
    parse("var x = /** @type {undefined} */ (y);");
    parse("var x = /** @type {void} */ (y);");
  }

  @Test
  public void testUnfinishedComment() {
    parseError("/** this is a comment ", "unterminated comment");
  }

  @Test
  public void testHtmlStartCommentAtStartOfLine() {
    parseWarning("<!-- This text is ignored.\nalert(1)", HTML_COMMENT_WARNING);
  }

  @Test
  public void testHtmlStartComment() {
    parseWarning("alert(1) <!-- This text is ignored.\nalert(2)", HTML_COMMENT_WARNING);
  }

  @Test
  public void testHtmlEndCommentAtStartOfLine() {
    parseWarning("alert(1)\n --> This text is ignored.", HTML_COMMENT_WARNING);
  }

  // "-->" is not the start of a comment, when it is not at the beginning
  // of a line.
  @Test
  public void testHtmlEndComment() {
    parse("while (x --> 0) {\n  alert(1)\n}");
  }

  @Test
  public void testParseBlockDescription() {
    isIdeMode = true;

    Node n = parse("/** This is a variable. */ var x;");
    Node var = n.getFirstChild();
    assertThat(var.getJSDocInfo()).isNotNull();
    assertThat(var.getJSDocInfo().getBlockDescription()).isEqualTo("This is a variable.");
  }

  @Test
  public void testUnnamedFunctionStatement() {
    // Statements
    parseError("function() {};", "'identifier' expected");
    parseError("if (true) { function() {}; }", "'identifier' expected");
    parse("function f() {};");
    // Expressions
    parse("(function f() {});");
    parse("(function () {});");
  }

  @Test
  public void testReservedKeywords() {
    expectFeatures(Feature.ES3_KEYWORDS_AS_IDENTIFIERS);
    mode = LanguageMode.ECMASCRIPT3;
    strictMode = SLOPPY;

    parseError("var boolean;", "identifier is a reserved word");
    parseError("function boolean() {};", "identifier is a reserved word");
    parseError("boolean = 1;", "identifier is a reserved word");

    expectFeatures();
    parseError("class = 1;", "'identifier' expected");
    parseError("public = 2;", "primary expression expected");

    mode = LanguageMode.ECMASCRIPT5;

    expectFeatures(Feature.ES3_KEYWORDS_AS_IDENTIFIERS);
    parse("var boolean;");
    parse("function boolean() {};");
    parse("boolean = 1;");

    expectFeatures();
    parseError("class = 1;", "'identifier' expected");
    parseError("var import = 0;", "'identifier' expected");
    // TODO(johnlenz): reenable
    // parse("public = 2;");

    mode = LanguageMode.ECMASCRIPT5;

    expectFeatures(Feature.ES3_KEYWORDS_AS_IDENTIFIERS);
    parse("var boolean;");
    parse("function boolean() {};");
    parse("boolean = 1;");

    expectFeatures();
    parseError("public = 2;", "primary expression expected");
    parseError("class = 1;", "'identifier' expected");

    mode = LanguageMode.ECMASCRIPT_2015;
    strictMode = SLOPPY;
    parseError("const else = 1;", "'identifier' expected");
  }

  @Test
  public void testTypeScriptKeywords() {
    parse("type = 2;");
    parse("var type = 3;");
    parse("type\nx = 5");
    parse("while (i--) { type = types[i]; }");

    parse("declare = 2;");
    parse("var declare = 3;");
    parse("declare\nx = 5");
    parse("while (i--) { declare = declares[i]; }");

    parse("module = 2;");
    parse("var module = 3;");
    parse("module\nx = 5");
    parse("while (i--) { module = module[i]; }");
  }

  @Test
  public void testKeywordsAsProperties1() {
    expectFeatures(Feature.KEYWORDS_AS_PROPERTIES);
    mode = LanguageMode.ECMASCRIPT3;
    strictMode = SLOPPY;

    parseWarning("var x = {function: 1};", IRFactory.INVALID_ES3_PROP_NAME);
    parseWarning("x.function;", IRFactory.INVALID_ES3_PROP_NAME);
    parseWarning("var x = {class: 1};", IRFactory.INVALID_ES3_PROP_NAME);
    expectFeatures();
    parse("var x = {'class': 1};");
    expectFeatures(Feature.KEYWORDS_AS_PROPERTIES);
    parseWarning("x.class;", IRFactory.INVALID_ES3_PROP_NAME);
    expectFeatures();
    parse("x['class'];");
    parse("var x = {let: 1};"); // 'let' is not reserved in ES3
    parse("x.let;");
    parse("var x = {yield: 1};"); // 'yield' is not reserved in ES3
    parse("x.yield;");
    expectFeatures(Feature.KEYWORDS_AS_PROPERTIES);
    parseWarning("x.prototype.catch = function() {};", IRFactory.INVALID_ES3_PROP_NAME);
    parseWarning("x().catch();", IRFactory.INVALID_ES3_PROP_NAME);

    mode = LanguageMode.ECMASCRIPT5;

    parse("var x = {function: 1};");
    parse("x.function;");
    parse("var x = {get function(){} };");
    parse("var x = {set function(a){} };");
    parse("var x = {class: 1};");
    parse("x.class;");
    expectFeatures();
    parse("var x = {let: 1};");
    parse("x.let;");
    parse("var x = {yield: 1};");
    parse("x.yield;");
    expectFeatures(Feature.KEYWORDS_AS_PROPERTIES);
    parse("x.prototype.catch = function() {};");
    parse("x().catch();");

    mode = LanguageMode.ECMASCRIPT5;

    parse("var x = {function: 1};");
    parse("x.function;");
    parse("var x = {get function(){} };");
    parse("var x = {set function(a){} };");
    parse("var x = {class: 1};");
    parse("x.class;");
    expectFeatures();
    parse("var x = {let: 1};");
    parse("x.let;");
    parse("var x = {yield: 1};");
    parse("x.yield;");
    expectFeatures(Feature.KEYWORDS_AS_PROPERTIES);
    parse("x.prototype.catch = function() {};");
    parse("x().catch();");
  }

  @Test
  public void testKeywordsAsProperties2() {
    parse("var x = {get 'function'(){} };");
    parse("var x = {get 1(){} };");
    parse("var x = {set 'function'(a){} };");
    parse("var x = {set 1(a){} };");
  }

  @Test
  public void testKeywordsAsProperties3() {
    parse("var x = {get 'function'(){} };");
    parse("var x = {get 1(){} };");
    parse("var x = {set 'function'(a){} };");
    parse("var x = {set 1(a){} };");
  }

  @Test
  public void testKeywordsAsPropertiesInExterns1() {
    mode = LanguageMode.ECMASCRIPT3;
    strictMode = SLOPPY;

    parse("/** @fileoverview\n@externs\n*/\n var x = {function: 1};");
  }

  @Test
  public void testKeywordsAsPropertiesInExterns2() {
    mode = LanguageMode.ECMASCRIPT3;
    strictMode = SLOPPY;

    parse("/** @fileoverview\n@externs\n*/\n var x = {}; x.function + 1;");
  }

  @Test
  public void testUnicodeInIdentifiers() {
    parse("var à");
    parse("var cosθ");
    parse("if(true){foo=α}");
    parse("if(true){foo=Δ}else bar()");
  }

  @Test
  public void testUnicodeEscapeInIdentifiers() {
    parse("var \\u00fb");
    parse("var \\u00fbtest\\u00fb");
    parse("Js\\u00C7ompiler");
    parse("Js\\u0043ompiler");
    parse("if(true){foo=\\u03b5}");
    parse("if(true){foo=\\u03b5}else bar()");
  }

  @Test
  public void testUnicodePointEscapeInIdentifiers() {
    parse("var \\u{0043}");
    parse("var \\u{0043}test\\u{0043}");
    parse("var \\u0043test\\u{0043}");
    parse("var \\u{0043}test\\u0043");
    parse("Js\\u{0043}ompiler");
    parse("Js\\u{275}ompiler");
    parse("var \\u0043;{43}");
  }

  @Test
  public void testUnicodePointEscapeStringLiterals() {
    parse("var i = \'\\u0043ompiler\'");
    parse("var i = \'\\u{43}ompiler\'");
    parse("var i = \'\\u{1f42a}ompiler\'");
    parse("var i = \'\\u{2603}ompiler\'");
    parse("var i = \'\\u{1}ompiler\'");
  }

  @Test
  public void testUnicodePointEscapeTemplateLiterals() {
    parse("var i = `\\u0043ompiler`");
    parse("var i = `\\u{43}ompiler`");
    parse("var i = `\\u{1f42a}ompiler`");
    parse("var i = `\\u{2603}ompiler`");
    parse("var i = `\\u{1}ompiler`");
  }

  @Test
  public void testInvalidUnicodePointEscapeInIdentifiers() {
    parseError("var \\u{defg", "Invalid escape sequence");
    parseError("var \\u{03b5", "Invalid escape sequence");
    parseError("var \\u43{43}", "Invalid escape sequence");
    parseError("var \\u{defgRestOfIdentifier", "Invalid escape sequence");
    parseError("var \\u03b5}", "primary expression expected");
    parseError("var \\u{03b5}}}", "primary expression expected");
    parseError("var \\u{03b5}{}", SEMICOLON_EXPECTED);
    parseError("var \\u0043{43}", SEMICOLON_EXPECTED);
    parseError("var \\u{DEFG}", "Invalid escape sequence");
    parseError("Js\\u{}ompiler", "Invalid escape sequence");
    // Legal unicode but invalid in identifier
    parseError("Js\\u{99}ompiler", "Invalid escape sequence");
    parseError("Js\\u{10000}ompiler", "Invalid escape sequence");
  }

  @Test
  public void testInvalidUnicodePointEscapeStringLiterals() {
    parseError("var i = \'\\u{defg\'", "Hex digit expected");
    parseError("var i = \'\\u{defgRestOfIdentifier\'", "Hex digit expected");
    parseError("var i = \'\\u{DEFG}\'", "Hex digit expected");
    parseError("var i = \'Js\\u{}ompiler\'", "Empty unicode escape");
    parseError("var i = \'\\u{345", "Hex digit expected");
    parseError("var i = \'\\u{110000}\'", "Undefined Unicode code-point");
  }

  @Test
  public void testInvalidUnicodePointEscapeTemplateLiterals() {
    parseError("var i = `\\u{defg`", "Hex digit expected");
    parseError("var i = `\\u{defgRestOfIdentifier`", "Hex digit expected");
    parseError("var i = `\\u{DEFG}`", "Hex digit expected");
    parseError("var i = `Js\\u{}ompiler`", "Empty unicode escape");
    parseError("var i = `\\u{345`", "Hex digit expected");
    parseError("var i = `\\u{110000}`", "Undefined Unicode code-point");
  }

  @Test
  public void testEs2018LiftIllegalEscapeSequenceRestrictionOnTaggedTemplates() {
    // These should not generate errors, even though they contain illegal escape sequences.
    // https://github.com/tc39/proposal-template-literal-revision
    parse("latex`\\unicode`");
    parse("foo`\\xerxes`");
    parse("bar`\\u{h}ere`");
    parse("bar`\\u{43`");

    // tagged malformed template literal throws error
    parseError("foo`\\unicode", "Unterminated template literal");
    // normal template literals still throw error
    parseError("var bad = `\\unicode`;", "Hex digit expected");
  }

  @Test
  public void testInvalidEscape() {
    parseError("var \\x39abc", "Invalid escape sequence");
    parseError("var abc\\t", "Invalid escape sequence");
  }

  @Test
  public void testUnnecessaryEscape() {
    parseWarning("var str = '\\a'", "Unnecessary escape: '\\a' is equivalent to just 'a'");
    parse("var str = '\\b'");
    parseWarning("var str = '\\c'", "Unnecessary escape: '\\c' is equivalent to just 'c'");
    parseWarning("var str = '\\d'", "Unnecessary escape: '\\d' is equivalent to just 'd'");
    parseWarning("var str = '\\e'", "Unnecessary escape: '\\e' is equivalent to just 'e'");
    parse("var str = '\\f'");
    parse("var str = '\\/'");
    parse("var str = '\\0'");
    strictMode = SLOPPY;
    parseWarning("var str = '\\1'", "Unnecessary escape: '\\1' is equivalent to just '1'");
    parseWarning("var str = '\\2'", "Unnecessary escape: '\\2' is equivalent to just '2'");
    parseWarning("var str = '\\3'", "Unnecessary escape: '\\3' is equivalent to just '3'");
    parseWarning("var str = '\\4'", "Unnecessary escape: '\\4' is equivalent to just '4'");
    parseWarning("var str = '\\5'", "Unnecessary escape: '\\5' is equivalent to just '5'");
    parseWarning("var str = '\\6'", "Unnecessary escape: '\\6' is equivalent to just '6'");
    parseWarning("var str = '\\7'", "Unnecessary escape: '\\7' is equivalent to just '7'");
    parseWarning("var str = '\\8'", "Unnecessary escape: '\\8' is equivalent to just '8'");
    parseWarning("var str = '\\9'", "Unnecessary escape: '\\9' is equivalent to just '9'");
    parseWarning("var str = '\\%'", "Unnecessary escape: '\\%' is equivalent to just '%'");

    parseWarning("var str = '\\$'", "Unnecessary escape: '\\$' is equivalent to just '$'");
  }

  @Test
  public void testUnnecessaryEscapeUntaggedTemplateLiterals() {
    parseWarning("var str = `\\a`", "Unnecessary escape: '\\a' is equivalent to just 'a'");
    parse("var str = `\\b`");
    parseWarning("var str = `\\c`", "Unnecessary escape: '\\c' is equivalent to just 'c'");
    parseWarning("var str = `\\d`", "Unnecessary escape: '\\d' is equivalent to just 'd'");
    parseWarning("var str = `\\e`", "Unnecessary escape: '\\e' is equivalent to just 'e'");
    parse("var str = `\\f`");
    parseWarning("var str = `\\/`", "Unnecessary escape: '\\/' is equivalent to just '/'");
    parse("var str = `\\0`");
    parseWarning("var str = `\\%`", "Unnecessary escape: '\\%' is equivalent to just '%'");

    // single and double quotes have no meaning in a template lit
    parseWarning("var str = `\\\"`", "Unnecessary escape: '\\\"' is equivalent to just '\"'");
    parseWarning("var str = `\\'`", "Unnecessary escape: \"\\'\" is equivalent to just \"'\"");

    // $ needs to be escaped to distinguish it from use of ${}
    parse("var str = `\\$`");
    // ` needs to be escaped to avoid ending the template lit
    parse("var str = `\\``");
  }

  @Test
  public void testUnnecessaryEscapeTaggedTemplateLiterals() {
    expectFeatures(Feature.TEMPLATE_LITERALS);

    // Don't warn for unnecessary escapes in tagged template literals since they may access the
    // raw string value
    parse("var str = String.raw`\\a`");
    parse("var str = String.raw`\\b`");
    parse("var str = String.raw`\\c`");
    parse("var str = String.raw`\\d`");
    parse("var str = String.raw`\\e`");
    parse("var str = String.raw`\\f`");
    parse("var str = String.raw`\\/`");
    parse("var str = String.raw`\\0`");
    parse("var str = String.raw`\\8`");
    parse("var str = String.raw`\\9`");
    parse("var str = String.raw`\\%`");

    parse("var str = String.raw`\\$`");
    parse("var str = String.raw`\\``");
  }

  @Test
  public void testEOFInUnicodeEscape() {
    parseError("var \\u1", "Invalid escape sequence");
    parseError("var \\u12", "Invalid escape sequence");
    parseError("var \\u123", "Invalid escape sequence");
  }

  @Test
  public void testEndOfIdentifierInUnicodeEscape() {
    parseError("var \\u1 = 1;", "Invalid escape sequence");
    parseError("var \\u12 = 2;", "Invalid escape sequence");
    parseError("var \\u123 = 3;", "Invalid escape sequence");
  }

  @Test
  public void testInvalidUnicodeEscape() {
    parseError("var \\uDEFG", "Invalid escape sequence");
  }

  @Test
  public void testUnicodeEscapeInvalidIdentifierStart() {
    parseError("var \\u0037yler", "Character '7' (U+0037) is not a valid identifier start char");
    parseError("var \\u{37}yler", "Character '7' (U+0037) is not a valid identifier start char");
    parseError("var \\u0020space", "Invalid escape sequence");
  }

  @Test
  public void testUnicodeEscapeInvalidIdentifierChar() {
    parseError("var sp\\u0020ce", "Invalid escape sequence");
  }

  /**
   * It is illegal to use a keyword as an identifier, even if you use unicode escapes to obscure the
   * fact that you are trying do that.
   */
  @Test
  public void testKeywordAsIdentifier() {
    parseError("var while;", "'identifier' expected");
    parseError("var wh\\u0069le;", "'identifier' expected");
  }

  @Test
  public void testGetPropFunctionName() {
    parseError("function a.b() {}", "'(' expected");
    parseError("var x = function a.b() {}", "'(' expected");
  }

  @Test
  public void testIdeModePartialTree() {
    Node partialTree = parseError("function Foo() {} f.", "'identifier' expected");
    assertThat(partialTree).isNull();

    isIdeMode = true;
    partialTree = parseError("function Foo() {} f.", "'identifier' expected");
    assertThat(partialTree).isNotNull();
  }

  @Test
  public void testForEach() {
    parseError(
        """
        function f(stamp, status) {
          for each ( var curTiming in this.timeLog.timings ) {
            if ( curTiming.callId == stamp ) {
              curTiming.flag = status;
              break;
            }
          }
        };
        """,
        "'(' expected");
  }

  @Test
  public void testValidTypeAnnotation1() {
    parse("/** @type {string} */ var o = 'str';");
    parse("var /** @type {string} */ o = 'str', /** @type {number} */ p = 0;");
    parse("/** @type {function():string} */ function o() { return 'str'; }");
    parse("var o = {}; /** @type {string} */ o.prop = 'str';");
    parse("var o = {}; /** @type {string} */ o['prop'] = 'str';");
    parse("var o = { /** @type {string} */ prop : 'str' };");
    parse("var o = { /** @type {string} */ 'prop' : 'str' };");
    parse("var o = { /** @type {string} */ 1 : 'str' };");
  }

  @Test
  public void testValidTypeAnnotation2() {
    strictMode = SLOPPY;
    expectFeatures(Feature.GETTER);
    parse("var o = { /** @type {string} */ get prop() { return 'str' }};");
    expectFeatures(Feature.SETTER);
    parse("var o = { /** @type {string} */ set prop(s) {}};");
  }

  @Test
  public void testValidTypeAnnotation3() {
    // This one we don't currently support in the type checker but
    // we would like to.
    parse("try {} catch (/** @type {Error} */ e) {}");
  }

  @Test
  public void testValidTypeAnnotation4() {
    strictMode = SLOPPY;
    expectFeatures(Feature.MODULES);
    parse("/** @type {number} */ export var x = 3;");
  }

  @Test
  public void testTypeofJsdoc() {
    assertNodeEquality(parse("var b = 0;"), parse("var /** typeof a */ b = 0;"));

    assertNodeEquality(
        parse("var b = 0;"),
        parseWarning("var /** typeof {a} */ b = 0;", UNNECESSARY_BRACES_MESSAGE));

    assertNodeEquality(
        parse("var b = 0;"),
        parseWarning("var /** typeof <a> */ b = 0;", NAME_NOT_RECOGNIZED_MESSAGE));
  }

  @Test
  public void testParsingAssociativity() {
    assertNodeEquality(parse("x * y * z"), parse("(x * y) * z"));
    assertNodeEquality(parse("x + y + z"), parse("(x + y) + z"));
    assertNodeEquality(parse("x | y | z"), parse("(x | y) | z"));
    assertNodeEquality(parse("x & y & z"), parse("(x & y) & z"));
    assertNodeEquality(parse("x ^ y ^ z"), parse("(x ^ y) ^ z"));
    assertNodeEquality(parse("x || y || z"), parse("(x || y) || z"));
    assertNodeEquality(parse("x && y && z"), parse("(x && y) && z"));
  }

  @Test
  public void testIssue1116() {
    parse("/**/");
  }

  @Test
  public void testUnterminatedStringLiteral() {
    parseError("var unterm = 'forgot closing quote", "Unterminated string literal");

    parseError(
        """
        var unterm = 'forgot closing quote
        alert(unterm);
        """,
        "Unterminated string literal");

    // test combo of a string continuation + useless escape warning + unterminated literal error
    // create a TestErrorReporter so that we can expect both a warning and an error
    String js = "var unterm = ' \\\n \\a \n";

    TestErrorReporter testErrorReporter =
        new TestErrorReporter()
            .expectAllWarnings("Unnecessary escape: '\\a' is equivalent to just 'a'")
            .expectAllErrors("Unterminated string literal");
    StaticSourceFile file = new SimpleSourceFile("input", SourceKind.STRONG);
    ParserRunner.parse(file, js, createConfig(), testErrorReporter);

    // verify we reported both the warning and error
    testErrorReporter.verifyHasEncounteredAllWarningsAndErrors();
  }

  /**
   * @bug 14231379
   */
  @Test
  public void testUnterminatedRegExp() {
    parseError("var unterm = /forgot trailing slash", "Expected '/' in regular expression literal");

    parseError(
        """
        var unterm = /forgot trailing slash
        alert(unterm);
        """,
        "Expected '/' in regular expression literal");
  }

  @Test
  public void testRegExp() {
    assertNodeEquality(parse("/a/"), script(expr(regex("a"))));
    assertNodeEquality(parse("/\\\\/"), script(expr(regex("\\\\"))));
    assertNodeEquality(parse("/\\s/"), script(expr(regex("\\s"))));
    assertNodeEquality(parse("/\\u000A/"), script(expr(regex("\\u000A"))));
    assertNodeEquality(parse("/[\\]]/"), script(expr(regex("[\\]]"))));
  }

  @Test
  public void testRegExpError() {
    parseError("/a\\/", "Expected '/' in regular expression literal");
    parseError("/\\ca\\/", "Expected '/' in regular expression literal");
    parseError("/\b.\\/", "Expected '/' in regular expression literal");
  }

  @Test
  public void testRegExpUnicode() {
    assertNodeEquality(parse("/\\u10fA/"), script(expr(regex("\\u10fA"))));
    assertNodeEquality(parse("/\\u{10fA}/u"), script(expr(regex("\\u{10fA}", "u"))));
    assertNodeEquality(parse("/\\u{1fA}/u"), script(expr(regex("\\u{1fA}", "u"))));
    assertNodeEquality(parse("/\\u{10FFFF}/u"), script(expr(regex("\\u{10FFFF}", "u"))));
  }

  @Test
  public void testRegExpFlags() {
    // Various valid combinations.
    parse("/a/");
    parse("/a/i");
    parse("/a/g");
    parse("/a/m");
    parse("/a/ig");
    parse("/a/gm");
    parse("/a/mgi");

    // Invalid combinations
    parseError("/a/a", "Invalid RegExp flag 'a'");
    parseError("/a/b", "Invalid RegExp flag 'b'");
    parseError(
        "/a/abc", "Invalid RegExp flag 'a'", "Invalid RegExp flag 'b'", "Invalid RegExp flag 'c'");
  }

  /** New RegExp flags added in ES6. */
  @Test
  public void testES6RegExpFlags() {
    expectFeatures(Feature.REGEXP_FLAG_Y);
    strictMode = SLOPPY;
    parse("/a/y");
    expectFeatures(Feature.REGEXP_FLAG_U);
    parse("/a/u");

    mode = LanguageMode.ECMASCRIPT5;
    expectFeatures(Feature.REGEXP_FLAG_Y);
    parseWarning("/a/y", requiresLanguageModeMessage(Feature.REGEXP_FLAG_Y));
    expectFeatures(Feature.REGEXP_FLAG_U);
    parseWarning("/a/u", requiresLanguageModeMessage(Feature.REGEXP_FLAG_U));
    parseWarning(
        "/a/yu",
        requiresLanguageModeMessage(Feature.REGEXP_FLAG_Y),
        requiresLanguageModeMessage(Feature.REGEXP_FLAG_U));
  }

  /** New RegExp flag 's' added in ES2018. */
  @Test
  public void testES2018RegExpFlagS() {
    expectFeatures(Feature.REGEXP_FLAG_S);
    parse("/a/s");

    mode = LanguageMode.ECMASCRIPT_2015;
    expectFeatures(Feature.REGEXP_FLAG_S);
    parseWarning("/a/s", requiresLanguageModeMessage(Feature.REGEXP_FLAG_S));
    parseWarning(
        "/a/us", // 'u' added in es6
        requiresLanguageModeMessage(Feature.REGEXP_FLAG_S));

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning(
        "/a/us", // 'u' added in es6
        requiresLanguageModeMessage(Feature.REGEXP_FLAG_U),
        requiresLanguageModeMessage(Feature.REGEXP_FLAG_S));
  }

  /** New RegExp flag 'd' added in ES2022. */
  @Test
  public void testES2022RegExpFlagD() {
    expectFeatures(Feature.REGEXP_FLAG_D);
    parse("/a/d");

    mode = LanguageMode.ECMASCRIPT_2015;
    expectFeatures(Feature.REGEXP_FLAG_D);
    parseWarning("/a/d", requiresLanguageModeMessage(Feature.REGEXP_FLAG_D));
    parseWarning(
        "/a/ud", // 'u' added in es6
        requiresLanguageModeMessage(Feature.REGEXP_FLAG_D));

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning(
        "/a/ud", // 'u' added in es6
        requiresLanguageModeMessage(Feature.REGEXP_FLAG_U),
        requiresLanguageModeMessage(Feature.REGEXP_FLAG_D));
  }

  @Test
  public void testDefaultParameters() {
    strictMode = SLOPPY;
    expectFeatures(Feature.DEFAULT_PARAMETERS);
    parse("function f(a, b=0) {}");
    parse("function f(a, b=0, c) {}");

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning("function f(a, b=0) {}", requiresLanguageModeMessage(Feature.DEFAULT_PARAMETERS));
  }

  @Test
  public void testDefaultParameterInlineJSDoc() {
    expectFeatures(Feature.DEFAULT_PARAMETERS);
    Node functionNode = parse("function f(/** number */ a = 0) {}").getFirstChild();
    Node parameterList = functionNode.getSecondChild();
    Node defaultValue = parameterList.getFirstChild();
    assertNode(defaultValue).hasType(Token.DEFAULT_VALUE);

    Node aName = defaultValue.getFirstChild();
    assertNode(aName).hasType(Token.NAME);
    assertNodeHasJSDocInfoWithJSType(aName, NUMBER_TYPE);
  }

  @Test
  public void testDefaultParameterInlineNonJSDocComment() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    expectFeatures(Feature.DEFAULT_PARAMETERS);
    Node functionNode = parse("function f(/* number */ a = 0) {}").getFirstChild();
    Node parameterList = functionNode.getSecondChild();
    Node defaultValue = parameterList.getFirstChild();
    assertNode(defaultValue).hasType(Token.DEFAULT_VALUE);

    Node aName = defaultValue.getFirstChild();
    assertNode(aName).hasType(Token.NAME);
    assertThat(aName.getNonJSDocCommentString()).contains("/* number */");
  }

  @Test
  public void testRestParameters() {
    strictMode = SLOPPY;

    parseError("(...xs, x) => xs", "')' expected");
    parseError(
        "function f(...a[0]) {}", "Only an identifier or destructuring pattern is allowed here.");
    parseError("function f(...y, z) {}", "A rest parameter must be last in a parameter list.");

    expectFeatures(Feature.REST_PARAMETERS);
    parse("function f(...b) {}");
    parse("(...xs) => xs");
    parse("(x, ...xs) => xs");
    parse("(x, y, ...xs) => xs");
  }

  @Test
  public void testTrailingCommaAfterRestParameters() {
    strictMode = SLOPPY;

    parseError("function f(...a,) {}", "A trailing comma must not follow a rest parameter.");
    // http://b/184088793, trailing commas should be allowed in arrow function then
    // the proper error is reported.
    parseError("((...xs,) => xs)", "')' expected");
  }

  @Test
  public void testDestructuredRestParameters() {
    strictMode = SLOPPY;
    parseError(
        "function f(...[a[0]]) {}", "Only an identifier or destructuring pattern is allowed here.");

    expectFeatures(Feature.REST_PARAMETERS, Feature.ARRAY_DESTRUCTURING);
    parse("(...[x]) => xs");
    parse("(...[x, y]) => xs");
    parse("(a, b, c, ...[x, y, z]) => x");
  }

  @Test
  public void testRestParameters_ES5() {
    mode = LanguageMode.ECMASCRIPT5;
    strictMode = SLOPPY;
    expectFeatures(Feature.REST_PARAMETERS);
    parseWarning("function f(...b) {}", requiresLanguageModeMessage(Feature.REST_PARAMETERS));
  }

  @Test
  public void testExpressionsThatLookLikeParameters1() {
    strictMode = SLOPPY;
    parseError("();", "invalid parenthesized expression");
    parseError("(...xs);", "invalid parenthesized expression");
    parseError("(x, ...xs);", "A rest parameter must be in a parameter list.");
    parseError("(a, b, c, ...xs);", "A rest parameter must be in a parameter list.");
  }

  @Test
  public void testExpressionsThatLookLikeParameters2() {
    strictMode = SLOPPY;
    parseError("!()", "invalid parenthesized expression");
    parseError("().method", "invalid parenthesized expression");
    parseError("() || a", "invalid parenthesized expression");
    parseError("() && a", "invalid parenthesized expression");
    parseError("x = ()", "invalid parenthesized expression");
  }

  @Test
  public void testExpressionsThatLookLikeParameters3() {
    strictMode = SLOPPY;
    parseError("!(...x)", "invalid parenthesized expression");
    parseError("(...x).method", "invalid parenthesized expression");
    parseError("(...x) || a", "invalid parenthesized expression");
    parseError("(...x) && a", "invalid parenthesized expression");
    parseError("x = (...x)", "invalid parenthesized expression");
  }

  @Test
  public void testDefaultParametersWithRestParameters() {
    strictMode = SLOPPY;
    parseError("function f(a=1, ...b=3) {}", "A default value cannot be specified after '...'");

    expectFeatures(Feature.DEFAULT_PARAMETERS, Feature.REST_PARAMETERS);
    parse("function f(a=0, ...b) {}");
    parse("function f(a, b=0, ...c) {}");
    parse("function f(a, b=0, c=1, ...d) {}");
  }

  @Test
  public void testClass1() {
    expectFeatures(Feature.CLASSES);
    strictMode = SLOPPY;
    parse("class C {}");

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning("class C {}", requiresLanguageModeMessage(Feature.CLASSES));

    mode = LanguageMode.ECMASCRIPT3;
    parseWarning("class C {}", requiresLanguageModeMessage(Feature.CLASSES));
  }

  @Test
  public void testClass2() {
    expectFeatures(Feature.CLASSES);
    strictMode = SLOPPY;
    parse("class C {}");

    parse(
        """
        class C {
          member() {}
          get prop() {}
          set prop(a) {}
        }
        """);

    parse(
        """
        class C {
          static member() {}
          static get prop() {}
          static set prop(a) {}
        }
        """);
  }

  @Test
  public void testClass3() {
    expectFeatures(Feature.CLASSES);
    strictMode = SLOPPY;
    parse(
        """
        class C {
          member() {};
          get prop() {};
          set prop(a) {};
        }
        """);

    parse(
        """
        class C {
          static member() {};
          static get prop() {};
          static set prop(a) {};
        }
        """);
  }

  @Test
  public void testClassKeywordsAsMethodNames() {
    expectFeatures(Feature.CLASSES, Feature.KEYWORDS_AS_PROPERTIES);
    strictMode = SLOPPY;
    parse(
        """
        class KeywordMethods {
          continue() {}
          throw() {}
          else() {}
        }
        """);
  }

  @Test
  public void testClassReservedWordsAsMethodNames() {
    expectFeatures(Feature.CLASSES, Feature.KEYWORDS_AS_PROPERTIES);
    strictMode = SLOPPY;
    parse(
        """
        class C {
          import() {};
          get break() {};
          set break(a) {};
        }
        """);

    parse(
        """
        class C {
          static import() {};
          static get break() {};
          static set break(a) {};
        }
        """);
  }

  @Test
  public void testClass_semicolonsInBodyAreIgnored() {
    Node tree =
        parse(
            """
            class C {
              foo() {};;;;;;
            }
            """);

    Node members = tree.getFirstChild().getChildAtIndex(2);
    assertThat(members.isClassMembers()).isTrue();
    assertThat(members.getChildCount()).isEqualTo(1);
  }

  @Test
  public void testClass_constructorMember_legalModifiers() {
    final String errorMsg = "Class constructor may not be getter, setter, async, or generator.";

    // The expected default case.
    parse("class A { constructor() { } }");

    // Modifiers are legal on a static "constructor" member.
    parse("class A { static constructor() { } }");
    parse("class A { static get constructor() { } }");
    parse("class A { static set constructor(x) { } }");
    parse("class A { static async constructor() { } }");
    parse("class A { static *constructor() { } }");

    // Modifiers are illegal on an instance "constructor" member.
    parseError("class A { get constructor() { } }", errorMsg);
    parseError("class A { set constructor(x) { } }", errorMsg);
    parseError("class A { async constructor() { } }", errorMsg);
    parseError("class A { *constructor() { } }", errorMsg);

    // Modifiers are also illegal on a constructor declared using a string literal.
    // TODO(b/123769080): These should be parse errors, but this case can't be detected currently.
    parse("class A { 'constructor'() { } }");
    parse("class A { get 'constructor'() { } }");
    parse("class A { set 'constructor'(x) { } }");
    parse("class A { async 'constructor'() { } }");
    parse("class A { *'constructor'() { } }");

    // Modifiers are legal on computed properties that happen to be named "constructor".
    parse("class A { ['constructor']() { } }");
    parse("class A { get ['constructor']() { } }");
    parse("class A { set ['constructor'](x) { } }");
    parse("class A { async ['constructor']() { } }");
    parse("class A { *['constructor']() { } }");
  }

  @Test
  public void testClass_constructorMember_atMostOne() {
    final String errorMsg = "Class may have only one constructor.";

    // The expected default cases.
    parse("class A { }");
    parse("class A { constructor() { } }");

    // Not more than one.
    parseError("class A { constructor() { } constructor() { } }", errorMsg);
    parseError(
        "class A { constructor() { } constructor() { } constructor() { } }", errorMsg, errorMsg);
    // TODO(b/123769080): These should be parse errors, but this case can't be detected currently.
    parse("class A { constructor() { } 'constructor'() { } }");

    // Computed properties can't be the class constructor.
    parse("class A { constructor() { } ['constructor']() { } }");

    // Statics can't be the class constructor.
    parse("class A { constructor() { } static constructor() { } }");
  }

  @Test
  public void testSingleFieldNoInitializerNoSemicolon() {
    Node n = parse("class C{ field }").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).hasType(Token.MEMBER_FIELD_DEF);
    assertNode(n.getFirstChild()).hasStringThat().isEqualTo("field");
  }

  @Test
  public void testSingleFieldWithInitializerNoSemicolon() {
    // check for automatic semicolon insertion
    Node n = parse("class C{field = 2}").getFirstChild().getLastChild().getFirstChild();
    assertNode(n).hasType(Token.MEMBER_FIELD_DEF);
    assertNode(n.getFirstChild()).isEqualTo(IR.number(2.0));
  }

  @Test
  public void testSingleFieldNoInitializerWithSemicolon() {
    Node n = parse("class C{ field; }").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).hasStringThat().isEqualTo("field");
  }

  @Test
  public void testSingleFieldWithInitializerWithSemicolon() {
    Node n = parse("class C{field = dog;}").getFirstChild().getLastChild().getFirstChild();
    assertNode(n).hasType(Token.MEMBER_FIELD_DEF);
    assertNode(n.getFirstChild()).isEqualTo(IR.name("dog"));
  }

  @Test
  public void testSingleComputedFieldNoInitializerNoSemicolon() {
    // check for automatic semicolon insertion
    Node n = parse("class C{['field']}").getFirstChild().getLastChild().getFirstChild();
    assertNode(n).hasType(Token.COMPUTED_FIELD_DEF);
    assertNode(n.getFirstChild()).hasStringThat().isEqualTo("field");
  }

  @Test
  public void testSingleComputedFieldWithInitializerNoSemicolon() {
    // check for automatic semicolon insertion
    Node n = parse("class C{['field'] = 2}").getFirstChild().getLastChild().getFirstChild();
    assertNode(n).hasType(Token.COMPUTED_FIELD_DEF);
    assertNode(n.getFirstChild()).hasStringThat().isEqualTo("field");
    assertNode(n.getLastChild()).isEqualTo(IR.number(2.0));
  }

  @Test
  public void testSingleComputedFieldNoInitializerWithSemicolon() {
    Node n = parse("class C{['field'];}").getFirstChild().getLastChild().getFirstChild();
    assertNode(n).hasType(Token.COMPUTED_FIELD_DEF);
    assertNode(n.getFirstChild()).hasStringThat().isEqualTo("field");
  }

  @Test
  public void testSingleComputedFieldWithInitializerWithSemicolon() {
    Node n = parse("class C{['field'] = dog;}").getFirstChild().getLastChild().getFirstChild();
    assertNode(n).hasType(Token.COMPUTED_FIELD_DEF);
    assertNode(n.getFirstChild()).hasStringThat().isEqualTo("field");
    assertNode(n.getLastChild()).isEqualTo(IR.name("dog"));
  }

  @Test
  public void testMultipleFieldsNoInitializerNoLineBreakNoSemicolon() {
    parseError("class C{ a b c }", "Semi-colon expected");
  }

  @Test
  public void testMultipleFieldsNoInitializerWithLineBreakNoSemicolon() {
    Node n = parse("class C{ a \n b \n c }").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).hasType(Token.MEMBER_FIELD_DEF);
    assertNode(n.getFirstChild()).hasStringThat().isEqualTo("a");
    assertNode(n.getSecondChild()).hasType(Token.MEMBER_FIELD_DEF);
    assertNode(n.getSecondChild()).hasStringThat().isEqualTo("b");
    assertNode(n.getLastChild()).hasType(Token.MEMBER_FIELD_DEF);
    assertNode(n.getLastChild()).hasStringThat().isEqualTo("c");
  }

  @Test
  public void testMultipleFieldsNoLineBreakNoSemicolon() {
    parseError("class C{ a = 2 b = 4 c = 5}", "Semi-colon expected");
  }

  @Test
  public void testMultipleFieldsWithLineBreakNoSemicolon() {
    // check for automatic semicolon insertion
    Node n = parse("class C{ a = 2 \n b = 4 \n c = 5}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).hasType(Token.MEMBER_FIELD_DEF);
    assertNode(n.getFirstChild()).hasStringThat().isEqualTo("a");
    assertNode(n.getFirstFirstChild()).isEqualTo(IR.number(2.0));
    assertNode(n.getSecondChild()).hasType(Token.MEMBER_FIELD_DEF);
    assertNode(n.getSecondChild()).hasStringThat().isEqualTo("b");
    assertNode(n.getSecondChild().getFirstChild()).isEqualTo(IR.number(4.0));
    assertNode(n.getLastChild()).hasType(Token.MEMBER_FIELD_DEF);
    assertNode(n.getLastChild()).hasStringThat().isEqualTo("c");
    assertNode(n.getLastChild().getFirstChild()).isEqualTo(IR.number(5.0));
  }

  @Test
  public void testMultipleFieldsNoLineBreakWithSemicolon() {
    Node n = parse("class C{field = 2; hi = 3;}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).hasType(Token.MEMBER_FIELD_DEF);
    assertNode(n.getFirstChild()).hasStringThat().isEqualTo("field");
    assertNode(n.getFirstFirstChild()).isEqualTo(IR.number(2.0));
    assertNode(n.getSecondChild()).hasStringThat().isEqualTo("hi");
    assertNode(n.getSecondChild().getFirstChild()).isEqualTo(IR.number(3.0));
  }

  @Test
  public void testMultipleFieldsWithLineBreakWithSemicolon() {
    Node n = parse("class C{field = 2; \n hi = 3;}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).hasType(Token.MEMBER_FIELD_DEF);
    assertNode(n.getFirstChild()).hasStringThat().isEqualTo("field");
    assertNode(n.getFirstFirstChild()).isEqualTo(IR.number(2.0));
    assertNode(n.getSecondChild()).hasStringThat().isEqualTo("hi");
    assertNode(n.getSecondChild().getFirstChild()).isEqualTo(IR.number(3.0));
  }

  @Test
  public void testMultipleComputedFieldsNoLineBreakNoSemicolon() {
    // tree is parsed correctly even though it looks weird
    Node n = parse("class C{ ['a'] = 2 ['b'] = 4 ['c'] = 5}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).hasType(Token.COMPUTED_FIELD_DEF);
  }

  @Test
  public void testMultipleComputedFieldsWithLineBreakNoSemicolon() {
    // tree is parsed correctly even though it looks weird
    Node n = parse("class C{ ['a'] = 2 \n ['b'] = 4 \n ['c'] = 5}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).hasType(Token.COMPUTED_FIELD_DEF);
  }

  @Test
  public void testMultipleComputedFieldsNoLineBreakWithSemicolon() {
    Node n = parse("class C{['field'] = 2; ['hi'] = 3;}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).hasType(Token.COMPUTED_FIELD_DEF);
    assertNode(n.getFirstFirstChild()).hasStringThat().isEqualTo("field");
    assertNode(n.getFirstChild().getLastChild()).isEqualTo(IR.number(2.0));
    assertNode(n.getSecondChild()).hasType(Token.COMPUTED_FIELD_DEF);
    assertNode(n.getSecondChild().getFirstChild()).hasStringThat().isEqualTo("hi");
    assertNode(n.getSecondChild().getLastChild()).isEqualTo(IR.number(3.0));
  }

  @Test
  public void testMultipleComputedFieldsWithLineBreaksWithSemicolons() {
    Node n = parse("class C{['field'] = 2; \n ['hi'] = 3;}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).hasType(Token.COMPUTED_FIELD_DEF);
    assertNode(n.getFirstFirstChild()).hasStringThat().isEqualTo("field");
    assertNode(n.getFirstChild().getLastChild()).isEqualTo(IR.number(2.0));
    assertNode(n.getSecondChild()).hasType(Token.COMPUTED_FIELD_DEF);
    assertNode(n.getSecondChild().getFirstChild()).hasStringThat().isEqualTo("hi");
    assertNode(n.getSecondChild().getLastChild()).isEqualTo(IR.number(3.0));
  }

  @Test
  public void testMultipleMixedFieldsNoLineBreakWithSemicolon() {
    Node n = parse("class C{  b = 4; ['a'] = 2 }").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).hasType(Token.MEMBER_FIELD_DEF);
    assertNode(n.getSecondChild()).hasType(Token.COMPUTED_FIELD_DEF);
  }

  @Test
  public void testMultipleMixedFieldsNoLineBreakNoSemicolon() {
    parseError("class C{ ['a'] = 2 b = 4}", "Semi-colon expected");

    // tree is parsed correctly even though it looks weird
    Node n = parse("class C{b = 4 ['a'] = 2 }").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).hasType(Token.MEMBER_FIELD_DEF);
  }

  @Test
  public void testMultipleMixedFieldsWithLineBreakNoSemicolon() {
    // tree is parsed correctly even though it looks weird
    Node n = parse("class C{b = 4 \n ['a'] = 2 }").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).hasType(Token.MEMBER_FIELD_DEF);
  }

  @Test
  public void testComputedFieldStringLit() {
    Node n = parse("class C {'x' = 2;}").getFirstChild().getLastChild().getFirstChild();
    assertNode(n).hasType(Token.COMPUTED_FIELD_DEF);
    assertNode(n.getFirstChild()).hasType(Token.STRINGLIT);
    assertNode(n.getFirstChild()).hasStringThat().isEqualTo("x");
    assertNode(n.getLastChild()).hasType(Token.NUMBER);
    assertNode(n.getLastChild()).isEqualTo(IR.number(2.0));
  }

  @Test
  public void testComputedFieldName() {
    Node n = parse("class C {[a] = 2;}").getFirstChild().getLastChild().getFirstChild();
    assertNode(n).hasType(Token.COMPUTED_FIELD_DEF);
    assertNode(n.getFirstChild()).hasType(Token.NAME);
    assertNode(n.getFirstChild()).hasStringThat().isEqualTo("a");
    assertNode(n.getLastChild()).hasType(Token.NUMBER);
    assertNode(n.getLastChild()).isEqualTo(IR.number(2.0));
  }

  @Test
  public void testComputedFieldNumber() {
    Node n = parse("class C{1 = 2;}").getFirstChild().getLastChild().getFirstChild();
    assertNode(n).hasType(Token.COMPUTED_FIELD_DEF);
    assertNode(n.getFirstChild()).hasType(Token.NUMBER);
    assertNode(n.getFirstChild()).isEqualTo(IR.number(1.0));
    assertNode(n.getLastChild()).hasType(Token.NUMBER);
    assertNode(n.getLastChild()).isEqualTo(IR.number(2.0));
  }

  @Test
  public void testStaticClassFields() {
    Node n = parse("class C{static field = 2;}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).isStatic();

    n = parse("class C{static field = 2; hi = 3;}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).isStatic();

    n = parse("class C{static field = 2; static hi = 3;}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).isStatic();
    assertNode(n.getSecondChild()).isStatic();
  }

  @Test
  public void testStaticClassComputedFields() {
    Node n = parse("class C{static ['field'] = 2;}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).isStatic();

    n = parse("class C{static ['field'] = 2; ['hi'] = 3;}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).isStatic();

    n = parse("class C{static ['field'] = 2; static ['hi'] = 3;}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).isStatic();
    assertNode(n.getSecondChild()).isStatic();

    n = parse("class C{static 'hi' = 2;}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).isStatic();

    n = parse("class C{static 1 = 2;}").getFirstChild().getLastChild();
    assertNode(n.getFirstChild()).isStatic();
  }

  @Test
  public void testClassField_es2020() {
    mode = LanguageMode.ECMASCRIPT_2020;
    expectFeatures(Feature.PUBLIC_CLASS_FIELDS);
    parseWarning("class C{field = 2;}", requiresLanguageModeMessage(Feature.PUBLIC_CLASS_FIELDS));
  }

  @Test
  public void testClassComputedField_es2020() {
    mode = LanguageMode.ECMASCRIPT_2020;
    expectFeatures(Feature.PUBLIC_CLASS_FIELDS);
    parseWarning("class C{['a'] = 2;}", requiresLanguageModeMessage(Feature.PUBLIC_CLASS_FIELDS));
  }

  @Test
  public void testClassComputedField_es2020_noWarningOrRecordingOfFeaturesInClosureUnawareCode() {
    mode = LanguageMode.ECMASCRIPT_2020;
    expectFeatures(Feature.PUBLIC_CLASS_FIELDS);
    parseWarning(
        "/** @closureUnaware */ (function() { class C{ a = 2;} }).call(globalThis);",
        // This warning is expected - it is used to halt compilation during parsing when
        // closure-unaware code is present when it should not be.
        "@closureUnaware annotation is not allowed in this compilation");

    // Note that the PUBLIC_CLASS_FIELDS feature is not recorded here. This is OK as we currently do
    // not support transpiling closure-unaware code.
    expectFeatures(Feature.CLASSES);
  }

  @Test
  public void testClassComputedField_es2020_warnsForCodeOutsideClosureUnawareRange() {
    mode = LanguageMode.ECMASCRIPT_2020;
    expectFeatures(Feature.PUBLIC_CLASS_FIELDS);
    parseWarning(
        """
        /** @closureUnaware */ (
          function() { class C{ a = 2;} }).call(globalThis); class C{ a = 2;
        }
        """,
        // This warning is expected - it is used to halt compilation during parsing when
        // closure-unaware code is present when it should not be.
        "@closureUnaware annotation is not allowed in this compilation",
        "This language feature is only supported for ES_NEXT mode or better: Public class"
            + " fields");

    expectFeatures(Feature.CLASSES, Feature.PUBLIC_CLASS_FIELDS);
  }

  @Test
  public void testPrivateProperty_unstable() {
    mode = LanguageMode.UNSTABLE;
    expectFeatures(Feature.PRIVATE_CLASS_PROPERTIES);
    parseWarning(
        "class C { #f = 2; }", requiresLanguageModeMessage(Feature.PRIVATE_CLASS_PROPERTIES));
  }

  @Test
  public void testPrivateProperty_singleClassMember() {
    expectFeatures(Feature.PRIVATE_CLASS_PROPERTIES);

    parse("class C { #f; }");
    parse("class C { #m() {} }");
    parse("class C { *#g() {} }");
    parse("class C { get #g() {} }");
    parse("class C { set #s(x) {} }");
    parse("class C { get #p() {} set #p(x) {} }");
    parse("class C { async #a() {} }");
    parse("class C { async *#ag() {} }");

    parse("class C { static #sf; }");
    parse("class C { static #sm() {} }");
    parse("class C { static *#sg() {} }");
    parse("class C { static get #sg() {} }");
    parse("class C { static set #ss(x) {} }");
    parse("class C { static get #sp() {} static set #sp(x) {} }");
    parse("class C { static async #sa() {} }");
    parse("class C { static async *#sag() {} }");
  }

  @Test
  public void testPrivateProperty_definition_linenocharno() {
    Node n =
        parse(
                """
                class C {
                  #pf = 1;
                  #pm() {}
                }
                """)
            .getFirstChild();

    Node members = NodeUtil.getClassMembers(n);

    Node privateField = members.getFirstChild();
    assertThat(privateField.getLineno()).isEqualTo(2);
    assertThat(privateField.getCharno()).isEqualTo(2);
    assertThat(privateField.getLength()).isEqualTo(8); // Includes the assignment

    Node privateMethod = members.getLastChild();
    assertThat(privateMethod.getLineno()).isEqualTo(3);
    assertThat(privateMethod.getCharno()).isEqualTo(2);
    assertThat(privateMethod.getLength()).isEqualTo(3); // Just the method name.
  }

  @Test
  public void testPrivateProperty_multipleClassMembers() {
    parse("class C { #f; #g; }");
    parse("class C { #m() {} #n() {} }");
    parse("class C { get #g() {} get #h() {} }");
    parse("class C { set #s(x) {} set #t(x) {} }");
    parse("class C { get #s() {} set #s(x) {} get #t() {} set #t(x) {} }");

    parse("class C { static #sf; static #sg; }");
    parse("class C { static #sm() {} static #sn() {} }");
    parse("class C { static get #sg() {} static get #sh() {} }");
    parse("class C { static set #ss(x) {} static set #st(x) {} }");
    parse(
        """
        class C { static get #ss() {} static set #ss(x) {}
        static get #st() {} static set #st(x) {} }
        """);

    parse("class C { #a; #b; c() {} d() {} get #e() {} set #e(x) {} set #f(x) {} }");
    parse("class C { static #a; #b; static c() {} d() {} static get #e() {} set #f(x) {} }");
    parse(
        """
        class C { static #a; static #b; static c() {} static d() {}
        static get #e() {} static set #f(x) {} }
        """);
  }

  @Test
  public void testPrivateProperty_invalid_identifierAlreadyDeclared() {
    String expectedError = "Identifier '#p' has already been declared";

    parseError("class C { #p; #p; }", expectedError);
    parseError("class C { #p() {} #p() {} }", expectedError);
    parseError("class C { get #p() {} get #p() {} }", expectedError);
    parseError("class C { set #p(x) {} set #p(x) {} }", expectedError);

    parseError("class C { static #p; static #p; }", expectedError);
    parseError("class C { static #p() {} static #p() {} }", expectedError);
    parseError("class C { static get #p() {} static get #p() {} }", expectedError);
    parseError("class C { static set #p(x) {} static set #p(x) {} }", expectedError);

    parseError("class C { #p; #p() {} }", expectedError);
    parseError("class C { #p; get #p() {} }", expectedError);
    parseError("class C { #p; set #p(x) {} }", expectedError);

    parseError("class C { #p() {} #p; }", expectedError);
    parseError("class C { #p() {} get #p() {} }", expectedError);
    parseError("class C { #p() {} set #p(x) {} }", expectedError);

    parseError("class C { get #p() {} #p; }", expectedError);
    parseError("class C { get #p() {} #p() {} }", expectedError);
    parse(/**/ "class C { get #p() {} set #p(x) {} }"); // OK

    parseError("class C { set #p(x) {} #p; }", expectedError);
    parseError("class C { set #p(x) {} #p() {} }", expectedError);
    parse(/**/ "class C { set #p(x) {} get #p() {} }"); // OK

    parseError("class C { #p; static #p; }", expectedError); // Cross-static duplicate field
  }

  @Test
  public void testPrivateProperty_invalid_identifierAlreadyDeclared_crossStaticGetterSetter() {
    String expectedError = "Identifier '#p' has already been declared";

    // Repeating in all orders as the check is unique for each to a degree.
    parseError("class C { get #p() {} static set #p(x) {} }", expectedError);
    parseError("class C { set #p(x) {} static get #p() {} }", expectedError);
    parseError("class C { static get #p() {} set #p(x) {} }", expectedError);
    parseError("class C { static set #p(x) {} get #p() {} }", expectedError);
  }

  @Test
  public void testPrivateProperty_classMembersReferencingOtherPrivateField() {
    expectFeatures(Feature.PRIVATE_CLASS_PROPERTIES);

    parse("class C { #f = 1; f = this.#f; }");

    parse("class C { static #sf = 1; static sf = this.#sf; }");
    parse("class C { static #sf = 1; static sf = C.#sf; }");
    parse("class C { static #sf; static { this.#sf = 1; } }");
    parse("class C { static #sf; static { C.#sf = 1; } }");
  }

  @Test
  public void testPrivateProperty_reference_linenocharno() {
    Node n =
        parse(
                """
                class C {
                  #pf1 = 1;
                  #pf2 = this.#pf1;
                }
                """)
            .getFirstChild();

    Node members = NodeUtil.getClassMembers(n);

    Node field2 = members.getLastChild();
    assertNode(field2).hasType(Token.MEMBER_FIELD_DEF);

    Node field2GetProp = field2.getFirstChild();
    assertNode(field2GetProp).hasType(Token.GETPROP);
    assertNode(field2GetProp).hasStringThat().isEqualTo("#pf1");
    assertThat(field2GetProp.getLineno()).isEqualTo(3);
    assertThat(field2GetProp.getCharno()).isEqualTo(14);
    assertThat(field2GetProp.getLength()).isEqualTo(4);
  }

  @Test
  public void testPrivateProperty_classMethodsReferencingOtherPrivateProp() {
    expectFeatures(Feature.PRIVATE_CLASS_PROPERTIES);

    parse("class C { #f = 1; method() { this.#f = 2; } }");
    parse("class C { #f = 1; #g = 2; method() { this.#f = this.#g; } }");
    parse("class C { #f = 1; method() { const t = this; t.#f = 2; } }");
    parse("class C { #f = 1; method() { const f = () => { this.#f = 2; }; } }");
    parse("class C { #f = 1; method() { const x = [this.#f]; } }");
    parse("class C { #f = 1; method() { const x = {y: this.#f}; } }");
    parse("class C { #pm() { this.#pm(); } }");

    parse("class C { static #f = 1; static method() { this.#f = 2; } }");
    parse("class C { static #f = 1; static method() { C.#f = 2; } }");
    parse("class C { static #f = 1; static method() { const x = [this.#f]; } }");
    parse("class C { static #f = 1; static method() { const x = {y: C.#f}; } }");
    parse("class C { static #pm() { this.#pm(); } }");
    parse("class C { static #pm() { C.#pm(); } }");
  }

  @Test
  public void testPrivateProperty_nestedClasses() {
    expectFeatures(Feature.PRIVATE_CLASS_PROPERTIES);

    parse(
        """
        class Outer {
          #of1;
          #of2;
          om() {
            this.#of1;
            this.#of2;
            class Inner {
              #if1;
              #if2;
              im() {
                this.#of1;
                this.#of2;
                this.#if1;
                this.#if2;
              }
            }
          }
        }
        """);
  }

  @Test
  public void testPrivateProperty_nestedClasses_invalid_referenceInnerFieldFromOuter() {
    parseError(
        """
        class Outer {
          #of1;
          #of2;
          om() {
            this.#of1;
            this.#of2;
            this.#if1; // Invalid
            this.#if2; // Invalid
            class Inner {
              #if1;
              #if2;
              im() {
                this.#of1;
                this.#of2;
                this.#if1;
                this.#if2;
              }
            }
          }
        }
        """,
        PRIVATE_FIELD_NOT_DEFINED,
        PRIVATE_FIELD_NOT_DEFINED);
  }

  @Test
  public void testPrivateProperty_classMethodsReferencingOtherPrivatePropViaOptionalChain() {
    expectFeatures(Feature.PRIVATE_CLASS_PROPERTIES);

    parse("class C { #f = 1; method() { const t = this; t?.#f; } }");
    parse("class C { #pm() { const t = this; t?.#pm(); } }");
  }

  @Test
  public void testPrivateProperty_destructuredAssignmentDefaultValue() {
    parse("class C { #px = 1; method(x) { const { y = this.#px } = x; } }");
    parse("class C { #px = 1; method(x) { const { y: z = this.#px } = x; } }");
  }

  @Test
  public void testPrivateProperty_invalid_nonExistentPrivateProp() {
    parseError("class C { #f = this.#missing; }", PRIVATE_FIELD_NOT_DEFINED);
    parseError("class C { method() { this.#missing = 1; } }", PRIVATE_FIELD_NOT_DEFINED);
    parseError(
        "class C { #f = 1; method() { this.#f = this.#missing; } }", PRIVATE_FIELD_NOT_DEFINED);
    parseError("class C { method() { const t = this; t.#missing; } }", PRIVATE_FIELD_NOT_DEFINED);
    parseError("class C { method() { const t = this; t?.#missing; } }", PRIVATE_FIELD_NOT_DEFINED);
    parseError(
        "class C { method() { const f = () => { this.#missing; }; } }", PRIVATE_FIELD_NOT_DEFINED);
    parseError(
        "class C { method() { class D { method() { this.#missing = 1; } } } }",
        PRIVATE_FIELD_NOT_DEFINED);
    parseError(
        "class C { method() { class D { method() { const x = this.#missing; } } } }",
        PRIVATE_FIELD_NOT_DEFINED);

    parseError("class C { method() { this.#missing(); } }", PRIVATE_METHOD_NOT_DEFINED);

    parseError("class C { static #f = this.#missing; }", PRIVATE_FIELD_NOT_DEFINED);
    parseError("class C { static #f = C.#missing; }", PRIVATE_FIELD_NOT_DEFINED);

    parseError("class C { static { this.#missing; } }", PRIVATE_FIELD_NOT_DEFINED);
    parseError("class C { static { C.#missing; } }", PRIVATE_FIELD_NOT_DEFINED);

    parseError("class C { static { this.#missing(); } }", PRIVATE_METHOD_NOT_DEFINED);
    parseError("class C { static { C.#missing(); } }", PRIVATE_METHOD_NOT_DEFINED);
  }

  @Test
  public void testPrivateProperty_invalid_deletePrivateField() {
    parseError("class C { #f = 1; method() { delete this.#f; } }", PRIVATE_FIELD_DELETED);
    parseError(
        "class C { #f = 1; method() { const t = this; delete t.#f; } }", PRIVATE_FIELD_DELETED);
    parseError(
        "class C { #f = 1; method() { const t = this; delete t?.#f; } }", PRIVATE_FIELD_DELETED);
    parseError(
        "class C { #f = 1; method() { const a = {b: this}; delete ((a.b).#f); } }",
        PRIVATE_FIELD_DELETED);

    parseError(
        "class C { static #f = 1; static method() { delete this.#f; } }", PRIVATE_FIELD_DELETED);
    parseError(
        "class C { static #f = 1; static method() { delete C.#f; } }", PRIVATE_FIELD_DELETED);
    parseError("class C { static #f = 1; static { delete this.#f; } }", PRIVATE_FIELD_DELETED);
    parseError("class C { static #f = 1; static { delete C.#f; } }", PRIVATE_FIELD_DELETED);
  }

  @Test
  public void testPrivateProperty_invalid_deleteUndeclaredPrivateField() {
    parseError(
        "class C { method() { delete this.#f; } }",
        PRIVATE_FIELD_DELETED,
        PRIVATE_FIELD_NOT_DEFINED);
    parseError(
        "class C { method() { const t = this; delete t.#f; } }",
        PRIVATE_FIELD_DELETED,
        PRIVATE_FIELD_NOT_DEFINED);
    parseError(
        "class C { method() { const t = this; delete t?.#f; } }",
        PRIVATE_FIELD_DELETED,
        PRIVATE_FIELD_NOT_DEFINED);
    parseError(
        "class C { method() { const a = {b: this}; delete ((a.b).#f); } }",
        PRIVATE_FIELD_DELETED,
        PRIVATE_FIELD_NOT_DEFINED);

    parseError(
        "class C { static method() { delete this.#f; } }",
        PRIVATE_FIELD_DELETED,
        PRIVATE_FIELD_NOT_DEFINED);
    parseError(
        "class C { static method() { delete C.#f; } }",
        PRIVATE_FIELD_DELETED,
        PRIVATE_FIELD_NOT_DEFINED);
    parseError(
        "class C { static { delete this.#f; } }", PRIVATE_FIELD_DELETED, PRIVATE_FIELD_NOT_DEFINED);
    parseError(
        "class C { static { delete C.#f; } }", PRIVATE_FIELD_DELETED, PRIVATE_FIELD_NOT_DEFINED);
  }

  @Test
  public void testPrivateProperty_invalid_objectLiteralProperty() {
    parseError("const x = { #pf: 1 }", INVALID_PRIVATE_ID);
    parseError("const x = { #pm() {} }", INVALID_PRIVATE_ID);
    parseError("const x = { get #pp() {} }", INVALID_PRIVATE_ID);
    parseError("const x = { set #ps(x) {} }", INVALID_PRIVATE_ID);

    parseError("class C { method() { const x = { #pf: 1 }; } }", INVALID_PRIVATE_ID);
    parseError("class C { method() { const x = { #pm() {} }; } }", INVALID_PRIVATE_ID);
    parseError("class C { method() { const x = { get #pp() {} }; } }", INVALID_PRIVATE_ID);
    parseError("class C { method() { const x = { set #ps(x) {} }; } }", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_invalid_destructuredAssignment() {
    parseError("const { #px } = x;", INVALID_PRIVATE_ID);
    parseError("const { x: #px } = x;", INVALID_PRIVATE_ID);
    parseError("const { x = #px } = x;", INVALID_PRIVATE_ID);
    parseError("const { x: y = #px } = x;", INVALID_PRIVATE_ID);

    parseError("class C { method() { const { #px } = x; } }", INVALID_PRIVATE_ID);
    parseError("class C { method() { const { x: #px } = x; } }", INVALID_PRIVATE_ID);
    parseError("class C { method() { const { x = #px } = x; } }", INVALID_PRIVATE_ID);
    parseError("class C { method() { const { x: y = #px } = x; } }", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_invalid_variableName() {
    parseError("const #pv = 1;", INVALID_PRIVATE_ID);

    parseError("class C { method() { const #pv = 1; } }", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_invalid_functionName() {
    parseError("function #pf() {}", INVALID_PRIVATE_ID);
    parseError("function* #pf() {}", INVALID_PRIVATE_ID);
    parseError("async function #pf() {}", INVALID_PRIVATE_ID);
    parseError("async function* #pf() {}", INVALID_PRIVATE_ID);

    parseError("class C { method() { function #pf() {} } }", INVALID_PRIVATE_ID);
    parseError("class C { method() { function* #pf() {} } }", INVALID_PRIVATE_ID);
    parseError("class C { method() { async function #pf() {} } }", INVALID_PRIVATE_ID);
    parseError("class C { method() { async function* #pf() {} } }", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_invalid_paramName() {
    parseError("function f(#p) {}", INVALID_PRIVATE_ID);
    parseError("class C { method(#p) {} }", INVALID_PRIVATE_ID);
    parseError("class C { #p; method(#p) {} }", INVALID_PRIVATE_ID);

    parseError("class C { method() { function f(#p) {} } }", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_invalid_className() {
    parseError("class #PC {}", INVALID_PRIVATE_ID);
    parseError("class C extends #PSC {}", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_invalid_referencePrivateOutsideClass() {
    parseError("class C { #f = 1; } const c = new C(); c.#f;", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_invalid_referencePrivateOutsideClassViaOptionalChain() {
    parseError("class C { #f = 1; } const c = new C(); c?.#f;", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_invalid_referencePrivatePropFromObjectLiteral() {
    parseError("const o = {}; o.#f = 1;", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_invalid_referencePrivatePropFromObjectLiteralViaOptionalChain() {
    parseError("const o = {}; o?.#f;", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_invalid_importName() {
    parseError("import #pi from './someModule'", INVALID_PRIVATE_ID);
    parseError("import {#pi} from './someModule'", INVALID_PRIVATE_ID);
    parseError("import {x as #pi} from './someModule'", INVALID_PRIVATE_ID);
    parseError("import * as #pi from './someModule'", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_invalid_exportName() {
    parseError("export const #px = 1", INVALID_PRIVATE_ID);
    parseError("export var #px = 1", INVALID_PRIVATE_ID);
    parseError("export function #pf() {}", INVALID_PRIVATE_ID);
    parseError("export class #pc {}", INVALID_PRIVATE_ID);
    parseError("export {#px}", INVALID_PRIVATE_ID);
    parseError("export {x as #px}", INVALID_PRIVATE_ID);
    parseError("export {#px as default}", INVALID_PRIVATE_ID);
    parseError("export {#y as class}", INVALID_PRIVATE_ID);

    parseError("export {x as #px} from './someModule'", INVALID_PRIVATE_ID);
    parseError("export {default as #pd} from './someModule'", INVALID_PRIVATE_ID);
    parseError("export {#px as default} from './someModule'", INVALID_PRIVATE_ID);
    parseError("export {#pc as class} from './someModule'", INVALID_PRIVATE_ID);
    parseError("export {#px} from './someModule'", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_invalid_labeledStatement() {
    parseError("#pl: while (true) {}", INVALID_PRIVATE_ID);
    parseError("while (true) { break #pl; }", INVALID_PRIVATE_ID, "undefined label \"#pl\"");
    parseError("while (true) { continue #pl; }", INVALID_PRIVATE_ID, "undefined label \"#pl\"");

    parseError("class C { method() { #pl: while (true) {} } }", INVALID_PRIVATE_ID);
    parseError(
        "class C { method() { while (true) { break #pl; } } }",
        INVALID_PRIVATE_ID,
        "undefined label \"#pl\"");
    parseError(
        "class C { method() { while (true) { continue #pl; } } }",
        INVALID_PRIVATE_ID,
        "undefined label \"#pl\"");
  }

  @Test
  public void testPrivateProperty_inOperatorWithPrivateProp_valid() {
    expectFeatures(Feature.PRIVATE_CLASS_PROPERTIES);

    parse("class C { #f = 1; static isC(x) { return #f in x; } }");
    parse("class C { #f = this; m() { return #f in this.#f; } }");
  }

  @Test
  public void testPrivateProperty_inOperatorWithPrivateProp_linenocharno() {
    Node n =
        parse(
                """
                class C {
                  #f = 1;
                  static isC(x) {
                    return #f in x;
                  }
                }
                """)
            .getFirstChild();

    Node members = NodeUtil.getClassMembers(n);

    Node staticMethod = members.getLastChild();
    assertNode(staticMethod).hasType(Token.MEMBER_FUNCTION_DEF);
    Node methodBlock = staticMethod.getFirstChild().getLastChild();
    assertNode(methodBlock).hasType(Token.BLOCK);
    Node returnStatement = methodBlock.getFirstChild();
    assertNode(returnStatement).hasType(Token.RETURN);
    Node inExpression = returnStatement.getFirstChild();
    assertNode(inExpression).hasType(Token.IN);
    Node inExpressionLeft = inExpression.getFirstChild();
    assertNode(inExpressionLeft).hasType(Token.NAME);
    assertNode(inExpressionLeft).hasStringThat().isEqualTo("#f");
    assertThat(inExpressionLeft.getLineno()).isEqualTo(4);
    assertThat(inExpressionLeft.getCharno()).isEqualTo(11);
    assertThat(inExpressionLeft.getLength()).isEqualTo(2);
  }

  @Test
  public void testPrivateProperty_inOperatorWithPrivateProp_invalid_nonExistentPrivateProp() {
    expectFeatures(Feature.PRIVATE_CLASS_PROPERTIES);

    parseError("class C { static isC(x) { return #missing in x; } }", PRIVATE_FIELD_NOT_DEFINED);
  }

  @Test
  public void testPrivateProperty_inOperatorWithPrivateProp_invalid_privatePropOnRhs() {
    expectFeatures(Feature.PRIVATE_CLASS_PROPERTIES);

    parseError("class C { #f = 1; static isC(x) { return #f in #f; } }", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_inOperatorWithPrivateProp_invalid_surroundingParenthesis() {
    expectFeatures(Feature.PRIVATE_CLASS_PROPERTIES);

    parseError("class C { #f = 1; static isC(x) { return (#f) in x; } }", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_inOperatorWithPrivateProp_invalid_notInClass() {
    parseError("const o = {}; if (#f in o) {}", INVALID_PRIVATE_ID);
  }

  @Test
  public void testPrivateProperty_stringKeyThatLooksLikePrivateProp() {
    parse("const o = {'#notAPrivateProp': 1};");
  }

  @Test
  public void testEmptyClassStaticBlock() {
    parse("class C { static { } }");
    parse("let a = class { static { } };");
  }

  @Test
  public void testReturnInClassStaticBlock() {
    parseError("function f() {class C { static { return; } }}", "return must be inside function");
    parseError("class C { static { return; } }", "return must be inside function");
    parse("class C {static {function f() {return;}}}");
  }

  @Test
  public void testContinueInClassStaticBlock() {
    parseError("class C {static {continue;}}", UNEXPECTED_CONTINUE);
    parseError("for (let i = 1; i < 2; i++) {class C {static {continue;}}}", UNEXPECTED_CONTINUE);
    parseError("while (true) {class C {static {continue;}}}", UNEXPECTED_CONTINUE);
    parse("class C {static {while (true) {continue;}}}");
    parse("class C {static {for (let i = 1; i < 2; i++) {continue;}}}");
    parseError(
        "x: for (let i = 1; i < 2; i++) {class C {static {continue x;}}}",
        UNDEFINED_LABEL + " \"x\"");
    parseError("x: while (true) {class C {static {continue x;}}}", UNDEFINED_LABEL + " \"x\"");
    parse("class C {static {x: while (true) {continue x;}}}");
    parse("class C {static {x: for (let i = 1; i < 2; i++) {continue x;}}}");
    parseError("class C {static {x: { while (true) {continue x;}}}}", UNEXPECTED_LABELLED_CONTINUE);
    parseError(
        "class C {static {x: {for (let i = 1; i < 2; i++) {continue x;}}}}",
        UNEXPECTED_LABELLED_CONTINUE);
  }

  @Test
  public void testBreakInClassStaticBlock() {
    parseError("class C {static {break;}}", UNLABELED_BREAK);
    parseError("for (let i = 1; i < 2; i++) {class C {static {break;}}}", UNLABELED_BREAK);
    parseError("while (true) {class C {static {break;}}}", UNLABELED_BREAK);
    parse("class C {static {while (true) {break;}}}");
    parse("class C {static {for (let i = 1; i < 2; i++) {break;}}}");
    parseError(
        "x: for (let i = 1; i < 2; i++) {class C {static {break x;}}}", UNDEFINED_LABEL + " \"x\"");
    parseError("x: while (true) {class C {static {break x;}}}", UNDEFINED_LABEL + " \"x\"");
    parse("class C {static {x: while (true) {break x;}}}");
    parse("class C {static {x: for (let i = 1; i < 2; i++) {break x;}}}");
    parse("class C {static {x: { while (true) {break x;}}}}");
    parse("class C {static {x: {for (let i = 1; i < 2; i++) {break x;}}}}");
  }

  @Test
  public void testYieldInClassStaticBlock() {
    parseError("class C {static {var ind; yield;}}", "primary expression expected");
    parseError("function* f(ind) {class C{ static {yield ind; ind++;}}}", UNEXPECTED_YIELD);
    parse("class C{ static {function* f(ind) {yield ind; ind++;}}}");
  }

  @Test
  public void testAwaitInClassStaticBlock() {
    parseError("class C {static {await 1;}}", UNEXPECTED_AWAIT);
    parseError("async function f() {class C{ static {await 1;}}}", UNEXPECTED_AWAIT);
    parse("class C{ static {async function f() {await 1;}}}");
    parseError("async () => {class C{ static {await 1;}}}", UNEXPECTED_AWAIT);
    parse("class C{ static {async ()=>{await 1;}}}");
  }

  @Test
  public void testClassStaticBlock_this() {
    // multiple fields
    parse(
        """
        class C {
        static field1 = 1; static field2 = 2; static field3 = 3;
        static {
        let x = this.field1; let y = this.field2; let z = this.field3;
        }
        }
        """);
    parse("class C { static { this.field1 = 1; this.field2 = 2; this.field3 = 3; } }");
    parse(
        """
        let a = class {
        static field1 = 1; static field2 = 2; static field3 = 3;
        static {
        let x = this.field1; let y = this.field2; let z = this.field3;
        }
        };
        """);
    parse("let a = class { static { this.field1 = 1; this.field2 = 2; this.field3 = 3; } };");
    // functions
    parse(
        """
        class C {
        static field1 = 1;
        static {
        function incr() { return ++A.field1; }
        console.log(incr());
        if(incr()) {
        this.field2 = 2;
        }
        }
        }
        """);
    // try catch
    parse(
        """
        class C {
        static field1 = 1;
        static {
        try {
        this.field1 = 2;
        }
        catch {
        }
        }
        }
        """);
  }

  @Test
  public void testClassStaticBlock_inheritance() {
    // It is a syntax error to call super() in a class static initialization block.
    // Must get reported in CheckSuper.java.
    parse("class Base {} class C extends Base { static { super(); } }");
    // allow accessing static properties of the base class
    parse("class Base { static y; } class C extends Base { static { super.y; } }");
    // allow accessing non-static properties of the base class
    parse("class Base { y; } class C extends Base { static { super.y; } }");
  }

  @Test
  public void testClassExtendsLeftHandSideExpression() {
    parse("class A {} class B extends (0, A) {}");
    parseError("class A {} class B extends 0, A {}", "'{' expected");
  }

  @Test
  public void testMultipleClassStaticBlocks() {
    // empty
    parse("class C { static { } static { } }");
    parse("let a = class { static { } static { } };");
    // multiple fields
    parse(
        """
        class C {
        static field1 = 1; static field2 = 2; static field3 = 3;
        static {
        let x = this.field1; let y = this.field2;
        }
        static {
        let z = this.field3;
        }
        }
        """);
    parse("class C { static { this.field1 = 1; this.field2 = 2; } static { this.field3 = 3; } }");
    parse(
        """
        let a = class {
        static field1 = 1; static field2 = 2; static field3 = 3;
        static {
        let x = this.field1; let y = this.field2;
        }
        static {
        let z = this.field3;
        }
        };
        """);
    parse(
        """
        let a = class {
        static {
        this.field1 = 1; this.field2 = 2;
        }
        static {
         this.field3 = 3;
        }
        };
        """);
  }

  @Test
  public void testClassStaticBlock_linenocharno() {
    Node n = parse("class C {\n static {}\n }").getFirstChild();

    assertNode(n).hasType(Token.CLASS);
    assertNode(n).hasLineno(1);
    assertNode(n).hasCharno(0);

    Node members = NodeUtil.getClassMembers(n);
    assertNode(members).hasType(Token.CLASS_MEMBERS);

    Node staticBlock = members.getFirstChild();

    assertNode(staticBlock).hasType(Token.BLOCK);
    assertNode(staticBlock).hasLineno(2);
    assertNode(staticBlock).hasCharno(8);
    assertNode(staticBlock).hasLength(2);
  }

  @Test
  public void testClassStaticBlock_invalid() {
    parseError("class { {} }", "'identifier' expected");
    parseError("class { static { static { } } }", "'identifier' expected");
    parseError("var o = { static {} };", "Cannot use keyword in short object literal");
  }

  @Test
  public void testClassStaticSuper() {
    parse(
        """
        class Bar {
          static double(n) {
            return n * 2
          }
        }
        class Baz extends Bar {
          // Used from a static field initializer.
          static val1 = super.double(6);

          static val2;
          static {
            // Used from within a static block.
            Baz.val2 = super.double(5);
          }
        }
        """);
  }

  @Test
  public void testSuper1() {
    expectFeatures(Feature.SUPER);
    strictMode = SLOPPY;

    // TODO(johnlenz): super in global scope should be a syntax error
    parse("super;");

    parse("function f() {super;};");

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning("super;", requiresLanguageModeMessage(Feature.SUPER));

    mode = LanguageMode.ECMASCRIPT3;
    parseWarning("super;", requiresLanguageModeMessage(Feature.SUPER));
  }

  @Test
  public void testNewTarget() {
    expectFeatures(Feature.NEW_TARGET);
    strictMode = SLOPPY;

    parseError("new.target;", "new.target must be inside a function");

    parse("function f() { new.target; };");

    mode = LanguageMode.ECMASCRIPT3;
    parseWarning(
        "class C { f() { new.target; } }",
        requiresLanguageModeMessage(Feature.CLASSES),
        requiresLanguageModeMessage(Feature.MEMBER_DECLARATIONS),
        requiresLanguageModeMessage(Feature.NEW_TARGET));

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning(
        "class C { f() { new.target; } }",
        requiresLanguageModeMessage(Feature.CLASSES),
        requiresLanguageModeMessage(Feature.MEMBER_DECLARATIONS),
        requiresLanguageModeMessage(Feature.NEW_TARGET));

    mode = LanguageMode.ECMASCRIPT_2015;
    expectFeatures(Feature.CLASSES, Feature.MEMBER_DECLARATIONS, Feature.NEW_TARGET);
    parse("class C { f() { new.target; } }");
  }

  @Test
  public void testNewDotSomethingInvalid() {
    strictMode = SLOPPY;

    parseError("function f(){new.something}", "'target' expected");
  }

  @Test
  public void hookWithDecimalNotParsedAsOptionalChaining() {
    Node n = parse("a?.1:2").getFirstFirstChild();

    assertNode(n).hasType(Token.HOOK);
    assertNode(n).hasLineno(1);
    assertNode(n).hasCharno(0);
    assertNode(n.getFirstChild()).isEqualTo(IR.name("a"));
    assertNode(n.getSecondChild()).isEqualTo(IR.number(0.1));
    assertNode(n.getLastChild()).isEqualTo(IR.number(2.0));
  }

  @Test
  public void optChainGetProp() {
    Node n = parse("a?.b").getFirstFirstChild();

    assertNode(n).hasType(Token.OPTCHAIN_GETPROP);
    assertNode(n).isOptionalChainStart();
    assertNode(n).hasLineno(1);
    assertNode(n).hasCharno(3);
    assertNode(n).hasLength(1);
    assertNode(n.getFirstChild()).isEqualTo(IR.name("a"));
    assertNode(n).hasStringThat().isEqualTo("b");
  }

  @Test
  public void optChainGetPropWithKeyword() {
    Node n = parse("a?.finally").getFirstFirstChild();

    assertNode(n).hasType(Token.OPTCHAIN_GETPROP);
    assertNode(n).isOptionalChainStart();
    assertNode(n).hasLineno(1);
    assertNode(n).hasCharno(3);
    assertNode(n).hasLength(7);
    assertNode(n.getFirstChild()).isEqualTo(IR.name("a"));
    assertNode(n).hasStringThat().isEqualTo("finally");
  }

  @Test
  public void optChainGetElem() {
    Node n = parse("a?.[1]").getFirstFirstChild();

    assertNode(n).hasType(Token.OPTCHAIN_GETELEM);
    assertNode(n).isOptionalChainStart();
    assertNode(n).hasLineno(1);
    assertNode(n).hasCharno(0);
    assertNode(n).hasLength(6);
    assertNode(n.getFirstChild()).isEqualTo(IR.name("a"));
    assertNode(n.getSecondChild()).isEqualTo(IR.number(1.0));
  }

  @Test
  public void optChainCall() {
    Node n = parse("a?.()").getFirstFirstChild();

    assertNode(n).hasType(Token.OPTCHAIN_CALL);
    assertNode(n).isOptionalChainStart();
    assertNode(n).hasLineno(1);
    assertNode(n).hasCharno(0);
    assertNode(n).hasLength(5);
    assertNode(n.getFirstChild()).isEqualTo(IR.name("a"));
  }

  // Check that optional chain node that is an arg of a call gets marked as the start of a new chain
  @Test
  public void optChainStartOfChain_innerChainIsArgOfACall() {
    Node optChainCall = parse("a?.b?.(x?.y);").getFirstFirstChild();
    assertThat(optChainCall.isOptChainCall()).isTrue();

    Node optChainGetProp = optChainCall.getFirstChild(); // `a?.b`
    Node optChainArg = optChainCall.getLastChild(); // `x?.y`

    // Check that `a?.b` is the start of an opt chain
    assertThat(optChainGetProp.isOptChainGetProp()).isTrue();
    assertThat(optChainGetProp.isOptionalChainStart()).isTrue();

    // Check that `x?.y` is the start of an opt chain
    assertThat(optChainArg.isOptChainGetProp()).isTrue();
    assertThat(optChainArg.isOptionalChainStart()).isTrue();
  }

  @Test
  public void optChainStartOfChain_optChainGetProp() {
    Node outerGet = parse("a?.b.c").getFirstFirstChild();
    Node innerGet = outerGet.getFirstChild();

    // `a?.b.c`
    assertNode(outerGet).hasType(Token.OPTCHAIN_GETPROP);
    assertNode(outerGet).isNotOptionalChainStart();
    assertNode(outerGet).hasLineno(1);
    assertNode(outerGet).hasCharno(5);
    assertNode(outerGet).hasLength(1);

    // `a?.b`
    assertNode(innerGet).hasType(Token.OPTCHAIN_GETPROP);
    assertNode(innerGet).isOptionalChainStart();
    assertNode(innerGet).hasLineno(1);
    assertNode(innerGet).hasCharno(3);
    assertNode(innerGet).hasLength(1);

    assertNode(outerGet).hasStringThat().isEqualTo("c");
  }

  @Test
  public void optChainStartOfChain_optChainGetElem() {
    Node outerGet = parse("a?.[b][c]").getFirstFirstChild();
    Node innerGet = outerGet.getFirstChild();

    // `a?.[b][c]`
    assertNode(outerGet).hasType(Token.OPTCHAIN_GETELEM);
    assertNode(outerGet).isNotOptionalChainStart();
    assertNode(outerGet).hasLineno(1);
    assertNode(outerGet).hasCharno(0);
    assertNode(outerGet).hasLength(9);
    //
    // // `a?.[b]`
    assertNode(innerGet).hasType(Token.OPTCHAIN_GETELEM);
    assertNode(innerGet).isOptionalChainStart();
    assertNode(innerGet).hasLineno(1);
    assertNode(innerGet).hasCharno(0);
    assertNode(innerGet).hasLength(6);

    assertNode(outerGet.getSecondChild()).isEqualTo(IR.name("c"));
  }

  @Test
  public void optChainStartOfChain_optChainCall() {
    Node outerCall = parse("a?.()(b)").getFirstFirstChild();
    Node innerCall = outerCall.getFirstChild();

    // `a?.()(b)`
    assertNode(outerCall).hasType(Token.OPTCHAIN_CALL);
    assertNode(outerCall).isNotOptionalChainStart();
    assertNode(outerCall).hasLineno(1);
    assertNode(outerCall).hasCharno(0);
    assertNode(outerCall).hasLength(8);

    // `a?.()`
    assertNode(innerCall).hasType(Token.OPTCHAIN_CALL);
    assertNode(innerCall).isOptionalChainStart();
    assertNode(innerCall).hasLineno(1);
    assertNode(innerCall).hasCharno(0);
    assertNode(innerCall).hasLength(5);

    assertNode(outerCall.getSecondChild()).isEqualTo(IR.name("b"));
  }

  @Test
  public void optChainParens_optChainGetProp() {
    Node outerGet = parse("(a?.b).c").getFirstFirstChild();
    Node innerGet = outerGet.getFirstChild();

    // `(a?.b).c`
    assertNode(outerGet).hasType(Token.GETPROP);
    assertNode(outerGet).hasLineno(1);
    assertNode(outerGet).hasCharno(7);
    assertNode(outerGet).hasLength(1);

    // `a?.b`
    assertNode(innerGet).hasType(Token.OPTCHAIN_GETPROP);
    assertNode(innerGet).isOptionalChainStart();
    assertNode(innerGet).hasLineno(1);
    assertNode(innerGet).hasCharno(4);
    assertNode(innerGet).hasLength(1);

    assertNode(outerGet).hasStringThat().isEqualTo("c");
  }

  @Test
  public void callExpressionBeforeOptionalGetProp() {
    Node get = parse("a()?.b").getFirstFirstChild();
    Node call = get.getFirstChild();

    assertNode(get).hasType(Token.OPTCHAIN_GETPROP);
    assertNode(get).hasLineno(1);
    assertNode(get).hasCharno(5);
    assertNode(get).hasLength(1);

    assertNode(call).hasType(Token.CALL);
    assertNode(call).hasLineno(1);
    assertNode(call).hasCharno(0);
    assertNode(call).hasLength(3);
  }

  @Test
  public void optChainChain() {

    parse("a?.b?.c");
    parse("a.b?.c");
    parse("a?.b?.[1]");
    parse("a?.b?.()");
    parse("a?.[1]?.b");
    parse("a?.[1]?.b()");
    parse("a?.b?.c?.d");
    parse("a?.b?.c?.[1]");
    parse("a?.b?.c?.()");
    parse("a?.(c)?.b");
    parse("a().b?.c");
  }

  @Test
  public void optChainAssignError() {
    parseError("a?.b = c", "invalid assignment target");
  }

  @Test
  public void optChainConstructorError() {
    parseError("new a?.()", "Optional chaining is forbidden in construction contexts.");
    parseError("new a?.b()", "Optional chaining is forbidden in construction contexts.");
  }

  @Test
  public void optChainTemplateLiteralError() {
    parseError("a?.()?.`hello`", "template literal cannot be used within optional chaining");
    parseError("a?.`hello`", "template literal cannot be used within optional chaining");
    parseError("a?.b`hello`", "template literal cannot be used within optional chaining");
    // https://github.com/tc39/test262/blob/master/test/language/expressions/optional-chaining/early-errors-tail-position-template-string-esi.js
    // test to prevent automatic semicolon insertion rules
    parseError("a?.b\n`hello`", "template literal cannot be used within optional chaining");
  }

  @Test
  public void optChainMiscErrors() {
    parseError("super?.()", "Optional chaining is forbidden in super?.");
    parseError("super?.foo", "Optional chaining is forbidden in super?.");
    parseError("new?.target", "Optional chaining is forbidden in `new?.target` contexts.");
    parseError("import?.('foo')", "Optional chaining is forbidden in import?.");
  }

  @Test
  public void optChainDeleteValid() {
    parse("delete a?.b");
  }

  @Test
  public void optChainEs2019() {
    expectFeatures(Feature.OPTIONAL_CHAINING);
    mode = LanguageMode.ECMASCRIPT_2019;

    parseWarning("a?.b", requiresLanguageModeMessage(Feature.OPTIONAL_CHAINING));
  }

  @Test
  public void optChainSyntaxError() {
    parseError("a?.{}", "syntax error: { not allowed in optional chain");
    // optional chain cannot be applied on a BLOCK `{}`
    parseError("{a:x}?.a", "primary expression expected");
    parse("({a:x})?.a");
  }

  @Test
  public void testArrow1() {
    expectFeatures(Feature.ARROW_FUNCTIONS);
    strictMode = SLOPPY;

    parse("()=>1;");
    parse("()=>{}");
    parse("(a,b) => a + b;");
    parse("a => b");
    parse("a => { return b }");
    parse("a => b");
    parse("var x = (a => b);");

    mode = LanguageMode.ECMASCRIPT5;
    parseWarning("a => b", requiresLanguageModeMessage(Feature.ARROW_FUNCTIONS));

    mode = LanguageMode.ECMASCRIPT3;
    parseWarning("a => b;", requiresLanguageModeMessage(Feature.ARROW_FUNCTIONS));
  }

  @Test
  public void testArrowInvalid1() {
    strictMode = SLOPPY;
    parseError("*()=>1;", "primary expression expected");
    parseError("var f = x\n=>2", "No newline allowed before '=>'");
    parseError("f = (x,y)\n=>2;", "No newline allowed before '=>'");
    parseError("f( (x,y)\n=>2)", "No newline allowed before '=>'");
  }

  @Test
  public void testInvalidAwait() {
    parseError("await 15;", UNEXPECTED_AWAIT);
    parseError("function f() { return await 5; }", UNEXPECTED_AWAIT);
    parseError(
        "async function f(x = await 15) { return x; }",
        "`await` is illegal in parameter default value.");
  }

  @Test
  public void testInvalidAwaitInsideNestedFunction() {
    parse("async function f() { async function f2() { return await 5; } }");
    parseError("async function f() { function f2() { return await 5; } }", UNEXPECTED_AWAIT);
  }

  @Test
  public void testAsyncFunction() {
    String asyncFunctionExpressionSource = "f = async function() {};";
    String asyncFunctionDeclarationSource = "async function f() {}";
    expectFeatures(Feature.ASYNC_FUNCTIONS);

    for (LanguageMode m : LanguageMode.values()) {
      mode = m;
      strictMode = (m == LanguageMode.ECMASCRIPT3) ? SLOPPY : STRICT;
      if (m.featureSet.has(Feature.ASYNC_FUNCTIONS)) {
        parse(asyncFunctionExpressionSource);
        parse(asyncFunctionDeclarationSource);
      } else {
        parseWarning(
            asyncFunctionExpressionSource, requiresLanguageModeMessage(Feature.ASYNC_FUNCTIONS));
        parseWarning(
            asyncFunctionDeclarationSource, requiresLanguageModeMessage(Feature.ASYNC_FUNCTIONS));
      }
    }
  }

  @Test
  public void testAsyncNamedFunction() {
    mode = LanguageMode.ECMASCRIPT_2015;
    expectFeatures(
        Feature.CLASSES,
        Feature.MEMBER_DECLARATIONS,
        Feature.CONST_DECLARATIONS,
        Feature.LET_DECLARATIONS);
    parse(
        """
        class C {
          async(x) { return x; }
        }
        const c = new C();
        c.async(1);
        let foo = async(5);
        """);
  }

  @Test
  public void testAsyncGeneratorFunction() {
    expectFeatures(Feature.ASYNC_FUNCTIONS, Feature.GENERATORS, Feature.ASYNC_GENERATORS);
    parse("async function *f(){}");
    parse("f = async function *(){}");
    parse("class C { async *foo(){} }");
  }

  @Test
  public void testAsyncArrowFunction() {
    doAsyncArrowFunctionTest("f = async (x) => x + 1");
    doAsyncArrowFunctionTest("f = async x => x + 1");
  }

  private void doAsyncArrowFunctionTest(String arrowFunctionSource) {
    expectFeatures(Feature.ASYNC_FUNCTIONS, Feature.ARROW_FUNCTIONS);

    for (LanguageMode m : LanguageMode.values()) {
      mode = m;
      strictMode = (m == LanguageMode.ECMASCRIPT3) ? SLOPPY : STRICT;
      if (m.featureSet.has(Feature.ASYNC_FUNCTIONS)) {
        parse(arrowFunctionSource);
      } else if (m.featureSet.has(Feature.ARROW_FUNCTIONS)) {
        parseWarning(arrowFunctionSource, requiresLanguageModeMessage(Feature.ASYNC_FUNCTIONS));
      } else {
        parseWarning(
            arrowFunctionSource,
            requiresLanguageModeMessage(Feature.ARROW_FUNCTIONS),
            requiresLanguageModeMessage(Feature.ASYNC_FUNCTIONS));
      }
    }
  }

  @Test
  public void testAsyncArrowInvalid() {
    parseError("f = not_async (x) => x + 1;", "'=>' unexpected");
  }

  @Test
  public void testAsyncMethod() {
    expectFeatures(Feature.ASYNC_FUNCTIONS);
    parse("o={async m(){}}");
    parse("o={async [a+b](){}}");
    parse("class C{async m(){}}");
    parse("class C{static async m(){}}");
    parse("class C{async [a+b](){}}");
    parse("class C{static async [a+b](){}}");
  }

  @Test
  public void testInvalidAsyncMethod() {
    strictMode = SLOPPY;
    expectFeatures(Feature.MEMBER_DECLARATIONS);
    // 'async' allowed as a name
    parse("o={async(){}}");
    parse("class C{async(){}}");
    parse("class C{static async(){}}");

    expectFeatures();
    parse("o={async:false}");
    // newline after 'async' forces it to be the property name
    parseError("o={async\nm(){}}", "'}' expected");
    parseError("o={static async\nm(){}}", "Cannot use keyword in short object literal");

    expectFeatures(Feature.PUBLIC_CLASS_FIELDS);
    parse("class C{async};"); // class field
    parse("class C{async\nm(){}}"); // class field
    parse("class C{static async\nm(){}}"); // class field
  }

  @Test
  public void testAwaitExpression() {
    expectFeatures(Feature.ASYNC_FUNCTIONS);
    parse("async function f(p){await p}");
    parse("f = async function(p){await p}");
    parse("f = async(p)=>await p");
    parse("class C{async m(p){await p}}");
    parse("class C{static async m(p){await p}}");
  }

  @Test
  public void testAwaitExpressionInvalid() {
    parseError("async function f() { await; }", "primary expression expected");
  }

  @Test
  public void testFor_ES5() {
    parse("for (var x; x != 10; x = next()) {}");
    parse("for (var x; x != 10; x = next());");
    parse("for (var x = 0; x != 10; x++) {}");
    parse("for (var x = 0; x != 10; x++);");

    parse("var x; for (x; x != 10; x = next()) {}");
    parse("var x; for (x; x != 10; x = next());");

    parseError("for (x in {};;) {}", "')' expected");
  }

  @Test
  public void testFor_ES6() {
    strictMode = SLOPPY;

    expectFeatures(Feature.LET_DECLARATIONS);
    parse("for (let x; x != 10; x = next()) {}");
    parse("for (let x; x != 10; x = next());");
    parse("for (let x = 0; x != 10; x++) {}");
    parse("for (let x = 0; x != 10; x++);");

    expectFeatures(Feature.CONST_DECLARATIONS);
    parse("for (const x = 0; x != 10; x++) {}");
    parse("for (const x = 0; x != 10; x++);");
  }

  @Test
  public void testForConstNoInitializer() {
    parseError("for (const x; x != 10; x = next()) {}", "const variables must have an initializer");
    parseError("for (const x; x != 10; x = next());", "const variables must have an initializer");
  }

  @Test
  public void testForIn_ES6() {
    strictMode = SLOPPY;

    parse("for (a in b) c;");
    parse("for (var a in b) c;");

    expectFeatures(Feature.LET_DECLARATIONS);
    parse("for (let a in b) c;");

    expectFeatures(Feature.CONST_DECLARATIONS);
    parse("for (const a in b) c;");

    expectFeatures();
    parseError("for (a,b in c) d;", INVALID_ASSIGNMENT_TARGET);
    parseError(
        "for (var a,b in c) d;",
        "for-in statement may not have more than one variable declaration");

    parseError(
        "for (let a,b in c) d;",
        "for-in statement may not have more than one variable declaration");

    parseError(
        "for (const a,b in c) d;",
        "for-in statement may not have more than one variable declaration");

    parseError("for (a=1 in b) c;", INVALID_ASSIGNMENT_TARGET);
    parseError("for (let a=1 in b) c;", "for-in statement may not have initializer");
    parseError("for (const a=1 in b) c;", "for-in statement may not have initializer");
    parseError("for (var a=1 in b) c;", "for-in statement may not have initializer");
    parseError("for (\"a\" in b) c;", INVALID_ASSIGNMENT_TARGET);
  }

  @Test
  public void testForIn_ES5() {
    mode = LanguageMode.ECMASCRIPT5;
    strictMode = SLOPPY;

    parse("for (a in b) c;");
    parse("for (var a in b) c;");

    parseError("for (a=1 in b) c;", INVALID_ASSIGNMENT_TARGET);
    parseWarning("for (var a=1 in b) c;", "for-in statement should not have initializer");
  }

  @Test
  public void testForInDestructuring() {
    strictMode = SLOPPY;

    expectFeatures(Feature.OBJECT_DESTRUCTURING);
    parse("for ({a} in b) c;");
    parse("for (var {a} in b) c;");
    expectFeatures(Feature.OBJECT_DESTRUCTURING, Feature.LET_DECLARATIONS);
    parse("for (let {a} in b) c;");
    expectFeatures(Feature.OBJECT_DESTRUCTURING, Feature.CONST_DECLARATIONS);
    parse("for (const {a} in b) c;");

    expectFeatures(Feature.OBJECT_DESTRUCTURING);
    parse("for ({a: b} in c) d;");
    parse("for (var {a: b} in c) d;");
    expectFeatures(Feature.OBJECT_DESTRUCTURING, Feature.LET_DECLARATIONS);
    parse("for (let {a: b} in c) d;");
    expectFeatures(Feature.OBJECT_DESTRUCTURING, Feature.CONST_DECLARATIONS);
    parse("for (const {a: b} in c) d;");

    expectFeatures(Feature.ARRAY_DESTRUCTURING);
    parse("for ([a] in b) c;");
    parse("for (var [a] in b) c;");
    expectFeatures(Feature.ARRAY_DESTRUCTURING, Feature.LET_DECLARATIONS);
    parse("for (let [a] in b) c;");
    expectFeatures(Feature.ARRAY_DESTRUCTURING, Feature.CONST_DECLARATIONS);
    parse("for (const [a] in b) c;");
  }

  @Test
  public void testForInDestructuringInvalid() {
    strictMode = SLOPPY;

    parseError("for ({a: b} = foo() in c) d;", INVALID_ASSIGNMENT_TARGET);
    parseError("for (var {a: b} = foo() in c) d;", "for-in statement may not have initializer");
    parseError("for (let {a: b} = foo() in c) d;", "for-in statement may not have initializer");
    parseError("for (const {a: b} = foo() in c) d;", "for-in statement may not have initializer");

    parseError("for ([a] = foo() in b) c;", INVALID_ASSIGNMENT_TARGET);
    parseError("for (var [a] = foo() in b) c;", "for-in statement may not have initializer");
    parseError("for (let [a] = foo() in b) c;", "for-in statement may not have initializer");
    parseError("for (const [a] = foo() in b) c;", "for-in statement may not have initializer");
  }

  @Test
  public void testForOf1() {
    strictMode = SLOPPY;

    expectFeatures(Feature.FOR_OF);
    parse("for(a of b) c;");
    parse("for(var a of b) c;");
    expectFeatures(Feature.FOR_OF, Feature.LET_DECLARATIONS);
    parse("for(let a of b) c;");
    expectFeatures(Feature.FOR_OF, Feature.CONST_DECLARATIONS);
    parse("for(const a of b) c;");
  }

  @Test
  public void testForOf2() {
    strictMode = SLOPPY;

    parseError("for(a=1 of b) c;", INVALID_ASSIGNMENT_TARGET);
    parseError("for(var a=1 of b) c;", "for-of statement may not have initializer");
    parseError("for(let a=1 of b) c;", "for-of statement may not have initializer");
    parseError("for(const a=1 of b) c;", "for-of statement may not have initializer");
  }

  @Test
  public void testForOf3() {
    strictMode = SLOPPY;

    parseError(
        "for(var a, b of c) d;",
        "for-of statement may not have more than one variable declaration");
    parseError(
        "for(let a, b of c) d;",
        "for-of statement may not have more than one variable declaration");
    parseError(
        "for(const a, b of c) d;",
        "for-of statement may not have more than one variable declaration");
  }

  @Test
  public void testForOf4() {
    strictMode = SLOPPY;

    parseError("for(a, b of c) d;", INVALID_ASSIGNMENT_TARGET);
  }

  @Test
  public void testValidForAwaitOf() {
    strictMode = SLOPPY;

    expectFeatures(Feature.FOR_AWAIT_OF);
    parse("async () => { for await(a of b) c;}");
    parse("async () => { for await(var a of b) c;}");
    parse("async () => { for await (a.x of b) c;}");
    parse("async () => { for await ([a1, a2, a3] of b) c;}");
    parse("async () => { for await (const {x, y, z} of b) c;}");
    // default value inside a pattern isn't an initializer
    parse("async () => { for await (const {x, y = 2, z} of b) c;}");
    expectFeatures(Feature.FOR_AWAIT_OF, Feature.LET_DECLARATIONS);
    parse("async () => { for await(let a of b) c;}");
    expectFeatures(Feature.FOR_AWAIT_OF, Feature.CONST_DECLARATIONS);
    parse("async () => { for await(const a of b) c;}");
  }

  @Test
  public void testInvalidForAwaitOfInitializers() {
    strictMode = SLOPPY;

    parseError("async () => { for await (a=1 of b) c;}", INVALID_ASSIGNMENT_TARGET);
    parseError(
        "async () => { for await (var a=1 of b) c;}",
        "for-await-of statement may not have initializer");
    parseError(
        "async () => { for await (let a=1 of b) c;}",
        "for-await-of statement may not have initializer");
    parseError(
        "async () => { for await (const a=1 of b) c;}",
        "for-await-of statement may not have initializer");
    parseError(
        "async () => { for await (let {a} = {} of b) c;}",
        "for-await-of statement may not have initializer");
  }

  @Test
  public void testInvalidForAwaitOfMultipleInitializerTargets() {
    strictMode = SLOPPY;

    parseError("async () => { for await (a, b of c) d;}", INVALID_ASSIGNMENT_TARGET);

    parseError(
        "async () => { for await (var a, b of c) d;}",
        "for-await-of statement may not have more than one variable declaration");
    parseError(
        "async () => { for await (let a, b of c) d;}",
        "for-await-of statement may not have more than one variable declaration");
    parseError(
        "async () => { for await (const a, b of c) d;}",
        "for-await-of statement may not have more than one variable declaration");
  }

  @Test
  public void testInvalidForAwaitOf() {
    parseError("for await (a of b) foo();", "'for-await-of' used in a non-async function context");
  }

  @Test
  public void testDestructuringInForLoops() {
    strictMode = SLOPPY;

    // Destructuring forbids an initializer in for-in/for-of
    parseError("for (var {x: y} = foo() in bar()) {}", "for-in statement may not have initializer");
    parseError("for (let {x: y} = foo() in bar()) {}", "for-in statement may not have initializer");
    parseError(
        "for (const {x: y} = foo() in bar()) {}", "for-in statement may not have initializer");

    parseError("for (var {x: y} = foo() of bar()) {}", "for-of statement may not have initializer");
    parseError("for (let {x: y} = foo() of bar()) {}", "for-of statement may not have initializer");
    parseError(
        "for (const {x: y} = foo() of bar()) {}", "for-of statement may not have initializer");

    // but requires it in a vanilla for loop
    parseError("for (var {x: y};;) {}", "destructuring must have an initializer");
    parseError("for (let {x: y};;) {}", "destructuring must have an initializer");
    parseError("for (const {x: y};;) {}", "const variables must have an initializer");
  }

  @Test
  public void testInvalidDestructuring() {
    strictMode = SLOPPY;

    // {x: 5} and {x: 'str'} are valid object literals but not valid patterns.
    parseError("for ({x: 5} in foo()) {}", INVALID_ASSIGNMENT_TARGET);
    parseError("for ({x: 'str'} in foo()) {}", INVALID_ASSIGNMENT_TARGET);
    parseError("var {x: 5} = foo();", INVALID_ASSIGNMENT_TARGET);
    parseError("var {x: 'str'} = foo();", INVALID_ASSIGNMENT_TARGET);
    parseError("({x: 5} = foo());", INVALID_ASSIGNMENT_TARGET);
    parseError("({x: 'str'} = foo());", INVALID_ASSIGNMENT_TARGET);

    // {method(){}} is a valid object literal but not a valid object pattern.
    parseError("function f({method(){}}) {}", "'}' expected");
    parseError("function f({method(){}} = foo()) {}", "'}' expected");
  }

  @Test
  public void testForOfPatterns() {
    strictMode = SLOPPY;

    expectFeatures(Feature.FOR_OF, Feature.OBJECT_DESTRUCTURING);
    parse("for({x} of b) c;");
    parse("for({x: y} of b) c;");

    expectFeatures(Feature.FOR_OF, Feature.ARRAY_DESTRUCTURING);
    parse("for([x, y] of b) c;");
    parse("for([x, ...y] of b) c;");

    expectFeatures(Feature.FOR_OF, Feature.OBJECT_DESTRUCTURING, Feature.LET_DECLARATIONS);
    parse("for(let {x} of b) c;");
    parse("for(let {x: y} of b) c;");

    expectFeatures(Feature.FOR_OF, Feature.ARRAY_DESTRUCTURING, Feature.LET_DECLARATIONS);
    parse("for(let [x, y] of b) c;");
    parse("for(let [x, ...y] of b) c;");

    expectFeatures(Feature.FOR_OF, Feature.OBJECT_DESTRUCTURING, Feature.CONST_DECLARATIONS);
    parse("for(const {x} of b) c;");
    parse("for(const {x: y} of b) c;");

    expectFeatures(Feature.FOR_OF, Feature.ARRAY_DESTRUCTURING, Feature.CONST_DECLARATIONS);
    parse("for(const [x, y] of b) c;");
    parse("for(const [x, ...y] of b) c;");

    expectFeatures(Feature.FOR_OF, Feature.OBJECT_DESTRUCTURING);
    parse("for(var {x} of b) c;");
    parse("for(var {x: y} of b) c;");

    expectFeatures(Feature.FOR_OF, Feature.ARRAY_DESTRUCTURING);
    parse("for(var [x, y] of b) c;");
    parse("for(var [x, ...y] of b) c;");
  }

  @Test
  public void testForOfPatternsWithInitializer() {
    strictMode = SLOPPY;

    parseError("for({x}=a of b) c;", INVALID_ASSIGNMENT_TARGET);
    parseError("for({x: y}=a of b) c;", INVALID_ASSIGNMENT_TARGET);
    parseError("for([x, y]=a of b) c;", INVALID_ASSIGNMENT_TARGET);
    parseError("for([x, ...y]=a of b) c;", INVALID_ASSIGNMENT_TARGET);

    parseError("for(let {x}=a of b) c;", "for-of statement may not have initializer");
    parseError("for(let {x: y}=a of b) c;", "for-of statement may not have initializer");
    parseError("for(let [x, y]=a of b) c;", "for-of statement may not have initializer");
    parseError("for(let [x, ...y]=a of b) c;", "for-of statement may not have initializer");

    parseError("for(const {x}=a of b) c;", "for-of statement may not have initializer");
    parseError("for(const {x: y}=a of b) c;", "for-of statement may not have initializer");
    parseError("for(const [x, y]=a of b) c;", "for-of statement may not have initializer");
    parseError("for(const [x, ...y]=a of b) c;", "for-of statement may not have initializer");
  }

  @Test
  public void testImport() {
    expectFeatures(Feature.MODULES);
    strictMode = SLOPPY;

    parse("import 'someModule'");
    parse("import d from './someModule'");
    parse("import {} from './someModule'");
    parse("import {x, y} from './someModule'");
    parse("import {x as x1, y as y1} from './someModule'");
    parse("import {x as x1, y as y1, } from './someModule'");
    parse("import {default as d, class as c} from './someModule'");
    parse("import d, {x as x1, y as y1} from './someModule'");
    parse("import * as sm from './someModule'");

    expectFeatures();
    parseError("import class from './someModule'", "cannot use keyword 'class' here.");
    parseError("import * as class from './someModule'", "'identifier' expected");
    parseError("import {a as class} from './someModule'", "'identifier' expected");
    parseError("import {class} from './someModule'", "'as' expected");
  }

  @Test
  public void testExport() {
    strictMode = SLOPPY;

    expectFeatures(Feature.MODULES);
    parse("export const x = 1");
    parse("export var x = 1");
    parse("export function f() {}");
    parse("export class c {}");
    parse("export {x, y}");
    parse("export {x as x1}");
    parse("export {x as x1, y as x2}");
    parse("export {x as default, y as class}");

    expectFeatures();
    parseError("export {default as x}", "cannot use keyword 'default' here.");
    parseError("export {package as x}", "cannot use keyword 'package' here.");
    parseError("export {package}", "cannot use keyword 'package' here.");

    expectFeatures(Feature.MODULES);
    parse("export {x as x1, y as y1} from './someModule'");
    parse("export {x as x1, y as y1, } from './someModule'");
    parse("export {default as d} from './someModule'");
    parse("export {d as default, c as class} from './someModule'");
    parse("export {default as default, class as class} from './someModule'");
    parse("export {class} from './someModule'");
    parse("export * from './someModule'");

    expectFeatures();
    parseError("export * as s from './someModule';", "'from' expected");
  }

  @Test
  public void testExportAsync() {
    strictMode = SLOPPY;

    expectFeatures(Feature.MODULES, Feature.ASYNC_FUNCTIONS);
    parse("export async function f() {}");
  }

  @Test
  public void testImportExportTypescriptKeyword() {
    parse("export { namespace };");
    parse("import { namespace } from './input0.js';");
  }

  @Test
  public void testGoogModule() {
    Node tree = parse("goog.module('example');");
    assertNode(tree).hasType(Token.SCRIPT);
    assertThat(tree.getStaticSourceFile()).isNotNull();
    assertNode(tree.getFirstChild()).hasType(Token.MODULE_BODY);
    assertThat(tree.getFirstChild().getStaticSourceFile()).isNotNull();
  }

  @Test
  public void testShebang() {
    parse("#!/usr/bin/node\n var x = 1;");
    parseError("var x = 1; \n #!/usr/bin/node", "Shebang comment must be at the start of the file");
  }

  @Test
  public void testInvalidPoundUsage() {
    parseError("var x = 1; \n# Wrong-style comment", "Invalid usage of #");
  }

  @Test
  public void testLookaheadGithubIssue699() {
    long start = System.currentTimeMillis();
    parse(
        """
        [1,[1,[1,[1,[1,[1,
        [1,[1,[1,[1,[1,[1,
        [1,[1,[1,[1,[1,[1,
        [1,[1,[1,[1,[1,[1,
        [1,[1,[1,[1,[1,[1,
        [1,[1,
        [1]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]
        """);

    long stop = System.currentTimeMillis();

    assertWithMessage("runtime").that(stop - start).isLessThan(5000L);
  }

  @Test
  public void testInvalidHandling1() {
    parse(
        """
        /**
         * @fileoverview Definition.
         * @mods {ns.bar}
         * @modName mod
         *
         * @extends {ns.bar}
         * @author someone
         */

        goog.provide('ns.foo');
        """);
  }

  @Test
  public void testUtf8() {
    mode = LanguageMode.ECMASCRIPT5;
    strictMode = SLOPPY;
    Node n = parse("\uFEFFfunction f() {}\n");
    Node fn = n.getFirstChild();
    assertNode(fn).hasType(Token.FUNCTION);
  }

  @Test
  public void testParseDeep1() {
    String code = "var x; x = \n";
    for (int i = 1; i < 15000; i++) {
      code += "  \'" + i + "\' +\n";
    }
    code += "\'end\';n";
    parse(code);
  }

  @Test
  public void testParseDeep2() {
    String code = "var x; x = \n";
    for (int i = 1; i < 15000; i++) {
      code += "  \'" + i + "\' +\n";
    }
    code += "\'end\'; /** a comment */\n";
    parse(code);
  }

  @Test
  public void testParseDeep3() {
    String code = "var x; x = \n";
    for (int i = 1; i < 15000; i++) {
      code += "  \'" + i + "\' +\n";
    }
    code += "  /** @type {string} */ (x);\n";
    parse(code);
  }

  @Test
  public void testParseDeep4() {
    // Currently, we back off if there is any JSDoc in the tree of binary expressions
    String code = "var x; x = \n";
    for (int i = 1; i < 15000; i++) {
      if (i == 5) {
        code += "  /** @type {string} */ (x) +\n";
      }
      code += "  \'" + i + "\' +\n";
    }
    code += "\'end\';n";
    try {
      parse(code);
      throw new AssertionError();
    } catch (RuntimeException e) {
      // expected exception
      assertThat(e).hasMessageThat().contains("Exception parsing");
    }
  }

  @Test
  public void testParseInlineSourceMap() {
    String code =
"""
var X = (function () {
    function X(input) {
        this.y = input;
    }
    return X;
}());
console.log(new X(1));
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiZm9vLmpzIiwic291cmNlUm9vdCI6IiIsInNvdXJjZXMiOlsiZm9vLnRzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiJBQUFBO0lBR0UsV0FBWSxLQUFhO1FBQ3ZCLElBQUksQ0FBQyxDQUFDLEdBQUcsS0FBSyxDQUFDO0lBQ2pCLENBQUM7SUFDSCxRQUFDO0FBQUQsQ0FBQyxBQU5ELElBTUM7QUFFRCxPQUFPLENBQUMsR0FBRyxDQUFDLElBQUksQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMifQ==
""";
    ParseResult result = doParse(code);
    assertThat(result.sourceMapURL)
        .isEqualTo(
            """
            data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiZm9vLmpz\
            Iiwic291cmNlUm9vdCI6IiIsInNvdXJjZXMiOlsiZm9vLnRzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiJBQU\
            FBO0lBR0UsV0FBWSxLQUFhO1FBQ3ZCLElBQUksQ0FBQyxDQUFDLEdBQUcsS0FBSyxDQUFDO0lBQ2pCLENBQUM7\
            SUFDSCxRQUFDO0FBQUQsQ0FBQyxBQU5ELElBTUM7QUFFRCxPQUFPLENBQUMsR0FBRyxDQUFDLElBQUksQ0FBQy\
            xDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMifQ==\
            """);
  }

  @Test
  public void testParseSourceMapRelativeURL() {
    String code =
        """
        var X = (function () {
            function X(input) {
                this.y = input;
            }
            return X;
        }());
        console.log(new X(1));
        //# sourceMappingURL=somefile.js.map
        """;
    ParseResult result = doParse(code);
    assertThat(result.sourceMapURL).isEqualTo("somefile.js.map");
  }

  /**
   * In the future, we may want absolute URLs to be mapable based on how the server exposes the
   * sources. See: b/62544959.
   */
  @Test
  public void testParseSourceMapAbsoluteURL() {
    String code =
        """
        console.log('asdf');
        //# sourceMappingURL=/some/absolute/path/to/somefile.js.map
        """;
    ParseResult result = doParse(code);
    assertThat(result.sourceMapURL).isEqualTo("/some/absolute/path/to/somefile.js.map");
  }

  /**
   * In the future, we may want absolute URLs to me mapable based on how the server exposes the
   * sources. See: b/62544959.
   */
  @Test
  public void testParseSourceMapAbsoluteURLHTTP() {
    String code =
        """
        console.log('asdf');
        //# sourceMappingURL=http://google.com/some/absolute/path/to/somefile.js.map
        """;
    ParseResult result = doParse(code);
    assertThat(result.sourceMapURL)
        .isEqualTo("http://google.com/some/absolute/path/to/somefile.js.map");
  }

  @Test
  public void testIncorrectAssignmentDoesntCrash() {
    // Check that error make sense in default "stop on error" mode.
    parseError("[1 + 2] = 3;", "invalid assignment target");

    // Ensure that in IDE mode parser doesn't crash. It produces much more errors but it's
    // "ignore errors" mode so it's ok.
    isIdeMode = true;
    parseError(
        "[1 + 2] = 3;",
        "invalid assignment target",
        "']' expected",
        "invalid assignment target",
        "Semi-colon expected",
        "Semi-colon expected",
        "primary expression expected",
        "invalid assignment target",
        "Semi-colon expected",
        "primary expression expected",
        "Semi-colon expected");
  }

  @Test
  public void testDynamicImport() {
    ImmutableList<String> dynamicImportUses =
        ImmutableList.of(
            "import('foo')",
            "import('foo').then(function(a) { return a; })",
            "var moduleNamespace = import('foo')",
            "Promise.all([import('foo')]).then(function(a) { return a; })",
            "function foo() { foo(); import('foo'); } foo();");
    expectFeatures(Feature.DYNAMIC_IMPORT);

    for (LanguageMode m : LanguageMode.values()) {
      mode = m;
      strictMode = (m == LanguageMode.ECMASCRIPT3) ? SLOPPY : STRICT;
      if (m.featureSet.has(Feature.DYNAMIC_IMPORT)) {
        for (String importUseSource : dynamicImportUses) {
          parse(importUseSource);
        }
      } else {
        for (String importUseSource : dynamicImportUses) {
          parseWarning(importUseSource, requiresLanguageModeMessage(Feature.DYNAMIC_IMPORT));
        }
      }
    }

    mode = LanguageMode.ECMASCRIPT_2020;
    parseError("function foo() { import bar from './someModule'; }", "'(' expected");
  }

  @Test
  public void testAwaitDynamicImport() {
    ImmutableList<String> awaitDynamicImportUses =
        ImmutableList.of(
            "(async function() { return await import('foo'); })()",
            "(async function() { await import('foo').then(function(a) { return a; }); })()",
            "(async function() { var moduleNamespace = await import('foo'); })()",
            """
            (async function() {
            await Promise.all([import('foo')]).then(function(a) { return a; }); })()
            """);
    expectFeatures(Feature.DYNAMIC_IMPORT, Feature.ASYNC_FUNCTIONS);

    for (String importUseSource : awaitDynamicImportUses) {
      parse(importUseSource);
    }
  }

  @Test
  public void testImportMeta() {
    expectFeatures(Feature.MODULES, Feature.IMPORT_META);

    Node tree = parse("import.meta");
    assertNode(tree.getFirstFirstChild()).isEqualTo(IR.exprResult(IR.importMeta()));
  }

  @Test
  public void testImportMeta_es5() {
    mode = LanguageMode.ECMASCRIPT5;
    expectFeatures(Feature.MODULES, Feature.IMPORT_META);

    parseWarning(
        "import.meta",
        requiresLanguageModeMessage(Feature.MODULES),
        requiresLanguageModeMessage(Feature.IMPORT_META));
  }

  @Test
  public void testImportMeta_es6() {
    mode = LanguageMode.ECMASCRIPT_2015;
    expectFeatures(Feature.MODULES, Feature.IMPORT_META);

    parseWarning("import.meta", requiresLanguageModeMessage(Feature.IMPORT_META));
  }

  @Test
  public void testImportMeta_inExpression() {
    expectFeatures(Feature.MODULES, Feature.IMPORT_META);

    Node propTree = parse("import.meta.url");
    assertNode(propTree.getFirstFirstChild())
        .isEqualTo(IR.exprResult(IR.getprop(IR.importMeta(), "url")));

    Node callTree = parse("f(import.meta.url)");
    assertNode(callTree.getFirstFirstChild())
        .isEqualTo(
            IR.exprResult(freeCall(IR.call(IR.name("f"), IR.getprop(IR.importMeta(), "url")))));
  }

  private Node freeCall(Node n) {
    n.putBooleanProp(Node.FREE_CALL, true);
    return n;
  }

  @Test
  public void testImportMeta_asDotProperty() {
    Node tree = parse("x.import.meta");
    assertNode(tree.getFirstChild())
        .isEqualTo(IR.exprResult(IR.getprop(IR.name("x"), "import", "meta")));
  }

  @Test
  public void testNullishCoalesce() {
    expectFeatures(Feature.NULL_COALESCE_OP);

    Node tree = parse("x??y");
    assertNode(tree.getFirstChild())
        .isEqualTo(IR.exprResult(IR.coalesce(IR.name("x"), IR.name("y"))));
  }

  @Test
  public void testNullishCoalesce_es2019() {
    mode = LanguageMode.ECMASCRIPT_2019;
    expectFeatures(Feature.NULL_COALESCE_OP);

    parseWarning("x??y", requiresLanguageModeMessage(Feature.NULL_COALESCE_OP));
  }

  @Test
  public void testNullishCoalesce_withLogicalAND_shouldFail() {
    parseError("x&&y??z", "Logical OR and logical AND require parentheses when used with '??'");
  }

  @Test
  public void testNullishCoalesce_withLogicalOR_shouldFail() {
    parseError("x??y||z", "Logical OR and logical AND require parentheses when used with '??'");
  }

  @Test
  public void testNullishCoalesce_withLogicalANDinParens() {
    expectFeatures(Feature.NULL_COALESCE_OP);

    Node tree = parse("(x&&y)??z");
    assertNode(tree.getFirstChild())
        .isEqualTo(IR.exprResult(IR.coalesce(IR.and(IR.name("x"), IR.name("y")), IR.name("z"))));
  }

  @Test
  public void testNullishCoalesce_chaining() {
    expectFeatures(Feature.NULL_COALESCE_OP);

    Node tree = parse("x??y??z");
    Node expr = tree.getFirstChild();
    Node coalesce = expr.getFirstChild();

    assertNode(expr)
        .isEqualTo(
            IR.exprResult(IR.coalesce(IR.coalesce(IR.name("x"), IR.name("y")), IR.name("z"))));
    assertNode(expr).hasLineno(1).hasCharno(0).hasLength(7);
    assertNode(coalesce).hasType(Token.COALESCE);
    assertNode(coalesce).hasLineno(1).hasCharno(0).hasLength(7);
    assertNode(coalesce.getFirstChild()).hasType(Token.COALESCE);
    assertNode(coalesce.getFirstChild()).hasLineno(1).hasCharno(0).hasLength(4);
  }

  @Test
  public void testAssignOR() {
    expectFeatures(Feature.LOGICAL_ASSIGNMENT);
    Node tree = parse("x||=y");
    assertNode(tree.getFirstChild())
        .isEqualTo(IR.exprResult(IR.assignOr(IR.name("x"), IR.name("y"))));
  }

  @Test
  public void testAssignOr_es2020() {
    mode = LanguageMode.ECMASCRIPT_2020;
    expectFeatures(Feature.LOGICAL_ASSIGNMENT);

    parseWarning("x||=y", requiresLanguageModeMessage(Feature.LOGICAL_ASSIGNMENT));
  }

  @Test
  public void testAssignAnd() {
    expectFeatures(Feature.LOGICAL_ASSIGNMENT);
    Node tree = parse("x&&=y");
    assertNode(tree.getFirstChild())
        .isEqualTo(IR.exprResult(IR.assignAnd(IR.name("x"), IR.name("y"))));
  }

  @Test
  public void testAssignAnd_es2020() {
    mode = LanguageMode.ECMASCRIPT_2020;
    expectFeatures(Feature.LOGICAL_ASSIGNMENT);

    parseWarning("x&&=y", requiresLanguageModeMessage(Feature.LOGICAL_ASSIGNMENT));
  }

  @Test
  public void testAssignCoalesce() {
    expectFeatures(Feature.LOGICAL_ASSIGNMENT);
    Node tree = parse("x??=y");
    assertNode(tree.getFirstChild())
        .isEqualTo(IR.exprResult(IR.assignCoalesce(IR.name("x"), IR.name("y"))));
  }

  @Test
  public void testAssignCoalesce_es2020() {
    mode = LanguageMode.ECMASCRIPT_2020;
    expectFeatures(Feature.LOGICAL_ASSIGNMENT);

    parseWarning("x??=y", requiresLanguageModeMessage(Feature.LOGICAL_ASSIGNMENT));
  }

  @Test
  public void testNoDuplicateComments_arrow_fn() {
    isIdeMode = true;
    parsingMode = JsDocParsing.INCLUDE_ALL_COMMENTS;
    List<Comment> comments = parseComments("const a = (/** number */ n) => {}");

    assertThat(comments).hasSize(1);
    assertThat(comments.get(0).value).isEqualTo("/** number */");
  }

  @Test
  public void testIndirectCallName() {
    Node script = parse("(0, foo)();");
    assertNode(script).hasToken(Token.SCRIPT);
    Node exprResult = script.getFirstChild();
    assertNode(exprResult).hasToken(Token.EXPR_RESULT);

    // the `(0, foo)()` should have been converted to `foo()` with the
    // `FREE_CALL` property.
    Node call = exprResult.getFirstChild();
    assertNode(call).isFreeCall().hasFirstChildThat().isName("foo");
  }

  @Test
  public void testIndirectCallGetProp() {
    Node script = parse("(0, foo.bar)();");
    assertNode(script).hasToken(Token.SCRIPT);
    Node exprResult = script.getFirstChild();
    assertNode(exprResult).hasToken(Token.EXPR_RESULT);

    // the `(0, foo)()` should have been converted to `foo.bar()` with the
    // `FREE_CALL` property, so it will actually get printed as `(0, foo.bar)()`.
    Node call = exprResult.getFirstChild();
    assertNode(call).isFreeCall().hasFirstChildThat().matchesQualifiedName("foo.bar");
  }

  @Test
  public void testFreeCall1() {
    Node script = parse("foo();");
    assertNode(script).hasToken(Token.SCRIPT);
    Node firstExpr = script.getFirstChild();
    Node call = firstExpr.getFirstChild();
    assertNode(call).hasToken(Token.CALL);

    assertNode(call).isFreeCall();
  }

  @Test
  public void testFreeCall2() {
    Node script = parse("x.foo();");
    assertNode(script).hasToken(Token.SCRIPT);
    Node firstExpr = script.getFirstChild();
    Node call = firstExpr.getFirstChild();
    assertNode(call).hasToken(Token.CALL);

    assertNode(call).isNotFreeCall();
  }

  @Test
  public void testTaggedTemplateFreeCall1() {
    Node script = parse("foo``;");
    assertNode(script).hasToken(Token.SCRIPT);
    Node firstExpr = script.getFirstChild();
    Node call = firstExpr.getFirstChild();
    assertNode(call).hasToken(Token.TAGGED_TEMPLATELIT);

    assertNode(call).isFreeCall();
  }

  @Test
  public void testTaggedTemplateFreeCall2() {
    Node script = parse("x.foo``;");
    assertNode(script).hasToken(Token.SCRIPT);
    Node firstExpr = script.getFirstChild();
    Node call = firstExpr.getFirstChild();
    assertNode(call).hasToken(Token.TAGGED_TEMPLATELIT);

    assertNode(call).isNotFreeCall();
  }

  @Test
  public void optionalFreeCall1() {
    Node script = parse("foo?.();");
    assertNode(script).hasToken(Token.SCRIPT);
    Node firstExpr = script.getFirstChild();
    Node call = firstExpr.getFirstChild();
    assertNode(call).hasToken(Token.OPTCHAIN_CALL);

    assertNode(call).isFreeCall();
  }

  @Test
  public void optChainFreeCall() {
    Node script = parse("x?.foo();");
    assertNode(script).hasToken(Token.SCRIPT);
    Node firstExpr = script.getFirstChild();
    Node call = firstExpr.getFirstChild();
    assertNode(call).hasToken(Token.OPTCHAIN_CALL);

    assertNode(call).isNotFreeCall();
  }

  private void assertNodeHasJSDocInfoWithJSType(Node node, JSType jsType) {
    JSDocInfo info = node.getJSDocInfo();
    assertWithMessage("Node has no JSDocInfo: %s", node).that(info).isNotNull();
    assertTypeEquals(jsType, info.getType());
  }

  private void assertNodeHasJSDocInfoWithNoJSType(Node node) {
    JSDocInfo info = node.getJSDocInfo();
    assertWithMessage("Node has no JSDocInfo: %s", node).that(info).isNotNull();
    JSTypeExpression type = info.getType();
    assertWithMessage("JSDoc unexpectedly has type").that(type).isNull();
  }

  private void assertNodeHasNoJSDocInfo(Node node) {
    JSDocInfo info = node.getJSDocInfo();
    assertWithMessage("Node %s has unexpected JSDocInfo %s", node, info).that(info).isNull();
  }

  private static String requiresLanguageModeMessage(Feature feature) {
    return IRFactory.languageFeatureWarningMessage(feature);
  }

  private static Node script(Node stmt) {
    return new Node(Token.SCRIPT, stmt);
  }

  private static Node expr(Node n) {
    return new Node(Token.EXPR_RESULT, n);
  }

  private static Node regex(String regex) {
    return new Node(Token.REGEXP, Node.newString(regex));
  }

  private static Node regex(String regex, String flag) {
    return new Node(Token.REGEXP, Node.newString(regex), Node.newString(flag));
  }

  /**
   * Verify that the given code has the given parse errors.
   *
   * @return If in IDE mode, returns a partial tree.
   */
  private Node parseError(String source, String... errors) {
    TestErrorReporter testErrorReporter = new TestErrorReporter().expectAllErrors(errors);
    ParseResult result =
        ParserRunner.parse(
            new SimpleSourceFile("input", SourceKind.STRONG),
            source,
            createConfig(),
            testErrorReporter);
    Node script = result.ast;

    // check expected features if specified
    assertFS(result.features).contains(expectedFeatures);

    // verifying that all errors were seen
    testErrorReporter.verifyHasEncounteredAllWarningsAndErrors();

    return script;
  }

  /**
   * Verify that the given code has the given parse warnings.
   *
   * @return The parse tree.
   */
  private Node parseWarning(String string, String... warnings) {
    return doParse(string, warnings).ast;
  }

  private ParseResult doParse(String string, String... warnings) {
    TestErrorReporter testErrorReporter = new TestErrorReporter().expectAllWarnings(warnings);
    StaticSourceFile file = new SimpleSourceFile("input", SourceKind.STRONG);
    ParseResult result = ParserRunner.parse(file, string, createConfig(), testErrorReporter);

    // check expected features if specified
    assertFS(result.features).contains(expectedFeatures);

    // verifying that all warnings were seen
    testErrorReporter.verifyHasEncounteredAllWarningsAndErrors();
    assertSourceInfoPresent(result.ast);
    return result;
  }

  private void assertSourceInfoPresent(Node node) {
    ArrayDeque<Node> deque = new ArrayDeque<>();
    deque.add(node);

    while (!deque.isEmpty()) {
      node = deque.remove();

      assertWithMessage("Source information must be present on %s", node)
          .that(node.getLineno())
          .isAtLeast(0);

      assertWithMessage("Source length must be nonnegative on %s", node)
          .that(node.getLength())
          .isAtLeast(0);

      for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
        deque.add(child);
      }
    }
  }

  /**
   * Verify that the given code has no parse warnings or errors.
   *
   * @return The parse tree.
   */
  private Node parse(String string) {
    return parseWarning(string);
  }

  /**
   * Return all comments recorded by the parser in IDE mode.
   *
   * <p>Assumes `isIdeMode` is true and an appropriate `parsingMode` is configured.
   */
  private List<Comment> parseComments(String string) {
    return doParse(string).comments;
  }

  private Config createConfig() {
    if (isIdeMode) {
      return ParserRunner.createConfig(
          mode, parsingMode, Config.RunMode.KEEP_GOING, null, true, strictMode);
    } else {
      return ParserRunner.createConfig(mode, null, strictMode);
    }
  }

  /** Sets expectedFeatures based on the list of features. */
  private void expectFeatures(Feature... features) {
    expectedFeatures = FeatureSet.BARE_MINIMUM.with(features);
  }

  private static class ParserResult {
    private final String code;
    private final Node node;

    private ParserResult(String code, Node node) {
      this.code = code;
      this.node = node;
    }
  }
}
