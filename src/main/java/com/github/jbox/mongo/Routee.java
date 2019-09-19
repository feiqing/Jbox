package com.github.jbox.mongo;

import com.github.jbox.utils.Collections3;
import com.google.common.base.Preconditions;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/9/19 7:57 PM.
 */
@Data
public class Routee<T extends MongoBatis> {

    private String id;

    private Set<String> slots;

    private T target;

    private boolean standby;

    public static <T extends MongoBatis> Routee<T> newSlots(String id, Set<String> slots, T target) {
        Preconditions.checkState(StringUtils.isNoneBlank(id));
        Preconditions.checkState(Collections3.isNotEmpty(slots));
        Preconditions.checkState(target != null);

        Routee<T> routee = new Routee<>();
        routee.id = id;
        routee.slots = slots;
        routee.target = target;
        routee.standby = false;

        return routee;
    }

    public static <T extends MongoBatis> Routee<T> newStandby(String id, T target) {
        Preconditions.checkState(StringUtils.isNoneBlank(id));
        Preconditions.checkState(target != null);

        Routee<T> routee = new Routee<>();
        routee.id = id;
        routee.standby = true;
        routee.target = target;
        routee.slots = null;

        return routee;
    }

}