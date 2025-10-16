package com.bdsw.springboot3server.controller;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/flow")
public class FlowController {

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Resource
    private HistoryService historyService;

    @Resource
    IdentityService identityService;

    /**
     * 初始化流程
     **/
    @GetMapping("initFlow")
    @Transactional(rollbackFor = Exception.class)
    public void initFlow() throws IOException {
        // 获取bpmn文件夹的所有.bpmn20.xml文件
        ClassPathResource bpmnFolder = new ClassPathResource("bpmn/");
        var files = bpmnFolder.getFile().listFiles((dir, name) -> name.endsWith(".bpmn20.xml"));

        if (files != null && files.length > 0) {
            // 创建部署对象
            var deploymentBuilder = repositoryService.createDeployment();

            for (var file : files) {
                // 添加BPMN文件到部署
                deploymentBuilder.addInputStream(file.getName(), file.toURI().toURL().openStream());
            }

            // 执行部署
            Deployment deployment = deploymentBuilder.deploy();
        }
    }

    /***
     * 查询所有的流程实例
     **/
    @GetMapping("/queryAllDeployedProcesses")
    public List<JSONObject> queryAllDeployedProcesses() {
        List<JSONObject> jsonObjects = new ArrayList<>();

        // 查询所有流程定义
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .orderByProcessDefinitionKey().asc() // 按流程定义的 Key 排序
                .latestVersion() // 只查询每个流程定义的最新版本
                .list();

        // 打印所有已部署的流程的 key 和 name
        for (ProcessDefinition processDefinition : processDefinitions) {
            System.out.println("Process ID: " + processDefinition.getId());
            System.out.println("Process Key: " + processDefinition.getKey());
            System.out.println("Process Name: " + processDefinition.getName());
            System.out.println("Process Version: " + processDefinition.getVersion());
            JSONObject object = new JSONObject();
            object.put("id", processDefinition.getId());
            object.put("key", processDefinition.getKey());
            object.put("name", processDefinition.getName());
            object.put("version", processDefinition.getVersion());

            jsonObjects.add(object);
        }

        //分页查询
        // 创建查询对象
//        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery()
//                .latestVersion() // 只查询最新版本的流程定义
//                .orderByProcessDefinitionKey().asc(); // 按流程定义的 Key 升序排序
//
//        // 获取总条数
//        long totalCount = query.count();
//
//        // 分页查询流程定义
//        List<ProcessDefinition> processDefinitions = query.listPage((pageNum - 1) * pageSize, pageSize);

        return jsonObjects;
    }

    /**
     * 是否被拒绝标记
     */
    public static final String REFUSEFLAG = "refuseFlag";
    /**
     * 发起流程
     **/
    @GetMapping("/startFlow")
    @Transactional(rollbackFor = Exception.class)
    public String startFlow(@RequestParam("key") String key) {
        Map<String, Object> map = Map.of(
                "businessType", "业务类型（业务审批、请假、出差等）",
                "day", 1,
                REFUSEFLAG, false
        );

        //发起人用户ID
        String userId = "initiator";

        //订单号
        String businessKey = "PO00001";

        //参数一 流程key
        //参数二 页面单据类型（比如请假流水号、订单号等）
        //参数三 运行时变量信息比如请假天数
        //设置发起人
        identityService.setAuthenticatedUserId(userId);

        //启动流程变量
        ProcessInstance processInstanceByKey = runtimeService.startProcessInstanceByKey(key, businessKey, map);

        log.info("流程实例id-{}", processInstanceByKey.getId());

        // 清除当前用户
        identityService.setAuthenticatedUserId(null);
        return processInstanceByKey.getId();
    }

    /**
     * 查询所有发起的流程及展示状态
     **/
    @GetMapping("queryAllprocess")
    public List<JSONObject> queryAllprocess() {
        // 获取所有活跃的流程实例（包括已结束的）
        List<HistoricProcessInstance> allProcessInstances = historyService.createHistoricProcessInstanceQuery()
                //查询用户参与过的流程实例
                //.involvedUser("1")
                //查询发起人用户
                //.startedBy(SysConstan.USER_ID)
                //根据变量查询（自己设的变量）
                //.variableValueEquals("businessType", "查询的业务类型")
                //根据订单号模糊查询
                //.processInstanceBusinessKeyLikeIgnoreCase("orderCode")
                .orderByProcessInstanceStartTime().asc()  // 可以按ID排序，便于调试
                .list(); // 查询所有的流程实例，包括历史的和活跃的

        List<JSONObject> jsonObjects = new ArrayList<>();

        for (HistoricProcessInstance processInstance : allProcessInstances) {
            String processInstanceId = processInstance.getId();
            JSONObject json = new JSONObject();
            if (processInstance.getEndTime() == null) {
                json.put("status", "审批中");
            } else {
                json.put("status", "审批完成");
            }
            json.put("id", processInstance.getProcessDefinitionId());
            json.put("processInstanceId", processInstanceId);
            json.put("startUser", processInstance.getStartUserId());
            json.put("key", processInstance.getProcessDefinitionKey());
            json.put("businessKey", processInstance.getBusinessKey());
            json.put("name", processInstance.getProcessDefinitionName());
            json.put("deleteReason", processInstance.getDeleteReason());
            json.put("startTime", DateFormatUtils.format(processInstance.getStartTime(), "yyyy-MM-dd HH:mm:ss"));
            json.put("endTime", processInstance.getEndTime() != null ? DateFormatUtils.format(processInstance.getEndTime(), "yyyy-MM-dd HH:mm:ss") : "");

            // 获取与任务相关的所有变量
            // 获取该流程实例的变量
            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list();
            if(CollectionUtil.isNotEmpty(variables)){
                for (HistoricVariableInstance variable : variables) {
                    json.put(variable.getVariableName(), variable.getValue());

                    if (REFUSEFLAG.equals(variable.getVariableName()) && variable.getValue() != null && variable.getValue().toString().equalsIgnoreCase("true")) {
                        json.put("status", "审批驳回");
                    }
                }
            }

            jsonObjects.add(json);
        }

        return jsonObjects;
    }

    /**
     * 获取代办列表 (这里暂时查看所有的)
     **/
    @GetMapping("/allTasks")
    public List<JSONObject> getTasks() {
        List<Task> taskList = taskService
                .createTaskQuery()
                //查询业务类型为指定的任务
                //.processInstanceBusinessKey("LEAVE")
                //查询所有zhangsan用户代办的任务
                //.taskAssignee(SysConstan.USER_ID)
                .list();

        List<JSONObject> jsonObjects = new ArrayList<>();

        for (Task task : taskList) {
            JSONObject json = new JSONObject();
            json.put("id", task.getId());
            json.put("name", task.getName());
            json.put("user", task.getAssignee());
            json.put("processDefinitionId", task.getProcessDefinitionId());
            json.put("processInstanceId", task.getProcessInstanceId());

            // 获取与任务相关的所有变量
            Map<String, Object> taskVariables = taskService.getVariables(task.getId());

            // 打印出任务的变量
            json.putAll(taskVariables);

            jsonObjects.add(json);
        }

        return jsonObjects;
    }

    /**
     * 完成任务
     **/
    @GetMapping("/testComplete")
    @Transactional(rollbackFor = Exception.class)
    public boolean testComplete(@RequestParam("id") String taskId) {     // 获取任务对应的流程实例ID
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        // 检查任务是否为空
        if (task == null) {
            System.out.println("任务不存在");
            return false;
        }

        String processInstanceId = task.getProcessInstanceId();

        String orderCode = (String) runtimeService.getVariable(processInstanceId, "orderCode");

        log.info("业务单据:{}", orderCode);
        // 1. 设置新执行者
        //taskService.setAssignee(taskId, newAssigneeId); // 任务的执行者被替换为新的用户

        taskService.addComment(taskId, processInstanceId, "备注信息test");
        // 完成任务
        taskService.complete(taskId);

        // 查询当前流程实例的所有活动任务
        boolean isFinish =  processInstanceFinished(processInstanceId);
        log.debug("流程是否完成L：{}", isFinish);

        return isFinish;
    }
    /**
     * 校验当前流程是否结束了
     **/
    public boolean processInstanceFinished(String processInstanceId) {
        // 获取当前流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        // 如果 processInstance 为空，表示该流程实例已结束
        return processInstance == null;
    }

    /**
     * 驳回流程
     **/
    @GetMapping("stopFlow")
    @Transactional(rollbackFor = Exception.class)
    public void stopFlow(@RequestParam("id") String processInstanceId) {
        // 在删除前设置拒绝变量
        runtimeService.setVariable(processInstanceId, "refuseFlag", true);

        //拒绝 后一个参数是拒绝的原因
        runtimeService.deleteProcessInstance(processInstanceId, "驳回任务备注原因");
    }
}
