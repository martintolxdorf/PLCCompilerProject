package plc.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException}.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    final CharStream chars;

    Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Lexes the input and returns the list of tokens.
     */
    public static List<Token> lex(String input) throws ParseException {
        return new Lexer(input).lex();
    }

    /**
     * Repeatedly lexes the next token using {@link #lexToken()} until the end
     * of the input is reached, returning the list of tokens lexed. This should
     * also handle skipping whitespace.
     */
    List<Token> lex() throws ParseException {
        List<Token> tList = new ArrayList<>();
        while (chars.has(0)) {
            if (!match("[ \n\r\t]")) {
                tList.add(lexToken());
            } else {
                chars.skip();
            }
        }
        return tList; //TODO
    }

    Token lexToken() throws ParseException {
//        if(peek("^[=]{2}$","^[!][=]$","^[ \n\r\t]")){
        if (peek("[=]") && peekPlus("[=]")) {
            return lexOperator();
        }else  if(peek("[!]") && peekPlus("[=]")) {
            return lexOperator();
        } else if (peek("[0-9]") || peek("[.]")) {
            return lexNumber();
        } else if (match("[A-Za-z_]") || match("[A-Za-z0-9_]")) {
            return lexIdentifier();
        }else if (peek("\"")){
            return lexString();
        }else if(peek("[^ \n\r\t]")){
            return lexOperator();
        }else{
            throw new ParseException("invalid operand", chars.index);
        }
    }

    /**
     * Lexes an IDENTIFIER token. Unlike the previous project, fewer characters
     * are allowed in identifiers.
     */
    Token lexIdentifier() throws ParseException {
        while (match("[A-Za-z_]")) ;
        return chars.emit(Token.Type.IDENTIFIER);
    }

    /**
     * Lexes an INTEGER or DECIMAL token. Unlike the previous project, we now
     * have integers and decimals instead of just numbers, and leading zeros are
     * not allowed (throw an exception if found). Since both start in the same
     * way, we handle this through a single method and change the token type of
     * the emitted token.
     */
    Token lexNumber() throws ParseException {
        if(match("[+-]","[0-9]")) ;
        else if (match("[0-9]")) ;
        else throw new ParseException("starts with decimal", chars.index);

        int decimal = 0;
        while (peek("[.]","[0-9]") || match("[0-9]")){
            if(match("[.]","[0-9]")){
                decimal++;
            }
        }
        if(decimal>1){
            throw new ParseException("more than one decimal", chars.index);
        }else if(decimal == 1){
            return chars.emit(Token.Type.DECIMAL);
        }

        return chars.emit(Token.Type.INTEGER);
    }

    /**
     * Lexes a STRING token. Unlike the previous project, there are limited
     * characters allowed in strings and escape characters are not supported. If
     * the character is invalid a {@link ParseException} should be thrown.
     */
    Token lexString() throws ParseException {
        if(!peek("\"")){
            throw new ParseException("not an acceptable input", chars.index);
        }
        match("\"");
        while (match("[^\"]*"));
        if (!match("\"")) {
            throw new ParseException("no terminating end quote", chars.index);
        }
        return chars.emit(Token.Type.STRING);

    }

    /**
     * Lexes an OPERATOR token. Unlike the previous project, we have two
     * multi-character operators: {@code ==} and {@code !=}. If the next
     * characters match either of these, you should emit both characters as a
     * <em>single</em> OPERATOR. As before, this is a 'fallback' for any other
     * unknown characters.
     */
    Token lexOperator() throws ParseException {
//        while (match("[==]","[!=]","^[ \n\r\t]"));
        if (peek("[=]") && peekPlus("[=]")){
            while (match("[==]"));
        }else if (peek("[!]") && peekPlus("[=]")){
            while (match("[!=]"));
        }else{
            match("[^ \\n\\r\\t]");
        }

        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true for the sequence {@code 'a', 'b', 'c'}
     */
    boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    boolean peekPlus(String... patterns) {
        chars.index++;
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        chars.index--;
        return true;
    }

    /**
     * Returns true in the same way as peek, but also advances the CharStream too
     * if the characters matched.
     */
    boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * This is basically a sequence of characters. The index is used to maintain
     * where in the input string the lexer currently is, and the builder
     * accumulates characters into the literal value for the next token.
     */
     public static final class CharStream {

        final String input;
        int index = 0;
        int length = 0;

        CharStream(String input) {
            this.input = input;
        }

        /**
         * Returns true if there is a character at index + offset, as defined by
         * the length of the input.
         */
        public boolean has(int offset) {
            return index + offset < input.length();
        }

        /**
         * Gets the character at index + offset, throwing an exception if the
         * character does not exist.
         */
        public char get(int offset) {
            return input.charAt(index + offset); //throws if out of bounds
        }

        /**
         * Advances to the next character, incrementing the current index and
         * length of the literal being built.
         */
        public void advance() {
            index++;
            length++;
        }


        /**
         * Resets the length to zero, skipping any consumed characters.
         */
        public void skip() {
            length = 0;
        }

        /**
         * Returns a token of the given type with the built literal. The index
         * of the token should be the starting index.
         */
        public Token emit(Token.Type type) {
            int start = index - length;
            skip(); //we've saved the starting point already
            return new Token(type, input.substring(start, index), start);
        }

    }
}
