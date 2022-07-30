package com.github.jbox.mybatis;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/2/5 10:53 AM.
 */
@Data
public class BaseEntity<ID extends Serializable> implements Serializable {

    private static final long serialVersionUID = 535912108299590205L;

    private ID id;

    private Date gmtCreate;

    private Date gmtModified;
}
