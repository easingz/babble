// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-

package com.easing.babble;

class Parser {
    private Lexer mLexer;

    public Parser(Lexer lexer) {
	mLexer = lexer;
    }

    public boolean parse() {
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
}
