// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-

package com.easing.babble.ast;

class CompoundBlockNode extends BlockNode {
    public CompoundBlockNode(Node... childs) {
        super(childs);
    }

    @Override
    public boolean valid() {
        // statements and last_statement could be null
        return childs.size() == 2;
    }

    @Override
    public String toString() {
        
    }
}
