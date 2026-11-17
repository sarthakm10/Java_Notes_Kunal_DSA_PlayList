package com.notes.mongo;

import com.notes.mongo.model.Priority;
import com.notes.mongo.model.Project;
import com.notes.mongo.model.Task;
import com.notes.mongo.model.User;
import com.notes.mongo.repo.TaskRepository;
import com.notes.mongo.service.TaskQueries;
import com.notes.mongo.service.TaskService;
import com.mongodb.MongoClientSettings;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.json.JsonWriterSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.TypeBasedAggregationOperationContext;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.convert.UpdateMapper;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * What would MongoDB receive? Everything here uses Boot's own auto-configured converter, mapping context
 * and MongoTemplate, but no server: there is none on this machine. The URI points at localhost with a
 * 300 ms server-selection timeout, so repository calls fail fast AFTER MongoTemplate has logged the
 * query it was about to send (DEBUG).
 */
@SpringBootTest(properties = {
        "spring.mongodb.uri=mongodb://localhost:27017/tasks?serverSelectionTimeoutMS=300",
        "spring.data.mongodb.auto-index-creation=false",          // would need a server at startup
        "logging.level.org.springframework.data.mongodb.core.MongoTemplate=DEBUG",
        "logging.level.org.mongodb.driver=ERROR"})
class WhatMongoReceivesTest {
    static final JsonWriterSettings PRETTY = JsonWriterSettings.builder().indent(true).build();
    /** The codecs the real MongoClient encodes with (they include java.time). */
    static final Codec<Document> DRIVER = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);

    static String json(Document d) { return d.toJson(PRETTY, DRIVER); }
    static String line(Document d) { return d.toJson(JsonWriterSettings.builder().build(), DRIVER); }

    @Autowired MappingMongoConverter converter;
    @Autowired MongoMappingContext mappingContext;
    @Autowired TaskRepository tasks;
    @Autowired TaskService service;

    MongoPersistentEntity<?> taskEntity() {
        return mappingContext.getRequiredPersistentEntity(Task.class);
    }

    static Task sampleTask() {
        Task t = new Task("TT", "Write the release notes", Priority.HIGH, LocalDate.of(2026, 11, 20));
        t.setAssignee(new Task.Assignee(2, "ravi"));
        t.getTags().addAll(List.of("docs", "release"));
        t.getComments().add(new Task.Comment("asha", "Include the migration steps", Instant.parse("2026-11-16T09:00:00Z")));
        return t;
    }

    @Test
    void anEntityBecomesOneDocument() {
        Document doc = new Document();
        converter.write(sampleTask(), doc);
        System.out.println("=== 1. Task -> BSON document (what insert() sends)\n" + json(doc));
        assertThat(doc).containsKeys("projectKey", "due", "assignee", "tags", "comments", "_class");
        assertThat(doc.get("priority")).isEqualTo("HIGH");                 // enums are stored by name
        assertThat(json(doc)).contains("2026-11-20T00:00:00Z");           // UTC midnight: MongoConfig
    }

    @Test
    void aDocumentReferenceStoresOnlyTheId() {
        Document doc = new Document();
        converter.write(new Project("TT", "Task Tracker", new User("u-asha", "asha")), doc);
        System.out.println("=== 2. Project with @DocumentReference owner\n" + json(doc));
        assertThat(doc.get("owner")).isEqualTo("u-asha");
    }

    @Test
    void criteriaQueriesAreMappedToFieldNamesAndBsonTypes() {
        Query q = TaskQueries.search("TT", Priority.HIGH, "release", LocalDate.of(2026, 12, 1));
        QueryMapper mapper = new QueryMapper(converter);
        Document filter = mapper.getMappedObject(q.getQueryObject(), taskEntity());
        Document sort = mapper.getMappedSort(q.getSortObject(), taskEntity());
        System.out.println("=== 3. TaskQueries.search(...)\nfilter: " + line(filter) + "\nsort:   " + line(sort)
                + "\nlimit:  " + q.getLimit());
        assertThat(filter).containsKey("due").doesNotContainKey("dueDate");
    }

    @Test
    void anUpdateIsAPartialChange() {
        Update u = TaskQueries.addComment(new Task.Comment("meera", "Done in PR 42", Instant.parse("2026-11-17T10:00:00Z")));
        Document mapped = new UpdateMapper(converter).getMappedObject(u.getUpdateObject(), taskEntity());
        System.out.println("=== 4. TaskQueries.addComment(...)\n" + json(mapped));
        assertThat(mapped).containsKey("$push").doesNotContainKey("$inc");
    }

    @Test
    void theTemplateAddsTheVersionIncrement() {
        System.out.println("=== 4b. The same updates sent through TaskService (MongoTemplate DEBUG lines)");
        catchThrowable(() -> service.addComment("6739a1f0c2b4e83d5f0a1b2c", "meera", "Done in PR 42"));
        catchThrowable(() -> service.completeOverdue());
    }

    @Test
    void aggregationsArePipelinesOfStages() {
        var ctx = new TypeBasedAggregationOperationContext(Task.class, mappingContext, new QueryMapper(converter));
        for (var entry : List.of(
                java.util.Map.entry("openByPriority(\"TT\")", TaskQueries.openByPriority("TT")),
                java.util.Map.entry("tagUsage()", TaskQueries.tagUsage()))) {
            Aggregation agg = entry.getValue();
            List<Document> pipeline = agg.toPipeline(ctx);
            System.out.println("=== 5. TaskQueries." + entry.getKey() + "\n"
                    + String.join("\n", pipeline.stream().map(WhatMongoReceivesTest::line).toList()));
            assertThat(pipeline).isNotEmpty();
        }
    }

    @Test
    void indexesComeFromAnnotations() {
        var resolver = new MongoPersistentEntityIndexResolver(mappingContext);
        StringBuilder out = new StringBuilder("=== 6. Indexes resolved from Task's and Project's annotations");
        for (Class<?> type : List.of(Task.class, Project.class)) {
            String collection = mappingContext.getRequiredPersistentEntity(type).getCollection();
            for (IndexDefinition def : resolver.resolveIndexFor(type)) {
                out.append("\n").append(collection).append(": keys ").append(def.getIndexKeys().toJson())
                        .append("  options ").append(def.getIndexOptions().toJson());
            }
        }
        System.out.println(out);
    }

    @Test
    void repositoryMethodsAreTranslatedBeforeTheyFail() {
        System.out.println("=== 7. Repository calls (MongoTemplate DEBUG lines), each then fails: no server");
        Throwable t = catchThrowable(() -> tasks.findByProjectKeyAndDoneFalseOrderByDueDateAsc("TT"));
        catchThrowable(() -> tasks.findByTags("release"));
        catchThrowable(() -> tasks.findByAssigneeName("ravi", PageRequest.of(0, 5)));
        catchThrowable(() -> tasks.findOverdue(Priority.HIGH, LocalDate.of(2026, 11, 17)));
        catchThrowable(() -> tasks.countByProjectKeyAndDoneFalse("TT"));
        System.out.println("=== 7b. The exception: " + t.getClass().getName() + "\n" + t.getMessage().lines().findFirst().orElse(""));
        assertThat(t).isInstanceOf(org.springframework.dao.DataAccessResourceFailureException.class);
    }
}
