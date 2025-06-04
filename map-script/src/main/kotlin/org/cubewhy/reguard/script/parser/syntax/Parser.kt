package org.cubewhy.reguard.script.parser.syntax

import org.cubewhy.reguard.script.core.AliasEntry
import org.cubewhy.reguard.script.core.ClassDeclaration
import org.cubewhy.reguard.script.core.FieldDeclaration
import org.cubewhy.reguard.script.core.ImportDeclaration
import org.cubewhy.reguard.script.core.MappingFile
import org.cubewhy.reguard.script.core.MethodDeclaration
import org.cubewhy.reguard.script.core.PackageDeclaration
import org.cubewhy.reguard.script.core.Parameter
import org.cubewhy.reguard.script.parser.lexer.Token
import org.cubewhy.reguard.script.parser.lexer.TokenType

class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): MappingFile {
        val packageDecl = parsePackageDeclaration()
        val imports = mutableListOf<ImportDeclaration>()
        val classes = mutableListOf<ClassDeclaration>()

        while (!isAtEnd()) {
            skipEmptyLines()
            when {
                check(TokenType.IMPORT) -> imports.add(parseImportDeclaration())
                check(TokenType.CLASS) -> classes.add(parseClassDeclaration())
                check(TokenType.COMMENT) || check(TokenType.JAVADOC) -> advance() // Skip comments
                else -> advance() // Skip unknown tokens
            }
        }

        return MappingFile(packageDecl, imports, classes)
    }

    private fun parsePackageDeclaration(): PackageDeclaration? {
        skipEmptyLines()
        if (!check(TokenType.PKG)) return null

        advance() // consume 'pkg'
        val packageName = parseQualifiedName()
        consume(TokenType.SEMICOLON, "Expected ';' after package declaration")

        return PackageDeclaration(packageName)
    }

    private fun parseImportDeclaration(): ImportDeclaration {
        advance() // consume 'import'
        val importPath = parseQualifiedName()

        val alias = if (match(TokenType.AS)) {
            // consume alias
            consume(TokenType.IDENTIFIER, "Expected alias name after 'as'").lexeme
        } else null

        consume(TokenType.SEMICOLON, "Expected ';' after import declaration")

        return ImportDeclaration(importPath, alias)
    }

    private fun parseClassDeclaration(): ClassDeclaration {
        advance() // consume 'class'
        val name = consume(TokenType.IDENTIFIER, "Expected class name").lexeme

        val obfuscatedName = if (match(TokenType.LEFT_ARROW)) {
            consume(TokenType.IDENTIFIER, "Expected obfuscated class name").lexeme
        } else null

        val metadata = parseMetadata()

        consume(TokenType.LEFT_BRACE, "Expected '{' after class declaration")

        val methods = mutableListOf<MethodDeclaration>()
        val fields = mutableListOf<FieldDeclaration>()
        val innerClasses = mutableListOf<ClassDeclaration>()
        val aliases = mutableListOf<AliasEntry>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            skipEmptyLines()
            when {
                check(TokenType.MD) -> methods.add(parseMethodDeclaration())
                check(TokenType.FD) -> fields.add(parseFieldDeclaration())
                check(TokenType.CLASS) -> innerClasses.add(parseClassDeclaration())
                check(TokenType.ALIASES) -> aliases.addAll(parseAliasesBlock())
                check(TokenType.COMMENT) || check(TokenType.JAVADOC) -> advance()
                else -> advance()
            }
            skipEmptyLines()
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}' after class body")

        return ClassDeclaration(name, obfuscatedName, metadata, methods, fields, innerClasses, aliases)
    }

    private fun parseMethodDeclaration(): MethodDeclaration {
        val javadoc = if (previous().type == TokenType.JAVADOC) previous().lexeme else null

        advance() // consume 'md'
        val name = consume(TokenType.IDENTIFIER, "Expected method name").lexeme

        val parameters = parseParameterList()

        val obfuscatedName = if (match(TokenType.LEFT_ARROW)) {
            consume(TokenType.IDENTIFIER, "Expected obfuscated method name").lexeme
        } else null

        val metadata = parseMetadata()

        val aliases = if (match(TokenType.LEFT_BRACE)) {
            val result = mutableListOf<AliasEntry>()
            while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                skipEmptyLines()
                if (check(TokenType.ALIASES)) {
                    result.addAll(parseAliasesBlock())
                } else {
                    advance()
                }
                skipEmptyLines()
            }
            consume(TokenType.RIGHT_BRACE, "Expected '}' after method body")
            result
        } else {
            consume(TokenType.SEMICOLON, "Expected ';' after method declaration")
            emptyList()
        }

        return MethodDeclaration(name, parameters, obfuscatedName, javadoc, aliases, metadata)
    }

    private fun parseFieldDeclaration(): FieldDeclaration {
        advance() // consume 'fd'
        val name = consume(TokenType.IDENTIFIER, "Expected field name").lexeme

        val obfuscatedName = if (match(TokenType.LEFT_ARROW)) {
            consume(TokenType.IDENTIFIER, "Expected obfuscated field name").lexeme
        } else null

        val metadata = parseMetadata()

        val aliases = if (match(TokenType.LEFT_BRACE)) {
            val result = mutableListOf<AliasEntry>()
            while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                skipEmptyLines()
                if (check(TokenType.ALIASES)) {
                    result.addAll(parseAliasesBlock())
                } else {
                    advance()
                }
                skipEmptyLines()
            }
            consume(TokenType.RIGHT_BRACE, "Expected '}' after field body")
            result
        } else {
            consume(TokenType.SEMICOLON, "Expected ';' after field declaration")
            emptyList()
        }

        return FieldDeclaration(name, obfuscatedName, aliases, metadata)
    }

    private fun parseParameterList(): List<Parameter> {
        if (!match(TokenType.LEFT_PAREN)) return emptyList()

        val parameters = mutableListOf<Parameter>()

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                val type = parseQualifiedName()
                val isVarargs = match(TokenType.VARARGS)
                parameters.add(Parameter(type, isVarargs))
            } while (match(TokenType.COMMA))
        }

        consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters")

        return parameters
    }

    private fun parseAliasesBlock(): List<AliasEntry> {
        advance() // consume 'aliases'
        consume(TokenType.LEFT_BRACE, "Expected '{' after 'aliases'")

        val aliases = mutableListOf<AliasEntry>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            skipEmptyLines()

            if (check(TokenType.REMOVED)) {
                advance() // consume 'removed'
                val version = consume(TokenType.IDENTIFIER, "Expected version after 'removed'").lexeme
                consume(TokenType.SEMICOLON, "Expected ';' after removed declaration")
                aliases.add(AliasEntry(version, "", emptyMap(), true))
            } else if (check(TokenType.IDENTIFIER)) {
                val version = advance().lexeme
                val obfuscatedName = consume(TokenType.IDENTIFIER, "Expected obfuscated name").lexeme
                val metadata = parseMetadata()
                consume(TokenType.SEMICOLON, "Expected ';' after alias entry")
                aliases.add(AliasEntry(version, obfuscatedName, metadata, false))
            } else {
                advance() // Skip unknown tokens
            }

            skipEmptyLines()
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}' after aliases block")

        return aliases
    }

    private fun parseMetadata(): Map<String, String> {
        if (!match(TokenType.LEFT_BRACKET)) return emptyMap()

        val metadata = mutableMapOf<String, String>()

        if (!check(TokenType.RIGHT_BRACKET)) {
            do {
                val key = consume(TokenType.IDENTIFIER, "Expected metadata key").lexeme
                consume(TokenType.EQUALS, "Expected '=' after metadata key")
                val value = consume(TokenType.STRING, "Expected string value for metadata").lexeme
                metadata[key] = value
            } while (match(TokenType.COMMA))
        }

        consume(TokenType.RIGHT_BRACKET, "Expected ']' after metadata")

        return metadata
    }

    private fun parseQualifiedName(): String {
        val parts = mutableListOf<String>()
        parts.add(consume(TokenType.IDENTIFIER, "Expected identifier").lexeme)

        while (match(TokenType.DOT)) {
            parts.add(consume(TokenType.IDENTIFIER, "Expected identifier after '.'").lexeme)
        }

        return parts.joinToString(".")
    }

    private fun skipEmptyLines() {
        while (match(TokenType.NEWLINE, TokenType.COMMENT)) {
            // Skip newlines
        }
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF

    private fun peek(): Token = tokens[current]

    private fun previous(): Token = tokens[current - 1]

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()

        val currentToken = peek()
        throw RuntimeException("$message at line ${currentToken.line}, column ${currentToken.column}. Got '${currentToken.lexeme}'")
    }
}