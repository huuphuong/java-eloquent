package io.github.phuonghuu.eloquent.loader;

import io.github.phuonghuu.eloquent.meta.RelationMeta;
import io.github.phuonghuu.eloquent.support.TextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class AttachStrategy {

    private AttachStrategy() {
    }

    public static void attachHasOne(List<?> parents, RelationMeta relationMeta, Map<Object, ?> byKey, String parentKeyProperty) {
        for (Object parent : parents) {
            Object key = propertyValue(parent, parentKeyProperty);
            relationMeta.attach(parent, byKey.get(key));
        }
    }

    public static void attachHasMany(List<?> parents, RelationMeta relationMeta, Map<Object, List<Object>> grouped, String parentKeyProperty) {
        for (Object parent : parents) {
            Object key = propertyValue(parent, parentKeyProperty);
            List<Object> children = grouped.containsKey(key) ? grouped.get(key) : Collections.<Object>emptyList();
            relationMeta.attach(parent, new ArrayList<Object>(children));
        }
    }

    public static void attachBelongsTo(List<?> parents, RelationMeta relationMeta, Map<Object, ?> byKey, String foreignKeyProperty) {
        for (Object parent : parents) {
            Object key = propertyValue(parent, foreignKeyProperty);
            relationMeta.attach(parent, byKey.get(key));
        }
    }

    public static void attachBelongsToMany(List<?> parents, RelationMeta relationMeta, Map<Object, List<Object>> grouped, String parentKeyProperty) {
        for (Object parent : parents) {
            Object key = propertyValue(parent, parentKeyProperty);
            List<Object> children = grouped.containsKey(key) ? grouped.get(key) : Collections.<Object>emptyList();
            relationMeta.attach(parent, new ArrayList<Object>(children));
        }
    }

    private static Object propertyValue(Object target, String property) {
        return new org.springframework.beans.BeanWrapperImpl(target).getPropertyValue(property);
    }
}

