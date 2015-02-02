// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-

package com.easing.babble.ast;

class BlockNode extends Node {
    public BlockNode(Node... childs) {
        suepr(childs);
    }

    @Override
    boolean valid() {
    }
}
