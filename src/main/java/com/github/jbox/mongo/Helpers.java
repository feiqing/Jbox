package com.github.jbox.mongo;

import com.github.jbox.utils.T;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.*;

import static com.github.jbox.mongo.Constants.GMT_MODIFIED;


/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-03-15 14:30:00.
 */
@Slf4j
class Helpers {

    static Query mapToQueryParam(Map<String, ?> map, String... excludes) {
        Query query = new Query();
        if (!CollectionUtils.isEmpty(map)) {
            map.entrySet().stream()
                    .filter(entry -> !contains(excludes, entry.getKey()))
                    .forEach(entry -> query.addCriteria(Criteria.where(entry.getKey()).is(entry.getValue())));
        }

        return query;
    }

    static Update mapToUpdateParam(Map<String, Object> map, String... excludes) {
        Update update = new Update();
        if (!CollectionUtils.isEmpty(map)) {
            map.entrySet().stream()
                    .filter(entry -> !contains(excludes, entry.getKey()))
                    .forEach(entry -> update.set(entry.getKey(), entry.getValue()));
        }

        update.set(GMT_MODIFIED, T.millisFormat(System.currentTimeMillis()));

        return update;
    }

    static Update mapToUpdateParamNoGmt(Map<String, Object> map, String... excludes) {
        Update update = new Update();
        if (!CollectionUtils.isEmpty(map)) {
            map.entrySet().stream()
                    .filter(entry -> !contains(excludes, entry.getKey()))
                    .forEach(entry -> update.set(entry.getKey(), entry.getValue()));
        }

        return update;
    }


    static void insertInit(MongoEntity object, String collection, MongoSequence mongoSequence) {
        // id
        if (object.getId() == null) {
            long id = mongoSequence.generateId(collection);
            object.setId(id);
        }

        // gmt_create
        if (Strings.isNullOrEmpty(object.getGmtCreate())) {
            object.setGmtCreate(T.millisFormat(System.currentTimeMillis()));
        }

        // gmt_modified
        object.setGmtModified(T.millisFormat(System.currentTimeMillis()));
    }

    static void checkCommandSuccess(Document document) throws MongoCommandExecuteException {
        int ok = document.getInteger("ok");
        if (ok != 1) {
            MongoCommandExecuteException exception = new MongoCommandExecuteException();
            exception.setOk(ok);
            exception.setErrorCode(document.getInteger("code"));
            exception.setCodeName(document.getString("codeName"));
            exception.setErrmsg(document.getString("errmsg"));
            throw exception;
        }
    }

    static Map<String, Object> beanToMap(Object obj, String... excludes) {

        Map<String, Object> map = new HashMap<>();
        if (obj != null) {
            Set<String> exclusive = new HashSet<>(Arrays.asList(excludes));
            ReflectionUtils.doWithFields(obj.getClass(), field -> {

                Field annotation = field.getAnnotation(Field.class);
                String name;
                if (annotation == null || Strings.isNullOrEmpty(name = annotation.value())) {
                    name = field.getName();
                }

                if (exclusive.contains(name)) {
                    return;
                }

                if (field.isSynthetic()) {
                    return;
                }

                if (Modifier.isStatic(field.getModifiers())) {
                    return;
                }

                ReflectionUtils.makeAccessible(field);
                Object value = field.get(obj);
                if (value == null) {
                    return;
                }

                map.put(name, value);
            });
        }
        return map;
    }

    private static boolean contains(String[] array, String element) {
        if (array == null || array.length == 0 || Strings.isNullOrEmpty(element)) {
            return false;
        }

        return Arrays
                .stream(array)
                .anyMatch(entry -> StringUtils.equals(entry, element));
    }

    public static class MongoCommandExecuteException extends RuntimeException {

        private static final long serialVersionUID = -8181715563606319069L;

        @Getter
        @Setter
        private int errorCode;

        @Getter
        @Setter
        private String codeName;

        @Getter
        @Setter
        private int ok;

        @Getter
        @Setter
        private String errmsg;

        @Override
        public String toString() {
            return "MongoCommandExecuteException{" +
                    "errorCode=" + errorCode +
                    ", codeName='" + codeName + '\'' +
                    ", ok=" + ok +
                    ", errmsg='" + errmsg + '\'' +
                    '}';
        }
    }
}
