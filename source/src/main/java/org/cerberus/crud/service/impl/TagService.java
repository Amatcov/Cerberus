/**
 * Cerberus Copyright (C) 2013 - 2017 cerberustesting
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This file is part of Cerberus.
 *
 * Cerberus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cerberus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cerberus.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cerberus.crud.service.impl;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.cerberus.crud.dao.ITagDAO;
import org.cerberus.crud.entity.Tag;
import org.cerberus.crud.factory.IFactoryTag;
import org.cerberus.crud.service.ITagService;
import org.cerberus.crud.service.ITestCaseExecutionService;
import org.cerberus.engine.entity.MessageGeneral;
import org.cerberus.enums.MessageEventEnum;
import org.cerberus.enums.MessageGeneralEnum;
import org.cerberus.exception.CerberusException;
import org.cerberus.service.ciresult.ICIService;
import org.cerberus.service.email.IEmailService;
import org.cerberus.util.StringUtil;
import org.cerberus.util.answer.Answer;
import org.cerberus.util.answer.AnswerItem;
import org.cerberus.util.answer.AnswerList;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author bcivel
 */
@Service
public class TagService implements ITagService {

    @Autowired
    private ITagDAO tagDAO;
    @Autowired
    private IFactoryTag factoryTag;
    @Autowired
    private IEmailService emailService;
    @Autowired
    private ITestCaseExecutionService testCaseExecutionService;
    @Autowired
    private ICIService ciService;

    private static final Logger LOG = LogManager.getLogger("TagService");

    private final String OBJECT_NAME = "Tag";

    @Override
    public AnswerItem<Tag> readByKey(String tag) {
        return tagDAO.readByKey(tag);
    }

    @Override
    public AnswerItem<Tag> readByKeyTech(long tag) {
        return tagDAO.readByKeyTech(tag);
    }

    @Override
    public AnswerList<Tag> readAll() {
        return tagDAO.readByVariousByCriteria(null, 0, 0, "id", "desc", null, null);
    }

    @Override
    public AnswerList readByCampaign(String campaign) {
        return tagDAO.readByVariousByCriteria(campaign, 0, 0, "id", "desc", null, null);
    }

    @Override
    public AnswerList readByCriteria(int startPosition, int length, String columnName, String sort, String searchParameter, Map<String, List<String>> individualSearch) {
        return tagDAO.readByVariousByCriteria(null, startPosition, length, columnName, sort, searchParameter, individualSearch);
    }

    @Override
    public AnswerList readByVariousByCriteria(String campaign, int startPosition, int length, String columnName, String sort, String searchParameter, Map<String, List<String>> individualSearch) {
        return tagDAO.readByVariousByCriteria(campaign, startPosition, length, columnName, sort, searchParameter, individualSearch);
    }

    @Override
    public boolean exist(String object) {
        AnswerItem objectAnswer = readByKey(object);
        return (objectAnswer.isCodeEquals(MessageEventEnum.DATA_OPERATION_OK.getCode())) && (objectAnswer.getItem() != null); // Call was successfull and object was found.
    }

    @Override
    public Answer create(Tag object) {
        return tagDAO.create(object);
    }

    @Override
    public Answer delete(Tag object) {
        return tagDAO.delete(object);
    }

    @Override
    public Answer update(String tag, Tag object) {
        return tagDAO.update(tag, object);
    }

    @Override
    public Answer updateEndOfQueueData(String tag) {

        try {
            Tag mytag = convert(readByKey(tag));

//            List<TestCaseExecution> testCaseExecutions = testCaseExecutionService.readLastExecutionAndExecutionInQueueByTag(tag);
//            List<TestCaseExecution> testCaseExecutionsTot = testCaseExecutionService.convert(testCaseExecutionService.readByTag(tag));
//            Map<String, Integer> axisMap = new HashMap<>();
//            Integer total;
//            total = testCaseExecutions.size();
//            for (TestCaseExecution execution : testCaseExecutions) {
//                if (axisMap.containsKey(execution.getControlStatus())) {
//                    axisMap.put(execution.getControlStatus(), axisMap.get(execution.getControlStatus()) + 1);
//                } else {
//                    axisMap.put(execution.getControlStatus(), 1);
//                }
//            }
            // Total execution.
            mytag.setNbExe(testCaseExecutionService.readNbByTag(tag));

            // End of queue is now.
            mytag.setDateEndQueue(new Timestamp(new Date().getTime()));
            
            // All the rest of the data are coming from ResultCI Servlet.
            JSONObject jsonResponse = ciService.getCIResult(tag);
            mytag.setCiResult(jsonResponse.getString("result"));
            mytag.setCiScore(jsonResponse.getInt("CI_finalResult"));
            mytag.setCiScoreThreshold(jsonResponse.getInt("CI_finalResultThreshold"));
            mytag.setNbOK(jsonResponse.getInt("status_OK_nbOfExecution"));
            mytag.setNbKO(jsonResponse.getInt("status_KO_nbOfExecution"));
            mytag.setNbFA(jsonResponse.getInt("status_FA_nbOfExecution"));
            mytag.setNbNA(jsonResponse.getInt("status_NA_nbOfExecution"));
            mytag.setNbNE(jsonResponse.getInt("status_NE_nbOfExecution"));
            mytag.setNbWE(jsonResponse.getInt("status_WE_nbOfExecution"));
            mytag.setNbPE(jsonResponse.getInt("status_PE_nbOfExecution"));
            mytag.setNbQU(jsonResponse.getInt("status_QU_nbOfExecution"));
            mytag.setNbQE(jsonResponse.getInt("status_QE_nbOfExecution"));
            mytag.setNbCA(jsonResponse.getInt("status_CA_nbOfExecution"));
            mytag.setNbExeUsefull(jsonResponse.getInt("TOTAL_nbOfExecution"));

            return tagDAO.updateDateEndQueue(mytag);

        } catch (CerberusException ex) {
            java.util.logging.Logger.getLogger(TagService.class.getName()).log(Level.SEVERE, null, ex);
            return null;

        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(TagService.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

    }

    @Override
    public Answer createAuto(String tagS, String campaign, String user) {
        AnswerItem answerTag;
        answerTag = readByKey(tagS);
        Tag tag = (Tag) answerTag.getItem();
        if (tag == null) {
            Answer ans = tagDAO.create(factoryTag.create(0, tagS, "", campaign, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", user, null, user, null));
            if (!StringUtil.isNullOrEmpty(campaign)) {
                emailService.generateAndSendNotifyStartTagExecution(tagS, campaign);
            }
            return ans;
            // If campaign is not empty, we could notify the Start of campaign execution.
        } else {
            if ((StringUtil.isNullOrEmpty(tag.getCampaign())) && !StringUtil.isNullOrEmpty(campaign)) {
                tag.setCampaign(campaign);
                return tagDAO.update(tag.getTag(), tag);
            }
            return null;
        }
    }

    @Override
    public Tag convert(AnswerItem<Tag> answerItem) throws CerberusException {
        if (answerItem.isCodeEquals(MessageEventEnum.DATA_OPERATION_OK.getCode())) {
            //if the service returns an OK message then we can get the item
            return (Tag) answerItem.getItem();
        }
        throw new CerberusException(new MessageGeneral(MessageGeneralEnum.DATA_OPERATION_ERROR));
    }

    @Override
    public List<Tag> convert(AnswerList<Tag> answerList) throws CerberusException {
        if (answerList.isCodeEquals(MessageEventEnum.DATA_OPERATION_OK.getCode())) {
            //if the service returns an OK message then we can get the item
            return (List<Tag>) answerList.getDataList();
        }
        throw new CerberusException(new MessageGeneral(MessageGeneralEnum.DATA_OPERATION_ERROR));
    }

    @Override
    public void convert(Answer answer) throws CerberusException {
        if (answer.isCodeEquals(MessageEventEnum.DATA_OPERATION_OK.getCode())) {
            //if the service returns an OK message then we can get the item
            return;
        }
        throw new CerberusException(new MessageGeneral(MessageGeneralEnum.DATA_OPERATION_ERROR));
    }

    @Override
    public AnswerList<String> readDistinctValuesByCriteria(String system, String searchParameter, Map<String, List<String>> individualSearch, String columnName) {
        return tagDAO.readDistinctValuesByCriteria(system, searchParameter, individualSearch, columnName);
    }

}
