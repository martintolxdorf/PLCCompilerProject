package plc.compiler;

import javax.swing.plaf.nimbus.State;
import java.awt.image.TileObserver;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the tokens and returns the parsed AST.
     */
    public static Ast parse(List<Token> tokens) throws ParseException {
        return new Parser(tokens).parseSource();
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Statement> parseList= new ArrayList<Ast.Statement>();
        while(tokens.has(0)) {
            parseList.add(parseStatement());
        }
        return new Ast.Source(parseList);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, assignment, if, or while
     * statement, then it is an expression statement. See these methods for
     * clarification on what starts each type of statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if(peek(Token.Type.IDENTIFIER)) {
            if (peek("IF")) {
                return parseIfStatement();
            } else if (peek("WHILE")) {
                return parseWhileStatement();
            } else if (peek("LET")) {
                return parseDeclarationStatement();
            } else if(peekPlus("=")) {
                return parseAssignmentStatement();
            }
        }

        return parseExpressionStatement();

    }

    /**
     * Parses the {@code expression-statement} rule. This method is called if
     * the next tokens do not start another statement type, as explained in the
     * javadocs of {@link #parseStatement()}.
     */
    public Ast.Statement.Expression parseExpressionStatement() throws ParseException {
        Ast.Expression expression = parseExpression();
        while(!peek(Token.Type.OPERATOR) && !peek(";")){
            expression = new Ast.Expression.Group(expression);
        }
        match(";");
        return new Ast.Statement.Expression(expression);
    }

    /**
     * Parses the {@code declaration-statement} rule. This method should only be
     * called if the next tokens start a declaration statement, aka {@code let}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        match("LET");

        if(!peek(Token.Type.IDENTIFIER)){
            throw new ParseException("missing identifier after let",tokens.index);
        }
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);

        if(!peek(Token.Type.OPERATOR) || !match(":")){
            throw new ParseException("missing : operator",tokens.index);
        }

        if(!peek(Token.Type.IDENTIFIER)){
            throw new ParseException("missing identifier after :",tokens.index);
        }
        String type = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);

        if(peek(Token.Type.OPERATOR) && peek("=")){
            match(Token.Type.OPERATOR);
            Ast.Expression expression = parseExpression();
            while(!peek(Token.Type.OPERATOR) && !peek(";")){
                expression = new Ast.Expression.Group(expression);
            }
            return new Ast.Statement.Declaration(name,type,Optional.of(expression));
        }else if(peek(Token.Type.OPERATOR) && match(";")){
            return new Ast.Statement.Declaration(name,type,Optional.empty());
        }

        throw new ParseException("missing ; at end", tokens.index);
    }

    /**
     * Parses the {@code assignment-statement} rule. This method should only be
     * called if the next tokens start an assignment statement, aka both an
     * {@code identifier} followed by {@code =}.
     */
    public Ast.Statement.Assignment parseAssignmentStatement() throws ParseException {
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);
        if(!peek(Token.Type.OPERATOR) || !match("=")){
            throw new ParseException("missing equals", tokens.index);
        }
        if(peek(Token.Type.OPERATOR) && peek(";")){
            throw new ParseException("missing expressiom", tokens.index);
        }
        Ast.Expression expression = parseExpression();
        while(!peek(Token.Type.OPERATOR) && !peek(";")){
            expression = new Ast.Expression.Group(expression);
        }
        return new Ast.Statement.Assignment(name,expression);
    }

    /**
     * Parses the {@code if-statement} rule. This method should only be called
     * if the next tokens start an if statement, aka {@code if}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        match("IF");

        Ast.Expression expression = parseExpression();
        while(!peek(Token.Type.IDENTIFIER) && !peek("THEN")){
            expression = new Ast.Expression.Group(expression);
        }

        match(Token.Type.IDENTIFIER);

        List<Ast.Statement> thenStatements = new ArrayList<>();
        while(!(peek(Token.Type.IDENTIFIER) && (peek("ELSE") || peek("END")))){
            thenStatements.add(parseStatement());
        }

        List<Ast.Statement> elseStatements = new ArrayList<>();
        while (peek(Token.Type.IDENTIFIER) && peek("ELSE")){
            match(Token.Type.IDENTIFIER);
            elseStatements.add(parseStatement());
        }

        if(peek(Token.Type.IDENTIFIER) && peek("END")){
            return new Ast.Statement.If(expression,thenStatements,elseStatements);
        }

        throw new ParseException("missing END", tokens.index);
    }

    /**
     * Parses the {@code while-statement} rule. This method should only be
     * called if the next tokens start a while statement, aka {@code while}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        match("WHILE");

        Ast.Expression condition = parseExpression();
        while(!peek(Token.Type.IDENTIFIER) && !peek("DO")){
            condition = new Ast.Expression.Group(condition);
        }
        match("DO");

        List<Ast.Statement> statements = new ArrayList<>();
        while(!(peek(Token.Type.IDENTIFIER) && peek("END"))){
            statements.add(parseStatement());
        }

        return new Ast.Statement.While(condition, statements);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseEqualityExpression();
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseEqualityExpression() throws ParseException {
        Ast.Expression expression = parseAdditiveExpression();
        if(!peek(Token.Type.OPERATOR) || !(peek("!=") || peek("=="))){
            return expression;
//            throw new ParseException("missing operator", tokens.index);
        }
        String operator = tokens.get(0).getLiteral();
        match(Token.Type.OPERATOR);
        Ast.Expression right = parseAdditiveExpression();
        expression = new Ast.Expression.Binary(operator, expression, right);
        while(peek(Token.Type.OPERATOR) && (peek("==") || peek("!="))){
            operator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            right = parseAdditiveExpression();
            expression = new Ast.Expression.Binary(operator, expression, right);
        }
        return expression;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression expression = parseMultiplicativeExpression();
        if(!peek(Token.Type.OPERATOR) || !(peek("+") || peek("-"))){
            return expression;
//            throw new ParseException("missing operator", tokens.index);
        }
        String operator = tokens.get(0).getLiteral();
        match(Token.Type.OPERATOR);
        Ast.Expression right = parseMultiplicativeExpression();
        expression = new Ast.Expression.Binary(operator, expression, right);
        while(peek(Token.Type.OPERATOR) && (peek("+") || peek("-"))){
            operator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            right = parseMultiplicativeExpression();
            expression = new Ast.Expression.Binary(operator, expression, right);
        }
        return expression;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression expression = parsePrimaryExpression();
        if(!peek(Token.Type.OPERATOR) || !(peek("*") || peek("/"))){
            return expression;
//            throw new ParseException("missing operator", tokens.index);
        }
        String operator = tokens.get(0).getLiteral();
        match(Token.Type.OPERATOR);
        Ast.Expression right = parsePrimaryExpression();
        expression = new Ast.Expression.Binary(operator, expression, right);
        while(peek(Token.Type.OPERATOR) && (peek("*") || peek("/"))){
            operator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            right = parsePrimaryExpression();
            expression = new Ast.Expression.Binary(operator, expression, right);
        }
        return expression;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if(peek(Token.Type.IDENTIFIER) && (peek("TRUE")) || peek("FALSE")){
            Object temp;
            if(peek("TRUE")){
                temp = true;
            }else{
                temp = false;
            }
            match(Token.Type.IDENTIFIER);
            return new Ast.Expression.Literal(temp);
        }else if(peek(Token.Type.INTEGER)){
            Object temp = new BigInteger(tokens.get(0).getLiteral());
            match(Token.Type.INTEGER);
            return new Ast.Expression.Literal(temp);
        }else if(peek(Token.Type.DECIMAL)){
            Object temp = new BigDecimal(tokens.get(0).getLiteral());
            match(Token.Type.DECIMAL);
            return new Ast.Expression.Literal(temp);
        }else if(peek(Token.Type.STRING)){
            String temp = tokens.get(0).getLiteral();
            temp = temp.substring(1,temp.length()-1);
            match(Token.Type.STRING);
            return new Ast.Expression.Literal(temp);
        }else if(peek(Token.Type.OPERATOR) && peek("(")){
            match("(");
            Ast.Expression expression = parseExpression();
            if(!peek(Token.Type.OPERATOR) || !match(")")){
                throw new ParseException("missing )", tokens.index);
            }
            return new Ast.Expression.Group(expression);
        }else if(peek(Token.Type.IDENTIFIER)){
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            List<Ast.Expression> arguments = new ArrayList<>();
            if(!peek(Token.Type.OPERATOR) || !peek("(")){
                return new Ast.Expression.Variable(name);
            }
            match("(");
            if(peek(Token.Type.OPERATOR) && peek(")")){
                return new Ast.Expression.Function(name, arguments);
            }
            arguments.add(parseExpression());
            while(peek(Token.Type.OPERATOR) && peek(",")){
                match(",");
                while(!peek(Token.Type.OPERATOR) && !peek(")")){
                    arguments.add(parseExpression());
                }
                match(")");
            }

            return new Ast.Expression.Function(name, arguments);
        }

        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError();
            }
        }
        return true;
    }

    private boolean peekPlus(Object... patterns) {
        tokens.index++;
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                tokens.index--;
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    tokens.index--;
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    tokens.index--;
                    return false;
                }
            } else {
                throw new AssertionError();
            }
        }
        tokens.index--;
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
