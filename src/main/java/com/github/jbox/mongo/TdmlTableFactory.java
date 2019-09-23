package com.github.jbox.mongo;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/9/23 11:13 PM.
 */
public class TdmlTableFactory {

    private static List<SlotLoader> loaders = Collections.emptyList();

    private String slotKey;

    public TdmlTableFactory(String slotKey) {
        this.slotKey = slotKey;
    }

    public String name(String prefix) {
        String slot = loadSlot(prefix);
        return generateCollectionName(prefix, slot);
    }

    private String loadSlot(String prefix) {
        String slot = MDC.get(slotKey);
        if (!Strings.isNullOrEmpty(slot)) {
            return slot;
        }

        for (SlotLoader loader : loaders) {
            slot = loader.getSlot();

            if (!Strings.isNullOrEmpty(slot)) {
                return slot;
            }
        }


        throw new TdmlException("table [" + prefix + "] not specify slotKey:[" + slotKey + "].");
    }

    private String generateCollectionName(String prefix, String slotValue) {
        return String.format("%s_%s", prefix, slotValue);
    }

    public static void registerSlotLoader(SlotLoader loader) {
        Preconditions.checkArgument(loader != null);

        List<SlotLoader> newLoaders = new LinkedList<>(loaders);
        newLoaders.add(loader);

        loaders = ImmutableList.copyOf(newLoaders);
    }

    public interface SlotLoader {
        String getSlot();
    }
}
