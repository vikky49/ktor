package io.ktor.client.utils

class ExecuteChain<In, Out>(base: suspend (In) -> Out) {
    private var chain: suspend (In) -> Out = base

    fun intercept(block: suspend (suspend (In) -> Out, In) -> Out) {
        val parent = chain
        chain = { input -> block(parent, input) }
    }

    suspend fun execute(input: In): Out = chain(input)
}