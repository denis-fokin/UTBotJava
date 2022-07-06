package org.utbot.intellij.plugin.js

import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

data class JsTestsModel(
    val project: Project,
    val srcModule: Module,
    val testModule: Module,
    val selectedMethods: Set<JSMemberInfo>?,
)
