// Copyright (c) 2012-2016 Saarland University
// All rights reserved.
//
// Author: Konrad Jamrozik, jamrozik@st.cs.uni-saarland.de
//
// This file is part of the "DroidMate" project.
//
// www.droidmate.org

package org.droidmate.monitor_generator

import groovy.util.logging.Slf4j
import org.droidmate.MonitorConstants
import org.droidmate.apis.ApiMethodSignature
import org.droidmate.common.logcat.Api
import org.droidmate.common.logcat.ApiLogcatMessage

/**
 * Class that add the instrumentation code to {@link MonitorJavaTemplate}
 *
 * <p> Informatin about update to Android 6.0: </p>
 *
 * Using AAR on ANT Script:
 *    http://community.openfl.org/t/integrating-aar-files/6837/2
 *    http://stackoverflow.com/questions/23777423/aar-in-eclipse-ant-project
 *
 * Using legacy org.apache.http package on Android 6.0
 *    http://stackoverflow.com/questions/33357561/compiling-google-download-library-targing-api-23-android-marshmallow
 *    http://stackoverflow.com/questions/32064633/how-to-include-http-library-in-android-project-using-m-preview-in-eclipse-ant-bu
 *    (Not working, just for information) http://stackoverflow.com/questions/31653002/how-to-use-the-legacy-apache-http-client-on-android-marshmallow
 *
 */
@Slf4j
class RedirectionsGenerator implements IRedirectionsGenerator
{

  private static final nl   = System.lineSeparator()
  private static final ind6 = "      "
  private static final ind4 = "    "

  private static String redirMethodNamePrefix = "redir_";
  private static String redirMethodDefPrefix = "Lorg/droidmate/monitor_generator/generated/Monitor;->$redirMethodNamePrefix";

  private static Map<Integer, String> ctorRedirNames = [:]
  private final  AndroidAPI           androidApi
  

  RedirectionsGenerator(AndroidAPI androidApi)
  {
    this.androidApi = androidApi
  }

  @Override
  List<String> generateCtorCallsAndTargets(List<ApiMethodSignature> signatures)
  {
    StringBuilder calls = new StringBuilder()
    StringBuilder out = new StringBuilder()
    signatures.findAll {it.isConstructor()}.eachWithIndex {ApiMethodSignature ams, int id ->

      ams.with {

        // --- Redirecting constructor calls ---

        String objectClassAsMethodName = getObjectClassAsMethodName(objectClass)
        ctorRedirNames[id] = "${id}_${objectClassAsMethodName}_ctor${paramClasses.size()}"

        if (androidApi == AndroidAPI.API_19)
        {
          String fromId = $/"${ams.objectClassJni}-><init>(${ams.paramsJni})V"/$
          /* We use Object here instead of the proper name because sometimes the class is hidden from public Android API
             and so the generated file couldn't be compiled. The instrumentation still works with Object, though.
          */
          String objectClassJni = "Ljava/lang/Object;" // ams.objectClassJni
          String toId = $/"$redirMethodDefPrefix${ctorRedirNames[id]}($objectClassJni${ams.paramsJni})V"/$

          calls << ind6 + "ctorHandles.add(Instrumentation.redirectMethod(" + nl
          calls << ind6 + ind4 + "Signature.fromIdentifier($fromId, classLoaders)," + nl
          calls << ind6 + ind4 + "Signature.fromIdentifier($toId, classLoaders)));" + nl
          calls << ind6 + nl
        }

        // --- The generation of redirected method (target of the .redirectMethod call) ---
        
        // Items for method signature.

        String objectClassWithDots = getObjectClassWithDots(objectClass)
        List<String> paramVarNames = buildParamVarNames(it)
        String formalParams = buildFormalParams(it, paramVarNames)

        // Items for logcat message payload.
        String stackTraceVarName = "stackTrace"
        String threadIdVarName = "threadId"
        List<String> paramValues = paramVarNames.collect {"convert(${it})"}
        String apiLogcatMessagePayload = buildApiLogcatMessagePayload(it, paramValues, threadIdVarName, stackTraceVarName)

        // Items for call to Instrumentation method returning value.

        String commaSeparatedParamVars = buildCommaSeparatedParamVarNames(ams, paramVarNames)

        if (androidApi == AndroidAPI.API_23)
        {
          out << ind4 + "@Hook(\"$objectClass->$methodName\") " + nl
        }
        out << ind4 + "public static void $redirMethodNamePrefix${ctorRedirNames[id]}($objectClassWithDots _this$formalParams)" + nl
        out << ind4 + "{" + nl
        out << ind4 + ind4 + "String $stackTraceVarName = getStackTrace();" + nl
        out << ind4 + ind4 + "long $threadIdVarName = getThreadId();" + nl
        out << ind4 + ind4 + "Log.${MonitorConstants.loglevel}(\"${MonitorConstants.tag_api}\", \"$apiLogcatMessagePayload\"); " + nl
        out << ind4 + ind4 + "addCurrentLogs(\"$apiLogcatMessagePayload\");" + nl
        out << ind4 + ind4 + "hookPlugin.hookBeforeApiCall(\"$apiLogcatMessagePayload\");" + nl
        if (androidApi == AndroidAPI.API_19)
        {
          out << ind4 + ind4 + "Instrumentation.callVoidMethod(ctorHandles.get($id), _this$commaSeparatedParamVars);" + nl
          out << ind4 + ind4 + "hookPlugin.hookAfterApiCall(\"$apiLogcatMessagePayload\", null);" + nl
        } else if (androidApi == AndroidAPI.API_23)
        {
          out << ind4 + ind4 + "OriginalMethod.by(new \$() {}).invoke(_this$commaSeparatedParamVars);" + nl
          out << ind4 + ind4 + "hookPlugin.hookAfterApiCall(\"$apiLogcatMessagePayload\", null);" + nl
        } else throw new IllegalStateException()
        out << ind4 + "}" + nl
        out << ind4 + nl
      }

    }
    return [calls.toString(), out.toString()]

  }

  private static String getObjectClassWithDots(String objectClass)
  {
    /* We use Object here instead of the proper name because sometimes the class is hidden from public Android API
       and so the generated file couldn't be compiled. The instrumentation still works with Object, though.
      */
    return "Object" //  objectClass.replace("\$", ".")
  }


  @Override
  String generateMethodTargets(List<ApiMethodSignature> signatures)
  {
    return signatures
      .findAll {!it.isConstructor()} // Skip ctors here. They are handled in #generateCtorCallsAndTargets()
      .findAll {!(it.objectClass.startsWith("android.test."))} // For justification, see [1] in dev doc at the end of this method.
      .collect {ApiMethodSignature ams ->

      StringBuilder out = new StringBuilder()

      ams.with {

        // Items for method signature.

        String objectClassAsMethodName = getObjectClassAsMethodName(objectClass)
        String redirMethodName = "$redirMethodNamePrefix${objectClassAsMethodName}_$methodName${paramClasses.size()}"
        String objectClassWithDots = getObjectClassWithDots(objectClass)
        String thisParam = isStatic ? "" : "$objectClassWithDots _this"
        List<String> paramVarNames = buildParamVarNames(it)
        String formalParams = buildFormalParams(it, paramVarNames)

        // Items for logcat message payload.

        String stackTraceVarName = "stackTrace"
        String threadIdVarName = "threadId"
        List<String> paramValues = paramVarNames.collect {"convert(${it})"}
        String apiLogcatMessagePayload = buildApiLogcatMessagePayload(it, paramValues, threadIdVarName, stackTraceVarName)

        // Items for handling return values from called API method.
        
        String castType = "(${degenerify(returnClass)})"
        String returnStatement = "return $castType "
        boolean returnsVoid = returnClass == "void"
        
        String thisVarOrClass = isStatic ? "${objectClassWithDots}.class" : "_this"
        String commaSeparatedParamVars = buildCommaSeparatedParamVarNames(ams, paramVarNames)

        if (androidApi == AndroidAPI.API_19)
        {
          out << ind4 + "@Redirect(\"$objectClass->$methodName\") " + nl
        } else if (androidApi == AndroidAPI.API_23)
        {
          out << ind4 + "@Hook(\"$objectClass->$methodName\") " + nl
        } else throw new IllegalStateException()
        
        out << ind4 + "public static $returnClass $redirMethodName($thisParam$formalParams)" + nl
        out << ind4 + "{" + nl
        
        if (objectClass == "android.util.Log" && methodName == "i" && paramClasses.size() == 2)
        {
          out << ind4 + ind4 + "if (p0.startsWith(\"${MonitorConstants.tag_prefix}\"))"
          out << ind4 + ind4 + "  return 0;"
        }
        
        out << ind4 + ind4 + "String $stackTraceVarName = getStackTrace();" + nl
        out << ind4 + ind4 + "long $threadIdVarName = getThreadId();" + nl
        out << ind4 + ind4 + "Log.${MonitorConstants.loglevel}(\"${MonitorConstants.tag_api}\", \"$apiLogcatMessagePayload\"); " + nl
        out << ind4 + ind4 + "addCurrentLogs(\"$apiLogcatMessagePayload\");" + nl
        out << ind4 + ind4 + "hookPlugin.hookBeforeApiCall(\"$apiLogcatMessagePayload\");" + nl
        
        if (androidApi == AndroidAPI.API_19)
        {
          String instrCallStatic = isStatic ? "Static" : ""
          String instrCallType = returnClass in instrCallMethodTypeMap.keySet() ? instrCallMethodTypeMap[returnClass] : "Object"
          out << ind4 + ind4 + "class \$ {} " + nl
          if (returnsVoid)
          {
            out << ind4 + ind4 + "Instrumentation.call${instrCallStatic}${instrCallType}Method(\$.class, ${thisVarOrClass}${commaSeparatedParamVars});" + nl
            out << ind4 + ind4 + "hookPlugin.hookAfterApiCall(\"$apiLogcatMessagePayload\", null);" + nl
          } else
          {
            out << ind4 + ind4 + "Object returnVal = Instrumentation.call${instrCallStatic}${instrCallType}Method(\$.class, ${thisVarOrClass}${commaSeparatedParamVars});" + nl
            out << ind4 + ind4 + "${returnStatement}hookPlugin.hookAfterApiCall(\"$apiLogcatMessagePayload\", ${castType}returnVal);" + nl
          }
        }
        else if (androidApi == AndroidAPI.API_23)
        {
          String invocation
          if (!isStatic)
            invocation = "OriginalMethod.by(new \$() {}).invoke(${thisVarOrClass}${commaSeparatedParamVars})"
          else
          {
            commaSeparatedParamVars = commaSeparatedParamVars.substring(2);
            invocation = "OriginalMethod.by(new \$() {}).invokeStatic(${commaSeparatedParamVars})"
          }
          if (returnsVoid)
          {
            out << ind4 + ind4 + "$invocation;" + nl
            out << ind4 + ind4 + "hookPlugin.hookAfterApiCall(\"$apiLogcatMessagePayload\", null);" + nl
          }
          else
          {
            out << ind4 + ind4 + "Object returnVal = $invocation;" + nl
            out << ind4 + ind4 + "${returnStatement}hookPlugin.hookAfterApiCall(\"$apiLogcatMessagePayload\", ${castType}returnVal);" + nl
          }
        } else throw new IllegalStateException()
        out << ind4 + "}" + nl
        out << ind4 + nl
      }

      return out.toString()
    }.join("")
    /*
    
    Note: Redirection fails on classes from android.test.*
    Snippet of observed exception stack trace:

    (...)
    java.lang.ClassNotFoundException: Didn't find class "android.test.SyncBaseInstrumentation" on path:
    DexPathList[[zip file "/data/local/tmp/monitor.apk"]
    (...)

    */
  }

  private static String getObjectClassAsMethodName(String objectClass)
  {
    return objectClass.replace("\$", "_").replace(".", "_")
  }

  private static List<String> buildParamVarNames(ApiMethodSignature ams)
  {
    return ams.paramClasses.isEmpty() ? [] : (0..ams.paramClasses.size() - 1).collect {"p$it"}
  }

  private static String buildFormalParams(ApiMethodSignature ams, List<String> paramVarNames)
  {
    return ams.paramClasses.isEmpty() ? "" : (ams.isStatic ? "" : ", ") + (0..ams.paramClasses.size() - 1).collect {
      ams.paramClasses[it] + " " + paramVarNames[it]
    }.join(", ")
  }

  private
  static String buildApiLogcatMessagePayload(ApiMethodSignature ams, List<String> paramValues, String threadIdVarName, String stackTraceVarName)
  {

    return ApiLogcatMessage.toLogcatMessagePayload(
      new Api(ams.objectClass, ams.methodName, ams.returnClass, ams.paramClasses, paramValues, threadIdVarName, stackTraceVarName),
      /* useVarNames */ true)
  }

  private static String buildCommaSeparatedParamVarNames(ApiMethodSignature ams, List<String> paramVarNames)
  {
    return ams.paramClasses.isEmpty() ? (ams.isStatic ? ", 0" : "") :
      ", " + (0..ams.paramClasses.size() - 1).collect {paramVarNames[it]}.join(", ")
  }

  private static String degenerify(String returnClass)
  {
    String degenerified
    // Generic types contain space in their name, e.g. "<T> T".
    if (returnClass.contains(" "))
      degenerified = returnClass.dropWhile {it != " "}.drop(1) // Will return only "T" in the above-given example.
    else
      degenerified = returnClass // No generics, return type as-is.
    
    // This conversion is necessary to avoid error of kind "error: incompatible types: Object cannot be converted to boolean"
    if (degenerified == "boolean")
      degenerified = "Boolean"
    if (degenerified == "int")
      degenerified = "Integer"
    if (degenerified == "float")
      degenerified = "Float"
    if (degenerified == "double")
      degenerified = "Double"
    if (degenerified == "long")
      degenerified = "Long"
    if (degenerified == "byte")
      degenerified = "Byte"
    if (degenerified == "short")
      degenerified = "Short"
    if (degenerified == "char")
      degenerified = "Character"    
    return degenerified
  }
  /*
    The generated source will be compiled with java 1.5 which requires this mapping.
    It is compiled with java 1.5 because it is build with the old ant-based android SDK build and java 1.5
    is what the ant build file definition in Android SDK defines.
   */
  static private final instrCallMethodTypeMap = [
    "void"   : "Void",
    "boolean": "Boolean",
    "byte"   : "Byte",
    "char"   : "Character",
    "float"  : "Float",
    "int"    : "Int",
    "long"   : "Long",
    "short"  : "Short",
    "double" : "Double"
  ]


}
