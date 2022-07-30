package com.github.jbox.mongo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Objects;

import static com.github.jbox.mongo.Constants.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-03-05 14:35:00.
 */
@Data
public class MongoEntity implements Serializable {

    private static final long serialVersionUID = 2109995371767737261L;

    @Id
    @Field(ID)
    @JSONField(name = "id")
    private Long _id;

    @Field(GMT_CREATE)
    @JSONField(name = GMT_CREATE)
    private String gmtCreate;

    @Field(GMT_MODIFIED)
    @JSONField(name = GMT_MODIFIED)
    private String gmtModified;

    public Long getId() {
        return _id;
    }

    public void setId(Long id) {
        this._id = id;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MongoEntity entity = (MongoEntity) o;
        return Objects.equals(_id, entity._id);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_id != null ? _id.hashCode() : 0);
        return result;
    }
}
