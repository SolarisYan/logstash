package org.apache.mesos.logstash.state;

import org.apache.log4j.Logger;
import org.apache.mesos.Protos.TaskInfo;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.NotSerializableException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.mesos.Protos.TaskID;

/**
 * Model of cluster state. User is able to add, remove and monitor task status.
 */
@Component
public class ClusterState {
    public static final Logger LOGGER = Logger.getLogger(ClusterState.class);
    public static final String STATE_LIST = "stateList";
    @Inject
    SerializableState state;
    @Inject
    FrameworkState frameworkState;
    @Inject
    StatePath statePath;

    /**
     * Get a list of all tasks with state
     * @return a list of TaskInfo
     */
    public List<TaskInfo> getTaskList() {
        List<TaskInfo> taskInfoList = null;
        try {
            taskInfoList = state.get(getKey());
        } catch (IOException ex) {
            LOGGER.info("Unable to get key for cluster state due to invalid frameworkID.");
        }
        return taskInfoList == null ? new ArrayList<>(0) : taskInfoList;
    }

    /**
     * Get the status of a specific task
     * @param taskID the taskID to retreive the task status for
     * @return a POJO representing TaskInfo, TaskStatus and FrameworkID packets
     * @throws InvalidParameterException when the taskId does not exist in the Task list.
     */
    public LSTaskStatus getStatus(TaskID taskID) throws InvalidParameterException {
        TaskInfo taskInfo = getTask(taskID);
        return new LSTaskStatus(state, frameworkState.getFrameworkID(), taskInfo, statePath);
    }

    public void addTask(TaskInfo taskInfo) {
        LOGGER.debug("Adding TaskInfo to cluster for task: " + taskInfo.getTaskId().getValue());
        if (exists(taskInfo.getTaskId())) {
            removeTask(taskInfo);
        }
        List<TaskInfo> taskList = getTaskList();
        taskList.add(taskInfo);
        setTaskInfoList(taskList);
    }

    public void removeTask(TaskInfo taskInfo) throws InvalidParameterException {
        List<TaskInfo> taskList = getTaskList();
        Boolean found = false;
        for (TaskInfo info : taskList) {
            if (isEqual(info, taskInfo)) {
                LOGGER.debug("Removing TaskInfo from cluster for task: " + taskInfo.getTaskId().getValue());
                taskList.remove(info);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new InvalidParameterException("TaskInfo does not exist in list: " + taskInfo.getTaskId().getValue());
        }
        setTaskInfoList(taskList);
    }

    private boolean isEqual(TaskInfo taskInfo1, TaskInfo taskInfo2) {
        return taskInfo1.getTaskId().getValue().equals(taskInfo2.getTaskId().getValue());
    }

    public Boolean exists(TaskID taskId) {
        try {
            getStatus(taskId);
        } catch (InvalidParameterException e) {
            return false;
        }
        return true;
    }

    /**
     * Get the TaskInfo packet for a specific task.
     * @param taskID the taskID to retreive the TaskInfo for
     * @return a TaskInfo packet
     * @throws InvalidParameterException when the taskId does not exist in the Task list.
     */
    private TaskInfo getTask(TaskID taskID) throws InvalidParameterException {
        LOGGER.debug("Getting taskInfo from cluster for task: " + taskID.getValue());
        List<TaskInfo> taskInfoList = getTaskList();
        TaskInfo taskInfo = null;
        for (TaskInfo info : taskInfoList) {
            if (info.getTaskId().equals(taskID)) {
                taskInfo = info;
                break;
            }
        }
        if (taskInfo == null) {
            throw new InvalidParameterException("Could not find executor with that task ID: " + taskID.getValue());
        }
        return taskInfo;
    }

    private String logTaskList(List<TaskInfo> taskInfoList) {
        List<String> res = new ArrayList<>();
        for (TaskInfo t : taskInfoList) {
            res.add(t.getTaskId().getValue());
        }
        return Arrays.toString(res.toArray());
    }

    private void setTaskInfoList(List<TaskInfo> taskInfoList) {
        LOGGER.debug("Writing executor state list: " + logTaskList(taskInfoList));
        try {
            statePath.mkdir(getKey());
            state.set(getKey(), taskInfoList);
        } catch (IOException ex) {
            LOGGER.error("Could not write list of executor states to zookeeper: ", ex);
        }
    }

    private String getKey() throws NotSerializableException {
        return frameworkState.getFrameworkID().getValue() + "/" + STATE_LIST;
    }

    public List<TaskID> getTaskIdList() {
        return getTaskList().stream().map(TaskInfo::getTaskId).collect(Collectors.toList());
    }
}
