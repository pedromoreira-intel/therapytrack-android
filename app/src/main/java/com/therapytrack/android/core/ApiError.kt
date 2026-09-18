package com.therapytrack.android.core

sealed class ApiError(message: String) : Exception(message) {
    class Network(cause: Throwable) : ApiError("Sem ligação: ${cause.message}")
    object Unauthorized : ApiError("Sessão terminada")
    /** 402: the request was fine; the plan does not cover it. */
    class NotInPlan(message: String) : ApiError(message)
    class Server(val status: Int, message: String) : ApiError(message)
    /**
     * The signed-in account is not the one this request was written by. Not a
     * failure of the item — a drain that sees this stops and leaves the item for
     * its owner.
     */
    object AccountChanged : ApiError("A conta com sessão iniciada mudou")
}
