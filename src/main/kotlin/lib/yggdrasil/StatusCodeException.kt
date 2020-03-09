package lib.yggdrasil

class StatusCodeException(message: String, val code: Int) : Exception(message)