package com.aliyun.opentracingdemo.demo02;

import com.aliyun.openservices.log.jaeger.sender.AliyunLogSender;
import com.aliyun.openservices.log.jaeger.sender.util.TracerHelper;
import com.google.common.collect.ImmutableMap;
import com.uber.jaeger.samplers.ConstSampler;
import io.opentracing.Scope;

public class HelloSimple {

  private void sayHello(String helloTo) {
    try (Scope scope = TracerHelper.traceLatency("say-hello")) {
      scope.span().setTag("hello-to", helloTo);

      String helloStr = formatString(helloTo);
      printHello(helloStr);
    }
  }

  private String formatString(String helloTo) {
    try (Scope scope = TracerHelper.traceLatency("formatString")) {
      String helloStr = String.format("Hello, %s!", helloTo);
      scope.span().log(ImmutableMap.of("event", "string-format", "value", helloStr));
      return helloStr;
    }
  }

  private void printHello(String helloStr) {
    try (Scope scope = TracerHelper.traceLatency("printHello")) {
      System.out.println(helloStr);
      scope.span().log(ImmutableMap.of("event", "println"));
    }
  }

  private static AliyunLogSender buildAliyunLogSender() {
    String projectName = System.getenv("PROJECT");
    String logStore = System.getenv("LOG_STORE");
    String endpoint = System.getenv("ENDPOINT");
    String accessKeyId = System.getenv("ACCESS_KEY_ID");
    String accessKeySecret = System.getenv("ACCESS_KEY_SECRET");
    if (projectName == null || logStore == null || endpoint == null || accessKeyId == null || accessKeySecret == null) {
      return null;
    }
    return new AliyunLogSender.Builder(projectName, logStore, endpoint, accessKeyId, accessKeySecret)
        .build();
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      throw new IllegalArgumentException("Expecting one argument");
    }
    String helloTo = args[0];
    TracerHelper
        .buildTracer("simple-opentracing-demo", buildAliyunLogSender(), new ConstSampler(true));
    new HelloSimple().sayHello(helloTo);
    TracerHelper.closeTracer();
  }

}
