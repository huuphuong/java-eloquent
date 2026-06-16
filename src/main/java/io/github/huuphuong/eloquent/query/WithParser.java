package io.github.huuphuong.eloquent.query;

import io.github.huuphuong.eloquent.support.TextUtils;

import java.util.Collection;

public final class WithParser {

    private WithParser() {
    }

    public static WithNode parse(Collection<String> paths) {
        WithNode root = new WithNode("__root__");
        for (String path : paths) {
            if (TextUtils.isBlank(path)) {
                continue;
            }
            root.addPath(path.split("\\."), 0);
        }
        return root;
    }
}

