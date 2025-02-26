// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.groovy.intentions.conversions;

import com.intellij.codeInspection.util.IntentionName;
import com.intellij.modcommand.ActionContext;
import com.intellij.modcommand.ModPsiUpdater;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.intentions.GroovyIntentionsBundle;
import org.jetbrains.plugins.groovy.intentions.base.GrPsiUpdateIntention;
import org.jetbrains.plugins.groovy.intentions.base.PsiElementPredicate;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifier;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrTypeElement;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import org.jetbrains.plugins.groovy.refactoring.GroovyRefactoringUtil;

/**
 * @author Max Medvedev
 */
public final class GrSplitDeclarationIntention extends GrPsiUpdateIntention {

  @Override
  protected void processIntention(@NotNull PsiElement element, @NotNull ActionContext context, @NotNull ModPsiUpdater updater) {
    if (!(element instanceof GrVariableDeclaration declaration)) return;

    Project project = context.project();
    GrVariable[] variables = declaration.getVariables();
    if (variables.length == 1) {
      processSingleVar(project, declaration, variables[0]);
    }
    else if (variables.length > 1) {
      if (!declaration.isTuple() || declaration.getTupleInitializer() instanceof GrListOrMap) {
        processMultipleVars(project, declaration);
      }
      else {
        processTuple(project, declaration);
      }
    }
  }

  private static void processTuple(Project project, GrVariableDeclaration declaration) {
    GrExpression initializer = declaration.getTupleInitializer();
    assert initializer != null;

    GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(project);

    GrVariable[] variables = declaration.getVariables();

    StringBuilder assignmentBuilder = new StringBuilder();
    assignmentBuilder.append('(');
    for (GrVariable variable : variables) {
      assignmentBuilder.append(variable.getName()).append(',');
    }
    assignmentBuilder.replace(assignmentBuilder.length() - 1, assignmentBuilder.length(), ")=");
    assignmentBuilder.append(initializer.getText());

    GrStatement assignment = factory.createStatementFromText(assignmentBuilder.toString());

    declaration = GroovyRefactoringUtil.addBlockIntoParent(declaration);
    declaration.getParent().addAfter(assignment, declaration);

    initializer.delete();
  }

  private static void processMultipleVars(Project project, GrVariableDeclaration declaration) {
    GrVariable[] variables = declaration.getVariables();
    String modifiers = declaration.getModifierList().getText();
    GrStatement[] sts = new GrStatement[variables.length];
    for (int i = 0; i < variables.length; i++) {
      sts[i] = createVarDeclaration(project, variables[i], modifiers, declaration.isTuple());
    }

    declaration = GroovyRefactoringUtil.addBlockIntoParent(declaration);

    for (int i = sts.length - 1; i >= 0; i--) {
      declaration.getParent().addAfter(sts[i], declaration);
    }

    declaration.delete();
  }

  private static void processSingleVar(Project project, GrVariableDeclaration declaration, GrVariable variable) {
    GrExpression initializer = variable.getInitializerGroovy();
    if (initializer != null) {
      GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(project);
      GrExpression assignment = factory.createExpressionFromText(variable.getName() + " = " + initializer.getText());
      initializer.delete();
      declaration = GroovyRefactoringUtil.addBlockIntoParent(declaration);
      declaration.getParent().addAfter(assignment, declaration);
    }
  }

  private static GrStatement createVarDeclaration(Project project, GrVariable variable, String modifiers, boolean isTuple) {
    StringBuilder builder = new StringBuilder();
    builder.append(modifiers).append(' ');
    GrTypeElement typeElement = variable.getTypeElementGroovy();
    if (typeElement != null) {
      builder.append(typeElement.getText()).append(' ');
    }
    builder.append(variable.getName());
    GrExpression initializer = variable.getInitializerGroovy();
    if (initializer != null) {
      builder.append('=').append(initializer.getText());
    }
    GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(project);
    GrVariableDeclaration decl = (GrVariableDeclaration)factory.createStatementFromText(builder);
    if (isTuple && (variable.getDeclaredType() != null || decl.getModifierList().getModifiers().length > 1)) {
      decl.getModifierList().setModifierProperty(GrModifier.DEF, false);
    }
    return decl;
  }

  @Override
  public @NotNull @IntentionName String getText(@NotNull PsiElement element) {
    GrVariableDeclaration decl = (GrVariableDeclaration)element;
    GrVariable[] variables = decl.getVariables();
    if (variables.length > 1 && PsiUtil.isLocalVariable(variables[0])) {
      if (!decl.isTuple() || decl.getTupleInitializer() instanceof GrListOrMap) {
        return GroovyIntentionsBundle.message("split.into.separate.declaration");
      }
      else {
        return GroovyIntentionsBundle.message("split.into.declaration.and.assignment");
      }
    }
    return GroovyIntentionsBundle.message("split.into.declaration.and.assignment");
  }

  @Override
  protected @NotNull PsiElementPredicate getElementPredicate() {
    return new PsiElementPredicate() {
      @Override
      public boolean satisfiedBy(@NotNull PsiElement element) {
        if (element instanceof GrVariableDeclaration decl) {
          GrVariable[] variables = decl.getVariables();
          return variables.length > 1 && PsiUtil.isLocalVariable(variables[0]) || 
                 variables.length == 1 && PsiUtil.isLocalVariable(variables[0]) && variables[0].getInitializerGroovy() != null;
        }
        return false;
      }
    };
  }
}
