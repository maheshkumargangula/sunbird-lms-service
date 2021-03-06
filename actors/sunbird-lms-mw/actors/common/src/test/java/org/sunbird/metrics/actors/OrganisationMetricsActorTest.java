package org.sunbird.metrics.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.HttpClientUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import scala.concurrent.Promise;

/**
 * Junit test cases for Org creation and consumption metrics.
 *
 * @author arvind.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ElasticSearchRestHighImpl.class,
  HttpClientUtil.class,
  ServiceFactory.class,
  EsClientFactory.class
})
@PowerMockIgnore("javax.management.*")
public class OrganisationMetricsActorTest {

  private ActorSystem system = ActorSystem.create("system");
  private static final Props prop = Props.create(OrganisationMetricsActor.class);
  private static String userId = "456-123";
  private static final String EXTERNAL_ID = "ex00001lvervk";
  private static final String PROVIDER = "pr00001kfej";
  private static final String CHANNEL = "hjryr9349";
  private static final String orgId = "ofure8ofp9yfpf9ego";
  private static ObjectMapper mapper = new ObjectMapper();
  private static Map<String, Object> userOrgMap = new HashMap<>();
  private static CassandraOperationImpl cassandraOperation;
  private static final String HTTP_POST = "POST";
  private ElasticSearchService esService;

  @BeforeClass
  public static void setUp() {

    userOrgMap = new HashMap<>();
    userOrgMap.put(JsonKey.ID, orgId);
    userOrgMap.put(JsonKey.IS_ROOT_ORG, true);
    userOrgMap.put(JsonKey.CHANNEL, CHANNEL);
    userOrgMap.put(JsonKey.PROVIDER, PROVIDER);
    userOrgMap.put(JsonKey.EXTERNAL_ID, EXTERNAL_ID);
    userOrgMap.put(JsonKey.HASHTAGID, orgId);
    userOrgMap.put(JsonKey.ORG_NAME, "rootOrg");
    userOrgMap.put(JsonKey.FIRST_NAME, "user_first_name");

    PowerMockito.mockStatic(ServiceFactory.class);

    cassandraOperation = mock(CassandraOperationImpl.class);

    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.mockStatic(HttpClientUtil.class);
  }

  @Before
  public void before() {
    PowerMockito.mockStatic(HttpClientUtil.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    Response response = createCassandraInsertSuccessResponse();
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(response);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(userOrgMap);
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
  }

  @SuppressWarnings({"deprecation", "unchecked"})
  @Test
  public void testOrgCreationMetricsSuccess() throws JsonProcessingException {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(prop);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(userOrgMap);
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
    Request actorMessage = new Request();
    actorMessage.put(JsonKey.ORG_ID, orgId);
    actorMessage.put(JsonKey.PERIOD, "7d");
    actorMessage.setOperation(ActorOperations.ORG_CREATION_METRICS.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exception =
      probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertNotNull(exception);
  }

  @SuppressWarnings({"unchecked", "deprecation"})
  @Test
  public void testOrgConsumptionMetricsSuccess() throws JsonProcessingException {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(prop);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(userOrgMap);
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.ORG_ID, orgId);
    actorMessage.put(JsonKey.PERIOD, "7d");
    actorMessage.setOperation(ActorOperations.ORG_CONSUMPTION_METRICS.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exception =
      probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertNotNull(exception);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testOrgCreationMetricsWithInvalidOrgId() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(prop);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(null);
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.ORG_ID, "ORG_001_INVALID");
    actorMessage.put(JsonKey.PERIOD, "7d");
    actorMessage.setOperation(ActorOperations.ORG_CREATION_METRICS.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException e =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertEquals(ResponseCode.invalidOrgData.getErrorCode(), e.getCode());
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testOrgConsumptionMetricsWithInvalidOrgId() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(prop);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(null);
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.ORG_ID, "ORG_001_INVALID");
    actorMessage.put(JsonKey.PERIOD, "7d");
    actorMessage.setOperation(ActorOperations.ORG_CONSUMPTION_METRICS.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException e =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertEquals(ResponseCode.invalidOrgData.getErrorCode(), e.getCode());
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testOrgCreationMetricsInvalidPeriod() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(prop);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(userOrgMap);
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.ORG_ID, orgId);
    actorMessage.put(JsonKey.PERIOD, "10d");
    actorMessage.setOperation(ActorOperations.ORG_CREATION_METRICS.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException e =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertEquals("INVALID_PERIOD", e.getCode());
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testOrgConsumptionMetricsWithInvalidPeriod() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(prop);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(userOrgMap);
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.ORG_ID, orgId);
    actorMessage.put(JsonKey.PERIOD, "10d");
    actorMessage.setOperation(ActorOperations.ORG_CONSUMPTION_METRICS.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException e =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertEquals("INVALID_PERIOD", e.getCode());
  }

  @SuppressWarnings({"deprecation", "unchecked"})
  @Test
  public void testOrgCreationMetricsReportDataSuccess() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(prop);

    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(userOrgMap);
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.ORG_ID, orgId);
    actorMessage.put(JsonKey.PERIOD, "7d");
    actorMessage.put(JsonKey.REQUESTED_BY, userId);
    actorMessage.put(JsonKey.FORMAT, "csv");
    actorMessage.setOperation(ActorOperations.ORG_CREATION_METRICS_REPORT.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Map<String, Object> data = res.getResult();
    String id = (String) data.get(JsonKey.REQUEST_ID);
    Assert.assertNotNull(id);
  }

  @SuppressWarnings({"deprecation", "unchecked"})
  @Test
  public void testOrgConsumptionMetricsReportDataSuccess() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(prop);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(userOrgMap);
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.ORG_ID, orgId);
    actorMessage.put(JsonKey.PERIOD, "7d");
    actorMessage.put(JsonKey.REQUESTED_BY, userId);
    actorMessage.put(JsonKey.FORMAT, "csv");
    actorMessage.setOperation(ActorOperations.ORG_CONSUMPTION_METRICS_REPORT.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Map<String, Object> data = res.getResult();
    String id = (String) data.get(JsonKey.REQUEST_ID);
    Assert.assertNotNull(id);
  }

  private Map<String, Object> orgCreationSuccessMap() {

    Map<String, Object> responseMap = new HashMap<>();
    Map<String, Object> resultMap = new HashMap<>();
    Map<String, Object> aggregateMap = new HashMap<>();
    Map<String, Object> statusMap = new HashMap<>();
    aggregateMap.put(JsonKey.STATUS, statusMap);
    resultMap.put(JsonKey.AGGREGATIONS, aggregateMap);
    responseMap.put(JsonKey.RESULT, resultMap);
    return responseMap;
  }

  private Map<String, Object> orgConsumptionSuccessMap() {
    Map<String, Object> responseMap = new HashMap<>();
    Map<String, Object> resultMap = new HashMap<>();
    Map<String, Object> aggregateMap = new HashMap<>();
    List<Map<String, Object>> metricsList = new ArrayList<>();
    Map<String, Object> statusMap = new HashMap<>();
    aggregateMap.put(JsonKey.STATUS, statusMap);
    resultMap.put(JsonKey.METRICS, metricsList);
    Map<String, Object> summaryMap = new HashMap<>();
    resultMap.put(JsonKey.SUMMARY, summaryMap);
    responseMap.put(JsonKey.RESULT, resultMap);
    return responseMap;
  }

  private Response createCassandraInsertSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }
}
