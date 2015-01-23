// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-

package com.easing.babble;

class Parser {
    private Lexer mLexer;

    public Parser(Lexer lexer) {
        mLexer = lexer;
    }

    public boolean dummyParse() {
        try {
            Token t = mLexer.next();
            while (t.type != TokenType.END) {
                Logger.d("parser", "read token at row "
                         + t.loc.row + " col " + t.loc.col + " : " + t.text);
                t = mLexer.next();
            }
            return true;
        } catch (ParseException e) {
            Logger.d("lexer", e.getMessage());
        }
        return false;
    }

    public boolean parse() {
        try {
            return matchStatements() && matchEnd();
        } catch (ParseException e) {
            Logger.d("parser", e.getMessage());
        }
        return false;
    }

    private boolean matchEnd() throws ParseException {
        logStart("end");
        return matchToken(TokenType.END);
    }

    private boolean matchToken(TokenType type) throws ParseException {
        return mLexer.next().type == type;
    }

    private boolean matchToken(TokenType type, String text) throws ParseException {
        assert text != null;
        Token t = mLexer.next();
        return t.type == type && t.text.equals(text);
    }

    private boolean matchStatements() throws ParseException {
        logStart("statements");
        Token t = mLexer.next();
        mLexer.pushBack(t);
        if (matchStatement()) {
            return matchToken(TokenType.SIGN, ";") && matchStatements();
        }
        return true;
    }

    private boolean matchStatement() throws ParseException {
        logStart("statement");
        Token t = mLexer.next();
        if (t.type == TokenType.IDENTIFIER) {
            // assignment or function call, should look ahead for '(' to differ
            Token ahead = mLexer.next();
            mLexer.pushBack(t);
            mLexer.pushBack(ahead);
            if (ahead.type == TokenType.SIGN && ahead.text == "(") {
                return matchFunctionCall();
            } else {
                return matchAssignment();
            }
        } else if (t.type == TokenType.KEYWORD) {
            // 'while', 'if', 'for', 'function', 'local'
            return matchWhile() || matchIf() || matchFor() ||
                matchFunctionDef() || matchLocalDecl();
        }
        logError("statement", t.loc);
        return false;
    }

    private boolean matchFunctionCall() throws ParseException {
        logStart("function call");
        Token t = mLexer.next();
        if (t.type = TokenType.IDENTIFIER) {
            Logger.d("parser", "call fuction: " + t.text);
            return matchArguments();
        }
        logError("function call", t.loc);
        return false;
    }

    private boolean matchArguments() throws ParseException {
        logStart("function arguments");
        return matchToken(TokenType.SIGN, "(") && matchExprList() &&
            matchToken(TokenType.SIGN, ")");
    }

    private boolean matchExprList() throws ParseException {
        logStart("expression list");
        return matchExpression() && matchExprContinue();
    }

    private boolean matchExpression() throws ParseException {
        logStart("expression");
        return matchFactor() && matchBinaryExpr();
    }

    private boolean matchFactor() throws ParseException {
        logStart("factor");
        Token t = mLexer.next();
        switch (t.type) {
            case TokenType.NIL:
            case TokenType.FALSE:
            case TokenType.NUMBER:
            case TokenType.STRING:
                Logger.d("parser", "got factor: " + t.text);
                return true;
            case TokenType.KEYWORD:
                if ("function".equals(t.text)) {
                    mLexer.pushBack(t);
                    return matchAnonymousFunction();
                } else {
                    return false;
                }
            case TokenType.OPERATOR:
                mLexer.pushBack(t);
                return matchUniqExpr();
            case TokenType.SIGN:
                if ("{".equals(t.text)) {
                    mLexer.pushBack(t);
                    return matchMap();
                }
                // maybe '(' else fall thru
            case TokenType.IDENTIFIER:
                mLexer.pushBack();
                return matchVar();
            default:
                logError("factor", t.loc);
                return false;
        }
    }

    private boolean matchUniqExpr() throws ParseException {
        logStart("unique expression");
        Token t = mLexer.next();
        if ("-".equals(t.text) || "#".equals(t.text) || "not".equals(t.text)) {
            Logger.d("parser", "got unique oeprator: " + t.text);
            return matchExpression();
        }
        mLexer.pushBack(t);
        logError("unique expression", t.loc);
        return false;
    }

    private boolean matchMap() throws ParseException {
        logStart("map");
        Token t = mLexer.next();
        if ("{".equals(t.text)) {
            return matchFieldList() && matchToken(TokenType.SIGN, "}");
        }
        mLexer.pushBack(t);
        logError("map", t.loc);
        return false;
    }

    private boolean matchAnonymousFunction() throws ParseException {
        logStart("anonymous function");
        Token t = mLexer.next();
        if ("function".equals(t.text)) {
            return matchFunctionBody();
        }
        mLexer.pushBack(t);
        logError("anonymous function", t.loc);
        return false;        
    }

    private boolean matchPrefixExpr() throws ParseException {
        logStart("prefix expression");
        Token t = mLexer.next();
        if (t.type == TokenType.IDENTIFIER) {
            Token ahead = mLexer.next();
            mLexer.pushBack(t);
            mLexer.pushBack(ahead);
            if (ahead.type == TokenType.SIGN && ahead.text == "(") {
                return matchFunctionCall();
            } else {
                return matchVar();
            }
        } else if (t.type == TokenType.SIGN && "(".equals(t.text)) {
            Logger.d("parser", "got a prefix expression starting with ( ");
            return matchExpression() && matchToken(TokenType.SIGN, ")");
        }
        mLexer.pushBack(t);
        logError("prefix expression", t.loc);
        return false;
    }

    private boolean matchBinaryExpr() throws ParseException {
        logStart("binary expresion");
        Token t = mLexer.next();
        if (t.type == TokenType.OPERATOR) {
            Logger.d("parser", "got binary operator: " + t.text);
            return matchExpression();
        }
        mLexer.pushBack(t);
        return true;
    }

    private boolean matchExprContinue() throws ParseException {
        logStart("more expressions");
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN && t.text.equals(",")) {
            return matchExprList();
        }
        mLexer.pushBack(t);
        return true;
    }

    private boolean matchAssignment() throws ParseException {
        logStart("assignment");
        return matchVarList() && matchToken(TokenType.SIGN, "=") && matchExprList();
    }

    private boolean matchVarList() throws ParseException {
        logStart("variable list");
        return matchVar() && matchVarContinue();
    }

    private boolean matchVar() throws ParseException {
        logStart("variable");
        Token t = mLexer.next();
        if (t.type == TokenType.IDENTIFIER) {
            logger.d("parser", "got an identifier: " + t.text);
            return matchVarExpr();
        } else if (t.type == TokenType.SIGN && "(".equals(t.text)) {
            return matchExpression() && matchToken(TokenType.SIGN, ")") && matchVarExpr();
        }
        mLexer.pushBack(t);
        logError("variable", t.loc);
        return false;
    }

    private boolean matchVarExpr() throws ParseException {
        logStart("variable of expression");
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN) {
            if ("[".equals(t.text)) {
                return matchExpression() && matchToken(TokenType.SIGN, "]") && matchVarExpr();
            } else if (".".equals(t.text)) {
                return matchToken(Token.IDENTIFIER) && matchVarExpr();
            } else if ("(".equals(t.text)) {
                mLexer.pushBack(t);
                return matchFunctionBody() && matchVarExpr();
            }
        }
        mLexer.pushBack(t);
        return true;
    }

    private boolean matchVarContinue() throws ParseException {
        logStart("more variables");
    }

    private boolean matchWhile() throws ParseException {
        logStart("while statement");
    }

    private boolean matchIf() throws ParseException {
        logStart("if statement");

    }

    private boolean matchFor() throws ParseException {
        logStart("for statement");
    }

    private boolean matchFunctionDef() throws ParseException {
        logStart("function defination");
    }

    private boolean matchFunctionBody() throws ParseException {
        logStart("function body");

    }

    private boolean matchLocalDecl() throws ParseException {
        logStart("local declarition");
    }

    private boolean matchFieldList() throws ParseException {
        logStart("field list");

    }

    private void logStart(String s) {
        Logger.d("parser", "Start parsing " + s);
    }

    private void logError(String s, Token.Location loc) {
        Logger.d("parser", "error happens when parsing " + s
                 + " at row " + loc.row + " col " + loc.row);
    }
}
