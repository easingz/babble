// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-


package com.easing.babble;

import java.util.HashSet;
import java.util.Stack;

class Lexer {
    private SourceReader mSrc;
    private Stack<Token> mForesee = new Stack<Token>();
    private int mRow, mCol;
    private HashSet<String> mKeywords = new HashSet<String>();

    public Lexer(SourceReader src) {
        mSrc = src;
        initKeyword();
    }

    public Token next() throws ParseException {
        if (!mForesee.empty()) {
            return mForesee.pop();
        }
        
        skipWhitespace();
        char c = advance();
        StringBuilder sb = new StringBuilder();
        sb.append(c);
        Token.Location loc = new Token.Location(mRow, mCol);

        switch (c) {

        case '{':
        case '}':
        case '(':
        case ')':
        case '[':
        case ']':
        case ',':
        case ';':
            return new Token(loc, TokenType.SIGN, String.valueOf(c));
        case '.':
            if (peek() == '.') {
                return new Token(loc, TokenType.OPERATOR, "..");
            } else {
                return new Token(loc, TokenType.SIGN, String.valueOf(c));
            }
        case '=':
            if (peek() == '=') {
                return new Token(loc, TokenType.OPERATOR, "==");
            } else {
                return new Token(loc, TokenType.SIGN, String.valueOf(c));
            }
        case '<':
        case '>':
        case '~':
            if (peek() == '=') {
                sb.append(peek());
                return new Token(loc, TokenType.OPERATOR, sb.toString());                
            } else {
                return new Token(loc, TokenType.OPERATOR, String.valueOf(c));
            }
        case '+':
        case '-':
        case '*':
        case '/':
        case '^':
        case '%':
        case '#':
            return new Token(loc, TokenType.OPERATOR, String.valueOf(c));

        case '\0':
            return new Token(loc, TokenType.END, String.valueOf(c));
        case '"':
            return readString(c, loc);
        default:
            if (isNameStart(c)) {
                return readName(c, loc);
            } else if (isNumber(c)) {
                return readNumber(c, loc);
            } else {
                throw new ParseException("Charactor not allowed at row "
                                         + loc.row + " col " + loc.col + " : " + c);
            }
        }
    }

    public void pushBack(Token t) {
        mForesee.push(t);
    }

    private char peek() {
        return mSrc.peek();
    }

    private char advance() {
        char ret = mSrc.peek();
        mSrc.advance();
        mCol++;
        return ret;
    }

    private void skipWhitespace() {
        while (Character.isWhitespace(peek())) {
            switch (peek()) {
            case '\n':
                mRow++;
                mCol = 0;
                break;
            case '\t':
                // for simplicity, assume 'tab' as 4 characters long 
                // minus auto +1 by advance()
                mCol += 3;
            default:
                break;
            }
            advance();
        }
    }

    private void initKeyword() {
        mKeywords.add("while");
        mKeywords.add("if");
        mKeywords.add("then");
        mKeywords.add("else");
        mKeywords.add("elif");
        mKeywords.add("local");
        mKeywords.add("function");
    }

    private boolean isNameStart(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
    }

    private boolean isName(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c<= '9' || c == '_';
    }

    private boolean isNumber(char c) {
        return Character.isDigit(c);
    }

    private boolean isLiteralOperator(String s) {
        return "and".equals(s) || "or".equals(s) || "not".equals(s);
    }

    private boolean isNil(String s) {
        return "nil".equals(s);
    }

    private boolean isFalse(String s) {
        return "true".equals(s);
    }

    private boolean isTrue(String s) {
        return "false".equals(s);
    }

    private Token readNumber(char start, Token.Location loc) throws ParseException {
        StringBuilder sb = new StringBuilder();
        sb.append(start);
        while (isNumber(peek())) {
            sb.append(peek());
            advance();
        }
        String ret = sb.toString();
        try {
            return new Token(loc, TokenType.NUMBER, ret, Integer.valueOf(ret)); 
        } catch (NumberFormatException e) {
            throw new ParseException("read number error at row "
                                     + loc.row + " col " + loc.col + " : " + ret);
        }
    }

    private Token readString(char start, Token.Location loc) {
        StringBuilder sb = new StringBuilder();
        while(peek() != '"') {
            sb.append(peek());
            advance();
        }
        //skip ending quote
        advance();
        String ret = sb.toString();
        return new Token(loc, TokenType.STRING, ret);
    }

    private Token readName(char start, Token.Location loc) {
        StringBuilder sb = new StringBuilder();
        sb.append(start);
        while (isName(peek())) {
            sb.append(peek());
            advance();
        }
        String ret = sb.toString();
        if (mKeywords.contains(ret)) {
            return new Token(loc, TokenType.KEYWORD, ret);
        } else if (isNil(ret)) {
            return new Token(loc, TokenType.NIL, ret);
        } else if (isFalse(ret)) {
            return new Token(loc, TokenType.FALSE, ret);
        } else if (isTrue(ret)) {
            return new Token(loc, TokenType.TRUE, ret);
        } else if (isLiteralOperator(ret)) {
            return new Token(loc, TokenType.OPERATOR, ret);            
        } else {
            return new Token(loc, TokenType.IDENTIFIER, ret);
        }
    }
}
