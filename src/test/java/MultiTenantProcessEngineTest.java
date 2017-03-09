/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.deanwangpro.activiti.MyApp;
import com.deanwangpro.activiti.SpringMultiTenantProcessEngineConfiguration;
import com.deanwangpro.activiti.TenantIdentityHolder;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Joram Barrez
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=MyApp.class, loader=SpringApplicationContextLoader.class)
public class MultiTenantProcessEngineTest {
  
  @Autowired
  private SpringMultiTenantProcessEngineConfiguration config;

  @Autowired
  private ProcessEngine processEngine;

  @Autowired
  private TenantIdentityHolder tenantInfoHolder;

  @Before
  public void setup() {
//    setupTenantInfoHolder();
  }
  
  @After
  public void close() {
//    processEngine.close();
  }

  private void setupTenantInfoHolder() {

//    tenantInfoHolder.addTenant("alfresco");
//    tenantInfoHolder.addUser("alfresco", "joram");
//    tenantInfoHolder.addUser("alfresco", "tijs");
//    tenantInfoHolder.addUser("alfresco", "paul");
//    tenantInfoHolder.addUser("alfresco", "yvo");
//
//    tenantInfoHolder.addTenant("acme");
//    tenantInfoHolder.addUser("acme", "raphael");
//    tenantInfoHolder.addUser("acme", "john");
//
//    tenantInfoHolder.addTenant("starkindustries");
//    tenantInfoHolder.addUser("starkindustries", "tony");
    
//    this.tenantInfoHolder = tenantInfoHolder;
  }
  
  /*private void setupProcessEngine(boolean sharedExecutor) {
    config = new MultiSchemaMultiTenantProcessEngineConfiguration(tenantInfoHolder);

    config.setDatabaseType(MultiSchemaMultiTenantProcessEngineConfiguration.DATABASE_TYPE_H2);
    config.setDatabaseSchemaUpdate(MultiSchemaMultiTenantProcessEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE);
    
    config.setAsyncExecutorEnabled(true);
    config.setAsyncExecutorActivate(true);
    
    
    if (sharedExecutor) {
      config.setAsyncExecutor(new SharedExecutorServiceAsyncExecutor(tenantInfoHolder));
    } else {
      config.setAsyncExecutor(new ExecutorPerTenantAsyncExecutor(tenantInfoHolder));
    }
    
    config.registerTenant("alfresco", createDataSource("jdbc:h2:mem:activiti-mt-alfresco;DB_CLOSE_DELAY=1000", "sa", ""));
    config.registerTenant("acme", createDataSource("jdbc:h2:mem:activiti-mt-acme;DB_CLOSE_DELAY=1000", "sa", ""));
    config.registerTenant("starkindustries", createDataSource("jdbc:h2:mem:activiti-mt-stark;DB_CLOSE_DELAY=1000", "sa", ""));
    
    
    processEngine = config.buildProcessEngine();
  }*/
  
  @Test
  public void testStartProcessInstancesWithExecutorPerTenantAsyncExecutor() throws Exception {
    runProcessInstanceTest();
  }

  private void runProcessInstanceTest() throws InterruptedException {
    // Generate data
    startProcessInstances("joram", "T1");
    startProcessInstances("joram","T1");
    startProcessInstances("joram","T1");
    startProcessInstances("raphael", "T2");
    startProcessInstances("raphael","T2");
    completeTasks("raphael", "T2");
    startProcessInstances("tony", "T3");
    
    // Verify
    assertData("joram", "T1", 6, 3);
    assertData("raphael", "T2",0, 0);
    assertData("tony", "T3",2, 1);
    
    // Adding a new tenant
    tenantInfoHolder.addTenant("T4");

    config.registerTenant("T4");
    
    // Start process instance for new tenant
    startProcessInstances("clark","T4");
    startProcessInstances("clark","T4");
    assertData("clark", "T4", 4, 2);
    
    // Move the clock 2 hours (jobs fire in one hour)
    config.getClock().setCurrentTime(new Date(config.getClock().getCurrentTime().getTime() + (2 * 60 * 60 * 1000)));
    Thread.sleep(15000L); // acquire time is 10 seconds, so 15 should be ok
    
    assertData("joram", "T1",6, 0);
    assertData("raphael", "T2",0, 0);
    assertData("tony", "T3",2, 0);
    assertData("clark", "T4",4, 0);
  }
  
  private void startProcessInstances(String userId, String tenantId) {
    
    System.out.println();
    System.out.println("Starting process instance for user " + userId);
    
    tenantInfoHolder.setCurrentTenantId(tenantId);
    
    Deployment deployment = processEngine.getRepositoryService().createDeployment()
          .addClasspathResource("oneTaskProcess.bpmn20.xml")
          .addClasspathResource("jobTest.bpmn20.xml")
          .tenantId(tenantId).deploy();
    System.out.println("Process deployed! Deployment id is " + deployment.getId());
    
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("data", "Hello from " + userId);
    
    ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceByKeyAndTenantId("oneTaskProcess", vars, tenantId);
    List<Task> tasks = processEngine.getTaskService().createTaskQuery().taskTenantId(tenantId).processInstanceId(processInstance.getId()).list();
    System.out.println("Got " + tasks.size() + " tasks");
    
    System.out.println("Got " + processEngine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceTenantId(tenantId).count() + " process instances in the system");
    
    // Start a process instance with a Job
    processEngine.getRuntimeService().startProcessInstanceByKeyAndTenantId("jobTest", tenantId);
    
    tenantInfoHolder.clearCurrentTenantId();
  }
  
  private void completeTasks(String userId, String tenantId) {
    tenantInfoHolder.setCurrentTenantId(tenantId);
    
   for (Task task : processEngine.getTaskService().createTaskQuery().taskTenantId(tenantId).list()) {
     processEngine.getTaskService().complete(task.getId());
   }
    
    tenantInfoHolder.clearCurrentTenantId();
  }
  
  private void assertData(String userId, String tenantId, long nrOfActiveProcessInstances, long nrOfActiveJobs) {
    tenantInfoHolder.setCurrentTenantId(tenantId);
    
    Assert.assertEquals(nrOfActiveProcessInstances, processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceTenantId(tenantId).count());
    Assert.assertEquals(nrOfActiveProcessInstances, processEngine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceTenantId(tenantId).unfinished().count());
    Assert.assertEquals(nrOfActiveJobs, processEngine.getManagementService().createJobQuery().jobTenantId(tenantId).count());
    
    tenantInfoHolder.clearCurrentTenantId();
  }
  
  // Helper //////////////////////////////////////////


}
