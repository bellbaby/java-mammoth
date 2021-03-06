package org.zwobble.mammoth.internal.documents;

import java.util.List;

public class TableRow implements DocumentElement, HasChildren {
    private final List<DocumentElement> children;

    public TableRow(List<DocumentElement> children) {
        this.children = children;
    }

    @Override
    public List<DocumentElement> getChildren() {
        return children;
    }

    @Override
    public <T> T accept(DocumentElementVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
