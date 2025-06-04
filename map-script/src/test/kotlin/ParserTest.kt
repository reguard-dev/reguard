import org.cubewhy.reguard.script.parser.lexer.Token
import org.cubewhy.reguard.script.parser.lexer.TokenType
import org.cubewhy.reguard.script.parser.syntax.Parser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ParserTest {

    private fun token(type: TokenType, lexeme: String = "", line: Int = 1, column: Int = 1) =
        Token(type, lexeme, line, column)

    private fun tokensForPackage(pkgName: String): List<Token> {
        val parts = pkgName.split(".")
        val tokens = mutableListOf<Token>()
        tokens.add(token(TokenType.PKG, "pkg"))
        parts.forEachIndexed { i, part ->
            tokens.add(token(TokenType.IDENTIFIER, part))
            if (i < parts.size - 1) tokens.add(token(TokenType.DOT, "."))
        }
        tokens.add(token(TokenType.SEMICOLON, ";"))
        tokens.add(token(TokenType.EOF))
        return tokens
    }

    @Test
    fun `parse simple package declaration`() {
        val tokens = tokensForPackage("com.example.test")
        val parser = Parser(tokens)
        val mappingFile = parser.parse()

        assertNotNull(mappingFile.packageDecl)
        assertEquals("com.example.test", mappingFile.packageDecl?.packageName)
    }

    @Test
    fun `parse import with alias`() {
        val tokens = listOf(
            token(TokenType.PKG, "pkg"),
            token(TokenType.IDENTIFIER, "com"),
            token(TokenType.DOT),
            token(TokenType.IDENTIFIER, "example"),
            token(TokenType.SEMICOLON),

            token(TokenType.IMPORT, "import"),
            token(TokenType.IDENTIFIER, "com"),
            token(TokenType.DOT),
            token(TokenType.IDENTIFIER, "example"),
            token(TokenType.DOT),
            token(TokenType.IDENTIFIER, "lib"),
            token(TokenType.AS, "as"),
            token(TokenType.IDENTIFIER, "libalias"),
            token(TokenType.SEMICOLON),

            token(TokenType.EOF)
        )

        val parser = Parser(tokens)
        val mappingFile = parser.parse()

        assertEquals(1, mappingFile.imports.size)
        val importDecl = mappingFile.imports[0]
        assertEquals("com.example.lib", importDecl.importPath)
        assertEquals("libalias", importDecl.alias)
    }

    @Test
    fun `parse class declaration with method and field`() {
        val tokens = listOf(
            token(TokenType.PKG, "pkg"),
            token(TokenType.IDENTIFIER, "com"),
            token(TokenType.DOT),
            token(TokenType.IDENTIFIER, "example"),
            token(TokenType.SEMICOLON),

            token(TokenType.CLASS, "class"),
            token(TokenType.IDENTIFIER, "MyClass"),
            token(TokenType.LEFT_BRACE, "{"),

            token(TokenType.MD, "md"),
            token(TokenType.IDENTIFIER, "myMethod"),
            token(TokenType.LEFT_PAREN, "("),
            token(TokenType.IDENTIFIER, "java.lang.String"),
            token(TokenType.RIGHT_PAREN, ")"),
            token(TokenType.SEMICOLON, ";"),

            token(TokenType.FD, "fd"),
            token(TokenType.IDENTIFIER, "myField"),
            token(TokenType.SEMICOLON, ";"),

            token(TokenType.RIGHT_BRACE, "}"),
            token(TokenType.EOF)
        )

        val parser = Parser(tokens)
        val mappingFile = parser.parse()

        assertEquals(1, mappingFile.classes.size)
        val clazz = mappingFile.classes[0]
        assertEquals("MyClass", clazz.name)
        assertEquals(1, clazz.methods.size)
        assertEquals("myMethod", clazz.methods[0].name)
        assertEquals(1, clazz.fields.size)
        assertEquals("myField", clazz.fields[0].name)
    }

    @Test
    fun `consume throws error on unexpected token`() {
        val tokens = listOf(
            token(TokenType.PKG, "pkg"),
            token(TokenType.IDENTIFIER, "com"),
            token(TokenType.LEFT_PAREN), // bad token
            token(TokenType.EOF)
        )
        val parser = Parser(tokens)

        val exception = assertThrows(RuntimeException::class.java) {
            parser.parse()
        }
        assertTrue(exception.message!!.contains("Expected ';' after package declaration at line 1, column 1. Got ''") ||
                exception.message!!.contains("Expected identifier"))
    }
}
