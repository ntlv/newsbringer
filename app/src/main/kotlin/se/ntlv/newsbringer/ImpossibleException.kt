package se.ntlv.newsbringer

class ImpossibleException(message : String) : Throwable(message)

fun thisShouldNeverHappen(message: String? = "This cannot be!"): Nothing = throw ImpossibleException(message ?: "This cannot be!")