package org.cubewhy.reguard.script.parser.lexer

class Lexer(private val source: String) {
    private var current = 0
    private var line = 1
    private var column = 1
    private val tokens = mutableListOf<Token>()

    // Context stack to track where we are in parsing
    private val contextStack = mutableListOf<ParseContext>()

    private val keywords = mapOf(
        "pkg" to TokenType.PKG,
        "import" to TokenType.IMPORT,
        "class" to TokenType.CLASS,
        "md" to TokenType.MD,
        "fd" to TokenType.FD,
        "aliases" to TokenType.ALIASES,
        "as" to TokenType.AS,
        "removed" to TokenType.REMOVED
    )

    private enum class ParseContext {
        GLOBAL,           // Top level
        PACKAGE_NAME,     // After 'pkg'
        IMPORT_PATH,      // After 'import'
        CLASS_NAME,       // After 'class' or in class mapping
        METHOD_NAME,      // After 'md' or in method context
        FIELD_NAME,       // After 'fd' or in field context
        ALIAS_CONTEXT     // Inside aliases block
    }

    fun tokenize(): List<Token> {
        contextStack.add(ParseContext.GLOBAL)

        while (!isAtEnd()) {
            scanToken()
        }

        tokens.add(Token(TokenType.EOF, "", line, column))
        return tokens
    }

    private fun scanToken() {
        val startColumn = column
        val c = advance()

        when (c) {
            ' ', '\r', '\t' -> {
                // Ignore whitespace
            }

            '\n' -> {
                addToken(TokenType.NEWLINE, "\n", line, startColumn)
                line++
                column = 1
                return
            }

            '{' -> {
                addToken(TokenType.LEFT_BRACE, "{", line, startColumn)
                // Push context based on previous tokens
                updateContextOnBrace()
            }

            '}' -> {
                addToken(TokenType.RIGHT_BRACE, "}", line, startColumn)
                // Pop context
                if (contextStack.size > 1) {
                    contextStack.removeLastOrNull()
                }
            }

            '(' -> addToken(TokenType.LEFT_PAREN, "(", line, startColumn)
            ')' -> addToken(TokenType.RIGHT_PAREN, ")", line, startColumn)
            '[' -> addToken(TokenType.LEFT_BRACKET, "[", line, startColumn)
            ']' -> addToken(TokenType.RIGHT_BRACKET, "]", line, startColumn)
            ';' -> {
                addToken(TokenType.SEMICOLON, ";", line, startColumn)
                resetToGlobalContext()
            }

            ',' -> addToken(TokenType.COMMA, ",", line, startColumn)
            '=' -> addToken(TokenType.EQUALS, "=", line, startColumn)
            '.' -> {
                if (match('.') && match('.')) {
                    addToken(TokenType.VARARGS, "...", line, startColumn)
                } else {
                    addToken(TokenType.DOT, ".", line, startColumn)
                }
            }

            '<' -> {
                if (match('-')) {
                    addToken(TokenType.LEFT_ARROW, "<-", line, startColumn)
                }
            }

            '/' -> handleComment(startColumn)
            '"' -> string(startColumn)
            else -> {
                when {
                    isDigit(c) -> number(startColumn)
                    isAlpha(c) -> identifier(startColumn)
                    else -> throw RuntimeException("Unexpected character '$c' at line $line, column $startColumn")
                }
            }
        }
    }

    private fun updateContextOnBrace() {
        // Look at the last few tokens to determine context
        val recentTokens = tokens.takeLast(3)
        when {
            recentTokens.any { it.type == TokenType.ALIASES } -> {
                contextStack.add(ParseContext.ALIAS_CONTEXT)
            }

            recentTokens.any { it.type == TokenType.CLASS } -> {
                contextStack.add(ParseContext.CLASS_NAME)
            }

            recentTokens.any { it.type == TokenType.MD } -> {
                contextStack.add(ParseContext.METHOD_NAME)
            }

            recentTokens.any { it.type == TokenType.FD } -> {
                contextStack.add(ParseContext.FIELD_NAME)
            }
        }
    }

    private fun resetToGlobalContext() {
        contextStack.clear()
        contextStack.add(ParseContext.GLOBAL)
    }

    private fun updateContextAfterKeyword(tokenType: TokenType) {
        when (tokenType) {
            TokenType.PKG -> contextStack.add(ParseContext.PACKAGE_NAME)
            TokenType.IMPORT -> contextStack.add(ParseContext.IMPORT_PATH)
            TokenType.CLASS -> contextStack.add(ParseContext.CLASS_NAME)
            TokenType.MD -> contextStack.add(ParseContext.METHOD_NAME)
            TokenType.FD -> contextStack.add(ParseContext.FIELD_NAME)
            else -> { /* No context change */
            }
        }
    }

    private fun shouldTreatAsKeyword(text: String): Boolean {
        val currentContext = contextStack.lastOrNull() ?: ParseContext.GLOBAL

        return when (currentContext) {
            ParseContext.PACKAGE_NAME,
            ParseContext.CLASS_NAME,
            ParseContext.METHOD_NAME,
            ParseContext.FIELD_NAME,
            ParseContext.ALIAS_CONTEXT -> false // Treat as identifier in these contexts
            ParseContext.IMPORT_PATH -> text == "as"
            ParseContext.GLOBAL -> keywords.containsKey(text) // Only treat as keyword in global context
        }
    }

    private fun handleComment(startColumn: Int) {
        when {
            match('/') -> {
                // Single line comment
                while (peek() != '\n' && !isAtEnd()) advance()
                val comment = source.substring(current - (column - startColumn), current)
                addToken(TokenType.COMMENT, comment, line, startColumn)
            }

            match('*') -> {
                // Block comment or javadoc
                val isJavadoc = peek() == '*'
                if (isJavadoc) advance()

                val start = current
                while (!(peek() == '*' && peekNext() == '/') && !isAtEnd()) {
                    if (peek() == '\n') {
                        line++
                        column = 0
                    }
                    advance()
                }

                if (isAtEnd()) {
                    throw RuntimeException("Unterminated comment at line $line")
                }

                val comment = source.substring(start, current)
                advance() // consume *
                advance() // consume /

                addToken(
                    if (isJavadoc) TokenType.JAVADOC else TokenType.COMMENT,
                    comment,
                    line,
                    startColumn
                )
            }
        }
    }

    private fun string(startColumn: Int) {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++
                column = 0
            }
            advance()
        }

        if (isAtEnd()) {
            throw RuntimeException("Unterminated string at line $line")
        }

        advance() // closing "

        val value = source.substring(current - (column - startColumn) + 1, current - 1)
        addToken(TokenType.STRING, value, line, startColumn)
    }

    private fun number(startColumn: Int) {
        while (isDigit(peek())) advance()

        val value = source.substring(current - (column - startColumn), current)
        addToken(TokenType.NUMBER, value, line, startColumn)
    }

    private fun identifier(startColumn: Int) {
        while (isAlphaNumeric(peek()) || peek() == '_') advance()

        val text = source.substring(current - (column - startColumn), current)

        // Check if we should treat this as a keyword based on context
        val type = if (shouldTreatAsKeyword(text)) {
            val keywordType = keywords[text] ?: TokenType.IDENTIFIER
            if (keywordType != TokenType.IDENTIFIER) {
                updateContextAfterKeyword(keywordType)
            }
            keywordType
        } else {
            TokenType.IDENTIFIER
        }

        addToken(type, text, line, startColumn)
    }

    private fun addToken(type: TokenType, lexeme: String, line: Int, column: Int) {
        tokens.add(Token(type, lexeme, line, column))
    }

    private fun advance(): Char {
        column++
        return source[current++]
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd() || source[current] != expected) return false
        current++
        column++
        return true
    }

    private fun peek(): Char = if (isAtEnd()) '\u0000' else source[current]

    private fun peekNext(): Char = if (current + 1 >= source.length) '\u0000' else source[current + 1]

    private fun isAtEnd(): Boolean = current >= source.length

    private fun isDigit(c: Char): Boolean = c in '0'..'9'

    private fun isAlpha(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z'

    private fun isAlphaNumeric(c: Char): Boolean = isAlpha(c) || isDigit(c)
}