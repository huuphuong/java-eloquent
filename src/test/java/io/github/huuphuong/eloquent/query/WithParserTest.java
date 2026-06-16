package io.github.huuphuong.eloquent.query;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WithParserTest {

    @Test
    void parsesNestedPathsAndSkipsBlankEntries() {
        WithNode root = WithParser.parse(Arrays.asList("users.profile", "", null, "users.orders", "roles"));

        assertEquals(2, root.children().size());
        assertTrue(hasChild(root, "users"));
        assertTrue(hasChild(root, "roles"));

        WithNode users = root.child("users");
        assertEquals(2, users.children().size());
        assertTrue(hasChild(users, "profile"));
        assertTrue(hasChild(users, "orders"));
    }

    @Test
    void absorbMergesTreeStateAndKeepsExistingChildren() {
        WithNode base = new WithNode("users");
        base.select(Arrays.asList("id"));
        base.orderBy("name", "ASC");

        WithNode incoming = new WithNode("users");
        incoming.select(Arrays.asList("name"));
        incoming.orderBy("createdAt", "DESC");
        incoming.limit(5);
        incoming.offset(10);
        incoming.limitPerParent(2);
        incoming.child("profile");

        base.absorb(incoming);

        assertEquals(Arrays.asList("id", "name"), base.selectColumns());
        assertEquals(2, base.orderBy().size());
        assertEquals(Integer.valueOf(5), base.limit());
        assertEquals(Integer.valueOf(10), base.offset());
        assertEquals(Integer.valueOf(2), base.perParentLimit());
        assertTrue(hasChild(base, "profile"));
    }

    private boolean hasChild(WithNode node, String childName) {
        for (WithNode child : node.children()) {
            if (child.name().equals(childName)) {
                return true;
            }
        }
        return false;
    }
}

