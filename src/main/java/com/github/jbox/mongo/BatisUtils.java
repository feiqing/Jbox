package com.github.jbox.mongo;

import com.github.jbox.utils.DateUtils;
import com.google.common.base.Strings;
import com.mongodb.CommandResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.*;

import static com.github.jbox.mongo.TableConstant.GMT_MODIFIED;


/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-03-15 14:30:00.
 */
@Slf4j
public class BatisUtils {

    public static Query mapToQueryParam(Map<String, ?> map, String... excludes) {
        Query query = new Query();
        if (!CollectionUtils.isEmpty(map)) {
            map.entrySet().stream()
                    .filter(entry -> !contains(excludes, entry.getKey()))
                    .forEach(entry -> query.addCriteria(Criteria.where(entry.getKey()).is(entry.getValue())));
        }

        return query;
    }

    public static Update mapToUpdateParam(Map<String, Object> map, String... excludes) {
        Update update = new Update();
        if (!CollectionUtils.isEmpty(map)) {
            map.entrySet().stream()
                    .filter(entry -> !contains(excludes, entry.getKey()))
                    .forEach(entry -> update.set(entry.getKey(), entry.getValue()));
        }

        update.set(GMT_MODIFIED, DateUtils.timeMillisFormat(System.currentTimeMillis()));

        return update;
    }

    public static Update mapToUpdateParamNoGmt(Map<String, Object> map, String... excludes) {
        Update update = new Update();
        if (!CollectionUtils.isEmpty(map)) {
            map.entrySet().stream()
                    .filter(entry -> !contains(excludes, entry.getKey()))
                    .forEach(entry -> update.set(entry.getKey(), entry.getValue()));
        }

        return update;
    }


    static void insertInit(MBaseModel object, String collection, SequenceDAO sequenceDAO) {
        // id
        if (object.getId() == null) {
            long id = sequenceDAO.generateId(collection);
            object.setId(id);
        }

        // gmt_create
        if (Strings.isNullOrEmpty(object.getGmtCreate())) {
            object.setGmtCreate(DateUtils.timeMillisFormat(System.currentTimeMillis()));
        }

        // gmt_modified
        object.setGmtModified(DateUtils.timeMillisFormat(System.currentTimeMillis()));
    }

    static void checkCommandSuccess(CommandResult document) throws MongoCommandExecuteException {
        int ok = (int) document.getDouble("ok");
        if (ok != 1) {
            MongoCommandExecuteException exception = new MongoCommandExecuteException();
            exception.setOk(ok);
            exception.setErrorCode(document.getInt("code"));
            exception.setCodeName(document.getString("codeName"));
            exception.setErrmsg(document.getString("errmsg"));
            throw exception;
        }
    }

    public static Map<String, Object> beanToMap(Object obj, String... excludes) {

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
}
