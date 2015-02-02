// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-

package com.easing.babble.ast;

import java.util.ArrayList;
import java.util.List;

abstract class Node {
    protected List<Node> mChilds = new ArrayList<Node>();

    public Node(Node... childs) {
        for (int i = 0; i < childs.length; i++) {
            mchilds.add(childs[i]);
        }
    }

    boolean valid();

    List<Node> getChilds() {
        return mChilds;
    }
}
