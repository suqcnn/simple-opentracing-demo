package com.aliyun.opentracingdemo.demo05;

import com.aliyun.openservices.log.jaeger.sender.AliyunLogSender;
import com.aliyun.openservices.log.jaeger.sender.util.TracerHelper;
import com.google.common.collect.ImmutableMap;

import com.uber.jaeger.samplers.ConstSampler;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.opentracing.Scope;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

public class Publisher extends Application<Configuration> {

  @Path("/publish")
  @Produces(MediaType.TEXT_PLAIN)
  public class PublisherResource {

    @GET
    public String publish(@QueryParam("helloStr") String helloStr,
        @Context HttpHeaders httpHeaders) {
      MultivaluedMap<String, String> rawHeaders = httpHeaders.getRequestHeaders();
      String spanContextStr = rawHeaders.get("trace-id").get(0);
      System.out.println(spanContextStr);
      if (rawHeaders.get("trace-id") != null) {
        String spanContextString = rawHeaders.get("trace-id").get(0);
        try (Scope scope = TracerHelper.traceLatency("publish", spanContextString)) {
          return doPublish(helloStr, scope);
        }
      } else {
        try (Scope scope = TracerHelper.traceLatency("publish")) {
          return doPublish(helloStr, scope);
        }
      }
    }
  }

  private String doPublish(String helloStr, Scope scope) {
    System.out.println(helloStr);
    scope.span().log(ImmutableMap.of("event", "println", "value", helloStr));
    return "published";
  }

  @Override
  public void run(Configuration configuration, Environment environment) throws Exception {
    environment.jersey().register(new PublisherResource());
  }

  private static AliyunLogSender buildAliyunLogSender() {
    String projectName = System.getenv("PROJECT");
    String logStore = System.getenv("LOG_STORE");
    String endpoint = System.getenv("ENDPOINT");
    String accessKeyId = System.getenv("ACCESS_KEY_ID");
    String accessKey = System.getenv("ACCESS_KEY_SECRET");
    return new AliyunLogSender.Builder(projectName, logStore, endpoint, accessKeyId, accessKey)
        .build();
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("dw.server.applicationConnectors[0].port", "8082");
    System.setProperty("dw.server.adminConnectors[0].port", "9082");
    TracerHelper
        .buildTracer("publisher", buildAliyunLogSender(), new ConstSampler(true));
    new Publisher().run(args);
  }
}
