package com.alibaba.jbox.trace;

import java.util.LinkedList;
import java.util.List;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/9/25 17:47:00.
 */
public class SeplTeset {

    private static final SpelExpressionParser SPEL_PARSER = new SpelExpressionParser();

    public static void main(String[] args) {

        class User {
            private String name;

            public User(String name) {
                this.name = name;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }
        List<User> lists = new LinkedList<>();
        lists.add(new User("feiqing"));
        StandardEvaluationContext context = new StandardEvaluationContext();
        //leadInFunctions(context);
        context.setVariable("args", lists);
        context.setVariable("ph", "-");
        Object value = SPEL_PARSER.parseExpression("#ifNotEmptyGet(#args, 0)?.name").getValue(context);
        System.out.println(value);
    }
}
