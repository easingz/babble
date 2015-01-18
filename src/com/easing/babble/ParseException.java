// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-

package com.easing.babble;

import java.util.Locale;

class ParseException extends Exception {

    public ParseException(String msg) {
	super(String.format(Locale.US, "Error occurs while parsing: %s", msg));
    }
}
