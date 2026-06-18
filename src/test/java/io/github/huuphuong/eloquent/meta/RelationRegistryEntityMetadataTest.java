package io.github.huuphuong.eloquent.meta;

import org.junit.jupiter.api.Test;

import javax.persistence.Id;
import javax.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelationRegistryEntityMetadataTest {

    @Test
    void readsTableAndPrimaryKeyFromEntityAnnotations() {
        RelationRegistry registry = new RelationRegistry();

        registry.forClass(ScoreModel.class)
            .fromEntity()
            .register();

        RelationRegistry.EntityMeta<ScoreModel> meta = registry.require(ScoreModel.class);
        assertEquals("score_model", meta.table());
        assertEquals("code", meta.primaryKeyProperty());
    }

    @Test
    void readsTableAndPrimaryKeySeparatelyWhenNeeded() {
        RelationRegistry registry = new RelationRegistry();

        registry.forClass(ScoreModel.class)
            .tableFromEntity()
            .primaryKeyFromEntity()
            .register();

        RelationRegistry.EntityMeta<ScoreModel> meta = registry.require(ScoreModel.class);
        assertEquals("score_model", meta.table());
        assertEquals("code", meta.primaryKeyProperty());
    }

    @Test
    void failsWhenEntityHasNoTableAnnotation() {
        RelationRegistry registry = new RelationRegistry();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            registry.forClass(NoTableModel.class)
                .fromEntity()
                .register()
        );

        assertEquals("No @Table annotation found for " + NoTableModel.class.getName(), exception.getMessage());
    }

    @Test
    void failsWhenEntityHasNoIdAnnotation() {
        RelationRegistry registry = new RelationRegistry();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            registry.forClass(NoIdModel.class)
                .fromEntity()
                .register()
        );

        assertEquals("No @Id annotation found for " + NoIdModel.class.getName(), exception.getMessage());
    }

    @Table(name = "score_model")
    public static class ScoreModel {
        @Id
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    public static class NoTableModel {
        @Id
        private Long id;
    }

    @Table(name = "no_id_model")
    public static class NoIdModel {
        private Long id;
    }
}
