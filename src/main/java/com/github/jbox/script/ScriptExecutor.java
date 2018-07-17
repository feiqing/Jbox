package com.github.jbox.script;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.jbox.spring.AbstractApplicationContextAware;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.script.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.1
 * @since jbox-1.4.0 (16/10/9 下午6:05).
 */
@Service("ScriptExecutor")
public class ScriptExecutor extends AbstractApplicationContextAware
        implements IScriptExecutor, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger("com.github.jbox.script");

    private static final String HACKER_SALT = "@$^_^$@";

    private static final String NAMES_KEY = "names";

    private static final String TYPES_KEY = "types";

    private static final Map<String, Object> contextNotInSpring = new HashMap<>();

    private Reference<Bindings> reference = new SoftReference<>(null);

    private Set<String> excludeNames = new HashSet<>();

    private Set<String> excludeTypes = new HashSet<>();

    private String salt;

    private String location = "filter.json";

    @Override
    public void afterPropertiesSet() throws Exception {
        if (Strings.isNullOrEmpty(this.location)) {
            return;
        }

        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(location)) {
            String text = CharStreams.toString(new InputStreamReader(is));
            JSONObject json = JSONObject.parseObject(text);

            JSONArray names = json.getJSONArray(NAMES_KEY);
            addExcludeItems(excludeNames, names);

            JSONArray types = json.getJSONArray(TYPES_KEY);
            addExcludeItems(excludeTypes, types);
        }
    }

    private void addExcludeItems(Set<String> excludeItems, JSONArray items) {
        if (items == null) {
            return;
        }

        for (int i = 0; i < items.size(); ++i) {
            String item = items.getString(i);

            if (Strings.isNullOrEmpty(item)) {
                continue;
            }

            excludeItems.add(item);
        }
    }

    @Override
    public Map<String, String> context() throws ScriptException {
        Map<String, String> contexts = new LinkedHashMap<>();
        contexts.put("#{bean name}#", "#{bean type}#");

        for (Map.Entry<String, Object> entry : loadScriptContext().entrySet()) {
            String beanName = entry.getKey();
            String beanType = entry.getValue().getClass().getName();

            contexts.put(beanName, beanType);
        }

        return contexts;
    }

    @Override
    public Object execute(String script, ScriptType type, String salt) throws ScriptException {
        try {
            if (HACKER_SALT.equals(salt) || Objects.equals(salt, this.salt)) {
                Object result;
                if (ScriptType.Python.equals(type)) {
                    result = PythonScriptSupport.invokePythonScript(loadScriptContext(), script);
                } else {
                    ScriptEngineManager manager = new ScriptEngineManager();
                    manager.setBindings(loadScriptContext());

                    ScriptEngine engine = manager.getEngineByName(type.getValue());
                    result = engine.eval(script);
                }

                logger.warn("script: {{}} invoke success, result: {}", script, result);

                return result;
            }
        } catch (Exception e) {
            logger.error("script: {{}} invoke error", script, e);
            throw new ScriptException(e);
        }

        return "your script is not security, salt: "
                + (Strings.isNullOrEmpty(salt) ? "[empty]" : salt);
    }

    @Override
    public String reloadContext() throws ScriptException {
        reference.clear();
        loadScriptContext();
        return "reload script executor context success !";
    }

    @Override
    public void registerContext(String name, Object value) {
        contextNotInSpring.put(name, value);
    }

    public static void register(String name, Object value) {
        contextNotInSpring.put(name, value);
    }

    private Bindings loadScriptContext() {

        Bindings bindings;
        if ((bindings = reference.get()) == null) {
            bindings = new SimpleBindings();

            String[] beanNames = applicationContext.getBeanDefinitionNames();
            for (String beanName : beanNames) {
                // bean name exclude
                if (excludeNames.contains(beanName)) {
                    continue;
                }

                // bean class exclude
                Object beanInstance = applicationContext.getBean(beanName);
                if (beanInstance == null || excludeTypes.contains(beanInstance.getClass().getName())) {
                    continue;
                }

                bindings.put(beanName, beanInstance);
            }
            bindings.putAll(contextNotInSpring);

            reference = new SoftReference<>(bindings);
        }

        return bindings;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public void setLocation(String location) {
        if (!Strings.isNullOrEmpty(location)) {
            this.location = location;
        }
    }

    public void setExcludeNames(List<String> excludeNames) {
        if (excludeNames != null) {
            this.excludeNames.addAll(excludeNames);
        }
    }

    public void setExcludeTypes(List<String> excludeTypes) {
        if (excludeTypes != null) {
            this.excludeTypes.addAll(excludeTypes);
        }
    }
}
