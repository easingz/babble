// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-

package com.easing.babble;

import java.io.IOException;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

class SourceFileReader implements SourceReader {

    private String mSource;
    private int mPosition;
    private final int LENGTH;

    public SourceFileReader(String filename) {
        mSource = readFile(filename);
        mPosition = 0;
        LENGTH = mSource.length();
    }

    public char peek() {
        if (mPosition < LENGTH) {
            return mSource.charAt(mPosition);
        }
        return '\0';
    }

    public void advance() {
        if (mPosition < LENGTH) {
            mPosition++;
        }
    }

    private String readFile(String filename) {
        try {
            return new String(readAllBytes(get(filename)));
        } catch (IOException e) {
            Logger.d("source", "Error when read source file: " + filename);
        }
        return "";
    }
}
