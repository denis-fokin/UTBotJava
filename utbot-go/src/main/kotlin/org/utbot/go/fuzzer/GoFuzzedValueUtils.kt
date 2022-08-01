package org.utbot.go.fuzzer

import org.utbot.framework.plugin.api.GoUtModel
import org.utbot.fuzzer.FuzzedValue

val FuzzedValue.goRequiredImports get() = (this.model as GoUtModel).requiredImports