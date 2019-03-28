package com.github.jbox.biz;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/3/28 11:19 AM.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageResultDO<T> extends ResultDO<T> {

    private static final long serialVersionUID = 5478501065767685096L;
    
    private int page;

    private int limit;

    private long total;

    private int pages;

    // 是否升序
    protected Boolean asc;

    // 排序字段
    protected String orderBy;

    public static <T> PageResultDO<T> errorOf(String code, String msg) {
        PageResultDO<T> resultDO = new PageResultDO<>();

        resultDO.setSuccess(false);
        resultDO.setCode(code);
        resultDO.setMsg(msg);

        return resultDO;
    }

    public static <T> PageResultDO<T> errorOf(IErrorCode error) {
        return errorOf(error.getCode(), error.getMsg());
    }

    public static <T> PageResultDO<T> errorOf(String msg) {
        return errorOf("500", msg);
    }

    public static <T> PageResultDO<T> successOf(T data, int page, int limit, long total) {
        PageResultDO<T> resultDO = new PageResultDO<>();

        resultDO.setData(data);

        resultDO.setSuccess(true);
        resultDO.setCode("200");
        resultDO.setMsg(null);
        resultDO.setPage(page);
        resultDO.setLimit(limit);
        resultDO.setTotal(total);
        resultDO.setPages((int) Math.ceil(total * 1.0 / limit));

        return resultDO;
    }

    public static <T> PageResultDO<T> successOf(int page, int limit, long total) {
        return successOf(null, page, limit, total);
    }
}
