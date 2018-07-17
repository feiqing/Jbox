package com.github.jbox.script;


import javax.script.ScriptException;
import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.1
 * @since jbox-1.4.0 (16/10/17 下午4:11).
 */
public interface IScriptExecutor {

    Map<String, String> context() throws ScriptException;

    Object execute(String script, ScriptType type, String salt) throws ScriptException;

    String reloadContext() throws ScriptException;

    void registerContext(String name, Object value) throws ScriptException;
}
