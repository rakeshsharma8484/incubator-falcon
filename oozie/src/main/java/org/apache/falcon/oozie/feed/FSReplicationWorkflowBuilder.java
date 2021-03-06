/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.falcon.oozie.feed;

import org.apache.falcon.FalconException;
import org.apache.falcon.Tag;
import org.apache.falcon.entity.EntityUtil;
import org.apache.falcon.entity.v0.cluster.Cluster;
import org.apache.falcon.entity.v0.feed.Feed;
import org.apache.falcon.oozie.workflow.ACTION;
import org.apache.falcon.oozie.workflow.WORKFLOWAPP;

import java.util.Arrays;

/**
 * Builds replication workflow for filesystem based feed.
 */
public class FSReplicationWorkflowBuilder extends FeedReplicationWorkflowBuilder{
    public FSReplicationWorkflowBuilder(Feed entity) {
        super(entity);
    }

    @Override protected WORKFLOWAPP getWorkflow(Cluster src, Cluster target) throws FalconException {
        WORKFLOWAPP workflow = new WORKFLOWAPP();
        String wfName = EntityUtil.getWorkflowName(Tag.REPLICATION, Arrays.asList(src.getName()), entity).toString();

        String start = REPLICATION_ACTION_NAME;

        //Add pre-processing
        if (shouldPreProcess()) {
            ACTION action = getPreProcessingAction(false, Tag.REPLICATION);
            addTransition(action, REPLICATION_ACTION_NAME, FAIL_POSTPROCESS_ACTION_NAME);
            workflow.getDecisionOrForkOrJoin().add(action);
            start = PREPROCESS_ACTION_NAME;
        }

        //Add replication
        ACTION replication = unmarshalAction(REPLICATION_ACTION_TEMPLATE);
        addTransition(replication, SUCCESS_POSTPROCESS_ACTION_NAME, FAIL_POSTPROCESS_ACTION_NAME);
        workflow.getDecisionOrForkOrJoin().add(replication);

        //Add post-processing actions
        ACTION success = getSuccessPostProcessAction();
        addTransition(success, OK_ACTION_NAME, FAIL_ACTION_NAME);
        workflow.getDecisionOrForkOrJoin().add(success);

        ACTION fail = getFailPostProcessAction();
        addTransition(fail, FAIL_ACTION_NAME, FAIL_ACTION_NAME);
        workflow.getDecisionOrForkOrJoin().add(fail);

        decorateWorkflow(workflow, wfName, start);
        return workflow;
    }
}
