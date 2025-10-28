/*
 * Copyright 2021 The Closure Compiler Authors.
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

import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test cases for transpilation pass that replaces public class fields and class static blocks:
 * <code><pre>
 * class C {
 *   x = 2;
 *   ['y'] = 3;
 *   static a;
 *   static ['b'] = 'hi';
 *   static {
 *     let c = 4;
 *     this.z = c;
 *   }
 * }
 * </pre></code>
 */
@RunWith(JUnit4.class)
public final class RewriteClassMembersTest extends CompilerTestCase {

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    enableNormalize();
    enableTypeInfoValidation();
    enableTypeCheck();
    replaceTypesWithColors();
    enableMultistageCompilation();
    setGenericNameReplacements(Es6NormalizeClasses.GENERIC_NAME_REPLACEMENTS);
    setLanguageOut(LanguageMode.ECMASCRIPT_2021);
  }

  @Override
  protected CompilerPass getProcessor(Compiler compiler) {
    return new Es6NormalizeClasses(compiler);
  }

  private void testEs2022(String input, String expected) {
    setLanguageOut(LanguageMode.ECMASCRIPT_NEXT);
    test(input, expected);
    setLanguageOut(LanguageMode.ECMASCRIPT_2021);
  }

  @Test
  public void testClassStaticBlock() {
    test(
        """
        class C {
          static {
            let x = 2
            this.y = x
          }
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            {
              let x = 2;
              C.y = x;
            }
          }
        }
        C.STATIC_INIT$0();
        """); // uses `this` in static block

    test(
        """
        class B {
          static y = 3;
        }
        class C extends B {
          static {
            let x = super.y;
          }
        }
        """,
        """
        class B {
          static STATIC_INIT$0() {
            B.y = 3;
          }
        }
        B.STATIC_INIT$0();
        class C extends B {
          static STATIC_INIT$1() {
            {
              // TODO (tflo): Reflect.get(B, 'y') is the technically correct way.
              let x = B.y;
            }
          }
        }
        C.STATIC_INIT$1();
        """); // uses `super`

    test(
        """
        const ns = {};
        ns.B = class {
          static y = 3;
        };
        class C extends ns.B {
          static {
            let x = super.y;
          }
        }
        """,
        """
        const ns = {};
        ns.B = class {
          static STATIC_INIT$0() {
            ns.B.y = 3;
          }
        };
        ns.B.STATIC_INIT$0();
        class C extends ns.B {
          static STATIC_INIT$1() {
            {
              let x = ns.B.y;
            }
          }
        }
        C.STATIC_INIT$1();
        """); // uses `super`

    test(
        """
        class C {
          static {
            C.x = 2
            const y = this.x
          }
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            {
              C.x = 2;
              const y = C.x;
            }
          }
        }
        C.STATIC_INIT$0();
        """); // uses `this` in static block

    test(
        """
        var z = 1
        class C {
          static {
            let x = 2
            var z = 3;
          }
        }
        """,
        """
        var z = 1;
        class C {
          static STATIC_INIT$0() {
            {
              let x = 2;
              var z$jscomp$1 = 3;
            }
          }
        }
        C.STATIC_INIT$0();
        """); // `var` in static block

    test(
        """
        let C = class {
          static prop = 5;
        };
        let D = class extends C {
          static {
            this.prop = 10;
          }
        };
        """,
        """
        let C = class {
          static STATIC_INIT$0() {
            C.prop = 5;
          }
        };
        C.STATIC_INIT$0();
        let D = class extends C {
          static STATIC_INIT$1() {
            {
              D.prop = 10;
            }
          }
        };
        D.STATIC_INIT$1();
        """);

    test(
        """
        let C = class {
          static prop = 5;
        },
        D = class extends C {
          static {
            this.prop = 10;
          }
        }
        """,
        """
        let C = class {
          static STATIC_INIT$0() {
            C.prop = 5;
          }
        };
        C.STATIC_INIT$0();
        let D = class extends C {
          static STATIC_INIT$1() {
            {
              D.prop = 10;
            }
          }
        };
        D.STATIC_INIT$1();
        """); // defines classes in the same let statement

    test(
        """
        class C {
          static f;
          static {
            C.f = 1;
          }
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            C.f;
            {
              C.f = 1;
            }
          }
        }
        C.STATIC_INIT$0();
        """);
  }

  @Test
  public void testMultipleStaticBlocks() {
    test(
        """
        var z = 1
        /** @unrestricted */
        class C {
          static x = 2;
          static {
            z = z + this.x;
          }
          static [z] = 3;
          static w = 5;
          static {
            z = z + this.w;
          }
        }
        """,
        """
        var z = 1;
        var COMP_FIELD$0 = z;
        class C {
          static STATIC_INIT$1() {
            C.x = 2;
            {
              z = z + C.x;
            }
            C[COMP_FIELD$0] = 3;
            C.w = 5;
            {
              z = z + C.w;
            }
          }
        }
        C.STATIC_INIT$1();
        """);
  }

  @Test
  public void testThisInNonStaticPublicField() {
    var src =
        """
        class A {
          /** @suppress {partialAlias} */
          b = 'word';
          c = this.b;
        }
        """;
    test(
        src,
        """
        class A {
          constructor() {
            /** @suppress {partialAlias} */
            this.b = 'word';
            this.c = this.b;
          }
        }
        """);
    testEs2022(src, src);

    test(
        """
        let obj = { bar() { return 9; } };
        class D {
          e = obj;
          f = this.e.bar() * 4;
        }
        """,
        """
        let obj = { bar() { return 9; } };
        class D {
          constructor() {
            this.e = obj;
            this.f = this.e.bar() * 4;
          }
        }
        """);

    test(
        """
        class Foo {
          y = 'apple';
          x = () => { return this.y + ' and banana'; };
        }
        """,
        """
        class Foo {
          constructor() {
            this.y = 'apple';
            this.x = () => { return this.y + ' and banana'; };
          }
        }
        """);

    test(
        """
        class Bar {
          x = () => { this.method(); };
          method() {}
        }
        """,
        """
        class Bar {
          constructor() {
            this.x = () => { this.method(); };
          }
          method() {}
        }
        """);
  }

  @Test
  public void testSuperInNonStaticPublicField() {
    var src =
        """
        class Foo {
          x() {
            return 3;
          }
        }
        class Bar extends Foo {
          y = 1 + super.x();
        }
        """;
    test(
        src,
        """
        class Foo {
          x() {
            return 3;
          }
        }
        class Bar extends Foo {
          constructor() {
            super(...arguments);
            this.y = 1 + super.x();
          }
        }
        """);
    testEs2022(src, src);
  }

  @Test
  public void testThisInStaticField() {
    var src =
        """
        class C {
          static x = 2;
          static y = () => this.x;
        }
        """;
    test(
        src,
        """
        class C {
          static STATIC_INIT$0() {
            C.x = 2;
            C.y = () => {
              // Note: This is the correct behavior.
              return C.x;
            };
          }
        }
        C.STATIC_INIT$0();
        """);
    testEs2022(
        src,
        """
        class C {
          static x;
          static y;
          static STATIC_INIT$0() {
            C.x = 2;
            C.y = () => {
              return C.x;
            };
          }
        }
        C.STATIC_INIT$0();
        """);

    test(
        """
        class F {
          static a = 'there';
          static b = this.c() + this.a;
          static c() { return 'hi'; }
        }
        """,
        """
        class F {
          static c() {
            return "hi";
          }
          static STATIC_INIT$0() {
            F.a = "there";
            F.b = F.c() + F.a;
          }
        }
        F.STATIC_INIT$0();
        """);
  }

  @Test
  public void testThisInStaticGetter() {
    testSame(
        """
        let x = 1;
        class Child {
          static getX() {
            return x;
          }
          static get prop() {
            return this.getX();
          }
        }
        """);
  }

  @Test
  public void testThisInStaticSetter() {
    testSame(
        """
        let x = 1;
        class Child {
          static setX(newX) {
            x = newX;
          }
          static set prop(p) {
            this.setX(p);
          }
        }
        """);
  }

  @Test
  public void testSuperInStaticField() {
    test(
        """
        class Foo {
          static x(a, b) {
            return a + b;
          }
          static y(c, d) {
            return c - d;
          }
        }
        class Bar extends Foo {
          static z = () => super.x(1, 2) + 12 + super.y(3, 4);
        }
        """,
        """
        class Foo {
          static x(a, b) {
            return a + b;
          }
          static y(c, d) {
            return c - d;
          }
        }
        class Bar extends Foo {
          static STATIC_INIT$0() {
            Bar.z = () => {
              return Foo.x(1, 2) + 12 + Foo.y(3, 4);
            };
          }
        }
        Bar.STATIC_INIT$0();
        """);

    test(
        """
        const ns = {};
        ns.Foo = class {
          static x(a, b) {
            return 5;
          }
        }
        class Bar extends ns.Foo {
          static z = () => super.x(1, 2);
        }
        """,
        """
        const ns = {};
        ns.Foo = class {
          static x(a, b) {
            return 5;
          }
        };
        class Bar extends ns.Foo {
          static STATIC_INIT$0() {
            Bar.z = () => {
              return ns.Foo.x(1, 2);
            };
          }
        }
        Bar.STATIC_INIT$0();
        """);

    test(
        """
        class Bar {
          static a = { method1() {} };
          static b = { method2() { super.method1(); } };
        }
        """,
        """
        class Bar {
          static STATIC_INIT$0() {
            Bar.a = {method1() {
            }};
            Bar.b = {method2() {
              super.method1();
            }};
          }
        }
        Bar.STATIC_INIT$0();
        """);

    test(
        """
        class Parent {
          static getName() {
            return 'Parent';
          }
          static get greeting() {
            return 'Hello ' + this.getName();
          }
        }
        class Child extends Parent {
          static getName() {
            return 'Child';
          }
          static msg = super.greeting;  // 'Hello Child'
        }
        """,
        """
        class Parent {
          static getName() {
            return "Parent";
          }
          static get greeting() {
            return "Hello " + this.getName();
          }
        }
        class Child extends Parent {
          static getName() {
            return "Child";
          }
          static STATIC_INIT$0() {
            Child.msg = Parent.greeting;  // b/454921132 Incorrectly: 'Hello Parent'
          }
        }
        Child.STATIC_INIT$0();
        """);
  }

  @Test
  public void testSuperInStaticFieldObjectSpread() {
    test(
        """
        class Base {
          static X = {a: 1};
        }

        class Child extends Base {
          /** @type {!Object} */
          static Y = {...super.X, b: 2};
        }
        """,
        """
        class Base {
          static STATIC_INIT$0() {
            Base.X = {a:1};
          }
        }
        Base.STATIC_INIT$0();
        class Child extends Base {
          static STATIC_INIT$1() {
            Child.Y = {...Base.X, b:2};
          }
        }
        Child.STATIC_INIT$1();
        """);

    test(
        """
        const ns = {};
        ns.Base = class {
          static X = {a: 1};
        };

        class Child extends ns.Base {
          /** @type {!Object} */
          static Y = {...super.X, b: 2};
        }
        """,
        """
        const ns = {};
        ns.Base = class {
          static STATIC_INIT$0() {
            ns.Base.X = {a:1};
          }
        };
        ns.Base.STATIC_INIT$0();
        class Child extends ns.Base {
          static STATIC_INIT$1() {
            Child.Y = {...ns.Base.X, b:2};
          }
        }
        Child.STATIC_INIT$1();
        """);
  }

  @Test
  public void testSuperInStaticBlock() {
    test(
        """
        class Parent {
          static getName() {
            return 'Parent';
          }
          static getGreeting() {
            return 'Hello ' + this.getName();
          }
        }
        class Child extends Parent {
          static getName() {
            return 'Child';
          }
          static {
            alert(super.getGreeting());  // Alerts: 'Hello Child'
          }
        }
        """,
        """
        class Parent {
          static getName() {
            return "Parent";
          }
          static getGreeting() {
            return "Hello " + this.getName();
          }
        }
        class Child extends Parent {
          static getName() {
            return "Child";
          }
          static STATIC_INIT$0() {
            {
              alert(Parent.getGreeting());  // Alerts: 'Hello Parent'
            }
          }
        }
        Child.STATIC_INIT$0();
        """);
  }

  @Test
  public void testSuperInStaticBlock_strictSuperRewrite() {
    String source =
        """
        class Parent {
          static getName() {
            return 'Parent';
          }
          static getGreeting() {
            return 'Hello ' + this.getName();
          }
        }
        class Child extends Parent {
          static getName() {
            return 'Child';
          }
          static {
            alert(super.getGreeting());  // Alerts: 'Hello Child'
          }
        }
        """;
    String expected =
        """
        class Parent {
          static getName() {
            return "Parent";
          }
          static getGreeting() {
            return "Hello " + this.getName();
          }
        }
        class Child extends Parent {
          static getName() {
            return "Child";
          }
          /** @nocollapse */
          static STATIC_INIT$0() {
            {
              alert(Parent.getGreeting.call(this));  // Alerts: 'Hello Child'
            }
          }
        }
        Child.STATIC_INIT$0();
        """;

    // Test both conditions that trigger strict super rewrite.

    setAssumeStaticInheritanceIsNotUsed(false);
    test(source, expected);

    setAssumeStaticInheritanceIsNotUsed(true);
    setLanguageOut(LanguageMode.ECMASCRIPT5);
    test(source, expected);
  }

  @Test
  public void testSuperInStaticMethod() {
    test(
        """
        class Parent {
          static getName() {
            return 'Parent';
          }
          static getGreeting() {
            return 'Hello ' + this.getName();
          }
        }
        class Child extends Parent {
          static getName() {
            return 'Child';
          }
          static sayHello() {
            alert(super.getGreeting());  // Alerts: 'Hello Child'
          }
        }
        Child.sayHello();
        """,
        """
        class Parent {
          static getName() {
            return "Parent";
          }
          static getGreeting() {
            return "Hello " + this.getName();
          }
        }
        class Child extends Parent {
          static getName() {
            return "Child";
          }
          static sayHello() {
            alert(Parent.getGreeting());  // Alerts: 'Hello Parent'
          }
        }
        Child.sayHello();
        """);
  }

  @Test
  public void testSuperInStaticMethod_strictSuperRewrite() {
    String source =
        """
        class Parent {
          static getName() {
            return 'Parent';
          }
          static getGreeting() {
            return 'Hello ' + this.getName();
          }
        }
        class Child extends Parent {
          static getName() {
            return 'Child';
          }
          static sayHello() {
            alert(super.getGreeting());  // Alerts: 'Hello Child'
          }
        }
        Child.sayHello();
        """;
    String expected =
        """
        class Parent {
          static getName() {
            return "Parent";
          }
          static getGreeting() {
            return "Hello " + this.getName();
          }
        }
        class Child extends Parent {
          static getName() {
            return "Child";
          }
          static sayHello() {
            alert(Parent.getGreeting.call(this));  // Alerts: 'Hello Child'
          }
        }
        Child.sayHello();
        """;

    // Test both conditions that trigger strict super rewrite.

    setAssumeStaticInheritanceIsNotUsed(false);
    test(source, expected);

    setAssumeStaticInheritanceIsNotUsed(true);
    setLanguageOut(LanguageMode.ECMASCRIPT5);
    test(source, expected);
  }

  @Test
  public void testSuperInComputedStaticMethod() {
    test(
        """
        class Parent {
          static getName() {
            return 'Parent';
          }
          static getGreeting() {
            return 'Hello ' + this.getName();
          }
        }
        /** @unrestricted */
        class Child extends Parent {
          static getName() {
            return 'Child';
          }
          static ['sayHello']() {
            alert(super.getGreeting());  // Alerts: 'Hello Child'
          }
        }
        Child['sayHello']();
        """,
        """
        class Parent {
          static getName() {
            return "Parent";
          }
          static getGreeting() {
            return "Hello " + this.getName();
          }
        }
        class Child extends Parent {
          static getName() {
            return "Child";
          }
          static ["sayHello"]() {
            alert(Parent.getGreeting());  // Alerts: 'Hello Parent'
          }
        }
        Child["sayHello"]();
        """);
  }

  @Test
  public void testSuperInStaticGetter() {
    test(
        """
        class Parent {
          static getVal() {
            return 1;
          }
        }
        class Child extends Parent {
          static get childProp() {
            return super.getVal();
          }
        }
        """,
        """
        class Parent {
          static getVal() {
            return 1;
          }
        }
        class Child extends Parent {
          static get childProp() {
            return Parent.getVal();
          }
        }
        """);
  }

  @Test
  public void testSuperInStaticSetter() {
    test(
        """
        class Parent {
          static getVal() {
            return 1;
          }
        }
        class Child extends Parent {
          static set childProp(x) {
            alert(super.getVal());
          }
        }
        """,
        """
        class Parent {
          static getVal() {
            return 1;
          }
        }
        class Child extends Parent {
          static set childProp(x) {
            alert(Parent.getVal());
          }
        }
        """);
  }

  @Test
  public void testComputedPropInNonStaticField() {
    var src =
        """
        /** @unrestricted */
        class C {
          [x+=1];
          [x+=2] = 3;
        }
        """;
    test(
        src,
        """
        var COMP_FIELD$0 = x = x + 1;
        var COMP_FIELD$1 = x = x + 2;
        class C {
          constructor() {
            this[COMP_FIELD$0];
            this[COMP_FIELD$1] = 3;
           }
        }
        """);
    testEs2022(
        src,
        """
        var COMP_FIELD$0 = x = x + 1;
        var COMP_FIELD$1 = x = x + 2;
        class C {
          [COMP_FIELD$0];
          [COMP_FIELD$1] = 3;
        }
        """);

    test(
        """
        /** @unrestricted */
        class C {
          [1] = 1;
          /** @suppress {partialAlias} */
          [2] = this[1];
        }
        """,
        """
        class C {
          constructor() {
            this[1] = 1;
            /** @suppress {partialAlias} */
            this[2] = this[1];
          }
        }
        """);

    test(
        """
        /** @unrestricted */
        let c = class C {
          static [1] = 2;
          [2] = C[1]
        }
        """,
        """
        let c = class {
          constructor() {
            this[2] = c[1];
          }
          static STATIC_INIT$0() {
            c[1] = 2;
          }
        };
        c.STATIC_INIT$0();
        """);

    test(
        """
        foo(/** @unrestricted */ class C {
          static [1] = 2;
          [2] = C[1]
        })
        """,
        """
        const CLASS_DECL$0 = class {
          constructor() {
            this[2] = CLASS_DECL$0[1];
          }
          static STATIC_INIT$1() {
            CLASS_DECL$0[1] = 2;
          }
        };
        CLASS_DECL$0.STATIC_INIT$1();
        foo(CLASS_DECL$0);
        """);

    test(
        """
        let c = class {
          x = 1
          y = this.x
        }
        /** @unrestricted */
        class B {
          [1] = 2;
          [2] = this[1]
        }
        """,
        """
        let c = class {
          constructor() {
            this.x = 1;
            this.y = this.x;
          }
        };
        class B {
          constructor() {
            this[1] = 2;
            this[2] = this[1];
          }
        }
        """);

    testSame(
        """
        class Clazz {
          [Symbol.toPrimitive]() {
            return 42;
          }
        }
        """);
  }

  @Test
  public void testComputedPropInStaticField() {
    var src =
        """
        /** @unrestricted */
        class C {
          static ['x'];
          static ['y'] = 2;
        }
        """;
    test(
        src,
        """
        class C {
          static STATIC_INIT$0() {
            C["x"];
            C["y"] = 2;
          }
        }
        C.STATIC_INIT$0();
        """);
    testEs2022(
        src,
        """
        class C {
          static ["x"];
          static ["y"];
          static STATIC_INIT$0() {
            C["y"] = 2;
          }
        }
        C.STATIC_INIT$0();
        """);

    test(
        """
        /** @unrestricted */
        class C {
          static [1] = 1;
          static [2] = this[1];
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            C[1] = 1;
            C[2] = C[1];
          }
        }
        C.STATIC_INIT$0();
        """);

    test(
        """
        /** @unrestricted */
        const C = class {
          static [1] = 1;
          static [2] = this[1];
        }
        """,
        """
        const C = class {
          static STATIC_INIT$0() {
            C[1] = 1;
            C[2] = C[1];
          }
        };
        C.STATIC_INIT$0();
        """);

    test(
        """
        /** @unrestricted */
        const C = class InnerC {
          static [1] = 1;
          static [2] = this[1];
          static [3] = InnerC[2];
        }
        """,
        """
        const C = class {
          static STATIC_INIT$0() {
            C[1] = 1;
            C[2] = C[1];
            C[3] = C[2];
          }
        };
        C.STATIC_INIT$0();
        """);

    test(
        """
        /** @unrestricted */
        let c = class C {
          static [1] = 2;
          static [2] = C[1]
        }
        """,
        """
        let c = class {
          static STATIC_INIT$0() {
            c[1] = 2;
            c[2] = c[1];
          }
        };
        c.STATIC_INIT$0();
        """);

    test(
        """
        foo(/** @unrestricted */ class C {
          static [1] = 2;
          static [2] = C[1]
        })
        """,
        """
        const CLASS_DECL$0 = class {
          static STATIC_INIT$1() {
            CLASS_DECL$0[1] = 2;
            CLASS_DECL$0[2] = CLASS_DECL$0[1];
          }
        };
        CLASS_DECL$0.STATIC_INIT$1();
        foo(CLASS_DECL$0);
        """);

    test(
        """
        foo(/** @unrestricted */ class {
          static [1] = 1
        })
        """,
        """
        const CLASS_DECL$0 = class {
          static STATIC_INIT$1() {
            CLASS_DECL$0[1] = 1;
          }
        };
        CLASS_DECL$0.STATIC_INIT$1();
        foo(CLASS_DECL$0);
        """);

    testSame(
        """
        class Clazz {
          static [Symbol.hasInstance](x) {
            return false;
          }
        }
        """);
  }

  @Test
  public void testSideEffectsInComputedField() {
    test(
        """
        function bar() {
          this.x = 3;
          /** @unrestricted */
          class Foo {
            y;
            [this.x] = 2;
          }
        }
        """,
        """
        function bar() {
          this.x = 3;
          var COMP_FIELD$0 = this.x;
          class Foo {
            constructor() {
              this.y;
              this[COMP_FIELD$0] = 2;
            }
          }
        }
        """);

    test(
        """
        class E {
          y() { return 1; }
        }
        class F extends E {
          x() {
            return /** @unrestricted */ class {
              [super.y()] = 4;
            }
          }
        }
        """,
        """
        class E {
          y() {
            return 1;
          }
        }
        class F extends E {
          x() {
            var COMP_FIELD$1 = super.y();
            const CLASS_DECL$0 = class {
              constructor() {
                this[COMP_FIELD$1] = 4;
              }
            };
            return CLASS_DECL$0;
          }
        }
        """);

    test(
        """
        function bar(num) {}
        /** @unrestricted */
        class Foo {
          [bar(1)] = 'a';
          static b = bar(3);
          static [bar(2)] = bar(4);
        }
        """,
        """
        function bar(num) {
        }
        var COMP_FIELD$0 = bar(1);
        var COMP_FIELD$1 = bar(2);
        class Foo {
          constructor() {
            this[COMP_FIELD$0] = "a";
          }
          static STATIC_INIT$2() {
            Foo.b = bar(3);
            Foo[COMP_FIELD$1] = bar(4);
          }
        }
        Foo.STATIC_INIT$2();
        """);

    test(
        """
        let x = 'hello';
        /** @unrestricted */ class Foo {
          static n = (x=5);
          static [x] = 'world';
        }
        """,
        """
        let x = "hello";
        var COMP_FIELD$0 = x;
        class Foo {
          static STATIC_INIT$1() {
            Foo.n = x = 5;
            Foo[COMP_FIELD$0] = "world";
          }
        }
        Foo.STATIC_INIT$1();
        """);

    test(
        """
        function foo(num) {}
        /** @unrestricted */
        class Baz {
          ['f' + foo(1)];
          static x = foo(6);
          ['m' + foo(2)]() {};
          static [foo(3)] = foo(7);
          [foo(4)] = 2;
          get [foo(5)]() {}
        }
        """,
        """
        function foo(num) {
        }
        var COMP_FIELD$0 = "f" + foo(1);
        var COMP_FIELD$1 = "m" + foo(2);
        var COMP_FIELD$2 = foo(3);
        var COMP_FIELD$3 = foo(4);
        var COMP_FIELD$4 = foo(5);
        class Baz {
          constructor() {
            this[COMP_FIELD$0];
            this[COMP_FIELD$3] = 2;
          }
          [COMP_FIELD$1]() {
          }
          get [COMP_FIELD$4]() {
          }
          static STATIC_INIT$5() {
            Baz.x = foo(6);
            Baz[COMP_FIELD$2] = foo(7);
          }
        }
        Baz.STATIC_INIT$5();
        """);
  }

  @Test
  public void testClassStaticBlocksNoFieldAssign() {
    test(
        """
        class C {
          static {
          }
        }
        """,
        """
        class C {
        }
        """);

    test(
        """
        class C {
          static {
            let x = 2
            const y = x
          }
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            {
              let x = 2;
              const y = x;
            }
          }
        }
        C.STATIC_INIT$0();
        """);

    test(
        """
        class C {
          static {
            let x = 2
            const y = x
            let z;
            if (x - y == 0) {z = 1} else {z = 2}
            while (x - z > 10) {z++;}
            for (;;) {break;}
          }
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            {
              let x = 2;
              const y = x;
              let z;
              if (x - y == 0) {
                z = 1;
              } else {
                z = 2;
              }
              for (; x - z > 10;) {
                z++;
              }
              for (;;) {
                break;
              }
            }
          }
        }
        C.STATIC_INIT$0();
        """);

    test(
        """
        class C {
          static {
            let x = 2
          }
          static {
            const y = x
          }
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            {
              let x = 2;
            }
            {
              const y = x;
            }
          }
        }
        C.STATIC_INIT$0();
        """);

    test(
        """
        class C {
          static {
            let x = 2
          }
          static {
            const y = x
          }
        }
        class D {
          static {
            let z = 1
          }
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            {
              let x = 2;
            }
            {
              const y = x;
            }
          }
        }
        C.STATIC_INIT$0();
        class D {
          static STATIC_INIT$1() {
            {
              let z = 1;
            }
          }
        }
        D.STATIC_INIT$1();
        """);

    test(
        """
        class C {
          static {
            let x = function () {return 1;}
            const y = () => {return 2;}
            function a() {return 3;}
            let z = (() => {return 4;})();
          }
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            {
              function a() {
                return 3;
              }
              let x = function() {
                return 1;
              };
              const y = () => {
                return 2;
              };
              let z = (() => {
                return 4;
              })();
            }
          }
        }
        C.STATIC_INIT$0();
        """);

    test(
        """
        class C {
          static {
            C.x = 2
            const y = C.x;
          }
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            {
              C.x = 2;
              const y = C.x;
            }
          }
        }
        C.STATIC_INIT$0();
        """);

    test(
        """
        class Foo {
          static {
            let x = 5;
            class Bar {
              static {
                let x = 'str';
              }
            }
          }
        }
        """,
        """
        class Foo {
          static STATIC_INIT$1() {
            {
              let x = 5;
              class Bar {
                static STATIC_INIT$0() {
                  {
                    let x$jscomp$1 = "str";
                  }
                }
              }
              Bar.STATIC_INIT$0();
            }
          }
        }
        Foo.STATIC_INIT$1();
        """);
  }

  @Test
  public void testStaticNoncomputed() {
    test(
        """
        class C {
          static x = 2
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            C.x = 2;
          }
        }
        C.STATIC_INIT$0();
        """);

    var src =
        """
        class C {
          static x;
        }
        """;
    test(
        src,
        """
        class C {
          static STATIC_INIT$0() {
            C.x;
          }
        }
        C.STATIC_INIT$0();
        """);
    testEs2022(src, src);

    src =
        """
        class C {
          static x = 2
          static y = 'hi'
          static z;
        }
        """;
    test(
        src,
        """
        class C {
          static STATIC_INIT$0() {
            C.x = 2;
            C.y = "hi";
            C.z;
          }
        }
        C.STATIC_INIT$0();
        """);
    testEs2022(
        src,
        """
        class C {
          static x;
          static y;
          static z;
          static STATIC_INIT$0() {
            C.x = 2;
            C.y = "hi";
          }
        }
        C.STATIC_INIT$0();
        """);

    test(
        """
        class C {
          static x = 2
          static y = 3
        }
        class D {
          static z = 1
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            C.x = 2;
            C.y = 3;
          }
        }
        C.STATIC_INIT$0();
        class D {
          static STATIC_INIT$1() {
            D.z = 1;
          }
        }
        D.STATIC_INIT$1();
        """);

    test(
        """
        class C {
          static w = function () {return 1;};
          static x = () => {return 2;};
          static y = (function a() {return 3;})();
          static z = (() => {return 4;})();
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            C.w = function() {
              return 1;
            };
            C.x = () => {
              return 2;
            };
            C.y = function a() {
              return 3;
            }();
            C.z = (() => {
              return 4;
            })();
          }
        }
        C.STATIC_INIT$0();
        """);

    test(
        """
        class C {
          static x = 2
          static y = C.x
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            C.x = 2;
            C.y = C.x;
          }
        }
        C.STATIC_INIT$0();
        """);

    test(
        """
        class C {
          static x = 2
          static {let y = C.x}
        }
        """,
        """
        class C {
          static STATIC_INIT$0() {
            C.x = 2;
            {
              let y = C.x;
            }
          }
        }
        C.STATIC_INIT$0();
        """);
  }

  @Test
  public void testInstanceNoncomputedWithNonemptyConstructor() {
    test(
        """
        class C extends Object {
          x = 1;
          z = 3;
          constructor() {
            super();
            this.y = 2;
          }
        }
        """,
        """
        class C extends Object{
          constructor() {
            super();
            this.x = 1
            this.z = 3
            this.y = 2;
          }
        }
        """);

    test(
        """
        class C {
          x;
          constructor() {
            this.y = 2;
          }
        }
        """,
        """
        class C {
          constructor() {
            this.x;
            this.y = 2;
          }
        }
        """);

    test(
        """
        class C {
          x = 1
          y = 2
          constructor() {
            this.z = 3;
          }
        }
        """,
        """
        class C {
          constructor() {
            this.x = 1;
            this.y = 2;
            this.z = 3;
          }
        }
        """);

    test(
        """
        class C {
          x = 1
          y = 2
          constructor() {
            alert(3);
            this.z = 4;
          }
        }
        """,
        """
        class C {
          constructor() {
            this.x = 1;
            this.y = 2;
            alert(3);
            this.z = 4;
          }
        }
        """);

    test(
        """
        class C {
          x = 1
          constructor() {
            alert(3);
            this.z = 4;
          }
          y = 2
        }
        """,
        """
        class C {
          constructor() {
            this.x = 1;
            this.y = 2;
            alert(3);
            this.z = 4;
          }
        }
        """);

    test(
        """
        class C {
          x = 1
          constructor() {
            alert(3);
            this.z = 4;
          }
          y = 2
        }
        class D {
          a = 5;
          constructor() { this.b = 6;}
        }
        """,
        """
        class C {
          constructor() {
            this.x = 1;
            this.y = 2;
            alert(3);
            this.z = 4;
          }
        }
        class D {
        constructor() {
          this.a = 5;
          this.b = 6
        }
        }
        """);
  }

  @Test
  public void testInstanceComputedWithNonemptyConstructorAndSuper() {
    var src =
        """
        class A { constructor() { alert(1); } }
        /** @unrestricted */ class C extends A {
          ['x'] = 1;
          constructor() {
            super();
            this['y'] = 2;
            this['z'] = 3;
          }
        }
        """;
    test(
        src,
        """
        class A { constructor() { alert(1); } }
        class C extends A {
          constructor() {
            super()
            this['x'] = 1
            this['y'] = 2;
            this['z'] = 3;
          }
        }
        """);
    testEs2022(
        src,
        """
        class A {
          constructor() {
            alert(1);
          }
        }
        class C extends A {
          ["x"] = 1;
          constructor() {
            super();
            this["y"] = 2;
            this["z"] = 3;
          }
        }
        """);
  }

  @Test
  public void testInstanceNoncomputedWithNonemptyConstructorAndSuper() {
    test(
        """
        class A { constructor() { alert(1); } }
        class C extends A {
          x = 1;
          constructor() {
            super()
            this.y = 2;
          }
        }
        """,
        """
        class A { constructor() { alert(1); } }
        class C extends A {
          constructor() {
            super()
            this.x = 1
            this.y = 2;
          }
        }
        """);

    test(
        """
        class A { constructor() { this.x = 1; } }
        class C extends A {
          y;
          constructor() {
            super()
            alert(3);
            this.z = 4;
          }
        }
        """,
        """
        class A { constructor() { this.x = 1; } }
        class C extends A {
          constructor() {
            super()
            this.y;
            alert(3);
            this.z = 4;
          }
        }
        """);

    test(
        """
        class A { constructor() { this.x = 1; } }
        class C extends A {
          y;
          constructor() {
            alert(3);
            super()
            this.z = 4;
          }
        }
        """,
        """
        class A { constructor() { this.x = 1; } }
        class C extends A {
          constructor() {
            alert(3);
            super()
            this.y;
            this.z = 4;
          }
        }
        """);
  }

  @Test
  public void testNonComputedInstanceWithEmptyConstructor() {
    test(
        """
        class C {
          x = 2;
          constructor() {}
        }
        """,
        """
        class C {
          constructor() {
            this.x = 2;
          }
        }
        """);

    test(
        """
        class C {
          x;
          constructor() {}
        }
        """,
        """
        class C {
          constructor() {
            this.x;
          }
        }
        """);

    test(
        """
        class C {
          x = 2
          y = 'hi'
          z;
          constructor() {}
        }
        """,
        """
        class C {
          constructor() {
            this.x = 2
            this.y = 'hi'
            this.z;
          }
        }
        """);

    test(
        """
        class C {
          x = 1
          constructor() {
          }
          y = 2
        }
        """,
        """
        class C {
          constructor() {
            this.x = 1;
            this.y = 2;
          }
        }
        """);

    test(
        """
        class C {
          x = 1
          constructor() {
          }
          y = 2
        }
        class D {
          a = 5;
          constructor() {}
        }
        """,
        """
        class C {
          constructor() {
            this.x = 1;
            this.y = 2;
          }
        }
        class D {
        constructor() {
          this.a = 5;
        }
        }
        """);

    test(
        """
        class C {
          w = function () {return 1;};
          x = () => {return 2;};
          y = (function a() {return 3;})();
          z = (() => {return 4;})();
          constructor() {}
        }
        """,
        """
        class C {
          constructor() {
            this.w = function () {return 1;};
            this.x = () => {return 2;};
            this.y = (function a() {return 3;})();
            this.z = (() => {return 4;})();
          }
        }
        """);

    test(
        """
        class C {
          static x = 2
          constructor() {}
          y = C.x
        }
        """,
        """
        class C {
          constructor() {
            this.y = C.x;
          }
          static STATIC_INIT$0() {
            C.x = 2;
          }
        }
        C.STATIC_INIT$0();
        """);
  }

  @Test
  public void testInstanceNoncomputedNoConstructor() {
    test(
        """
        class C {
          x = 2;
        }
        """,
        """
        class C {
          constructor() {this.x=2;}
        }
        """);

    test(
        """
        class C {
          x;
        }
        """,
        """
        class C {
          constructor() {this.x;}
        }
        """);

    test(
        """
        class C {
          x = 2
          y = 'hi'
          z;
        }
        """,
        """
        class C {
          constructor() {this.x=2; this.y='hi'; this.z;}
        }
        """);
    test(
        """
        class C {
          foo() {}
          x = 1;
        }
        """,
        """
        class C {
          constructor() {this.x = 1;}
          foo() {}
        }
        """);

    test(
        """
        class C {
          static x = 2
          y = C.x
        }
        """,
        """
        class C {
          constructor() {
            this.y = C.x;
          }
          static STATIC_INIT$0() {
            C.x = 2;
          }
        }
        C.STATIC_INIT$0();
        """);

    test(
        """
        class C {
          w = function () {return 1;};
          x = () => {return 2;};
          y = (function a() {return 3;})();
          z = (() => {return 4;})();
        }
        """,
        """
        class C {
          constructor() {
            this.w = function () {return 1;};
            this.x = () => {return 2;};
            this.y = (function a() {return 3;})();
            this.z = (() => {return 4;})();
          }
        }
        """);
  }

  @Test
  public void testInstanceNonComputedNoConstructorWithSuperclass() {
    test(
        """
        class B {}
        class C extends B {x = 1;}
        """,
        """
        class B {}
        class C extends B {
          constructor() {
            super(...arguments);
            this.x = 1;
          }
        }
        """);
    test(
        """
        class B {constructor() {}; y = 2;}
        class C extends B {x = 1;}
        """,
        """
        class B {constructor() {this.y = 2}}
        class C extends B {
          constructor() {
            super(...arguments);
            this.x = 1;
          }
        }
        """);
    test(
        """
        class B {constructor(a, b) {}; y = 2;}
        class C extends B {x = 1;}
        """,
        """
        class B {constructor(a, b) {this.y = 2}}
        class C extends B {
          constructor() {
            super(...arguments);
            this.x = 1;
          }
        }
        """);
  }

  @Test
  public void testClassExpressionsStaticBlocks() {
    test(
        """
        let c = class C {
          static {
            C.y = 2;
            let x = C.y
          }
        }
        """,
        """
        let c = class {
          static STATIC_INIT$0() {
            {
              c.y = 2;
              let x = c.y;
            }
          }
        };
        c.STATIC_INIT$0();
        """);

    test(
        """
        foo(class C {
          static {
            C.y = 2;
            let x = C.y
          }
        })
        """,
        """
        foo((() => {
          const CLASS_DECL$0 = class {
            static STATIC_INIT$1() {
              {
                CLASS_DECL$0.y = 2;
                let x = CLASS_DECL$0.y;
              }
            }
          };
          CLASS_DECL$0.STATIC_INIT$1();
          return CLASS_DECL$0;
        })());
        """);

    test(
        """
        class A { static b; }
        foo(A.b.c = class C {
          static {
            C.y = 2;
            let x = C.y
          }
        })
        """,
        """
        class A {
          static STATIC_INIT$0() {
            A.b;
          }
        }
        A.STATIC_INIT$0();
        foo(A.b.c = (() => {
          const CLASS_DECL$1 = class {
            static STATIC_INIT$2() {
              {
                CLASS_DECL$1.y = 2;
                let x = CLASS_DECL$1.y;
              }
            }
          };
          CLASS_DECL$1.STATIC_INIT$2();
          return CLASS_DECL$1;
        })());
        """);
  }

  @Test
  public void testNonClassDeclarationsStaticBlocks() {
    test(
        """
        let c = class {
          static {
            let x = 1
          }
        }
        """,
        """
        let c = class {
          static STATIC_INIT$0() {
            {
              let x = 1;
            }
          }
        };
        c.STATIC_INIT$0();
        """);

    test(
        """
        class A {}
        A.c = class {
          static {
            let x = 1
          }
        }
        """,
        """
        class A {
        }
        A.c = class {
          static STATIC_INIT$0() {
            {
              let x = 1;
            }
          }
        };
        A.c.STATIC_INIT$0();
        """);

    test(
        """
        class A {}
        A[1] = class {
          static {
            let x = 1
          }
        }
        """,
        """
        class A {
        }
        A[1] = (() => {
          const CLASS_DECL$0 = class {
            static STATIC_INIT$1() {
              {
                let x = 1;
              }
            }
          };
          CLASS_DECL$0.STATIC_INIT$1();
          return CLASS_DECL$0;
        })();
        """);
  }

  @Test
  public void testNonClassDeclarationsStaticNoncomputedFields() {
    test(
        """
        let c = class {
          static x = 1
        }
        """,
        """
        let c = class {
          static STATIC_INIT$0() {
            c.x = 1;
          }
        };
        c.STATIC_INIT$0();
        """);

    test(
        """
        class A {}
        A.c = class {
          static x = 1
        }
        """,
        """
        class A {
        }
        A.c = class {
          static STATIC_INIT$0() {
            A.c.x = 1;
          }
        };
        A.c.STATIC_INIT$0();
        """);

    test(
        """
        class A {}
        A[1] = class {
          static x = 1
        }
        """,
        """
        class A {
        }
        const CLASS_DECL$0 = class {
          static STATIC_INIT$1() {
            CLASS_DECL$0.x = 1;
          }
        };
        CLASS_DECL$0.STATIC_INIT$1();
        A[1] = CLASS_DECL$0;
        """);

    test(
        """
        let c = class C {
          static y = 2;
          static x = C.y
        }
        """,
        """
        let c = class {
          static STATIC_INIT$0() {
            c.y = 2;
            c.x = c.y;
          }
        };
        c.STATIC_INIT$0();
        """);

    test(
        """
        foo(class C {
          static y = 2;
          static x = C.y
        })
        """,
        """
        const CLASS_DECL$0 = class {
          static STATIC_INIT$1() {
            CLASS_DECL$0.y = 2;
            CLASS_DECL$0.x = CLASS_DECL$0.y;
          }
        };
        CLASS_DECL$0.STATIC_INIT$1();
        foo(CLASS_DECL$0);
        """);

    test(
        """
        foo(class C {
          static y = 2;
          x = C.y
        })
        """,
        """
        const CLASS_DECL$0 = class {
          constructor() {
            this.x = CLASS_DECL$0.y;
          }
          static STATIC_INIT$1() {
            CLASS_DECL$0.y = 2;
          }
        };
        CLASS_DECL$0.STATIC_INIT$1();
        foo(CLASS_DECL$0);
        """);
  }

  @Test
  public void testNonClassDeclarationsInstanceNoncomputedFields() {
    test(
        """
        let c = class {
          y = 2;
        }
        """,
        """
        let c = class {
          constructor() {
            this.y = 2;
          }
        }
        """);

    test(
        """
        let c = class C {
          y = 2;
        }
        """,
        """
        let c = class {
          constructor() {
            this.y = 2;
          }
        };
        """);

    test(
        """
        class A {}
        A.c = class {
          y = 2;
        }
        """,
        """
        class A {}
        A.c = class {
          constructor() {
            this.y = 2;
          }
        }
        """);

    test(
        """
        A[1] = class {
          y = 2;
        }
        """,
        """
        const CLASS_DECL$0 = class {
          constructor() {
            this.y = 2;
          }
        };
        A[1] = CLASS_DECL$0;
        """);

    test(
        """
        let c = class C {
          y = 2;
        }
        """,
        """
        let c = class {
          constructor() {
            this.y = 2;
          }
        };
        """);

    test(
        """
        class A {}
        A.c = class C {
          y = 2;
        }
        """,
        """
        class A {}
        A.c = class {
          constructor() {
            this.y = 2;
          }
        };

        """);

    test(
        """
        A[1] = class C {
          y = 2;
        }
        """,
        """
        const CLASS_DECL$0 = class {
          constructor() {
            this.y = 2;
          }
        };
        A[1] = CLASS_DECL$0;
        """);

    test(
        """
        foo(class C {
          y = 2;
        })
        """,
        """
        const CLASS_DECL$0 = class {
          constructor() {
            this.y = 2;
          }
        };
        foo(CLASS_DECL$0);
        """);
  }

  @Test
  public void testConstuctorAndStaticFieldDontConflict() {
    test(
        """
        let x = 2;
        class C {
          static y = x
          constructor(x) {}
        }
        """,
        """
        let x = 2;
        class C {
          constructor(x$jscomp$1) {
          }
          static STATIC_INIT$0() {
            C.y = x;
          }
        }
        C.STATIC_INIT$0();
        """);
  }

  @Test
  public void testInstanceInitializerShadowsConstructorDeclaration() {
    test(
        """
        let x = 2;
        class C {
          y = x;
          constructor(x) {}
        }
        """,
        """
        let x = 2;
        class C {
          constructor(x$jscomp$1) {
            this.y = x;
          }
        }
        """);

    test(
        """
        let x = 2;
        class C {
          y = x;
          constructor() { let x; }
        }
        """,
        """
        let x = 2;
        class C {
          constructor() {
            this.y = x;
            let x$jscomp$1;
          }
        }
        """);

    test(
        """
        let x = 2;
        class C {
          y = x
          constructor() { {var x;} }
        }
        """,
        """
        let x = 2;
        class C {
          constructor() {
            this.y = x;
            {
             var x$jscomp$1;
            }
          }
        }
        """);

    test(
        """
        function f() { return 4; }
        class C {
          y = f();
          constructor() {function f() { return 'str'; }}
        }
        """,
        """
        function f() {
          return 4;
        }
        class C {
          constructor() {
            function f$jscomp$1() {
              return 'str';
            }
            this.y = f();
          }
        }
        """);

    test(
        """
        class Foo {
          constructor(x) {}
          y = (x) => x;
        }
        """,
        """
        class Foo {
          constructor(x) {
            this.y = x$jscomp$1 => {
              return x$jscomp$1;
            };
          }
        }
        """);

    test(
        """
        let x = 2;
        class C {
          y = (x) => x;
          constructor(x) {}
        }
        """,
        """
        let x = 2;
        class C {
          constructor(x$jscomp$2) {
            this.y = x$jscomp$1 => {
              return x$jscomp$1;
            };
          }
        }
        """);
  }

  @Test
  public void testInstanceInitializerDoesntShadowConstructorDeclaration() {
    test(
        """
        let x = 2;
        class C {
          y = x;
          constructor() { {let x;} }
        }
        """,
        """
        let x = 2;
        class C {
          constructor() {
            this.y = x;
            {let x$jscomp$1;}
          }
        }
        """);

    test(
        """
        let x = 2;
        class C {
          y = x
          constructor() {() => { let x; };}
        }
        """,
        """
        let x = 2;
        class C {
          constructor() {
            this.y = x;
            () => { let x$jscomp$1; };
          }
        }
        """);

    test(
        """
        let x = 2;
        class C {
          y = x
          constructor() {(x) => 3;}
        }
        """,
        """
        let x = 2;
        class C {
          constructor() {
            this.y = x;
            (x$jscomp$1) => { return 3; };
          }
        }
        """);
  }

  @Test
  public void testInstanceFieldInitializersDontBleedOut() {
    test(
        """
        class C {
          y = z
          method() { x; }
          constructor(x) {}
        }
        """,
        """
        class C {
          method() { x; }
          constructor(x) {
            this.y = z;
          }
        }
        """);
  }

  @Test
  public void testNestedClassesWithShadowingInstanceFields() {
    test(
        """
        let x = 2;
        class C {
          y = () => {
            class Foo { z = x }
          };
          constructor(x) {}
        }
        """,
        """
        let x = 2;
        class C {
          constructor(x$jscomp$1) {
            this.y = () => {
              class Foo {
                constructor() {
                  this.z = x;
                }
              }
            };
          }
        }
        """);
  }

  // Added when fixing transpilation of real-world code that passed a class expression to a
  // constructor call.
  @Test
  public void testPublicFieldsInClassExpressionInNew() {
    test(
        """
        let foo = new (
            class Bar {
              x;
              static y;
            }
        )();
        """,
        """
        const CLASS_DECL$0 = class {
          constructor() {
            this.x;
          }
          static STATIC_INIT$1() {
            CLASS_DECL$0.y;
          }
        };
        CLASS_DECL$0.STATIC_INIT$1();
        let foo = new CLASS_DECL$0();
        """);
  }

  @Test
  public void testNonClassDeclarationsFunctionArgs() {
    test(
        """
        A[foo()] = class {
          static x;
        }
        """,
        """
        A[foo()] = (() => {
          const CLASS_DECL$0 = class {
            static STATIC_INIT$1() {
              CLASS_DECL$0.x;
            }
          };
          CLASS_DECL$0.STATIC_INIT$1();
          return CLASS_DECL$0;
        })();
        """);

    test(
        """
        foo(c = class {
          static x;
        })
        """,
        """
        const CLASS_DECL$0 = class {
          static STATIC_INIT$1() {
            CLASS_DECL$0.x;
          }
        };
        CLASS_DECL$0.STATIC_INIT$1();
        foo(c = CLASS_DECL$0);
        """);

    test(
        """
        function foo(c = class {
          static x;
        }) {}
        """,
        """
        function foo(c = (() => {
          const CLASS_DECL$0 = class {
            static STATIC_INIT$1() {
              CLASS_DECL$0.x;
            }
          };
          CLASS_DECL$0.STATIC_INIT$1();
          return CLASS_DECL$0;
        })()) {
        }
        """);
  }

  @Test
  public void testAnonymousClassExpression() {
    test(
        """
        function foo() {
          return class {
            y;
            static x;
          }
        }
        """,
        """
        function foo() {
          const CLASS_DECL$0 = class {
            constructor() {
              this.y;
            }
            static STATIC_INIT$1() {
              CLASS_DECL$0.x;
            }
          };
          CLASS_DECL$0.STATIC_INIT$1();
          return CLASS_DECL$0;
        }
        """);

    test(
        """
        foo(class {
          y = 2;
        })
        """,
        """
        const CLASS_DECL$0 = class {
          constructor() {
            this.y = 2;
          }
        };
        foo(CLASS_DECL$0);
        """);

    test(
        """
        foo(class {
          static x = 1;
        })
        """,
        """
        const CLASS_DECL$0 = class {
          static STATIC_INIT$1() {
            CLASS_DECL$0.x = 1;
          }
        };
        CLASS_DECL$0.STATIC_INIT$1();
        foo(CLASS_DECL$0);
        """);
  }
}
