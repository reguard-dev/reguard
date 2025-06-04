package org.cubewhy.reguard.script.parser.lexer

enum class TokenType {
    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    PKG, IMPORT, CLASS, MD, FD, ALIASES, AS, REMOVED,

    // Operators and punctuation
    LEFT_BRACE, RIGHT_BRACE,
    LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACKET, RIGHT_BRACKET,
    SEMICOLON, COMMA, EQUALS, DOT,
    LEFT_ARROW, VARARGS,

    // Special
    NEWLINE, COMMENT, JAVADOC,
    EOF
}