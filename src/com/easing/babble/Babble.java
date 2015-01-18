// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-

package com.easing.babble;

public class Babble {

    public static void main(String args[]) {
        new Parser(new Lexer(new SourceFileReader(args[0]))).parse();
    }
}
