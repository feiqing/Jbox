package com.github.jbox.spring.support;

import com.github.jbox.spring.DynamicPropertySourcesPlaceholder;
import com.github.jbox.spring.LazyInitializingBean;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.taobao.diamond.client.Diamond;
import com.taobao.diamond.manager.ManagerListener;
import com.taobao.diamond.manager.ManagerListenerAdapter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-14 11:01:00.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DiamondPropertySourcesPlaceholder extends DynamicPropertySourcesPlaceholder implements DisposableBean, LazyInitializingBean {

    private String dataId;

    private String groupId;

    private long timeoutMs = 5 * 1000;

    private ManagerListener listener = new ManagerListenerAdapter() {
        @Override
        public void receiveConfigInfo(String dynamicConfigData) {
            DiamondPropertySourcesPlaceholder.super.onDynamicConfigData(dynamicConfigData);
        }
    };

    @Override
    protected String getInitConfigData() {
        Preconditions.checkState(!Strings.isNullOrEmpty(this.groupId), "diamond 'groupId' can not be empty.");
        Preconditions.checkState(!Strings.isNullOrEmpty(this.dataId), "diamond 'dataId' can not be empty.");

        try {
            return Diamond.getConfig(this.dataId, this.groupId, this.timeoutMs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterApplicationInitiated(ApplicationContext applicationContext) {
        Diamond.addListener(this.dataId, this.groupId, this.listener);
    }

    @Override
    public void destroy() {
        if (this.listener != null) {
            Diamond.removeListener(this.dataId, this.groupId, this.listener);
        }
    }
}
