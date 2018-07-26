package com.github.jbox.trace

import com.github.jbox.trace.tlog.TlogManager
import org.springframework.core.io.FileSystemResource

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/10/20 15:50:00.
 */
class TlogManagerTest extends GroovyTestCase {

    public static void main(String[] args) throws IOException {
        TlogManager tLogManager = new TlogManager()
        tLogManager.setResource(new FileSystemResource("/Users/jifang.zjf/IdeaProjects/jbox/src/test/java/resources/tlog-config.xml"))
        System.out.println(tLogManager.getMethodELMap())
        assertTrue tLogManager.methodELMap.size() == 7
        def userWrapperSize = tLogManager.methodELMap.find { key, value ->
            key.contains("userWrapper")
        }.value.size()

        assertTrue userWrapperSize == 3
    }
}
