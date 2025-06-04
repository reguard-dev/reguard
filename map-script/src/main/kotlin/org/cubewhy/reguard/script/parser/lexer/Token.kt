package org.cubewhy.reguard.script.parser.lexer

data class Token(
    val type: TokenType,
    val lexeme: String,
    val line: Int,
    val column: Int
)