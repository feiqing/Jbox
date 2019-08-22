package com.github.jbox.mongo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
public class MongoBatis<T extends MBaseModel> {

    @Getter
    private SequenceDAO sequenceDAO;

    private MongoOperations mongoTemplate;

    private Class<T> type;

    public MongoBatis(MongoOperations mongoTemplate, SequenceDAO sequenceDAO) {
        this.type = getType();
        this.mongoTemplate = mongoTemplate;
        this.sequenceDAO = sequenceDAO;
    }

    public MongoBatis(Class<?> type, MongoOperations mongoTemplate, SequenceDAO sequenceDAO) {
        this.type = (Class<T>) type;
        this.mongoTemplate = mongoTemplate;
        this.sequenceDAO = sequenceDAO;
    }

    /* ------- findById ------- */
    public T findById(long id) {
        return this.findById(id, getCName());
    }

    private T findById(long id, String collection) {
        return getMongo().findOne(new Query(Criteria.where(ID).is(id)), asType(), collection);
    }

    public T findByIdWithCheck(long id) {
        return this.findByIdWithCheck(id, getCName());
    }

    private T findByIdWithCheck(long id, String collection) {
        T data = getMongo().findOne(new Query(Criteria.where(ID).is(id)), asType(), collection);
        return Optional
                .ofNullable(data)
                .orElseThrow(() -> bizException("RESOURCE_EMPTY", "could not find id:[{}] in:[{}]", id, collection));
    }

    /* ------- findByIds ------- */
    public List<T> findByIds(Collection<Long> ids) {
        return this.findByIds(ids, getCName());
    }

    private List<T> findByIds(Collection<Long> ids, String collection) {
        return getMongo().find(new Query(Criteria.where(ID).in(ids)), asType(), collection);
    }

    /* ------- findAll ------- */
    public List<T> findAll() {
        return this.findAll(getCName());
    }

    private List<T> findAll(String collection) {
        return getMongo().find(new Query(), asType(), collection);
    }

    /* ------- findOne ------- */
    public T findOne(Map<String, Object> where) {
        return this.findOne(where, getCName());
    }

    private T findOne(Map<String, Object> where, String collection) {
        Query queryParam = mapToQueryParam(where);
        return getMongo().findOne(queryParam, asType(), collection);
    }

    /* ------- 通用 find ------- */
    public List<T> find(Map<String, Object> where) {
        return this.find(where, getCName());
    }

    private List<T> find(Map<String, Object> where, String collection) {
        Query queryParam = mapToQueryParam(where);
        return getMongo().find(queryParam, asType(), collection);
    }

    /* ------- 分页find(start page : 1) ------- */
    public List<T> find(Map<String, Object> where, int page, int limit, List<Sort.Order> orderBy) {
        return this.find(where, page, limit, orderBy, getCName());
    }

    private List<T> find(Map<String, Object> where, int page, int limit, List<Sort.Order> orderBy, String collection) {
        Preconditions.checkArgument(page >= 1);

        Query queryParam = mapToQueryParam(where);
        queryParam.skip((page - 1) * limit);
        queryParam.limit(limit);
        if (!CollectionUtils.isEmpty(orderBy)) {
            queryParam.with(new Sort(orderBy));
        }

        return getMongo().find(queryParam, asType(), collection);
    }

    /* ------- count ------- */
    public long count(Map<String, Object> where) {
        return this.count(where, getCName());
    }

    private long count(Map<String, Object> where, String collection) {
        Query query = mapToQueryParam(where);
        return getMongo().count(query, collection);
    }

    /* ------- save: 更新或保存 ------- */
    public long save(T object) {
        if (object.getId() != null) {
            updateById(object);
        } else {
            insert(object);
        }
        return object.getId();
    }

    public WriteResult upsert(Query query, Update update) {
        return getMongo().upsert(query, update, asType(), getCName());
    }

    /* ------- insert ------- */
    public long insert(T object) {
        return this.insert(object, getCName());
    }

    private long insert(T object, String collection) {
        insertInit(object, collection, sequenceDAO);
        getMongo().insert(object, collection);
        return object.getId();
    }

    public void insert(Collection<T> objects) {
        this.insert(objects, getCName());
    }

    private void insert(Collection<T> objects, String collection) {
        for (T object : objects) {
            insertInit(object, collection, sequenceDAO);
        }
        getMongo().insert(objects, collection);
    }

    /* ------- updateOne ------- */
    public WriteResult updateOne(Map<String, Object> where, Map<String, Object> update) {
        return updateOne(where, update, getCName());
    }

    private WriteResult updateOne(Map<String, Object> where, Map<String, Object> update, String collection) {
        Query queryParam = mapToQueryParam(where);
        Update updateParam = mapToUpdateParam(update, ID, SERIAL_VERSION_ID);
        return getMongo().updateFirst(queryParam, updateParam, collection);
    }

    /* ------- updateById ------- */
    public WriteResult updateById(Map<String, Object> update, long id) {
        return updateById(update, id, getCName());
    }

    public WriteResult updateById(T object) {
        Map<String, Object> update = beanToMap(object, ID, SERIAL_VERSION_ID);

        return this.updateById(update, object.getId());
    }

    private WriteResult updateById(Map<String, Object> update, long id, String collection) {
        Query queryParam = new Query(Criteria.where(ID).is(id));
        Update updateParam = mapToUpdateParam(update);

        return getMongo().updateFirst(queryParam, updateParam, collection);
    }

    private WriteResult updateById(T object, String collection) {
        Map<String, Object> update = beanToMap(object, ID, SERIAL_VERSION_ID);

        return updateById(update, object.getId(), collection);
    }

    /* ------- update ------- */
    public WriteResult update(Map<String, Object> where, Map<String, Object> update) {
        return update(where, update, getCName());
    }

    private WriteResult update(Map<String, Object> where, Map<String, Object> update, String collection) {
        Query queryParam = mapToQueryParam(where);
        Update updateParam = mapToUpdateParam(update, ID, SERIAL_VERSION_ID);
        return getMongo().updateMulti(queryParam, updateParam, collection);
    }

    /* ------- delete ------- */
    public WriteResult removeById(long id) {
        return this.removeById(id, getCName());
    }

    private WriteResult removeById(long id, String collection) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(id));
        return getMongo().remove(query, collection);
    }

    public WriteResult removeByIds(Collection<Long> ids) {
        return this.removeByIds(ids, getCName());
    }

    private WriteResult removeByIds(Collection<Long> ids, String collection) {
        Query query = new Query().addCriteria(Criteria.where("_id").in(ids));
        return getMongo().remove(query, collection);
    }

    public WriteResult remove(Map<String, Object> where) {
        return this.remove(where, getCName());
    }

    private WriteResult remove(Map<String, Object> where, String collection) {
        Query query = mapToQueryParam(where);
        return getMongo().remove(query, collection);
    }

    /* ------- distinct ------- */

    public List<Object> distinct(String key, DBObject query) {
        return distinct(key, query, getCName());
    }

    private List<Object> distinct(String key, DBObject query, String collection) {
        DBObject document = new BasicDBObject();
        document.put("distinct", collection);
        document.put("key", key);
        document.put("query", query);

        CommandResult result = getMongo().executeCommand(document);
        checkCommandSuccess(result);
        return (List<Object>) result.get("values");
    }


    /* ------- helpers ------- */
    public MongoOperations getMongo() {
        return mongoTemplate;
    }

    protected String getCName() {
        return getMongo().getCollectionName(type);
    }

    protected Class<T> asType() {
        return type;
    }

    private Class<T> getType() {
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
