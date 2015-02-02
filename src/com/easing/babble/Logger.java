// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-

package com.easing.babble;

import java.util.HashSet;

class Logger {
    private static final String SCOPES[] = {
        "parser",
        "lexer",
        "ast",
        "symbol",
        "source",
        "babble",
        "parser2",
    };
    // for speed
    private static HashSet<String> mScopes = new HashSet<String>();

    static {
        for (String scope : SCOPES) {
            mScopes.add(scope);
        }
    }
    public static void d(String scope, String log) {
        if (mScopes.contains(scope)) {
            System.out.println('[' + scope + "] " + log);
        }
    }
}
