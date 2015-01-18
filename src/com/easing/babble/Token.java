// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-

package com.easing.babble;

class Token {

    static class Location {
        public int row, col;

        public Location(int r, int c) {
            row = r;
            col = c;
        }
    }

    Location loc;
    TokenType type;
    String text;
    Object val;

    public Token(Location loc, TokenType type, String text, Object val) {
	this.loc = loc;
	this.type = type;
	this.text = text;
	this.val = val;
    }

    public Token(Location loc, TokenType type, String text) {
	this(loc, type, text, null);
    }
}
