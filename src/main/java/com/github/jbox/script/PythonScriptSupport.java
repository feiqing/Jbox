package com.github.jbox.script;

import javax.script.Bindings;

import org.python.util.PythonInterpreter;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/9/13 11:09:00.
 */
class PythonScriptSupport {

    /**
     * 由于Python解释器不会自动将脚本执行结果返回,
     * 因此需要让用户在传入的Python脚本中将最终需要返回的值全部写入到result变量中,
     * 在脚本执行结束后返回给调用方.
     */
    private static final String SCRIPT_RESULT_KEY = "result";

    static Object invokePythonScript(Bindings context, String script) {
        PythonInterpreter interpreter = new PythonInterpreter();
        context.forEach(interpreter::set);
        interpreter.exec(script);
        return interpreter.get(SCRIPT_RESULT_KEY);
    }
}
