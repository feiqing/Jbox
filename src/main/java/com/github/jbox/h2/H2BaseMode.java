package com.github.jbox.h2;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/9/24 10:23 AM.
 */
@Data
public class H2BaseMode implements Serializable {

    private static final long serialVersionUID = -4979428424805475148L;

    @Column(type = "BIGINT IDENTITY PRIMARY KEY", nullable = false)
    private Long id;

    @Column(type = "TIMESTAMP", nullable = false)
    private Date gmt_create;

    @Column(type = "TIMESTAMP", nullable = false)
    private Date gmt_modified;
}
