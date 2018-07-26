package com.github.jbox.trace.nodes;

import com.alibaba.fastjson.JSONObject;
import com.github.jbox.trace.InvokerNode;
import com.github.jbox.trace.NodeContext;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 1.3 - validate arguments
 * @since 2018-07-26 08:01:00.
 */
public class ArgsValidateInvokerNode implements InvokerNode {

    private static final ExecutableValidator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator().forExecutables();
    }

    @Override
    public void invoke(NodeContext context) throws Throwable {
        Object target = context.getTarget();
        Method method = context.getMethod();
        Object[] args = context.getArgs();

        Set<ConstraintViolation<Object>> violationSet = validator.validateParameters(target, method, args);
        if (violationSet != null && !violationSet.isEmpty()) {
            StringBuilder msgBuilder = new StringBuilder(128);
            for (ConstraintViolation violation : violationSet) {
                msgBuilder
                        .append("path: ")
                        .append(violation.getPropertyPath())
                        .append(", err msg:")
                        .append(violation.getMessage())
                        .append("\n");
            }
            msgBuilder.append("### your request params: ")
                    .append(JSONObject.toJSONString(args));

            throw new ValidationException(msgBuilder.toString());
        }

        context.next();
    }
}
