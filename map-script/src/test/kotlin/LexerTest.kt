import org.cubewhy.reguard.script.parser.lexer.Lexer
import org.cubewhy.reguard.script.parser.lexer.Token
import org.cubewhy.reguard.script.parser.lexer.TokenType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LexerTest {

    private fun lex(source: String): List<Token> {
        return Lexer(source).tokenize()
    }

    @Test
    fun `test empty input`() {
        val tokens = lex("")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.EOF, tokens[0].type)
    }

    @Test
    fun `test punctuation tokens`() {
        val source = "{ } ( ) [ ] ; , = ... <- ."
        val tokens = lex(source)
        val types = tokens.map { it.type }
        val expectedTypes = listOf(
            TokenType.LEFT_BRACE,
            TokenType.RIGHT_BRACE,
            TokenType.LEFT_PAREN,
            TokenType.RIGHT_PAREN,
            TokenType.LEFT_BRACKET,
            TokenType.RIGHT_BRACKET,
            TokenType.SEMICOLON,
            TokenType.COMMA,
            TokenType.EQUALS,
            TokenType.VARARGS,
            TokenType.LEFT_ARROW,
            TokenType.DOT,
            TokenType.EOF
        )
        assertEquals(expectedTypes, types)
    }

    @Test
    fun `test string literal`() {
        val source = "\"hello world\""
        val tokens = lex(source)
        assertEquals(2, tokens.size) // STRING + EOF
        assertEquals(TokenType.STRING, tokens[0].type)
        assertEquals("hello world", tokens[0].lexeme)
    }

    @Test
    fun `test number literal`() {
        val source = "123456"
        val tokens = lex(source)
        assertEquals(2, tokens.size)
        assertEquals(TokenType.NUMBER, tokens[0].type)
        assertEquals("123456", tokens[0].lexeme)
    }

    @Test
    fun `test comment single line`() {
        val source = "// this is comment\npkg"
        val tokens = lex(source)
        assertTrue(tokens.any { it.type == TokenType.COMMENT && it.lexeme.contains("this is comment") })
        assertTrue(tokens.any { it.type == TokenType.PKG })
    }

    @Test
    fun `test comment block and javadoc`() {
        val block = "/* block comment */"
        val javadoc = "/** javadoc comment */"
        val blockTokens = lex(block)
        val javadocTokens = lex(javadoc)

        assertTrue(blockTokens.any { it.type == TokenType.COMMENT && it.lexeme.contains("block comment") })
        assertTrue(javadocTokens.any { it.type == TokenType.JAVADOC && it.lexeme.contains("javadoc comment") })
    }

    @Test
    fun `test context sensitive keywords`() {
        // In package name context, 'class' is identifier
        val source = "pkg class"
        val tokens = lex(source)
        assertEquals(TokenType.PKG, tokens[0].type)
        assertEquals(TokenType.IDENTIFIER, tokens[1].type)
    }

    @Test
    fun `test unexpected character throws`() {
        val source = "@"
        val exception = assertThrows<RuntimeException> {
            lex(source)
        }
        assertTrue(exception.message?.contains("Unexpected character") == true)
    }
}
