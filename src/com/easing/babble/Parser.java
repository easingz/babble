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
        Logger.d("parser", "start parsing end");
        return matchToken(TokenType.END);
    }

    private boolean matchToken(TokenType type) throws ParseException {
        return mLexer.next().type == type;
    }

    private boolean matchStatements() throws ParseException {
        Logger.d("parser", "start parsing statements");
        Token t = mLexer.next();
        mLexer.pushBack(t);
        if (matchStatement()) {
            return matchStmtSeparator() && matchStatements();
        }
        return true;
    }

    private boolean matchStatement() throws ParseException {
        Logger.d("parser", "starting parse statement");
        Token t = mLexer.next();

    }
}
