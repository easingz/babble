// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-

package com.easing.babble.ast;

class ProgramNode extends Node {
    public ProgramNode(Node... childs) {
        super(childs);
    }

    @Override
    public boolean valid() {
        // statements could be null
        return childs.size() == 1;
    }
}
