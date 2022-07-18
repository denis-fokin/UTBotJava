package org.utbot.go.fuzzer

import org.utbot.fuzzer.FuzzedConcreteValue

// TODO: find and use real third-party Go parser

sealed class GoAstVisitor {
    val fuzzedConcreteValues: MutableSet<FuzzedConcreteValue> = mutableSetOf()
}

object DummyGoAstVisitor : GoAstVisitor()