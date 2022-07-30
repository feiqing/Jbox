package com.github.jbox.mongo;

import com.alibaba.fastjson.JSONObject;
import com.github.jbox.utils.Collections3;
import com.google.common.base.Preconditions;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import lombok.Getter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static com.github.jbox.biz.BizException.bizException;
import static com.github.jbox.mongo.BatisUtils.*;
import static com.github.jbox.mongo.TableConstant.ID;
import static com.github.jbox.mongo.TableConstant.SERIAL_VERSION_ID;


/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.2
 * @since 2018-03-04 12:53:00.
 */
@SuppressWarnings("unchecked")
public class MongoMapper<T extends MongoEntity> {

    @Getter
    private final SequenceDAO sequenceDAO;

    private final MongoOperations mongoTemplate;

    private final Class<T> type;

    public MongoMapper(MongoOperations mongoTemplate, SequenceDAO sequenceDAO) {
        this.type = _type();
        this.mongoTemplate = mongoTemplate;
        this.sequenceDAO = sequenceDAO;
    }

    public MongoMapper(Class<?> type, MongoOperations mongoTemplate, SequenceDAO sequenceDAO) {
        this.type = (Class<T>) type;
        this.mongoTemplate = mongoTemplate;
        this.sequenceDAO = sequenceDAO;
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

        Query queryParam = mapToQueryParam(where);
        queryParam.skip((page - 1) * limit);
        queryParam.limit(limit);
        if (!CollectionUtils.isEmpty(orderBy)) {
            queryParam.with(new Sort(orderBy));
        }

        return mongo().find(queryParam, type(), collection);
    }

    public T findOne(Map<String, Object> where) {
        return this.findOne(where, cname());
    }

    private T findOne(Map<String, Object> where, String collection) {
        Query queryParam = mapToQueryParam(where);
        return mongo().findOne(queryParam, type(), collection);
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

    public WriteResult upsert(Query query, Update update) {
        return mongo().upsert(query, update, type(), cname());
    }

    public long insert(T object) {
        return this.insert(object, cname());
    }

    private long insert(T object, String collection) {
        insertInit(object, collection, sequenceDAO);
        mongo().insert(object, collection);
        return object.getId();
    }

    public void insert(Collection<T> objects) {
        this.insert(objects, cname());
    }

    private void insert(Collection<T> objects, String collection) {
        for (T object : objects) {
            insertInit(object, collection, sequenceDAO);
        }
        mongo().insert(objects, collection);
    }

    public WriteResult updateOne(Map<String, Object> where, Map<String, Object> update) {
        return updateOne(where, update, cname());
    }

    private WriteResult updateOne(Map<String, Object> where, Map<String, Object> update, String collection) {
        Query queryParam = mapToQueryParam(where);
        Update updateParam = mapToUpdateParam(update, ID, SERIAL_VERSION_ID);
        return mongo().updateFirst(queryParam, updateParam, collection);
    }

    public WriteResult updateById(Map<String, Object> update, long id) {
        return updateById(update, id, cname());
    }

    public WriteResult updateById(T object) {
        Map<String, Object> update = beanToMap(object, ID, SERIAL_VERSION_ID);

        return this.updateById(update, object.getId());
    }

    private WriteResult updateById(Map<String, Object> update, long id, String collection) {
        Query queryParam = new Query(Criteria.where(ID).is(id));
        Update updateParam = mapToUpdateParam(update);

        return mongo().updateFirst(queryParam, updateParam, collection);
    }

    private WriteResult updateById(T object, String collection) {
        Map<String, Object> update = beanToMap(object, ID, SERIAL_VERSION_ID);

        return updateById(update, object.getId(), collection);
    }

    public WriteResult update(Map<String, Object> where, Map<String, Object> update) {
        return update(where, update, cname());
    }

    private WriteResult update(Map<String, Object> where, Map<String, Object> update, String collection) {
        Query queryParam = mapToQueryParam(where);
        Update updateParam = mapToUpdateParam(update, ID, SERIAL_VERSION_ID);
        return mongo().updateMulti(queryParam, updateParam, collection);
    }

    public int update(Query query, Update update) {
        return mongo().updateMulti(query, update, type(), cname()).getN();
    }

    public WriteResult removeById(long id) {
        return this.removeById(id, cname());
    }

    private WriteResult removeById(long id, String collection) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(id));
        return mongo().remove(query, collection);
    }

    public WriteResult removeByIds(Collection<Long> ids) {
        return this.removeByIds(ids, cname());
    }

    private WriteResult removeByIds(Collection<Long> ids, String collection) {
        Query query = new Query().addCriteria(Criteria.where("_id").in(ids));
        return mongo().remove(query, collection);
    }

    public WriteResult remove(Map<String, Object> where) {
        return this.remove(where, cname());
    }

    private WriteResult remove(Map<String, Object> where, String collection) {
        Query query = mapToQueryParam(where);
        return mongo().remove(query, collection);
    }

    public List<Object> distinct(String key, DBObject query) {
        return distinct(key, query, cname());
    }

    private List<Object> distinct(String key, DBObject query, String collection) {
        DBObject document = new BasicDBObject();
        document.put("distinct", collection);
        document.put("key", key);
        document.put("query", query);

        CommandResult result = mongo().executeCommand(document);
        checkCommandSuccess(result);
        return (List<Object>) result.get("values");
    }

    /* ------- helpers ------- */
    public MongoOperations mongo() {
        return mongoTemplate;
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
            superclass = ((Class) superclass).getGenericSuperclass();
        }

        if (!(superclass instanceof ParameterizedType)) {
            throw new RuntimeException(String.format("class:%s extends MongoBatis<T> not replace generic type <T>", this.getClass().getName()));
        }

        return (Class<T>) ((ParameterizedType) superclass).getActualTypeArguments()[0];
    }
}
