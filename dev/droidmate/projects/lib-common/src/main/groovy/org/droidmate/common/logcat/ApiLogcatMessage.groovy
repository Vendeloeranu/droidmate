// Copyright (c) 2013-2015 Saarland University
// All right reserved.
//
// Author: Konrad Jamrozik, jamrozik@st.cs.uni-saarland.de
//
// This file is part of the "DroidMate" project.
//
// www.droidmate.org

// WISH move to org.droidmate.logcat. Now it is not done as it would break deserialization. See also: org.droidmate.deprecated_still_used.DeprecatedClassesDeserializer
package org.droidmate.common.logcat

import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.droidmate.apis.IApi
import org.droidmate.common.ClassFileFormat
import org.droidmate.common.DroidmateException
import org.droidmate.logcat.IApiLogcatMessage
import org.droidmate.logcat.ITimeFormattedLogcatMessage

/**
 * See {@link IApiLogcatMessage}
 */
@Canonical
@Slf4j
class ApiLogcatMessage implements IApiLogcatMessage, Serializable
{

  private static final long serialVersionUID = 1

  private static final String spaceEscapeInParamValue = "_"

  @Delegate
  ITimeFormattedLogcatMessage message

  @Delegate
  IApi api

  static ApiLogcatMessage from(ITimeFormattedLogcatMessage logcatMessage)
  {
    assert logcatMessage?.messagePayload?.size() > 0

    IApi monitoredApiCallData = from(logcatMessage.messagePayload)

    return new ApiLogcatMessage(logcatMessage, monitoredApiCallData)
  }

  /**
   * <p>
   * Example string to parse:
   * <pre><code>
   *   objCls: java.net.URLConnection mthd: &lt;init> retCls: void params: java.net.URL http://www.google.com
   * </code></pre>
   */
  static IApi from(String logcatMessagePayload)
  {
    assert logcatMessagePayload?.size() > 0

    def payload = new ApiLogcatMessagePayload(logcatMessagePayload)
    return new Api(payload.objectClass, payload.methodName, payload.returnClass, payload.paramTypes, payload.paramValues, payload.threadId, payload.stackTrace)
  }


  @Override
  public String toString()
  {
    message.toString()
  }

  /**
   * <p>
   * If {@code useVarNames} is true, the output string, when treated as code, will evaluate
   * {@code api.paramValues} and {@code api.stackTrace} as variable names instead of constants in a string.
   *
   * </p><p>
   * As an example, if {@code useVarNames} is false, the method will return a string like (not an actual implementation, but similar):
   * <pre><code>"lorem ipsum param1: param1value param2: param2value stackTrace: contents"</code></pre>
   * but if set to true it will return:
   * <pre><code>"lorem ipsum param1: "+param1Value+" param2: "+param2Value+" stackTrace: "+stackTrace+""</pre></code>
   *
   * </p>
   */
  static String toLogcatMessagePayload(IApi api, boolean useVarNames = false)
  {
    assert api.paramTypes.size() == api.paramValues.size()

    List<String> processedParamTypes = api.paramTypes.collect {it.replace(" ", ClassFileFormat.genericTypeEscape)}
    List<String> processedParamValues = api.paramValues.collect {
      useVarNames ? /"+/ + it + /+"/ : it.replace(" ", spaceEscapeInParamValue)
    }
    String actualThreadId = useVarNames ? /"+/ + api.threadId + /+"/ : api.threadId
    String actualStackTrace = useVarNames ? /"+/ + api.stackTrace + /+"/ : api.stackTrace

    return new ApiLogcatMessagePayload(
      actualThreadId, api.objectClass, api.methodName, api.returnClass, processedParamTypes, processedParamValues, actualStackTrace)
      .toString()
  }

  private static class ApiLogcatMessagePayload
  {

    private static final String keyword_TId        = "TId:"
    private static final String keyword_objCls     = "objCls:"
    private static final String keyword_mthd       = "mthd:"
    private static final String keyword_retCls     = "retCls:"
    private static final String keyword_params     = "params:"
    // !!! DUPLICATION WARNING !!! because of 'stacktrace: ': with org.droidmate.lib_android.MonitorJavaTemplate.MonitorTCPServer#removeSocketInitLogFromMonitorTCPServer
    private static final String keyword_stacktrace = "stacktrace:"

    private static
    final List<String> keywords = [keyword_TId, keyword_objCls, keyword_mthd, keyword_retCls, keyword_params, keyword_stacktrace]

    private final String       threadId
    private final String       objectClass
    private final String       methodName
    private final String       returnClass
    private final List<String> paramTypes
    private final List<String> paramValues
    private final String       stackTrace


    ApiLogcatMessagePayload(String threadId, String objectClass, String methodName, String returnClass, List<String> paramTypes, List<String> paramValues, String stackTrace)
    {
      this.threadId = threadId
      this.objectClass = objectClass
      this.methodName = methodName
      this.returnClass = returnClass
      this.paramTypes = paramTypes
      this.paramValues = paramValues
      this.stackTrace = stackTrace
    }

    ApiLogcatMessagePayload(String payload)
    {
      /* WISH instead of this complex process of extracting the API method signature from a serialized string, the monitor should
      send through TCP a list of strings, not a string. See: org.droidmate.lib_android.MonitorJavaTemplate.addCurrentLogs

      Currently such implementation is in place because in the past the API logs were read from logcat, not from TCP socket.
      So far I decided just adapt the new TCP interface to send the same data type as it went through logcat. I did it because
      then editing the monitor source file was a pain. Since then I streamlined the process so it should be easier.

       */

      List<String> elements = payload.tokenize(' ')
      assert !elements.empty

      addThreadIdIfNecessary(elements)
      Map<String, List<String>> keywordToValues = computeKeywordToValues(elements, payload)
      Tuple2<List<String>, List<String>> params = splitAndValidateParams(keywordToValues)

      this.threadId = keywordToValues[keyword_TId].findSingle()
      this.objectClass = keywordToValues[keyword_objCls].findSingle()
      this.methodName = keywordToValues[keyword_mthd].findSingle()
      this.returnClass = keywordToValues[keyword_retCls].findSingle()
      this.paramTypes = params.first
      this.paramValues = params.second
      this.stackTrace = keywordToValues[keyword_stacktrace].join(" ")
    }

    private void addThreadIdIfNecessary(List<String> elements)
    {
      if (!(elements.first() == keyword_TId))
      {
        elements.add(0, "?")
        elements.add(0, keyword_TId)
      }
    }

    private Map<String, List<String>> computeKeywordToValues(List<String> elements, String payload)
    {
      Map<Integer, String> indexToKeyword = keywords.collectEntries {String keyword ->

        List<Number> indexes = elements.findIndexValues {it == keyword}
        assert indexes.size() >= 1

        if (indexes.size() > 1)
          throw new DroidmateException("An API logcat message payload contains the keyword $keyword more than once. " +
            "DroidMate doesn't support such case yet. The offending payload:\n$payload")

        assert indexes.size() == 1

        [(elements.findIndexOf {it == keyword}): keyword]
      }
      Set<Integer> keywordIndexes = indexToKeyword.keySet()

      Map<String, List<String>> keywordToValues = keywords.collectEntries {[(it): []]}
      elements.eachWithIndex {String element, int i ->

        if (element in keywords)
          return

        int elementKeywordIndex = keywordIndexes.findAll {it < i}.max()
        keywordToValues[indexToKeyword[elementKeywordIndex]].add(element)
      }
      return keywordToValues
    }

    private Tuple2<List<String>, List<String>> splitAndValidateParams(Map<String, List<String>> keywordToValues)
    {
      assert keywordToValues[keyword_params].size() % 2 == 0

      List<String> paramTypes = keywordToValues[keyword_params].indexed().findAll {it.key % 2 == 0}.values() as List<String>
      List<String> paramValues = keywordToValues[keyword_params].indexed().findAll {it.key % 2 == 1}.values() as List<String>
      paramTypes.each {assert it.matches(ClassFileFormat.javaTypePattern)}
      Tuple2<List<String>, List<String>> params = new Tuple2(paramTypes, paramValues)
      return params
    }

    @Override
    public String toString()
    {
      String.format("$keyword_TId %s $keyword_objCls %s $keyword_mthd %s $keyword_retCls %s " +
        "$keyword_params %s $keyword_stacktrace %s",
        this.threadId, this.objectClass, this.methodName, this.returnClass, this.params, this.stackTrace)
    }

    String getParams()
    {
      [this.paramTypes, this.paramValues].transpose().flatten().join(" ")
    }
  }

}


