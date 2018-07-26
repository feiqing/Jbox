package com.github.jbox.trace.tlog

import com.github.jbox.trace.TraceException
import com.google.common.base.Strings
import groovy.util.slurpersupport.GPathResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static AbstractTlogConfig.ELConfig

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/10/18 18:25:00.
 */
class ELXmlResolver {

    private static final Logger logger = LoggerFactory.getLogger(TlogManager.class)

    protected static void resolve(def fileName, def xmlIs, def methodELMap, def templateELs) {
        GPathResult traces = new XmlSlurper().parse(xmlIs)

        // 1. read definitions
        Map<String, String> definitions = [:]
        traces.define.collect { definitions.put(it.@name.toString(), it.@value.toString()) }

        logConfigs(fileName, "definitions", definitions)

        // 2. resolve new configs
        boolean usedTemplate = false
        Map<String, String> references = [:]
        traces.trace.each {
            String method = ELResolveHelpers.replaceRefToRealString(it.@method.toString(), definitions)
            String ref = ELResolveHelpers.replaceRefToRealString(it.@ref.toString(), definitions)
            boolean isTemplate = Boolean.valueOf(it.@template.toString())

            // ref conflict default
            if (!Strings.isNullOrEmpty(ref) && isTemplate) {
                throw new TraceException("<trace/> config is conflict for ref and default attribute: ref=\"${ref}\" template=\"${isTemplate}\".")
            }
            if (isTemplate) {
                // multi template
                if (usedTemplate) {
                    throw new TraceException("<trace template=\"true\"> can only use once in a config file.")
                }
                usedTemplate = true
            }

            // ref another config
            if (!Strings.isNullOrEmpty(ref)) {
                references.put(method, ref)
            } else {
                resolveExpressions(isTemplate, method, it.expression, methodELMap, templateELs)
            }
        }

        // 3. process ref configs
        references.each { method, ref ->
            def refConfig = methodELMap.computeIfAbsent(ref, { key -> throw new TraceException("relative config '${key}' is not defined.") })
            methodELMap.put(method, refConfig)
        }

        logConfigs(fileName, "methodELMap", methodELMap)
    }

    private static void logConfigs(def fileName, def configName, Map<String, ?> methodELMap) {

        // find max key length
        int maxLength = Integer.MIN_VALUE
        methodELMap.each { key, value -> maxLength = key.length() > maxLength ? key.length() : maxLength }

        // sort map
        StringBuilder sb = new StringBuilder()
        Map<String, ?> sortedMap = methodELMap.sort { entry1, entry2 -> entry2.key.length() - entry1.key.length() }

        // append log msg
        sortedMap.each { key, value ->
            sb.append(String.format(" %-${maxLength + 2}s", "'${key}'"))
            sb.append(" -> ")
            sb.append(value)
            sb.append("\n")
        }

        logger.info("read from [{}] {}:\n{}", fileName, configName, sb)
    }

    private static void resolveExpressions(boolean isDefault, String method, def expressions, def methodELMap, def defaultELs) {
        expressions.each {
            iterator ->
                String key = iterator.@key
                String multi = iterator.@multi

                def elConfig
                if (!Strings.isNullOrEmpty(method) && Boolean.valueOf(multi)) {     // is multi config
                    elConfig = resolveFields(key, iterator.field)
                } else {                                                            // single config
                    elConfig = new ELConfig(key, null)
                }
                def elConfigs = methodELMap.computeIfAbsent(method, { k -> [] })
                elConfigs.add(elConfig)

                if (isDefault) {
                    defaultELs.set(elConfigs)
                }
        }
    }

    private static ELConfig resolveFields(String key, def fields) {
        List<String> inListParamEL = []
        fields.collect {
            inListParamEL.add(it.@value.toString())
        }
        new ELConfig(key, inListParamEL)
    }
}
