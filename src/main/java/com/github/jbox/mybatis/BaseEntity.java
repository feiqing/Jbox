package com.github.jbox.mybatis;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/2/5 10:53 AM.
 */
@ToString(exclude = {"_use_gmt_create", "_use_gmt_modified"})
public class BaseEntity<ID> implements Serializable {

    private static final long serialVersionUID = 535912108299590205L;
    
    @Getter
    @Setter
    private ID id;

    @Getter
    @Setter
    private Date gmtCreate;

    @Getter
    @Setter
    private Date gmtModified;

    // meta
    private transient boolean _use_gmt_create = true;

    private transient boolean _use_gmt_modified = true;

    public boolean useGmtCreate() {
        return _use_gmt_create;
    }

    protected void useGmtCreate(boolean use) {
        _use_gmt_create = use;
    }

    public boolean useGmtModified() {
        return _use_gmt_modified;
    }

    protected void useGmtModified(boolean use) {
        _use_gmt_modified = use;
    }
}
