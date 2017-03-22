package se.ntlv.newsbringer

class ImpossibleException(message : String) : Throwable(message)

fun thisShouldNeverHappen(message: String = "This cannot be!"): Nothing = throw ImpossibleException(message)

fun throwingPlaceholder(message: String): () -> Any = { thisShouldNeverHappen(message) }