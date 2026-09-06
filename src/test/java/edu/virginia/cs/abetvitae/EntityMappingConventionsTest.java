package edu.virginia.cs.abetvitae;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import org.hibernate.annotations.UuidGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMappingConventionsTest {

    private static final String BASE_PACKAGE = "edu.virginia.cs.abetvitae";

    private static final Set<String> VERSIONED_ENTITIES = Set.of(
            "FacultyCycleRecord",
            "ExtractionReview",
            "ExtractionReviewItem",
            "ProcessingJob"
    );

    @Test
    void everyEntityUsesAUuidVersionSevenGeneratedIdentifier() {
        for (Class<?> entityClass : entityClasses()) {
            List<Field> identifierFields = fieldsAnnotatedWith(entityClass, Id.class);

            assertThat(identifierFields)
                    .as("identifier fields on %s", entityClass.getSimpleName())
                    .hasSize(1);

            Field identifier = identifierFields.getFirst();
            UuidGenerator generator = identifier.getAnnotation(UuidGenerator.class);

            assertThat(identifier.getType())
                    .as("identifier type on %s", entityClass.getSimpleName())
                    .isEqualTo(java.util.UUID.class);
            assertThat(identifier.isAnnotationPresent(GeneratedValue.class))
                    .as("@GeneratedValue on %s", entityClass.getSimpleName())
                    .isTrue();
            assertThat(generator)
                    .as("@UuidGenerator on %s", entityClass.getSimpleName())
                    .isNotNull();
            assertThat(generator.style())
                    .as("UUID style on %s", entityClass.getSimpleName())
                    .isEqualTo(UuidGenerator.Style.VERSION_7);
        }
    }

    @Test
    void everyEntityRelationshipIsLazy() {
        for (Class<?> entityClass : entityClasses()) {
            for (Field field : entityClass.getDeclaredFields()) {
                assertLazyRelationship(entityClass, field);
            }
        }
    }

    @Test
    void onlyConcurrencyControlledEntitiesDeclareAVersionField() {
        for (Class<?> entityClass : entityClasses()) {
            List<Field> versionFields = fieldsAnnotatedWith(entityClass, Version.class);

            if (VERSIONED_ENTITIES.contains(entityClass.getSimpleName())) {
                assertThat(versionFields)
                        .as("version fields on %s", entityClass.getSimpleName())
                        .hasSize(1);
            } else {
                assertThat(versionFields)
                        .as("version fields on %s", entityClass.getSimpleName())
                        .isEmpty();
            }
        }
    }

    private static List<Class<?>> entityClasses() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        return scanner.findCandidateComponents(BASE_PACKAGE).stream()
                .<Class<?>>map(definition -> loadClass(definition.getBeanClassName()))
                .toList();
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Could not load entity " + className, exception);
        }
    }

    private static List<Field> fieldsAnnotatedWith(
            Class<?> entityClass,
            Class<? extends java.lang.annotation.Annotation> annotationClass
    ) {
        return List.of(entityClass.getDeclaredFields()).stream()
                .filter(field -> field.isAnnotationPresent(annotationClass))
                .toList();
    }

    private static void assertLazyRelationship(Class<?> entityClass, Field field) {
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        if (manyToOne != null) {
            assertThat(manyToOne.fetch())
                    .as("fetch type of %s.%s", entityClass.getSimpleName(), field.getName())
                    .isEqualTo(FetchType.LAZY);
        }

        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        if (oneToOne != null) {
            assertThat(oneToOne.fetch())
                    .as("fetch type of %s.%s", entityClass.getSimpleName(), field.getName())
                    .isEqualTo(FetchType.LAZY);
        }

        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        if (oneToMany != null) {
            assertThat(oneToMany.fetch())
                    .as("fetch type of %s.%s", entityClass.getSimpleName(), field.getName())
                    .isEqualTo(FetchType.LAZY);
        }

        ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
        if (manyToMany != null) {
            assertThat(manyToMany.fetch())
                    .as("fetch type of %s.%s", entityClass.getSimpleName(), field.getName())
                    .isEqualTo(FetchType.LAZY);
        }
    }
}
