// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.util;

import com.intellij.execution.CommonProgramRunConfigurationParameters;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.execution.configurations.SimpleProgramParameters;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.workspace.SubprojectInfoProvider;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.Nullable;

public final class ProgramParametersUtil {
  public static void configureConfiguration(SimpleProgramParameters parameters, CommonProgramRunConfigurationParameters configuration) {
    new ProgramParametersConfigurator().configureConfiguration(parameters, configuration);
  }

  public static String getWorkingDir(CommonProgramRunConfigurationParameters configuration, Project project, Module module) {
    return new ProgramParametersConfigurator().getWorkingDir(configuration, project, module);
  }

  public static void checkWorkingDirectoryExist(CommonProgramRunConfigurationParameters configuration, Project project, Module module)
    throws RuntimeConfigurationWarning {
    ProgramParametersConfigurator configurator = new ProgramParametersConfigurator();
    configurator.setValidation(true);
    try {
      configurator.checkWorkingDirectoryExist(configuration, project, module);
    }
    catch (IncorrectOperationException ignore) {
    }
  }

  public static String expandPath(String path, Module module, Project project) {
    return new ProgramParametersConfigurator().expandPath(path, module, project);
  }

  public static String expandPathAndMacros(String path, Module module, Project project) {
    return new ProgramParametersConfigurator().expandPathAndMacros(path, module, project);
  }

  public static @Nullable Module getModule(CommonProgramRunConfigurationParameters configuration) {
    return new ProgramParametersConfigurator().getModule(configuration);
  }

  public static String getWorkingDirectoryByModule(ModuleBasedConfiguration<?, ?> configuration) {
    Module module = configuration.getConfigurationModule().getModule();
    if (module != null) {
      String path = SubprojectInfoProvider.Companion.getSubprojectPath(module);
      if (path != null) return path;
    }
    return PathUtil.toSystemDependentName(configuration.getProject().getBasePath());
  }
}
