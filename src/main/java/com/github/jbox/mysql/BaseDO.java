package com.github.jbox.mysql;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/2/5 10:53 AM.
 */
@Data
public class BaseDO implements Serializable {

    private static final long serialVersionUID = 535912108299590205L;

    private Long id;

    private Date gmtCreate;

    private Date gmtModified;
}
