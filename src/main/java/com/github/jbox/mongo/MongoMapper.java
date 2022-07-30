package com.github.jbox.mongo;

import com.alibaba.fastjson.JSONObject;
import com.github.jbox.utils.Collections3;
import com.google.common.base.Preconditions;
import com.mongodb.DBObject;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lombok.Getter;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.github.jbox.mongo.Constants.ID;
import static com.github.jbox.mongo.Constants.SERIAL_VERSION_ID;
import static com.github.jbox.mongo.Helpers.*;


/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.2
 * @since 2018-03-04 12:53:00.
 */
@SuppressWarnings("unchecked")
public class MongoMapper<T extends MongoEntity> {

    @Getter
    private final MongoSequence sequence;

    private final MongoOperations operations;

    private final Class<T> type;

    public MongoMapper(MongoOperations operations, MongoSequence sequence) {
        this.type = _type();
        this.operations = operations;
        this.sequence = sequence;
    }

    public MongoMapper(Class<?> type, MongoOperations operations, MongoSequence sequence) {
        this.type = (Class<T>) type;
        this.operations = operations;
        this.sequence = sequence;
    }

    public T findById(long id) {
        return this.findById(id, cname());
    }

    private T findById(long id, String collection) {
        return mongo().findOne(new Query(Criteria.where(ID).is(id)), type(), collection);
    }

    public List<T> findByIds(Collection<Long> ids) {
        return this.findByIds(ids, cname());
    }

    private List<T> findByIds(Collection<Long> ids, String collection) {
        return mongo().find(new Query(Criteria.where(ID).in(ids)), type(), collection);
    }

    public List<T> findAll() {
        return this.findAll(cname());
    }

    private List<T> findAll(String collection) {
        return mongo().find(new Query(), type(), collection);
    }

    public List<T> find(Map<String, Object> where) {
        return this.find(where, cname());
    }

    private List<T> find(Map<String, Object> where, String collection) {
        Query queryParam = mapToQueryParam(where);
        return mongo().find(queryParam, type(), collection);
    }

    public List<T> find(Query query) {
        return mongo().find(query, this.type());
    }

    public List<JSONObject> find(Map<String, Object> where, String... fields) {
        Query query = new Query();
        Collections3.nullToEmpty(where).forEach((k, v) -> query.addCriteria(Criteria.where(k).is(v)));
        Arrays.stream(fields).forEach(field -> query.fields().include(field));

        return this.mongo().find(query, JSONObject.class, cname());
    }

    /* ------- 分页find(start page : 1) ------- */
    public List<T> find(Map<String, Object> where, int page, int limit, List<Sort.Order> orderBy) {
        return this.find(where, page, limit, orderBy, cname());
    }

    private List<T> find(Map<String, Object> where, int page, int limit, List<Sort.Order> orderBy, String collection) {
        Preconditions.checkArgument(page >= 1);

        Query query = mapToQueryParam(where);
        query.skip((long) (page - 1) * limit);
        query.limit(limit);
        if (!CollectionUtils.isEmpty(orderBy)) {
            query.with(Sort.by(orderBy));
        }

        return mongo().find(query, type(), collection);
    }

    public T findOne(Map<String, Object> where) {
        return this.findOne(where, cname());
    }

    private T findOne(Map<String, Object> where, String collection) {
        return mongo().findOne(mapToQueryParam(where), type(), collection);
    }

    public long count(Map<String, Object> where) {
        return this.count(where, cname());
    }

    private long count(Map<String, Object> where, String collection) {
        Query query = mapToQueryParam(where);
        return mongo().count(query, collection);
    }

    public long count(Query query) {
        return this.mongo().count(query, this.type());
    }

    public long save(T object) {
        if (object.getId() != null) {
            updateById(object);
        } else {
            insert(object);
        }
        return object.getId();
    }

    public UpdateResult upsert(Query query, Update update) {
        return mongo().upsert(query, update, type(), cname());
    }

    public long insert(T object) {
        return this.insert(object, cname());
    }

    private long insert(T object, String collection) {
        insertInit(object, collection, sequence);
        mongo().insert(object, collection);
        return object.getId();
    }

    public void insert(Collection<T> objects) {
        this.insert(objects, cname());
    }

    private void insert(Collection<T> objects, String collection) {
        for (T object : objects) {
            insertInit(object, collection, sequence);
        }
        mongo().insert(objects, collection);
    }

    public UpdateResult updateOne(Map<String, Object> where, Map<String, Object> update) {
        return updateOne(where, update, cname());
    }

    private UpdateResult updateOne(Map<String, Object> where, Map<String, Object> update, String collection) {
        Query queryParam = mapToQueryParam(where);
        Update updateParam = mapToUpdateParam(update, ID, SERIAL_VERSION_ID);
        return mongo().updateFirst(queryParam, updateParam, collection);
    }

    public UpdateResult updateById(Map<String, Object> update, long id) {
        return updateById(update, id, cname());
    }

    public UpdateResult updateById(T object) {
        Map<String, Object> update = beanToMap(object, ID, SERIAL_VERSION_ID);

        return this.updateById(update, object.getId());
    }

    private UpdateResult updateById(Map<String, Object> update, long id, String collection) {
        Query queryParam = new Query(Criteria.where(ID).is(id));
        Update updateParam = mapToUpdateParam(update);

        return mongo().updateFirst(queryParam, updateParam, collection);
    }

    private UpdateResult updateById(T object, String collection) {
        Map<String, Object> update = beanToMap(object, ID, SERIAL_VERSION_ID);

        return updateById(update, object.getId(), collection);
    }

    public UpdateResult update(Map<String, Object> where, Map<String, Object> update) {
        return update(where, update, cname());
    }

    private UpdateResult update(Map<String, Object> where, Map<String, Object> update, String collection) {
        Query queryParam = mapToQueryParam(where);
        Update updateParam = mapToUpdateParam(update, ID, SERIAL_VERSION_ID);
        return mongo().updateMulti(queryParam, updateParam, collection);
    }

    public UpdateResult update(Query query, Update update) {
        return mongo().updateMulti(query, update, type(), cname());
    }

    public DeleteResult removeById(long id) {
        return this.removeById(id, cname());
    }

    private DeleteResult removeById(long id, String collection) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(id));
        return mongo().remove(query, collection);
    }

    public DeleteResult removeByIds(Collection<Long> ids) {
        return this.removeByIds(ids, cname());
    }

    private DeleteResult removeByIds(Collection<Long> ids, String collection) {
        Query query = new Query().addCriteria(Criteria.where("_id").in(ids));
        return mongo().remove(query, collection);
    }

    public DeleteResult remove(Map<String, Object> where) {
        return this.remove(where, cname());
    }

    private DeleteResult remove(Map<String, Object> where, String collection) {
        Query query = mapToQueryParam(where);
        return mongo().remove(query, collection);
    }

    public List<Object> distinct(String key, DBObject query) {
        return distinct(key, query, cname());
    }

    private List<Object> distinct(String key, DBObject query, String collection) {
        Document document = new Document();
        document.put("distinct", collection);
        document.put("key", key);
        document.put("query", query);

        Document result = mongo().executeCommand(document);
        checkCommandSuccess(result);
        return (List<Object>) result.get("values");
    }

    /* ------- helpers ------- */
    public MongoOperations mongo() {
        return operations;
    }

    protected String cname() {
        return mongo().getCollectionName(type);
    }

    protected Class<T> type() {
        return type;
    }

    private Class<T> _type() {
        Type superclass = this.getClass().getGenericSuperclass();
        while (!superclass.equals(Object.class) && !(superclass instanceof ParameterizedType)) {
            superclass = ((Class<?>) superclass).getGenericSuperclass();
        }

        if (!(superclass instanceof ParameterizedType)) {
            throw new RuntimeException(String.format("class:%s extends MongoMapper<T> not replace generic type <T>", this.getClass().getName()));
        }

        return (Class<T>) ((ParameterizedType) superclass).getActualTypeArguments()[0];
    }
}
