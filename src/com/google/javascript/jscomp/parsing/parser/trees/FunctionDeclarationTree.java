/*
 * Copyright 2011 The Closure Compiler Authors.
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

package com.google.javascript.jscomp.parsing.parser.trees;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.javascript.jscomp.parsing.parser.IdentifierToken;
import com.google.javascript.jscomp.parsing.parser.util.SourceRange;
import org.jspecify.annotations.Nullable;

public class FunctionDeclarationTree extends ParseTree {

  public static enum Kind {
    DECLARATION,
    EXPRESSION,
    MEMBER,
    ARROW
  }

  public final @Nullable IdentifierToken name;
  public final FormalParameterListTree formalParameterList;
  public final ParseTree functionBody;
  public final boolean isClassMember;
  public final boolean isStatic;
  public final boolean isGenerator;
  public final boolean isOptional;
  public final boolean isAsync;
  public final Kind kind;

  public static Builder builder(Kind kind) {
    return new Builder(kind);
  }

  private FunctionDeclarationTree(Builder builder) {
    super(ParseTreeType.FUNCTION_DECLARATION, builder.location);

    this.name = builder.name;
    this.isClassMember = builder.isClassMember;
    this.isStatic = builder.isStatic;
    this.isGenerator = builder.isGenerator;
    this.isOptional = builder.isOptional;
    this.kind = checkNotNull(builder.kind);
    this.formalParameterList = checkNotNull(builder.formalParameterList);
    this.functionBody = checkNotNull(builder.functionBody);
    this.isAsync = builder.isAsync;
  }

  /** Builds a {@link FunctionDeclarationTree}. */
  public static class Builder {
    private final Kind kind;

    private @Nullable IdentifierToken name = null;
    private @Nullable FormalParameterListTree formalParameterList = null;
    private @Nullable ParseTree functionBody = null;
    private boolean isClassMember = false;
    private boolean isStatic = false;
    private boolean isGenerator = false;
    private boolean isOptional = false;
    private boolean isAsync = false;
    private SourceRange location;

    Builder(Kind kind) {
      this.kind = kind;
    }

    /**
     * Optional function name.
     *
     * <p>Default is {@code null}.
     */
    @CanIgnoreReturnValue
    public Builder setName(IdentifierToken name) {
      this.name = name;
      return this;
    }

    /** Required parameter list. */
    @CanIgnoreReturnValue
    public Builder setFormalParameterList(FormalParameterListTree formalParameterList) {
      this.formalParameterList = formalParameterList;
      return this;
    }

    /** Required function body. */
    @CanIgnoreReturnValue
    public Builder setFunctionBody(ParseTree functionBody) {
      this.functionBody = functionBody;
      return this;
    }

    /**
     * Is the method an ES6 class member?
     *
     * <p>Default is {@code false}. Only relevant for class method member declarations.
     */
    @CanIgnoreReturnValue
    public Builder setIsClassMember(boolean isClassMember) {
      this.isClassMember = isClassMember;
      return this;
    }

    /**
     * Is the method static?
     *
     * <p>Default is {@code false}. Only relevant for class method member declarations.
     */
    @CanIgnoreReturnValue
    public Builder setStatic(boolean isStatic) {
      this.isStatic = isStatic;
      return this;
    }

    /**
     * Is this a generator function?
     *
     * <p>Default is {@code false}.
     */
    @CanIgnoreReturnValue
    public Builder setGenerator(boolean isGenerator) {
      this.isGenerator = isGenerator;
      return this;
    }

    /**
     * Is this the declaration of an optional function parameter? Default is {@code false}.
     *
     * <p>Only relevant for function declaration as a parameter to another function.
     */
    @CanIgnoreReturnValue
    public Builder setOptional(boolean isOptional) {
      this.isOptional = isOptional;
      return this;
    }

    /**
     * Is this an asynchronous function?
     *
     * <p>Default is {@code false}.
     */
    @CanIgnoreReturnValue
    public Builder setAsync(boolean isAsync) {
      this.isAsync = isAsync;
      return this;
    }

    /**
     * Return a new {@link FunctionDeclarationTree}.
     *
     * <p>The location is provided at this point because it cannot be correctly calculated until the
     * whole function has been parsed.
     */
    public FunctionDeclarationTree build(SourceRange location) {
      this.location = location;
      return new FunctionDeclarationTree(this);
    }
  }
}
