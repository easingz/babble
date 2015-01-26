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
        Token t = mLexer.next();
        boolean ret = t.type == type;
        if (!ret) {
            mLexer.pushBack(t);
            logError("token", t.loc);
        }
        return ret;
    }

    private boolean matchToken(TokenType type, String text) throws ParseException {
        assert text != null;
        Logger.d("parser", "trying to match : " + text);
        Token t = mLexer.next();
        boolean ret = t.type == type && t.text.equals(text);
        if (!ret) {
            mLexer.pushBack(t);
            logError("token " + t.text, t.loc);
        }
        return ret;
    }

    private boolean matchBlock() throws ParseException {
        logStart("block");
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN && "{".equals(t.text)) {
            return matchStatements() && matchLastStatment() && matchToken(TokenType.SIGN, "}");
        } else {
            mLexer.pushBack(t);
            return matchStatement();
        }
    }

    private boolean matchLastStatment() throws ParseException {
        logStart("last statement");
        Token t = mLexer.next();
        if (t.type == TokenType.KEYWORD && "return".equals(t.text)) {
            return matchExpression() && matchStmtSeparator();
        }
        mLexer.pushBack(t);
        return true;
    }

    private boolean matchStatements() throws ParseException {
        logStart("statements");
        Token t = mLexer.next();
        mLexer.pushBack(t);
        if (t.type == TokenType.END || t.type == TokenType.KEYWORD && "return".equals(t.text)) {
            // match epslon
            return true;
        } else {
            return matchStatement() && matchStmtSeparator() && matchStatements();
        }
    }

    private boolean matchStmtSeparator() throws ParseException {
        logStart("statement separator");
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN && ";".equals(t.text)) {
            return true;
        }
        mLexer.pushBack(t);
        return true;
    }

    private boolean matchStatement() throws ParseException {
        logStart("statement");
        Token t = mLexer.next();
        if (t.type == TokenType.IDENTIFIER) {
            Token ahead = mLexer.next();
            mLexer.pushBack(ahead);
            mLexer.pushBack(t);
            if (ahead.type == TokenType.SIGN && "(".equals(ahead.text)) {
                Logger.d("parser", "it's a function call");
                return matchFunctionCall() && matchMoreThanFuncCall();
            } else {
                Logger.d("parser", "it's an assignment");
                return matchAssignment();
            }
        } else if (t.type == TokenType.KEYWORD) {
            // 'while', 'if', 'for', 'function', 'local'
            mLexer.pushBack(t);
            switch (t.text) {
            case "while" : return matchWhile();
            case "if" : return matchIf();
            case "for" : return matchFor();
            case "function" : return matchFunctionDef();
            case "local" : return matchLocalDecl();
            default: //no-op
            }
        }
        mLexer.pushBack(t);
        logError("statement", t.loc);
        return false;
    }

    private boolean matchFunctionCall() throws ParseException {
        logStart("function call");
        Token t = mLexer.next();
        if (t.type == TokenType.IDENTIFIER) {
            Logger.d("parser", "call fuction: " + t.text);
            return matchArguments();
        }
        mLexer.pushBack(t);
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
            case NIL:
            case FALSE:
            case NUMBER:
            case STRING:
                Logger.d("parser", "got factor: " + t.text);
                return true;
            case KEYWORD:
                if ("function".equals(t.text)) {
                    mLexer.pushBack(t);
                    return matchAnonymousFunction();
                } else {
                    logError("factor: keyword " + t.text + " not allowed.", t.loc);
                    mLexer.pushBack(t);
                    return false;
                }
            case OPERATOR:
                mLexer.pushBack(t);
                return matchUniqExpr();
            case SIGN:
                if ("{".equals(t.text)) {
                    mLexer.pushBack(t);
                    return matchMap();
                }
                // maybe '(' else fall thru
            case IDENTIFIER:
                Token ahead = mLexer.next();
                mLexer.pushBack(ahead);
                mLexer.pushBack(t);
                if (ahead.type == TokenType.SIGN && "(".equals(ahead.text)) {
                    Logger.d("parser", "it's a prefix expression");
                    return matchPrefixExpr();
                } else {
                    Logger.d("parser", "it's a variable");
                    return matchVar();
                }
            default:
                mLexer.pushBack(t);
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
            mLexer.pushBack(ahead);
            mLexer.pushBack(t);
            if (ahead.type == TokenType.SIGN && "(".equals(ahead.text)) {
                // only use the right value attribute of variable with function call start
                // creepy hack for LL grammar
                return matchFunctionCall() && matchVar2();
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
            Logger.d("parser", "got an identifier: " + t.text);
            return matchVar2();
        } else if (t.type == TokenType.SIGN && "(".equals(t.text)) {
            return matchExpression() && matchToken(TokenType.SIGN, ")") && matchVar1();
        }
        mLexer.pushBack(t);
        logError("variable", t.loc);
        return false;
    }

    private boolean matchVar2() throws ParseException {
        logStart("optional variable of expression");
        Token t = mLexer.next();
        mLexer.pushBack(t);
        if (t.type == TokenType.SIGN && ("[".equals(t.text) || ".".equals(t.text))) {
            return matchVar1();
        }
        return true;
    }

    private boolean matchVar1() throws ParseException {
        logStart("variable of expression");
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN) {
            if ("[".equals(t.text)) {
                return matchExpression() && matchToken(TokenType.SIGN, "]") && matchVar2();
            } else if (".".equals(t.text)) {
                return matchToken(TokenType.IDENTIFIER) && matchVar2();
            }
        }
        mLexer.pushBack(t);
        logError("variable of expression", t.loc);
        return false;
    }

    private boolean matchVarContinue() throws ParseException {
        logStart("more variables");
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN && ",".equals(t.text)) {
            return matchVarList();
        }
        mLexer.pushBack(t);
        return true;
    }

    private boolean matchWhile() throws ParseException {
        logStart("while statement");
        return matchToken(TokenType.KEYWORD, "while") && matchExpression() && matchBlock();
    }

    private boolean matchIf() throws ParseException {
        logStart("if statement");
        return matchToken(TokenType.KEYWORD, "if") && matchExpression() &&
            matchToken(TokenType.KEYWORD, "then") && matchBlock() && matchElifStatement() &&
            matchElseStatement();
    }

    private boolean matchElifStatement() throws ParseException {
        logStart("elif statement");
        Token t = mLexer.next();
        if (t.type == TokenType.KEYWORD && "elif".equals(t.text)) {
            return matchExpression() && matchToken(TokenType.KEYWORD, "then") &&
                matchBlock() && matchElifStatement();
        }
        mLexer.pushBack(t);
        return true;
    }

    private boolean matchElseStatement() throws ParseException {
        logStart("else statement");
        Token t = mLexer.next();
        if (t.type == TokenType.KEYWORD && "else".equals(t.text)) {
            return matchBlock();
        }
        mLexer.pushBack(t);
        return true;
    }

    private boolean matchFor() throws ParseException {
        logStart("for statement");
        Token t = mLexer.next();
        Token id = mLexer.next();
        if (t.type == TokenType.KEYWORD && "for".equals(t.text) && id.type == TokenType.IDENTIFIER) {
            Token ahead = mLexer.next();
            if (ahead.type == TokenType.SIGN && "=".equals(ahead.text)) {
                return matchExpression() && matchToken(TokenType.SIGN, ",") &&
                    matchExpression() && matchOptionalStep() && matchBlock();
            } else if (ahead.type == TokenType.SIGN && ",".equals(ahead.text) ||
                       ahead.type == TokenType.KEYWORD && "in".equals(ahead.text)) {
                mLexer.pushBack(ahead);
                mLexer.pushBack(id);
                return matchIdList() && matchToken(TokenType.KEYWORD, "in") &&
                    matchExprList() && matchBlock();
            }
            logError("for", ahead.loc);
        }
        mLexer.pushBack(id);
        mLexer.pushBack(t);
        logError("for", t.loc);
        return false;
    }

    private boolean matchIdList() throws ParseException {
        logStart("Identifier list");
        return matchToken(TokenType.IDENTIFIER) && matchIdContinue();
    }

    private boolean matchIdContinue() throws ParseException {
        logStart("more identifiers");
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN && ",".equals(t.text)) {
            return matchIdList();
        }
        mLexer.pushBack(t);
        return true;
    }

    private boolean matchOptionalStep() throws ParseException {
        logStart("optional step");
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN && ",".equals(t.text)) {
            return matchExpression();
        }
        mLexer.pushBack(t);
        return true;

    }

    private boolean matchFunctionDef() throws ParseException {
        logStart("function defination");
        return matchToken(TokenType.KEYWORD, "function") && matchToken(TokenType.IDENTIFIER) &&
            matchFunctionBody();
    }

    private boolean matchFunctionBody() throws ParseException {
        logStart("function body");
        return matchToken(TokenType.SIGN, "(") && matchIdList() &&
            matchToken(TokenType.SIGN, ")") && matchBlock();
    }

    private boolean matchLocalDecl() throws ParseException {
        logStart("local declarition");
        Token t = mLexer.next();
        Token ahead = mLexer.next();
        mLexer.pushBack(ahead);
        if (t.type == TokenType.KEYWORD && "local".equals(t.text)) {
            if (ahead.type == TokenType.KEYWORD && "function".equals(t.text)) {
                return matchFunctionDef();
            } else if (ahead.type == TokenType.IDENTIFIER) {
                return matchIdList() && matchToken(TokenType.SIGN, "=") &&
                    matchExprList();
            }
        }
        mLexer.pushBack(t);
        logError("local declaration", t.loc);
        return false;
    }

    private boolean matchFieldList() throws ParseException {
        logStart("field list");
        Token t = mLexer.next();
        mLexer.pushBack(t);
        if (t.type == TokenType.SIGN && "}".equals(t.text)) {
            return true;
        } else {
            return matchField() && matchFieldContinue();
        }
    }

    private boolean matchField() throws ParseException {
        logStart("field");
        Token t = mLexer.next();
        Token ahead = mLexer.next();
        if (t.type == TokenType.IDENTIFIER && ahead.type == TokenType.SIGN &&
            "=".equals(ahead.text)) {
            Logger.d("parser", "got a field key: " + t.text);
            return matchExpression();
        } else {
            mLexer.pushBack(ahead);
            mLexer.pushBack(t);
            return matchExpression();
        }
    }

    private boolean matchFieldContinue() throws ParseException {
        logStart("more fields");
        Token t = mLexer.next();
        if (t.type == TokenType.SIGN && ",".equals(t.text)) {
            return matchFieldList();
        }
        mLexer.pushBack(t);
        return true;
    }

    private boolean matchMoreThanFuncCall() throws ParseException {
        logStart("more than function call");
        Token t = mLexer.next();
        mLexer.pushBack(t);
        if (t.type == TokenType.SIGN && ("[".equals(t.text) || ".".equals(t.text))) {
            return matchVar1() && matchVarContinue() && matchToken(TokenType.SIGN, "=") &&
                matchExprList();
        }
        // match epslon
        return true;
    }

    private void logStart(String s) {
        Logger.d("parser", "Start parsing " + s);
    }

    private void logError(String s, Token.Location loc) {
        Logger.d("parser", "error happens when parsing " + s
                 + " at row " + loc.row + " col " + loc.row);
    }
}
