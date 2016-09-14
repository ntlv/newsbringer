package se.ntlv.newsbringer

class ImpossibleException(message: String = "This cannot be!") : Throwable(message)

fun thisShouldNeverHappen(): Nothing = throw ImpossibleException()

fun thisShouldNeverHappen(message: String): Nothing = throw ImpossibleException(message)